package com.varabyte.kobweb.site.components.widgets

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.border
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.fontWeight
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.left
import com.varabyte.kobweb.compose.ui.modifiers.position
import com.varabyte.kobweb.compose.ui.modifiers.size
import com.varabyte.kobweb.compose.ui.modifiers.top
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun RatingToolIcon(sizePx: Int, modifier: Modifier = Modifier) {
    Div(
        attrs = modifier
            .size(sizePx.px)
            .borderRadius(24.px)
            .backgroundColor(Color.rgba(239, 190, 83, 0.16f))
            .border(1.px, LineStyle.Solid, Color.rgba(239, 190, 83, 0.42f))
            .position(Position.Relative)
            .styleModifier {
                property("box-shadow", "inset 0 1px 0 rgba(255,255,255,0.16)")
            }
            .toAttrs()
    ) {
        RatingStar(sizePx, leftPercent = 16, topPercent = 34, scale = 0.34)
        RatingStar(sizePx, leftPercent = 50, topPercent = 22, scale = 0.42)
        RatingStar(sizePx, leftPercent = 84, topPercent = 34, scale = 0.34)
    }
}

@Composable
private fun RatingStar(sizePx: Int, leftPercent: Int, topPercent: Int, scale: Double) {
    Span(
        attrs = Modifier
            .position(Position.Absolute)
            .left(leftPercent.percent)
            .top(topPercent.percent)
            .fontSize((sizePx * scale).px)
            .color(Color.rgb(247, 219, 143))
            .styleModifier {
                property("transform", "translate(-50%, -50%)")
                property("line-height", "1")
                property("text-shadow", "0 3px 10px rgba(239, 190, 83, 0.34)")
            }
            .toAttrs()
    ) {
        Text("★")
    }
}

@Composable
fun EightBallToolIcon(sizePx: Int, modifier: Modifier = Modifier) {
    Div(
        attrs = modifier
            .size(sizePx.px)
            .borderRadius(50.percent)
            .backgroundColor(Color.rgb(16, 18, 22))
            .border(2.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.22f))
            .position(Position.Relative)
            .styleModifier {
                property("box-shadow", "inset 0 14px 20px rgba(255,255,255,0.1), 0 14px 30px rgba(0,0,0,0.24)")
            }
            .toAttrs()
    ) {
        Div(
            attrs = Modifier
                .position(Position.Absolute)
                .left(50.percent)
                .top(50.percent)
                .size((sizePx * 0.48).px)
                .borderRadius(50.percent)
                .backgroundColor(Colors.White)
                .styleModifier { property("transform", "translate(-50%, -50%)") }
                .toAttrs()
        )
        Span(
            attrs = Modifier
                .position(Position.Absolute)
                .left(50.percent)
                .top(50.percent)
                .fontSize((sizePx * 0.26).px)
                .fontWeight(FontWeight.Bold)
                .color(Color.rgb(15, 16, 18))
                .styleModifier {
                    property("transform", "translate(-50%, -50%)")
                    property("line-height", "1")
                }
                .toAttrs()
        ) {
            Text("8")
        }
    }
}

@Composable
fun OverlapToolIcon(sizePx: Int, modifier: Modifier = Modifier) {
    Div(
        attrs = modifier
            .size(sizePx.px)
            .borderRadius(24.px)
            .backgroundColor(Color.rgba(76, 211, 140, 0.14f))
            .border(1.px, LineStyle.Solid, Color.rgba(76, 211, 140, 0.38f))
            .position(Position.Relative)
            .styleModifier {
                property("box-shadow", "inset 0 1px 0 rgba(255,255,255,0.16)")
            }
            .toAttrs()
    ) {
        Div(
            attrs = Modifier
                .position(Position.Absolute)
                .left(22.percent)
                .top(32.percent)
                .size((sizePx * 0.43).px)
                .borderRadius(50.percent)
                .backgroundColor(Color.rgb(218, 46, 46))
                .border(2.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.72f))
                .toAttrs()
        )
        Div(
            attrs = Modifier
                .position(Position.Absolute)
                .left(40.percent)
                .top(32.percent)
                .size((sizePx * 0.43).px)
                .borderRadius(50.percent)
                .backgroundColor(Color.rgba(246, 246, 239, 0.88f))
                .border(2.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.88f))
                .toAttrs()
        )
    }
}
