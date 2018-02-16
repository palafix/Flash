package nl.arnhem.flash.contracts

import android.app.Activity
import android.widget.FrameLayout
import ca.allanwang.kau.utils.inflate
import nl.arnhem.flash.R
import nl.arnhem.flash.utils.L
import nl.arnhem.flash.views.FlashVideoContainerContract
import nl.arnhem.flash.views.FlashVideoViewer

/**
 * Created by Allan Wang on 2017-11-10.
 */
interface VideoViewHolder : FrameWrapper, FlashVideoContainerContract {

    var videoViewer: FlashVideoViewer?

    fun showVideo(url: String)
            = showVideo(url, false)

    /**
     * Create new viewer and reuse existing one
     * The url will be formatted upon loading
     */
    fun showVideo(url: String, repeat: Boolean) {
        if (videoViewer != null)
            videoViewer?.setVideo(url, repeat)
        else
            videoViewer = FlashVideoViewer.showVideo(url, repeat, this)
    }

    fun videoOnStop() = videoViewer?.pause()

    fun videoOnBackPress() = videoViewer?.onBackPressed() ?: false

    override val videoContainer: FrameLayout
        get() = frameWrapper

    override fun onVideoFinished() {
        L.d { "Video view released" }
        videoViewer = null
    }
}

interface FrameWrapper {

    val frameWrapper: FrameLayout

    fun Activity.setFrameContentView(layoutRes: Int) {
        setContentView(R.layout.activity_frame_wrapper)
        frameWrapper.inflate(layoutRes, true)
    }

}
