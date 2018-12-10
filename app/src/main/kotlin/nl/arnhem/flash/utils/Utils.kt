@file:Suppress("KDocUnresolvedReference", "DEPRECATION")

package nl.arnhem.flash.utils

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.support.annotation.StringRes
import android.support.design.widget.SnackbarContentLayout
import android.support.design.widget.Snackbar
import android.support.v4.app.NotificationCompat
import android.support.v4.content.FileProvider
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.FrameLayout
import android.widget.TextView
import ca.allanwang.kau.email.EmailBuilder
import ca.allanwang.kau.email.sendEmail
import ca.allanwang.kau.mediapicker.createMediaFile
import ca.allanwang.kau.mediapicker.createPrivateMediaFile
import ca.allanwang.kau.utils.*
import ca.allanwang.kau.xml.showChangelog
import com.afollestad.materialdialogs.MaterialDialog
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom
import nl.arnhem.flash.BuildConfig
import nl.arnhem.flash.R
import nl.arnhem.flash.activities.*
import nl.arnhem.flash.dbflow.CookieModel
import nl.arnhem.flash.facebook.*
import nl.arnhem.flash.facebook.FbUrlFormatter.Companion.VIDEO_REDIRECT
import nl.arnhem.flash.facebook.FbUrlFormatter.Companion.WWW_VIDEO_REDIRECT
import nl.arnhem.flash.services.NOTIF_CHANNEL_DOWNLOADS
import nl.arnhem.flash.services.setFlashAlert
import nl.arnhem.flash.utils.iab.IS_Flash_PRO
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Allan Wang on 2017-06-03.
 **/
const val EXTRA_COOKIES = "extra_cookies"
const val ARG_URL = "arg_url"
const val ARG_USER_ID = "arg_user_id"
const val ARG_IMAGE_URL = "arg_image_url"
const val ARG_TEXT = "arg_text"
const val ARG_COOKIE = "arg_cookie"
const val ARG_TITLE = "arg_title"


inline fun <reified T : Activity> Context.launchNewTask(cookieList: ArrayList<CookieModel> = arrayListOf(), clearStack: Boolean = false) {
    startActivity<T>(clearStack, intentBuilder = {
        putParcelableArrayListExtra(EXTRA_COOKIES, cookieList)
    })
}

fun Context.launchLogin(cookieList: ArrayList<CookieModel>, clearStack: Boolean = true) {
    if (cookieList.isNotEmpty()) launchNewTask<SelectorActivity>(cookieList, clearStack)
    else launchNewTask<LoginActivity>(clearStack = clearStack)
}

fun Activity.cookies(): ArrayList<CookieModel> {
    return intent?.getParcelableArrayListExtra<CookieModel>(EXTRA_COOKIES) ?: arrayListOf()
}

/**
 * Launches the given url in a new overlay (if it already isn't in an overlay)
 * Note that most requests may need to first check if the url can be launched as an overlay
 * See [requestWebOverlay] to verify the launch
 */
private inline fun <reified T : WebOverlayActivityBase> Context.launchWebOverlayImpl(url: String) {
    val argUrl = url.formattedFbUrl
    L.v { "Launch received: $url\nLaunch web overlay: $argUrl" }
    if (argUrl.isFacebookUrl && argUrl.contains("/logout.php"))
        FbCookie.logout(this)
    if (argUrl.isFacebookUrl)
        startActivity<T>(intentBuilder = {
            putExtra(ARG_URL, argUrl)
        })
    else if (!(Prefs.linksInDefaultApp && resolveActivityForUri(Uri.parse(argUrl)))) {
        val intent = Intent(this, CustomTabs::class.java)
        intent.data = Uri.parse(argUrl)
        startActivity(intent)
        return
    }
}

fun Context.launchWebOverlay(url: String) = launchWebOverlayImpl<WebOverlayActivity>(url)

fun Context.launchWebOverlayBasic(url: String) = launchWebOverlayImpl<WebOverlayBasicActivity>(url)

fun Context.launchWebOverlayBasicFlash(url: String) = launchWebOverlayImpl<WebOverlayBasicFlashActivity>(url)

fun Context.fadeBundle() = ActivityOptions.makeCustomAnimation(this,
        android.R.anim.fade_in, android.R.anim.fade_out).toBundle()!!

