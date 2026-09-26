package com.varabyte.kobweb.site.pages

import androidx.compose.runtime.Composable
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
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.site.components.layouts.PageLayoutData
import com.varabyte.kobweb.site.components.widgets.PoolBall
import com.varabyte.kobweb.site.model.LocalSiteLanguage
import com.varabyte.kobweb.site.model.SiteLanguage
import com.varabyte.kobweb.site.model.text
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.fr
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import kotlin.math.abs
import kotlin.math.pow

private data class FargoPlayer(
    val id: String,
    val name: String,
    val image: String,
    val countryImage: String,
    val fargoRating: Double,
    val latestHandicap: Double?,
    val games: Int,
    val wins: Int,
    val losses: Int,
    val disciplineRatings: List<FargoDisciplineRating>,
)

private data class FargoDisciplineRating(
    val component: String,
    val fargoRating: Double?,
    val games: Int,
    val wins: Int,
    val losses: Int,
)

private data class PlayerPairStats(
    val firstPlayerId: String,
    val secondPlayerId: String,
    val component: String,
    val firstWins: Int,
    val secondWins: Int,
    val games: Int,
)

private enum class FargoSortMode {
    Fargo,
    Obk,
}

private data class FargoDiscipline(
    val component: String,
    val badge: String,
    val english: String,
    val norwegian: String,
    val background: Color,
    val border: Color,
    val foreground: Color,
) {
    fun label(language: SiteLanguage): String = language.text(english, norwegian)
}

private data class RaceSuggestion(
    val raceTo: Int,
    val favorite: FargoPlayer,
    val underdog: FargoPlayer,
    val favoriteStart: Int,
    val underdogStart: Int,
    val favoriteWinProbability: Double,
) {
    fun startText(language: SiteLanguage): String {
        return if (favoriteStart == 0 && underdogStart == 0) {
            language.text("Even race", "Jevnt race")
        } else {
            language.text(
                "${underdog.name} starts $underdogStart-$favoriteStart",
                "${underdog.name} starter $underdogStart-$favoriteStart",
            )
        }
    }
}

private const val OVERALL_COMPONENT = "1"
private const val EIGHT_BALL_COMPONENT = "8"
private const val NINE_BALL_COMPONENT = "9"
private const val TEN_BALL_COMPONENT = "10"
private const val OBK_GITHUB_RAW_BASE = "https://raw.githubusercontent.com/Flavelloni/Cue-Score/main/OBK"
private const val ROBUST_FARGO_GAME_THRESHOLD = 200
private const val INITIAL_LIST_MIN_GAMES = 100

@InitRoute
fun initObkFargoPage(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("OBK Fargo Rating", "Oslo Biljardklubb Fargo-style ratings and matchup helper."))
}

@Page("/obk-fargo")
@Composable
@Layout(".components.layouts.PageLayout")
fun ObkFargoPage() {
    val language = LocalSiteLanguage.current
    var players by remember { mutableStateOf<List<FargoPlayer>?>(null) }
    var playerPairs by remember { mutableStateOf<List<PlayerPairStats>>(emptyList()) }
    var historyYears by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var sortMode by remember { mutableStateOf(FargoSortMode.Fargo) }
    var playerFilter by remember { mutableStateOf("") }
    var firstQuery by remember { mutableStateOf("") }
    var secondQuery by remember { mutableStateOf("") }
    var firstPlayer by remember { mutableStateOf<FargoPlayer?>(null) }
    var secondPlayer by remember { mutableStateOf<FargoPlayer?>(null) }

    LaunchedEffect(Unit) {
        fetchFargoText(
            "$OBK_GITHUB_RAW_BASE/player_fargo_ratings.csv",
            onSuccess = {
                players = parseFargoPlayers(it)
                loadError = null
            },
            onError = { loadError = language.text("Could not load Fargo ratings: $it", "Kunne ikke laste Fargo-ratinger: $it") },
        )
        fetchFargoText(
            "$OBK_GITHUB_RAW_BASE/tournament_stats_by_year.csv",
            onSuccess = { historyYears = parseFargoHistoryYearsFromCsv(it) },
            onError = { historyYears = null },
        )
        fetchFargoText(
            "$OBK_GITHUB_RAW_BASE/player_pairs.csv",
            onSuccess = { playerPairs = parsePlayerPairs(it) },
            onError = { playerPairs = emptyList() },
        )
    }

    val loadedPlayers = players.orEmpty()
    val trimmedFilter = playerFilter.trim()
    val filteredPlayers = loadedPlayers
        .filter {
            if (trimmedFilter.isBlank()) {
                it.games >= INITIAL_LIST_MIN_GAMES
            } else {
                it.name.contains(trimmedFilter, ignoreCase = true)
            }
        }
        .sortedWith(
            when (sortMode) {
                FargoSortMode.Fargo -> compareByDescending<FargoPlayer> { it.fargoRating }.thenBy { it.name }
                FargoSortMode.Obk -> compareByDescending<FargoPlayer> { it.latestHandicap ?: Double.NEGATIVE_INFINITY }.thenBy { it.name }
            }
        )

    Column(
        Modifier
            .fillMaxWidth()
            .padding(leftRight = 1.25.cssRem, top = 2.cssRem, bottom = 4.cssRem)
            .gap(1.4.cssRem)
            .styleModifier {
                property("min-height", "calc(100vh - 64px)")
                property("background", "linear-gradient(145deg, #071012 0%, #13201b 48%, #15131d 100%)")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .maxWidth(1180.px)
                .gap(1.cssRem),
        ) {
            H1(
                attrs = Modifier
                    .margin(0.px)
                    .fontSize(2.55.cssRem)
                    .lineHeight(1.05)
                    .fontWeight(FontWeight.Bold)
                    .color(Colors.White)
                    .toAttrs()
            ) {
                Text("OBK Fargo Rating")
            }
            P(
                attrs = Modifier
                    .margin(0.px)
                    .maxWidth(860.px)
                    .fontSize(1.02.cssRem)
                    .lineHeight(1.6)
                    .color(Color.rgba(245, 248, 244, 0.76f))
                    .toAttrs()
            ) {
                Text(language.text("Ratings are calculated solely from Oslo Biljardklubb tournaments.", "Ratingene er beregnet kun fra Oslo Biljardklubb-turneringer."))
            }
        }

        when {
            loadError != null -> FargoMessage(loadError ?: language.text("Could not load Fargo ratings.", "Kunne ikke laste Fargo-ratinger."))
            players == null -> FargoMessage(language.text("Loading ratings...", "Laster ratinger..."))
            else -> {
                MatchupPanel(
                    language = language,
                    players = loadedPlayers,
                    playerPairs = playerPairs,
                    firstQuery = firstQuery,
                    secondQuery = secondQuery,
                    firstPlayer = firstPlayer,
                    secondPlayer = secondPlayer,
                    onFirstQuery = {
                        firstQuery = it
                        firstPlayer = null
                    },
                    onSecondQuery = {
                        secondQuery = it
                        secondPlayer = null
                    },
                    onFirstPlayer = {
                        firstPlayer = it
                        firstQuery = it.name
                    },
                    onSecondPlayer = {
                        secondPlayer = it
                        secondQuery = it.name
                    },
                )

                PlayerListPanel(
                    language = language,
                    players = filteredPlayers,
                    totalPlayers = loadedPlayers.size,
                    sortMode = sortMode,
                    filter = playerFilter,
                    onSortMode = { sortMode = it },
                    onFilter = { playerFilter = it },
                )
            }
        }
    }
}

