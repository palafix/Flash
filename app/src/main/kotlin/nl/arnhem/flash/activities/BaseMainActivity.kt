package nl.arnhem.flash.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.*
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import ca.allanwang.kau.searchview.SearchItem
import ca.allanwang.kau.searchview.SearchView
import ca.allanwang.kau.searchview.SearchViewHolder
import ca.allanwang.kau.searchview.bindSearchView
import ca.allanwang.kau.utils.*
import co.zsmb.materialdrawerkt.builders.Builder
import co.zsmb.materialdrawerkt.builders.accountHeader
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.draweritems.badge
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import co.zsmb.materialdrawerkt.draweritems.divider
import co.zsmb.materialdrawerkt.draweritems.profile.profile
import co.zsmb.materialdrawerkt.draweritems.profile.profileSetting
import co.zsmb.materialdrawerkt.draweritems.sectionHeader
import co.zsmb.materialdrawerkt.draweritems.sectionItem
import com.crashlytics.android.answers.ContentViewEvent
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.materialdrawer.AccountHeader
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.holder.StringHolder
import io.realm.Realm
import nl.arnhem.flash.BuildConfig
import nl.arnhem.flash.R
import nl.arnhem.flash.contracts.FileChooserContract
import nl.arnhem.flash.contracts.FileChooserDelegate
import nl.arnhem.flash.contracts.MainActivityContract
import nl.arnhem.flash.contracts.VideoViewHolder
import nl.arnhem.flash.dbflow.TAB_COUNT
import nl.arnhem.flash.dbflow.loadFbCookie
import nl.arnhem.flash.dbflow.loadFbTabs
import nl.arnhem.flash.enums.MainActivityLayout
import nl.arnhem.flash.enums.Theme
import nl.arnhem.flash.facebook.FB_URL_BASE
import nl.arnhem.flash.facebook.FbCookie
import nl.arnhem.flash.facebook.FbItem
import nl.arnhem.flash.facebook.PROFILE_PICTURE_URL
import nl.arnhem.flash.fragments.BaseFragment
import nl.arnhem.flash.fragments.WebFragment
import nl.arnhem.flash.model.BookmarkModel
import nl.arnhem.flash.parsers.FlashSearch
import nl.arnhem.flash.parsers.SearchParser
import nl.arnhem.flash.utils.*
import nl.arnhem.flash.utils.Prefs.userId
import nl.arnhem.flash.utils.iab.FlashBilling
import nl.arnhem.flash.utils.iab.IS_Flash_PRO
import nl.arnhem.flash.utils.iab.IabMain
import nl.arnhem.flash.views.BadgedIcon
import nl.arnhem.flash.views.FlashVideoViewer
import nl.arnhem.flash.views.FlashViewPager
import org.jetbrains.anko.withAlpha
import java.util.*
import kotlin.collections.set
import kotlin.properties.Delegates
//import kotlinx.android.synthetic.main.activity_main.*
//import kotlinx.android.synthetic.main.view_main_toolbar.*
/**
 * Created by Allan Wang on 20/12/17.
 *
 * Most of the logic that is unrelated to handling fragments
 */

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")
abstract class BaseMainActivity : BaseActivity(), MainActivityContract,
        FileChooserContract by FileChooserDelegate(),
        VideoViewHolder, SearchViewHolder,
        FlashBilling by IabMain() {

    protected lateinit var adapter: SectionsPagerAdapter
    override val frameWrapper: FrameLayout by bindView(R.id.frame_wrapper)
    val toolbar: Toolbar by bindView(R.id.toolbar)
    val viewPager: FlashViewPager by bindView(R.id.container)
    val fab: FloatingActionButton by bindView(R.id.fab)
    val tabs: TabLayout by bindView(R.id.tabs)
    private val appBar: AppBarLayout by bindView(R.id.appbar)
    val coordinator: CoordinatorLayout by bindView(R.id.main_content)
    override var videoViewer: FlashVideoViewer? = null

    private lateinit var drawer: Drawer
    private lateinit var drawerHeader: AccountHeader
    private var lastAccessTime = -1L

    override var searchView: SearchView? = null
    private val searchViewCache = mutableMapOf<String, List<SearchItem>>()
    private var controlWebview: WebView? = null

    var realm: Realm by Delegates.notNull()

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Realm.init(this)
        realm = Realm.getDefaultInstance()
        val start = System.currentTimeMillis()
        setFrameContentView(Prefs.mainActivityLayout.layoutRes)
        if (Prefs.DayNight && isNightTime(Activity())) {
            setFlashDayNightColors {
                toolbar(toolbar)
                themeWindow = false
                header(appBar)
                background(viewPager)
            }
        } else {
            setFlashColors {
                toolbar(toolbar)
                themeWindow = false
                header(appBar)
                background(viewPager)
            }
        }

        setSupportActionBar(toolbar)

        val params = toolbar.layoutParams as AppBarLayout.LayoutParams
        params.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP

        adapter = SectionsPagerAdapter(loadFbTabs())
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = TAB_COUNT

        tabs.setBackgroundColor(Prefs.mainActivityLayout.backgroundColor())
        onNestedCreate(savedInstanceState)
        L.i { "Main finished loading UI in ${System.currentTimeMillis() - start} ms" }
        controlWebview = WebView(this)
        onCreateBilling()
        if (BuildConfig.VERSION_CODE > Prefs.versionCode) {
            Prefs.prevVersionCode = Prefs.versionCode
            Prefs.versionCode = BuildConfig.VERSION_CODE
            if (!BuildConfig.DEBUG) {
                flashChangelog()
                flashAnswersCustom("Version",
                        "Version code" to BuildConfig.VERSION_CODE,
                        "Prev version code" to Prefs.prevVersionCode,
                        "Version name" to BuildConfig.VERSION_NAME,
                        "Build type" to BuildConfig.BUILD_TYPE,
                        "Flash id" to Prefs.FlashId)
            }
        }
        setupDrawer(savedInstanceState)
        L.i { "Main started in ${System.currentTimeMillis() - start} ms" }

        initFab()
        lastAccessTime = System.currentTimeMillis()

        if (Prefs.AutoUpdate) {
            AppUpdater(this)
                    .setDisplay(Display.DIALOG)
                    .setTitleOnUpdateAvailable(R.string.found_update)
                    .setTitleOnUpdateNotAvailable(R.string.no_update)
                    .setContentOnUpdateNotAvailable(R.string.no_update_desc)
                    .setButtonUpdate(R.string.update_now)
                    .setButtonDismiss(R.string.cancel_update)
                    .setButtonDoNotShowAgain(R.string.no_show_update)
                    .setButtonDoNotShowAgainClickListener { _, _ ->
                        materialDialogThemed {
                            title(R.string.no_show_update)
                            content(R.string.no_Auto_update_desc)
                            positiveText(R.string.kau_yes)
                            negativeText(R.string.kau_no)
                            onPositive { _, _ -> Prefs.AutoUpdate = false }
                        }
                    }
                    .setUpdateFrom(UpdateFrom.JSON)
                    .setUpdateJSON("http://updatephase.palafix.nl/flash_updater.json")
                    .setIcon(R.drawable.flash_notify) // Notification icon
                    .showAppUpdated(false)
                    .start()
        }
    }
    /**
     * Injector to handle creation for sub classes
     */
    protected abstract fun onNestedCreate(savedInstanceState: Bundle?)

    private var hasFab = false
    private var shouldShow = false

    private fun initFab() {
        hasFab = false
        shouldShow = false
        fab.backgroundTintList = ColorStateList.valueOf(Prefs.headerColor.withMinAlpha(200))
        fab.hide()
        appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (!hasFab) return@OnOffsetChangedListener
            val percent = Math.abs(verticalOffset.toFloat() / appBarLayout.totalScrollRange)
            val shouldShow = percent < 0.2
            if (this.shouldShow != shouldShow) {
                this.shouldShow = shouldShow
                fab.showIf(shouldShow)
            }
        })
    }

    override fun showFab(iicon: IIcon, clickEvent: () -> Unit) {
        hasFab = true
        fab.setOnClickListener {
            clickEvent()
            //launchWebOverlay(FB_URL_BASE)
        }
        //fab.setOnClickListener {
            //clickEvent()
         //   launchSharerActivity("https://www.facebook.com/")
        //}
        if (shouldShow) {
            if (fab.isShown) {
                fab.fadeScaleTransition {
                    setIcon(iicon, 16, Prefs.iconColor)
                }
                return
            }
        }
        fab.setIcon(iicon, 16, Prefs.iconColor)
        fab.showIf(shouldShow)
    }

    override fun hideFab() {
        hasFab = false
        fab.setOnClickListener(null)
        fab.hide()
    }

    fun tabsForEachView(action: (position: Int, view: BadgedIcon) -> Unit) {
        (0 until tabs.tabCount).asSequence().forEach { i ->
            action(i, tabs.getTabAt(i)!!.customView as BadgedIcon)
        }
    }

    @SuppressLint("PrivateResource")
    private fun setupDrawer(savedInstanceState: Bundle?) {
        val navBg = if (Prefs.DayNight && isNightTime(Activity())) Color.BLACK.withAlpha(255).darken().toLong() else Prefs.bgColor.withAlpha(255).darken().toLong()
        val accent = if (Prefs.DayNight && isNightTime(Activity())) Color.LTGRAY.withAlpha(255).darken().toLong() else Prefs.accentColor.toLong()
        val text = if (Prefs.DayNight && isNightTime(Activity())) Color.WHITE.withAlpha(255).darken().toLong() else Prefs.textColor.toLong()
        val icon = if (Prefs.DayNight && isNightTime(Activity())) Color.WHITE.withAlpha(255).darken().toLong() else Prefs.iconColor.toLong()
        val noti = if (Prefs.DayNight && isNightTime(Activity())) Color.GRAY.withAlpha(255).darken().toLong() else Prefs.notiColor.toLong()
        drawer = drawer {
            toolbar = this@BaseMainActivity.toolbar
            savedInstance = savedInstanceState
            actionBarDrawerToggleAnimated = true
            translucentStatusBar = false
            sliderBackgroundColor = navBg
            drawerHeader = accountHeader {
                background = (R.drawable.flash_f_24)
                customViewRes = R.layout.material_drawer_header
                textColor = accent
                selectionSecondLineShown = false
                cookies().forEach { (id, name) ->
                    profile(name = name ?: "") {
                        iconUrl = PROFILE_PICTURE_URL(id)
                        textColor = text
                        selectedTextColor = accent
                        selectedColor = 0x00000001.toLong()
                        identifier = id
                    }
                }
                profileSetting(nameRes = R.string.kau_logout) {
                    iicon = GoogleMaterial.Icon.gmd_exit_to_app
                    iconColor = text
                    textColor = text
                    identifier = -2L
                }
                profileSetting(nameRes = R.string.kau_add_account) {
                    iconDrawable = IconicsDrawable(this@BaseMainActivity, GoogleMaterial.Icon.gmd_add).actionBar().paddingDp(5).color(text.toInt())
                    textColor = text
                    identifier = -3L
                }
                profileSetting(nameRes = R.string.kau_manage_account) {
                    iicon = GoogleMaterial.Icon.gmd_settings
                    iconColor = text
                    textColor = text
                    identifier = -4L
                }
                onProfileChanged { _, profile, current ->
                    if (current) launchWebOverlay(FbItem.PROFILE.url)
                    else when (profile.identifier) {
                        -2L -> {
                            val currentCookie = loadFbCookie(Prefs.userId)
                            if (currentCookie == null) {
                                toast(R.string.account_not_found)
                                FbCookie.reset { launchLogin(cookies(), true) }
                            } else {
                                materialDialogThemed {
                                    title(R.string.kau_logout)
                                    content(String.format(string(R.string.kau_logout_confirm_as_x), currentCookie.name
                                            ?: Prefs.userId.toString()))
                                    positiveText(R.string.kau_yes)
                                    negativeText(R.string.kau_no)
                                    onPositive { _, _ -> FbCookie.logout(this@BaseMainActivity) }
                                }
                            }
                        }
                        -3L -> launchNewTask<LoginActivity>(clearStack = false)
                        -4L -> launchNewTask<SelectorActivity>(cookies(), false)
                        else -> {
                            FbCookie.switchUser(profile.identifier, this@BaseMainActivity::refreshAll)
                            tabsForEachView { _, view -> view.removeNotification(0) }
                        }
                    }
                    false
                }
            }
            drawerHeader.setActiveProfile(Prefs.userId)

            sectionItem {
                divider = false
            }
            val drawerItem = primaryItem(R.string.bookmarks) {
                iicon = GoogleMaterial.Icon.gmd_bookmark
                iconColor = text
                textColor = text
                selectedIconColor = text
                selectedTextColor = text
                selectedColor = 0x00000001.toLong()
                identifier = -999L
                onClick { _ ->
                    flashAnswers {
                        logContentView(ContentViewEvent()
                                .putContentType("drawer_item"))
                    }
                    launchBookMarkOverlay()
                    false
                }
                badge(realm.where(BookmarkModel::class.java).findAll().size.toString()) {
                    textColor = icon
                    color = noti
                    cornersDp = 4
                    paddingHorizontalDp = 25
                }
            }

            sectionHeader(R.string.feed)
            primaryFlashItem(FbItem.FEED_MOST_RECENT)
            primaryFlashItem(FbItem.FEED_TOP_STORIES)

            sectionHeader(R.string.more)
            primaryItem(R.string.videos) {
                iicon = GoogleMaterial.Icon.gmd_video_library
                iconColor = text
                textColor = text
                selectedIconColor = text
                selectedTextColor = text
                selectedColor = 0x00000001.toLong()
                onClick { _ ->
                    flashAnswers {
                        logContentView(ContentViewEvent()
                                .putContentType("drawer_item"))
                    }
                    launchWebOverlayBasic("https://www.facebook.com/$userId/videos_by?lst=$userId:$userId:1529771337")
                    false
                }
            }
            primaryFlashItem(FbItem.PHOTOS)
            primaryFlashItem(FbItem.POKE)
            primaryFlashItem(FbItem.GROUPS)
            primaryFlashItem(FbItem.MY_FRIENDS)
            primaryFlashItem(FbItem.CHAT)
            primaryFlashItem(FbItem.PAGES)

            sectionHeader(R.string.reminders)
            primaryFlashItem(FbItem.EVENTS)
            primaryFlashItem(FbItem.BIRTHDAYS)
            primaryFlashItem(FbItem.ON_THIS_DAY)
            primaryFlashItem(FbItem.ACTIVITY_LOG)

            sectionHeader(R.string.savenotes)
            primaryFlashItem(FbItem.SAVED)
            primaryFlashItem(FbItem.NOTES)

            divider()
            primaryItem(R.string.appversion) {
                iicon = GoogleMaterial.Icon.gmd_info_outline
                iconColor = text
                textColor = text
                selectedIconColor = text
                selectedTextColor = text
                selectedColor = 0x00000001.toLong()
                onClick { _ ->
                    flashAnswers {
                        logContentView(ContentViewEvent()
                                .putContentType("drawer_item"))
                    }
                    verSion()
                    false
                }
                badge(BuildConfig.VERSION_NAME) {
                    textColor = icon
                    color = noti
                    cornersDp = 4
                    paddingHorizontalDp = 25
                }
            }
            onOpened {
                drawerItem.badge?.run {
                    drawer.updateBadge(-999L, StringHolder(realm.where(BookmarkModel::class.java).findAll().size.toString()))
                    drawer.updateItem(drawerItem)
                }
            }

            onClosed {
                drawerItem.badge?.run {
                    drawer.updateBadge(-999L, StringHolder(realm.where(BookmarkModel::class.java).findAll().size.toString()))
                    drawer.updateItem(drawerItem)
                }
            }

        }
    }

    private fun Builder.primaryFlashItem(item: FbItem) = this.primaryItem(item.titleId) {
        val text = if (Prefs.DayNight && isNightTime(Activity())) Color.WHITE.withAlpha(255).darken().toLong() else Prefs.textColor.toLong()
        iicon = item.icon
        iconColor = text
        textColor = text
        selectedIconColor = text
        selectedTextColor = text
        selectedColor = 0x00000001.toLong()
        identifier = item.titleId.toLong()
        onClick { _ ->
            flashAnswers {
                logContentView(ContentViewEvent()
                        .putContentName(item.name)
                        .putContentType("drawer_item"))
            }
            launchWebOverlay(item.url)
            false
        }
    }

    private fun refreshAll() {
        L.d { "Refresh all" }
        fragmentSubject.onNext(REQUEST_REFRESH)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        toolbar.tint(Prefs.iconColor)
        setMenuIcons(menu, Prefs.iconColor,
                R.id.action_settings to GoogleMaterial.Icon.gmd_settings,
                R.id.action_search to GoogleMaterial.Icon.gmd_search)
        searchViewBindIfNull {
            bindSearchView(menu, R.id.action_search, Prefs.iconColor) {
                textCallback = { query, searchView ->
                    val results = searchViewCache[query]
                    if (results != null)
                        searchView.results = results
                    else {
                        val data = SearchParser.query(FbCookie.webCookie, query)?.data?.results
                        if (data != null) {
                            val items = data.mapTo(mutableListOf(), FlashSearch::toSearchItem)
                            if (items.isNotEmpty())
                                items.add(SearchItem("${FbItem._SEARCH.url}?q=$query", string(R.string.show_all_results), iicon = null))
                            searchViewCache[query] = items
                            searchView.results = items
                        }
                    }
                }
                //extraIcon = Pair(GoogleMaterial.Icon.gmd_delete_forever, View.OnClickListener {
                //    materialDialogThemed {
                //        title(R.string.searchremoval)
                //        content(R.string.searchremoval_desc)
                //        positiveText(R.string.kau_yes)
                //        negativeText(R.string.kau_no)
                //        onPositive { _, _ -> launchWebOverlay("https://mobile.facebook.com/$userId/allactivity?log_filter=search") }
                //    }
                //})
                shouldClearOnClose = true
                withDivider = true
                hintText = string(R.string.facebook_search)
                textDebounceInterval = 300
                searchCallback = { query, _ -> launchWebOverlay("${FbItem._SEARCH.url}/?q=$query"); true }
                closeListener = { _ -> searchViewCache.clear() }
                foregroundColor = Prefs.textColor
                backgroundColor = Prefs.bgColor.withMinAlpha(200)
                onItemClick = { _, key, _, _ -> launchWebOverlay(key) }
            }
        }
        return true
    }

    @SuppressLint("RestrictedApi")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                intent.putParcelableArrayListExtra(EXTRA_COOKIES, cookies())
                val bundle = ActivityOptions.makeCustomAnimation(this, R.anim.kau_slide_in_right, R.anim.kau_fade_out).toBundle()
                startActivityForResult(intent, ACTIVITY_SETTINGS, bundle)
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun openFileChooser(filePathCallback: ValueCallback<Array<Uri>?>, fileChooserParams: WebChromeClient.FileChooserParams) {
        openMediaPicker(filePathCallback, fileChooserParams)
    }

    @SuppressLint("NewApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (onActivityResultWeb(requestCode, resultCode, data)) return
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTIVITY_SETTINGS) {
            if (resultCode and REQUEST_RESTART_APPLICATION > 0) { //completely restart application
                L.d { "Restart Application Requested" }
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                val pending = PendingIntent.getActivity(this, 666, intent, PendingIntent.FLAG_CANCEL_CURRENT)
                val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (buildIsMarshmallowAndUp)
                    alarm.setExactAndAllowWhileIdle(AlarmManager.RTC, System.currentTimeMillis() + 100, pending)
                else
                    alarm.setExact(AlarmManager.RTC, System.currentTimeMillis() + 100, pending)
                finish()
                System.exit(0)
                return
            }
            if (resultCode and REQUEST_RESTART > 0) return restart()
            /*
             * These results can be stacked
             */
            if (resultCode and REQUEST_REFRESH > 0) fragmentSubject.onNext(REQUEST_REFRESH)
            if (resultCode and REQUEST_NAV > 0) flashNavigationBar()
            if (resultCode and REQUEST_TEXT_ZOOM > 0) fragmentSubject.onNext(REQUEST_TEXT_ZOOM)
            if (resultCode and REQUEST_SEARCH > 0) invalidateOptionsMenu()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(STATE_FORCE_FALLBACK, ArrayList(adapter.forcedFallbacks))
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        adapter.forcedFallbacks.clear()
        adapter.forcedFallbacks.addAll(savedInstanceState.getStringArrayList(STATE_FORCE_FALLBACK))
    }

    override fun onResume() {
        super.onResume()
        FbCookie.switchBackUser {}
        drawer.updateBadge(-999L, StringHolder(realm.where(BookmarkModel::class.java).findAll().size.toString()))
        controlWebview?.resumeTimers()
        if (System.currentTimeMillis() - lastAccessTime > MAIN_TIMEOUT_DURATION) {
            refreshAll()
        }
        lastAccessTime = System.currentTimeMillis() // precaution to avoid loops
    }

    override fun onPause() {
        controlWebview?.pauseTimers()
        L.v { "Pause main web timers" }
        lastAccessTime = System.currentTimeMillis()
        super.onPause()
    }

    override fun onStart() {
        //validate some pro features
        if (!IS_Flash_PRO) {
            if (Prefs.theme == Theme.CUSTOM.ordinal) Prefs.theme = Theme.DEFAULT.ordinal
        }
        super.onStart()
    }

    override fun onDestroy() {
        onDestroyBilling()
        controlWebview?.clearCache(true)
        controlWebview?.destroy()
        super.onDestroy()
    }

    override fun collapseAppBar() {
        appBar.post { appBar.setExpanded(false) }
    }

    override fun backConsumer(): Boolean {
        if (drawer.isDrawerOpen) {
            drawer.closeDrawer()
            drawer.updateBadge(-999L, StringHolder(realm.where(BookmarkModel::class.java).findAll().size.toString()))
            return true
        }
        if (currentFragment.onBackPressed()) return true
        if (viewPager.currentItem != 0) {
            viewPager.setCurrentItem(viewPager.currentItem - 5, true)
            return true
        }
        if (Prefs.exitConfirmation) {
            materialDialogThemed {
                title(R.string.exit)
                content(R.string.close_app_confirmation)
                positiveText(R.string.yes)
                negativeText(R.string.no)
                onPositive { _, _ -> finishAndRemoveTask() }
                checkBoxPromptRes(R.string.do_not_show_again, false) { _, b -> Prefs.exitConfirmation = !b }
            }
            return true
        } else {
            controlWebview?.clearCache(true)
            finishAndRemoveTask()
        }
        return false
    }

    inline val currentFragment
        get() = supportFragmentManager.findFragmentByTag("android:switcher:${R.id.container}:${viewPager.currentItem}") as BaseFragment

    override fun reloadFragment(fragment: BaseFragment) {
        runOnUiThread { adapter.reloadFragment(fragment) }
    }

    inner class SectionsPagerAdapter(val pages: List<FbItem>) : FragmentPagerAdapter(supportFragmentManager) {

        val forcedFallbacks = mutableSetOf<String>()

        fun reloadFragment(fragment: BaseFragment) {
            if (fragment is WebFragment) return
            L.d { "Reload fragment ${fragment.position}: ${fragment.baseEnum.name}" }
            forcedFallbacks.add(fragment.baseEnum.name)
            supportFragmentManager.beginTransaction().remove(fragment).commitNowAllowingStateLoss()
            notifyDataSetChanged()
        }

        override fun getItem(position: Int): Fragment {
            val item = pages[position]
            return BaseFragment(item.fragmentCreator,
                    forcedFallbacks.contains(item.name),
                    item,
                    position)
        }

        override fun getCount() = pages.size

        override fun getPageTitle(position: Int): CharSequence = getString(pages[position].titleId)

        override fun getItemPosition(fragment: Any) =
                if (fragment !is BaseFragment)
                    POSITION_UNCHANGED
                else if (fragment is WebFragment || fragment.valid)
                    POSITION_UNCHANGED
                else
                    POSITION_NONE
    }

    override val lowerVideoPadding: PointF
        get() =
            if (Prefs.mainActivityLayout == MainActivityLayout.BOTTOM_BAR)
                PointF(0f, toolbar.height.toFloat())
            else
                PointF(0f, 0f)

    companion object {
        private const val STATE_FORCE_FALLBACK = "flash_state_force_fallback"
    }
}


