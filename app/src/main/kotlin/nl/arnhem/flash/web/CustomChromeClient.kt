package nl.arnhem.flash.web

import android.annotation.SuppressLint
import android.app.Activity
import android.media.MediaPlayer
import android.os.Build
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.VideoView
import ca.allanwang.kau.utils.adjustAlpha
import ca.allanwang.kau.utils.lighten
import ca.allanwang.kau.utils.withMinAlpha
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.GravityEnum
import nl.arnhem.flash.R
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.materialDialogThemed


@Suppress("OverridingDeprecatedMember")
open class CustomChromeClient constructor(private val activity: Activity) : WebChromeClient(), MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private var isVideoFullscreen: Boolean = false
    private var videoViewContainer: FrameLayout? = null
    private var videoViewCallback: WebChromeClient.CustomViewCallback? = null
    private var customViewDialog: AlertDialog? = null

    init {
        isVideoFullscreen = false
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        if (view is FrameLayout) {
            val focusedChild = view.focusedChild

            this.isVideoFullscreen = true
            this.videoViewContainer = view
            this.videoViewCallback = callback

            if (customViewDialog != null && customViewDialog!!.isShowing)
                customViewDialog!!.dismiss()

            customViewDialog = AlertDialog.Builder(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen).setView(videoViewContainer).setOnDismissListener {
                val attrs = activity.window.attributes
                attrs.flags = attrs.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN.inv()
                attrs.flags = attrs.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
                activity.window.attributes = attrs
                if (Build.VERSION.SDK_INT >= 14)
                    activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }.create()
            customViewDialog!!.show()

            val attrs = activity.window.attributes
            attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
            attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            activity.window.attributes = attrs
            if (Build.VERSION.SDK_INT >= 14)
                activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE

            if (focusedChild is VideoView) {

                focusedChild.setOnPreparedListener(this)
                focusedChild.setOnCompletionListener(this)
                focusedChild.setOnErrorListener(this)
            }
        }
    }

    override fun onShowCustomView(view: View, requestedOrientation: Int, callback: WebChromeClient.CustomViewCallback) {
        onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        if (isVideoFullscreen) {
            if (customViewDialog != null && customViewDialog!!.isShowing)
                customViewDialog!!.dismiss()

            if (videoViewCallback != null && !videoViewCallback!!.javaClass.name.contains(".chromium.")) {
                videoViewCallback!!.onCustomViewHidden()
            }

            isVideoFullscreen = false
            videoViewContainer = null
            videoViewCallback = null
        }
    }

    override fun onPrepared(mp: MediaPlayer) {}

    override fun onCompletion(mp: MediaPlayer) {
        onHideCustomView()
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        return false
    }

    fun onBackPressed(): Boolean {
        return if (isVideoFullscreen) {
            onHideCustomView()
            true
        } else {
            false
        }
    }

    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
        val dimmerTextColor = Prefs.textColor.adjustAlpha(0.8f)
        activity.materialDialogThemed {
            title(url)
            titleColor(Prefs.textColor)
            backgroundColor(Prefs.bgColor.lighten(0.1f).withMinAlpha(200))
            dividerColor(Prefs.notiColor)
            //iconRes(R.drawable.flash_f_256)
            content(message)
            contentColor(dimmerTextColor)
            widgetColor(dimmerTextColor)
            positiveText(R.string.kau_ok)
            positiveColor(Prefs.textColor)
            btnSelector(R.drawable.md_btn_selector_custom, DialogAction.POSITIVE)
            buttonRippleColor(Prefs.iconColor)
            buttonsGravity(GravityEnum.CENTER)
            onPositive { _, _ -> result.confirm() }
            dismissListener { result.cancel() }
        }
        return true
    }


    override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
        val dimmerTextColor = Prefs.textColor.adjustAlpha(0.8f)
        activity.materialDialogThemed {
            title(url)
            titleColor(Prefs.textColor)
            backgroundColor(Prefs.bgColor.lighten(0.1f).withMinAlpha(200))
            dividerColor(Prefs.notiColor)
            //iconRes(R.drawable.flash_f_256)
            content(message)
            contentColor(dimmerTextColor)
            widgetColor(dimmerTextColor)
            positiveText(R.string.kau_ok)
            positiveColor(Prefs.textColor)
            negativeText(R.string.kau_cancel)
            negativeColor(Prefs.textColor)
            btnSelector(R.drawable.md_btn_selector_custom, DialogAction.POSITIVE)
            buttonRippleColor(Prefs.iconColor)
            buttonsGravity(GravityEnum.CENTER)
            onPositive { _, _ -> result.confirm() }
            onNegative { _, _ -> result.cancel() }
            dismissListener { result.cancel() }
            show()
        }
        return true
    }

    override fun onJsPrompt(view: WebView, url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
        val dimmerTextColor = Prefs.textColor.adjustAlpha(0.8f)
        activity.materialDialogThemed {
            title(url)
            titleColor(Prefs.textColor)
            backgroundColor(Prefs.bgColor.lighten(0.1f).withMinAlpha(200))
            dividerColor(Prefs.notiColor)
            //iconRes(R.drawable.flash_f_256)
            content(message)
            contentColor(dimmerTextColor)
            widgetColor(dimmerTextColor)
            inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            input(R.string.kau_search, 0) { _, _ ->
                defaultValue.toInt()// Do something
            }
            negativeText(R.string.kau_cancel)
            negativeColor(Prefs.textColor)
            btnSelector(R.drawable.md_btn_selector_custom, DialogAction.POSITIVE)
            buttonRippleColor(Prefs.iconColor)
            buttonsGravity(GravityEnum.CENTER)
            onPositive { _, _ -> result.confirm() }
            onNegative { _, _ -> result.cancel() }
            dismissListener { result.cancel() }
            show()
        }
        return true
    }
}