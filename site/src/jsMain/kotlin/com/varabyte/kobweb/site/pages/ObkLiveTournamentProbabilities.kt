package com.varabyte.kobweb.site.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.border
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.flexWrap
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.fontWeight
import com.varabyte.kobweb.compose.ui.modifiers.gap
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.lineHeight
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.maxWidth
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.size
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.core.rememberPageContext
import com.varabyte.kobweb.site.components.layouts.PageLayoutData
import com.varabyte.kobweb.site.components.widgets.PoolBall
import kotlinx.browser.window
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import kotlin.math.pow

private const val CUESCORE_API_BASE = "https://api.cuescore.com"
private const val OBK_TOURNAMENTS_URL = "$CUESCORE_API_BASE/tournaments/?view=all&type=tournaments&q=Oslo%20BK&offset=0"
private const val OBK_GITHUB_RAW_BASE = "https://raw.githubusercontent.com/Flavelloni/Cue-Score/main/combined"

private data class LiveTournamentSummary(
    val tournamentId: Int,
    val name: String,
    val starttime: String,
    val stoptime: String,
)

private data class LiveTournament(
    val tournamentId: Int,
    val name: String,
    val discipline: String,
    val starttime: String,
    val stoptime: String,
    val defaultRaceTo: Int,
    val matches: List<LiveMatch>,
)

private data class LiveMatch(
    val matchId: Int,
    val roundName: String,
    val playerA: LivePlayer,
    val playerB: LivePlayer,
    val scoreA: Int,
    val scoreB: Int,
    val raceTo: Int,
    val tableName: String,
    val starttime: String,
    val stoptime: String,
    val matchstatus: String,
    val handicapA: Int,
    val handicapB: Int,
)

private data class LivePlayer(
    val id: String,
    val name: String,
    val image: String,
)

private data class LiveTable(
    val name: String,
    val matches: List<LiveMatch>,
)

private data class LiveFargoPlayer(
    val id: String,
    val name: String,
    val image: String,
    val rating: Double,
    val games: Int,
)

private data class LivePairStats(
    val firstPlayerId: String,
    val secondPlayerId: String,
    val firstWins: Int,
    val secondWins: Int,
    val games: Int,
)

private var cachedLiveTournament: LiveTournament? = null
private var cachedLivePlayers: List<LiveFargoPlayer> = emptyList()
private var cachedLivePairs: List<LivePairStats> = emptyList()
private var cachedLiveUpdatedAt: String = ""

@InitRoute
fun initObkLiveTournamentProbabilitiesPage(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("Oslo BK live tournament probabilities", "Live Oslo BK table pages with Performance Rating probabilities."))
}

