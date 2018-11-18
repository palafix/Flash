package nl.arnhem.flash.intro

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View
import ca.allanwang.kau.utils.bindViewResettable
import ca.allanwang.kau.utils.scaleXY
import nl.arnhem.flash.R
import nl.arnhem.flash.activities.IntroActivity
import nl.arnhem.flash.enums.Theme
import nl.arnhem.flash.utils.Prefs
import nl.arnhem.flash.utils.flashSnackbar
import nl.arnhem.flash.utils.iab.FlashBilling
import nl.arnhem.flash.utils.iab.IS_Flash_PRO
import nl.arnhem.flash.utils.iab.IabSettings
import nl.arnhem.flash.utils.launchSettingsActivity
import nl.arnhem.flash.utils.materialDialogThemed

/**
 * Created by Allan Wang on 2017-07-28.
 */
class IntroFragmentTheme : BaseIntroFragment(R.layout.intro_theme), FlashBilling by IabSettings() {

    private val gray: View by bindViewResettable(R.id.intro_theme_gray)
    private val default: View by bindViewResettable(R.id.intro_theme_default)
    private val custom: View by bindViewResettable(R.id.intro_theme_custom)
    private val dark: View by bindViewResettable(R.id.intro_theme_dark)
    private val amoled: View by bindViewResettable(R.id.intro_theme_amoled)
    private val glass: View by bindViewResettable(R.id.intro_theme_glass)


    private val themeList
        get() = listOf(gray, dark, default, custom, amoled, glass)

    override fun viewArray(): Array<Array<out View>> = arrayOf(arrayOf(title), arrayOf(gray, dark), arrayOf(custom, default), arrayOf(amoled, glass))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gray.setThemeClick(Theme.GRAY)
        default.setThemeClick(Theme.DEFAULT)
        custom.setCustomClick(Theme.CUSTOM)
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

    private fun View.setCustomClick(theme: Theme) {
        setOnClickListener { v ->
            flashSnackbar(R.string.Check_for_flash_pro) {
                duration = Snackbar.LENGTH_LONG
                setAction(R.string.got_it) { _ ->
                    if (IS_Flash_PRO) {
                        Prefs.theme = theme.ordinal
                        (activity as IntroActivity).apply {
                            ripple.ripple(Prefs.bgColor, v.x + v.pivotX, v.y + v.pivotY)
                            theme()
                        }
                        themeList.forEach { it.animate().scaleXY(if (it == this@setCustomClick) 1.6f else 0.8f).start() }
                    } else
                        activity?.materialDialogThemed {
                            title(R.string.uh_oh_no_pro)
                            content(resources.getString(R.string.get_pro_desc))
                            positiveText(R.string.yes)
                            negativeText(R.string.no)
                            onPositive { _, _ ->
                                activity?.launchSettingsActivity()
                            }
                            onNegative { _, _ ->
                                dismiss()
                            }
                        }
                }
            }
        }
    }
}