@Composable
private fun FargoMessage(message: String) {
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
private fun MatchupPanel(
    language: SiteLanguage,
    players: List<FargoPlayer>,
    playerPairs: List<PlayerPairStats>,
    firstQuery: String,
    secondQuery: String,
    firstPlayer: FargoPlayer?,
    secondPlayer: FargoPlayer?,
    onFirstQuery: (String) -> Unit,
    onSecondQuery: (String) -> Unit,
    onFirstPlayer: (FargoPlayer) -> Unit,
    onSecondPlayer: (FargoPlayer) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .maxWidth(1180.px)
            .padding(1.15.cssRem)
            .borderRadius(24.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.08f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.15f))
            .styleModifier { property("box-shadow", "0 26px 70px rgba(0, 0, 0, 0.28)") }
            .gap(1.cssRem)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .gap(1.cssRem)
                .flexWrap(FlexWrap.Wrap),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                Modifier
                    .gap(0.45.cssRem)
                    .styleModifier { property("flex", "1 1 260px") }
            ) {
                H2(
                    attrs = Modifier
                        .margin(0.px)
                        .fontSize(1.45.cssRem)
                        .lineHeight(1.2)
                        .fontWeight(FontWeight.Bold)
                        .color(Colors.White)
                        .toAttrs()
                ) {
                    Text(language.text("Matchup Helper", "Matchup-hjelper"))
                }
                P(
                    attrs = Modifier
                        .margin(0.px)
                        .fontSize(0.95.cssRem)
                        .lineHeight(1.55)
                        .color(Color.rgba(245, 248, 244, 0.72f))
                        .toAttrs()
                ) {
                    Text(language.text("Search two players, compare expected win chances", "Søk opp to spillere og sammenlign forventede vinnersjanser."))
                }
            }
            Row(
                Modifier
                    .gap(0.85.cssRem)
                    .flexWrap(FlexWrap.Wrap)
                    .styleModifier { property("flex", "2 1 560px") },
                verticalAlignment = Alignment.Top,
            ) {
                PlayerPicker(language.text("Player A", "Spiller A"), firstQuery, firstPlayer, players, onFirstQuery, onFirstPlayer, language)
                PlayerPicker(language.text("Player B", "Spiller B"), secondQuery, secondPlayer, players, onSecondQuery, onSecondPlayer, language)
            }
        }

        MatchupResult(firstPlayer, secondPlayer, playerPairs, language)
    }
}

@Composable
private fun PlayerPicker(
    label: String,
    query: String,
    selectedPlayer: FargoPlayer?,
    players: List<FargoPlayer>,
    onQuery: (String) -> Unit,
    onSelect: (FargoPlayer) -> Unit,
    language: SiteLanguage,
) {
    val suggestions = players
        .filter { query.isNotBlank() && it.name.contains(query, ignoreCase = true) }
        .sortedBy { it.name }
        .take(5)

    Column(
        Modifier
            .gap(0.45.cssRem)
            .styleModifier { property("flex", "1 1 250px") }
    ) {
        Span(
            attrs = Modifier
                .fontSize(0.82.cssRem)
                .fontWeight(FontWeight.Bold)
                .color(Color.rgba(245, 248, 244, 0.72f))
                .toAttrs()
        ) {
            Text(label)
        }
        FargoInput(
            value = query,
            placeholder = language.text("Search player", "Søk spiller"),
            onValue = onQuery,
        )
        if (selectedPlayer != null) {
            FargoChip(language.text("Confirmed: ${selectedPlayer.name}", "Bekreftet: ${selectedPlayer.name}"))
        } else if (suggestions.isNotEmpty()) {
            Column(Modifier.gap(0.35.cssRem)) {
                suggestions.forEach { player ->
                    Button(
                        attrs = Modifier
                            .fillMaxWidth()
                            .padding(leftRight = 0.75.cssRem, topBottom = 0.5.cssRem)
                            .borderRadius(12.px)
                            .backgroundColor(Color.rgba(255, 255, 255, 0.08f))
                            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.12f))
                            .color(Colors.White)
                            .styleModifier {
                                property("cursor", "pointer")
                                property("text-align", "left")
                            }
                            .toAttrs { onClick { onSelect(player) } }
                    ) {
                        Text("${player.name} (${formatRating(player.fargoRating)})")
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchupResult(firstPlayer: FargoPlayer?, secondPlayer: FargoPlayer?, playerPairs: List<PlayerPairStats>, language: SiteLanguage) {
    when {
        firstPlayer == null || secondPlayer == null -> {
            P(
                attrs = Modifier
                    .margin(0.px)
                    .padding(top = 0.25.cssRem)
                    .fontSize(0.95.cssRem)
                    .lineHeight(1.55)
                    .color(Color.rgba(245, 248, 244, 0.66f))
                    .toAttrs()
            ) {
                Text(language.text("Confirm both player names to show the matchup.", "Bekreft begge spillernavnene for å vise matchupen."))
            }
        }
        firstPlayer.id == secondPlayer.id -> FargoMessage(language.text("Pick two different players.", "Velg to forskjellige spillere."))
        else -> {
            val firstProbability = fargoGameProbability(firstPlayer.fargoRating, secondPlayer.fargoRating)
            val secondProbability = 1.0 - firstProbability

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(1.cssRem)
                    .borderRadius(18.px)
                    .backgroundColor(Color.rgba(0, 0, 0, 0.18f))
                    .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.12f))
                    .gap(0.9.cssRem)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .gap(0.85.cssRem)
                        .flexWrap(FlexWrap.Wrap),
                    verticalAlignment = Alignment.Top,
                ) {
                    PlayerComparisonCard(firstPlayer, firstProbability)
                    PlayerComparisonCard(secondPlayer, secondProbability)
                }
                RaceSuggestionsCard(firstPlayer, secondPlayer, firstProbability, language)
                HeadToHeadCard(firstPlayer, secondPlayer, playerPairs, language)
            }
        }
    }
}