@Page("/obk-live-tournament-probabilities")
@Composable
@Layout(".components.layouts.PageLayout")
fun ObkLiveTournamentProbabilitiesPage() {
    var tournament by remember { mutableStateOf(cachedLiveTournament) }
    var players by remember { mutableStateOf(cachedLivePlayers) }
    var pairs by remember { mutableStateOf(cachedLivePairs) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var updatedAt by remember { mutableStateOf(cachedLiveUpdatedAt) }
    var historyBack by remember { mutableStateOf(0) }
    var manualRefreshEnabled by remember { mutableStateOf(true) }
    val ctx = rememberPageContext()
    val tableQuery = ctx.route.queryParams["table"]?.takeIf { it.isNotBlank() }

    fun refreshTournament() {
        loadLatestObkTournament(
            onLoaded = {
                cachedLiveTournament = it
                cachedLiveUpdatedAt = currentTimeText()
                tournament = it
                loadError = null
                updatedAt = cachedLiveUpdatedAt
            },
            onError = { loadError = it },
        )
    }

    LaunchedEffect(Unit) {
        if (cachedLiveTournament == null) {
            refreshTournament()
        }
        if (cachedLivePlayers.isEmpty()) {
            fetchText(
                "$OBK_GITHUB_RAW_BASE/player_fargo_ratings.csv",
                onSuccess = {
                    cachedLivePlayers = parseLiveFargoPlayers(it)
                    players = cachedLivePlayers
                },
                onError = {},
            )
        }
        if (cachedLivePairs.isEmpty()) {
            fetchText(
                "$OBK_GITHUB_RAW_BASE/player_pairs.csv",
                onSuccess = {
                    cachedLivePairs = parseLivePairStats(it)
                    pairs = cachedLivePairs
                },
                onError = {},
            )
        }
    }

    DisposableEffect(Unit) {
        val timerId = window.setInterval({ refreshTournament() }, 600000)
        onDispose { window.clearInterval(timerId) }
    }

    fun manualRefreshTournament() {
        if (!manualRefreshEnabled) return
        manualRefreshEnabled = false
        refreshTournament()
        window.setTimeout({ manualRefreshEnabled = true }, 15000)
    }

    val loadedTournament = tournament
    val tables = loadedTournament?.matches.orEmpty()
        .filter { it.tableName.isNotBlank() }
        .groupBy { it.tableName }
        .map { (name, matches) -> LiveTable(name, matches.sortedBy { matchSortTime(it) }) }
        .sortedWith(compareBy<LiveTable> { it.name.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it.name })
    val selectedTable = tableQuery?.let { query -> tables.firstOrNull { it.name == query } }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(leftRight = 1.25.cssRem, top = 2.cssRem, bottom = 4.cssRem)
            .gap(1.2.cssRem)
            .styleModifier {
                property("min-height", "calc(100vh - 64px)")
                property("background", "linear-gradient(145deg, #071012 0%, #12201a 48%, #17131a 100%)")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.fillMaxWidth().maxWidth(1180.px).gap(0.7.cssRem)) {
            H1(
                attrs = Modifier
                    .margin(0.px)
                    .fontSize(2.35.cssRem)
                    .lineHeight(1.05)
                    .fontWeight(FontWeight.Bold)
                    .color(Colors.White)
                    .toAttrs()
            ) {
                Text("Oslo BK live tournament probabilities")
            }
            P(
                attrs = Modifier
                    .margin(0.px)
                    .fontSize(0.98.cssRem)
                    .lineHeight(1.55)
                    .color(Color.rgba(245, 248, 244, 0.72f))
                    .toAttrs()
            ) {
                Text("Updates when user refreshes manually.")
            }
        }

        when {
            loadError != null -> LiveMessage(loadError ?: "Could not load live tournament.")
            loadedTournament == null -> LiveLoadingMessage()
            selectedTable != null -> TablePage(
                tournament = loadedTournament,
                table = selectedTable,
                players = players,
                pairs = pairs,
                updatedAt = updatedAt,
                historyBack = historyBack.coerceAtMost((selectedTable.matches.size - 1).coerceAtLeast(0)),
                onHistoryBack = { historyBack = it.coerceIn(0, (selectedTable.matches.size - 1).coerceAtLeast(0)) },
                manualRefreshEnabled = manualRefreshEnabled,
                onRefresh = { manualRefreshTournament() },
                onAllTables = { ctx.router.navigateTo("/obk-live-tournament-probabilities") },
            )
            else -> TournamentOverview(
                tournament = loadedTournament,
                tables = tables,
                updatedAt = updatedAt,
                onTable = { tableName ->
                    ctx.router.navigateTo("/obk-live-tournament-probabilities?table=${encodeURIComponent(tableName)}")
                },
            )
        }
    }
}