fun Context.launchImageActivity(imageUrl: String, text: String? = null, title: String? = null, cookie: String? = null) {
    startActivity<ImageActivity>(intentBuilder = {
        putExtras(fadeBundle())
        putExtra(ARG_IMAGE_URL, imageUrl)
        putExtra(ARG_TEXT, text)
        putExtra(ARG_TITLE, title)
        putExtra(ARG_COOKIE, cookie)
    })
}

//fun Context.popToRoot() {
//    val intent = Intent(this, Test::class.java)
//    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
//    startActivity(intent)
//    Activity().overridePendingTransition(R.anim.kau_slide_in_bottom, R.anim.mu_no_anim)
//}
//
//fun Context.popToTest(imageUrl: String) {
//    startActivity<Test>(intentBuilder = {
//        putExtra(ARG_URL, imageUrl)
//    })
//}

/**
 * No Delete
 */
fun Context.launchBookMarkOverlay() {
    val intent = Intent(this, BookMarkActivity::class.java)
    startActivity(intent)
    return
}

fun Context.launchSettingsActivity() {
    val intent = Intent(this, SettingsActivity::class.java)
    startActivity(intent)
    return
}

/**
 * Used to get MimeType from url.
 *
 * @param url Url.
 * @return Mime Type for the given url.
 */

fun Context.onComplete(url: String): BroadcastReceiver? {
    val onComplete: BroadcastReceiver
    onComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val folder = if (url.endsWith(".mp4")
                    || url.endsWith(".webm")
                    || url.endsWith(".avi")
                    || url.endsWith(".mkv")
                    || url.endsWith(".mpg")
                    || url.endsWith(".flv")
                    || url.endsWith(".swv")
                    || url.endsWith(".rm")
                    || url.endsWith(".wmv")
                    || url.endsWith(".mov")
                    || url.endsWith(".m4v")) {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + File.separator + "Flash/Flash_Media/$url")
            } else if (url.endsWith(".jpg")
                    || url.endsWith(".img")
                    || url.endsWith(".png")
                    || url.endsWith(".jpeg")
                    || url.endsWith(".gif")) {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + File.separator + "Flash/Flash_Images/$url")
            } else
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + File.separator + "Flash/Flash_Documents/$url")
            val uri = flashUriFromFile(folder)
            Log.d(BuildConfig.APPLICATION_ID, "File to download = $folder")
            val mime = MimeTypeMap.getSingleton()
            val ext = folder.name.substring(folder.name.indexOf(".") + 1)
            val type = mime.getMimeTypeFromExtension(ext)
            val openFile = Intent(Intent.ACTION_VIEW, Uri.parse("$folder"))
            openFile.setDataAndType(Uri.parse("$folder"), type)
            openFile.data = uri
            openFile.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            openFile.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            val pIntent = PendingIntent.getActivity(context, 0, openFile, 0)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // you must create a notification channel for API 26 and Above
                val dls = string(R.string.download)
                val appName = string(R.string.flash_name)
                val description = string(R.string.download_notification_description)
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(NOTIF_CHANNEL_DOWNLOADS, "$appName: $dls", importance)
                channel.description = description
                channel.enableLights(true)
                channel.lightColor = R.color.md_white_1000
                channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                channel.setShowBadge(true)
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                nm.createNotificationChannel(channel)
            }

                val flashdownload = NotificationCompat.Builder(context, NOTIF_CHANNEL_DOWNLOADS)
                flashdownload.setAutoCancel(true)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setWhen(System.currentTimeMillis())
                        .setProgress(100, 100, false)
                        .setSmallIcon(R.drawable.flash_f_256)
                        .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
                        .setTicker("{Thanks for using Flash Social}")
                        .setContentTitle(string(R.string.flash_name))
                        .setContentText(url)
                        .setStyle(NotificationCompat.BigTextStyle())
                        .setFlashAlert(Prefs.notificationVibrate, Prefs.notificationRingtone)
                        .setSubText(string(R.string.downloaded_compleet))
                        .setContentIntent(pIntent)
                        .color = color(R.color.flash_notification_icon)
            nm.notify(1, flashdownload.build())
            }
        }
    return onComplete
}

fun Context.verSion(): Boolean {
    AppUpdater(this)
            .setDisplay(Display.SNACKBAR)
            .setUpdateFrom(UpdateFrom.JSON)
            .setUpdateJSON("http://updatephase.palafix.nl/flash_updater.json")
            .setIcon(R.drawable.flash_notify) // Notification icon
            .showAppUpdated(false)
            .start()
    return true
}

