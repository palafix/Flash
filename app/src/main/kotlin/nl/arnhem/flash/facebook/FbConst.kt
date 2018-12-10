package nl.arnhem.flash.facebook

/**
 * Created by Allan Wang on 2017-06-01.
 **/

const val FACEBOOK_COM = "facebook.com"
const val WWW_FACEBOOK_COM = "https://wwww.$FACEBOOK_COM"
const val FBCDN_NET = "fbcdn.net"
const val HTTPS_FACEBOOK_COM = "https://$FACEBOOK_COM"
const val FB_URL_BASE = "https://m.$FACEBOOK_COM/"
const val FB_BACK = "${FB_URL_BASE}home.php"
fun PROFILE_PICTURE_URL(id: Long) = "https://graph.facebook.com/$id/picture?type=large"
const val FB_LOGIN_URL = "${FB_URL_BASE}login"

const val USER_AGENT_FULL = "Mozilla/5.0 (Linux; Android 4.4.2; en-us; SAMSUNG SM-G900T Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/1.6 Chrome/28.0.1500.94 Mobile Safari/537.36"
const val USER_AGENT_MESSENGER = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Safari/537.36"
const val USER_AGENT_BASIC_JSOUP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Safari/537.36"
const val USER_AGENT_BASIC = "Mozilla/5.0 (Linux; Android 7.1; Mi A1 Build/N2G47H) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.83 Mobile Safari/537.36"
const val USER_AGENT_VIDEO_SETTINGS = "Mozilla/5.0 (Linux; Android 7.1; Mi A1 Build/N2G47H) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.83 Mobile Safari/537.36"
const val USER_AGENT_STATUS = "Mozilla/5.0 (iPad; CPU OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1"


/**
 * Animation transition delay, just to ensure that the styles
 * have properly set in
 */
const val WEB_LOAD_DELAY = 50L

const val CHROMEWEB_LOAD_DELAY = 450L

/**
 * Additional delay for transition when called from commit.
 * Note that transitions are also called from onFinish, so this value
 * will never make a load slower than it is
 */
const val WEB_COMMIT_LOAD_DELAY = 200L