@Composable
private fun TournamentOverview(
    tournament: LiveTournament,
    tables: List<LiveTable>,
    updatedAt: String,
    onTable: (String) -> Unit,
) {
    LivePanel {
        TournamentHeader(tournament, updatedAt)
        if (isTournamentOver(tournament)) {
            LiveMutedText("Tournament is over. Showing final table history from the latest started Oslo BK tournament.")
        } else {
            LiveMutedText("Tournament is live or scheduled to continue. Open a table page for QR-specific display.")
        }
        if (tables.isEmpty()) {
            LiveMessage("No table matches found in the tournament data yet.")
        } else {
            Row(
                Modifier.fillMaxWidth().gap(0.65.cssRem).flexWrap(FlexWrap.Wrap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tables.forEach { table ->
                    Button(
                        attrs = Modifier
                            .padding(leftRight = 0.95.cssRem, topBottom = 0.72.cssRem)
                            .borderRadius(14.px)
                            .backgroundColor(Color.rgba(255, 116, 92, 0.16f))
                            .border(1.px, LineStyle.Solid, Color.rgba(255, 116, 92, 0.38f))
                            .color(Colors.White)
                            .fontWeight(FontWeight.Bold)
                            .styleModifier {
                                property("display", "inline-flex")
                                property("cursor", "pointer")
                            }
                            .toAttrs { onClick { onTable(table.name) } }
                    ) {
                        Text("Table ${table.name}")
                    }
                }
            }
        }
    }
}

@Composable
private fun TablePage(
    tournament: LiveTournament,
    table: LiveTable,
    players: List<LiveFargoPlayer>,
    pairs: List<LivePairStats>,
    updatedAt: String,
    historyBack: Int,
    onHistoryBack: (Int) -> Unit,
    manualRefreshEnabled: Boolean,
    onRefresh: () -> Unit,
    onAllTables: () -> Unit,
) {
    val matchIndex = (table.matches.lastIndex - historyBack).coerceIn(0, table.matches.lastIndex)
    val match = table.matches[matchIndex]
    val fargoA = findFargoPlayer(players, match.playerA)
    val fargoB = findFargoPlayer(players, match.playerB)
    val rackProbabilityA = if (fargoA != null && fargoB != null) fargoGameProbability(fargoA.rating, fargoB.rating) else null
    val matchProbabilityA = rackProbabilityA?.let {
        matchWinProbabilityWithHandicap(it, match.raceTo, match.handicapA, match.handicapB)
    }
    val currentMatchProbabilityA = rackProbabilityA?.let {
        matchWinProbabilityFromScore(it, match.raceTo, match.scoreA, match.scoreB)
    }
    val h2h = if (fargoA != null && fargoB != null) findPairStats(pairs, fargoA.id, fargoB.id) else null

    LivePanel {
        TournamentHeader(tournament, updatedAt)
        Row(
            Modifier.fillMaxWidth().gap(1.cssRem).flexWrap(FlexWrap.Wrap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Span(
                attrs = Modifier
                    .fontSize(2.35.cssRem)
                    .fontWeight(FontWeight.Bold)
                    .lineHeight(1.0)
                    .color(Color.rgb(255, 210, 132))
                    .toAttrs()
            ) {
                Text("Table ${table.name}")
            }
            Span(
                attrs = Modifier
                    .fontSize(0.92.cssRem)
                    .fontWeight(FontWeight.SemiBold)
                    .color(Color.rgba(245, 248, 244, 0.62f))
                    .toAttrs()
            ) {
                Text("${matchIndex + 1} / ${table.matches.size}")
            }
        }
        Row(
            Modifier.fillMaxWidth().gap(0.75.cssRem).flexWrap(FlexWrap.Wrap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                attrs = Modifier
                    .padding(leftRight = 0.8.cssRem, topBottom = 0.55.cssRem)
                    .borderRadius(999.px)
                    .backgroundColor(Color.rgba(255, 255, 255, 0.09f))
                    .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.14f))
                    .color(Colors.White)
                    .styleModifier { property("cursor", "pointer") }
                    .toAttrs { onClick { onAllTables() } }
            ) {
                Text("All tables")
            }
            LiveChip("Race to ${match.raceTo}")
            LiveChip(match.matchstatus.ifBlank { "unknown" })
            LiveRefreshButton(enabled = manualRefreshEnabled, onClick = onRefresh)
        }

        H2(
            attrs = Modifier
                .margin(0.px)
                .fontSize(1.35.cssRem)
                .lineHeight(1.2)
                .fontWeight(FontWeight.Bold)
                .color(Colors.White)
                .toAttrs()
        ) {
            Text(match.roundName.ifBlank { "Current match" })
        }

//        Row(
//            Modifier.fillMaxWidth().gap(0.9.cssRem).flexWrap(FlexWrap.Wrap),
//            verticalAlignment = Alignment.Top,
//        ) {
//            PlayerProbabilityCard(match.playerA, fargoA, matchProbabilityA, currentMatchProbabilityA, match.scoreA, match.scoreB)
//            PlayerProbabilityCard(match.playerB, fargoB, matchProbabilityA?.let { 1.0 - it }, currentMatchProbabilityA?.let { 1.0 - it }, match.scoreB, match.scoreA)
//        }

        CombinedMatchCard(
            match = match,
            fargoA = fargoA,
            fargoB = fargoB,
            initialProbabilityA = matchProbabilityA,
            currentProbabilityA = currentMatchProbabilityA,
        )

        Row(
            Modifier.fillMaxWidth().gap(0.75.cssRem).flexWrap(FlexWrap.Wrap),
            verticalAlignment = Alignment.Top,
        ) {
            DetailCard("Handicap", "${match.handicapA}:${match.handicapB}", "Start score.")
            DetailCard("Rack win", rackProbabilityA?.let { "${formatPercent(it)} / ${formatPercent(1.0 - it)}" } ?: "-", "Based on player Performance Ratings.")
            DetailCard("Head-to-head rack stats", h2h?.let { "${it.firstWins}-${it.secondWins} (${it.games})" } ?: "-", "Player-pair rack history when available.")
        }

        Row(
            Modifier.fillMaxWidth().gap(0.75.cssRem).flexWrap(FlexWrap.Wrap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LiveNavButton("Previous", enabled = historyBack < table.matches.lastIndex) { onHistoryBack(historyBack + 1) }
            LiveNavButton("Latest", enabled = historyBack != 0) { onHistoryBack(0) }
            LiveNavButton("Next", enabled = historyBack > 0) { onHistoryBack(historyBack - 1) }
        }
    }
}

@Composable
private fun CombinedMatchCard(
    match: LiveMatch,
    fargoA: LiveFargoPlayer?,
    fargoB: LiveFargoPlayer?,
    initialProbabilityA: Double?,
    currentProbabilityA: Double?,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(1.cssRem)
            .borderRadius(18.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.075f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.12f))
            .gap(0.85.cssRem)
            .styleModifier { property("box-sizing", "border-box") },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .gap(0.8.cssRem)
                .styleModifier {
                    property("display", "grid")
                    property("grid-template-columns", "minmax(0, 1fr) auto minmax(0, 1fr)")
                    property("align-items", "center")
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CombinedPlayerSide(match.playerA, fargoA, alignEnd = false)
            Row(
                Modifier.gap(0.62.cssRem),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScoreNumber(match.scoreA)
                Span(
                    attrs = Modifier
                        .fontSize(2.3.cssRem)
                        .fontWeight(FontWeight.Bold)
                        .lineHeight(1.0)
                        .color(Color.rgba(245, 248, 244, 0.58f))
                        .toAttrs()
                ) {
                    Text("-")
                }
                ScoreNumber(match.scoreB)
            }
            CombinedPlayerSide(match.playerB, fargoB, alignEnd = true)
        }

        CombinedProbabilityStack(
            currentProbabilityA = currentProbabilityA,
            currentProbabilityB = currentProbabilityA?.let { 1.0 - it },
            initialProbabilityA = initialProbabilityA,
            initialProbabilityB = initialProbabilityA?.let { 1.0 - it },
        )
    }
}

