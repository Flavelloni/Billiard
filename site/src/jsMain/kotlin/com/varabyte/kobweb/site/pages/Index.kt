package com.varabyte.kobweb.site.pages

import androidx.compose.runtime.Composable
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
import com.varabyte.kobweb.compose.ui.modifiers.size
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.navigation.Anchor
import com.varabyte.kobweb.site.components.layouts.PageLayoutData
import com.varabyte.kobweb.site.components.widgets.EightBallToolIcon
import com.varabyte.kobweb.site.components.widgets.OverlapToolIcon
import com.varabyte.kobweb.site.components.widgets.RatingToolIcon
import com.varabyte.kobweb.site.model.LocalSiteLanguage
import com.varabyte.kobweb.site.model.SiteLanguage
import com.varabyte.kobweb.site.model.text
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

private data class HomeTool(
    val titleEnglish: String,
    val titleNorwegian: String,
    val subtitleEnglish: String,
    val subtitleNorwegian: String,
    val href: String,
    val accent: Color.Rgb,
    val icon: @Composable (Int) -> Unit,
)

private val homeTools = listOf(
    HomeTool(
        titleEnglish = "OBK Fargo",
        titleNorwegian = "OBK Fargo",
        subtitleEnglish = "Ratings, player search, matchup odds, and race handicap suggestions.",
        subtitleNorwegian = "Ratinger, spillersøk, matchup-sjanser og forslag til handicap i race.",
        href = "/obk-fargo",
        accent = Color.rgb(239, 190, 83),
        icon = { size -> RatingToolIcon(size) },
    ),
    HomeTool(
        titleEnglish = "8 Ball Prediction",
        titleNorwegian = "8-ball prediksjon",
        subtitleEnglish = "Pick the next ball from real match layouts.",
        subtitleNorwegian = "Velg neste ball fra ekte kampoppsett.",
        href = "/8-ball-prediction",
        accent = Color.rgb(99, 164, 255),
        icon = { size -> EightBallToolIcon(size) },
    ),
    HomeTool(
        titleEnglish = "Fractional Aiming",
        titleNorwegian = "Fraksjonssikting",
        subtitleEnglish = "Practice cue-ball overlap and cut-angle reads.",
        subtitleNorwegian = "Tren på overlapp, treffbilde og lesing av kuttvinkler.",
        href = "/pool-trainer",
        accent = Color.rgb(76, 211, 140),
        icon = { size -> OverlapToolIcon(size) },
    ),
)

@InitRoute
fun initHomePage(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("Pool Buddy", "Pool tools for aiming practice, OBK Fargo ratings, and 8-ball shot prediction."))
}

@Page
@Composable
@Layout(".components.layouts.PageLayout")
fun HomePage() {
    val language = LocalSiteLanguage.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(leftRight = 1.25.cssRem, top = 3.cssRem, bottom = 4.cssRem)
            .gap(2.2.cssRem)
            .styleModifier {
                property("min-height", "calc(100vh - 64px)")
                property("background", "linear-gradient(145deg, #071012 0%, #13201b 45%, #15131d 100%)")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .maxWidth(1120.px)
                .gap(1.cssRem),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            H1(
                attrs = Modifier
                    .margin(0.px)
                    .fontSize(3.2.cssRem)
                    .lineHeight(1.0)
                    .fontWeight(FontWeight.Bold)
                    .color(Colors.White)
                    .styleModifier { property("text-align", "center") }
                    .toAttrs()
            ) {
                Text("Pool Buddy")
            }
            P(
                attrs = Modifier
                    .margin(0.px)
                    .maxWidth(760.px)
                    .fontSize(1.1.cssRem)
                    .lineHeight(1.65)
                    .color(Color.rgba(245, 248, 244, 0.76f))
                    .styleModifier { property("text-align", "center") }
                    .toAttrs()
            ) {
                Text(language.text("Choose a tool for aiming practice, OBK Fargo ratings, or 8-ball shot prediction.", "Velg et verktøy for siktetrening, OBK Fargo-rating eller 8-ball prediksjon."))
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .maxWidth(1120.px)
                .gap(1.cssRem)
                .flexWrap(FlexWrap.Wrap)
                .styleModifier { property("justify-content", "center") },
            verticalAlignment = Alignment.Top,
        ) {
            homeTools.forEach { tool ->
                HomeToolTile(tool, language)
            }
        }
    }
}

@Composable
private fun HomeToolTile(tool: HomeTool, language: SiteLanguage) {
    Anchor(
        href = tool.href,
        attrs = Modifier
            .padding(1.2.cssRem)
            .borderRadius(24.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.08f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.15f))
            .color(Colors.White)
            .styleModifier {
                property("width", "min(100%, 340px)")
                property("height", "260px")
                property("box-sizing", "border-box")
                property("text-decoration", "none")
                property("display", "flex")
                property("align-items", "stretch")
                property("box-shadow", "0 26px 70px rgba(0, 0, 0, 0.28)")
                property("transition", "transform 160ms ease, border-color 160ms ease, background 160ms ease")
            }
            .toAttrs()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .gap(1.cssRem),
            horizontalAlignment = Alignment.Start,
        ) {
            Div(
                attrs = Modifier
                    .size(76.px)
                    .styleModifier {
                        property("display", "flex")
                        property("align-items", "center")
                        property("justify-content", "center")
                    }
                    .toAttrs()
            ) {
                tool.icon(76)
            }
            Column(Modifier.gap(0.45.cssRem)) {
                Span(
                    attrs = Modifier
                        .fontSize(1.45.cssRem)
                        .fontWeight(FontWeight.Bold)
                        .lineHeight(1.1)
                        .toAttrs()
                ) {
                    Text(language.text(tool.titleEnglish, tool.titleNorwegian))
                }
                P(
                    attrs = Modifier
                        .margin(0.px)
                        .fontSize(0.98.cssRem)
                        .lineHeight(1.55)
                        .color(Color.rgba(245, 248, 244, 0.74f))
                        .toAttrs()
                ) {
                    Text(language.text(tool.subtitleEnglish, tool.subtitleNorwegian))
                }
            }
            Span(
                attrs = Modifier
                    .padding(top = 0.4.cssRem)
                    .fontSize(0.9.cssRem)
                    .fontWeight(FontWeight.SemiBold)
                    .color(tool.accent)
                    .styleModifier { property("margin-top", "auto") }
                    .toAttrs()
            ) {
                Text(language.text("Open", "Åpne"))
            }
        }
    }
}
