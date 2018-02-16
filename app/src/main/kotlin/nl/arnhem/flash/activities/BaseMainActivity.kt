package nl.arnhem.flash.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import co.zsmb.materialdrawerkt.draweritems.badgeable.secondaryItem
import co.zsmb.materialdrawerkt.draweritems.divider
import co.zsmb.materialdrawerkt.draweritems.profile.profile
import co.zsmb.materialdrawerkt.draweritems.profile.profileSetting
import co.zsmb.materialdrawerkt.draweritems.sectionHeader
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
import nl.arnhem.flash.facebook.FbCookie
import nl.arnhem.flash.facebook.FbItem
import nl.arnhem.flash.facebook.PROFILE_PICTURE_URL
import nl.arnhem.flash.fragments.BaseFragment
import nl.arnhem.flash.fragments.WebFragment
import nl.arnhem.flash.model.BookmarkModel
import nl.arnhem.flash.parsers.FlashSearch
import nl.arnhem.flash.parsers.SearchParser
import nl.arnhem.flash.utils.*
import nl.arnhem.flash.utils.iab.FlashBilling
import nl.arnhem.flash.utils.iab.IS_Flash_PRO
import nl.arnhem.flash.utils.iab.IabMain
import nl.arnhem.flash.views.BadgedIcon
import nl.arnhem.flash.views.FlashVideoViewer
import nl.arnhem.flash.views.FlashViewPager
import java.util.*
import kotlin.collections.set
import kotlin.properties.Delegates
import kotlin.reflect.KClass

