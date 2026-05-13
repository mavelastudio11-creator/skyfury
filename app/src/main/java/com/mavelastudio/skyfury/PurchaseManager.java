package com.mavelastudio.skyfury;

import android.app.Activity;

interface PurchaseManager {
    void launchBuyGold(Activity activity, String productId, int amount, Callback callback);

    void destroy();

    interface Callback {
        void onGoldGranted(int amount, String message);

        void onPurchasePending(String message);

        void onPurchaseFailed(String message);

        void onStoreUnavailable(String message);
    }
}
