package nl.arnhem.flash.injectors

import android.webkit.WebView

/**
 * Created by Allan Wang on 2017-05-31.
 *
 * List of elements to hide
 */
enum class CssStatus(vararg val items: String) : InjectorContract {

    COMPOSER_STATUS("#globalContainer, #contentArea, ._59s7, #contentCol");

    val injector: JsInjector by lazy {
        JsBuilder().css("${items.joinToString(separator = ",")}{position: fixed !important;" +
                "top: 0 !important;" +
                "bottom: 0 !important;" +
                "left: 0 !important;" +
                "right: 0 !important;" +
                "width: 100% !important;" +
                "min-width: 10% !important;" +
                "height: 500px !important;" +
                "margin: 0 auto !important;")
                .single(name).build()
    }

    override fun inject(webView: WebView, callback: (() -> Unit)?) {
        injector.inject(webView, callback)
    }

}