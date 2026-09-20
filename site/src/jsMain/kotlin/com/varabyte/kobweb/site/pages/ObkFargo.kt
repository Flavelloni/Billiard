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
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.site.components.layouts.PageLayoutData
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
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import kotlin.math.abs
import kotlin.math.pow

private data class FargoPlayer(
    val id: String,
    val name: String,
    val fargoRating: Double,
    val latestHandicap: Double?,
    val games: Int,
    val wins: Int,
    val losses: Int,
)

private enum class FargoSortMode {
    Fargo,
    Obk,
}

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
            BasePath.prependTo("/fargo/player_fargo_ratings.csv"),
            onSuccess = {
                players = parseFargoPlayers(it)
                loadError = null
            },
            onError = { loadError = language.text("Could not load Fargo ratings: $it", "Kunne ikke laste Fargo-ratinger: $it") },
        )
        fetchFargoText(
            BasePath.prependTo("/fargo/meta.json"),
            onSuccess = { historyYears = parseFargoHistoryYears(it) },
            onError = { historyYears = null },
        )
    }

    val loadedPlayers = players.orEmpty()
    val filteredPlayers = loadedPlayers
        .filter { playerFilter.isBlank() || it.name.contains(playerFilter, ignoreCase = true) }
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
                Text(language.text("Ratings are calculated solely from Oslo Biljardklubb tournaments from the last ${historyYears ?: "?"} years.", "Ratingene er beregnet kun fra Oslo Biljardklubb-turneringer fra de siste ${historyYears ?: "?"} årene."))
            }
        }

        when {
            loadError != null -> FargoMessage(loadError ?: language.text("Could not load Fargo ratings.", "Kunne ikke laste Fargo-ratinger."))
            players == null -> FargoMessage(language.text("Loading ratings...", "Laster ratinger..."))
            else -> {
                MatchupPanel(
                    language = language,
                    players = loadedPlayers,
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
                    Text(language.text("Search two players, confirm the names, then compare ratings, OBK records, expected win chances, and fair race spots.", "Søk opp to spillere, bekreft navnene, og sammenlign ratinger, OBK-statistikk, forventede vinnersjanser og rettferdige race-handicap."))
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

        MatchupResult(firstPlayer, secondPlayer, language)
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
private fun MatchupResult(firstPlayer: FargoPlayer?, secondPlayer: FargoPlayer?, language: SiteLanguage) {
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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .gap(0.7.cssRem)
                        .styleModifier {
                            property("flex-direction", "column")
                        },
                    verticalAlignment = Alignment.Top,
                ) {
                    RaceSuggestionCard(4, firstPlayer, secondPlayer, firstProbability, language)
                    RaceSuggestionCard(5, firstPlayer, secondPlayer, firstProbability, language)
                }
                P(
                    attrs = Modifier
                        .margin(0.px)
                        .fontSize(0.88.cssRem)
                        .lineHeight(1.5)
                        .color(Color.rgba(245, 248, 244, 0.6f))
                        .toAttrs()
                ) {
                    Text(language.text("The CSV contains aggregate OBK records, not direct opponent-by-opponent match history. The head-to-head figures here are calculated from Fargo rating difference.", "CSV-filen inneholder samlet OBK-statistikk, ikke direkte kampstatistikk spiller mot spiller. Head-to-head-tallene her beregnes fra Fargo-ratingforskjellen."))
                }
            }
        }
    }
}

@Composable
private fun PlayerComparisonCard(player: FargoPlayer, probability: Double) {
    Column(
        Modifier
            .padding(0.85.cssRem)
            .borderRadius(16.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.07f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.1f))
            .gap(0.35.cssRem)
            .styleModifier { property("flex", "1 1 260px") }
    ) {
        Span(
            attrs = Modifier
                .fontSize(1.05.cssRem)
                .fontWeight(FontWeight.Bold)
                .color(Colors.White)
                .toAttrs()
        ) {
            Text(player.name)
        }
        FargoStatLine("Fargo", formatRating(player.fargoRating))
        FargoStatLine(LocalSiteLanguage.current.text("Simple OBK", "Enkel OBK"), formatOptionalRating(player.latestHandicap))
        FargoStatLine(LocalSiteLanguage.current.text("OBK record", "OBK-statistikk"), "${player.wins}-${player.losses} (${player.games} ${LocalSiteLanguage.current.text("games", "partier")})")
        FargoStatLine(LocalSiteLanguage.current.text("Expected rack win", "Forventet partisjanse"), formatPercent(probability))
    }
}