@Composable
private fun PlayerComparisonCard(player: FargoPlayer, probability: Double) {
    var expanded by remember(player.id) { mutableStateOf(false) }

    Column(
        Modifier
            .padding(0.85.cssRem)
            .borderRadius(16.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.07f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.1f))
            .gap(0.35.cssRem)
            .styleModifier { property("flex", "1 1 260px") }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .gap(0.65.cssRem),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Span(
                attrs = Modifier
                    .fontSize(1.05.cssRem)
                    .fontWeight(FontWeight.Bold)
                    .color(Colors.White)
                    .styleModifier {
                        property("min-width", "0")
                        property("flex", "1 1 auto")
                        property("overflow-wrap", "anywhere")
                    }
                    .toAttrs()
            ) {
                Text(player.name)
            }
            ExpandButton(
                expanded = expanded,
                hasDetails = player.disciplineRatings.isNotEmpty(),
                onToggle = { expanded = !expanded },
                language = LocalSiteLanguage.current,
            )
        }
        FargoStatLine("Fargo", formatRating(player.fargoRating), valueColor = fargoRobustnessColor(player.games))
        FargoStatLine(LocalSiteLanguage.current.text("Simple OBK", "Enkel OBK"), formatOptionalRating(player.latestHandicap))
        FargoStatLine(LocalSiteLanguage.current.text("OBK record", "OBK-statistikk"), "${player.wins}-${player.losses} (${player.games} ${LocalSiteLanguage.current.text("games", "partier")})")
        FargoStatLine(LocalSiteLanguage.current.text("Expected rack win", "Forventet partisjanse"), formatPercent(probability))
        if (expanded) {
            DisciplineRatingsPanel(player, LocalSiteLanguage.current)
        }
    }
}

@Composable
private fun RaceSuggestionsCard(firstPlayer: FargoPlayer, secondPlayer: FargoPlayer, firstProbability: Double, language: SiteLanguage) {
    val suggestions = listOf(3, 4, 5, 6).map { raceSuggestion(it, firstPlayer, secondPlayer, firstProbability) }

    Column(
        Modifier
            .padding(0.85.cssRem)
            .borderRadius(16.px)
            .backgroundColor(Color.rgba(239, 190, 83, 0.12f))
            .border(1.px, LineStyle.Solid, Color.rgba(239, 190, 83, 0.3f))
            .gap(0.35.cssRem)
            .styleModifier {
                property("width", "100%")
                property("box-sizing", "border-box")
            }
    ) {
        Span(
            attrs = Modifier
                .fontSize(0.85.cssRem)
                .fontWeight(FontWeight.Bold)
                .color(Color.rgb(239, 210, 133))
                .toAttrs()
        ) {
            Text(language.text("Race suggestions", "Race-forslag"))
        }
        Div(
            attrs = Modifier
                .fillMaxWidth()
                .styleModifier {
                    property("display", "grid")
                    property("grid-template-columns", "repeat(auto-fit, minmax(160px, 1fr))")
                    property("gap", "0.5rem")
                }
                .toAttrs()
        ) {
            suggestions.forEach { suggestion ->
                RaceSuggestionTile(suggestion, language)
            }
        }
        P(
            attrs = Modifier
                .margin(0.px)
                .fontSize(0.82.cssRem)
                .lineHeight(1.45)
                .color(Color.rgba(245, 248, 244, 0.58f))
                .toAttrs()
        ) {
            Text(language.text("Calculated from the overall Fargo rating difference only, testing all valid starting scores below the race target.", "Beregnet kun fra samlet Fargo-ratingforskjell, med alle gyldige startstillinger under race-målet testet."))
        }
    }
}

@Composable
private fun RaceSuggestionTile(suggestion: RaceSuggestion, language: SiteLanguage) {
    Column(
        Modifier
            .padding(leftRight = 0.68.cssRem, topBottom = 0.58.cssRem)
            .borderRadius(12.px)
            .backgroundColor(Color.rgba(0, 0, 0, 0.14f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.08f))
            .gap(0.25.cssRem)
    ) {
        Span(
            attrs = Modifier
                .fontSize(0.74.cssRem)
                .fontWeight(FontWeight.Bold)
                .color(Color.rgba(245, 248, 244, 0.58f))
                .toAttrs()
        ) {
            Text(language.text("Race to ${suggestion.raceTo}", "Race til ${suggestion.raceTo}"))
        }
        Span(
            attrs = Modifier
                .fontSize(0.98.cssRem)
                .fontWeight(FontWeight.Bold)
                .lineHeight(1.25)
                .color(Colors.White)
                .toAttrs()
        ) {
            Text(suggestion.startText(language))
        }
        Span(
            attrs = Modifier
                .fontSize(0.78.cssRem)
                .lineHeight(1.25)
                .color(Color.rgba(245, 248, 244, 0.64f))
                .toAttrs()
        ) {
            Text(language.text("${suggestion.favorite.name}: ${formatPercent(suggestion.favoriteWinProbability)}", "${suggestion.favorite.name}: ${formatPercent(suggestion.favoriteWinProbability)}"))
        }
    }
}

@Composable
private fun HeadToHeadCard(firstPlayer: FargoPlayer, secondPlayer: FargoPlayer, playerPairs: List<PlayerPairStats>, language: SiteLanguage) {
    val stats = matchupPairStats(firstPlayer, secondPlayer, playerPairs)

    Column(
        Modifier
            .padding(0.85.cssRem)
            .borderRadius(16.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.07f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.1f))
            .gap(0.55.cssRem)
            .styleModifier {
                property("width", "100%")
                property("box-sizing", "border-box")
            }
    ) {
        Span(
            attrs = Modifier
                .fontSize(0.85.cssRem)
                .fontWeight(FontWeight.Bold)
                .color(Color.rgba(245, 248, 244, 0.78f))
                .toAttrs()
        ) {
            Text(language.text("Head-to-head", "Innbyrdes oppgjør"))
        }
        Div(
            attrs = Modifier
                .fillMaxWidth()
                .styleModifier {
                    property("display", "grid")
                    property("grid-template-columns", "repeat(auto-fit, minmax(170px, 1fr))")
                    property("gap", "0.5rem")
                }
                .toAttrs()
        ) {
            matchupDisciplines().forEach { discipline ->
                HeadToHeadTile(
                    discipline = discipline,
                    stats = stats[discipline.component],
                    firstPlayer = firstPlayer,
                    secondPlayer = secondPlayer,
                    language = language,
                )
            }
        }
    }
}

