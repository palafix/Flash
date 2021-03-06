@file:Suppress("DEPRECATION")

package nl.arnhem.flash.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.Toolbar
import ca.allanwang.kau.internal.KauBaseActivity
import ca.allanwang.kau.utils.*
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import nl.arnhem.flash.R
import nl.arnhem.flash.facebook.FbItem
import nl.arnhem.flash.injectors.JsActions
import nl.arnhem.flash.utils.L
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.createFreshDir
import nl.arnhem.flash.utils.setFlashColors
import nl.arnhem.flash.web.DebugWebView
import java.io.File

/**
 * Created by Allan Wang on 05/01/18.
 **/
class DebugActivity : KauBaseActivity() {

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val web: DebugWebView by bindView(R.id.debug_webview)
    private val swipeRefresh: SwipeRefreshLayout by bindView(R.id.swipe_refresh)
    private val fab: FloatingActionButton by bindView(R.id.fab)

    companion object {
        const val RESULT_URL = "extra_result_url"
        const val RESULT_SCREENSHOT = "extra_result_screenshot"
        const val RESULT_BODY = "extra_result_body"
        fun baseDir(context: Context) = File(context.externalCacheDir, "offline_debug")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        setTitle(R.string.debug_flash)
        toolbar.navigationIcon = GoogleMaterial.Icon.gmd_close.toDrawable(this, 16, Prefs.iconColor)
        toolbar.setNavigationOnClickListener { finishSlideOut() }
        setFlashColors {
            toolbar(toolbar)
        }
        web.loadUrl(FbItem.FEED.url)
        web.onPageFinished = { swipeRefresh.isRefreshing = false }

        swipeRefresh.setOnRefreshListener(web::reload)

        fab.visible().setIcon(GoogleMaterial.Icon.gmd_bug_report, 16, Prefs.iconColor)
        fab.backgroundTintList = ColorStateList.valueOf(Prefs.accentColor)
        fab.setOnClickListener { _ ->
            fab.hide()

            val parent = baseDir(this)
            parent.createFreshDir()
            val rxScreenshot = Single.fromCallable {
                web.getScreenshot(File(parent, "screenshot.png"))
            }.subscribeOn(Schedulers.io())
            val rxBody = Single.create<String> { emitter ->
                web.evaluateJavascript(JsActions.RETURN_BODY.function) {
                    emitter.onSuccess(it)
                }
            }.subscribeOn(AndroidSchedulers.mainThread())
            Single.zip(listOf(rxScreenshot, rxBody)) {
                val screenshot = it[0] == true
                val body = it[1] as? String
                screenshot to body
            }.observeOn(AndroidSchedulers.mainThread())
                    .subscribe { (screenshot, body), err ->
                        if (err != null) {
                            L.e { "DebugActivity error ${err.message}" }
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                            return@subscribe
                        }
                        val intent = Intent()
                        intent.putExtra(RESULT_URL, web.url)
                        intent.putExtra(RESULT_SCREENSHOT, screenshot)
                        if (body != null)
                            intent.putExtra(RESULT_BODY, body)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        web.resumeTimers()
    }

    override fun onPause() {
        web.pauseTimers()
        super.onPause()
    }

    override fun onBackPressed() {
        if (web.canGoBack())
            web.goBack()
        else
            super.onBackPressed()
    }
}