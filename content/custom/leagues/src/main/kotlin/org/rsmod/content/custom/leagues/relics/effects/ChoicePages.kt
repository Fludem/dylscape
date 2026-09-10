package org.rsmod.content.custom.leagues.relics.effects

import org.rsmod.api.player.protect.ProtectedAccess

/**
 * A chat-option menu over any number of [entries], four to a page with "More..." cycling through
 * the pages. Returns the pick, or `null` when the menu was closed without one.
 *
 * `choice5` is the widest chat menu there is, so a page holds four entries plus "More...". On the
 * last page "More..." wraps back to the first.
 */
internal suspend fun <T : Any> ProtectedAccess.choosePaged(
    entries: List<T>,
    title: String,
    label: (T) -> String,
): T? {
    if (entries.isEmpty()) {
        return null
    }
    val pages = entries.chunked(PER_PAGE)
    var page = 0
    while (true) {
        val pick = choosePage(pages[page], title, label, more = pages.size > 1)
        when (pick) {
            is Pick.Chosen -> return pick.value
            Pick.More -> page = (page + 1) % pages.size
        }
    }
}

private sealed interface Pick<out T> {
    data class Chosen<T>(val value: T) : Pick<T>

    data object More : Pick<Nothing>
}

private suspend fun <T : Any> ProtectedAccess.choosePage(
    page: List<T>,
    title: String,
    label: (T) -> String,
    more: Boolean,
): Pick<T> {
    val options = page.map { label(it) to Pick.Chosen(it) } + listOfNotNull(MORE.takeIf { more })
    return when (options.size) {
        5 ->
            choice5(
                options[0].first,
                options[0].second,
                options[1].first,
                options[1].second,
                options[2].first,
                options[2].second,
                options[3].first,
                options[3].second,
                options[4].first,
                options[4].second,
                title,
            )
        4 ->
            choice4(
                options[0].first,
                options[0].second,
                options[1].first,
                options[1].second,
                options[2].first,
                options[2].second,
                options[3].first,
                options[3].second,
                title,
            )
        3 ->
            choice3(
                options[0].first,
                options[0].second,
                options[1].first,
                options[1].second,
                options[2].first,
                options[2].second,
                title,
            )
        2 ->
            choice2(options[0].first, options[0].second, options[1].first, options[1].second, title)
        else -> options[0].second
    }
}

private const val PER_PAGE = 4

private val MORE: Pair<String, Pick<Nothing>> = "More..." to Pick.More
