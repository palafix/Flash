package nl.arnhem.flash.fragments

import com.mikepenz.fastadapter.IItem
import nl.arnhem.flash.facebook.FbCookie
import nl.arnhem.flash.facebook.FbItem
import nl.arnhem.flash.facebook.requests.*
import nl.arnhem.flash.iitems.*
import nl.arnhem.flash.parsers.FlashNotifs
import nl.arnhem.flash.parsers.NotifParser
import nl.arnhem.flash.parsers.ParseResponse
import nl.arnhem.flash.utils.flashJsoup
import nl.arnhem.flash.views.FlashRecyclerView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * Created by Allan Wang on 27/12/17.
 */
class NotificationFragment : FlashParserFragment<FlashNotifs, NotificationIItem>() {

    override val parser = NotifParser

    override fun getDoc(cookie: String?) = flashJsoup(cookie, "${FbItem.NOTIFICATIONS.url}?more")

    override fun toItems(response: ParseResponse<FlashNotifs>): List<NotificationIItem> =
            response.data.notifs.map { NotificationIItem(it, response.cookie) }

    override fun bindImpl(recyclerView: FlashRecyclerView) {
        NotificationIItem.bindEvents(adapter)
    }
}

class MenuFragment : GenericRecyclerFragment<MenuItemData, IItem<*, *>>() {

    override fun mapper(data: MenuItemData): IItem<*, *> = when (data) {
        is MenuHeader -> MenuHeaderIItem(data)
        is MenuItem -> MenuContentIItem(data)
        is MenuFooterItem ->
            if (data.isSmall) MenuFooterSmallIItem(data)
            else MenuFooterIItem(data)
        else -> throw IllegalArgumentException("Menu item in fragment has invalid type ${data::class.java.simpleName}")
    }

    override fun bindImpl(recyclerView: FlashRecyclerView) {
        ClickableIItemContract.bindEvents(adapter)
    }

    override fun reloadImpl(progress: (Int) -> Unit, callback: (Boolean) -> Unit) {
        doAsync {
            val cookie = FbCookie.webCookie
            progress(10)
            cookie.fbRequest({ callback(false) }) {
                progress(30)
                val data = getMenuData().invoke() ?: return@fbRequest callback(false)
                if (data.data.isEmpty()) return@fbRequest callback(false)
                progress(70)
                val items = data.flatMapValid()
                progress(90)
                uiThread { adapter.add(items) }
                callback(true)
            }
        }
    }
}