package nl.arnhem.flash.activities

import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import ca.allanwang.kau.about.AboutActivityBase
import ca.allanwang.kau.about.LibraryIItem
import ca.allanwang.kau.adapters.FastItemThemedAdapter
import ca.allanwang.kau.adapters.ThemableIItem
import ca.allanwang.kau.adapters.ThemableIItemDelegate
import ca.allanwang.kau.utils.*
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.typeface.IIcon
import nl.arnhem.flash.BuildConfig
import nl.arnhem.flash.R
import nl.arnhem.flash.utils.L
import nl.arnhem.flash.utils.Prefs


/**
 * Created by Allan Wang on 2017-06-26.
 **/
class AboutActivity : AboutActivityBase(null, {
    textColor = Prefs.textColor
    accentColor = Prefs.accentColor
    backgroundColor = Prefs.bgColor.withMinAlpha(200)
    cutoutForeground = Prefs.accentColor
    cutoutDrawableRes = R.drawable.flash_f_256
    faqPageTitleRes = R.string.faq_title
    faqXmlRes = R.xml.flash_faq
    faqParseNewLine = false
}) {

    override fun getLibraries(libs: Libs): List<Library> {
        val include = arrayOf(
                "AboutLibraries",
                "AndroidIconics",
                "androidin_appbillingv3",
                "androidslidinguppanel",
                "Crashlytics",
                "dbflow",
                "fastadapter",
                "glide",
                "Jsoup",
                "kau",
                "kotterknife",
                "materialdialogs",
                "materialdrawer",
                "rxjava",
                "subsamplingscaleimageview",
                "Realm"
        )

        //        l.forEach { KL.d{"Lib ${it.definedName}"} }
        return libs.prepareLibraries(this, include, null, false, true, true)
    }

    private var lastClick = -1L
    private var clickCount = 0

    override fun postInflateMainPage(adapter: FastItemThemedAdapter<IItem<*, *>>) {
        /**
         * Flash may not be a library but we're conveying the same info
         */
        val flash = Library().apply {
            libraryName = string(R.string.flash_name)
            author = string(R.string.dev_name)
            libraryWebsite = string(R.string.github_url)
            isOpenSource = true
            libraryDescription = string(R.string.flash_description)
            libraryVersion = BuildConfig.VERSION_NAME
            license = License().apply {
                licenseName = "GNU GPL v3"
                licenseWebsite = "https://www.gnu.org/licenses/gpl-3.0.en.html"
            }
        }
        adapter.add(LibraryIItem(flash)).add(AboutLinks())
        adapter.withOnClickListener { _, _, item, _ ->
            if (item is LibraryIItem) {
                val now = System.currentTimeMillis()
                if (now - lastClick > 500)
                    clickCount = 0
                else
                    clickCount++
                lastClick = now
                if (clickCount == 7 && !Prefs.debugSettings) {
                    Prefs.debugSettings = true
                    L.d { "Debugging section enabled" }
                    toast(R.string.debug_toast_enabled)
                } else if (clickCount == 10 && Prefs.debugSettings) {
                    Prefs.debugSettings = false
                    L.d { "Debugging section disabled" }
                    toast(R.string.debug_toast_disabled)
                } else if (clickCount == 50 && !Prefs.freeProSettings) {
                    Prefs.freeProSettings = true
                    L.d { "Flash [Pro-Free] section enabled" }
                    toast(R.string.pro_free_toast_enabled)
                } else if (clickCount == 60 && Prefs.freeProSettings) {
                    Prefs.freeProSettings = false
                    L.d { "Flash [Pro-Free] section disabled" }
                    toast(R.string.pro_free_toast_disabled)
                }
                if (clickCount >= 25) {
                    toast(clickCount.toString())
                }
            }
            false
        }
    }

    class AboutLinks : AbstractItem<AboutLinks, AboutLinks.ViewHolder>(), ThemableIItem by ThemableIItemDelegate() {
        override fun getViewHolder(v: View): ViewHolder = ViewHolder(v)

        override fun getType(): Int = R.id.item_about_links

        override fun getLayoutRes(): Int = R.layout.item_about_links

        override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
            super.bindView(holder, payloads)
            with(holder) {
                bindIconColor(*images.toTypedArray())
                bindBackgroundColor(container)
            }
        }

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {

            val container: ConstraintLayout by bindView(R.id.about_icons_container)
            val images: List<ImageView>

            /**
             * There are a lot of constraints to be added to each item just to have them chained properly
             * My as well do it programmatically
             * Initializing the viewholder will setup the icons, scale type and background of all icons,
             * link their click listeners and chain them together via a horizontal spread
             */
            init {
                val c = itemView.context
                val size = c.dimenPixelSize(R.dimen.kau_avatar_bounds)
                images = arrayOf<Pair<IIcon, () -> Unit>>(
                        GoogleMaterial.Icon.gmd_flash_on to { c.startPlayStoreLink(R.string.play_store_package_id) },
                        CommunityMaterial.Icon.cmd_github_circle to { c.startLink(R.string.github_url) },
                        GoogleMaterial.Icon.gmd_ac_unit to { c.startLink(R.string.frost_url) }
                ).mapIndexed { i, (icon, onClick) ->
                    ImageView(c).apply {
                        layoutParams = ViewGroup.LayoutParams(size, size)
                        id = 109389 + i
                        setImageDrawable(icon.toDrawable(context, 32))
                        scaleType = ImageView.ScaleType.CENTER
                        background = context.resolveDrawable(android.R.attr.selectableItemBackgroundBorderless)
                        setOnClickListener { onClick() }
                        container.addView(this)
                    }
                }
                val set = ConstraintSet()
                set.clone(container)
                set.createHorizontalChain(ConstraintSet.PARENT_ID, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT,
                        images.map { it.id }.toIntArray(), null, ConstraintSet.CHAIN_SPREAD_INSIDE)
                set.applyTo(container)
            }
        }
    }
}