@Composable
private fun CombinedPlayerSide(player: LivePlayer, fargo: LiveFargoPlayer?, alignEnd: Boolean) {
    Column(
        Modifier
            .gap(0.45.cssRem)
            .styleModifier {
                property("min-width", "0")
                property("justify-self", if (alignEnd) "end" else "start")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PlayerImage(player)
        Column(
            Modifier
                .gap(0.18.cssRem)
                .styleModifier {
                    property("min-width", "0")
                    property("text-align", "center")
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Span(
                attrs = Modifier
                    .fontSize(1.02.cssRem)
                    .fontWeight(FontWeight.Bold)
                    .lineHeight(1.18)
                    .color(Colors.White)
                    .styleModifier { property("overflow-wrap", "anywhere") }
                    .toAttrs()
            ) {
                Text(player.name.ifBlank { "Unknown player" })
            }
            Span(
                attrs = Modifier
                    .fontSize(0.78.cssRem)
                    .color(Color.rgba(245, 248, 244, 0.58f))
                    .toAttrs()
            ) {
                Text("PR ${fargo?.rating?.toInt()?.toString() ?: "-"}")
            }
        }
    }
}

@Composable
private fun ScoreNumber(score: Int) {
    Span(
        attrs = Modifier
            .fontSize(3.cssRem)
            .fontWeight(FontWeight.Bold)
            .lineHeight(1.0)
            .color(Colors.White)
            .toAttrs()
    ) {
        Text(score.toString())
    }
}

@Composable
private fun CombinedProbabilityStack(
    currentProbabilityA: Double?,
    currentProbabilityB: Double?,
    initialProbabilityA: Double?,
    initialProbabilityB: Double?,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(leftRight = 0.9.cssRem, topBottom = 0.78.cssRem)
            .borderRadius(14.px)
            .backgroundColor(Color.rgba(0, 0, 0, 0.16f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.09f))
            .gap(0.55.cssRem)
            .styleModifier {
                property("box-sizing", "border-box")
                property("max-width", "560px")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CombinedProbabilityRow("Current win probability", currentProbabilityA, currentProbabilityB, large = true)
        CombinedProbabilityRow("Initial win probability", initialProbabilityA, initialProbabilityB, large = false)
    }
}

@Composable
private fun CombinedProbabilityRow(label: String, probabilityA: Double?, probabilityB: Double?, large: Boolean) {
    Column(
        Modifier.fillMaxWidth().gap(0.32.cssRem),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Span(
            attrs = Modifier
                .fontSize(0.76.cssRem)
                .fontWeight(FontWeight.Bold)
                .lineHeight(1.2)
                .color(Color.rgba(245, 248, 244, 0.58f))
                .toAttrs()
        ) {
            Text(label)
        }
        Row(Modifier.fillMaxWidth().gap(1.cssRem), verticalAlignment = Alignment.CenterVertically) {
            CombinedProbabilityValue(probabilityA, alignEnd = false, large = large)
            CombinedProbabilityValue(probabilityB, alignEnd = true, large = large)
        }
    }
}

@Composable
private fun CombinedProbabilityValue(probability: Double?, alignEnd: Boolean, large: Boolean) {
    Span(
        attrs = Modifier
            .fontSize(if (large) 1.7.cssRem else 1.14.cssRem)
            .fontWeight(FontWeight.Bold)
            .lineHeight(1.0)
            .color(if (large) Color.rgb(255, 210, 132) else Color.rgba(245, 248, 244, 0.82f))
            .styleModifier {
                property("flex", "1 1 0")
                property("text-align", if (alignEnd) "right" else "left")
            }
            .toAttrs()
    ) {
        Text(probability?.let { formatPercent(it) } ?: "-")
    }
}

@Composable
private fun PlayerProbabilityCard(
    player: LivePlayer,
    fargo: LiveFargoPlayer?,
    initialProbability: Double?,
    currentProbability: Double?,
    score: Int,
    opponentScore: Int,
) {
    Column(
        Modifier
            .padding(1.cssRem)
            .borderRadius(18.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.075f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.12f))
            .gap(0.75.cssRem)
            .styleModifier { property("flex", "1 1 300px") },
    ) {
        Row(Modifier.fillMaxWidth().gap(0.75.cssRem), verticalAlignment = Alignment.CenterVertically) {
            PlayerImage(player)
            Column(Modifier.gap(0.2.cssRem).styleModifier { property("min-width", "0") }) {
                Span(
                    attrs = Modifier
                        .fontSize(1.12.cssRem)
                        .fontWeight(FontWeight.Bold)
                        .lineHeight(1.18)
                        .color(Colors.White)
                        .styleModifier { property("overflow-wrap", "anywhere") }
                        .toAttrs()
                ) {
                    Text(player.name.ifBlank { "Unknown player" })
                }
                Span(
                    attrs = Modifier
                        .fontSize(0.86.cssRem)
                        .color(Color.rgba(245, 248, 244, 0.62f))
                        .toAttrs()
                ) {
                    Text("Current rack standing: $score-$opponentScore")
                }
            }
        }
        Row(Modifier.fillMaxWidth().gap(0.65.cssRem).flexWrap(FlexWrap.Wrap), verticalAlignment = Alignment.Top) {
            ProbabilityBlock("Initial win probability", initialProbability)
            ProbabilityBlock("Current win probability", currentProbability)
        }
        LiveStatLine("Performance Rating", fargo?.rating?.toInt()?.toString() ?: "-")
    }
}

@Composable
private fun ProbabilityBlock(label: String, probability: Double?) {
    Column(
        Modifier
            .padding(0.68.cssRem)
            .borderRadius(14.px)
            .backgroundColor(Color.rgba(0, 0, 0, 0.16f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.09f))
            .gap(0.25.cssRem)
            .styleModifier { property("flex", "1 1 130px") },
    ) {
        Span(attrs = Modifier.fontSize(0.76.cssRem).fontWeight(FontWeight.Bold).color(Color.rgba(245, 248, 244, 0.58f)).toAttrs()) {
            Text(label)
        }
        Span(attrs = Modifier.fontSize(1.55.cssRem).fontWeight(FontWeight.Bold).lineHeight(1.0).color(Color.rgb(255, 210, 132)).toAttrs()) {
            Text(probability?.let { formatPercent(it) } ?: "-")
        }
    }
}

@Composable
private fun PlayerImage(player: LivePlayer) {
    val image = normalizeImageUrl(player.image)
    if (image.isBlank()) {
        Div(
            attrs = Modifier
                .size(64.px)
                .borderRadius(50.percent)
                .backgroundColor(Color.rgba(255, 255, 255, 0.12f))
                .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.16f))
                .toAttrs()
        )
    } else {
        Img(
            src = image,
            attrs = Modifier
                .size(64.px)
                .borderRadius(50.percent)
                .backgroundColor(Color.rgba(255, 255, 255, 0.12f))
                .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.16f))
                .styleModifier { property("object-fit", "cover") }
                .toAttrs { attr("alt", player.name) }
        )
    }
}

