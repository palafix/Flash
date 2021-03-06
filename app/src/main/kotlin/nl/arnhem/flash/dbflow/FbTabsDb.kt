package nl.arnhem.flash.dbflow

import com.raizlabs.android.dbflow.annotation.Database
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table
import com.raizlabs.android.dbflow.kotlinextensions.database
import com.raizlabs.android.dbflow.kotlinextensions.fastSave
import com.raizlabs.android.dbflow.kotlinextensions.from
import com.raizlabs.android.dbflow.kotlinextensions.select
import com.raizlabs.android.dbflow.structure.BaseModel
import nl.arnhem.flash.facebook.FbItem
import nl.arnhem.flash.facebook.defaultTabs
import nl.arnhem.flash.utils.L

/**
 * Created by Allan Wang on 2017-05-30.
 **/
const val TAB_COUNT = 5

@Database(version = FbTabsDb.VERSION)
object FbTabsDb {
    const val NAME = "FlashTabs"
    const val VERSION = 1
}

@Table(database = FbTabsDb::class, allFields = true)
data class FbTabModel(@PrimaryKey var position: Int = -1, var tab: FbItem = FbItem.FEED) : BaseModel()

/**
 * Load tabs synchronously
 * Note that tab length should never be a big number anyways
 */
fun loadFbTabs(): List<FbItem> {
    val tabs: List<FbTabModel>? = (select from (FbTabModel::class)).orderBy(FbTabModel_Table.position, true).queryList()
    if (tabs?.size == TAB_COUNT) return tabs.map(FbTabModel::tab)
    L.d { "No tabs (${tabs?.size}); loading default" }
    return defaultTabs()
}

fun List<FbItem>.save() {
    database<FbTabsDb>().beginTransactionAsync(mapIndexed(::FbTabModel).fastSave().build()).execute()
}