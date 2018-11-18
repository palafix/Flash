@file:Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package nl.arnhem.flash.activities

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.CardView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.*
import android.widget.*
import ca.allanwang.kau.permissions.PERMISSION_ACCESS_FINE_LOCATION
import ca.allanwang.kau.permissions.kauRequestPermissions
import ca.allanwang.kau.searchview.SearchItem
import ca.allanwang.kau.searchview.SearchView
import ca.allanwang.kau.searchview.SearchViewHolder
import ca.allanwang.kau.searchview.bindSearchView
import ca.allanwang.kau.swipe.kauSwipeOnCreate
import ca.allanwang.kau.swipe.kauSwipeOnDestroy
import ca.allanwang.kau.utils.*
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import io.realm.OrderedRealmCollection
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.view_video.*
import nl.arnhem.flash.R
import nl.arnhem.flash.contracts.*
import nl.arnhem.flash.enums.OverlayContext
import nl.arnhem.flash.facebook.CHROMEWEB_LOAD_DELAY
import nl.arnhem.flash.facebook.FbItem
import nl.arnhem.flash.glide.FlashGlide
import nl.arnhem.flash.injectors.*
import nl.arnhem.flash.model.BookmarkModel
import nl.arnhem.flash.utils.*
import nl.arnhem.flash.utils.Prefs.animate
import nl.arnhem.flash.utils.iab.IS_Flash_PRO
import nl.arnhem.flash.glide.GlideApp
import nl.arnhem.flash.views.FlashVideoViewer
import nl.arnhem.flash.web.CustomChromeClient
import nl.arnhem.flash.web.WebViewFlash
import nl.arnhem.flash.web.shouldFlashInterceptRequest
import org.jetbrains.anko.withAlpha
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import kotlin.properties.Delegates


