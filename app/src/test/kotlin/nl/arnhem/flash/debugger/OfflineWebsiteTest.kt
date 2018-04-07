package nl.arnhem.flash.debugger

import nl.arnhem.flash.facebook.FB_URL_BASE
import nl.arnhem.flash.internal.COOKIE
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch

/**
 * Created by Allan Wang on 05/01/18.
 **/
class OfflineWebsiteTest {

    @Test
    fun basic() {
        val countdown = CountDownLatch(1)
        val buildPath = if (File(".").parentFile?.name == "app") "build/offline_test" else "app/build/offline_test"
        OfflineWebsite(FB_URL_BASE, COOKIE, File(buildPath))
                .loadAndZip("test") {
                    println("Outcome $it")
                    countdown.countDown()
                }
        countdown.await()
    }
}