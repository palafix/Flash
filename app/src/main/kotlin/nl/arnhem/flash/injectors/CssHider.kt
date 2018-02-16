package nl.arnhem.flash.injectors

import android.webkit.WebView

/**
 * Created by Allan Wang on 2017-05-31.
 *
 * List of elements to hide
 */
enum class CssHider(vararg val items: String) : InjectorContract {
    CORE("[data-sigil=m_login_upsell]", "role=progressbar"),
    //HEADER("#header", "[data-sigil=MTopBlueBarHeader]",
    //        "#header-notices", "[data-sigil*=m-promo-jewel-header]"),
    ADS("article[data-xt*=sponsor]",
            "article[data-store*=sponsor]"),
    PEOPLE_YOU_MAY_KNOW("article._d2r"),
    SUGGESTED_GROUPS(".scrollArea","._5yxv","._5yxt._5yxt","._55wo._5rgr._5gh8._5gh8._35au"),
    COMPOSER("#MComposer"),
    MESSENGER("._s15", "[data-testid=info_panel]", ".js_i"),
    NON_RECENT("article:not([data-store*=actor_name])")
    ;

    val injector: JsInjector by lazy {
        JsBuilder().css("${items.joinToString(separator = ",")}{display: none !important;}")
                .single(name).build()
    }

    override fun inject(webView: WebView, callback: (() -> Unit)?) {
        injector.inject(webView, callback)
    }

}