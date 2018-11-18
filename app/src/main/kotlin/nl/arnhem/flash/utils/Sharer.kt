@file:Suppress("DEPRECATION")

package nl.arnhem.flash.utils

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatTextView

import nl.arnhem.flash.R

/** Created by Jorell on 5/5/2016. */
object Sharer {
    private const val DOWNLOAD_MANAGER_PACKAGE_NAME = "com.android.providers.downloads"

    /**
     * Resolve whether the DownloadManager is enable in current devices.
     *
     * @return true if DownloadManager is enable, false otherwise.
     */
    fun resolve(context: Context): Boolean {
        val enable = resolveEnable(context)
        if (!enable) {
            val alertDialog = createDialog(context)
            alertDialog.show()
            alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(context, R.color.md_red_500))
        }
        return enable
    }

    /**
     * Resolve whether the DownloadManager is enable in current devices.
     *
     * @param context Context of application
     * @return true if DownloadManager is enable, false otherwise.
     */
    private fun resolveEnable(context: Context): Boolean {
        val state = context.packageManager
                .getApplicationEnabledSetting(DOWNLOAD_MANAGER_PACKAGE_NAME)

        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            !(state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                    state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                    || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED)
        } else {
            !(state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun createDialog(context: Context): AlertDialog {
        val messageTextView = AppCompatTextView(context)
        messageTextView.textSize = 16f
        messageTextView.text = context.getString(R.string.kau_do_not_show_again)

        return AlertDialog.Builder(context)
                .setView(messageTextView, 50, 30, 50, 30)
                .setPositiveButton("ok") { _, _ -> enableDownloadManager(context) }
                .setCancelable(false)
                .create()
    }

    /**
     * Start activity to enable DownloadManager in Settings.
     */
    private fun enableDownloadManager(context: Context) {
        try {
            // open the specific App Info page
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$DOWNLOAD_MANAGER_PACKAGE_NAME")
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()

            // open the generic Apps page
            try {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                context.startActivity(intent)
            } catch (ignored: ActivityNotFoundException) {
            }

        }

    }

}