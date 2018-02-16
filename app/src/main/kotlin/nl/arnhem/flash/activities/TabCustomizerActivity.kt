package nl.arnhem.flash.activities

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import ca.allanwang.kau.kotlin.lazyContext
import ca.allanwang.kau.utils.bindView
import ca.allanwang.kau.utils.scaleXY
import ca.allanwang.kau.utils.setIcon
import ca.allanwang.kau.utils.withAlpha
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.mikepenz.fastadapter_extensions.drag.ItemTouchCallback
import com.mikepenz.fastadapter_extensions.drag.SimpleDragCallback
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import nl.arnhem.flash.R
import nl.arnhem.flash.dbflow.TAB_COUNT
import nl.arnhem.flash.dbflow.loadFbTabs
import nl.arnhem.flash.dbflow.save
import nl.arnhem.flash.facebook.FbItem
import nl.arnhem.flash.iitems.TabIItem
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.setFlashColors
import java.util.*

/**
 * Created by Allan Wang on 26/11/17.
 **/
class TabCustomizerActivity : BaseActivity() {

    val toolbar: View by bindView(R.id.pseudo_toolbar)
    val recycler: RecyclerView by bindView(R.id.tab_recycler)
    val instructions: TextView by bindView(R.id.instructions)
    val divider: View by bindView(R.id.divider)
    val adapter = FastItemAdapter<TabIItem>()
    val fabCancel: FloatingActionButton by bindView(R.id.fab_cancel)
    val fabSave: FloatingActionButton by bindView(R.id.fab_save)

    private val wobble = lazyContext { AnimationUtils.loadAnimation(it, R.anim.rotate_delta) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tab_customizer)

        toolbar.setBackgroundColor(Prefs.headerColor)

        recycler.layoutManager = GridLayoutManager(this, TAB_COUNT, GridLayoutManager.VERTICAL, false)
        recycler.adapter = adapter
        recycler.setHasFixedSize(true)

        divider.setBackgroundColor(Prefs.textColor.withAlpha(30))
        instructions.setTextColor(Prefs.textColor)

        val tabs = loadFbTabs().toMutableList()
        val remaining = FbItem.values().filter { it.name[0] != '_' }.toMutableList()
        remaining.removeAll(tabs)
        tabs.addAll(remaining)

        adapter.add(tabs.map(::TabIItem))
        bindSwapper(adapter, recycler)

        adapter.withOnClickListener { view, _, _, _ -> view.wobble(); true }

        setResult(Activity.RESULT_CANCELED)

        fabSave.setIcon(GoogleMaterial.Icon.gmd_check, Prefs.iconColor)
        fabSave.backgroundTintList = ColorStateList.valueOf(Prefs.accentColor)
        fabSave.setOnClickListener {
            adapter.adapterItems.subList(0, TAB_COUNT).map(TabIItem::item).save()
            setResult(Activity.RESULT_OK)
            finish()
        }
        fabCancel.setIcon(GoogleMaterial.Icon.gmd_close, Prefs.iconColor)
        fabCancel.backgroundTintList = ColorStateList.valueOf(Prefs.accentColor)
        fabCancel.setOnClickListener { finish() }
        setFlashColors {
            themeWindow = true
        }
    }

    private fun View.wobble() = startAnimation(wobble(context))

    private fun bindSwapper(adapter: FastItemAdapter<*>, recycler: RecyclerView) {
        val dragCallback = TabDragCallback(SimpleDragCallback.ALL, swapper(adapter))
        ItemTouchHelper(dragCallback).attachToRecyclerView(recycler)
    }

    private fun swapper(adapter: FastItemAdapter<*>) = object : ItemTouchCallback {
        override fun itemTouchOnMove(oldPosition: Int, newPosition: Int): Boolean {
            Collections.swap(adapter.adapterItems, oldPosition, newPosition)
            adapter.notifyAdapterDataSetChanged()
            return true
        }

        override fun itemTouchDropped(oldPosition: Int, newPosition: Int) = Unit
    }


    private class TabDragCallback(
            directions: Int, itemTouchCallback: ItemTouchCallback
    ) : SimpleDragCallback(directions, itemTouchCallback) {

        private var draggingView: TabIItem.ViewHolder? = null

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            when (actionState) {
                ItemTouchHelper.ACTION_STATE_DRAG -> {
                    (viewHolder as? TabIItem.ViewHolder)?.apply {
                        draggingView = this
                        itemView.animate().scaleXY(1.3f)
                        text.animate().alpha(0f)
                    }
                }
                ItemTouchHelper.ACTION_STATE_IDLE -> {
                    draggingView?.apply {
                        itemView.animate().scaleXY(1f)
                        text.animate().alpha(1f)
                    }
                    draggingView = null
                }
            }
        }

    }

}
