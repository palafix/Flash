package nl.arnhem.flash.injectors

import android.graphics.Color
import android.webkit.WebView
import ca.allanwang.kau.kotlin.lazyContext
import ca.allanwang.kau.utils.*
import nl.arnhem.flash.utils.L
import nl.arnhem.flash.utils.Prefs
import java.io.FileNotFoundException
import java.util.*

/**
 * Created by Allan Wang on 2017-05-31.
 * Mapping of the available assets
 * The enum name must match the css file name
 */
enum class CssAssets(private val folder: String = "themes") : InjectorContract {
    FACEBOOK_MOBILE, MATERIAL_GRAY, MATERIAL_DARK, MATERIAL_AMOLED, MATERIAL_GLASS, CUSTOM, ROUND_ICONS("components")
    ;

    var file = "${name.toLowerCase(Locale.CANADA)}.css"
    var injector = lazyContext {
        try {
            var content = it.assets.open("css/$folder/$file").bufferedReader().use { it.readText() }
            if (this == CUSTOM) {
                val bt: String = if (Color.alpha(Prefs.bgColor) == 255) {
                    Prefs.bgColor.toRgbaString()
                } else {
                    "transparent"
                }
                content = content
                        .replace("\$T\$", Prefs.textColor.toRgbaString())
                        .replace("\$TT\$", Prefs.textColor.colorToBackground(0.05f).toRgbaString())
                        .replace("\$A\$", Prefs.accentColor.toRgbaString())
                        .replace("\$B\$", Prefs.bgColor.toRgbaString())
                        .replace("\$BT\$", bt)
                        .replace("\$BBT\$", Prefs.bgColor.withAlpha(51).colorToForeground(0.35f).toRgbaString())
                        .replace("\$O\$", Prefs.bgColor.withAlpha(255).toRgbaString())
                        .replace("\$OO\$", Prefs.bgColor.withAlpha(255).colorToForeground(0.35f).toRgbaString())
                        .replace("\$D\$", Prefs.textColor.adjustAlpha(0.3f).toRgbaString())
                        .replace("\$BTR\$", Prefs.notiColor.toRgbaString())
                        .replace("\$HDR\$", Prefs.notiColor.toRgbaString())
            }

            JsBuilder().css(content).build()
        } catch (e: FileNotFoundException) {
            L.e(e) { "CssAssets file not found" }
            JsInjector(JsActions.EMPTY.function)
        }
    }

    override fun inject(webView: WebView, callback: (() -> Unit)?) {
        injector(webView.context).inject(webView, callback)
    }

    fun reset() {
        injector.invalidate()
    }
}
