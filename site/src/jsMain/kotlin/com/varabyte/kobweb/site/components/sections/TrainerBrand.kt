package com.varabyte.kobweb.site.components.sections

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.AlignItems
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.*
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.navigation.Anchor
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun TrainerBrand(modifier: Modifier = Modifier, iconSizePx: Int = 34, textSizeRem: Double = 1.05) {
    Anchor(href = "/") {
        Row(
            modifier
                .gap(0.75.cssRem)
                .alignItems(AlignItems.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PoolBallIcon(iconSizePx)
            Span(
                attrs = Modifier
                    .fontSize(textSizeRem.cssRem)
                    .fontWeight(FontWeight.SemiBold)
                    .toAttrs()
            ) {
                Text("Fractional Aiming Trainer")
            }
        }
    }
}

@Composable
fun PoolBallIcon(sizePx: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(sizePx.px)
            .borderRadius(50.percent)
            .backgroundColor(Color.rgb(26, 112, 66))
            .border(2.px, org.jetbrains.compose.web.css.LineStyle.Solid, Color.rgba(255, 255, 255, 0.26f))
            .position(org.jetbrains.compose.web.css.Position.Relative)
            .styleModifier {
                property("box-shadow", "inset 0 10px 16px rgba(255,255,255,0.15), 0 8px 16px rgba(0,0,0,0.18)")
            },
        contentAlignment = Alignment.Center
    ) {
        Div(
            attrs = Modifier
                .size((sizePx * 0.52).px)
                .borderRadius(50.percent)
                .backgroundColor(Colors.White)
                .toAttrs()
        )
        Span(
            attrs = Modifier
                .position(org.jetbrains.compose.web.css.Position.Absolute)
                .fontSize((sizePx * 0.32).px)
                .fontWeight(FontWeight.Bold)
                .color(Color.rgb(19, 24, 20))
                .toAttrs()
        ) {
            Text("8")
        }
    }
}
