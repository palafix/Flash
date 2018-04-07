package nl.arnhem.flash.settings

import ca.allanwang.kau.kpref.activity.KPrefAdapterBuilder
import ca.allanwang.kau.utils.string
import nl.arnhem.flash.R
import nl.arnhem.flash.activities.SettingsActivity
import nl.arnhem.flash.enums.FeedSort
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.REQUEST_REFRESH
import nl.arnhem.flash.utils.REQUEST_RESTART
import nl.arnhem.flash.utils.materialDialogThemed

/**
 * Created by Allan Wang on 2017-06-29.
 **/
fun SettingsActivity.getFeedPrefs(): KPrefAdapterBuilder.() -> Unit = {

    text(R.string.newsfeed_sort, Prefs::feedSort, { Prefs.feedSort = it }) {
        descRes = R.string.newsfeed_sort_desc
        onClick = {
            materialDialogThemed {
                title(R.string.newsfeed_sort)
                items(FeedSort.values().map { string(it.textRes) })
                itemsCallbackSingleChoice(item.pref, { _, _, which, _ ->
                    if (item.pref != which) {
                        item.pref = which
                        shouldRestartMain()
                    }
                    true
                })
            }
        }
        textGetter = { string(FeedSort(it).textRes) }
    }

    checkbox(R.string.aggressive_recents, Prefs::aggressiveRecents, {
        Prefs.aggressiveRecents = it
        setFlashResult(REQUEST_REFRESH)
    }) {
        descRes = R.string.aggressive_recents_desc
    }

    checkbox(R.string.hide_fab, Prefs::HasFab, {
        Prefs.HasFab = it
        setFlashResult(REQUEST_RESTART)
    }) {
        descRes = R.string.hide_fab_desc
    }

    checkbox(R.string.composer, Prefs::showComposer, {
        Prefs.showComposer = it
        setFlashResult(REQUEST_REFRESH)
    }) {
        descRes = R.string.composer_desc
    }

    header(R.string.pro_features)

    checkbox(R.string.suggested_friends, Prefs::showSuggestedFriends, {
        Prefs.showSuggestedFriends = it
        setFlashResult(REQUEST_REFRESH)
    }) {
        descRes = R.string.suggested_friends_desc
        dependsOnPro()
    }

    checkbox(R.string.suggested_groups, Prefs::showSuggestedGroups, {
        Prefs.showSuggestedGroups = it
        setFlashResult(REQUEST_REFRESH)
    }) {
        descRes = R.string.suggested_groups_desc
        dependsOnPro()
    }

    checkbox(R.string.facebook_ads, Prefs::showFacebookAds, {
        Prefs.showFacebookAds = it
        setFlashResult(REQUEST_REFRESH)
    }) {
        descRes = R.string.facebook_ads_desc
        dependsOnPro()
    }

    checkbox(R.string.adremoval, Prefs::AdRemoval, {
        Prefs.AdRemoval = it
        setFlashResult(REQUEST_REFRESH)
    }) {
        descRes = R.string.adremoval_desc
        dependsOnPro()
    }
}