/**
 * Created by Allan Wang on 20/12/17.
 *
 * Most of the logic that is unrelated to handling fragments
 */

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
        setFlashColors {
            toolbar(toolbar)
            themeWindow = false
            header(appBar)
            background(viewPager)
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
        AppUpdater(this)
                .setDisplay(Display.NOTIFICATION)
                .setUpdateFrom(UpdateFrom.JSON)
                .setIcon(R.drawable.flash_notify) // Notification icon
                .setUpdateJSON("http://updatephase.palafix.nl/flash_updater.json")
                .start()
    }

    /**
     * Injector to handle creation for sub classes
     */
    protected abstract fun onNestedCreate(savedInstanceState: Bundle?)

    private var hasFab = false
    private var shouldShow = false

    private fun initFab() {
        //if (hasFab && shouldShow && Prefs.enableFab) {
        hasFab = false
        shouldShow = false
        fab.backgroundTintList = ColorStateList.valueOf(Prefs.headerColor.withMinAlpha(200))
        fab.hide()
        appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (!hasFab) return@addOnOffsetChangedListener
            val percent = Math.abs(verticalOffset.toFloat() / appBarLayout.totalScrollRange)
            val shouldShow = percent < 0.2
            if (this.shouldShow != shouldShow) {
                this.shouldShow = shouldShow
                fab.showIf(shouldShow)
            }
        }
    }


    override fun showFab(iicon: IIcon, clickEvent: () -> Unit) {
        hasFab = true
        fab.setOnClickListener { clickEvent() }
        if (shouldShow) {
            if (fab.isShown) {
                fab.fadeScaleTransition {
                    setIcon(iicon, Prefs.iconColor)
                }
                return
            }
        }
        fab.setIcon(iicon, Prefs.iconColor)
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
        val navBg = Prefs.bgColor.withMinAlpha(200).toLong()
        val navHeader = Prefs.headerColor.withMinAlpha(175)
        drawer = drawer {
            toolbar = this@BaseMainActivity.toolbar
            savedInstance = savedInstanceState
            actionBarDrawerToggleAnimated = true
            translucentStatusBar = false
            sliderBackgroundColor = navBg
            drawerHeader = accountHeader {
                backgroundDrawable = ColorDrawable(navHeader)
                customViewRes = R.layout.material_drawer_header
                textColor = Prefs.iconColor.toLong()
                selectionSecondLineShown = false
                cookies().forEach { (id, name) ->
                    profile(name = name ?: "") {
                        iconUrl = PROFILE_PICTURE_URL(id)
                        textColor = Prefs.textColor.toLong()
                        selectedTextColor = Prefs.textColor.toLong()
                        selectedColor = 0x00000001.toLong()
                        identifier = id
                    }
                }
                profileSetting(nameRes = R.string.kau_logout) {
                    iicon = GoogleMaterial.Icon.gmd_exit_to_app
                    iconColor = Prefs.textColor.toLong()
                    textColor = Prefs.textColor.toLong()
                    identifier = -2L
                }
                profileSetting(nameRes = R.string.kau_add_account) {
                    iconDrawable = IconicsDrawable(this@BaseMainActivity, GoogleMaterial.Icon.gmd_add).actionBar().paddingDp(5).color(Prefs.textColor)
                    textColor = Prefs.textColor.toLong()
                    identifier = -3L
                }
                profileSetting(nameRes = R.string.kau_manage_account) {
                    iicon = GoogleMaterial.Icon.gmd_settings
                    iconColor = Prefs.textColor.toLong()
                    textColor = Prefs.textColor.toLong()
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

            sectionHeader(R.string.bookmark_beta) {
                divider = false
            }
            val drawerItem = primaryItem(R.string.bookmarks) {
                iicon = GoogleMaterial.Icon.gmd_bookmark
                iconColor = Prefs.textColor.toLong()
                textColor = Prefs.textColor.toLong()
                selectedIconColor = Prefs.textColor.toLong()
                selectedTextColor = Prefs.textColor.toLong()
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
                    textColor = Prefs.iconColor.toLong()
                    color = Prefs.notiColor.toLong()
                    cornersDp = 4
                    paddingHorizontalDp = 25
                }
            }

            primaryItem(R.string.refresh_bookmark) {
                iicon = GoogleMaterial.Icon.gmd_refresh
                iconColor = Prefs.textColor.toLong()
                textColor = Prefs.textColor.toLong()
                selectedIconColor = Prefs.textColor.toLong()
                selectedTextColor = Prefs.textColor.toLong()
                selectedColor = 0x00000001.toLong()
                identifier = -200L
                onClick { _ ->
                    drawerItem.withBadge(realm.where(BookmarkModel::class.java).findAll().size.toString())
                    drawer.updateBadge(-999L, StringHolder(realm.where(BookmarkModel::class.java).findAll().size.toString()))
                    drawer.updateItem(drawerItem)
                    true
                }
            }

            sectionHeader(R.string.feed)
            primaryFlashItem(FbItem.FEED_MOST_RECENT)
            primaryFlashItem(FbItem.FEED_TOP_STORIES)

            sectionHeader(R.string.more)
            primaryFlashItem(FbItem.POKE)
            primaryFlashItem(FbItem.PHOTOS)
            primaryFlashItem(FbItem.GROUPS)
            primaryFlashItem(FbItem.FRIENDS)
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
            secondaryFlashItem(R.string.youtube)

            divider()
            primaryItem(R.string.settings) {
                iicon = GoogleMaterial.Icon.gmd_settings
                iconColor = Prefs.textColor.toLong()
                textColor = Prefs.textColor.toLong()
                selectedIconColor = Prefs.textColor.toLong()
                selectedTextColor = Prefs.textColor.toLong()
                selectedColor = 0x00000001.toLong()
                identifier = 2
                onClick(openActivity(SettingsActivity::class))
            }
            secondaryItem(R.string.appversion) {
                badge(BuildConfig.VERSION_NAME) {
                    textColor = Prefs.iconColor.toLong()
                    color = Prefs.notiColor.toLong()
                    cornersDp = 4
                    paddingHorizontalDp = 25
                }
                iicon = GoogleMaterial.Icon.gmd_info_outline
                iconColor = Prefs.textColor.toLong()
                textColor = Prefs.textColor.toLong()
                selectedIconColor = Prefs.textColor.toLong()
                selectedTextColor = Prefs.textColor.toLong()
                selectedColor = 0x00000001.toLong()
            }

            onOpened {
                drawerItem.badge?.let {
                    drawer.updateBadge(-999L, StringHolder(realm.where(BookmarkModel::class.java).findAll().size.toString()))
                    drawer.updateItem(drawerItem)
                }
            }

            onClosed {
                drawerItem.badge?.let {
                    drawer.updateBadge(-999L, StringHolder(realm.where(BookmarkModel::class.java).findAll().size.toString()))
                    drawer.updateItem(drawerItem)
                }
            }

        }
    }

    private fun Builder.primaryFlashItem(item: FbItem) = this.primaryItem(item.titleId) {
        iicon = item.icon
        iconColor = Prefs.textColor.toLong()
        textColor = Prefs.textColor.toLong()
        selectedIconColor = Prefs.textColor.toLong()
        selectedTextColor = Prefs.textColor.toLong()
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

    private fun Builder.secondaryFlashItem(@StringRes title: Int) = this.secondaryItem(title) {
        iicon = GoogleMaterial.Icon.gmd_youtube_searched_for
        iconColor = Prefs.textColor.toLong()
        textColor = Prefs.textColor.toLong()
        selectedIconColor = Prefs.textColor.toLong()
        selectedTextColor = Prefs.textColor.toLong()
        selectedColor = 0x00000001.toLong()
        identifier = title.toLong()
        onClick { _ ->
            flashAnswers {
                logContentView(ContentViewEvent()
                        .putContentType("drawer_item"))
            }
            launchWebOverlay(url = YOUTUBE)
            false
        }
    }

    private fun refreshAll() {
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
                            val items = data.map(FlashSearch::toSearchItem).toMutableList()
                            if (items.isNotEmpty())
                                items.add(SearchItem("${FbItem._SEARCH.url}?q=$query", string(R.string.show_all_results), iicon = null))
                            searchViewCache[query] = items
                            searchView.results = items
                        }
                    }
                }
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

    private val STATE_FORCE_FALLBACK = "flash_state_force_fallback"

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
    }

    override fun onPause() {
        controlWebview?.pauseTimers()
        L.v { "Pause main web timers" }
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
        controlWebview?.destroy()
        controlWebview?.clearCache(true)
        super.onDestroy()
    }

    override fun collapseAppBar() {
        appBar.setExpanded(false)
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
                title(R.string.kau_exit)
                content(R.string.kau_exit_confirmation)
                positiveText(R.string.kau_yes)
                negativeText(R.string.kau_no)
                onPositive { _, _ -> finishAndRemoveTask() }
                checkBoxPromptRes(R.string.kau_do_not_show_again, false, { _, b -> Prefs.exitConfirmation = !b })
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

    private fun <T : Activity> openActivity(activity: KClass<T>): (View?) -> Boolean = {
        startActivity(Intent(this@BaseMainActivity, activity.java))
        false
    }

    override val lowerVideoPadding: PointF
        get() =
            if (Prefs.mainActivityLayout == MainActivityLayout.BOTTOM_BAR)
                PointF(0f, toolbar.height.toFloat())
            else
                PointF(0f, 0f)
}


