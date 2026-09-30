package com.chuckwolfe.inboxpriorities

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

class BillingManager(
    context: Context,
    private val onPurchaseSuccess: () -> Unit
) : PurchasesUpdatedListener {

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    fun startConnection(onConnected: () -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    onConnected()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Try to restart connection on the next request to Google Play by calling the startConnection again.
            }
        })
    }

    fun queryProductDetails(onResult: (ProductDetails?) -> Unit) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("inbox_priorities_pro_lifetime")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult: BillingResult, productDetailsList: List<ProductDetails> ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onResult(productDetailsList.firstOrNull())
            } else {
                onResult(null)
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgeParams) { billingResult: BillingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        onPurchaseSuccess()
                    }
                }
            } else {
                onPurchaseSuccess()
            }
        }
    }

    fun checkExistingPurchases(onChecked: (Boolean) -> Unit) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult: BillingResult, purchases: List<Purchase> ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPurchased = purchases.any { purchase ->
                    purchase.products.contains("inbox_priorities_pro_lifetime") &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                onChecked(hasPurchased)
            } else {
                onChecked(false)
            }
        }
    }
}