@Composable
private fun HeadToHeadTile(discipline: FargoDiscipline, stats: PlayerPairStats?, firstPlayer: FargoPlayer, secondPlayer: FargoPlayer, language: SiteLanguage) {
    Column(
        Modifier
            .padding(leftRight = 0.68.cssRem, topBottom = 0.58.cssRem)
            .borderRadius(12.px)
            .backgroundColor(Color.rgba(0, 0, 0, 0.16f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.08f))
            .gap(0.28.cssRem)
    ) {
        Row(Modifier.gap(0.45.cssRem), verticalAlignment = Alignment.CenterVertically) {
            DisciplineBadge(discipline)
        }
        Span(
            attrs = Modifier
                .fontSize(1.02.cssRem)
                .fontWeight(FontWeight.Bold)
                .lineHeight(1.2)
                .color(Colors.White)
                .toAttrs()
        ) {
            Text(stats?.let { "${it.firstWins}-${it.secondWins}" } ?: "-")
        }
    }
}

@Composable
private fun PlayerListPanel(
    language: SiteLanguage,
    players: List<FargoPlayer>,
    totalPlayers: Int,
    sortMode: FargoSortMode,
    filter: String,
    onSortMode: (FargoSortMode) -> Unit,
    onFilter: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .maxWidth(1180.px)
            .padding(1.15.cssRem)
            .borderRadius(24.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.07f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.13f))
            .gap(1.cssRem)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .gap(0.8.cssRem)
                .flexWrap(FlexWrap.Wrap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier
                    .gap(0.25.cssRem)
                    .styleModifier { property("flex", "1 1 280px") }
            ) {
                H2(
                    attrs = Modifier
                        .margin(0.px)
                        .fontSize(1.35.cssRem)
                        .fontWeight(FontWeight.Bold)
                        .color(Colors.White)
                        .toAttrs()
                ) {
                    Text(language.text("Player Ratings", "Spillerratinger"))
                }
                Span(
                    attrs = Modifier
                        .fontSize(0.9.cssRem)
                        .color(Color.rgba(245, 248, 244, 0.64f))
                        .toAttrs()
                ) {
                    Text(
                        if (filter.isBlank()) {
                            language.text(
                                "${players.size} players with 100+ games",
                                "${players.size} spillere med 100+ partier",
                            )
                        } else {
                            language.text("${players.size} of $totalPlayers players", "${players.size} av $totalPlayers spillere")
                        }
                    )
                }
            }
            Row(
                Modifier
                    .gap(0.5.cssRem)
                    .flexWrap(FlexWrap.Wrap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Span(
                    attrs = Modifier
                        .fontSize(0.82.cssRem)
                        .fontWeight(FontWeight.Bold)
                        .color(Color.rgba(245, 248, 244, 0.68f))
                        .toAttrs()
                ) {
                    Text(language.text("Sort by", "Sorter etter"))
                }
                SortButton(language.text("Fargo rating", "Fargo-rating"), sortMode == FargoSortMode.Fargo) { onSortMode(FargoSortMode.Fargo) }
                SortButton(language.text("Simple OBK rating", "Enkel OBK-rating"), sortMode == FargoSortMode.Obk) { onSortMode(FargoSortMode.Obk) }
                Div(attrs = Modifier.width(230.px).toAttrs()) {
                    FargoInput(filter, language.text("Filter player", "Filtrer spiller"), onFilter)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .gap(0.55.cssRem)
                .flexWrap(FlexWrap.Wrap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RobustnessLegendChip(
                color = robustFargoColor,
                text = language.text("Green Fargo: 200+ recorded games", "Grønn Fargo: 200+ registrerte partier"),
            )
            RobustnessLegendChip(
                color = provisionalFargoColor,
                text = language.text("Red Fargo: under 200 games, less robust", "Rød Fargo: under 200 partier, mindre robust"),
            )
            RobustnessLegendChip(
                color = Color.rgba(245, 248, 244, 0.56f),
                text = language.text("Initial list hides players below 100 games; search still finds them", "Startlisten skjuler spillere under 100 partier; søk finner dem fortsatt"),
            )
        }

        Column(Modifier.fillMaxWidth().gap(0.65.cssRem)) {
            Div(
                attrs = Modifier
                    .fillMaxWidth()
                    .styleModifier {
                        property("display", "grid")
                        property("gap", "0.45rem")
                    }
                    .toAttrs()
            ) {
                players.take(120).forEachIndexed { index, player ->
                    PlayerRow(index + 1, player, language)
                }
            }
            if (players.size > 120) {
                FargoMessage(language.text("Showing first 120 matching players. Use the filter to narrow the list.", "Viser de første 120 treffene. Bruk filteret for å snevre inn listen."))
            }
        }
    }
}

@Composable
private fun PlayerListHeader() {
    Div(
        attrs = Modifier
            .fillMaxWidth()
            .padding(leftRight = 0.8.cssRem, topBottom = 0.35.cssRem)
            .color(Color.rgba(245, 248, 244, 0.58f))
            .fontSize(0.78.cssRem)
            .fontWeight(FontWeight.Bold)
            .styleModifier {
                property("display", "grid")
                property("grid-template-columns", "56px minmax(220px, 1fr) 110px 130px 100px")
                property("column-gap", "1rem")
                property("align-items", "center")
                property("min-width", "0")
            }
            .toAttrs {
                classes("obk-player-header")
            }
    ) {
        PlayerTableHeaderCell("#")
        PlayerTableHeaderCell(LocalSiteLanguage.current.text("Player", "Spiller"))
        PlayerTableHeaderCell("Fargo")
        PlayerTableHeaderCell(LocalSiteLanguage.current.text("Simple OBK", "Enkel OBK"))
        PlayerTableHeaderCell(LocalSiteLanguage.current.text("Record", "Statistikk"))
    }
}

@Composable
private fun PlayerRow(index: Int, player: FargoPlayer, language: SiteLanguage) {
    var expanded by remember(player.id) { mutableStateOf(false) }

    Div(
        attrs = Modifier
            .fillMaxWidth()
            .padding(0.95.cssRem)
            .borderRadius(14.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.055f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.08f))
            .color(Colors.White)
            .fontSize(0.92.cssRem)
            .styleModifier {
                property("box-sizing", "border-box")
                property("min-width", "0")
            }
            .toAttrs()
    ) {
        Column(Modifier.fillMaxWidth().gap(0.72.cssRem)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .gap(0.8.cssRem),
                verticalAlignment = Alignment.Top,
            ) {
                Span(
                    attrs = Modifier
                        .fontSize(0.82.cssRem)
                        .fontWeight(FontWeight.Bold)
                        .color(Color.rgba(245, 248, 244, 0.52f))
                        .styleModifier { property("flex", "0 0 2.2rem") }
                        .toAttrs()
                ) {
                    Text("#$index")
                }
                PlayerIdentity(player)
                ExpandButton(
                    expanded = expanded,
                    hasDetails = player.disciplineRatings.isNotEmpty(),
                    onToggle = { expanded = !expanded },
                    language = language,
                )
            }
            Div(
                attrs = Modifier
                    .fillMaxWidth()
                    .styleModifier {
                        property("display", "grid")
                        property("grid-template-columns", "repeat(3, minmax(0, 1fr))")
                        property("gap", "0.55rem")
                    }
                    .toAttrs()
            ) {
                PlayerMobileStat("Fargo", formatRating(player.fargoRating), valueColor = fargoRobustnessColor(player.games))
                PlayerMobileStat(language.text("Simple OBK", "Enkel OBK"), formatOptionalRating(player.latestHandicap))
                PlayerMobileStat(language.text("Record", "Statistikk"), "${player.wins}-${player.losses}")
            }
            if (expanded) {
                DisciplineRatingsPanel(player, language)
            }
        }
    }
}

