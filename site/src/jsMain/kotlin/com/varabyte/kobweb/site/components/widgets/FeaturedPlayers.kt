package com.varabyte.kobweb.site.components.widgets

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
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
import com.varabyte.kobweb.compose.ui.modifiers.maxWidth
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text

private data class FeaturedPlayer(
    val name: String,
    val subtitle: String,
    val imageUrl: String,
    val accent: Color,
)

private val featuredPlayers = listOf(
    FeaturedPlayer(
        name = "Joshua Filler",
        subtitle = "The Killer",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/59/Joshua_Filler_straight_pool.jpg/250px-Joshua_Filler_straight_pool.jpg",
        accent = Color.rgb(54, 154, 232),
    ),
    FeaturedPlayer(
        name = "Shane Van Boening",
        subtitle = "South Dakota Kid",
        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f0/Shane_Van_Boening.JPG/250px-Shane_Van_Boening.JPG",
        accent = Color.rgb(211, 43, 43),
    ),
)

@Composable
fun FeaturedPlayers() {
    Row(
        Modifier
            .fillMaxWidth()
            .maxWidth(900.px)
            .gap(0.9.cssRem)
            .flexWrap(FlexWrap.Wrap)
            .styleModifier {
                property("justify-content", "center")
            }
    ) {
        featuredPlayers.forEach { player ->
            PlayerChip(player)
        }
    }
}

@Composable
private fun PlayerChip(player: FeaturedPlayer) {
    Row(
        Modifier
            .padding(leftRight = 0.85.cssRem, topBottom = 0.72.cssRem)
            .gap(0.78.cssRem)
            .borderRadius(18.px)
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.14f))
            .backgroundColor(Color.rgba(255, 255, 255, 0.07f))
            .styleModifier {
                property("align-items", "center")
                property("box-shadow", "0 12px 34px rgba(0,0,0,0.22), inset 0 1px 0 rgba(255,255,255,0.08)")
                property("backdrop-filter", "blur(10px)")
            }
    ) {
        Img(
            src = player.imageUrl,
            attrs = Modifier
                .width(112.px)
                .height(82.px)
                .borderRadius(12.px)
                .border(2.px, LineStyle.Solid, player.accent)
                .styleModifier {
                    property("object-fit", "contain")
                    property("box-shadow", "0 8px 18px rgba(0,0,0,0.32)")
                    property("background", "#111827")
                }
                .toAttrs {
                    attr("alt", player.name)
                    attr("loading", "lazy")
                    attr("referrerpolicy", "no-referrer")
                }
        )
        Column(Modifier.gap(0.12.cssRem)) {
            Div(
                attrs = Modifier
                    .fontSize(1.05.cssRem)
                    .fontWeight(FontWeight.Bold)
                    .lineHeight(1.1)
                    .color(Colors.White)
                    .toAttrs()
            ) {
                Text(player.name)
            }
            Div(
                attrs = Modifier
                    .fontSize(0.82.cssRem)
                    .lineHeight(1.15)
                    .color(Color.rgba(226, 238, 230, 0.66f))
                    .toAttrs()
            ) {
                Text(player.subtitle)
            }
        }
    }
}
