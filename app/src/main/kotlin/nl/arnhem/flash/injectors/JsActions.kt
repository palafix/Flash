package nl.arnhem.flash.injectors

import android.webkit.WebView
import nl.arnhem.flash.facebook.FB_URL_BASE

/**
 * Created by Allan Wang on 2017-05-31.
 *
 * Collection of short js functions that are embedded directly
 */
enum class JsActions(body: String) : InjectorContract {
    /**
     * Redirects to login activity if create account is found
     * see [nl.arnhem.flash.web.FlashJSI.loadLogin]
     */
    LOGIN_CHECK("document.getElementById('signup-button')&&Flash.loadLogin();"),
    BASE_HREF("""document.write("<base href='$FB_URL_BASE'/>");"""),
    FETCH_BODY("""setTimeout(function(){var e=document.querySelector("main");e||(e=document.querySelector("body")),Fast.handleHtml(e.outerHTML)},1e2);"""),
    CREATE_POST(clickBySelector("button[name=view_overview]")),
//    CREATE_MSG(clickBySelector("a[rel=dialog]")),
    /**
     * Used as a pseudoinjector for maybe functions
     */
    EMPTY("");

    val function = "!function(){$body}();"

    override fun inject(webView: WebView, callback: (() -> Unit)?) =
            JsInjector(function).inject(webView, callback)

}

@Suppress("NOTHING_TO_INLINE")
private inline fun clickBySelector(selector: String): String =
        """document.querySelector("$selector").click()"""