@Composable
private fun DetailCard(label: String, value: String, detail: String) {
    Column(
        Modifier
            .padding(0.85.cssRem)
            .borderRadius(14.px)
            .backgroundColor(Color.rgba(0, 0, 0, 0.16f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.1f))
            .gap(0.25.cssRem)
            .styleModifier { property("flex", "1 1 220px") },
    ) {
        Span(attrs = Modifier.fontSize(0.78.cssRem).fontWeight(FontWeight.Bold).color(Color.rgba(245, 248, 244, 0.55f)).toAttrs()) {
            Text(label)
        }
        Span(attrs = Modifier.fontSize(1.12.cssRem).fontWeight(FontWeight.Bold).color(Colors.White).toAttrs()) {
            Text(value)
        }
        Span(attrs = Modifier.fontSize(0.82.cssRem).lineHeight(1.35).color(Color.rgba(245, 248, 244, 0.55f)).toAttrs()) {
            Text(detail)
        }
    }
}

@Composable
private fun TournamentHeader(tournament: LiveTournament, updatedAt: String) {
    Column(Modifier.fillMaxWidth().gap(0.45.cssRem)) {
        Span(
            attrs = Modifier
                .fontSize(1.15.cssRem)
                .fontWeight(FontWeight.Bold)
                .lineHeight(1.2)
                .color(Colors.White)
                .styleModifier { property("overflow-wrap", "anywhere") }
                .toAttrs()
        ) {
            Text(tournament.name)
        }
        Row(Modifier.gap(0.45.cssRem).flexWrap(FlexWrap.Wrap), verticalAlignment = Alignment.CenterVertically) {
            LiveChip(tournament.discipline.ifBlank { "Tournament ${tournament.tournamentId}" })
            LiveChip(if (isTournamentOver(tournament)) "Over" else "Live")
            if (updatedAt.isNotBlank()) LiveChip("Updated $updatedAt")
        }
    }
}

@Composable
private fun LivePanel(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .maxWidth(1180.px)
            .padding(1.15.cssRem)
            .borderRadius(22.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.08f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.15f))
            .gap(1.cssRem)
            .styleModifier {
                property("box-shadow", "0 26px 70px rgba(0, 0, 0, 0.28)")
                property("box-sizing", "border-box")
            },
    ) {
        content()
    }
}

