package com.mavelastudio.skyfury;

import android.app.Activity;

final class NoOpPurchaseManager implements PurchaseManager {
    @Override
    public void launchBuyGold(Activity activity, String productId, int amount, Callback callback) {
        callback.onStoreUnavailable("In-app purchases are coming soon.");
    }

    @Override
    public void destroy() {
        // No-op by design.
    }
}
