package nl.arnhem.flash.fragments

import android.webkit.WebView
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import nl.arnhem.flash.R
import nl.arnhem.flash.contracts.MainFabContract
import nl.arnhem.flash.facebook.FbItem
import nl.arnhem.flash.injectors.JsActions
import nl.arnhem.flash.utils.L
import nl.arnhem.flash.views.FlashWebView
import nl.arnhem.flash.web.FlashWebViewClient
import nl.arnhem.flash.web.FlashWebViewClientMenu

/**
 * Created by Allan Wang on 27/12/17.
 *
 * Basic webfragment
 * Do not extend as this is always a fallback
 */
class WebFragment : BaseFragment() {

    override val layoutRes: Int = R.layout.view_content_web

    /**
     * Given a webview, output a client
     */
    fun client(web: FlashWebView) = when (baseEnum) {
        FbItem.MENU -> FlashWebViewClientMenu(web)
        else -> FlashWebViewClient(web)
    }

    override fun updateFab(contract: MainFabContract) {
        L.e { "Update fab" }
        val web = core as? WebView
        if (web == null) {
            L.e { "Webview not found in fragment $baseEnum" }
            return super.updateFab(contract)
        }
        if (baseEnum.isFeed) {
            contract.showFab(GoogleMaterial.Icon.gmd_flash_on) {
                JsActions.CREATE_POST.inject(web)
            }
            L.e { "UPP" }
            return
        }
        super.updateFab(contract)
    }
}