@file:Suppress("DEPRECATED_IDENTITY_EQUALS")

package nl.arnhem.flash.settings

import ca.allanwang.kau.kpref.activity.KPrefAdapterBuilder
import nl.arnhem.flash.R
import nl.arnhem.flash.activities.SettingsActivity
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.REQUEST_REFRESH

/**
 * Created by Allan Wang on 2017-06-30.
 **/

fun SettingsActivity.getBehaviourPrefs(): KPrefAdapterBuilder.() -> Unit = {

    checkbox(R.string.enable_pip, Prefs::enablePip, {
        Prefs.enablePip = it
        setFlashResult(REQUEST_REFRESH)
    }) {
        descRes = R.string.enable_pip_desc
    }

    checkbox(R.string.fancy_animations, Prefs::animate, { Prefs.animate = it; animate = it }) {
        descRes = R.string.fancy_animations_desc
    }

    checkbox(R.string.overlay_swipe, Prefs::overlayEnabled, { Prefs.overlayEnabled = it; setFlashResult(REQUEST_REFRESH) }) {
        descRes = R.string.overlay_swipe_desc
    }

    checkbox(R.string.overlay_full_screen_swipe, Prefs::overlayFullScreenSwipe, { Prefs.overlayFullScreenSwipe = it }) {
        descRes = R.string.overlay_full_screen_swipe_desc
    }

    checkbox(R.string.open_links_in_default, Prefs::linksInDefaultApp, { Prefs.linksInDefaultApp = it }) {
        descRes = R.string.open_links_in_default_desc
    }

    checkbox(R.string.viewpager_swipe, Prefs::viewpagerSwipe, { Prefs.viewpagerSwipe = it }) {
        descRes = R.string.viewpager_swipe_desc
    }

    checkbox(R.string.force_message_bottom, Prefs::messageScrollToBottom, { Prefs.messageScrollToBottom = it }) {
        descRes = R.string.force_message_bottom_desc
    }

    checkbox(R.string.exit_confirmation, Prefs::exitConfirmation, { Prefs.exitConfirmation = it }) {
        descRes = R.string.exit_confirmation_desc
    }
}
