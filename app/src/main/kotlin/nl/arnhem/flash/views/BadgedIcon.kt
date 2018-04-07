package nl.arnhem.flash.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import ca.allanwang.kau.utils.*
import com.mikepenz.iconics.typeface.IIcon
import nl.arnhem.flash.R
import nl.arnhem.flash.utils.Prefs


/**
 * Created by Allan Wang on 2017-06-19.
 **/
class BadgedIcon @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val badgeTextView: TextView by bindView(R.id.badge_text)
    private val badgeImage: ImageView by bindView(R.id.badge_image)

    init {
        inflate(context, R.layout.view_badged_icon, this)
        val badgeColor = Color.RED.withAlpha(255).colorToForeground(0.2f)
        val badgeBackground = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(badgeColor, badgeColor))
        badgeBackground.cornerRadius = 13.dpToPx.toFloat()
        badgeTextView.background = badgeBackground
        badgeTextView.setTextColor(Color.WHITE)
    }

    var iicon: IIcon? = null
        set(value) {
            field = value
            badgeImage.setImageDrawable(value?.toDrawable(context, sizeDp = 20, color = Prefs.mainActivityLayout.iconColor()))
        }

    fun setAllAlpha(alpha: Float) {
        //badgeTextView.setTextColor(Prefs.textColor.withAlpha(alpha.toInt()))
        badgeImage.drawable.alpha = alpha.toInt()
    }

    var badgeText: String?
        get() = badgeTextView.text.toString()
        set(value) {
            if (badgeTextView.text == value) return
            badgeTextView.text = value
            if (value != null && value != "0") badgeTextView.visible()
            else badgeTextView.gone()
        }


    fun removeNotification(notificationCount: Int) {
        badgeTextView.visibility = View.INVISIBLE
        if (notificationCount <= 0) {
            badgeTextView.text = notificationCount.toString()
        }
    }

    var addfeed: String?
        get() = badgeTextView.text.toString()
        set(String) {
            badgeTextView.gone()
            val handler = Handler()
            handler.postDelayed({
                val number = "1"
                if (badgeTextView.text == number) return@postDelayed
                badgeTextView.text = number
                if (number == "1") badgeTextView.visible()
                else badgeTextView.gone()
            }, 300000)
            handler.postDelayed({
                val number = "9+"
                if (badgeTextView.text == number) return@postDelayed
                badgeTextView.text = number
                if (number == "9+") badgeTextView.visible()
                else badgeTextView.gone()
            }, 680000)
        }
}




