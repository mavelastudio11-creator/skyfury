package com.mavelastudio.skyfury;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PlayBillingPurchaseManager implements PurchaseManager, PurchasesUpdatedListener {
    private static final String PREFS = "sky_fury_billing";
    private static final String PREF_GRANTED_TOKENS = "granted_tokens";

    private final Context appContext;
    private final SharedPreferences prefs;
    private final Map<String, Integer> productGoldMap = new HashMap<>();

    private BillingClient billingClient;
    private boolean connecting;
    private boolean ready;
    private boolean launchInProgress;

    private PurchaseManager.Callback callback;
    private String pendingProductId;
    private int pendingAmount;
    private WeakReference<Activity> pendingActivityRef;

    PlayBillingPurchaseManager(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        productGoldMap.put("gold_100", 100);
        productGoldMap.put("gold_300", 300);
        productGoldMap.put("gold_500", 500);
        productGoldMap.put("gold_1000", 1000);
        ensureClient();
    }

    @Override
    public synchronized void launchBuyGold(Activity activity, String productId, int amount, PurchaseManager.Callback cb) {
        callback = cb;
        pendingProductId = productId;
        pendingAmount = amount;
        pendingActivityRef = new WeakReference<>(activity);
        launchInProgress = true;

        if (!productGoldMap.containsKey(productId)) {
            launchInProgress = false;
            cb.onPurchaseFailed("Unknown product: " + productId);
            return;
        }

        ensureConnectedAndLaunch();
    }

    @Override
    public synchronized void destroy() {
        launchInProgress = false;
        pendingActivityRef = null;
        callback = null;
        pendingProductId = null;
        pendingAmount = 0;
        ready = false;
        connecting = false;
        if (billingClient != null) {
            billingClient.endConnection();
            billingClient = null;
        }
    }

    private void ensureClient() {
        if (billingClient != null) {
            return;
        }
        billingClient = BillingClient.newBuilder(appContext)
                .setListener(this)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()
                                .build())
                .build();
    }

    private synchronized void ensureConnectedAndLaunch() {
        ensureClient();
        if (billingClient == null) {
            notifyStoreUnavailable("Billing service is unavailable.");
            return;
        }
        if (ready) {
            launchPendingPurchase();
            return;
        }
        if (connecting) {
            return;
        }
        connecting = true;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                synchronized (PlayBillingPurchaseManager.this) {
                    ready = false;
                    connecting = false;
                }
            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                synchronized (PlayBillingPurchaseManager.this) {
                    connecting = false;
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        ready = true;
                        queryOwnedConsumables();
                        launchPendingPurchase();
                    } else {
                        ready = false;
                        notifyStoreUnavailable("Unable to connect to Google Play Billing.");
                    }
                }
            }
        });
    }

    private synchronized void launchPendingPurchase() {
        if (!launchInProgress || pendingProductId == null) {
            return;
        }
        if (billingClient == null || !ready) {
            return;
        }
        Activity activity = pendingActivityRef != null ? pendingActivityRef.get() : null;
        if (activity == null || activity.isFinishing()) {
            launchInProgress = false;
            notifyPurchaseFailed("Store is unavailable right now. Please try again.");
            return;
        }

        QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(pendingProductId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Arrays.asList(product))
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsResult) -> {
            synchronized (PlayBillingPurchaseManager.this) {
                if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    launchInProgress = false;
                    notifyPurchaseFailed("Could not load product details from Google Play.");
                    return;
                }
                List<ProductDetails> productDetailsList =
                        productDetailsResult != null ? productDetailsResult.getProductDetailsList() : null;
                if (productDetailsList == null || productDetailsList.isEmpty()) {
                    launchInProgress = false;
                    notifyPurchaseFailed("Product is not available in Google Play Console yet.");
                    return;
                }

                ProductDetails details = productDetailsList.get(0);
                BillingFlowParams.ProductDetailsParams pdParams =
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(details)
                                .build();
                BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(Arrays.asList(pdParams))
                        .build();

                BillingResult launchResult = billingClient.launchBillingFlow(activity, flowParams);
                if (launchResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    launchInProgress = false;
                    notifyPurchaseFailed("Unable to open purchase flow.");
                }
            }
        });
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        synchronized (this) {
            int code = billingResult.getResponseCode();
            if (code == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (Purchase purchase : purchases) {
                    processPurchase(purchase);
                }
                return;
            }

            launchInProgress = false;
            if (code == BillingClient.BillingResponseCode.USER_CANCELED) {
                notifyPurchaseFailed("Purchase canceled.");
            } else if (code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                queryOwnedConsumables();
            } else {
                notifyPurchaseFailed("Purchase failed. Please try again.");
            }
        }
    }

    private void queryOwnedConsumables() {
        if (billingClient == null || !ready) {
            return;
        }
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK || purchases == null) {
                return;
            }
            synchronized (PlayBillingPurchaseManager.this) {
                for (Purchase purchase : purchases) {
                    processPurchase(purchase);
                }
            }
        });
    }

    private void processPurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            notifyPending("Purchase is pending approval.");
            return;
        }
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) {
            return;
        }

        List<String> products = purchase.getProducts();
        if (products == null || products.isEmpty()) {
            return;
        }
        String token = purchase.getPurchaseToken();
        if (token == null || token.isEmpty()) {
            return;
        }

        boolean alreadyGranted = isTokenGranted(token);
        int totalGold = 0;
        for (String productId : products) {
            Integer amount = productGoldMap.get(productId);
            if (amount != null) {
                totalGold += amount;
            }
        }
        if (totalGold <= 0) {
            return;
        }

        if (!alreadyGranted) {
            markTokenGranted(token);
            notifyGoldGranted(totalGold, "Purchased " + totalGold + " gold.");
        }
        consumePurchase(token);
    }

    private void consumePurchase(String purchaseToken) {
        if (billingClient == null || !ready) {
            return;
        }
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build();
        billingClient.consumeAsync(consumeParams, (billingResult, token) -> {
            synchronized (PlayBillingPurchaseManager.this) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    launchInProgress = false;
                }
            }
        });
    }

    private boolean isTokenGranted(String token) {
        Set<String> grantedTokens = prefs.getStringSet(PREF_GRANTED_TOKENS, new HashSet<>());
        return grantedTokens != null && grantedTokens.contains(token);
    }

    private void markTokenGranted(String token) {
        Set<String> current = prefs.getStringSet(PREF_GRANTED_TOKENS, new HashSet<>());
        Set<String> copy = new HashSet<>();
        if (current != null) {
            copy.addAll(current);
        }
        copy.add(token);
        prefs.edit().putStringSet(PREF_GRANTED_TOKENS, copy).apply();
    }

    private void notifyGoldGranted(int amount, String message) {
        if (callback != null) {
            callback.onGoldGranted(amount, message);
        }
    }

    private void notifyPending(String message) {
        if (callback != null) {
            callback.onPurchasePending(message);
        }
    }

    private void notifyPurchaseFailed(String message) {
        if (callback != null) {
            callback.onPurchaseFailed(message);
        }
    }

    private void notifyStoreUnavailable(String message) {
        if (callback != null) {
            callback.onStoreUnavailable(message);
        }
    }
}
