package nl.arnhem.flash.injectors

import android.webkit.WebView

/**
 * Created by Allan Wang on 2017-05-31.
 *
 * List of elements to hide
 */
enum class CssPadding(vararg val items: String) : InjectorContract {

    COMPOSER_PADDING("._55wr");

    val injector: JsInjector by lazy {
        JsBuilder().css("${items.joinToString(separator = ",")}{padding-top: 80px !important;" +
                "margin: 10px !important;}")
                .single(name).build()
    }

    override fun inject(webView: WebView, callback: (() -> Unit)?) {
        injector.inject(webView, callback)
    }

}