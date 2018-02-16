package nl.arnhem.flash.utils

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.support.annotation.WorkerThread
import android.text.TextUtils
import android.webkit.WebResourceResponse
import okhttp3.HttpUrl
import okio.Okio
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.*


object FlashAdBlock : AdBlocker("adblock.txt")

object FlashPglAdBlock : AdBlocker("pgl.yoyo.org.txt")


open class AdBlocker(val assetPath: String) {
    val AD_HOSTS_FILE = "adblock.txt"
    val AD_HOSTS = HashSet<String>()
    @SuppressLint("StaticFieldLeak")
    fun init(context: Context) {
        object : AsyncTask<Void, Void, Void>() {
            val content = context.assets.open(assetPath).bufferedReader().use { it.readLines().filter { !it.startsWith("#") } }
            override fun doInBackground(vararg params: Void): Void? {
                try {
                    loadFromAssets(context)
                } catch (e: IOException) {
                    // noop
                }
                return null
            }
        }.execute()
    }

    @WorkerThread
    @Throws(IOException::class)
    fun loadFromAssets(context: Context) {
        val stream = context.assets.open(AD_HOSTS_FILE)
        val buffer = Okio.buffer(Okio.source(stream))
        var line: String? = null
        while ({ line = buffer.readUtf8Line(); line }() != null) {
            AD_HOSTS.add(line!!)
        }
        buffer.close()
        stream.close()
    }

    fun isAd(url: String): Boolean {
        val httpUrl = HttpUrl.parse(url)
        return isAdHost(if (httpUrl != null) httpUrl.host() else "")
    }

    fun isAdHost(host: String): Boolean {
        if (TextUtils.isEmpty(host)) {
            return false
        }
        val index = host.indexOf(".")
        return (index >= 0 && ((AD_HOSTS.contains(host) || index + 1 < host.length && isAdHost(host.substring(index + 1)))))
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    fun createEmptyResource(): WebResourceResponse {
        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
    }
}