package nl.arnhem.flash.injectors

import android.webkit.WebView

/**
 * Created by Allan Wang on 2017-05-31.
 *
 * List of elements to hide
 */
enum class CssFixed(vararg val items: String) : InjectorContract {

    COMPOSER_FIXED("._6beq");

    val injector: JsInjector by lazy {
        JsBuilder().css("${items.joinToString(separator = ",")}{top: 0 !important; " +
                "z-index: 100 !important;" +
                "position: fixed !important;" +
                "width: 100% !important;" +
                "border-radius: 110px !important;}")
                .single(name).build()
    }

    override fun inject(webView: WebView, callback: (() -> Unit)?) {
        injector.inject(webView, callback)
    }

}