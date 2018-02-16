package nl.arnhem.flash.fragments

import ca.allanwang.kau.adapters.fastAdapter
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter_extensions.items.ProgressItem
import nl.arnhem.flash.R
import nl.arnhem.flash.facebook.FbCookie
import nl.arnhem.flash.parsers.FlashParser
import nl.arnhem.flash.parsers.ParseResponse
import nl.arnhem.flash.utils.L
import nl.arnhem.flash.utils.flashJsoup
import nl.arnhem.flash.views.FlashRecyclerView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * Created by Allan Wang on 27/12/17.
 */
abstract class RecyclerFragment : BaseFragment(), RecyclerContentContract {

    override val layoutRes: Int = R.layout.view_content_recycler

    override fun firstLoadRequest() {
        val core = core ?: return
        if (firstLoad) {
            core.reloadBase(true)
            firstLoad = false
        }
    }

    final override fun reload(progress: (Int) -> Unit, callback: (Boolean) -> Unit) {
        reloadImpl(progress) {
            if (it)
                callback(it)
            else
                valid = false
        }
    }

    protected abstract fun reloadImpl(progress: (Int) -> Unit, callback: (Boolean) -> Unit)
}

abstract class GenericRecyclerFragment<T, Item : IItem<*, *>> : RecyclerFragment() {

    abstract fun mapper(data: T): Item

    val adapter: ModelAdapter<T, Item> = ModelAdapter(this::mapper)

    final override fun bind(recyclerView: FlashRecyclerView) {
        recyclerView.adapter = getAdapter()
        recyclerView.onReloadClear = { adapter.clear() }
        bindImpl(recyclerView)
    }

    /**
     * Anything to call for one time bindings
     * At this stage, all adapters will have FastAdapter references
     */
    open fun bindImpl(recyclerView: FlashRecyclerView) = Unit

    /**
     * Create the fast adapter to bind to the recyclerview
     */
    open fun getAdapter(): FastAdapter<IItem<*, *>> = fastAdapter(this.adapter)

}

abstract class FlashParserFragment<T : Any, Item : IItem<*, *>> : RecyclerFragment() {

    /**
     * The parser to make this all happen
     */
    abstract val parser: FlashParser<T>

    open fun getDoc(cookie: String?) = flashJsoup(cookie, parser.url)

    abstract fun toItems(response: ParseResponse<T>): List<Item>

    val adapter: ItemAdapter<Item> = ItemAdapter()

    final override fun bind(recyclerView: FlashRecyclerView) {
        recyclerView.adapter = getAdapter()
        recyclerView.onReloadClear = { adapter.clear() }
        bindImpl(recyclerView)
    }

    /**
     * Anything to call for one time bindings
     * At this stage, all adapters will have FastAdapter references
     */
    open fun bindImpl(recyclerView: FlashRecyclerView) = Unit

    /**
     * Create the fast adapter to bind to the recyclerview
     */
    open fun getAdapter(): FastAdapter<IItem<*, *>> = fastAdapter(this.adapter)

    override fun reloadImpl(progress: (Int) -> Unit, callback: (Boolean) -> Unit) {
        doAsync {
            progress(10)
            val cookie = FbCookie.webCookie
            val doc = getDoc(cookie)
            progress(60)
            val response = parser.parse(cookie, doc)
            if (response == null) {
                L.i { "RecyclerFragment failed for ${baseEnum.name}" }
                return@doAsync callback(false)
            }
            progress(80)
            val items = toItems(response)
            progress(97)
            uiThread { adapter.setNewList(items) }
            callback(true)
        }
    }
}

//abstract class PagedRecyclerFragment<T : Any, Item : IItem<*, *>> : RecyclerFragment<T, Item>() {
//
//    var allowPagedLoading = true
//
//    val footerAdapter = ItemAdapter<FlashProgress>()
//
//    val footerScrollListener = object : EndlessRecyclerOnScrollListener(footerAdapter) {
//        override fun onLoadMore(currentPage: Int) {
//            TODO("not implemented")
//
//        }
//
//    }
//
//    override fun getAdapter() = fastAdapter(adapter, footerAdapter)
//
//    override fun bindImpl(recyclerView: FlashRecyclerView) {
//        recyclerView.addOnScrollListener(footerScrollListener)
//    }
//
//    override fun reload(progress: (Int) -> Unit, callback: (Boolean) -> Unit) {
//        footerScrollListener.
//        super.reload(progress, callback)
//    }
//}

class FastProgress : ProgressItem()