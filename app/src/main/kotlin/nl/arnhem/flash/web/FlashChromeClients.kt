package nl.arnhem.flash.web

import android.app.Activity
import android.net.Uri
import android.text.InputType
import android.webkit.*
import ca.allanwang.kau.permissions.PERMISSION_ACCESS_FINE_LOCATION
import ca.allanwang.kau.permissions.kauRequestPermissions
import ca.allanwang.kau.utils.adjustAlpha
import ca.allanwang.kau.utils.lighten
import ca.allanwang.kau.utils.withMinAlpha
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.GravityEnum
import com.afollestad.materialdialogs.MaterialDialog
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import nl.arnhem.flash.R
import nl.arnhem.flash.contracts.ActivityContract
import nl.arnhem.flash.utils.L
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.flashSnackbar
import nl.arnhem.flash.views.FlashWebView

/**
 * Created by Allan Wang on 2017-05-31.
 *
 * Collection of chrome clients
 */


/**
 * The default chrome client
 */

@Suppress("OverridingDeprecatedMember")
open class FlashChromeClient(web: FlashWebView) : WebChromeClient() {


    private val progress: Subject<Int> = web.parent.progressObservable
    private val title: BehaviorSubject<String> = web.parent.titleObservable
    private var activity = (web.context as? ActivityContract)
    private val context = web.context!! as Activity


    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
        val dimmerTextColor = Prefs.textColor.adjustAlpha(0.8f)
        MaterialDialog.Builder(context)
                .title(R.string.flash_name)
                .titleColor(Prefs.textColor)
                .backgroundColor(Prefs.bgColor.lighten(0.1f).withMinAlpha(200))
                .dividerColor(Prefs.notiColor)
                .iconRes(R.drawable.flash_f_256)
                .content(message)
                .contentColor(dimmerTextColor)
                .widgetColor(dimmerTextColor)
                .positiveText(R.string.kau_ok)
                .positiveColor(Prefs.textColor)
                .btnSelector(R.drawable.md_btn_selector_custom, DialogAction.POSITIVE)
                .buttonRippleColor(Prefs.iconColor)
                .buttonsGravity(GravityEnum.CENTER)
                .onPositive { _, _ -> result.confirm() }
                .dismissListener { result.cancel() }
                .show()
        return true
    }


    override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
        val dimmerTextColor = Prefs.textColor.adjustAlpha(0.8f)
        MaterialDialog.Builder(context)
                .title(R.string.flash_name)
                .titleColor(Prefs.textColor)
                .backgroundColor(Prefs.bgColor.lighten(0.1f).withMinAlpha(200))
                .dividerColor(Prefs.notiColor)
                .iconRes(R.drawable.flash_f_256)
                .content(message)
                .contentColor(dimmerTextColor)
                .widgetColor(dimmerTextColor)
                .positiveText(R.string.kau_ok)
                .positiveColor(Prefs.textColor)
                .negativeText(R.string.kau_cancel)
                .negativeColor(Prefs.textColor)
                .btnSelector(R.drawable.md_btn_selector_custom, DialogAction.POSITIVE)
                .buttonRippleColor(Prefs.iconColor)
                .buttonsGravity(GravityEnum.CENTER)
                .onPositive { _, _ -> result.confirm() }
                .onNegative { _, _ -> result.cancel() }
                .dismissListener { result.cancel() }
                .show()
        return true
    }

    override fun onJsPrompt(view: WebView, url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
        val dimmerTextColor = Prefs.textColor.adjustAlpha(0.8f)
        MaterialDialog.Builder(context)
                .title(R.string.flash_name)
                .titleColor(Prefs.textColor)
                .backgroundColor(Prefs.bgColor.lighten(0.1f).withMinAlpha(200))
                .dividerColor(Prefs.notiColor)
                .iconRes(R.drawable.flash_f_256)
                .content(message)
                .contentColor(dimmerTextColor)
                .widgetColor(dimmerTextColor)
                .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .input(R.string.abc_search_hint, 0) { _, _ ->
                    defaultValue.toInt()// Do something
                }
                .negativeText(R.string.kau_cancel)
                .negativeColor(Prefs.textColor)
                .btnSelector(R.drawable.md_btn_selector_custom, DialogAction.POSITIVE)
                .buttonRippleColor(Prefs.iconColor)
                .buttonsGravity(GravityEnum.CENTER)
                .onPositive { _, _ -> result.confirm() }
                .onNegative { _, _ -> result.cancel() }
                .dismissListener { result.cancel() }
                .show()
        return true
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        L.v { "Chrome Console ${consoleMessage.lineNumber()}: ${consoleMessage.message()}" }
        return true
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        super.onReceivedTitle(view, title)
        if (title.startsWith("http") || this.title.value == title) return
        this.title.onNext(title)
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        progress.onNext(newProgress)
    }

    override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>?>, fileChooserParams: FileChooserParams): Boolean {
        activity?.openFileChooser(filePathCallback, fileChooserParams)
                ?: webView.flashSnackbar(R.string.file_chooser_not_found)
        return activity != null
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        L.i { "Requesting geolocation" }
        context.kauRequestPermissions(PERMISSION_ACCESS_FINE_LOCATION) { granted, _ ->
            L.i { "Geolocation response received; ${if (granted) "granted" else "denied"}" }
            callback(origin, granted, true)
        }
    }
}