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
    FETCH_BODY("""setTimeout(function(){var e=document.querySelector("main");e||(e=document.querySelector("body")),Flash.handleHtml(e.outerHTML)},1e2);"""),

    AUDIO_OFF("""function suppressAudioMedia(){Object.defineProperty(HTMLAudioElement.prototype.__proto__, 'play', {value: function () {},writable: false});} suppressAudioMedia();"""),
    AUTO_VIDEO_OFF("""function vidAutoPlay(){var el='div[data-store*=videoID]';var tolerance=200;el=document.querySelectorAll(el);for(i=0;i<el.length;i+=1){if(el!=null){var elRect=el[i].getBoundingClientRect();var isVidVisible=elRect.bottom-tolerance>0&&elRect.right-tolerance>0&&elRect.left+tolerance<window.innerWidth&&elRect.top+tolerance<window.innerHeight;if(isVidVisible){el[i].querySelector('div[data-sigil*=playInlineVideo]').click();el[i].querySelector('video').muted=true;}}}}window.onscroll=function(){isHorizontalScrollAreaVisible();vidAutoPlay()};"""),
    RETURN_BODY("return(document.getElementsByTagName('html')[0].innerHTML);"),

    AUTO_CLICK_IMAGE_VIEW_FULL_SIZE("document.querySelector(\"a[href*='view_full_size']\").click();"),
    AUTO_CLICK_IMAGE_JPG("document.querySelector(\"a[href*='.jpg']\").click();"),

    CREATE_SHARE(clickBySelector("button[data-testid=react-composer-post-button]")),

    CREATE_POST(clickBySelector("[role=textbox][onclick]")),
    //DELETE_SEARCH(clickBySelector("a[aria-labelledby=u_44_1]")),
    //CREATE_MSG(clickBySelector("a[rel=dialog]")),
    /**
     * Used as a pseudoinjector for maybe functions
     */
    EMPTY("");

    val function = "!function(){$body}();"

    override fun inject(webView: WebView, callback: (() -> Unit)?) =
        JsInjector(function).inject(webView, callback)
}

@Suppress("NOTHING_TO_INLINE")
inline fun clickBySelector(selector: String): String =
        """document.querySelector("$selector").click()"""