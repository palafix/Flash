package nl.arnhem.flash.enums

import android.content.Context
import android.support.annotation.StringRes
import ca.allanwang.kau.utils.string
import nl.arnhem.flash.R
import nl.arnhem.flash.utils.sendFlashEmail

/**
 * Created by Allan Wang on 2017-06-29.
 **/
enum class Support(@StringRes val title: Int) {
    FEEDBACK(R.string.feedback),
    BUG(R.string.bug_report),
    THEME(R.string.theme_issue),
    FEATURE(R.string.feature_request);

    fun sendEmail(context: Context) {
        with(context) {
            this.sendFlashEmail("${string(R.string.flash_prefix)} ${string(title)}") {
            }
        }
    }
}