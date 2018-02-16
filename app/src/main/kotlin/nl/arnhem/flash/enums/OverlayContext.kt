package nl.arnhem.flash.enums

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import ca.allanwang.kau.utils.toDrawable
import nl.arnhem.flash.R
import nl.arnhem.flash.facebook.FbItem
import nl.arnhem.flash.utils.EnumBundle
import nl.arnhem.flash.utils.EnumBundleCompanion
import nl.arnhem.flash.utils.EnumCompanion
import nl.arnhem.flash.views.FlashWebView

/**
 * Created by Allan Wang on 2017-09-16.
 *
 * Options for [WebOverlayActivityBase] to give more info as to what kind of
 * overlay is present.
 *
 * For now, this is able to add new menu options upon first load
 */
enum class OverlayContext(private val menuItem: FlashMenuItem?) : EnumBundle<OverlayContext> {

    NOTIFICATION(FlashMenuItem(R.id.action_notification, FbItem.NOTIFICATIONS)),
    MESSAGE(FlashMenuItem(R.id.action_messages, FbItem.MESSAGES));

    /**
     * Inject the [menuItem] in the order that they are given at the front of the menu
     */
    fun onMenuCreate(context: Context, menu: Menu) {
        menuItem?.addToMenu(context, menu, 0)
    }

    override val bundleContract: EnumBundleCompanion<OverlayContext>
        get() = Companion

    companion object : EnumCompanion<OverlayContext>("flash_arg_overlay_context", values()) {

        /**
         * Execute selection call for an item by id
         * Returns [true] if selection was consumed, [false] otherwise
         */
        fun onOptionsItemSelected(web: FlashWebView, id: Int): Boolean {
            val item = values.firstOrNull { id == it.menuItem?.id }?.menuItem ?: return false
            web.loadUrl(item.fbItem.url, true)
            return true
        }
    }
}

/**
 * Frame for an injectable menu item
 */
class FlashMenuItem(
        val id: Int,
        val fbItem: FbItem,
        val showAsAction: Int = MenuItem.SHOW_AS_ACTION_ALWAYS) {
    fun addToMenu(context: Context, menu: Menu, index: Int) {
        val item = menu.add(Menu.NONE, id, index, fbItem.titleId)
        item.icon = fbItem.icon.toDrawable(context, 18)
        item.setShowAsAction(showAsAction)
    }
}