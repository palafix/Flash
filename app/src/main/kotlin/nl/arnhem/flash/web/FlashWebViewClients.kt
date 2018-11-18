package nl.arnhem.flash.web

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.webkit.*
import io.reactivex.subjects.Subject
import nl.arnhem.flash.R
import nl.arnhem.flash.facebook.*
import nl.arnhem.flash.injectors.*
import nl.arnhem.flash.utils.*
import nl.arnhem.flash.utils.iab.IS_Flash_PRO
import nl.arnhem.flash.views.FlashWebView
import org.jetbrains.anko.withAlpha


/**
 * Created by Allan Wang on 2017-05-31.
 *
 * Collection of webview clients
 */

/**
 * The base of all webview clients
 * Used to ensure that resources are properly intercepted
 */

open class BaseWebViewClient : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? = view.shouldFlashInterceptRequest(request)
}


/**
 * The default webview client
 */
open class FlashWebViewClient(val web: FlashWebView) : BaseWebViewClient() {

    private val refresh: Subject<Boolean> = web.parent.refreshObservable
    private val isMain = web.parent.baseEnum != null
    protected inline fun v(crossinline message: () -> Any?) = L.v { "web client: ${message()}" }

    private val dialog=  web.context.materialDialogThemedImage{
            progress(true, 100)
            content(R.string.image_loading)
            negativeText(R.string.kau_cancel)
            onNegative { dialog, _ -> dialog.dismiss() }
            canceledOnTouchOutside(false)
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (url == null) return
        v { "loading $url" }
        refresh.onNext(true)
    }


    private fun injectBackgroundColor() {
        web.setBackgroundColor(
                when {
                    isMain -> Color.TRANSPARENT
                    web.url.isFacebookUrl -> Prefs.bgColor.withAlpha(255)
                    else -> Color.WHITE
                }
        )
    }

    override fun onPageCommitVisible(view: WebView, url: String?) {
        super.onPageCommitVisible(view, url)
        injectBackgroundColor()
        if (url.isFacebookUrl) {
            web.jsInject(
                    CssAssets.ROUND_ICONS.maybe(Prefs.showRoundedIcons),
                    //CssStatus.COMPOSER_STATUS,
                    CssHider.CORE,
                    CssHider.COMPOSER.maybe(!Prefs.showComposer),
                    CssHider.STORIESTRAY.maybe(!Prefs.showStoriesTray),
                    CssFixed.COMPOSER_FIXED.maybe(Prefs.fixedComposer),
                    CssPadding.COMPOSER_PADDING.maybe(Prefs.paddingComposer),
                    CssHider.COMPOSER_BOTTOM.maybe(Prefs.bottomComposer),
                    CssHider.PEOPLE_YOU_MAY_KNOW.maybe(!Prefs.showSuggestedFriends && IS_Flash_PRO),
                    CssHider.SUGGESTED_GROUPS.maybe(!Prefs.showSuggestedGroups && IS_Flash_PRO),
                    Prefs.themeInjector,
                    CssHider.NON_RECENT.maybe((web.url?.contains("?sk=h_chr")
                            ?: false)
                            && Prefs.aggressiveRecents),
                    CssHider.VIDEOS,
                    CssHider.VIDEOS2,
                    //CssHider.WWW_FACEBOOK_COM,
                    CssHider.HEADER_TOP,
                    CssHider.HEADER,
                    JsAssets.DOCUMENT_WATCHER,
                    JsAssets.HEADER_HIDER,
                    JsAssets.CLICK_A,
                    CssHider.ADS.maybe(!Prefs.showFacebookAds && IS_Flash_PRO),
                    JsAssets.CONTEXT_A,
                    JsActions.AUDIO_OFF.maybe(Prefs.DisableAudio),
                    CssAssets.MATERIAL_AMOLED.maybe(Prefs.DayNight && isNightTime(Activity())),
                    JsAssets.MEDIA)
        } else
            refresh.onNext(false)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        url ?: return
        //if (url.startsWith("https://www.facebook.com") || url.startsWith("https://web.facebook.com")) {
        //    view.loadUrl("javascript:function removeElement(id) { var node = document.getElementById(id); node.parentNode.removeChild(node); } removeElement('pagelet_bluebar');removeElement('leftCol');removeElement('rightCol');")
        //}
        v { "finished $url" }
        if (!url.isFacebookUrl) {
            refresh.onNext(false)
            return
        }
        //if (web.copyBackForwardList().currentIndex > 0) {
            //web.clearHistory()
        //} else if (web.copyBackForwardList().currentIndex == 0) {
        //return
        //}
        onPageFinishedActions(url)
    }

