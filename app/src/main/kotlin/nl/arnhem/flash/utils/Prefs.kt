package nl.arnhem.flash.utils

import android.graphics.Color
import ca.allanwang.kau.kotlin.lazyResettable
import ca.allanwang.kau.kpref.KPref
import ca.allanwang.kau.kpref.kpref
import ca.allanwang.kau.utils.colorToForeground
import ca.allanwang.kau.utils.darken
import ca.allanwang.kau.utils.isColorVisibleOn
import ca.allanwang.kau.utils.withAlpha
import nl.arnhem.flash.enums.FACEBOOK_BLUE
import nl.arnhem.flash.enums.FeedSort
import nl.arnhem.flash.enums.MainActivityLayout
import nl.arnhem.flash.enums.Theme
import nl.arnhem.flash.injectors.InjectorContract

/**
 * Created by Allan Wang on 2017-05-28.
 *
 * Shared Preference object with lazy cached retrievals
 */
object Prefs : KPref() {

    var lastLaunch: Long by kpref("last_launch", -1L)

    var userId: Long by kpref("user_id", -1L)

    var prevId: Long by kpref("prev_id", -1L)

    var theme: Int by kpref("theme", 0, postSetter = { _: Int -> loader.invalidate() })

    var customTextColor: Int by kpref("color_text", 0xFFF57043.toInt())

    var customAccentColor: Int by kpref("color_accent", 0xff00B0FF.toInt())

    var customBackgroundColor: Int by kpref("color_bg", 0xff212121.toInt())

    var customHeaderColor: Int by kpref("color_header", 0xff00B0FF.toInt())

    var customIconColor: Int by kpref("color_icons", 0xFFF57043.toInt())

    var customNotiColor: Int by kpref("color_noti", 0xFF616161.toInt())

    var exitConfirmation: Boolean by kpref("exit_confirmation", true)

    var notificationFreq: Long by kpref("notification_freq", 15L)

    var versionCode: Int by kpref("version_code", -1)

    var prevVersionCode: Int by kpref("prev_version_code", -1)

    var installDate: Long by kpref("install_date", -1L)

    var identifier: Int by kpref("identifier", -1)

    private val loader = lazyResettable { Theme.values[Prefs.theme] }

    private val t: Theme by loader

    inline val accentColorForWhite: Int
        get() = when {
            accentColor.isColorVisibleOn(Color.WHITE) -> accentColor
            textColor.isColorVisibleOn(Color.WHITE) -> textColor
            else -> FACEBOOK_BLUE
        }

    inline val nativeBgColor: Int
        get() = Prefs.bgColor.withAlpha(30)

    fun nativeBgColor(unread: Boolean) = Prefs.bgColor
            .colorToForeground(if (unread) 0.9f else 0.0f)
            .withAlpha(30)

    fun nativeDayNightBgColor(unread: Boolean) = Color.BLACK
            .colorToForeground(if (unread) 0.3f else 0.0f)
            .withAlpha(255).darken()

    inline val nativeDayNightBgColor: Int
        get() = Color.BLACK

    val textColor: Int
        get() = t.textColor

    val accentColor: Int
        get() = t.accentColor

    val bgColor: Int
        get() = t.bgColor

    val headerColor: Int
        get() = t.headerColor

    val iconColor: Int
        get() = t.iconColor

    val notiColor: Int
        get() = t.notiColor

    val themeInjector: InjectorContract
        get() = t.injector

    val isCustomTheme: Boolean
        get() = t == Theme.CUSTOM

    inline val FlashId: String
        get() = "$installDate-$identifier"

    var tintNavBar: Boolean by kpref("tint_nav_bar", true)

    var webTextScaling: Int by kpref("web_text_scaling", 100)

    var nightTheme: Int by kpref("night_theme", 1830)

    var dayTheme: Int by kpref("day_theme", 630)

    var feedSort: Int by kpref("feed_sort", FeedSort.MOST_RECENT.ordinal)

    var aggressiveRecents: Boolean by kpref("aggressive_recents", false)

    var showRoundedIcons: Boolean by kpref("rounded_icons", true)

    var showComposer: Boolean by kpref("status_composer_feed", true)

    var showStoriesTray: Boolean by kpref("status_stories_feed", true)

    var fixedComposer: Boolean by kpref("status_fixed_composer_feed", false)

    var bottomComposer: Boolean by kpref("status_bottom_composer_feed", false)

    var paddingComposer: Boolean by kpref("status_padding_composer_feed", false)

    var showSuggestedFriends: Boolean by kpref("suggested_friends_feed", true)

    var showSuggestedGroups: Boolean by kpref("suggested_groups_feed", true)

    var showFacebookAds: Boolean by kpref("facebook_ads", true)

    var animate: Boolean by kpref("fancy_animations", true)

    var notificationKeywords: Set<String> by kpref("notification_keywords", mutableSetOf())

    var notificationsGeneral: Boolean by kpref("notification_general", true)

    var notificationAllAccounts: Boolean by kpref("notification_all_accounts", true)

    var notificationsInstantMessages: Boolean by kpref("notification_im", true)

    var notificationsImAllAccounts: Boolean by kpref("notification_im_all_accounts", false)

    var notificationVibrate: Boolean by kpref("notification_vibrate", true)

    var notificationSound: Boolean by kpref("notification_sound", true)

    var notificationRingtone: String by kpref("notification_ringtone", "")

    var messageRingtone: String by kpref("message_ringtone", "")

    var notificationLights: Boolean by kpref("notification_lights", true)

    var messageScrollToBottom: Boolean by kpref("message_scroll_to_bottom", true)

    var enablePip: Boolean by kpref("enable_pip", true)

    var HasFab: Boolean by kpref("has_fab", true)

    var AdRemoval: Boolean by kpref("ad_removal", true)

    var AutoUpdate: Boolean by kpref("auto_update", true)

    var blackMediaBg: Boolean by kpref("black_media_bg", false)

    var DisableAudio: Boolean by kpref("disable_audio", false)

    //var DisableVideoAUTO: Boolean by kpref("disable_video_auto", true)

    var DayNight: Boolean by kpref("day_night", false)

    var freeProSettings: Boolean by kpref("free_pro_settings", false)

    var autoImageLoader: Boolean by kpref("auto_image_loader", false)

    var backToTop: Boolean by kpref("back_to_top", false)

    /**
     * Cache like value to determine if user has or had pro
     * In most cases, [nl.arnhem.flash.utils.iab.IS_Flash_PRO] should be looked at instead
     * This has been renamed to pro for short, but keep in mind that it only reflects the value
     * of when it was previously verified
     */

    var pro: Boolean by kpref("previously_pro", false)

    var debugPro: Boolean by kpref("debug_pro", false)

    var verboseLogging: Boolean by kpref("verbose_logging", false)

    var analytics: Boolean by kpref("analytics", true)

    var overlayEnabled: Boolean by kpref("overlay_enabled", true)

    var overlayFullScreenSwipe: Boolean by kpref("overlay_full_screen_swipe", true)

    var viewpagerSwipe: Boolean by kpref("viewpager_swipe", true)

    var loadMediaOnMeteredNetwork: Boolean by kpref("media_on_metered_network", true)

    var debugSettings: Boolean by kpref("debug_settings", false)

    var linksInDefaultApp: Boolean by kpref("link_in_default_app", false)

    var mainActivityLayoutType: Int by kpref("main_activity_layout_type", 0)

    inline val mainActivityLayout: MainActivityLayout
        get() = MainActivityLayout(mainActivityLayoutType)

    override fun deleteKeys() = arrayOf("search_bar")
}
