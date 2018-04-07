package nl.arnhem.flash.views

import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.net.Uri
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import ca.allanwang.kau.utils.*
import com.devbrackets.android.exomedia.listener.VideoControlsVisibilityListener
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import nl.arnhem.flash.R
import nl.arnhem.flash.utils.L
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.flashDownload
import nl.arnhem.flash.utils.flashSnackbar


/**
 * Created by Allan Wang on 2017-10-13.
 **/
class FlashVideoViewer @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), FlashVideoViewerContract {

    val container: ViewGroup by bindView(R.id.video_container)
    val toolbar: Toolbar by bindView(R.id.video_toolbar)
    val background: View by bindView(R.id.video_background)
    val video: FlashVideoView by bindView(R.id.video)
    private val restarter: ImageView by bindView(R.id.video_restart)


    companion object {
        /**
         * Matches VideoControls.CONTROL_VISIBILITY_ANIMATION_LENGTH
         */
        private const val CONTROL_ANIMATION_DURATION = 300L

        /**
         * Simplified binding to add video to layout, and remove it when finished
         * This is under the assumption that the container allows for overlays,
         * such as a FrameLayout
         */
        fun showVideo(url: String, repeat: Boolean, contract: FlashVideoContainerContract): FlashVideoViewer {
            val container = contract.videoContainer
            val videoViewer = FlashVideoViewer(container.context)
            container.addView(videoViewer)
            videoViewer.bringToFront()
            videoViewer.setVideo(url, repeat)
            videoViewer.video.containerContract = contract
            videoViewer.video.onFinishedListener = { container.removeView(videoViewer); contract.onVideoFinished() }
            return videoViewer
        }
    }

    init {
        inflate(R.layout.view_video, true)
        alpha = 0f
        background.setBackgroundColor(
                if (!Prefs.blackMediaBg && Prefs.bgColor.isColorDark)
                    Prefs.bgColor.withMinAlpha(200)
                else
                    Color.BLACK)
        video.setViewerContract(this)
        video.pause()
        toolbar.inflateMenu(R.menu.menu_video)
        context.setMenuIcons(toolbar.menu, Color.WHITE,
                R.id.action_pip to GoogleMaterial.Icon.gmd_picture_in_picture_alt,
                R.id.action_download to GoogleMaterial.Icon.gmd_file_download
        )
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_pip -> video.isExpanded = false
                R.id.action_download -> {
                    context.flashDownload(video.videoUri)
                    flashSnackbar(R.string.downloading)
                }
            }
            true
        }
        restarter.gone().setIcon(GoogleMaterial.Icon.gmd_replay, 64)
        restarter.setOnClickListener {
            video.restart()
            restarter.fadeOut { restarter.gone() }
        }
    }

    fun setVideo(url: String, repeat: Boolean = false) {
        L.d { "Load video; repeat: $repeat" }
        L._d { "Video Url: $url" }
        animate().alpha(1f).setDuration(FlashVideoView.ANIMATION_DURATION).start()
        video.setVideoURI(Uri.parse(url))
        video.repeat = repeat
    }

    /**
     * Handle back presses
     * returns true if consumed, false otherwise
     */
    fun onBackPressed(): Boolean {
        parent ?: return false
        if (video.isExpanded)
            video.isExpanded = false
        else
            video.destroy()
        return true
    }

    fun pause() = video.pause()

    /*
     * -------------------------------------------------------------
     * FlashVideoViewerContract
     * -------------------------------------------------------------
     */


    override fun onExpand(progress: Float) {
        toolbar.goneIf(progress == 0f).alpha = progress
        background.alpha = progress
    }

    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
        if (restarter.isVisible) {
            restarter.performClick()
            return true
        }
        return false
    }

    override fun onVideoComplete() {
        video.jumpToStart()
        restarter.fadeIn()
    }

    fun updateLocation() {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                video.updateLocation()
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    override fun onControlsShown() {
        if (video.isExpanded)
            toolbar.fadeIn(duration = CONTROL_ANIMATION_DURATION, onStart = { toolbar.visible() })
    }

    override fun onControlsHidden() {
        if (!toolbar.isGone)
            toolbar.fadeOut(duration = CONTROL_ANIMATION_DURATION) { toolbar.gone() }
    }

}

interface FlashVideoViewerContract : VideoControlsVisibilityListener {
    fun onSingleTapConfirmed(event: MotionEvent): Boolean
    /**
     * Process of expansion
     * 1f represents an expanded view, 0f represents a minimized view
     */
    fun onExpand(progress: Float)

    fun onVideoComplete()
}

interface FlashVideoContainerContract {
    /**
     * Returns extra padding to be added
     * from the right and from the bottom respectively
     */
    val lowerVideoPadding: PointF

    /**
     * Get the container which will hold the video viewer
     */
    val videoContainer: FrameLayout

    /**
     * Called once the video has stopped & should be removed
     */
    fun onVideoFinished()
}