@Composable
private fun LiveMessage(message: String) {
    P(
        attrs = Modifier
            .fillMaxWidth()
            .maxWidth(1180.px)
            .margin(0.px)
            .fontSize(1.cssRem)
            .color(Color.rgba(245, 248, 244, 0.78f))
            .toAttrs()
    ) {
        Text(message)
    }
}

@Composable
private fun LiveLoadingMessage() {
    var visibleBalls by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        val timerId = window.setInterval({
            visibleBalls = (visibleBalls + 1) % 6
        }, 260)
        onDispose { window.clearInterval(timerId) }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .maxWidth(1180.px)
            .gap(0.75.cssRem),
        horizontalAlignment = Alignment.Start,
    ) {
        LiveMessage("Loading latest Oslo BK tournament...")
        Row(
            Modifier.gap(0.5.cssRem),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            (1..5).forEach { ball ->
                Div(
                    attrs = Modifier
                        .width(34.px)
                        .height(34.px)
                        .styleModifier {
                            property("opacity", if (ball <= visibleBalls) "1" else "0.18")
                            property("transform", if (ball <= visibleBalls) "translateY(0)" else "translateY(5px)")
                            property("transition", "opacity 180ms ease, transform 180ms ease")
                        }
                        .toAttrs()
                ) {
                    PoolBall(ballNumber = ball, fillParent = true)
                }
            }
        }
    }
}

@Composable
private fun LiveMutedText(message: String) {
    P(
        attrs = Modifier
            .margin(0.px)
            .fontSize(0.94.cssRem)
            .lineHeight(1.5)
            .color(Color.rgba(245, 248, 244, 0.64f))
            .toAttrs()
    ) {
        Text(message)
    }
}

@Composable
private fun LiveChip(text: String) {
    Span(
        attrs = Modifier
            .padding(leftRight = 0.68.cssRem, topBottom = 0.38.cssRem)
            .borderRadius(999.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.08f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.13f))
            .fontSize(0.84.cssRem)
            .fontWeight(FontWeight.SemiBold)
            .color(Color.rgba(245, 248, 244, 0.78f))
            .toAttrs()
    ) {
        Text(text)
    }
}

@Composable
private fun LiveStatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().gap(0.65.cssRem), verticalAlignment = Alignment.CenterVertically) {
        Span(attrs = Modifier.width(7.5.cssRem).fontSize(0.84.cssRem).color(Color.rgba(245, 248, 244, 0.58f)).toAttrs()) {
            Text(label)
        }
        Span(attrs = Modifier.fontSize(0.92.cssRem).fontWeight(FontWeight.SemiBold).color(Color.rgba(245, 248, 244, 0.9f)).toAttrs()) {
            Text(value)
        }
    }
}

@Composable
private fun LiveNavButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        attrs = Modifier
            .padding(leftRight = 0.95.cssRem, topBottom = 0.65.cssRem)
            .borderRadius(999.px)
            .backgroundColor(Color.rgba(255, 255, 255, if (enabled) 0.1f else 0.045f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, if (enabled) 0.18f else 0.08f))
            .color(if (enabled) Colors.White else Color.rgba(245, 248, 244, 0.42f))
            .fontWeight(FontWeight.Bold)
            .styleModifier { property("cursor", if (enabled) "pointer" else "not-allowed") }
            .toAttrs {
                if (enabled) onClick { onClick() }
            }
    ) {
        Text(label)
    }
}

@Composable
private fun LiveRefreshButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        attrs = Modifier
            .padding(leftRight = 1.15.cssRem, topBottom = 0.72.cssRem)
            .borderRadius(999.px)
            .backgroundColor(Color.rgba(255, 116, 92, 0.28f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 116, 92, 0.62f))
            .color(Colors.White)
            .fontWeight(FontWeight.Bold)
            .styleModifier {
                property("cursor", "pointer")
                property("box-shadow", "0 10px 28px rgba(255, 116, 92, 0.18)")
            }
            .toAttrs {
                onClick {
                    if (enabled) onClick()
                }
            }
    ) {
        Text("Refresh live data")
    }
}

private fun loadLatestObkTournament(onLoaded: (LiveTournament) -> Unit, onError: (String) -> Unit) {
    fetchText(
        OBK_TOURNAMENTS_URL,
        onSuccess = { text ->
            val summaries = parseTournamentSummaries(text)
            val now = js("Date.now()") as Double
            val latest = summaries
                .filter { parseDateMs(it.starttime) <= now }
                .maxByOrNull { parseDateMs(it.starttime) }
                ?: summaries.minByOrNull { parseDateMs(it.starttime) }

            if (latest == null) {
                onError("No Oslo BK tournaments found.")
                return@fetchText
            }

            fetchText(
                "$CUESCORE_API_BASE/tournament/?id=${latest.tournamentId}",
                onSuccess = { tournamentText -> onLoaded(parseLiveTournament(tournamentText)) },
                onError = { onError("Could not load tournament ${latest.tournamentId}: $it") },
            )
        },
        onError = { onError("Could not load Oslo BK tournament list: $it") },
    )
}

