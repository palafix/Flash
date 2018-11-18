package nl.arnhem.flash.iitems

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.support.v7.widget.CardView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import ca.allanwang.kau.iitems.KauIItem
import ca.allanwang.kau.ui.createSimpleRippleDrawable
import ca.allanwang.kau.utils.*
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.commons.utils.DiffCallback
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import nl.arnhem.flash.R
import nl.arnhem.flash.glide.FlashGlide
import nl.arnhem.flash.glide.GlideApp
import nl.arnhem.flash.parsers.FlashNotif
import nl.arnhem.flash.services.FlashRunnable
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.isNightTime
import nl.arnhem.flash.utils.launchWebOverlay

/**
 * Created by Allan Wang on 27/12/17.
 **/
class NotificationIItem(val notification: FlashNotif, val cookie: String) : KauIItem<NotificationIItem, NotificationIItem.ViewHolder>(
        R.layout.iitem_notification, ::ViewHolder
) {

    companion object {
        fun bindEvents(adapter: ItemAdapter<NotificationIItem>) {
            adapter.fastAdapter.withSelectable(false)
                    .withOnClickListener { v, _, item, position ->
                        val notif = item.notification
                        if (notif.unread) {
                            FlashRunnable.markNotificationRead(v!!.context, notif.id, item.cookie)
                            adapter.set(position, NotificationIItem(notif.copy(unread = false), item.cookie))
                        }
                        v!!.context.launchWebOverlay(notif.url)
                        true
                    }
        }

        //todo see if necessary
        val DIFF: DiffCallback<NotificationIItem> by lazy(::Diff)
    }

    private class Diff : DiffCallback<NotificationIItem> {

        override fun areItemsTheSame(oldItem: NotificationIItem, newItem: NotificationIItem) =
                oldItem.notification.id == newItem.notification.id

        override fun areContentsTheSame(oldItem: NotificationIItem, newItem: NotificationIItem) =
                oldItem.notification == newItem.notification

        override fun getChangePayload(oldItem: NotificationIItem, oldItemPosition: Int, newItem: NotificationIItem, newItemPosition: Int): Any? {
            return newItem
        }
    }

    @Suppress("DEPRECATION")
    class ViewHolder(itemView: View) : FastAdapter.ViewHolder<NotificationIItem>(itemView) {

        private val frame: ViewGroup by bindView(R.id.item_frame)
        private val avatar: ImageView by bindView(R.id.item_avatar)
        private val content: TextView by bindView(R.id.item_content)
        private val date: TextView by bindView(R.id.item_date)
        private val minithumb: ImageView by bindView(R.id.item_minithumb)
        private val thumbnail: ImageView by bindView(R.id.item_thumbnail)
        private val cardnail: CardView by bindView(R.id.item_cardnail)

        private val glide
            get() = GlideApp.with(itemView)

        override fun bindView(item: NotificationIItem, payloads: MutableList<Any>) {
            val notif = item.notification
            if (Prefs.DayNight && isNightTime(Activity())) {
                frame.background = createSimpleRippleDrawable(Color.DKGRAY,
                        Prefs.nativeDayNightBgColor(notif.unread))
                content.setTextColor(Color.WHITE)
                date.setTextColor(Color.WHITE.withAlpha(150))
            } else {
                frame.background = createSimpleRippleDrawable(Prefs.textColor,
                        Prefs.nativeBgColor(notif.unread))
                content.setTextColor(Prefs.textColor)
                date.setTextColor(Prefs.textColor.withAlpha(150))

            }

            content.textSize = Prefs.webTextScaling.div(6).toFloat()
            date.textSize = Prefs.webTextScaling.div(8).toFloat()
            val glide = glide
            glide.load(notif.img)
                    .transform(FlashGlide.roundCorner)
                    .into(avatar)
            System.out.println(notif.thumbnailUrl)

            if (notif.thumbnailUrl != null) {
                glide.load(notif.thumbnailUrl)
                        .override(5000,5000)
                        .into(thumbnail.visible())
                cardnail.visible()
                content.text = notif.content
                date.text = notif.timeString
                minithumb.setIcon(GoogleMaterial.Icon.gmd_flash_on, 10, Prefs.accentColor)
            } else {
                glide.clear(thumbnail)
                thumbnail.gone()
                cardnail.gone()
                content.text = notif.content
                date.text = notif.timeString
                minithumb.setIcon(GoogleMaterial.Icon.gmd_flash_on, 10, Prefs.accentColor)
            }
        }

        override fun unbindView(item: NotificationIItem) {
            frame.background = null
            val glide = glide
            glide.clear(avatar)
            glide.clear(thumbnail)
            thumbnail.gone()
            content.text = null
            date.text = null
        }
    }
}