@Composable
private fun ExpandButton(expanded: Boolean, hasDetails: Boolean, onToggle: () -> Unit, language: SiteLanguage) {
    Button(
        attrs = Modifier
            .width(2.1.cssRem)
            .padding(0.px)
            .borderRadius(999.px)
            .backgroundColor(if (hasDetails) Color.rgba(239, 190, 83, 0.16f) else Color.rgba(255, 255, 255, 0.08f))
            .border(1.px, LineStyle.Solid, if (hasDetails) Color.rgba(239, 190, 83, 0.38f) else Color.rgba(255, 255, 255, 0.14f))
            .color(if (hasDetails) Color.rgb(247, 219, 143) else Color.rgba(245, 248, 244, 0.66f))
            .fontSize(1.cssRem)
            .fontWeight(FontWeight.Bold)
            .styleModifier {
                property("height", "2.1rem")
                property("cursor", "pointer")
                property("line-height", "1")
                property("flex", "0 0 auto")
            }
            .toAttrs {
                attr("aria-expanded", expanded.toString())
                attr("aria-label", language.text("Show discipline ratings", "Vis disiplinratinger"))
                attr("title", language.text("Show discipline ratings", "Vis disiplinratinger"))
                onClick { onToggle() }
            }
    ) {
        Text(if (expanded) "⌄" else "›")
    }
}

@Composable
private fun DisciplineRatingsPanel(player: FargoPlayer, language: SiteLanguage) {
    val ratingsByComponent = player.disciplineRatings.associateBy { it.component }

    Div(
        attrs = Modifier
            .fillMaxWidth()
            .padding(top = 0.4.cssRem)
            .styleModifier {
                property("display", "grid")
                property("grid-template-columns", "repeat(auto-fit, minmax(150px, 1fr))")
                property("gap", "0.5rem")
            }
            .toAttrs()
    ) {
        matchupDisciplines().forEach { discipline ->
            val rating = if (discipline.component == OVERALL_COMPONENT) {
                FargoDisciplineRating(
                    component = OVERALL_COMPONENT,
                    fargoRating = player.fargoRating,
                    games = player.games,
                    wins = player.wins,
                    losses = player.losses,
                )
            } else {
                ratingsByComponent[discipline.component]
            }

            Column(
                Modifier
                    .padding(leftRight = 0.68.cssRem, topBottom = 0.58.cssRem)
                    .borderRadius(12.px)
                    .backgroundColor(Color.rgba(0, 0, 0, 0.18f))
                    .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.08f))
                    .gap(0.28.cssRem)
            ) {
                Row(Modifier.gap(0.45.cssRem), verticalAlignment = Alignment.CenterVertically) {
                    DisciplineBadge(discipline)
                }
                Span(
                    attrs = Modifier
                        .fontSize(1.cssRem)
                        .fontWeight(FontWeight.Bold)
                        .lineHeight(1.2)
                        .color(rating?.fargoRating?.let { fargoRobustnessColor(rating.games) } ?: Color.rgba(245, 248, 244, 0.52f))
                        .toAttrs()
                ) {
                    Text(formatOptionalRating(rating?.fargoRating))
                }
                Span(
                    attrs = Modifier
                        .fontSize(0.78.cssRem)
                        .lineHeight(1.25)
                        .color(Color.rgba(245, 248, 244, 0.66f))
                        .toAttrs()
                ) {
                    Text(rating?.let { "${it.wins}-${it.losses} · ${it.games} ${language.text("games", "partier")}" } ?: language.text("No games", "Ingen partier"))
                }
            }
        }
    }
}

@Composable
private fun DisciplineBadge(discipline: FargoDiscipline) {
    val ballNumber = discipline.component.toIntOrNull()
    if (ballNumber != null && ballNumber in 8..10) {
        PoolBall(
            ballNumber = ballNumber,
            modifier = Modifier
                .width(1.85.cssRem)
                .height(1.85.cssRem),
        )
        return
    }

    Span(
        attrs = Modifier
            .width(1.85.cssRem)
            .borderRadius(999.px)
            .backgroundColor(discipline.background)
            .border(1.px, LineStyle.Solid, discipline.border)
            .color(discipline.foreground)
            .fontSize(0.72.cssRem)
            .fontWeight(FontWeight.Bold)
            .lineHeight(1.0)
            .styleModifier {
                property("height", "1.85rem")
                property("display", "inline-flex")
                property("align-items", "center")
                property("justify-content", "center")
                property("flex", "0 0 auto")
            }
            .toAttrs()
    ) {
        Text(discipline.badge)
    }
}

