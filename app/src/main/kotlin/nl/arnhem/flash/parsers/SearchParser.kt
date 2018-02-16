package nl.arnhem.flash.parsers

import ca.allanwang.kau.searchview.SearchItem
import nl.arnhem.flash.facebook.FbItem
import nl.arnhem.flash.facebook.formattedFbUrl
import nl.arnhem.flash.parsers.FlashSearch.Companion.create
import nl.arnhem.flash.utils.L
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Created by Allan Wang on 2017-10-09.
 */
object SearchParser : FlashParser<FlashSearches> by SearchParserImpl() {
    fun query(cookie: String?, input: String): ParseResponse<FlashSearches>? {
        val url = "${FbItem._SEARCH.url}?q=${if (input.isNotBlank()) input else "a"}"
        L._i { "Search Query $url" }
        return parseFromUrl(cookie, url)
    }
}

enum class SearchKeys(val key: String) {
    USERS("keywords_users"),
    EVENTS("keywords_events")
}

data class FlashSearches(val results: List<FlashSearch>) {

    override fun toString() = StringBuilder().apply {
        append("FlashSearches {\n")
        append(results.toJsonString("results", 1))
        append("}")
    }.toString()
}

/**
 * As far as I'm aware, all links are independent, so the queries don't matter
 * A lot of it is tracking information, which I'll strip away
 * Other text items are formatted for safety
 *
 * Note that it's best to create search results from [create]
 */
data class FlashSearch(val href: String, val title: String, val description: String?) {

    fun toSearchItem() = SearchItem(href, title, description)

    companion object {
        fun create(href: String, title: String, description: String?) = FlashSearch(
                with(href.indexOf("?")) { if (this == -1) href else href.substring(0, this) },
                title.format(),
                description?.format()
        )
    }
}

private class SearchParserImpl : FlashParserBase<FlashSearches>(false) {

    override var nameRes = FbItem._SEARCH.titleId

    override val url = "${FbItem._SEARCH.url}?q=a"

    override fun parseImpl(doc: Document): FlashSearches? {
        val container: Element = doc.getElementById("BrowseResultsContainer")
                ?: doc.getElementById("root")
                ?: return null
        /**
         *
         * Removed [data-store*=result_id]
         */
        return FlashSearches(container.select("a.touchable[href]").filter(Element::hasText).map {
            FlashSearch.create(it.attr("href").formattedFbUrl,
                    it.select("._uoi").first()?.text() ?: "",
                    it.select("._1tcc").first()?.text())
        }.filter { it.title.isNotBlank() })
    }

}