@file:Suppress("KDocUnresolvedReference")

package nl.arnhem.flash.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import ca.allanwang.kau.permissions.PERMISSION_ACCESS_FINE_LOCATION
import ca.allanwang.kau.permissions.kauRequestPermissions
import ca.allanwang.kau.swipe.kauSwipeOnCreate
import ca.allanwang.kau.swipe.kauSwipeOnDestroy
import ca.allanwang.kau.utils.*
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_web_overlay.*
import nl.arnhem.flash.R
import nl.arnhem.flash.contracts.*
import nl.arnhem.flash.enums.OverlayContext
import nl.arnhem.flash.facebook.*
import nl.arnhem.flash.model.BookmarkModel
import nl.arnhem.flash.services.FlashRunnable
import nl.arnhem.flash.utils.*
import nl.arnhem.flash.views.FlashContentWeb
import nl.arnhem.flash.views.FlashVideoViewer
import nl.arnhem.flash.views.FlashWebView
import okhttp3.HttpUrl
import kotlin.properties.Delegates

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
        FlashPglAdBlock.init(this)
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

    var bundle: Bundle? = null
    var realm: Realm by Delegates.notNull()
    private lateinit var fullscreenView: View

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
        setSupportActionBar(toolbar)
        Realm.init(this)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon = GoogleMaterial.Icon.gmd_close.toDrawable(this, 16, Prefs.iconColor)
        toolbar.setNavigationOnClickListener { finishSlideOut() }
        FlashPglAdBlock.init(this)
        realm = Realm.getDefaultInstance()
        setFlashColors {
            toolbar(toolbar)
            themeWindow = false
        }
        coordinator.setBackgroundColor(Prefs.bgColor.withAlpha(255))
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
                coordinator.flashSnackbar(R.string.web_overlay_swipe_hint) {
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
            updateProgress()
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            textZoom = Prefs.webTextScaling
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
            javaScriptCanOpenWindowsAutomatically = true
            setGeolocationEnabled(true)
            allowFileAccess = true
            setAppCacheEnabled(true)
            domStorageEnabled = true
            databaseEnabled = true
            web.isVerticalScrollBarEnabled = true
            setSupportZoom(true)
            displayZoomControls = false
            builtInZoomControls = true
            saveFormData = true
            useWideViewPort = true
            loadWithOverviewMode = true
            pluginState = WebSettings.PluginState.ON_DEMAND
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            setRenderPriority(WebSettings.RenderPriority.HIGH)
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

    /**
     * Our theme for the overlay should be fully opaque
     */
    fun theme() {
        val opaqueAccent = Prefs.headerColor.withAlpha(255)
        statusBarColor = opaqueAccent.darken()
        navigationBarColor = opaqueAccent
        toolbar.setBackgroundColor(opaqueAccent)
        toolbar.setTitleTextColor(Prefs.iconColor)
        coordinator.setBackgroundColor(Prefs.bgColor.withAlpha(255))
        toolbar.overflowIcon?.setTint(Prefs.iconColor)
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
                R.id.action_copy_link to GoogleMaterial.Icon.gmd_content_copy,
                R.id.addbookmark to GoogleMaterial.Icon.gmd_bookmark_border)
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
            R.id.addbookmark -> {
                var flag = false
                val bookmarkk = realm.where(BookmarkModel::class.java).findAll()
                for (model in bookmarkk) {
                    if (model.bookMark == web.url.toString()) {
                        flag = true
                        break
                    }
                    flag = false
                }
                when (item.itemId) {
                    R.id.addbookmark -> {
                        realm.executeTransaction {
                            if (!flag) {
                                item.icon = getDrawable(R.drawable.ic_bookmark)
                                item.icon.setTint(Prefs.iconColor)
                                val bookmark = realm.createObject(BookmarkModel::class.java)
                                bookmark.bookMark = web.url.toString()
                                bookmark.title = web.title.toString()
                                Toast.makeText(baseContext, getString(R.string.added) + web.title + getString(R.string.added_string), Toast.LENGTH_LONG).show()
                            } else {
                                item.icon = getDrawable(R.drawable.ic_bookmark_border)
                                item.icon.setTint(Prefs.iconColor)
                                val results: RealmResults<BookmarkModel> = realm.where(BookmarkModel::class.java).equalTo("bookMark", web.url.toString()).findAll()
                                results.deleteAllFromRealm()
                            }
                        }
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Find the Bookmark
     */
    fun getBookmark(realm: Realm): OrderedRealmCollection<BookmarkModel> {
        return realm.where(BookmarkModel::class.java).findAll()
    }

    /**
     * ----------------------------------------------------
     * Video Contract
     * ----------------------------------------------------
     */
    override var videoViewer: FlashVideoViewer? = null
    override val lowerVideoPadding: PointF = PointF(0f, 0f)

    /**
     * Fullscreen Videos
     */
    private fun updateProgress() {
        web.webChromeClient = object : WebChromeClient() {
            val progress: Subject<Int> = web.parent.progressObservable
            val title: BehaviorSubject<String> = web.parent.titleObservable
            val activity = (web.context as? ActivityContract)
            val context = web.context!!
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                super.onShowCustomView(view, callback)
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN)
                if (view is FrameLayout) {
                    fullscreenView = view
                    fullscreenContainer.addView(fullscreenView)
                    fullscreenContainer.setBackgroundColor(Color.TRANSPARENT)
                    fullscreenContainer.visibility = View.VISIBLE
                    mainContainer.visibility = View.GONE
                }
            }

            override fun onHideCustomView() {
                super.onHideCustomView()
                fullscreenContainer.removeView(fullscreenView)
                fullscreenContainer.visibility = View.GONE
                mainContainer.setBackgroundColor(Color.TRANSPARENT)
                mainContainer.visibility = View.VISIBLE
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                L.v { "Chrome Console ${consoleMessage.lineNumber()}: ${consoleMessage.message()}" }
                return true
            }

            override fun onReceivedTitle(view: WebView, title: String) {
                super.onReceivedTitle(view, title)
                if (title.startsWith("http") || this.title.value == title) return
                this.title.onNext(title)
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progress.onNext(newProgress)
            }

            override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>?>, fileChooserParams: FileChooserParams): Boolean {
                activity?.openFileChooser(filePathCallback, fileChooserParams)
                        ?: webView.flashSnackbar(R.string.file_chooser_not_found)
                return activity != null
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                L.i { "Requesting geolocation" }
                context.kauRequestPermissions(PERMISSION_ACCESS_FINE_LOCATION) { granted, _ ->
                    L.i { "Geolocation response received; ${if (granted) "granted" else "denied"}" }
                    callback(origin, granted, true)
                }
            }
        }
    }
}