@Composable
private fun PlayerIdentity(player: FargoPlayer) {
    Row(
        Modifier
            .gap(0.55.cssRem)
            .styleModifier {
                property("min-width", "0")
                property("flex", "1 1 auto")
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerImage(
            src = player.image,
            alt = player.name,
            width = "2.35rem",
            height = "2.35rem",
            radius = "50%",
        )
        Span(
            attrs = Modifier
                .fontSize(1.04.cssRem)
                .fontWeight(FontWeight.Bold)
                .lineHeight(1.25)
                .color(Color.rgba(245, 248, 244, 0.94f))
                .styleModifier {
                    property("min-width", "0")
                    property("overflow-wrap", "anywhere")
                }
                .toAttrs()
        ) {
            Text(player.name)
        }
        PlayerImage(
            src = player.countryImage,
            alt = "${player.name} country",
            width = "1.65rem",
            height = "1.1rem",
            radius = "3px",
        )
    }
}

@Composable
private fun PlayerImage(src: String, alt: String, width: String, height: String, radius: String) {
    if (src.isBlank()) return

    Img(
        src = src,
        attrs = Modifier
            .styleModifier {
                property("width", width)
                property("height", height)
                property("border-radius", radius)
                property("object-fit", "cover")
                property("flex", "0 0 auto")
                property("background", "rgba(255, 255, 255, 0.1)")
            }
            .toAttrs {
                attr("alt", alt)
                attr("loading", "lazy")
                attr("referrerpolicy", "no-referrer")
            }
    )
}

@Composable
private fun PlayerDesktopCell(text: String, strong: Boolean = false, muted: Boolean = false) {
    Span(
        attrs = Modifier
            .lineHeight(1.25)
            .fontWeight(if (strong) FontWeight.SemiBold else FontWeight.Normal)
            .color(if (muted) Color.rgba(245, 248, 244, 0.56f) else Color.rgba(245, 248, 244, 0.9f))
            .styleModifier {
                property("white-space", "nowrap")
                property("overflow", "hidden")
                property("text-overflow", "ellipsis")
                property("min-width", "0")
            }
            .toAttrs {
                classes("obk-player-desktop-cell")
                attr("title", text)
            }
    ) {
        Text(text)
    }
}

@Composable
private fun PlayerMobileStat(label: String, value: String, valueColor: Color = Color.rgba(245, 248, 244, 0.92f)) {
    Column(
        Modifier
            .padding(leftRight = 0.58.cssRem, topBottom = 0.5.cssRem)
            .borderRadius(12.px)
            .backgroundColor(Color.rgba(0, 0, 0, 0.16f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.08f))
            .gap(0.18.cssRem)
            .styleModifier { property("min-width", "0") }
    ) {
        Span(
            attrs = Modifier
                .fontSize(0.72.cssRem)
                .fontWeight(FontWeight.Bold)
                .lineHeight(1.15)
                .color(Color.rgba(245, 248, 244, 0.52f))
                .styleModifier {
                    property("white-space", "nowrap")
                    property("overflow", "hidden")
                    property("text-overflow", "ellipsis")
                }
                .toAttrs()
        ) {
            Text(label)
        }
        Span(
            attrs = Modifier
                .fontSize(0.96.cssRem)
                .fontWeight(FontWeight.Bold)
                .lineHeight(1.2)
                .color(valueColor)
                .toAttrs()
        ) {
            Text(value)
        }
    }
}

@Composable
private fun PlayerTableHeaderCell(text: String) {
    Span(
        attrs = Modifier
            .lineHeight(1.25)
            .styleModifier {
                property("white-space", "nowrap")
                property("overflow", "hidden")
                property("text-overflow", "ellipsis")
            }
            .toAttrs()
    ) {
        Text(text)
    }
}

@Composable
private fun PlayerTableCell(text: String, strong: Boolean = false, muted: Boolean = false) {
    Span(
        attrs = Modifier
            .lineHeight(1.25)
            .fontWeight(if (strong) FontWeight.SemiBold else FontWeight.Normal)
            .color(if (muted) Color.rgba(245, 248, 244, 0.56f) else Color.rgba(245, 248, 244, 0.9f))
            .styleModifier {
                property("white-space", "nowrap")
                property("overflow", "hidden")
                property("text-overflow", "ellipsis")
                property("min-width", "0")
            }
            .toAttrs {
                attr("title", text)
            }
    ) {
        Text(text)
    }
}

@Composable
private fun SortButton(label: String, active: Boolean, onClick: () -> Unit) {
    Button(
        attrs = Modifier
            .padding(leftRight = 0.85.cssRem, topBottom = 0.56.cssRem)
            .borderRadius(999.px)
            .backgroundColor(if (active) Color.rgba(239, 190, 83, 0.18f) else Color.rgba(255, 255, 255, 0.08f))
            .border(1.px, LineStyle.Solid, if (active) Color.rgba(239, 190, 83, 0.48f) else Color.rgba(255, 255, 255, 0.13f))
            .color(if (active) Color.rgb(247, 219, 143) else Color.rgba(245, 248, 244, 0.78f))
            .fontWeight(FontWeight.Bold)
            .styleModifier { property("cursor", "pointer") }
            .toAttrs { onClick { onClick() } }
    ) {
        Text(label)
    }
}

@Composable
private fun FargoInput(value: String, placeholder: String, onValue: (String) -> Unit) {
    Input(
        type = InputType.Text,
        attrs = Modifier
            .fillMaxWidth()
            .padding(leftRight = 0.8.cssRem, topBottom = 0.65.cssRem)
            .borderRadius(14.px)
            .backgroundColor(Color.rgba(0, 0, 0, 0.18f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.14f))
            .color(Colors.White)
            .fontSize(0.95.cssRem)
            .styleModifier {
                property("box-sizing", "border-box")
                property("outline", "none")
            }
            .toAttrs {
                attr("value", value)
                attr("placeholder", placeholder)
                onInput { onValue(it.value) }
            }
    )
}

@Composable
private fun FargoChip(text: String) {
    Span(
        attrs = Modifier
            .padding(leftRight = 0.72.cssRem, topBottom = 0.4.cssRem)
            .borderRadius(999.px)
            .backgroundColor(Color.rgba(76, 211, 140, 0.13f))
            .border(1.px, LineStyle.Solid, Color.rgba(76, 211, 140, 0.38f))
            .color(Color.rgb(147, 235, 183))
            .fontSize(0.86.cssRem)
            .fontWeight(FontWeight.SemiBold)
            .toAttrs()
    ) {
        Text(text)
    }
}

@Composable
private fun RobustnessLegendChip(color: Color, text: String) {
    Row(
        Modifier
            .padding(leftRight = 0.62.cssRem, topBottom = 0.42.cssRem)
            .borderRadius(999.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.055f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.1f))
            .gap(0.4.cssRem),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Span(
            attrs = Modifier
                .fontSize(1.05.cssRem)
                .lineHeight(1.0)
                .color(color)
                .toAttrs()
        ) {
            Text("●")
        }
        Span(
            attrs = Modifier
                .fontSize(0.82.cssRem)
                .lineHeight(1.25)
                .color(Color.rgba(245, 248, 244, 0.68f))
                .toAttrs()
        ) {
            Text(text)
        }
    }
}

