package nl.arnhem.flash.injectors

import android.webkit.WebView

/**
 * Created by Allan Wang on 2017-05-31.
 *
 * List of elements to hide
 */
enum class CssHider(vararg val items: String) : InjectorContract {
    CORE("[data-sigil=m_login_upsell]", "role=progressbar"),
    HEADER("#header", "[data-sigil=MTopBlueBarHeader]",
            "#header-notices", "[data-sigil*=m-promo-jewel-header]"),
    HEADER_TOP("._3wo2 ._129_ ", "._52z5"),
    ADS("article[data-xt*=sponsor]", "article[data-store*=sponsor]", "article[data-ft*=ei]"),
    PEOPLE_YOU_MAY_KNOW("article._d2r"),
    SUGGESTED_GROUPS("._41ft", "._177o", "._5yxu"),
    COMPOSER("#MComposer"),
    STORIESTRAY("#MStoriesTray"),
    COMPOSER_BOTTOM("._6beo ._6bep", "._6dsj"),
    MESSENGER("._s15", "[data-testid=info_panel]", ".js_i"),
    NON_RECENT("article:not([data-store*=actor_name])"),
    VIDEOS(".hasLeftCol #leftCol", "._2s1x ._2s1y", ".fbTimelineSection"),
    VIDEOS2(".hasLeftCol #leftCol", "._2s1x ._2s1y"),
    WWW_FACEBOOK_COM("#pagelet_bluebar, #leftCol, li._1tm3:nth-child(2), #rightCol, ._5pcb, ._4-u2.mvm._495i._4-u8, ._1p1t", "li._1tm3:nth-child(2), ._5pcb, ._4-u2.mvm._495i._4-u8");

    val injector: JsInjector by lazy {
        JsBuilder().css("${items.joinToString(separator = ",")}{display: none !important;}")
                .single(name).build()
    }

    override fun inject(webView: WebView, callback: (() -> Unit)?) {
        injector.inject(webView, callback)
    }

}