package nl.arnhem.flash.settings

import ca.allanwang.kau.kpref.activity.KPrefAdapterBuilder
import nl.arnhem.flash.R
import nl.arnhem.flash.activities.SettingsActivity
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.REQUEST_REFRESH
import nl.arnhem.flash.utils.launchWebOverlayBasic


fun SettingsActivity.getMediaPrefs(): KPrefAdapterBuilder.() -> Unit = {



    checkbox(R.string.audio, Prefs::DisableAudio, {
        Prefs.DisableAudio = it
        setFlashResult(REQUEST_REFRESH)
    }) {
        descRes = R.string.audio_test
    }

    checkbox(R.string.enable_pip, Prefs::enablePip, {
        Prefs.enablePip = it
        setFlashResult(REQUEST_REFRESH)
    }) {
        descRes = R.string.enable_pip_desc
    }

    plainText(R.string.video_beta) {
        descRes = R.string.video_settings
        setFlashResult(REQUEST_REFRESH)
        onClick = { launchWebOverlayBasic("https://m.facebook.com/settings/videos/") }
    }
}