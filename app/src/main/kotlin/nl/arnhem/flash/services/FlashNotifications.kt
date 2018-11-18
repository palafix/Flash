@file:Suppress("KDocUnresolvedReference")

package nl.arnhem.flash.services

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BaseBundle
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import ca.allanwang.kau.utils.dpToPx
import ca.allanwang.kau.utils.string
import me.leolin.shortcutbadger.ShortcutBadger
import nl.arnhem.flash.BuildConfig
import nl.arnhem.flash.R
import nl.arnhem.flash.activities.FlashWebActivity
import nl.arnhem.flash.dbflow.CookieModel
import nl.arnhem.flash.dbflow.NotificationModel
import nl.arnhem.flash.dbflow.lastNotificationTime
import nl.arnhem.flash.enums.OverlayContext
import nl.arnhem.flash.facebook.FbItem
import nl.arnhem.flash.glide.FlashGlide
import nl.arnhem.flash.glide.GlideApp
import nl.arnhem.flash.parsers.FlashParser
import nl.arnhem.flash.parsers.MessageParser
import nl.arnhem.flash.parsers.NotifParser
import nl.arnhem.flash.parsers.ParseNotification
import nl.arnhem.flash.utils.ARG_USER_ID
import nl.arnhem.flash.utils.L
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.flashEvent
import java.util.*

/**
 * Created by Allan Wang on 2017-07-08.
 *
 * Logic for build notifications, scheduling notifications, and showing notifications
 */
private val _40_DP = 40.dpToPx

/**
 * Enum to handle notification creations
 */