private fun parseTournamentSummaries(json: String): List<LiveTournamentSummary> {
    val root = js("JSON.parse(json)")
    val items = root.items
    val length = dynLength(items)
    return (0 until length).mapNotNull { index ->
        val item = items[index]
        val owner = item.owner
        val ownerStub = dynString(owner, "stub")
        if (ownerStub != "obk") return@mapNotNull null
        LiveTournamentSummary(
            tournamentId = dynInt(item, "tournamentId"),
            name = dynString(item, "name"),
            starttime = dynString(item, "starttime"),
            stoptime = dynString(item, "stoptime"),
        )
    }.filter { it.tournamentId > 0 }
}

private fun parseLiveTournament(json: String): LiveTournament {
    val root = js("JSON.parse(json)")
    val matchesValue = root.matches
    val matches = (0 until dynLength(matchesValue)).mapNotNull { index ->
        parseLiveMatch(matchesValue[index])
    }
    return LiveTournament(
        tournamentId = dynInt(root, "tournamentId"),
        name = dynString(root, "name"),
        discipline = dynString(root, "discipline"),
        starttime = dynString(root, "starttime"),
        stoptime = dynString(root, "stoptime"),
        defaultRaceTo = dynInt(root, "defaultRaceTo").takeIf { it > 0 } ?: 1,
        matches = matches,
    )
}

private fun parseLiveMatch(item: dynamic): LiveMatch? {
    val playerA = parseLivePlayer(item.playerA)
    val playerB = parseLivePlayer(item.playerB)
    if (playerA.name.isBlank() && playerB.name.isBlank()) return null
    val raceTo = dynInt(item, "raceTo").takeIf { it > 0 } ?: 1
    val table = item.table
    val handicaps = parseHandicaps(item.notes)
    return LiveMatch(
        matchId = dynInt(item, "matchId"),
        roundName = dynString(item, "roundName"),
        playerA = playerA,
        playerB = playerB,
        scoreA = dynInt(item, "scoreA"),
        scoreB = dynInt(item, "scoreB"),
        raceTo = raceTo,
        tableName = dynString(table, "name"),
        starttime = dynString(item, "starttime"),
        stoptime = dynString(item, "stoptime"),
        matchstatus = dynString(item, "matchstatus"),
        handicapA = handicaps.first,
        handicapB = handicaps.second,
    )
}

private fun parseLivePlayer(item: dynamic): LivePlayer {
    return LivePlayer(
        id = dynInt(item, "playerId").takeIf { it > 0 }?.toString().orEmpty(),
        name = dynString(item, "name"),
        image = dynString(item, "image"),
    )
}

private fun parseHandicaps(notes: dynamic): Pair<Int, Int> {
    var a = 0
    var b = 0
    for (index in 0 until dynLength(notes)) {
        when (dynString(notes[index], "note")) {
            "A frame win handicap" -> a += 1
            "B frame win handicap" -> b += 1
        }
    }
    return a to b
}

private fun parseLiveFargoPlayers(csv: String): List<LiveFargoPlayer> {
    val lines = csv.lineSequence().filter { it.isNotBlank() }.toList()
    if (lines.isEmpty()) return emptyList()
    val header = parseCsvLine(lines.first())
    val columnIndex = header.withIndex().associate { it.value.cleanCsvHeader() to it.index }
    fun List<String>.value(column: String): String = getOrNull(columnIndex[column] ?: -1).orEmpty()

    return lines.drop(1).mapNotNull { line ->
        val columns = parseCsvLine(line)
        val rating = columns.value("fargo_rating").toDoubleOrNull() ?: return@mapNotNull null
        val wins = columns.value("wins").toIntOrNull() ?: 0
        val losses = columns.value("losses").toIntOrNull() ?: 0
        LiveFargoPlayer(
            id = columns.value("player_id").trim(),
            name = columns.value("player_name").trim(),
            image = columns.value("image").trim(),
            rating = rating,
            games = columns.value("games").toIntOrNull() ?: wins + losses,
        )
    }
}

private fun parseLivePairStats(csv: String): List<LivePairStats> {
    val lines = csv.lineSequence().filter { it.isNotBlank() }.toList()
    if (lines.isEmpty()) return emptyList()
    val header = parseCsvLine(lines.first())
    val columnIndex = header.withIndex().associate { it.value.cleanCsvHeader() to it.index }
    fun List<String>.value(column: String): String = getOrNull(columnIndex[column] ?: -1).orEmpty()
    fun List<String>.valueAny(vararg columns: String): String = columns.firstNotNullOfOrNull { value(it).takeIf(String::isNotBlank) }.orEmpty()

    return lines.drop(1).mapNotNull { line ->
        val columns = parseCsvLine(line)
        val firstId = columns.valueAny("first_player_id", "player1_id", "player_1_id", "player_id_1", "p1_id").trim()
        val secondId = columns.valueAny("second_player_id", "player2_id", "player_2_id", "player_id_2", "p2_id").trim()
        val firstWins = columns.value("player1_wins").toIntOrNull()
        val secondWins = columns.value("player2_wins").toIntOrNull()
        val games = columns.value("games").toIntOrNull()
        if (firstId.isBlank() || secondId.isBlank() || firstWins == null || secondWins == null || games == null || games == 0) {
            null
        } else {
            LivePairStats(firstId, secondId, firstWins, secondWins, games)
        }
    }
}

