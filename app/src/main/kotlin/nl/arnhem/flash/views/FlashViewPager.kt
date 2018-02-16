package nl.arnhem.flash.views

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent
import nl.arnhem.flash.utils.Prefs

/**
 * Created by Allan Wang on 2017-07-07.
 *
 * Basic override to allow us to control swiping
 */
class FlashViewPager @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ViewPager(context, attrs) {
    var enableSwipe = true

    override fun onInterceptTouchEvent(ev: MotionEvent?) =
            try {
                Prefs.viewpagerSwipe && enableSwipe && super.onInterceptTouchEvent(ev)
            } catch (e: IllegalArgumentException) {
                false
            }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean =
            try {
                Prefs.viewpagerSwipe && enableSwipe && super.onTouchEvent(ev)
            } catch (e: IllegalArgumentException) {
                false
            }
}