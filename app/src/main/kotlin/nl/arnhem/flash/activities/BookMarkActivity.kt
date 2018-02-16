package nl.arnhem.flash.activities

import android.graphics.Point
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.WindowManager
import android.widget.TextView
import ca.allanwang.kau.swipe.kauSwipeOnCreate
import ca.allanwang.kau.swipe.kauSwipeOnDestroy
import ca.allanwang.kau.utils.*
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import io.realm.Realm
import kotlinx.android.synthetic.main.bookmark_layout.*
import nl.arnhem.flash.R
import nl.arnhem.flash.adapter.BookmarkAdapter
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.launchWebOverlay
import nl.arnhem.flash.utils.setFlashColors
import kotlin.properties.Delegates

/**
 * Created by meeera on 6/10/17.
 **/
class BookMarkActivity : AppCompatActivity(), BookmarkAdapter.onItemClicked {

    override fun onItemClick(data: String?) {
        launchWebOverlay(data.toString())
        finish()
    }

    val toolbar: Toolbar by bindView(R.id.book_toolbar)
    var realm: Realm by Delegates.notNull()
    var adapter: BookmarkAdapter? = null
    val coordinator: CoordinatorLayout by bindView(R.id.overlay_bookmark_content)
    val text: TextView by bindView(R.id.textView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWindow()
        setContentView(R.layout.bookmark_layout)
        realm = Realm.getDefaultInstance()
        adapter = BookmarkAdapter(this, this, WebOverlayActivity().getBookmark(realm), true)
        recyclerbookmark.adapter = adapter
        recyclerbookmark.layoutManager = LinearLayoutManager(this)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon = GoogleMaterial.Icon.gmd_close.toDrawable(this, 16, Prefs.iconColor)
        toolbar.setNavigationOnClickListener { finishSlideOut() }
        toolbar.setBackgroundColor(Prefs.headerColor.withAlpha(255))
        text.setTextColor(Prefs.iconColor)
        coordinator.setBackgroundColor(Prefs.bgColor)
        setFlashColors {
            toolbar(toolbar)
            themeWindow = false
        }

        kauSwipeOnCreate {
            if (!Prefs.overlayFullScreenSwipe) edgeSize = 20.dpToPx
            transitionSystemBars = false
        }
    }

    private fun showWindow() {
        window.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        val params = window.attributes
        params.alpha = 1.0f
        params.dimAmount = 0.5f
        window.attributes = params
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x
        val height = size.y
        if (height > width) {
            window.setLayout((width * .9).toInt(), (height * 0.9).toInt())
        } else {
            window.setLayout((width * .9).toInt(), (height * 0.9).toInt())
        }
        run({
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        kauSwipeOnDestroy()
        realm.close()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishSlideOut()
    }
}