package nl.arnhem.flash.iitems

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import ca.allanwang.kau.iitems.KauIItem
import ca.allanwang.kau.ui.createSimpleRippleDrawable
import ca.allanwang.kau.utils.bindView
import ca.allanwang.kau.utils.gone
import ca.allanwang.kau.utils.visible
import com.mikepenz.fastadapter.FastAdapter
import nl.arnhem.flash.R
import nl.arnhem.flash.facebook.requests.MenuFooterItem
import nl.arnhem.flash.facebook.requests.MenuHeader
import nl.arnhem.flash.facebook.requests.MenuItem
import nl.arnhem.flash.glide.FlashGlide
import nl.arnhem.flash.glide.GlideApp
import nl.arnhem.flash.utils.Prefs


/**
 * Created by Allan Wang on 30/12/17.
 */
class MenuContentIItem(val data: MenuItem)
    : KauIItem<MenuContentIItem, MenuContentIItem.ViewHolder>(R.layout.iitem_menu, ::ViewHolder),
        ClickableIItemContract {

    override val url: String?
        get() = data.url

    class ViewHolder(itemView: View) : FastAdapter.ViewHolder<MenuContentIItem>(itemView) {

        val frame: ViewGroup by bindView(R.id.item_frame)
        val icon: ImageView by bindView(R.id.item_icon)
        val content: TextView by bindView(R.id.item_content)
        val badge: TextView by bindView(R.id.item_badge)

        override fun bindView(item: MenuContentIItem, payloads: MutableList<Any>) {
            frame.background = createSimpleRippleDrawable(Prefs.textColor, Prefs.nativeBgColor)
            content.setTextColor(Prefs.textColor)
            badge.setTextColor(Prefs.textColor)
            val iconUrl = item.data.pic
            if (iconUrl != null)
                GlideApp.with(itemView)
                        .load(iconUrl)
                        .transform(FlashGlide.roundCorner)
                        .into(icon.visible())
            else
                icon.gone()
            content.text = item.data.name
        }

        override fun unbindView(item: MenuContentIItem) {
            badge.gone()
        }
    }
}

class MenuHeaderIItem(val data: MenuHeader) : HeaderIItem(data.header,
        itemId = R.id.item_menu_header)

class MenuFooterIItem(val data: MenuFooterItem)
    : TextIItem(data.name, data.url, R.id.item_menu_footer)

class MenuFooterSmallIItem(val data: MenuFooterItem)
    : TextIItem(data.name, data.url, R.id.item_menu_footer_small)