private fun parseCsvLine(line: String): List<String> {
    val values = mutableListOf<String>()
    val current = StringBuilder()
    var quoted = false
    var index = 0
    while (index < line.length) {
        val char = line[index]
        when {
            char == '"' && quoted && index + 1 < line.length && line[index + 1] == '"' -> {
                current.append('"')
                index += 1
            }
            char == '"' -> quoted = !quoted
            char == ',' && !quoted -> {
                values += current.toString()
                current.clear()
            }
            else -> current.append(char)
        }
        index += 1
    }
    values += current.toString()
    return values
}

private fun String.cleanCsvHeader(): String = trim().removePrefix("\uFEFF")

private fun findFargoPlayer(players: List<LiveFargoPlayer>, player: LivePlayer): LiveFargoPlayer? {
    return players.firstOrNull { it.id == player.id }
        ?: players.firstOrNull { normalizeName(it.name) == normalizeName(player.name) }
}

private fun findPairStats(pairs: List<LivePairStats>, firstId: String, secondId: String): LivePairStats? {
    val direct = pairs.firstOrNull { it.firstPlayerId == firstId && it.secondPlayerId == secondId }
    if (direct != null) return direct
    return pairs.firstOrNull { it.firstPlayerId == secondId && it.secondPlayerId == firstId }
        ?.let { LivePairStats(firstId, secondId, it.secondWins, it.firstWins, it.games) }
}

private fun fargoGameProbability(rating: Double, opponentRating: Double): Double {
    return 1.0 / (1.0 + 2.0.pow((opponentRating - rating) / 100.0))
}

private fun matchWinProbabilityWithHandicap(rackProbability: Double, raceTo: Int, handicapA: Int, handicapB: Int): Double {
    val targetA = raceTo - handicapA
    val targetB = raceTo - handicapB
    return matchWinProbability(rackProbability, targetA, targetB)
}

private fun matchWinProbabilityFromScore(rackProbability: Double, raceTo: Int, scoreA: Int, scoreB: Int): Double {
    return matchWinProbability(rackProbability, raceTo - scoreA, raceTo - scoreB)
}

private fun matchWinProbability(rackProbability: Double, targetA: Int, targetB: Int): Double {
    if (targetA <= 0) return 1.0
    if (targetB <= 0) return 0.0
    var probability = 0.0
    for (bWins in 0 until targetB) {
        probability += combinations(targetA - 1 + bWins, bWins) *
            rackProbability.pow(targetA) *
            (1.0 - rackProbability).pow(bWins)
    }
    return probability.coerceIn(0.0, 1.0)
}

private fun combinations(n: Int, k: Int): Double {
    if (k < 0 || k > n) return 0.0
    val effectiveK = minOf(k, n - k)
    var result = 1.0
    for (i in 1..effectiveK) {
        result = result * (n - effectiveK + i) / i
    }
    return result
}

private fun isTournamentOver(tournament: LiveTournament): Boolean {
    val stop = parseDateMs(tournament.stoptime)
    val now = js("Date.now()") as Double
    return stop > 0.0 && stop < now
}

private fun matchSortTime(match: LiveMatch): Double {
    return parseDateMs(match.starttime).takeIf { it > 0.0 } ?: parseDateMs(match.stoptime)
}

private fun parseDateMs(value: String): Double {
    if (value.isBlank()) return 0.0
    return js("Date.parse(value)") as Double
}

private fun currentTimeText(): String {
    return js("new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })") as String
}

private fun normalizeImageUrl(value: String): String {
    return when {
        value.startsWith("//") -> "https:$value"
        value.startsWith("/") -> "https://cuescore.com$value"
        else -> value
    }
}

private fun normalizeName(value: String): String {
    return value.trim().lowercase()
        .replace(Regex("\\s+"), " ")
        .replace(".", "")
}

private fun formatPercent(value: Double): String = "${(value * 1000.0).toInt() / 10.0}%"

private fun dynLength(value: dynamic): Int {
    return (js("value && value.length ? value.length : 0") as Number).toInt()
}

private fun dynString(value: dynamic, key: String): String {
    return (js("value && value[key] !== undefined && value[key] !== null ? String(value[key]) : ''") as String)
}

private fun dynInt(value: dynamic, key: String): Int {
    return (js("value && value[key] !== undefined && value[key] !== null ? Number(value[key]) : 0") as Number).toInt()
}

private fun encodeURIComponent(value: String): String {
    return js("encodeURIComponent(value)") as String
}

private fun fetchText(url: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
    js("fetch(url).then(function(response) { if (!response.ok) { throw new Error(response.status + ' ' + response.statusText); } return response.text(); }).then(function(text) { onSuccess(text); }).catch(function(error) { onError(String(error)); });")
}
