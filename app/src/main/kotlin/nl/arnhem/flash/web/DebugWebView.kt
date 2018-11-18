package nl.arnhem.flash.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.support.annotation.WorkerThread
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView
import nl.arnhem.flash.facebook.USER_AGENT_BASIC
import nl.arnhem.flash.injectors.CssAssets
import nl.arnhem.flash.injectors.CssHider
import nl.arnhem.flash.injectors.jsInject
import nl.arnhem.flash.utils.L
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.createFreshFile
import nl.arnhem.flash.utils.iab.IS_Flash_PRO
import nl.arnhem.flash.utils.isFacebookUrl
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.jetbrains.anko.withAlpha
import java.io.File

/**
 * Created by Allan Wang on 2018-01-05.
 *
 * A barebone webview with a refresh listener
 */
class DebugWebView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    var onPageFinished: (String?) -> Unit = {}

    init {
        setupWebview()
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun setupWebview() {
        settings.javaScriptEnabled = true
        settings.userAgentString = USER_AGENT_BASIC
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webViewClient = DebugClient()
        isDrawingCacheEnabled = true
    }

    @WorkerThread
    fun getScreenshot(output: File): Boolean {

        if (!output.createFreshFile()) {
            L.e { "Failed to create ${output.absolutePath} for debug screenshot" }
            return false
        }
        return try {
            output.outputStream().use {
                drawingCache.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            L.d { "Created screenshot at ${output.absolutePath}" }
            true
        } catch (e: Exception) {
            L.e { "An error occurred ${e.message}" }
            false
        }
    }

    private inner class DebugClient : BaseWebViewClient() {

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            onPageFinished(url)
        }

        private fun injectBackgroundColor() {
            setBackgroundColor(
                    if (url.isFacebookUrl) Prefs.bgColor.withAlpha(255)
                    else Color.WHITE)
        }


        override fun onPageCommitVisible(view: WebView, url: String?) {
            super.onPageCommitVisible(view, url)
            injectBackgroundColor()
            if (url.isFacebookUrl)
                view.jsInject(
                        CssAssets.ROUND_ICONS.maybe(Prefs.showRoundedIcons),
//                        CssHider.CORE,
                        CssHider.COMPOSER.maybe(!Prefs.showComposer),
                        CssHider.PEOPLE_YOU_MAY_KNOW.maybe(!Prefs.showSuggestedFriends),
                        CssHider.SUGGESTED_GROUPS.maybe(!Prefs.showSuggestedGroups),
                        Prefs.themeInjector,
                        CssHider.NON_RECENT.maybe((url?.contains("?sk=h_chr") ?: false)
                                && Prefs.aggressiveRecents))
        }
    }

}