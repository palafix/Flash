package nl.arnhem.flash.utils

import android.util.Log
import ca.allanwang.kau.logging.KauLogger
import com.crashlytics.android.Crashlytics
import nl.arnhem.flash.BuildConfig


/**
 * Created by Allan Wang on 2017-05-28.
 *
 * Logging for Flash
 */
object L : KauLogger("Flash", {
    when (it) {
        Log.VERBOSE -> BuildConfig.DEBUG
        Log.INFO, Log.ERROR -> true
        else -> BuildConfig.DEBUG || Prefs.verboseLogging
    }
}) {

    inline fun _i(message: () -> Any?) {
        if (BuildConfig.DEBUG)
            i(message)
    }

    inline fun _d(message: () -> Any?) {
        if (BuildConfig.DEBUG)
            d(message)
    }

    override fun logImpl(priority: Int, message: String?, t: Throwable?) {
        if (BuildConfig.DEBUG)
            super.logImpl(priority, message, t)
        else {
            if (message != null)
                Crashlytics.log(priority, tag, message)
            if (t != null)
                Crashlytics.logException(t)
        }
    }

}