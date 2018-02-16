package nl.arnhem.flash.intro

import android.os.Bundle
import android.view.View
import ca.allanwang.kau.utils.bindViewResettable
import ca.allanwang.kau.utils.scaleXY
import nl.arnhem.flash.R
import nl.arnhem.flash.activities.IntroActivity
import nl.arnhem.flash.enums.Theme
import nl.arnhem.flash.utils.Prefs

/**
 * Created by Allan Wang on 2017-07-28.
 */
class IntroFragmentTheme : BaseIntroFragment(R.layout.intro_theme) {

    val gray: View by bindViewResettable(R.id.intro_theme_gray)
    val dark: View by bindViewResettable(R.id.intro_theme_dark)
    val amoled: View by bindViewResettable(R.id.intro_theme_amoled)
    val glass: View by bindViewResettable(R.id.intro_theme_glass)

    val themeList
        get() = listOf(gray, dark, amoled, glass)

    override fun viewArray(): Array<Array<out View>>
            = arrayOf(arrayOf(title), arrayOf(gray, dark), arrayOf(amoled, glass))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gray.setThemeClick(Theme.GRAY)
        dark.setThemeClick(Theme.DARK)
        amoled.setThemeClick(Theme.AMOLED)
        glass.setThemeClick(Theme.GLASS)
        val currentTheme = Prefs.theme - 1
        if (currentTheme in 0..3)
            themeList.forEachIndexed { index, v -> v.scaleXY = if (index == currentTheme) 1.6f else 0.8f }
    }

    private fun View.setThemeClick(theme: Theme) {
        setOnClickListener { v ->
            Prefs.theme = theme.ordinal
            (activity as IntroActivity).apply {
                ripple.ripple(Prefs.bgColor, v.x + v.pivotX, v.y + v.pivotY)
                theme()
            }
            themeList.forEach { it.animate().scaleXY(if (it == this) 1.6f else 0.8f).start() }
        }
    }

}