@file:Suppress("KDocUnresolvedReference")

package nl.arnhem.flash.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import ca.allanwang.kau.swipe.kauSwipeOnCreate
import ca.allanwang.kau.swipe.kauSwipeOnDestroy
import ca.allanwang.kau.utils.*
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import nl.arnhem.flash.R
import nl.arnhem.flash.contracts.*
import nl.arnhem.flash.enums.OverlayContext
import nl.arnhem.flash.facebook.*
import nl.arnhem.flash.services.FlashRunnable
import nl.arnhem.flash.utils.*
import nl.arnhem.flash.views.FlashContentWeb
import nl.arnhem.flash.views.FlashVideoViewer
import nl.arnhem.flash.views.FlashWebView
import okhttp3.HttpUrl

/**
 * Created by Allan Wang on 2017-06-01.
 *
 * Collection of overlay activities for Flash
 *
 * Each one is largely the same layout, but is separated so they may run is separate single tasks
 * All overlays support user switches
 */

/**
 * Used by notifications. Unlike the other overlays, this runs as a singleInstance
 * Going back will bring you back to the previous app
 */
class FlashWebActivity : WebOverlayActivityBase(false) {

    override fun onCreate(savedInstanceState: Bundle?) {
        val requiresAction = !parseActionSend()
        super.onCreate(savedInstanceState)
        if (requiresAction) {
            /*
             * Signifies that we need to let the user know of a bad url
             * We will subscribe to the load cycle once,
             * and pop a dialog giving the user the option to copy the shared text
             */
            var disposable: Disposable? = null
            disposable = content.refreshObservable.subscribe {
                disposable?.dispose()
                materialDialogThemed {
                    title(R.string.invalid_share_url)
                    content(R.string.invalid_share_url_desc)
                }
            }
        }
    }

    /**
     * Attempts to parse the action url
     * Returns [true] if no action exists or if the action has been consumed, [false] if we need to notify the user of a bad action
     */
    private fun parseActionSend(): Boolean {
        if (intent.action != Intent.ACTION_SEND || intent.type != "text/plain") return true
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return true
        val url = HttpUrl.parse(text)?.toString()
        return if (url == null) {
            L.i { "Attempted to share a non-url" }
            L._i { "Shared text: $text" }
            copyToClipboard(text, "Text to Share", showToast = false)
            intent.putExtra(ARG_URL, FbItem.FEED.url)
            false
        } else {
            L.i { "Sharing url through overlay" }
            L._i { "Url: $url" }
            intent.putExtra(ARG_URL, "${FB_URL_BASE}sharer/sharer.php?u=$url")
            true
        }
    }

}

/**
 * Variant that forces a basic user agent. This is largely internal,
 * and is only necessary when we are launching from an existing [WebOverlayActivityBase]
 */
class WebOverlayBasicActivity : WebOverlayActivityBase(true)

/**
 * Internal overlay for the app; this is tied with the main task and is singleTop as opposed to singleInstance
 */
class WebOverlayActivity : WebOverlayActivityBase(false)


/**
 * The Overlay for external site's
 */