fun Activity.launchTabCustomizerActivity() {
    startActivityForResult<TabCustomizerActivity>(SettingsActivity.ACTIVITY_REQUEST_TABS, bundleBuilder = {
        with(fadeBundle())
    })
}

fun WebOverlayActivity.url(): String {
    return intent.getStringExtra(ARG_URL) ?: FbItem.FEED.url
}

fun Context.materialDialogThemed(action: MaterialDialog.Builder.() -> Unit): MaterialDialog {
    val builder = MaterialDialog.Builder(this).theme()
    builder.action()
    return builder.show()
}

fun Context.materialDialogThemedImage(action: MaterialDialog.Builder.() -> Unit): MaterialDialog {
    val builder = MaterialDialog.Builder(this).theme()
    builder.action()
    return builder.build()
}

fun MaterialDialog.Builder.theme(): MaterialDialog.Builder {
    val dimmerTextColor = Prefs.textColor.adjustAlpha(0.8f)
    titleColor(Prefs.textColor)
    contentColor(dimmerTextColor)
    widgetColor(dimmerTextColor)
    backgroundColor(Prefs.bgColor.lighten(0.1f).withMinAlpha(200))
    positiveColor(Prefs.textColor)
    negativeColor(Prefs.textColor)
    neutralColor(Prefs.textColor)
    return this
}

fun Activity.setFlashTheme(forceTransparent: Boolean = false) {
    val isTransparent = (Color.alpha(Prefs.bgColor) != 255) || forceTransparent
    if (Prefs.bgColor.isColorDark)
        setTheme(if (isTransparent) R.style.FlashTheme_Transparent else R.style.FlashTheme)
    else
        setTheme(if (isTransparent) R.style.FlashTheme_Light_Transparent else R.style.FlashTheme_Light)
}

class ActivityThemeUtils {

    private var toolbar: Toolbar? = null
    var themeWindow = true
    private var texts = mutableListOf<TextView>()
    private var headers = mutableListOf<View>()
    private var backgrounds = mutableListOf<View>()

    fun toolbar(toolbar: Toolbar) {
        this.toolbar = toolbar
    }

    fun text(vararg views: TextView) {
        texts.addAll(views)
    }

    fun header(vararg views: View) {
        headers.addAll(views)
    }

    fun background(vararg views: View) {
        backgrounds.addAll(views)
    }

    fun theme(activity: Activity) {
        with(activity) {
            statusBarColor = Prefs.headerColor
            if (Prefs.tintNavBar) navigationBarColor = Prefs.headerColor
            if (themeWindow) window.setBackgroundDrawable(ColorDrawable(Prefs.bgColor))
            toolbar?.setBackgroundColor(Prefs.headerColor)
            toolbar?.setTitleTextColor(Prefs.iconColor)
            toolbar?.overflowIcon?.setTint(Prefs.iconColor)
            texts.forEach { it.setTextColor(Prefs.textColor) }
            headers.forEach { it.setBackgroundColor(Prefs.headerColor) }
            backgrounds.forEach { it.setBackgroundColor(Prefs.bgColor) }
        }
    }
}