@Composable
private fun RaceSuggestionCard(raceTo: Int, firstPlayer: FargoPlayer, secondPlayer: FargoPlayer, firstProbability: Double, language: SiteLanguage) {
    val suggestion = raceSpotSuggestion(raceTo, firstPlayer, secondPlayer, firstProbability, language)
    Column(
        Modifier
            .padding(0.85.cssRem)
            .borderRadius(16.px)
            .backgroundColor(Color.rgba(239, 190, 83, 0.12f))
            .border(1.px, LineStyle.Solid, Color.rgba(239, 190, 83, 0.3f))
            .gap(0.35.cssRem)
            .styleModifier { property("flex", "1 1 260px") }
    ) {
        Span(
            attrs = Modifier
                .fontSize(0.85.cssRem)
                .fontWeight(FontWeight.Bold)
                .color(Color.rgb(239, 210, 133))
                .toAttrs()
        ) {
            Text(language.text("Race to $raceTo", "Race til $raceTo"))
        }
        Span(
            attrs = Modifier
                .fontSize(1.02.cssRem)
                .lineHeight(1.45)
                .fontWeight(FontWeight.SemiBold)
                .color(Colors.White)
                .toAttrs()
        ) {
            Text(suggestion)
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
                    Text(language.text("${players.size} of $totalPlayers players", "${players.size} av $totalPlayers spillere"))
                }
            }
            Row(
                Modifier
                    .gap(0.5.cssRem)
                    .flexWrap(FlexWrap.Wrap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SortButton("Fargo", sortMode == FargoSortMode.Fargo) { onSortMode(FargoSortMode.Fargo) }
                SortButton(language.text("Simple OBK", "Enkel OBK"), sortMode == FargoSortMode.Obk) { onSortMode(FargoSortMode.Obk) }
                Div(attrs = Modifier.width(230.px).toAttrs()) {
                    FargoInput(filter, language.text("Filter player", "Filtrer spiller"), onFilter)
                }
            }
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
                PlayerMobileStat("Fargo", formatRating(player.fargoRating))
                PlayerMobileStat(language.text("Simple OBK", "Enkel OBK"), formatOptionalRating(player.latestHandicap))
                PlayerMobileStat(language.text("Record", "Statistikk"), "${player.wins}-${player.losses}")
            }
        }
    }
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
private fun PlayerMobileStat(label: String, value: String) {
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
                .color(Color.rgba(245, 248, 244, 0.92f))
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
private fun FargoStatLine(label: String, value: String) {
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
                .color(Color.rgba(245, 248, 244, 0.9f))
                .toAttrs()
        ) {
            Text(value)
        }
    }
}

private fun parseFargoPlayers(csv: String): List<FargoPlayer> {
    return csv.lineSequence()
        .drop(1)
        .mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val columns = parseCsvLine(line)
            if (columns.size < 10) return@mapNotNull null
            FargoPlayer(
                id = columns[0],
                name = columns[1],
                fargoRating = columns[2].toDoubleOrNull() ?: return@mapNotNull null,
                latestHandicap = columns[3].toDoubleOrNull(),
                games = columns[6].toIntOrNull() ?: 0,
                wins = columns[7].toIntOrNull() ?: 0,
                losses = columns[8].toIntOrNull() ?: 0,
            )
        }
        .toList()
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

private fun parseFargoHistoryYears(json: String): String {
    val item = js("JSON.parse(json)")
    return (item.history_years as? String).orEmpty().ifBlank { "?" }
}

private fun fargoGameProbability(rating: Double, opponentRating: Double): Double {
    return 1.0 / (1.0 + 2.0.pow((opponentRating - rating) / 100.0))
}

private fun raceSpotSuggestion(raceTo: Int, firstPlayer: FargoPlayer, secondPlayer: FargoPlayer, firstProbability: Double, language: SiteLanguage): String {
    val favorite = if (firstProbability >= 0.5) firstPlayer else secondPlayer
    val underdog = if (favorite == firstPlayer) secondPlayer else firstPlayer
    val favoriteRackProbability = if (favorite == firstPlayer) firstProbability else 1.0 - firstProbability
    val bestSpot = (0 until raceTo).minByOrNull { spot ->
        abs(matchWinProbability(favoriteRackProbability, raceTo, raceTo - spot) - 0.5)
    } ?: 0

    val favoriteMatchProbability = matchWinProbability(favoriteRackProbability, raceTo, raceTo - bestSpot)
    return if (bestSpot == 0) {
        language.text(
            "Even race. ${favorite.name} is about ${formatPercent(favoriteMatchProbability)} to win.",
            "Jevnt race. ${favorite.name} har omtrent ${formatPercent(favoriteMatchProbability)} sjanse til å vinne.",
        )
    } else {
        language.text(
            "${underdog.name} starts ahead $bestSpot-0. ${favorite.name} is about ${formatPercent(favoriteMatchProbability)} to win.",
            "${underdog.name} starter foran $bestSpot-0. ${favorite.name} har omtrent ${formatPercent(favoriteMatchProbability)} sjanse til å vinne.",
        )
    }
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