@Suppress("OverridingDeprecatedMember", "DEPRECATION", "NAME_SHADOWING")
class CustomTabs : BaseActivity(),
        SearchViewHolder, VideoViewHolder,
        ActivityContract, FileChooserContract by FileChooserDelegate() {

    override val frameWrapper: FrameLayout by bindView(R.id.frame_wrapper)
    val swipeRefreshLayout: SwipeRefreshLayout by bindView(R.id.swipe)
    val coordinator: CoordinatorLayout by bindView(R.id.overlay_main_content)
    val toolbar: Toolbar by bindView(R.id.browser_toolbar)
    private val appBar: AppBarLayout by bindView(R.id.appbaroverlay)
    val secure: ImageView by bindView(R.id.lockButton)
    val unsecure: ImageView by bindView(R.id.unlockButton)
    private val toolbarTitle: TextView by bindView(R.id.toolbarTitle)
    private val toolbarSub: TextView by bindView(R.id.toolbarSub)
    val copy: TextView by bindView(R.id.simple_copy_link)
    val share: TextView by bindView(R.id.action_share)
    val download: TextView by bindView(R.id.action_download)
    private val bookmark: TextView by bindView(R.id.action_bookmark)
    val open: TextView by bindView(R.id.simple_open_link)
    val progressBar: ProgressBar by bindView(R.id.content_progress)
    private val goForward: ImageView by bindView(R.id.simple_go_forward)
    private val addBookMark: ImageView by bindView(R.id.addbookmark)
    val browserrefresh: ImageView by bindView(R.id.simple_refresh)
    val browserstop: ImageView by bindView(R.id.simple_stop)
    val info: ImageView by bindView(R.id.simple_info)
    val overflowMenu: CardView by bindView(R.id.main_menu)
    private val menuScroll: ScrollView by bindView(R.id.scroller)
    private val menuHolder: FrameLayout by bindView(R.id.menu_holder)
    val webView: WebViewFlash by bindView(R.id.webview)

    override var searchView: SearchView? = null
    private val searchViewCache = mutableMapOf<String, List<SearchItem>>()

    private var mFactory: XmlPullParserFactory? = null
    private var mXpp: XmlPullParser? = null

    private var appDirectoryName: String? = null
    private var appDirectoryName2: String? = null
    private var appDirectoryName3: String? = null
    private var appDirectoryName4: String? = null

    private val overlayContext: OverlayContext?
        get() = OverlayContext[intent.extras]
    var realm: Realm by Delegates.notNull()

    private lateinit var addRess: String

    private var flag = false

    private inline fun v(crossinline message: () -> Any?) = L.v { "web client: ${message()}" }

    private val glide
        get() = GlideApp.with(this)

    @SuppressLint("ClickableViewAccessibility", "SetJavaScriptEnabled", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)
        itemsOverFlowOnclickMenu()
        Realm.init(this)
        progressBar.max = 100
        progressBar.progress = 1
        progressBar.tint(Prefs.textColor.withAlpha(180))
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        toolbar.setTitleTextColor(Prefs.iconColor)
        toolbarTitle.setTextColor(Prefs.iconColor)
        toolbarSub.setTextColor(Prefs.iconColor)
        toolbar.navigationIcon = GoogleMaterial.Icon.gmd_close.toDrawable(this, 16, Prefs.iconColor)
        toolbar.setNavigationOnClickListener { finishSlideOut() }
        coordinator.setBackgroundColor(Prefs.bgColor)
        realm = Realm.getDefaultInstance()
        overflowMenu.setCardBackgroundColor(Prefs.headerColor.withAlpha(255))
        browserrefresh.setColorFilter(Prefs.iconColor)
        browserstop.setColorFilter(Prefs.iconColor.withAlpha(255).lighten())
        info.setColorFilter(Prefs.iconColor)
        goForward.setColorFilter(Prefs.iconColor)
        addBookMark.setColorFilter(Prefs.iconColor)
        share.setTextColor(Prefs.iconColor)
        copy.setTextColor(Prefs.iconColor)
        open.setTextColor(Prefs.iconColor)
        download.setTextColor(Prefs.iconColor)
        bookmark.setTextColor(Prefs.iconColor)
        appDirectoryName = getString(R.string.flash_name).replace(" ", " ")
        appDirectoryName2 = getString(R.string.flash_name_image).replace(" ", " ")
        appDirectoryName3 = getString(R.string.flash_name_docu).replace(" ", " ")
        appDirectoryName4 = getString(R.string.flash_name_video).replace(" ", " ")
        if (Prefs.DayNight && isNightTime(Activity())) {
            setFlashDayNightColors {
                toolbar(toolbar)
                header(appBar)
                themeWindow = false
            }
        } else {
            setFlashColors {
                toolbar(toolbar)
                header(appBar)
                themeWindow = false
            }
        }
        if (Showcase.firstWebOverlay) {
            coordinator.flashSnackbar(getString(R.string.web_overlay_swipe_hint)) {
                duration = Snackbar.LENGTH_INDEFINITE
                setAction(R.string.got_it) { _ -> this.dismiss() }
            }
        }
        kauSwipeOnCreate {
            if (!Prefs.overlayFullScreenSwipe)
                edgeSize = 20.dpToPx
            sensitivity = 0.5f
            setEdgeSizePercent(0.2f)//0.2 mean left 20% of screen can touch to begin swipe.
            scrimColor = Prefs.iconColor
            transitionSystemBars = true
        }

        val browserIntent = Intent("android.intent.action.VIEW", Uri.parse("http://"))
        val ri = packageManager.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val pn = ri.loadLabel(packageManager).toString()
        if (pn.contains("Android")) {
            open.text = resources.getString(R.string.open_link) + "..."
        } else {
            open.text = resources.getString(R.string.open_link) + " " + pn
        }

        AdRemoval.init(this@CustomTabs)
        val url = intent.data
        addRess = "$url"

        swipeRefreshLayout.setColorSchemeColors(Prefs.iconColor)
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(Prefs.headerColor.withAlpha(255))
        swipeRefreshLayout.setOnRefreshListener {
            try {
                webView.reload()
                if (applicationContext.isNetworkAvailable)
                    swipeRefreshLayout.isRefreshing = false
                else {
                    Handler().postDelayed({ swipeRefreshLayout.isRefreshing = false }, 3000)
                }
            } catch (ignored: Exception) {

            }
        }

        with(webView.settings) {
            javaScriptEnabled = true
            textZoom = Prefs.webTextScaling
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            javaScriptCanOpenWindowsAutomatically = true
            allowFileAccess = true
            setAppCacheEnabled(true)
            domStorageEnabled = true
            setSupportZoom(true)
            displayZoomControls = false
            builtInZoomControls = true
            saveFormData = true
            useWideViewPort = true
            loadWithOverviewMode = true
            webView.isVerticalScrollBarEnabled = false
            webView.isFocusable = true
            pluginState = WebSettings.PluginState.ON_DEMAND
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.setAcceptThirdPartyCookies(webView, true)
            }
        }
        try {
            webView.loadUrl(addRess)
        } catch (ignored: Exception) {

        }

        webView.setOnTouchListener { _, _ ->
            overflowMenu.visibility = View.INVISIBLE
            false
        }

        webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? = view.shouldFlashInterceptRequest(request)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                v { "Url loading: ${request.url}" }
                val path = request.url?.path ?: return true
                v { "Url path $path" }
                val url = request.url.toString()
                    if (url.startsWith("market:")
                            || url.startsWith("https://play.google.com") || url.startsWith("magnet:")
                            || url.startsWith("mailto:") || url.startsWith("intent:")
                            || url.startsWith("https://mail.google.com") || url.startsWith("https://plus.google.com")
                            || url.startsWith("geo:") || url.startsWith("https://mega.nz/") || url.startsWith("google.streetview:")) {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        finish()
                        return true
                    }
                    if (url.contains("http://") || url.contains("https://")) {
                        return true
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    try {
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Log.e("shouldOverrideUrlLoad", "No Activity to handle action", e)
                        e.printStackTrace()
                    }
                return true
            }

            override fun onLoadResource(view: WebView, url: String) {
                if (url.contains(".mp4") || url.contains(".webm") || url.contains(".avi") || url.contains(".mkv")
                        || url.contains(".mpg") || url.contains(".flv") || url.contains(".swv") || url.contains(".rm")
                        || url.contains(".wmv") || url.contains(".mov") || url.contains(".m4v")) {
                    download.visibility = View.VISIBLE
                } else
                    super.onLoadResource(view, url)
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                try {
                    viewGone(view)
                    AdRemoval.init(this@CustomTabs)
                    swipeRefreshLayout.isRefreshing = true
                    swipeRefreshLayout.isEnabled = true
                    //Handler().postDelayed({ swipeRefreshLayout.isRefreshing = false }, 3000)
                    if (webView.canGoForward()) {
                        goForward.setImageDrawable(resources.getDrawable(R.drawable.ic_go_forward))
                    } else {
                        goForward.setImageDrawable(resources.getDrawable(R.drawable.ic_go_forward_light))
                    }
                    (findViewById<View>(R.id.toolbarSub) as TextView).text = url
                    if (url != null) {
                        if (url.contains("https://")) {
                            secure.setIcon(GoogleMaterial.Icon.gmd_lock, 16, Color.GREEN)
                            secure.visibility = View.VISIBLE
                            unsecure.visibility = View.GONE
                        } else {
                            unsecure.setIcon(GoogleMaterial.Icon.gmd_lock_open, 16, Color.RED)
                            secure.visibility = View.GONE
                            unsecure.visibility = View.VISIBLE
                        }
                    }
                    browserstop.visibility = View.VISIBLE
                    if (browserrefresh.visibility == View.VISIBLE) {
                        browserrefresh.visibility = View.GONE
                    }
                } catch (ignored: NullPointerException) {
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                url ?: return
                viewVisible(view)
                v { "finished $url" }
                try {
                    AdRemoval.init(this@CustomTabs)
                    swipeRefreshLayout.isRefreshing = false
                    browserstop.visibility = View.GONE
                } catch (ignored: Exception) {

                }
                browserrefresh.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                if (!url.isFacebookUrl) {
                    swipeRefreshLayout.isRefreshing = false
                    return
                }
                onPageFinishedActions(url)
            }

            private fun injectBackgroundColor() {
                webView.setBackgroundColor(
                        when {
                            webView.url.isFacebookUrl -> Prefs.bgColor.withAlpha(255)
                            else -> Color.WHITE
                        }
                )
            }

            override fun onPageCommitVisible(view: WebView, url: String?) {
                super.onPageCommitVisible(view, url)
                injectBackgroundColor()
                if (webView.url.isFacebookUrl) {
                    view.jsInject(
                            CssAssets.ROUND_ICONS.maybe(Prefs.showRoundedIcons),
                            CssHider.CORE,
                            CssHider.COMPOSER.maybe(!Prefs.showComposer),
                            CssFixed.COMPOSER_FIXED.maybe(Prefs.fixedComposer),
                            CssPadding.COMPOSER_PADDING.maybe(Prefs.paddingComposer),
                            CssHider.COMPOSER_BOTTOM.maybe(Prefs.bottomComposer),
                            CssHider.PEOPLE_YOU_MAY_KNOW.maybe(!Prefs.showSuggestedFriends && IS_Flash_PRO),
                            CssHider.SUGGESTED_GROUPS.maybe(!Prefs.showSuggestedGroups && IS_Flash_PRO),
                            Prefs.themeInjector,
                            CssHider.NON_RECENT.maybe(webView.url.contains("?sk=h_chr")
                                    && Prefs.aggressiveRecents),
                            CssHider.VIDEOS,
                            CssHider.VIDEOS2,
                            CssHider.HEADER,
                            CssHider.HEADER_TOP,
                            JsAssets.DOCUMENT_WATCHER,
                            JsAssets.HEADER_HIDER,
                            JsAssets.CLICK_A,
                            CssHider.ADS.maybe(!Prefs.showFacebookAds && IS_Flash_PRO),
                            JsAssets.CONTEXT_A,
                            JsActions.AUDIO_OFF.maybe(Prefs.DisableAudio),
                            JsAssets.MEDIA)
                } else
                    swipeRefreshLayout.isRefreshing = false
            }


            private fun onPageFinishedActions(url: String) {
                if (url.startsWith("${FbItem.MESSAGES.url}/read/") && Prefs.messageScrollToBottom)
                    webView.pageDown(true)
                injectAndFinish()
            }

            private fun injectAndFinish() {
                v { "page finished reveal" }
                swipeRefreshLayout.isRefreshing = false
                injectBackgroundColor()
                webView.jsInject(
                        JsActions.LOGIN_CHECK,
                        JsAssets.TEXTAREA_LISTENER)
            }


            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                if (view.url == failingUrl) {
                    materialDialogThemed {
                        title(webView.title)
                        content(resources.getString(R.string.no_network))
                        positiveText(R.string.kau_ok)
                        onPositive { _, _ ->
                            if (webView.canGoBack()) {
                                webView.reload()
                                webView.goBack()
                            }
                        }
                    }
                }
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.d("Browser : ", "Failed to load page")
            }

        }

        webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            flashChromeDownload(url, contentDisposition, mimeType)
            flashSnackbar(string(R.string.downloading))
        }

        webView.webChromeClient = object : CustomChromeClient(this) {
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                L.i { "Requesting geolocation" }
                kauRequestPermissions(PERMISSION_ACCESS_FINE_LOCATION) { granted, _ ->
                    L.i { "Geolocation response received; ${if (granted) "granted" else "denied"}" }
                    callback(origin, granted, true)
                }
            }

            override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>?>, fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
                openFileChooser(filePathCallback, fileChooserParams)
                return true
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 125) {
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                } else {
                    progressBar.visibility = View.VISIBLE
                }
            }

            override fun onReceivedTitle(view: WebView, title: String) {
                super.onReceivedTitle(view, title)
                try {
                    toolbarTitle.text = title
                    texFade()
                } catch (ignored: Exception) {

                }
            }
        }
    }

    override fun openFileChooser(filePathCallback: ValueCallback<Array<Uri>?>, fileChooserParams: WebChromeClient.FileChooserParams) {
        openMediaPicker(filePathCallback, fileChooserParams)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (onActivityResultWeb(requestCode, resultCode, data)) return
    }

    private fun viewVisible(view: WebView) {
        if (animate && Prefs.animate) {
            val cx = (view.left) / 2
            val cy = (view.top) / 2
            val finalRadius = Math.max(view.height, view.width)
            val anim = ViewAnimationUtils
                    .createCircularReveal(view, cx, cy, 0.toFloat(), finalRadius.toFloat())
            L.e {
                ("Reveal animation params ["
                        + view.height
                        + " " + view.width
                        + ", 0, " + finalRadius + "]")
            }

            anim.duration = (CHROMEWEB_LOAD_DELAY)
            anim.start()
            view.visibility = View.VISIBLE
        } else view.visibility = View.VISIBLE
    }

    private fun viewGone(view: WebView) {
        view.visibility = View.GONE
    }


    private fun searchChrome(vararg arg0: String): List<String> {
        if (!isNetworkConnected(this@CustomTabs)) {
            return ArrayList()
        }
        val filter = ArrayList<String>()
        var query = arg0[0]
        try {
            query = query.replace(" ", " ")
            URLEncoder.encode(query, ENCODING)
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        var download: InputStream? = null
        try {
            download = URL("http://google.com/complete/search?q=" + query + "&output=toolbar&hl=en").openStream()
            if (mFactory == null) {
                mFactory = XmlPullParserFactory.newInstance()
                mFactory!!.isNamespaceAware = true
            }
            if (mXpp == null) {
                mXpp = mFactory!!.newPullParser()
            }
            mXpp!!.setInput(download, ENCODING)
            var eventType = mXpp!!.eventType
            var counter = 0
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("suggestion" == mXpp!!.name) {
                        val suggestion = mXpp!!.getAttributeValue(null, "data")
                        filter.add(suggestion).toString()
                        counter++
                        if (counter >= 6) {
                            break
                        }
                    }
                }
                eventType = mXpp!!.next()
            }

        } catch (e: FileNotFoundException) {
        } catch (e: MalformedURLException) {
        } catch (e: IOException) {
        } catch (e: XmlPullParserException) {
        } finally {
            if (download != null) {
                try {
                    download.close()
                } catch (e: IOException) {
                }
            }
        }
        return filter
    }


    private fun isNetworkConnected(context: Context): Boolean {
        val networkInfo = getActiveNetworkInfo(context)
        return networkInfo != null && networkInfo.isConnected
    }

    private fun getActiveNetworkInfo(context: Context): NetworkInfo? {
        val connectivity = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivity.activeNetworkInfo
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.browser_menu, menu)
        overlayContext?.onMenuCreate(this, menu)
        toolbar.tint(Prefs.iconColor)
        setMenuIcons(menu, Prefs.iconColor,
                R.id.simple_overflow to GoogleMaterial.Icon.gmd_more_vert,
                R.id.custom_search to GoogleMaterial.Icon.gmd_search)
        searchViewBindIfNull {
            bindSearchView(menu, R.id.custom_search, Prefs.iconColor) {
                textCallback = { query, searchView ->
                    val items = searchChrome(query).asSequence().filter { it.contains(query) }.sorted().map { SearchItem(it) }.toMutableList()
                    if (items.isEmpty()) {
                        items.add(SearchItem(string(R.string.no_suggestions), iicon = GoogleMaterial.Icon.gmd_error))
                    } else (items.isNotEmpty())
                    items.add(SearchItem("https://www.google.com/search?q=$query", string(R.string.show_all_results), iicon = null))
                    searchView.results = items
                }
                hintText = string(R.string.google_search)
                textDebounceInterval = 300
                searchCallback = { url, _ ->
                    if (url.endsWith(".com")
                            || url.endsWith(".as")
                            || url.endsWith(".uk")
                            || url.endsWith(".biz")
                            || url.endsWith(".nl")
                            || url.endsWith(".de")
                            || url.endsWith(".it")
                            || url.endsWith(".xyz")
                            || url.endsWith(".org")
                            || url.endsWith(".fr")
                            || url.endsWith(".en")) {
                        if (!url.startsWith("http://")
                                && !url.startsWith("https://")) {
                            webView.loadUrl("http://$url")
                        }
                    } else
                        webView.loadUrl("https://www.google.com/search?q=$url")
                    ; true
                }
                shouldClearOnClose = true
                withDivider = true
                closeListener = { _ -> searchViewCache.clear() }
                foregroundColor = Prefs.textColor
                backgroundColor = Prefs.bgColor.withMinAlpha(200)
                onItemClick = { _, key, _, _ ->
                    webView.loadUrl("https://www.google.com/search?q=$key")
                    searchView!!.revealClose()
                }
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.simple_overflow -> {
                try {
                    showMenu()
                } catch (ignored: Exception) {
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Find the Bookmark
     */
    fun getBookmark(realm: Realm): OrderedRealmCollection<BookmarkModel> {
        return realm.where(BookmarkModel::class.java).findAll()
    }

    public override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        try {
            if (overflowMenu.visibility == View.VISIBLE) {
                hideMenu()
            }
        } catch (ignored: Exception) {

        }

    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterForContextMenu(webView)
            webView.onPause()
            webView.pauseTimers()
        } catch (ignored: Exception) {
        }
    }

    private fun hideMenu() {
        val fade = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        fade.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                overflowMenu.visibility = View.GONE
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
        })
        overflowMenu.startAnimation(fade)
        menuHolder.isClickable = false
        menuHolder.isFocusable = false
        menuHolder.isSoundEffectsEnabled = false
    }

    private fun showMenu() {
        menuScroll.scrollY = 0
        val grow = AnimationUtils.loadAnimation(this, R.anim.kau_slide_in_top)
        val `in` = AnimationUtils.loadAnimation(this, R.anim.kau_slide_out_top)
        grow.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                overflowMenu.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
        })
        `in`.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                overflowMenu.visibility = View.GONE
            }
            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
        })
        overflowMenu.startAnimation(grow)
        copy.startAnimation(grow)
        share.startAnimation(grow)
        download.startAnimation(grow)
        bookmark.startAnimation(grow)
        open.startAnimation(grow)
        menuHolder.isClickable = true
        menuHolder.isFocusable = true
        overflowMenu.isSoundEffectsEnabled = false
        menuHolder.isSoundEffectsEnabled = false
        val bookmarkk = realm.where(BookmarkModel::class.java).findAll()
        for (model in bookmarkk) {
            if (model.bookMark == webView.url.toString() || model.title == webView.title.toString()) {
                addBookMark.setImageDrawable(resources.getDrawable(R.drawable.ic_bookmark))
                flag = true
                break
            }
            addBookMark.setImageDrawable(resources.getDrawable(R.drawable.ic_bookmark_border))
            flag = false
        }
    }

    override fun backConsumer(): Boolean {
        if (overflowMenu.visibility == View.VISIBLE) {
            hideMenu()
            return true
        }
        if (Prefs.backToTop) {
            if (webView.scrollY > 5) {
                scrollToTop()
                return true
            }
        }
        if (webView.canGoBack()) {
            webView.goBack()
            return true
        } else (finishSlideOut())
        return true
    }

    private fun scrollToTop() {
        webView.flingScroll(0, 0) // stop fling
        if (webView.scrollY > 10000)
            webView.scrollTo(0, 0)
        else
            smoothScrollTo(0)
    }

    private fun smoothScrollTo(y: Int) {
        ValueAnimator.ofInt(webView.scrollY, y).apply {
            duration = Math.min(Math.abs(webView.scrollY - y), 500).toLong()
            interpolator = AnimHolder.fastOutSlowInInterpolator(webView.context)
            addUpdateListener { webView.scrollY = it.animatedValue as Int }
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            webView.onResume()
            webView.resumeTimers()
            registerForContextMenu(webView)
        } catch (ignored: Exception) {

        }
    }

    override fun onStart() {
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.removeAllViews()
        webView.destroy()
        kauSwipeOnDestroy()
    }

    override fun onCreateContextMenu(menu: ContextMenu?, view: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, view, menuInfo)
        val result = webView.hitTestResult
        if (result != null) {
            val type = result.type
            if (type == WebView.HitTestResult.IMAGE_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                showLongPressedImageMenu(result.extra)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val result = webView.hitTestResult.extra
            when (item.itemId) {
                R.id.ID_CONTEXT_MENU_SAVE_IMAGE -> {
                    if (URLUtil.isValidUrl(result)) {
                        flashContextDownload(mPendingImageUrlToSave!!)
                        flashSnackbar(R.string.downloading)
                    }
                }
                R.id.ID_CONTEXT_MENU_SHARE_IMAGE -> {
                    try {
                        shareText(mPendingImageUrlToSave)
                        flashSnackbar(R.string.share_image)
                    } catch (e: Exception) {
                        e.logFlashAnswers("Image share failed")
                        flashSnackbar(R.string.image_share_failed)
                    }
                }

                R.id.ID_CONTEXT_MENU_COPY_IMAGE -> {
                    copyToClipboard(webView.url)
                    flashSnackbar(R.string.copy_link)
                }
            }
        return super.onContextItemSelected(item)
    }

    @SuppressLint("InflateParams")
    private fun showLongPressedImageMenu(url: String) {
        val result = webView.hitTestResult.extra
        mPendingImageUrlToSave = url
        val view = layoutInflater.inflate(R.layout.textview_context_menu, null)
        val content = view.findViewById(R.id.context_window) as RelativeLayout
        val titelImage = view.findViewById(R.id.context_image) as ImageView
        val imageMENUDownloader = view.findViewById(R.id.ID_CONTEXT_MENU_SAVE_IMAGE) as TextView
        val imageMENUShare = view.findViewById(R.id.ID_CONTEXT_MENU_SHARE_IMAGE) as TextView
        val imageMENUCopy = view.findViewById(R.id.ID_CONTEXT_MENU_COPY_IMAGE) as TextView
        val imageSAVE = view.findViewById(R.id.ID_CONTEXT_SAVE_IMAGE) as ImageView
        val imageSHARE = view.findViewById(R.id.ID_CONTEXT_SHARE_IMAGE) as ImageView
        val imageCOPY = view.findViewById(R.id.ID_CONTEXT_COPY_IMAGE) as ImageView
        val mBottomSheetDialog = Dialog(this, R.style.MaterialDialogSheet)

        content.setBackgroundColor(Prefs.bgColor)
        val glide = glide
        glide.load(mPendingImageUrlToSave)
                .transform(FlashGlide.customRoundCorner)
                .into(titelImage)
        imageMENUDownloader.setTextColor(Prefs.textColor.withAlpha(200).lighten())
        imageMENUShare.setTextColor(Prefs.textColor.withAlpha(200).lighten())
        imageMENUCopy.setTextColor(Prefs.textColor.withAlpha(200).lighten())
        imageSAVE.setIcon(GoogleMaterial.Icon.gmd_file_download, 16, Prefs.accentColor)
        imageSHARE.setIcon(GoogleMaterial.Icon.gmd_share, 16, Prefs.accentColor)
        imageCOPY.setIcon(GoogleMaterial.Icon.gmd_content_copy, 16, Prefs.accentColor)

        mBottomSheetDialog.setContentView(view)
        mBottomSheetDialog.window.setLayout(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        mBottomSheetDialog.window.setGravity(Gravity.BOTTOM)

        imageMENUDownloader.setOnClickListener {
            if (URLUtil.isValidUrl(result)) {
                flashContextDownload(mPendingImageUrlToSave!!)
                flashSnackbar(R.string.downloading)
            }
            mBottomSheetDialog.dismiss()
        }

        imageMENUShare.setOnClickListener {
            try {
                shareText(mPendingImageUrlToSave)
                flashSnackbar(R.string.share_image)
            } catch (e: Exception) {
                e.logFlashAnswers("Image share failed")
                flashSnackbar(R.string.image_share_failed)
            }
            mBottomSheetDialog.dismiss()
        }
        imageMENUCopy.setOnClickListener {
            copyToClipboard(webView.url, "",  false)
            flashSnackbar(R.string.copy_link)
            mBottomSheetDialog.dismiss()
        }
        mBottomSheetDialog.show()
        mBottomSheetDialog.setOnCancelListener{
            mBottomSheetDialog.dismiss()
        }
    }

    private fun itemsOverFlowOnclickMenu() {
        menuHolder.setOnClickListener(onClickListener)
        menuHolder.isClickable = false
        menuHolder.isFocusable = false
        goForward.setOnClickListener(onClickListener)
        addBookMark.setOnClickListener(onClickListener)
        info.setOnClickListener(onClickListener)
        browserstop.setOnClickListener(onClickListener)
        browserrefresh.setOnClickListener(onClickListener)
        copy.setOnClickListener(onClickListener)
        share.setOnClickListener(onClickListener)
        download.setOnClickListener(onClickListener)
        bookmark.setOnClickListener(onClickListener)
        open.setOnClickListener(onClickListener)
    }

    private fun texFade() {
        val grow = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        grow.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
        })
        toolbarTitle.startAnimation(grow)
    }

    /**
     * ENCODING
     */
    companion object {
        //private const val ID_CONTEXT_MENU_SAVE_IMAGE = 2562617
        //private const val ID_CONTEXT_MENU_SHARE_IMAGE = 2562618
        //private const val ID_CONTEXT_MENU_COPY_IMAGE = 2562619
        private const val ENCODING = "UTF-8"
    }

    /**
     * Overflowmenu onClickListeners
     */
    private val onClickListener = View.OnClickListener { v ->
        when (v.id) {
            R.id.menu_holder -> {
                hideMenu()
                return@OnClickListener
            }
            R.id.simple_go_forward -> {
                if (webView.canGoForward()) {
                    goForward.setImageDrawable(resources.getDrawable(R.drawable.ic_go_forward))
                    webView.goForward()
                    hideMenu()
                } else {
                    goForward.setImageDrawable(resources.getDrawable(R.drawable.ic_go_forward_light))
                }
                return@OnClickListener
            }

            R.id.addbookmark -> {
                hideMenu()
                realm.executeTransaction {
                    if (!flag) {
                        addBookMark.setImageDrawable(resources.getDrawable(R.drawable.ic_bookmark))
                        val bookmark = realm.createObject(BookmarkModel::class.java)
                        bookmark.bookMark = webView.url.toString()
                        bookmark.title = webView.title.toString()
                        flashSnackbar(getString(R.string.added) + webView.title + getString(R.string.added_string)) {
                            duration = Snackbar.LENGTH_LONG
                        }
                    } else {
                        addBookMark.setImageDrawable(resources.getDrawable(R.drawable.ic_bookmark_border))
                        val results: RealmResults<BookmarkModel> = realm.where(BookmarkModel::class.java).equalTo("bookMark", webView.url.toString()).findAll()
                        results.deleteAllFromRealm()
                    }
                }
                return@OnClickListener
            }

            R.id.simple_info -> {
                hideMenu()
                materialDialogThemed {
                    title(webView.title)
                    if (webView.url.contains("https://")) {
                        content(resources.getString(R.string.private_info))
                    } else {
                        content(resources.getString(R.string.none_private_info))
                    }
                    positiveText(R.string.kau_ok)
                    onPositive { _, _ -> }
                }
                return@OnClickListener
            }
            R.id.simple_refresh -> {
                webView.reload()
                return@OnClickListener
            }

            R.id.action_bookmark -> {
                hideMenu()
                launchBookMarkOverlay()
                return@OnClickListener
            }

            R.id.action_download -> {
                hideMenu()
                if (webView.url.contains("mp4")
                        || webView.url.contains("webm")
                        || webView.url.contains("avi")
                        || webView.url.contains("mkv")
                        || webView.url.contains("mpg")
                        || webView.url.contains("flv")
                        || webView.url.contains("swv")
                        || webView.url.contains("rm")
                        || webView.url.contains("wmv")
                        || webView.url.contains("mov")) {
                    flashDownload(video.videoUri)
                    download.visibility = View.VISIBLE
                } else download.visibility = View.GONE
                return@OnClickListener
            }
            R.id.action_share -> {
                hideMenu()
                shareText(webView.url)
                return@OnClickListener
            }

            R.id.simple_copy_link -> {
                hideMenu()
                copyToClipboard(webView.url)
                return@OnClickListener
            }

            R.id.simple_open_link -> {
                hideMenu()
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(webView.url)
                startActivity(intent)
                return@OnClickListener
            }

            R.id.simple_stop -> {
                webView.stopLoading()
                return@OnClickListener
            }

            else -> {
                hideMenu()
                return@OnClickListener
            }
        }
    }

    /**
     * ----------------------------------------------------
     * Video Contract
     * ----------------------------------------------------
     */
    override var videoViewer: FlashVideoViewer? = null
    override val lowerVideoPadding: PointF = PointF(0f, 0f)

}