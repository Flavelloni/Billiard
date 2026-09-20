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
    val title: String,
    val subtitle: String,
    val href: String,
    val icon: String,
    val accent: Color.Rgb,
)

private val homeTools = listOf(
    HomeTool(
        title = "Fractional Aiming",
        subtitle = "Practice cue-ball overlap and cut-angle reads.",
        href = "/pool-trainer",
        icon = "AIM",
        accent = Color.rgb(76, 211, 140),
    ),
    HomeTool(
        title = "OBK Fargo",
        subtitle = "Ratings, player search, and race handicap suggestions.",
        href = "/obk-fargo",
        icon = "OBK",
        accent = Color.rgb(239, 190, 83),
    ),
    HomeTool(
        title = "8 Ball Prediction",
        subtitle = "Pick the next ball from real match layouts.",
        href = "/8-ball-prediction",
        icon = "8",
        accent = Color.rgb(99, 164, 255),
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
                Text("Choose a tool for aiming practice, OBK Fargo ratings, or 8-ball shot prediction.")
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
                HomeToolTile(tool)
            }
        }
    }
}

@Composable
private fun HomeToolTile(tool: HomeTool) {
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
                property("min-height", "230px")
                property("box-sizing", "border-box")
                property("text-decoration", "none")
                property("display", "flex")
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
                    .borderRadius(22.px)
                    .backgroundColor(tool.accent.copyf(alpha = 0.18f))
                    .border(1.px, LineStyle.Solid, tool.accent.copyf(alpha = 0.48f))
                    .styleModifier {
                        property("display", "flex")
                        property("align-items", "center")
                        property("justify-content", "center")
                        property("box-shadow", "inset 0 1px 0 rgba(255,255,255,0.12)")
                    }
                    .toAttrs()
            ) {
                Span(
                    attrs = Modifier
                        .fontSize(if (tool.icon == "8") 2.4.cssRem else 1.05.cssRem)
                        .fontWeight(FontWeight.Bold)
                        .color(tool.accent)
                        .toAttrs()
                ) {
                    Text(tool.icon)
                }
            }
            Column(Modifier.gap(0.45.cssRem)) {
                Span(
                    attrs = Modifier
                        .fontSize(1.45.cssRem)
                        .fontWeight(FontWeight.Bold)
                        .lineHeight(1.1)
                        .toAttrs()
                ) {
                    Text(tool.title)
                }
                P(
                    attrs = Modifier
                        .margin(0.px)
                        .fontSize(0.98.cssRem)
                        .lineHeight(1.55)
                        .color(Color.rgba(245, 248, 244, 0.74f))
                        .toAttrs()
                ) {
                    Text(tool.subtitle)
                }
            }
            Span(
                attrs = Modifier
                    .padding(top = 0.4.cssRem)
                    .fontSize(0.9.cssRem)
                    .fontWeight(FontWeight.SemiBold)
                    .color(tool.accent)
                    .toAttrs()
            ) {
                Text("Open")
            }
        }
    }
}
