package com.polotech.inboxpriorities

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)

    // Initialize state from local storage so it persists across sessions
    private val _isProUnlocked = MutableStateFlow(prefs.getBoolean("is_pro_unlocked", false))
    val isProUnlocked: StateFlow<Boolean> = _isProUnlocked.asStateFlow()

    private val _productPrice = MutableStateFlow("Loading...")
    val productPrice: StateFlow<String> = _productPrice.asStateFlow()

    private var cachedProductDetails: ProductDetails? = null

    private val billingManager = BillingManager(application) {
        // This callback triggers when a purchase is successfully acknowledged
        _isProUnlocked.value = true
        prefs.edit().putBoolean("is_pro_unlocked", true).apply()
    }

    init {
        billingManager.startConnection {
            // Check if the user already bought it previously
            billingManager.checkExistingPurchases { hasPurchased ->
                _isProUnlocked.value = hasPurchased
                prefs.edit().putBoolean("is_pro_unlocked", hasPurchased).apply()
            }
            // Fetch the price from Google Play Console
            billingManager.queryProductDetails { details ->
                cachedProductDetails = details
                _productPrice.value = details?.oneTimePurchaseOfferDetails?.formattedPrice ?: "N/A"
            }
        }
    }

    fun buyPro(activity: Activity) {
        val details = cachedProductDetails
        if (details != null) {
            billingManager.launchPurchaseFlow(activity, details)
        } else {
            // Fallback for local testing so you know why it didn't launch
            android.widget.Toast.makeText(
                getApplication(),
                "Product details not loaded. Check Play Console SKU configuration.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}