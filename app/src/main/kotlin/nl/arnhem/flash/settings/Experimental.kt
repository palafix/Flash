package nl.arnhem.flash.settings

import android.util.Log
import ca.allanwang.kau.kpref.activity.KPrefAdapterBuilder
import ca.allanwang.kau.logging.KL
import nl.arnhem.flash.R
import nl.arnhem.flash.activities.SettingsActivity
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.REQUEST_RESTART_APPLICATION
import nl.arnhem.flash.utils.Showcase

/**
 * Created by Allan Wang on 2017-06-29.
 */
fun SettingsActivity.getExperimentalPrefs(): KPrefAdapterBuilder.() -> Unit = {

    plainText(R.string.experimental_disclaimer) {
        descRes = R.string.experimental_disclaimer_info
    }

    checkbox(R.string.experimental_by_default, Showcase::experimentalDefault, { Showcase.experimentalDefault = it }) {
        descRes = R.string.experimental_by_default_desc
    }

    // Experimental content starts here ------------------


    // Experimental content ends here --------------------

    checkbox(R.string.verbose_logging, Prefs::verboseLogging, {
        Prefs.verboseLogging = it
        KL.shouldLog = { it != Log.VERBOSE }
    }) {
        descRes = R.string.verbose_logging_desc
    }

    plainText(R.string.restart_flash) {
        descRes = R.string.restart_flash_desc
        onClick = {
            setFlashResult(REQUEST_RESTART_APPLICATION)
            finish()
        }
    }
}