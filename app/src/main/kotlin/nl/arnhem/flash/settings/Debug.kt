package nl.arnhem.flash.settings

import android.content.Context
import ca.allanwang.kau.kpref.activity.KPrefAdapterBuilder
import ca.allanwang.kau.utils.materialDialog
import ca.allanwang.kau.utils.startActivityForResult
import ca.allanwang.kau.utils.string
import nl.arnhem.flash.R
import nl.arnhem.flash.activities.DebugActivity
import nl.arnhem.flash.activities.SettingsActivity
import nl.arnhem.flash.activities.SettingsActivity.Companion.ACTIVITY_REQUEST_DEBUG
import nl.arnhem.flash.debugger.OfflineWebsite
import nl.arnhem.flash.facebook.FB_URL_BASE
import nl.arnhem.flash.facebook.FbCookie
import nl.arnhem.flash.facebook.FbItem
import nl.arnhem.flash.parsers.FlashParser
import nl.arnhem.flash.parsers.MessageParser
import nl.arnhem.flash.parsers.NotifParser
import nl.arnhem.flash.parsers.SearchParser
import nl.arnhem.flash.utils.L
import nl.arnhem.flash.utils.flashUriFromFile
import nl.arnhem.flash.utils.sendFlashEmail
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.File
import java.util.concurrent.Future

/**
 * Created by Allan Wang on 2017-06-30.
 *
 * A sub pref section that is enabled through a hidden preference
 * Each category will load a page, extract the contents, remove private info, and create a report
 */
fun SettingsActivity.getDebugPrefs(): KPrefAdapterBuilder.() -> Unit = {

    plainText(R.string.experimental_disclaimer) {
        descRes = R.string.debug_disclaimer_info
    }

    plainText(R.string.debug_web) {
        descRes = R.string.debug_web_desc
        onClick = { this@getDebugPrefs.startActivityForResult<DebugActivity>(ACTIVITY_REQUEST_DEBUG) }
    }

    plainText(R.string.debug_parsers) {
        descRes = R.string.debug_parsers_desc
        onClick = {

            val parsers = arrayOf(NotifParser, MessageParser, SearchParser)

            materialDialog {
                items(parsers.map { string(it.nameRes) })
                itemsCallback { dialog, _, position, _ ->
                    dialog.dismiss()
                    val parser = parsers[position]
                    var attempt: Future<Unit>? = null
                    val loading = materialDialog {
                        content(parser.nameRes)
                        progress(true, 100)
                        negativeText(R.string.kau_cancel)
                        onNegative { dialog, _ ->
                            attempt?.cancel(true)
                            dialog.dismiss()
                        }
                        canceledOnTouchOutside(false)
                    }

                    attempt = loading.doAsync({
                        createEmail(parser, "Error: ${it.message}")
                    }) {
                        val data = parser.parse(FbCookie.webCookie)
                        uiThread {
                            if (it.isCancelled) return@uiThread
                            it.dismiss()
                            createEmail(parser, data?.data)
                        }
                    }
                }
            }

        }
    }
}

private fun Context.createEmail(parser: FlashParser<*>, content: Any?) =
        sendFlashEmail("${string(R.string.debug_report)}: ${parser::class.java.simpleName}") {
            addItem("Url", parser.url)
            addItem("Contents", "$content")
        }

private const val ZIP_NAME = "debug"

fun SettingsActivity.sendDebug(url: String, html: String?) {

    val downloader = OfflineWebsite(url, FbCookie.webCookie ?: "",
            baseUrl = FB_URL_BASE,
            html = html,
            baseDir = DebugActivity.baseDir(this))

    val md = materialDialog {
        title(R.string.parsing_data)
        progress(false, 100)
        negativeText(R.string.kau_cancel)
        onNegative { dialog, _ -> dialog.dismiss() }
        canceledOnTouchOutside(false)
        dismissListener { downloader.cancel() }
    }

    md.doAsync {
        downloader.loadAndZip(ZIP_NAME, { progress ->
            uiThread { it.setProgress(progress) }
        }) { success ->
            uiThread {
                it.dismiss()
                if (success) {
                    val zipUri = it.context.flashUriFromFile(
                            File(downloader.baseDir, "$ZIP_NAME.zip"))
                    L.i { "Sending debug zip with uri $zipUri" }
                    sendFlashEmail(R.string.debug_report_email_title) {
                        addItem("Url", url)
                        addAttachment(zipUri)
                        extras = {
                            type = "application/zip"
                        }
                    }
                } else {
                    toast(R.string.error_generic)
                }
            }
        }

    }

}