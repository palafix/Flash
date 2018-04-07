package nl.arnhem.flash.activities

import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.TextView
import ca.allanwang.kau.swipe.kauSwipeOnCreate
import ca.allanwang.kau.swipe.kauSwipeOnDestroy
import ca.allanwang.kau.utils.*
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.GravityEnum
import com.mikepenz.actionitembadge.library.ActionItemBadge
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import io.realm.Realm
import kotlinx.android.synthetic.main.history_layout.*
import nl.arnhem.flash.R
import nl.arnhem.flash.adapter.HistoryAdapter
import nl.arnhem.flash.enums.OverlayContext
import nl.arnhem.flash.model.HistoryModel
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.launchWebOverlayBasic
import nl.arnhem.flash.utils.materialDialogThemed
import nl.arnhem.flash.utils.setFlashColors
import kotlin.properties.Delegates


@Suppress("DEPRECATION")
/**
 * Created by meeera on 6/10/17.Edited by Robin Bouwman for this app on 1/1/18
 **/
class HistoryActivity : AppCompatActivity(), HistoryAdapter.OnItemClicked {

    override fun onItemClick(data: String?) {
        launchWebOverlayBasic(data.toString())
        finish()
    }

    val toolbar: Toolbar by bindView(R.id.history_toolbar)
    val coordinator: CoordinatorLayout by bindView(R.id.overlay_history_content)
    val text: TextView by bindView(R.id.textViewHistory)

    private val overlayContext: OverlayContext?
        get() = OverlayContext[intent.extras]

    var realm: Realm by Delegates.notNull()
    var adapter: HistoryAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_layout)
        showWindow()
        realm = Realm.getDefaultInstance()
        adapter = HistoryAdapter(this, this, CustomTabs().getHistory(realm), true)
        recyclerhistory.adapter = adapter
        recyclerhistory.layoutManager = LinearLayoutManager(this)
        setSupportActionBar(toolbar)
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
        val params = window.attributes
        params.alpha = 1.0f
        params.dimAmount = 0.4f
        window.attributes = params
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x
        val height = size.y
        if (height > width) {
            window.setLayout((width * .9).toInt(), (height * .9).toInt())
        } else {
            window.setLayout((width * .9).toInt(), (height * .9).toInt())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val badgeCount = realm.where(HistoryModel::class.java).findAll().size
        menuInflater.inflate(R.menu.history_menu, menu)
        overlayContext?.onMenuCreate(this, menu)
        toolbar.tint(Prefs.iconColor)
        if (badgeCount > 0) {
            ActionItemBadge.update(this, menu.findItem(R.id.simple_delete), GoogleMaterial.Icon.gmd_delete_forever.toDrawable(this, 19, Prefs.iconColor), ActionItemBadge.BadgeStyles.GREY, badgeCount)
        } else {
            ActionItemBadge.hide(menu.findItem(R.id.simple_delete))
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.simple_delete -> {
                val dimmerTextColor = Prefs.textColor.adjustAlpha(0.8f)
                materialDialogThemed {
                    title(R.string.remove_history)
                    titleColor(Prefs.textColor)
                    titleGravity(GravityEnum.CENTER)
                    backgroundColor(Prefs.bgColor.lighten(0.1f).withMinAlpha(200))
                    dividerColor(Prefs.notiColor)
                    iconRes(R.drawable.ic_warning)
                    content(context.resources.getString(R.string.remove_history_desc))
                    contentColor(dimmerTextColor)
                    widgetColor(dimmerTextColor)
                    positiveText(R.string.kau_ok)
                    positiveColor(Prefs.textColor)
                    negativeText(R.string.kau_cancel)
                    negativeColor(Prefs.textColor)
                    btnSelector(R.drawable.md_btn_selector_custom, DialogAction.POSITIVE)
                    buttonRippleColor(Prefs.iconColor)
                    buttonsGravity(GravityEnum.CENTER)
                    onPositive { _, _ ->
                        realm.executeTransaction {
                            realm.delete(HistoryModel::class.java)
                        }
                        reStart(context)
                    }
                    onNegative({ _, _ -> })
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun reStart(ctx: Context) {
        ctx as HistoryActivity
        ctx.recreate()
    }

    override fun onResume() {
        super.onResume()
        realm.refresh()
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