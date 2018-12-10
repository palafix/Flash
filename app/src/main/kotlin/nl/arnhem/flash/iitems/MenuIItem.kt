@file:Suppress("DEPRECATION")

package nl.arnhem.flash.iitems

import android.graphics.drawable.GradientDrawable
import android.support.v7.widget.CardView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import ca.allanwang.kau.iitems.KauIItem
import ca.allanwang.kau.ui.createSimpleRippleDrawable
import ca.allanwang.kau.utils.*
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

        private val frame: ViewGroup by bindView(R.id.item_frame)
        private val icon: ImageView by bindView(R.id.item_icon)
        private val content: TextView by bindView(R.id.item_content)
        private val badge: TextView by bindView(R.id.item_badge)
        private val card: CardView by bindView(R.id.badgecard)

        override fun bindView(item: MenuContentIItem, payloads: MutableList<Any>) {
                frame.background = createSimpleRippleDrawable(Prefs.textColor, Prefs.nativeBgColor)
                content.setTextColor(Prefs.textColor)
                badge.setTextColor(Prefs.textColor)
                val badgeColor = Prefs.accentColor.withAlpha(255).colorToForeground(0.2f)
                val badgeBackground = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(badgeColor, badgeColor))
                badgeBackground.cornerRadius = 13.dpToPx.toFloat()
                card.background = badgeBackground

            content.textSize = Prefs.webTextScaling.div(6).toFloat()
            badge.textSize = Prefs.webTextScaling.div(8).toFloat()
            val iconUrl = item.data.pic
            if (iconUrl != null) {
                GlideApp.with(itemView)
                        .load(iconUrl)
                        .transform(FlashGlide.roundCorner)
                        .into(icon.visible())
            } else {
                icon.gone()
            }
            content.text = item.data.name
            val badgeUrl = item.data.badge
            if (badgeUrl != null) {
                card.visible()
            } else
                card.gone()
            badge.text = item.data.badge
        }


        override fun unbindView(item: MenuContentIItem) {
            GlideApp.with(itemView).clear(icon)
            content.text = null
            badge.text = null
            card.gone()
        }
    }
}

class MenuHeaderIItem(val data: MenuHeader) : HeaderIItem(data.header,
        itemId = R.id.item_menu_header)

class MenuFooterIItem(val data: MenuFooterItem)
    : TextIItem(data.name, data.url, R.id.item_menu_footer)

class MenuFooterSmallIItem(val data: MenuFooterItem)
    : TextIItem(data.name, data.url, R.id.item_menu_footer_small)