    @Suppress("DEPRECATION", "OverridingDeprecatedMember")
    override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
        if (view.url == failingUrl) {
            view.context.materialDialogThemed {
                title(web.title)
                content(view.context.resources.getString(R.string.no_network))
                positiveText(R.string.kau_ok)
                onPositive { _, _ ->
                        web.reload()
                }
            }
        }
        super.onReceivedError(view, errorCode, description, failingUrl)
        Log.d("Facebook : ", "Failed to load page")
    }

    internal open fun onPageFinishedActions(url: String) {
        if (url.startsWith("${FbItem.MESSAGES.url}/read/") && Prefs.messageScrollToBottom)
            web.pageDown(true)
        injectAndFinish()
    }

    internal fun injectAndFinish() {
        v { "page finished reveal" }
        refresh.onNext(false)
        injectBackgroundColor()
        web.jsInject(
                JsActions.LOGIN_CHECK,
                JsAssets.TEXTAREA_LISTENER,
                JsAssets.HEADER_BADGES.maybe(isMain))
    }

    open fun handleHtml(html: String?) {
        L.d { "Handle Html" }
    }

    open fun emit(flag: Int) {
        L.d { "Emit $flag" }
    }

    /**
     * Helper to format the request and launch it
     * returns true to override the url
     * returns false if we are already in an overlaying activity
     */
    private fun launchRequest(request: WebResourceRequest): Boolean {
        v { "Launching url: ${request.url}" }
        return web.requestWebOverlay(request.url.toString())
    }

    private fun launchImage(url: String, text: String? = null, title: String? = null, cookie: String? = null): Boolean {
        v { "Launching image: $url" }
        web.context.launchImageActivity(url, text, title, cookie)
        if (web.canGoBack()) web.goBack()
        return true
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        super.shouldOverrideUrlLoading(view, request)
        v { "Url loading: ${request.url}" }
        val path = request.url?.path ?: return super.shouldOverrideUrlLoading(view, request)
        v { "Url path $path" }
        val url = request.url.toString()
        if (url.isExplicitIntent) {
            view.context.resolveActivityForUri(request.url)
            return true
        }
        if (path.startsWith("/composer/")) return launchRequest(request)
        if (url.isImageUrl) return launchImage(url.formattedFbUrl, view.title)
        if (url.isIndirectImageUrl) return launchImage(url.formattedFbUrl, view.title, cookie = FbCookie.webCookie)
        if (Prefs.linksInDefaultApp && view.context.resolveActivityForUri(request.url)) return true
        return super.shouldOverrideUrlLoading(view, request)
    }
}

private const val EMIT_THEME = 0b1
private const val EMIT_ID = 0b10
private const val EMIT_COMPLETE = EMIT_THEME or EMIT_ID
private const val EMIT_FINISH = 0

/**
 * Client variant for the menu view
 */
class FlashWebViewClientMenu(web: FlashWebView) : FlashWebViewClient(web) {

    private val String.shouldInjectMenu
        get() = when (removePrefix(FB_URL_BASE)) {
            "settings",
            "settings#",
            "settings#!/settings?soft=bookmarks" -> true
            else -> false
        }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        if (url == null) return
        if (url.shouldInjectMenu) jsInject(JsAssets.MENU)
    }

    override fun emit(flag: Int) {
        super.emit(flag)
        when (flag) {
            EMIT_FINISH -> super.injectAndFinish()
        }
    }

    override fun onPageFinishedActions(url: String) {
        v { "Should inject ${url.shouldInjectMenu}" }
        if (!url.shouldInjectMenu) injectAndFinish()
    }
}