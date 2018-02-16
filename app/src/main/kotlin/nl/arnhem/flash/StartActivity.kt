package nl.arnhem.flash

import android.os.Bundle
import ca.allanwang.kau.internal.KauBaseActivity
import ca.allanwang.kau.utils.startActivity
import nl.arnhem.flash.activities.LoginActivity
import nl.arnhem.flash.activities.MainActivity
import nl.arnhem.flash.activities.SelectorActivity
import nl.arnhem.flash.dbflow.loadFbCookiesAsync
import nl.arnhem.flash.facebook.FbCookie
import nl.arnhem.flash.utils.EXTRA_COOKIES
import nl.arnhem.flash.utils.L
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.launchNewTask

/**
 * Created by Allan Wang on 2017-05-28.
 */
class StartActivity : KauBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FbCookie.switchBackUser {
            loadFbCookiesAsync {
                val cookies = ArrayList(it)
                L.i { "Cookies loaded at time ${System.currentTimeMillis()}" }
                L._d { "Cookies: ${cookies.joinToString("\t")}" }
                if (cookies.isNotEmpty()) {
                    if (Prefs.userId != -1L)
                        startActivity<MainActivity>(intentBuilder = {
                            putParcelableArrayListExtra(EXTRA_COOKIES, cookies)
                            //flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    else
                        launchNewTask<SelectorActivity>(cookies)
                } else
                    launchNewTask<LoginActivity>()
            }
        }
    }
}