@Composable
private fun FargoStatLine(label: String, value: String, valueColor: Color = Color.rgba(245, 248, 244, 0.9f)) {
    Row(
        Modifier.fillMaxWidth().gap(0.65.cssRem),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Span(
            attrs = Modifier
                .fontSize(0.86.cssRem)
                .color(Color.rgba(245, 248, 244, 0.58f))
                .styleModifier { property("flex", "0 0 118px") }
                .toAttrs()
        ) {
            Text(label)
        }
        Span(
            attrs = Modifier
                .fontSize(0.92.cssRem)
                .fontWeight(FontWeight.SemiBold)
                .color(valueColor)
                .toAttrs()
        ) {
            Text(value)
        }
    }
}

private fun parseFargoPlayers(csv: String): List<FargoPlayer> {
    val lines = csv.lineSequence().filter { it.isNotBlank() }.toList()
    if (lines.isEmpty()) return emptyList()

    val header = parseCsvLine(lines.first())
    val columnIndex = header.withIndex().associate { it.value.cleanCsvHeader() to it.index }

    fun List<String>.value(column: String): String {
        val index = columnIndex[column] ?: return ""
        return getOrNull(index).orEmpty()
    }

    fun List<String>.disciplineRating(component: String, suffix: String): FargoDisciplineRating? {
        val wins = value("wins_$suffix").toIntOrNull() ?: 0
        val losses = value("losses_$suffix").toIntOrNull() ?: 0
        val rating = value("fargo_rating_$suffix").toDoubleOrNull()

        return if (rating == null && wins + losses == 0) {
            null
        } else {
            FargoDisciplineRating(
                component = component,
                fargoRating = rating,
                games = wins + losses,
                wins = wins,
                losses = losses,
            )
        }
    }

    return lines.asSequence()
        .drop(1)
        .mapNotNull { line ->
            val columns = parseCsvLine(line)
            val fargoRating = columns.value("fargo_rating").toDoubleOrNull() ?: return@mapNotNull null
            val wins = columns.value("wins").toIntOrNull() ?: 0
            val losses = columns.value("losses").toIntOrNull() ?: 0

            FargoPlayer(
                id = columns.value("player_id").trim(),
                name = columns.value("player_name").trim(),
                image = columns.value("image").trim(),
                countryImage = columns.value("countryImage").trim(),
                fargoRating = fargoRating,
                latestHandicap = columns.value("latest_handicap").toDoubleOrNull(),
                games = columns.value("games").toIntOrNull() ?: wins + losses,
                wins = wins,
                losses = losses,
                disciplineRatings = listOfNotNull(
                    columns.disciplineRating(EIGHT_BALL_COMPONENT, "8_ball"),
                    columns.disciplineRating(NINE_BALL_COMPONENT, "9_ball"),
                    columns.disciplineRating(TEN_BALL_COMPONENT, "10_ball"),
                ),
            )
        }
        .toList()
}

private fun parsePlayerPairs(csv: String): List<PlayerPairStats> {
    val lines = csv.lineSequence().filter { it.isNotBlank() }.toList()
    if (lines.isEmpty()) return emptyList()

    val header = parseCsvLine(lines.first())
    val columnIndex = header.withIndex().associate { it.value.cleanCsvHeader() to it.index }

    fun List<String>.value(column: String): String {
        val index = columnIndex[column] ?: return ""
        return getOrNull(index).orEmpty()
    }

    fun List<String>.valueAny(vararg columns: String): String {
        return columns.firstNotNullOfOrNull { column -> value(column).takeIf { it.isNotBlank() } }.orEmpty()
    }

    fun List<String>.pairStats(
        firstPlayerId: String,
        secondPlayerId: String,
        component: String,
        winsSuffix: String,
    ): PlayerPairStats? {
        val firstWins = value("player1_wins$winsSuffix").toIntOrNull()
        val secondWins = value("player2_wins$winsSuffix").toIntOrNull()
        val games = value("games$winsSuffix").toIntOrNull()

        return if (firstWins == null || secondWins == null || games == null || games == 0) {
            null
        } else {
            PlayerPairStats(
                firstPlayerId = firstPlayerId,
                secondPlayerId = secondPlayerId,
                component = component,
                firstWins = firstWins,
                secondWins = secondWins,
                games = games,
            )
        }
    }

    return lines.asSequence()
        .drop(1)
        .flatMap { line ->
            val columns = parseCsvLine(line)
            val firstPlayerId = columns.valueAny("first_player_id", "player1_id", "player_1_id", "player_a_id", "player_id_1", "player_id_a", "p1_id", "player_id").trim()
            val secondPlayerId = columns.valueAny("second_player_id", "player2_id", "player_2_id", "player_b_id", "player_id_2", "player_id_b", "p2_id", "opponent_id").trim()

            if (firstPlayerId.isBlank() || secondPlayerId.isBlank()) {
                return@flatMap emptyList<PlayerPairStats>().asSequence()
            }

            listOfNotNull(
                columns.pairStats(firstPlayerId, secondPlayerId, OVERALL_COMPONENT, ""),
                columns.pairStats(firstPlayerId, secondPlayerId, EIGHT_BALL_COMPONENT, "_8_ball"),
                columns.pairStats(firstPlayerId, secondPlayerId, NINE_BALL_COMPONENT, "_9_ball"),
                columns.pairStats(firstPlayerId, secondPlayerId, TEN_BALL_COMPONENT, "_10_ball"),
            ).asSequence()
        }
        .toList()
}

