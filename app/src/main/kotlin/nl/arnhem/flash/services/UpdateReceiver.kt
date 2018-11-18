package nl.arnhem.flash.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nl.arnhem.flash.utils.L
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.onComplete

/**
 * Created by Allan Wang on 2017-05-31.
 *
 * Receiver that is triggered whenever the app updates so it can bind the notifications again
 */
class UpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        L.d { "Flash has updated" }
        context.scheduleNotifications(Prefs.notificationFreq) //Update notifications
    }
}