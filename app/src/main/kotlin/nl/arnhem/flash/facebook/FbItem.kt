package nl.arnhem.flash.facebook

import android.support.annotation.StringRes
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic
import nl.arnhem.flash.R
import nl.arnhem.flash.fragments.BaseFragment
import nl.arnhem.flash.fragments.MenuFragment
import nl.arnhem.flash.fragments.NotificationFragment
import nl.arnhem.flash.fragments.WebFragment
import nl.arnhem.flash.utils.EnumBundle
import nl.arnhem.flash.utils.EnumBundleCompanion
import nl.arnhem.flash.utils.EnumCompanion


enum class FbItem(
        @StringRes val titleId: Int,
        val icon: IIcon,
        relativeUrl: String,
        val fragmentCreator: () -> BaseFragment = ::WebFragment
) : EnumBundle<FbItem> {

    ACTIVITY_LOG(R.string.activity_log, GoogleMaterial.Icon.gmd_list, "me/allactivity"),
    BIRTHDAYS(R.string.birthdays, GoogleMaterial.Icon.gmd_cake, "events/birthdays"),
    CHAT(R.string.chat, GoogleMaterial.Icon.gmd_chat, "buddylist"),
    EVENTS(R.string.events, GoogleMaterial.Icon.gmd_event_note, "events/upcoming"),
    FEED(R.string.feed, CommunityMaterial.Icon.cmd_newspaper, ""),
    FEED_MOST_RECENT(R.string.most_recent, GoogleMaterial.Icon.gmd_history, "home.php?sk=h_chr"),
    FEED_TOP_STORIES(R.string.top_stories, GoogleMaterial.Icon.gmd_star, "home.php?sk=h_nor"),
    FRIENDS(R.string.friends, GoogleMaterial.Icon.gmd_person_add, "friends/center/requests"),
    GROUPS(R.string.groups, GoogleMaterial.Icon.gmd_group, "groups"),
    MENU(R.string.menu, GoogleMaterial.Icon.gmd_menu, "settings", ::MenuFragment),
    MESSAGES(R.string.messages, MaterialDesignIconic.Icon.gmi_comments, "messages"),
    NOTES(R.string.notes, CommunityMaterial.Icon.cmd_note, "notes"),
    NOTIFICATIONS(R.string.notifications, MaterialDesignIconic.Icon.gmi_globe, "notifications", ::NotificationFragment),
    ON_THIS_DAY(R.string.on_this_day, GoogleMaterial.Icon.gmd_today, "onthisday"),
    PAGES(R.string.pages, GoogleMaterial.Icon.gmd_flag, "pages"),
    PHOTOS(R.string.photos, GoogleMaterial.Icon.gmd_photo, "me/photos"),
    PROFILE(R.string.profile, CommunityMaterial.Icon.cmd_account, "me"),
    SAVED(R.string.saved, GoogleMaterial.Icon.gmd_bookmark, "saved"),
    _SEARCH(R.string.search_menu_title, GoogleMaterial.Icon.gmd_search, "search/top"),
    SETTINGS(R.string.settings, GoogleMaterial.Icon.gmd_settings, "settings/"),
	POKE(R.string.pokes, GoogleMaterial.Icon.gmd_thumb_up, "pokes"),
    ;

    val url = "$FB_URL_BASE$relativeUrl"

    val isFeed: Boolean
        get() = when (this) {
            FEED, FEED_MOST_RECENT, FEED_TOP_STORIES -> true
            else -> false
        }

    override val bundleContract: EnumBundleCompanion<FbItem>
        get() = Companion

    companion object : EnumCompanion<FbItem>("flash_arg_fb_item", values())
}
fun defaultTabs(): List<FbItem> = listOf(FbItem.FEED, FbItem.FRIENDS, FbItem.MESSAGES, FbItem.NOTIFICATIONS, FbItem.MENU)
