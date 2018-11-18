package nl.arnhem.flash.utils.iab

import android.app.Activity
import android.content.Intent
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import com.crashlytics.android.answers.PurchaseEvent
import nl.arnhem.flash.BuildConfig
import nl.arnhem.flash.utils.L
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.flashAnswers
import nl.arnhem.flash.utils.logFlashAnswers
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.onComplete
import org.jetbrains.anko.uiThread
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.util.*

/**
 * Created by Allan Wang on 2017-07-22.
 **/
private const val Flash_PRO = "arnhem_flash_pro"

/**
 * Implemented pro checker with a hook for debug builds
 * Use this when checking if the pro feature is enabled
 */
inline val IS_Flash_PRO: Boolean
    get() = Prefs.pro || Prefs.debugPro || (BuildConfig.DEBUG)

interface FlashBilling : BillingProcessor.IBillingHandler {
    fun Activity.onCreateBilling()
    fun onDestroyBilling()
    fun purchasePro()
    fun restorePurchases()
    fun onActivityResultBilling(requestCode: Int, resultCode: Int, data: Intent?): Boolean
}

abstract class IabBinder : FlashBilling {

    var bp: BillingProcessor? = null
    private lateinit var activityRef: WeakReference<Activity>
    val activity
        get() = activityRef.get()

    final override fun Activity.onCreateBilling() {
        activityRef = WeakReference(this)
        doAsync {
            bp = BillingProcessor.newBillingProcessor(this@onCreateBilling, PUBLIC_BILLING_KEY, this@IabBinder)
            bp?.initialize()
        }
    }

    override fun onDestroyBilling() {
        activityRef.clear()
        bp?.release()
        bp = null
    }

    override fun onBillingInitialized() = L.i { "IAB initialized" }

    override fun onPurchaseHistoryRestored() = L.d { "IAB restored" }

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
        bp.doAsync {
            L.i { "IAB $productId purchased" }
            val listing = weakRef.get()?.getPurchaseListingDetails(productId) ?: return@doAsync
            val currency = try {
                Currency.getInstance(listing.currency)
            } catch (e: Exception) {
                null
            }
            flashAnswers {
                logPurchase(PurchaseEvent().apply {
                    putItemId(productId)
                    putSuccess(true)
                    if (currency != null) {
                        putCurrency(currency)
                        putItemType(productId)
                        putItemPrice(BigDecimal.valueOf(listing.priceValue))
                    }
                })
            }
        }
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        flashAnswers {
            logPurchase(PurchaseEvent()
                    .putCustomAttribute("result", errorCode.toString())
                    .putSuccess(false))
        }
        error.logFlashAnswers("IAB error $errorCode")
    }

    override fun onActivityResultBilling(requestCode: Int, resultCode: Int, data: Intent?): Boolean = bp?.handleActivityResult(requestCode, resultCode, data)
            ?: false

    override fun purchasePro() {
        val bp = this.bp
        if (bp == null) {
            flashAnswers {
                logPurchase(PurchaseEvent()
                        .putCustomAttribute("result", "null bp")
                        .putSuccess(false))
            }
            L.eThrow("IAB null bp on purchase attempt")
            return
        }
        val a = activity ?: return

        if (!BillingProcessor.isIabServiceAvailable(a) || !bp.isInitialized || !bp.isOneTimePurchaseSupported)
            a.playStorePurchaseUnsupported()
        else
            bp.purchase(a, Flash_PRO)
    }

}

class IabSettings : IabBinder() {

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
        super.onProductPurchased(productId, details)
        activity?.playStorePurchasedSuccessfully(productId)
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        super.onBillingError(errorCode, error)
        L.e { "Billing error $errorCode ${error?.message}" }
    }

    /**
     * Attempts to get pro, or launch purchase flow if user doesn't have it
     */
    override fun restorePurchases() {
        bp.doAsync {
            val load = weakRef.get()?.loadOwnedPurchasesFromGoogle() ?: return@doAsync
            L.d { "IAB settings load from google $load" }
            uiThread {
                if (!(weakRef.get()?.isPurchased(Flash_PRO) ?: return@uiThread)) {
                    if (Prefs.pro) activity.playStoreNoLongerPro()
                    else purchasePro()
                } else {
                    if (!Prefs.pro) activity.playStoreFoundPro()
                    else activity?.purchaseRestored()
                }
            }
        }
    }
}

class IabMain : IabBinder() {

    override fun onBillingInitialized() {
        super.onBillingInitialized()
        restorePurchases()
    }

    override fun onPurchaseHistoryRestored() {
        super.onPurchaseHistoryRestored()
        restorePurchases()
    }

    private var restored = false

    /**
     * Checks for pro and only does so once
     * A null check is added but it should never happen
     * given that this is only called with bp is ready
     */
    override fun restorePurchases() {
        if (restored || bp == null) return
        restored = true
        bp.doAsync {
            val load = weakRef.get()?.loadOwnedPurchasesFromGoogle() ?: false
            L.d { "IAB main load from google $load" }
            onComplete {
                if (weakRef.get()?.isPurchased(Flash_PRO) != true) {
                    if (Prefs.pro) activity.playStoreNoLongerPro()
                } else {
                    if (!Prefs.pro) activity.playStoreFoundPro()
                }
                onDestroyBilling()
            }
        }
    }
}