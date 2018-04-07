@file:Suppress("KDocUnresolvedReference")

package nl.arnhem.flash.contracts


import com.mikepenz.iconics.typeface.IIcon
import io.reactivex.subjects.PublishSubject
import nl.arnhem.flash.fragments.BaseFragment

/**
 * All the contracts for [MainActivity]
 */
interface ActivityContract : FileChooserActivityContract

interface MainActivityContract : ActivityContract, MainFabContract {
    val fragmentSubject: PublishSubject<Int>
    fun setTitle(res: Int)
    fun setTitle(text: CharSequence)
    /**
     * Available on all threads
     */
    fun collapseAppBar()

    fun reloadFragment(fragment: BaseFragment)
}

interface MainFabContract {
    fun showFab(iicon: IIcon, clickEvent: () -> Unit)
    fun hideFab()
}