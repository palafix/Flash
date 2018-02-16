package nl.arnhem.flash.facebook

import nl.arnhem.flash.internal.COOKIE
import nl.arnhem.flash.internal.assertComponentsNotEmpty
import nl.arnhem.flash.internal.assertDescending
import nl.arnhem.flash.internal.authDependent
import nl.arnhem.flash.parsers.*
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Created by Allan Wang on 24/12/17.
 */
class FbParseTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun before() {
            authDependent()
        }
    }

    private inline fun <reified T : Any> FlashParser<T>.test(action: T.() -> Unit = {}) =
            parse(COOKIE).test(url, action)

    private inline fun <reified T : Any> ParseResponse<T>?.test(url: String, action: T.() -> Unit = {}) {
        val response = this
                ?: fail("${T::class.simpleName} parser returned null for $url")
        println(response)
        response.data.action()
    }

    @Test
    fun message() = MessageParser.test {
        threads.forEach {
            it.assertComponentsNotEmpty()
            assertTrue(it.id > FALLBACK_TIME_MOD, "id may not be properly matched")
            assertNotNull(it.img, "img may not be properly matched")
        }
        threads.map(FlashThread::time).assertDescending("thread time values")
    }

    @Test
    fun messageUser() = MessageParser.queryUser(COOKIE, "allan").test("allan query")

    @Test
    fun search() = SearchParser.test()

    @Test
    fun notif() = NotifParser.test {
        notifs.forEach {
            it.assertComponentsNotEmpty()
            assertTrue(it.id > FALLBACK_TIME_MOD, "id may not be properly matched")
            assertNotNull(it.img, "img may not be properly matched")
        }
        notifs.map(FlashNotif::time).assertDescending("notif time values")
    }
}