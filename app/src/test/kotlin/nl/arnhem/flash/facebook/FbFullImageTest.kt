package nl.arnhem.flash.facebook

import nl.arnhem.flash.facebook.requests.getFullSizedImageUrl
import nl.arnhem.flash.internal.COOKIE
import nl.arnhem.flash.internal.authDependent
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertNotNull

/**
 * Created by Allan Wang on 12/04/18.
 */
class FbFullImageTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun before() {
            authDependent()
        }
    }

    @Test
    fun getFullImage() {
        val url = "https://touch.facebook.com/photo/view_full_size/?fbid=107368839645039"
        val result = COOKIE.getFullSizedImageUrl(url).blockingGet()
        assertNotNull(result)
        println(result)
    }
}