private fun normalizeComponent(value: String): String {
    val normalized = value.trim().lowercase()
    return when (normalized) {
        "", "1", "all", "overall", "total", "totalt" -> OVERALL_COMPONENT
        "8", "8ball", "8-ball", "eight", "eight-ball" -> "8"
        "9", "9ball", "9-ball", "nine", "nine-ball" -> "9"
        "10", "10ball", "10-ball", "ten", "ten-ball" -> "10"
        else -> value.trim()
    }
}

private fun matchupPairStats(firstPlayer: FargoPlayer, secondPlayer: FargoPlayer, playerPairs: List<PlayerPairStats>): Map<String, PlayerPairStats> {
    return playerPairs
        .mapNotNull { pair ->
            val component = normalizeComponent(pair.component)
            when {
                pair.firstPlayerId == firstPlayer.id && pair.secondPlayerId == secondPlayer.id -> pair.copy(component = component)
                pair.firstPlayerId == secondPlayer.id && pair.secondPlayerId == firstPlayer.id -> PlayerPairStats(
                    firstPlayerId = firstPlayer.id,
                    secondPlayerId = secondPlayer.id,
                    component = component,
                    firstWins = pair.secondWins,
                    secondWins = pair.firstWins,
                    games = pair.games,
                )
                else -> null
            }
        }
        .groupBy { it.component }
        .mapValues { (_, stats) ->
            stats.reduce { acc, stat ->
                acc.copy(
                    firstWins = acc.firstWins + stat.firstWins,
                    secondWins = acc.secondWins + stat.secondWins,
                    games = acc.games + stat.games,
                )
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

private fun parseFargoHistoryYearsFromCsv(csv: String): String {
    val years = csv.lineSequence()
        .drop(1)
        .mapNotNull { line -> parseCsvLine(line).firstOrNull()?.trim()?.toIntOrNull() }
        .toList()

    val firstYear = years.minOrNull() ?: return "?"
    val lastYear = years.maxOrNull() ?: return firstYear.toString()
    return if (firstYear == lastYear) firstYear.toString() else "$firstYear-$lastYear"
}

private val robustFargoColor = Color.rgb(126, 218, 116)

private val provisionalFargoColor = Color.rgb(255, 114, 114)

private fun matchupDisciplines(): List<FargoDiscipline> = listOf(
    FargoDiscipline(
        component = OVERALL_COMPONENT,
        badge = "All",
        english = "Overall",
        norwegian = "Totalt",
        background = Color.rgba(245, 248, 244, 0.12f),
        border = Color.rgba(245, 248, 244, 0.26f),
        foreground = Color.rgba(245, 248, 244, 0.88f),
    ),
    FargoDiscipline(
        component = "8",
        badge = "8",
        english = "8-ball",
        norwegian = "8-ball",
        background = Color.rgba(36, 36, 36, 0.92f),
        border = Color.rgba(255, 255, 255, 0.24f),
        foreground = Colors.White,
    ),
    FargoDiscipline(
        component = "9",
        badge = "9",
        english = "9-ball",
        norwegian = "9-ball",
        background = Color.rgba(239, 190, 83, 0.24f),
        border = Color.rgba(239, 190, 83, 0.48f),
        foreground = Color.rgb(247, 219, 143),
    ),
    FargoDiscipline(
        component = "10",
        badge = "10",
        english = "10-ball",
        norwegian = "10-ball",
        background = Color.rgba(78, 167, 255, 0.2f),
        border = Color.rgba(78, 167, 255, 0.44f),
        foreground = Color.rgb(164, 210, 255),
    ),
)

private fun fargoRobustnessColor(games: Int): Color {
    return if (games >= ROBUST_FARGO_GAME_THRESHOLD) robustFargoColor else provisionalFargoColor
}

private fun fargoGameProbability(rating: Double, opponentRating: Double): Double {
    return 1.0 / (1.0 + 2.0.pow((opponentRating - rating) / 100.0))
}

private fun raceSuggestion(raceTo: Int, firstPlayer: FargoPlayer, secondPlayer: FargoPlayer, firstProbability: Double): RaceSuggestion {
    val favorite = if (firstProbability >= 0.5) firstPlayer else secondPlayer
    val underdog = if (favorite == firstPlayer) secondPlayer else firstPlayer
    val favoriteRackProbability = if (favorite == firstPlayer) firstProbability else 1.0 - firstProbability

    val starts = buildList {
        add(0 to 0)
        for (favoriteStart in 0 until raceTo) {
            for (underdogStart in (favoriteStart + 1) until raceTo) {
                add(favoriteStart to underdogStart)
            }
        }
    }
    val bestStart = starts.minByOrNull { (favoriteStart, underdogStart) ->
        val favoriteTarget = raceTo - favoriteStart
        val underdogTarget = raceTo - underdogStart
        abs(matchWinProbability(favoriteRackProbability, favoriteTarget, underdogTarget) - 0.5)
    } ?: (0 to 0)

    val favoriteStart = bestStart.first
    val underdogStart = bestStart.second
    val favoriteMatchProbability = matchWinProbability(
        favoriteRackProbability,
        raceTo - favoriteStart,
        raceTo - underdogStart,
    )
    return RaceSuggestion(
        raceTo = raceTo,
        favorite = favorite,
        underdog = underdog,
        favoriteStart = favoriteStart,
        underdogStart = underdogStart,
        favoriteWinProbability = favoriteMatchProbability,
    )
}

private fun matchWinProbability(rackProbability: Double, favoriteTarget: Int, underdogTarget: Int): Double {
    var probability = 0.0
    for (underdogWins in 0 until underdogTarget) {
        probability += combinations(favoriteTarget - 1 + underdogWins, underdogWins) *
            rackProbability.pow(favoriteTarget) *
            (1.0 - rackProbability).pow(underdogWins)
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

private fun formatRating(value: Double): String = value.toInt().toString()

private fun formatOptionalRating(value: Double?): String = value?.toInt()?.toString() ?: "-"

private fun formatPercent(value: Double): String = "${(value * 1000.0).toInt() / 10.0}%"

private fun fetchFargoText(url: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
    js("fetch(url).then(function(response) { if (!response.ok) { throw new Error(response.status + ' ' + response.statusText); } return response.text(); }).then(function(text) { onSuccess(text); }).catch(function(error) { onError(String(error)); });")
}
