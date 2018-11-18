package nl.arnhem.flash

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import ca.allanwang.kau.internal.KauBaseActivity
import ca.allanwang.kau.utils.buildIsLollipopAndUp
import ca.allanwang.kau.utils.setIcon
import ca.allanwang.kau.utils.startActivity
import ca.allanwang.kau.utils.string
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import nl.arnhem.flash.activities.LoginActivity
import nl.arnhem.flash.activities.MainActivity
import nl.arnhem.flash.activities.SelectorActivity
import nl.arnhem.flash.dbflow.loadFbCookiesAsync
import nl.arnhem.flash.facebook.FbCookie
import nl.arnhem.flash.utils.*
import java.util.*


/**
 * Created by Allan Wang on 2017-05-28.
 */
class StartActivity : KauBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!buildIsLollipopAndUp) { // not supported
            showInvalidSdkView()
            return
        }

        try {
            FbCookie.switchBackUser {
                loadFbCookiesAsync {
                    val cookies = ArrayList(it)
                    L.i { "Cookies loaded at time ${System.currentTimeMillis()}" }
                    L._d { "Cookies: ${cookies.joinToString("\t")}" }
                    if (cookies.isNotEmpty()) {
                        if (Prefs.userId != -1L)
                            startActivity<MainActivity>(intentBuilder = {
                                putExtras(fadeBundle())
                                putParcelableArrayListExtra(EXTRA_COOKIES, cookies)
                            })
                        else
                            launchNewTask<SelectorActivity>(cookies)
                    } else
                        launchNewTask<LoginActivity>()
                }
            }
        } catch (e: Exception) {
            showInvalidWebView()
        }

    }

    private fun showInvalidWebView() =
            showInvalidView(R.string.error_webview)

    private fun showInvalidSdkView() {
        val text = try {
            String.format(getString(R.string.error_sdk), Build.VERSION.SDK_INT)
        } catch (e: IllegalFormatException) {
            string(R.string.error_sdk)
        }
        showInvalidView(text)
    }

    private fun showInvalidView(textRes: Int) =
            showInvalidView(string(textRes))

    private fun showInvalidView(text: String) {
        setContentView(R.layout.activity_invalid)
        findViewById<ImageView>(R.id.invalid_icon)
                .setIcon(GoogleMaterial.Icon.gmd_adb, -1, Color.WHITE)
        findViewById<TextView>(R.id.invalid_text).text = text
    }
}