@SuppressLint("WrongConstant")
fun isNightTime(activity: Activity): Boolean {
    with(activity) {
        if (!Prefs.DayNight) {
            return false
        }
        try {
            val startTime: Int
            val endTime: Int
            val start = Prefs.nightTheme
            val nightStart = start.toString()
            val daylight = start.toString()
            val end = Prefs.dayTheme
            val endDaynight = end.toString()
            val nightEnd = end.toString()
            val calNow = Calendar.getInstance()
            val isDS = TimeZone.getDefault()
            calNow.set(Calendar.PM, Calendar.AM)
            val timeNow = SimpleDateFormat("HHmm", Locale.getDefault()).format(calNow.time)
            startTime = if (isDS.useDaylightTime()) {
                Integer.parseInt(daylight)
            } else {
                Integer.parseInt(nightStart)
            }
            endTime = if (isDS.useDaylightTime()) {
                Integer.parseInt(endDaynight)
            } else {
                Integer.parseInt(nightEnd)
            }
            val nowTime = Integer.parseInt(timeNow)
            return if (startTime > endTime) {
                nowTime > startTime || nowTime < endTime
            } else
                !(startTime >= endTime || nowTime < startTime || nowTime > endTime)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}

inline fun Activity.setFlashColors(builder: ActivityThemeUtils.() -> Unit) {
    val themer = ActivityThemeUtils()
    themer.builder()
    themer.theme(this)
}

fun flashEvent(name: String, vararg events: Pair<String, Any>) {
    // todo bind
    L.v { "Event: $name ${events.joinToString(", ")}" }
}

/**
 * Helper method to quietly keep track of throwable issues
 */
fun Throwable?.logFlashEvent(text: String) {
    val msg = if (this == null) text else "$text: $message"
    L.e { msg }
    flashEvent("Errors", "text" to text, "message" to (this?.message ?: "NA"))
}

fun flashAnswers(action: Answers.() -> Unit) {
    if (BuildConfig.DEBUG || !Prefs.analytics) return
    Answers.getInstance().action()
}

fun flashAnswersCustom(name: String, vararg events: Pair<String, Any>) {
    flashAnswers {
        logCustom(CustomEvent("Flash $name").apply {
            events.forEach { (key, value) ->
                if (value is Number) putCustomAttribute(key, value)
                else putCustomAttribute(key, value.toString())
            }
        })
    }
}

/**
 * Helper method to quietly keep track of throwable issues
 */
fun Throwable?.logFlashAnswers(text: String) {
    val msg = if (this == null) text else "$text: $message"
    L.e { msg }
    flashAnswersCustom("Errors", "text" to text, "message" to (this?.message ?: "NA"))
}

fun Activity.flashSnackbar(@StringRes string: Int, builder: Snackbar.() -> Unit = {}) = snackbar(string, Snackbar.LENGTH_LONG, flashSnackbar(builder))

fun Activity.flashSnackbar(@StringRes string: String, builder: Snackbar.() -> Unit = {}) = snackbar(string, Snackbar.LENGTH_LONG, flashSnackbar(builder))

fun View.flashSnackbar(@StringRes string: String, builder: Snackbar.() -> Unit = {}) = snackbar(string, Snackbar.LENGTH_LONG, flashSnackbar(builder))

fun View.flashSnackbar(@StringRes string: Int, builder: Snackbar.() -> Unit = {}) = snackbar(string, Snackbar.LENGTH_LONG, flashSnackbar(builder))

@SuppressLint("RestrictedApi")
private inline fun flashSnackbar(crossinline builder: Snackbar.() -> Unit): Snackbar.() -> Unit = {
    builder()
    //hacky workaround, but it has proper checks and shouldn't crash
    ((view as? FrameLayout)?.getChildAt(0) as? SnackbarContentLayout)?.apply {
        messageView.setTextColor(Prefs.textColor)
        actionView.setTextColor(Prefs.accentColor)
        //only set if previous text colors are set
        view.setBackgroundColor(Prefs.bgColor.withAlpha(255).colorToForeground(0.1f))
    }
}

fun Activity.flashNavigationBar() {
    navigationBarColor = if (Prefs.tintNavBar) Prefs.headerColor else Color.BLACK
}

@Throws(IOException::class)
fun createMediaFile(extension: String) = createMediaFile("Flash", extension)

@Throws(IOException::class)
fun Context.createPrivateMediaFile(extension: String) = createPrivateMediaFile("Flash", extension)

/**
 * Tries to send the uri to the proper activity via an intent
 * returns [true] if activity is resolved, [false] otherwise
 * For safety, any uri that [isFacebookUrl] without [isExplicitIntent] will return [false]
 */
fun Context.resolveActivityForUri(uri: Uri): Boolean {
    val url = uri.toString()
    if (url.isFacebookUrl && !url.isExplicitIntent) return false
    val intent = Intent(Intent.ACTION_VIEW, uri)
    if (intent.resolveActivity(packageManager) == null) return false
    startActivity(intent)
    return true
}

/**
 * [true] if url contains [FACEBOOK_COM]
 */
inline val String?.isFacebookUrl
    get() = this != null && (contains(FACEBOOK_COM) || contains(FBCDN_NET))

/**
 * [true] if url is a video and can be accepted by VideoViewer
 */
inline val String.isVideoUrl
    get() = startsWith(VIDEO_REDIRECT) || (startsWith("https://video-") && contains(FBCDN_NET)) || startsWith(WWW_VIDEO_REDIRECT) && contains("video_source=user_video_tab")

/**
 * [true] if url directly leads to a usable image
 */
inline val String.isImageUrl: Boolean
    get() {
        return contains(FBCDN_NET) && (contains(".png") || contains(".jpg"))
    }

/**
 * [true] if url can be retrieved to get a direct image url
 */
inline val String.isIndirectImageUrl: Boolean
    get() {
        return contains("/photo/view_full_size/") && contains("fbid=")
    }
/**
 * [true] if url can be displayed in a different webview
 */
inline val String?.isIndependent: Boolean
    get() {
        if (this == null || length < 5) return false            // ignore short queries
        if (this[0] == '#' && !contains('/')) return false      // ignore element values
        if (startsWith("http") && !isFacebookUrl) return true   // ignore non facebook urls
        if (dependentSet.any { contains(it) }) return false     // ignore known dependent segments
        return true
    }

val dependentSet = setOf(
        "photoset_token",
        "direct_action_execute",
        "events/permalink",
        "events/feed/watch",
        "messages/?pageNum",
        "sharer.php",
        "add_friend.php",
        "/pymk/xout/",
        "/place/review/xout/",
        "/review/xout/",
        /*
         * Add new members to groups
         */
        "madminpanel",
        /*
         * Editing images
         */
        "/confirmation/?",
        "&p=",
        /*
         * Facebook messages have the following cases for the tid query
         * mid* or id* for newer threads, which can be launched in new windows
         * or a hash for old threads, which must be loaded on old threads
         */
        "messages/read/?tid=id",
        "messages/read/?tid=mid"
)

inline val String?.isExplicitIntent
    get() = this != null && startsWith("intent://")

fun Context.flashChangelog() = showChangelog(R.xml.flash_changelog, Prefs.textColor) {
    theme()
    if (System.currentTimeMillis() - Prefs.installDate > 864000000) { //show after 10 days
        neutralText(R.string.kau_rate)
        onNeutral { _, _ -> startPlayStoreLink(R.string.play_store_package_id) }
    }
}

fun Context.flashNewChangelog() = showChangelog(R.xml.flash_changelog, Prefs.textColor) {
    theme()
}

/**
 * Show Why Flash Pro
 */
fun Context.flashWhyPro() =
        showChangelog(R.xml.flash_why_pro, Prefs.textColor) {
            title(R.string.why_pro)
    theme()
}

fun Context.flashUriFromFile(file: File): Uri =
        FileProvider.getUriForFile(this,
                BuildConfig.APPLICATION_ID + ".provider",
                file)

inline fun Context.sendFlashEmail(@StringRes subjectId: Int, crossinline builder: EmailBuilder.() -> Unit) = sendFlashEmail(string(subjectId), builder)

inline fun Context.sendFlashEmail(subjectId: String, crossinline builder: EmailBuilder.() -> Unit) = sendEmail(string(R.string.dev_email), subjectId) {
    builder()
    addFlashDetails()
}

fun EmailBuilder.addFlashDetails() {
    addItem("Prev version", Prefs.prevVersionCode.toString())
    val proTag = if (IS_Flash_PRO) "TY" else "FP"
    addItem("Random Flash ID", "${Prefs.FlashId}-$proTag")
    addItem("Locale", Locale.getDefault().displayName)
}

fun flashJsoup(url: String) = flashJsoup(FbCookie.webCookie, url)

fun flashJsoup(cookie: String?, url: String) = Jsoup.connect(url).cookie(FACEBOOK_COM, cookie).userAgent(USER_AGENT_BASIC_JSOUP).get()!!

fun Element.first(vararg select: String): Element? {
    select.forEach {
        val e = select(it)
        if (e.size > 0) return e.first()
    }
    return null
}

fun File.createFreshFile(): Boolean {
    if (exists()) {
        if (!delete()) return false
    } else {
        val parent = parentFile
        if (!parent.exists() && !parent.mkdirs())
            return false
    }
    return createNewFile()
}

fun File.createFreshDir(): Boolean {
    if (exists() && !deleteRecursively())
        return false
    return mkdirs()
}

fun String.unescapeHtml(): String =
        StringEscapeUtils.unescapeXml(this)
                .replace("\\u003C", "<")
                .replace("\\\"", "\"")