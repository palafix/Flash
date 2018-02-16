package nl.arnhem.flash.contracts

import nl.arnhem.flash.facebook.FbItem

/**
 * Created by Allan Wang on 19/12/17.
 **/
interface FlashUrlData {

    /**
     * The main (and fallback) url
     */
    var baseUrl: String

    /**
     * Only base viewpager should pass an enum
     */
    var baseEnum: FbItem?

    fun passUrlDataTo(other: FlashUrlData) {
        other.baseUrl = baseUrl
        other.baseEnum = baseEnum
    }

}