enum class NotificationType(
        private val channelId: String,
        private val overlayContext: OverlayContext,
        private val fbItem: FbItem,
        private val parser: FlashParser<ParseNotification>,
        private val getTime: (notif: NotificationModel) -> Long,
        private val putTime: (notif: NotificationModel, time: Long) -> NotificationModel,
        private val ringtone: () -> String) {

    GENERAL(NOTIF_CHANNEL_GENERAL,
            OverlayContext.NOTIFICATION,
            FbItem.NOTIFICATIONS,
            NotifParser,
            NotificationModel::epoch,
            { notif, time -> notif.copy(epoch = time) },
            Prefs::notificationRingtone) {

        override fun bindRequest(content: NotificationContent, cookie: String) =
                FlashRunnable.prepareMarkNotificationRead(content.id, cookie)
    },

    MESSAGE(NOTIF_CHANNEL_MESSAGES,
            OverlayContext.MESSAGE,
            FbItem.MESSAGES,
            MessageParser,
            NotificationModel::epochIm,
            { notif, time -> notif.copy(epochIm = time) },
            Prefs::messageRingtone);

    private val groupPrefix = "flash_${name.toLowerCase(Locale.CANADA)}"

    /**
     * Optional binder to return the request bundle builder
     */
    internal open fun bindRequest(content: NotificationContent, cookie: String): (BaseBundle.() -> Unit)? = null

    private fun bindRequest(intent: Intent, content: NotificationContent, cookie: String?) {
        cookie ?: return
        val binder = bindRequest(content, cookie) ?: return
        val bundle = Bundle()
        bundle.binder()
        intent.putExtras(bundle)
    }

    /**
     * Get unread data from designated parser
     * Display notifications for those after old epoch
     * Save new epoch
     *
     * Returns the number of notifications generated,
     * or -1 if an error occurred
     */
    fun fetch(context: Context, data: CookieModel): Int {
        val response = parser.parse(data.cookie)
        if (response == null) {
            L.v { "$name notification data not found" }
            return -1
        }
        val notifContents = response.data.getUnreadNotifications(data).filter { notif ->
            val text = notif.text
            Prefs.notificationKeywords.none { text.contains(it, true) }
        }
        if (notifContents.isEmpty()) return 0
        val userId = data.id
        val prevNotifTime = lastNotificationTime(userId)
        val prevLatestEpoch = getTime(prevNotifTime)
        L.v { "Notif $name prev epoch $prevLatestEpoch" }
        var newLatestEpoch = prevLatestEpoch
        val notifs = mutableListOf<FlashNotification>()
        notifContents.forEach { notif ->
            L.v { "Notif timestamp ${notif.timestamp}" }
            if (notif.timestamp <= prevLatestEpoch) return@forEach
            notifs.add(createNotification(context, notif))
            if (notif.timestamp > newLatestEpoch)
                newLatestEpoch = notif.timestamp
        }
        if (newLatestEpoch > prevLatestEpoch)
            putTime(prevNotifTime, newLatestEpoch).save()
        L.d { "Notif $name new epoch ${getTime(lastNotificationTime(userId))}" }
        if (prevLatestEpoch == -1L && !BuildConfig.DEBUG) {
            L.d { "Skipping first notification fetch" }
            return 0 // do not notify the first time
        }
        flashEvent("Notifications", "Type" to name, "Count" to notifs.size)
        if (notifs.size > 1)
            summaryNotification(context, userId, notifs.size).notify(context)
        val ringtone = ringtone()
        notifs.forEachIndexed { i, notif ->
            notif.withAlert(i < 2, ringtone).notify(context)
        }
        return notifs.size
    }

    fun debugNotification(context: Context, data: CookieModel) {
        val content = NotificationContent(data,
                System.currentTimeMillis(),
                "https://github.com/palafix/Flash",
                "Debug Notif",
                "Test 123",
                System.currentTimeMillis() / 1000,
                "https://www.iconexperience.com/_img/v_collection_png/256x256/shadow/dog.png")
        createNotification(context, content).notify(context)
    }

    /**
     * Create and submit a new notification with the given [content]
     */
    private fun createNotification(context: Context, content: NotificationContent): FlashNotification =
        with(content) {
            val intent = Intent(context, FlashWebActivity::class.java)
            intent.data = Uri.parse(href)
            intent.putExtra(ARG_USER_ID, data.id)
            overlayContext.put(intent)
            bindRequest(intent, content, data.cookie)
            val group = "${groupPrefix}_${data.id}"
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val notifBuilder = context.flashNotification(channelId)
                    .setContentTitle(title ?: context.string(R.string.flash_name))
                    .setContentText(text)
                    .setContentIntent(pendingIntent)
                    .setCategory(Notification.CATEGORY_SOCIAL)
                    .setSubText(data.name)
                    .setGroup(group)
            ShortcutBadger.applyCount(context, 1)
            if (timestamp != -1L) notifBuilder.setWhen(timestamp * 1000)
            L.v { "Notif load $content" }

            if (profileUrl != null) {
                try {
                    val profileImg = GlideApp.with(context)
                            .asBitmap()
                            .load(profileUrl)
                            .transform(FlashGlide.circleCrop)
                            .submit(_40_DP, _40_DP)
                            .get()
                    notifBuilder.setLargeIcon(profileImg)
                } catch (e: Exception) {
                    L.e { "Failed to get image $profileUrl" }
                }
            }
            FlashNotification(group, notifId, notifBuilder)
        }



    /**
     * Create a summary notification to wrap the previous ones
     * This will always produce sound, vibration, and lights based on preferences
     * and will only show if we have at least 2 notifications
     */
    private fun summaryNotification(context: Context, userId: Long, count: Int): FlashNotification {
        val intent = Intent(context, FlashWebActivity::class.java)
        intent.data = Uri.parse(fbItem.url)
        intent.putExtra(ARG_USER_ID, userId)
        val group = "${groupPrefix}_$userId"
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val notifBuilder = context.flashNotification(channelId)
                .setContentTitle(context.string(R.string.flash_name))
                .setContentText("$count ${context.string(fbItem.titleId)}")
                .setGroup(group)
                .setGroupSummary(true)
                .setContentIntent(pendingIntent)
                .setCategory(Notification.CATEGORY_SOCIAL)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifBuilder.setGroupAlertBehavior(Notification.GROUP_ALERT_CHILDREN)
        }

        return FlashNotification(group, 1, notifBuilder)
    }

}

/**
 * Notification data holder
 */
data class NotificationContent(val data: CookieModel,
                               val id: Long,
                               val href: String,
                               val title: String? = null, // defaults to flash title
                               val text: String,
                               val timestamp: Long,
                               val profileUrl: String?) {

    val notifId = Math.abs(id.toInt())

}

/**
 * Wrapper for a complete notification builder and identifier
 * which can be immediately notified when given a [Context]
 */
data class FlashNotification(private val tag: String,
                             private val id: Int,
                             val notif: NotificationCompat.Builder) {

    fun withAlert(enable: Boolean, ringtone: String): FlashNotification {
        notif.setFlashAlert(enable, ringtone)
        return this
    }

    fun notify(context: Context) =
            NotificationManagerCompat.from(context).notify(tag, id, notif.build())
}

const val NOTIFICATION_PERIODIC_JOB = 7

fun Context.scheduleNotifications(minutes: Long): Boolean =
        scheduleJob<NotificationService>(NOTIFICATION_PERIODIC_JOB, minutes)

const val NOTIFICATION_JOB_NOW = 6

fun Context.fetchNotifications(): Boolean =
        fetchJob<NotificationService>(NOTIFICATION_JOB_NOW)