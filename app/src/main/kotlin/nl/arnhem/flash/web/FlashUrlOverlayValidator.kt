package nl.arnhem.flash.web

import nl.arnhem.flash.activities.WebOverlayActivity
import nl.arnhem.flash.activities.WebOverlayActivityBase
import nl.arnhem.flash.activities.WebOverlayBasicActivity
import nl.arnhem.flash.contracts.VideoViewHolder
import nl.arnhem.flash.facebook.*
import nl.arnhem.flash.utils.*
import nl.arnhem.flash.views.FlashWebView
import org.jetbrains.anko.runOnUiThread

/**
 * Created by Allan Wang on 2017-08-15.
 *
 * Due to the nature of facebook href's, many links
 * cannot be resolved on a new window and must instead
 * by loaded in the current page
 * This helper method will collect all known cases and launch the overlay accordingly
 * Returns [true] (default) if action is consumed, [false] otherwise
 *
 * Note that this is not always called on the main thread!
 * UI related methods should always be posted or they may not be properly executed.
 *
 * If the request already comes from an instance of [WebOverlayActivity], we will then judge
 * whether the user agent string should be changed. All propagated results will return false,
 * as we have no need of sending a new intent to the same activity
 */
fun FlashWebView.requestWebOverlay(url: String): Boolean {
    L.v { "Request web overlay: $url" }
    val context = context // finalize reference
    if (url.isVideoUrl && context is VideoViewHolder) {
        L.d { "Found video through overlay" }
        context.runOnUiThread { context.showVideo(url.formattedFbUrl) }
        return true
    }

    //if (Prefs.autoImageLoader) {
    //    if (url.contains("/photos/a.")
    //            || url.contains("/photos/ms.")
    //            || url.contains("/photos/pcb.")
    //            || url.contains("/photo.php")) {
    //        context.popToTest(url.formattedFbUrl)
    //        return true
    //    }
    if (url.contains("/photos/viewer/?photoset_token")) {
            return false
        }
    //}

    if (url.isImageUrl) {
        L.d { "Found fb image" }
        context.launchImageActivity(url.formattedFbUrl, title)
        return true
    }
    if (url.isIndirectImageUrl) {
        L.d { "Found fb image" }
        context.launchImageActivity(url.formattedFbUrl, title, cookie = FbCookie.webCookie)
        return true
    }
    if (!url.isIndependent) {
        L.d { "Forbid overlay switch" }
        return false
    }
    if (!Prefs.overlayEnabled) return false
    if (context is WebOverlayActivityBase) {
        L.v { "Check web request from overlay" }
        val shouldUseBasic = url.formattedFbUrl.shouldUseBasicAgent
        val shouldUseMessage = url.formattedFbUrl.shouldUseMessageAgent
        //already overlay; manage user agent
        if (userAgentString != USER_AGENT_BASIC && shouldUseBasic) {
            L.i { "Switch to basic agent overlay" }
            context.launchWebOverlayBasicFlash(url)
            return true
        } else
            if (userAgentString != USER_AGENT_BASIC && shouldUseMessage) {
                L.d { "Switch to messenger agent overlay" }
                context.launchWebOverlayBasicFlash(url)
                return true
            }
        if (context is WebOverlayBasicActivity && !shouldUseBasic) {
            L.i { "Switch from basic agent" }
            context.launchWebOverlay(url)
            return true
        }

        if (url.contains("filter_type=")) {
            L.i { "return false switch" }
            return false
        } else
            L.i { "Request web overlay passed" }
        context.launchWebOverlayBasicFlash(url)
        return true
    } else
        L.v { "Request web overlay passed" }
    context.launchWebOverlay(url)
    return true
}

fun WebViewFlash.requestWebOverlay(url: String): Boolean {
    L.v { "Request web overlay: $url" }
    val context = context // finalize reference
    if (url.isVideoUrl && context is VideoViewHolder) {
        L.d { "Found video through overlay" }
        context.runOnUiThread { context.showVideo(url.formattedFbUrl) }
        return true
    }
    if (url.isImageUrl) {
        L.d { "Found fb image" }
        context.launchImageActivity(url.formattedFbUrl, null)
        return true
    }
    if (!url.isIndependent) {
        L.d { "Forbid overlay switch" }
        return false
    }
    L.v { "Request web overlay passed" }
    context.launchWebOverlay(url)
    return true
}

/**
 * If the url contains any one of the whitelist segments, switch to the chat overlay
 */
val messageWhitelist: Set<String> =
        setOf(FbItem.MESSAGES, FbItem.CHAT, FbItem.FEED_MOST_RECENT, FbItem.FEED_TOP_STORIES)
                .mapTo(mutableSetOf(), FbItem::url)

val String.shouldUseBasicAgent: Boolean
    get() {
        if (contains("story.php"))  // do not use basic for comment section
            return false
        if (contains("/events/"))   // do not use for events (namely the map)
            return false
        return true                 // use for everything else
    }

val String.shouldUseMessageAgent: Boolean
    get() {
        (messageWhitelist.any { contains(it) }) || this == FB_URL_BASE
        if (contains("/messages/read/"))  // do not use basic for messages section
            return true
        return true // use for everything else

    }
