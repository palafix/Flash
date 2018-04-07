@file:Suppress("DEPRECATION")

package nl.arnhem.flash.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import ca.allanwang.kau.permissions.PERMISSION_WRITE_EXTERNAL_STORAGE
import ca.allanwang.kau.permissions.kauRequestPermissions
import ca.allanwang.kau.utils.isAppEnabled
import ca.allanwang.kau.utils.showAppInfo
import ca.allanwang.kau.utils.string
import ca.allanwang.kau.utils.toast
import nl.arnhem.flash.R
import nl.arnhem.flash.dbflow.loadFbCookie
import nl.arnhem.flash.facebook.USER_AGENT_BASIC


/**
 * Created by Allan Wang on 2017-08-04.
 * This is For Facebook video download
 * With reference to <a href="https://stackoverflow.com/questions/33434532/android-webview-download-files-like-browsers-do">Stack Overflow</a>
 */
fun Context.flashDownload(url: String?, userAgent: String = USER_AGENT_BASIC, contentDisposition: String? = null, mimeType: String? = null, contentLength: Long = 0L) {
    url ?: return
    flashDownload(Uri.parse(url), userAgent, contentDisposition, mimeType, contentLength)
}

fun Context.flashDownload(uri: Uri?, userAgent: String = USER_AGENT_BASIC, contentDisposition: String? = null, mimeType: String? = null, contentLength: Long = 0L) {
    uri ?: return
    L.d { "Received download request" }
    if (uri.scheme != "http" && uri.scheme != "https") {
        toast(R.string.error_invalid_download)
        return L.e { "Invalid download $uri" }
    }
    if (!isAppEnabled(DOWNLOAD_MANAGER_PACKAGE)) {
        materialDialogThemed {
            title(R.string.no_download_manager)
            content(R.string.no_download_manager_desc)
            positiveText(R.string.kau_yes)
            onPositive { _, _ -> showAppInfo(DOWNLOAD_MANAGER_PACKAGE) }
            negativeText(R.string.kau_no)
        }
        return
    }
    kauRequestPermissions(PERMISSION_WRITE_EXTERNAL_STORAGE) { granted, _ ->
        if (!granted) return@kauRequestPermissions
        val request = DownloadManager.Request(uri)
        request.setMimeType(mimeType)
        val cookie = loadFbCookie(Prefs.userId) ?: return@kauRequestPermissions
        val filename = URLUtil.guessFileName(uri.toString(), contentDisposition, mimeType)
        request.addRequestHeader("Cookie", cookie.cookie)
        request.addRequestHeader("User-Agent", userAgent)
        request.setDescription(string(R.string.flash_name))
        request.setTitle(filename)
        request.allowScanningByMediaScanner()
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Flash/Flash Media/Flash_$filename")
        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        try {
            dm.enqueue(request)
        } catch (e: Exception) {
            toast(R.string.error_generic)
            L.e(e) { "Download" }
        }
        registerReceiver(onComplete(filename), IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }
}

/**
 *
 * This is For Chrome Custom Tabs Downloads
 *
 */

fun Context.flashChromeDownload(url: String, userAgent: String? = null, contentDisposition: String? = null, mimeType: String? = null, contentLength: Long = 0L) {
    if (!isAppEnabled(DOWNLOAD_MANAGER_PACKAGE)) {
        materialDialogThemed {
            title(R.string.no_download_manager)
            content(R.string.no_download_manager_desc)
            positiveText(R.string.kau_yes)
            onPositive { _, _ -> showAppInfo(DOWNLOAD_MANAGER_PACKAGE) }
            negativeText(R.string.kau_no)
        }
        return
    }
    kauRequestPermissions(PERMISSION_WRITE_EXTERNAL_STORAGE) { granted, _ ->
        if (!granted) return@kauRequestPermissions
        val cookies = CookieManager.getInstance().getCookie(url)
        val request = DownloadManager.Request(Uri.parse(url))
        val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
        if (url.contains(".mp4") || url.contains(".webm") || url.contains(".avi") || url.contains(".mkv")
                || url.contains(".mpg") || url.contains(".flv") || url.contains(".swv") || url.contains(".rm")
                || url.contains(".wmv") || url.contains(".mov") || url.contains(".m4v")) {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Flash/Flash Media/Flash_$filename")
        } else
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Flash/Flash Documents/Flash_$filename")
        request.addRequestHeader("cookie", cookies)
        //------------------------COOKIE!!------------------------
        request.addRequestHeader("User-Agent", userAgent)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setAllowedOverRoaming(true)
        request.setTitle(filename)
        request.setDescription(string(R.string.flash_name))
        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        try {
            dm.enqueue(request)
        } catch (e: Exception) {
            toast(R.string.error_generic)
            L.e(e) { "Download" }
        }
        registerReceiver(onComplete(filename), IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }
}

/**
 *
 * This is For Chrome Custom Tabs Context Menu Image Downloads
 *
 */

fun Context.flashContextDownload(url: String, userAgent: String? = null, contentDisposition: String? = null, mimeType: String? = null, contentLength: Long = 0L) {
    if (!Sharer.resolve(this)) {
        mPendingImageUrlToSave = null
        return
    }
    if (!isAppEnabled(DOWNLOAD_MANAGER_PACKAGE)) {
        materialDialogThemed {
            title(R.string.no_download_manager)
            content(R.string.no_download_manager_desc)
            positiveText(R.string.kau_yes)
            onPositive { _, _ -> showAppInfo(DOWNLOAD_MANAGER_PACKAGE) }
            negativeText(R.string.kau_no)
        }
        return
    }
    kauRequestPermissions(PERMISSION_WRITE_EXTERNAL_STORAGE) { granted, _ ->
        if (!granted) return@kauRequestPermissions
        val dm = this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(url)
        val request = DownloadManager.Request(downloadUri)
        // default image extension
        val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Flash/Flash Images/Flash_$filename")
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setAllowedOverRoaming(true)
        request.setTitle(filename)
        request.setDescription(string(R.string.flash_name))
        try {
            dm.enqueue(request)
        } catch (e: Exception) {
            toast(R.string.error_generic)
            L.e(e) { "Download" }
        }
        registerReceiver(onComplete(filename), IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }
}


var mPendingImageUrlToSave: String? = null
private const val DOWNLOAD_MANAGER_PACKAGE = "com.android.providers.downloads"