package nl.arnhem.flash.views

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import ca.allanwang.kau.utils.AnimHolder
import nl.arnhem.flash.contracts.FlashContentContainer
import nl.arnhem.flash.contracts.FlashContentCore
import nl.arnhem.flash.contracts.FlashContentParent
import nl.arnhem.flash.facebook.USER_AGENT_BASIC
import nl.arnhem.flash.fragments.WebFragment
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.flashDownload
import nl.arnhem.flash.web.*


@Suppress("DEPRECATION")
class FlashWebView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : NestedWebView(context, attrs, defStyleAttr),
        FlashContentCore {

    override fun reload(animate: Boolean) {
        if (parent.registerTransition(false, animate))
            super.reload()
    }

    override lateinit var parent: FlashContentParent

    internal lateinit var flashWebClient: FlashWebViewClient

    override val currentUrl: String
        get() = url ?: ""

    @SuppressLint("SetJavaScriptEnabled")
    override fun bind(container: FlashContentContainer): View {
        if (parent.baseEnum != null || parent.baseUrl.shouldUseBasicAgent)
            userAgentString = USER_AGENT_BASIC // go through our own agent ref
        with(settings) {
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            textZoom = Prefs.webTextScaling
            javaScriptCanOpenWindowsAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
            isVerticalScrollBarEnabled = true
            setSupportZoom(true)
            displayZoomControls = false
            builtInZoomControls = true
            saveFormData = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        setLayerType(LAYER_TYPE_HARDWARE, null)
        // attempt to get custom client; otherwise fallback to original
        flashWebClient = (container as? WebFragment)?.client(this) ?: FlashWebViewClient(this)
        webViewClient = flashWebClient
        webChromeClient = FlashChromeClient(this)
        addJavascriptInterface(FlashJSI(this), "Flash")
        setBackgroundColor(Color.TRANSPARENT)
        setDownloadListener(context::flashDownload)
        return this
    }

    /**
     * Wrapper to the main userAgentString to cache it.
     * This decouples it from the UiThread
     *
     * Note that this defaults to null, but the main purpose is to
     * check if we've set our own agent.
     *
     * A null value may be interpreted as the default value
     */
    var userAgentString: String? = null
        set(value) {
            field = value
            settings.userAgentString = value
        }

    init {
        isNestedScrollingEnabled = true
    }

    fun loadUrl(url: String?, animate: Boolean) {
        if (url == null) return
        if (parent.registerTransition(this.url != url, animate))
            super.loadUrl(url)
    }

    override fun reloadBase(animate: Boolean) {
        loadUrl(parent.baseUrl, animate)
    }

    override fun onBackPressed(): Boolean {
        if (canGoBack()) {
            goBack()
            return true
        }
        return false
    }

    /**
     * If webview is already at the top, refresh
     * Otherwise scroll to top
     */
    override fun onTabClicked() {
        if (scrollY < 5) reloadBase(true)
        else scrollToTop()
    }

    private fun scrollToTop() {
        flingScroll(0, 0) // stop fling
        if (scrollY > 10000)
            scrollTo(0, 0)
        else
            smoothScrollTo(0)
    }

    private fun smoothScrollTo(y: Int) {
        ValueAnimator.ofInt(scrollY, y).apply {
            duration = Math.min(Math.abs(scrollY - y), 500).toLong()
            interpolator = AnimHolder.fastOutSlowInInterpolator(context)
            addUpdateListener { scrollY = it.animatedValue as Int }
            start()
        }
    }

    private fun smoothScrollBy(y: Int) = smoothScrollTo(Math.max(0, scrollY + y))

    //override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    //   when (event.keyCode) {
    //       KeyEvent.KEYCODE_VOLUME_UP -> smoothScrollBy(-height)
    //        KeyEvent.KEYCODE_VOLUME_DOWN -> smoothScrollBy(height)
    //        else -> return super.onKeyDown(keyCode, event)
    //    }
    //    return true
    //}

    override var active: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            if (field) onResume()
            else onPause()
        }

    override fun reloadTheme() {
        reloadThemeSelf()
    }

    override fun reloadThemeSelf() {
        reload(false) // todo see if there's a better solution
    }

    override fun reloadTextSize() {
        reloadTextSizeSelf()
    }

    override fun reloadTextSizeSelf() {
        settings.textZoom = Prefs.webTextScaling
    }

    override fun destroy() {
        val parent = getParent() as? ViewGroup
        if (parent != null) {
            parent.removeView(this)
            super.destroy()
        }
    }
}