@Suppress("DEPRECATION")
@SuppressLint("Registered")
open class WebOverlayActivityBase(private val forceBasicAgent: Boolean) : BaseActivity(),
        ActivityContract, FlashContentContainer,
        VideoViewHolder, FileChooserContract by FileChooserDelegate() {

    override val frameWrapper: FrameLayout by bindView(R.id.frame_wrapper)
    val toolbar: Toolbar by bindView(R.id.overlay_toolbar)
    val content: FlashContentWeb by bindView(R.id.flash_content_web)
    val web: FlashWebView
        get() = content.coreView
    val coordinator: CoordinatorLayout by bindView(R.id.overlay_main_content)

    private inline val urlTest: String?
        get() = intent.getStringExtra(ARG_URL) ?: intent.dataString

    override val baseUrl: String
        get() = (intent.getStringExtra(ARG_URL) ?: intent.dataString).formattedFbUrl

    override val baseEnum: FbItem? = null

    private inline val userId: Long
        get() = intent?.getLongExtra(ARG_USER_ID, Prefs.userId) ?: Prefs.userId

    private val overlayContext: OverlayContext?
        get() = OverlayContext[intent.extras]

    override fun setTitle(title: String) {
        toolbar.title = title
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (urlTest == null) {
            L.e { "Empty link on web overlay" }
            toast(R.string.null_url_overlay)
            finish()
            return
        }
        setFrameContentView(R.layout.activity_web_overlay)
        setFlashTheme(true)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        statusBarColor = Prefs.headerColor.withAlpha(255).darken()
        navigationBarColor = Prefs.headerColor.withAlpha(255)
        toolbar.setBackgroundColor(Prefs.headerColor.withAlpha(255))
        coordinator.setBackgroundColor(Prefs.bgColor.withAlpha(255))
        toolbar.overflowIcon?.setTint(Prefs.iconColor)
        toolbar.setTitleTextColor(Prefs.iconColor)
        toolbar.navigationIcon = GoogleMaterial.Icon.gmd_close.toDrawable(this, 16, Prefs.iconColor)
        toolbar.setNavigationOnClickListener { finishSlideOut() }
        setFlashColors {
            toolbar(toolbar)
            themeWindow = false
        }

        content.bind(this)
        content.titleObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { toolbar.title = it }
        with(web) {
            if (forceBasicAgent) //todo check; the webview already adds it dynamically
                userAgentString = USER_AGENT_BASIC
            Prefs.prevId = Prefs.userId
            if (userId != Prefs.userId) FbCookie.switchUser(userId) { reloadBase(true) }
            else reloadBase(true)
            if (Showcase.firstWebOverlay) {
                coordinator.flashSnackbar(getString(R.string.web_overlay_swipe_hint)) {
                    duration = Snackbar.LENGTH_INDEFINITE
                    setAction(R.string.got_it) { _ -> this.dismiss() }
                }
            }
        }
        FlashRunnable.propagate(this, intent)
        L.v { "Done propagation" }
        kauSwipeOnCreate {
            if (!Prefs.overlayFullScreenSwipe) edgeSize = 20.dpToPx
            transitionSystemBars = false
        }
        with(web.settings) {
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = true
            textZoom = Prefs.webTextScaling
            javaScriptCanOpenWindowsAutomatically = true
            allowFileAccess = true
            web.isVerticalScrollBarEnabled = true
            setSupportZoom(true)
            displayZoomControls = false
            builtInZoomControls = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }
    }

    /**
     * Manage url loadings
     * This is usually only called when multiple listeners are added and inject the same url
     * We will avoid reloading if the url is the same
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        L.d { "New intent" }
        val newUrl = (intent.getStringExtra(ARG_URL) ?: intent.dataString)?.formattedFbUrl ?: return
        if (baseUrl != newUrl) {
            this.intent = intent
            content.baseUrl = newUrl
            web.reloadBase(true)
        }
    }

    override fun backConsumer(): Boolean {
        if (web.canGoBack()) {
            web.goBack()
            return true
        } else (finishSlideOut())
        return true
    }

    override fun onResume() {
        super.onResume()
        web.resumeTimers()
    }

    override fun onPause() {
        web.pauseTimers()
        L.v { "Pause overlay web timers" }
        super.onPause()
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
        kauSwipeOnDestroy()
    }

    override fun openFileChooser(filePathCallback: ValueCallback<Array<Uri>?>, fileChooserParams: WebChromeClient.FileChooserParams) {
        openMediaPicker(filePathCallback, fileChooserParams)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (onActivityResultWeb(requestCode, resultCode, data)) return
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_web, menu)
        overlayContext?.onMenuCreate(this, menu)
        toolbar.tint(Prefs.iconColor)
        setMenuIcons(menu, Prefs.iconColor,
                R.id.action_share to CommunityMaterial.Icon.cmd_share,
                R.id.action_copy_link to GoogleMaterial.Icon.gmd_content_copy)
        return true
    }

    /**
     * Toolbar WebOverlay
     */
    @SuppressLint("RestrictedApi")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_copy_link -> copyToClipboard(web.currentUrl)
            R.id.action_share -> shareText(web.currentUrl)
        }
        return false
    }

    /**
     * ----------------------------------------------------
     * Video Contract
     * ----------------------------------------------------
     */
    override var videoViewer: FlashVideoViewer? = null
    override val lowerVideoPadding: PointF = PointF(0f, 0f)
}






