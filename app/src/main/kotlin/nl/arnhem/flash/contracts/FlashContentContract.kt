package nl.arnhem.flash.contracts

import android.view.View
import nl.arnhem.flash.facebook.FbItem
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

/**
 * Created by Allan Wang on 20/12/17.
 **/

/**
 * Contract for the underlying parent,
 * binds to activities & fragments
 */
interface FlashContentContainer {

    val baseUrl: String

    val baseEnum: FbItem?

    /**
     * Update toolbar title
     */
    fun setTitle(title: String)

}

/**
 * Contract for components shared among
 * all content providers
 */
interface FlashContentParent : DynamicUiContract {

    val core: FlashContentCore

    /**
     * Observable to get data on whether view is refreshing or not
     */
    val refreshObservable: PublishSubject<Boolean>

    /**
     * Observable to get data on refresh progress, with range [0, 100]
     */
    val progressObservable: PublishSubject<Int>

    /**
     * Observable to get new title data (unique values only)
     */
    val titleObservable: BehaviorSubject<String>

    var baseUrl: String

    var baseEnum: FbItem?

    /**
     * Toggle state for allowing swipes
     */
    var swipeEnabled: Boolean

    /**
     * Binds the container to self
     * this will also handle all future bindings
     * Must be called by container!
     */
    fun bind(container: FlashContentContainer)

    /**
     * Signal that the contract will not be used again
     * Clean up resources where applicable
     */
    fun destroy()

    /**
     * Hook onto the refresh observable for one cycle
     * Animate toggles between the fancy ripple and the basic fade
     * The cycle only starts on the first load since
     * there may have been another process when this is registered
     *
     *
     * Returns true to proceed with load
     * In some cases when the url has not changed,
     * it may not be advisable to proceed with the load
     * For those cases, we will return false to stop it
     */

    fun registerTransition(urlChanged: Boolean, animate: Boolean): Boolean

}

/**
 * Underlying contract for the content itself
 */
interface FlashContentCore : DynamicUiContract {

    /**
     * Reference to parent
     * Bound through calling [FlashContentParent.bind]
     */
    var parent: FlashContentParent

    /**
     * Initializes view through given [container]
     *
     * The content may be free to extract other data from
     * the container if necessary
     *
     * [parent] must be bounded before calling this!
     */
    fun bind(container: FlashContentContainer): View

    /**
     * Call to reload wrapped data
     */
    fun reload(animate: Boolean)

    /**
     * Call to reload base data
     */
    fun reloadBase(animate: Boolean)

    /**
     * If possible, remove anything in the view stack
     * Applies namely to webviews
     */
    fun clearHistory()

    /**
     * Should be called when a back press is triggered
     * Return [true] if consumed, [false] otherwise
     */
    fun onBackPressed(): Boolean

    val currentUrl: String

    /**
     * Condition to help pause certain background resources
     */
    var active: Boolean

    /**
     * Triggered when view is within viewpager
     * and tab is clicked
     */
    fun onTabClicked()

    /**
     * Signal destruction to release some content manually
     */
    fun destroy()

}