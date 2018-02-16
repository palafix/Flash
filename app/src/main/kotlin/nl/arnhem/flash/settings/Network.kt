package nl.arnhem.flash.settings

import ca.allanwang.kau.kpref.activity.KPrefAdapterBuilder
import nl.arnhem.flash.R
import nl.arnhem.flash.activities.SettingsActivity
import nl.arnhem.flash.utils.Prefs

/**
 * Created by Allan Wang on 2017-08-08.
 **/
fun SettingsActivity.getNetworkPrefs(): KPrefAdapterBuilder.() -> Unit = {

    checkbox(R.string.network_media_on_metered, { !Prefs.loadMediaOnMeteredNetwork }, { Prefs.loadMediaOnMeteredNetwork = !it }) {
        descRes = R.string.network_media_on_metered_desc
    }

}