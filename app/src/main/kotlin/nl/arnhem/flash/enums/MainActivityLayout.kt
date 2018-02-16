package nl.arnhem.flash.enums

import nl.arnhem.flash.R
import nl.arnhem.flash.utils.Prefs
import android.graphics.Color
/**
 * Created by Allan Wang on 2017-08-19.
 **/
enum class MainActivityLayout(
        val titleRes: Int,
        val layoutRes: Int,
        val backgroundColor: () -> Int,
        val iconColor: () -> Int) {

    TOP_BAR(R.string.top_bar,
            R.layout.activity_main,
            { Color.WHITE },
            { Color.DKGRAY }),

    BOTTOM_BAR(R.string.bottom_bar,
            R.layout.activity_main_bottom_tabs,
            { Prefs.headerColor },
            { Prefs.iconColor });

    companion object {
        val values = values() //save one instance
        operator fun invoke(index: Int) = values[index]
    }
}