package com.varabyte.kobweb.site.components.widgets

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.border
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.display
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.fontWeight
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.left
import com.varabyte.kobweb.compose.ui.modifiers.lineHeight
import com.varabyte.kobweb.compose.ui.modifiers.overflow
import com.varabyte.kobweb.compose.ui.modifiers.position
import com.varabyte.kobweb.compose.ui.modifiers.top
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.modifiers.zIndex
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

private data class BallSpec(val color: Color)

private val ballSpecs = mapOf(
    1 to BallSpec(Color.rgb(243, 197, 31)),
    2 to BallSpec(Color.rgb(32, 79, 189)),
    3 to BallSpec(Color.rgb(209, 40, 40)),
    4 to BallSpec(Color.rgb(90, 39, 143)),
    5 to BallSpec(Color.rgb(236, 118, 35)),
    6 to BallSpec(Color.rgb(35, 123, 75)),
    7 to BallSpec(Color.rgb(138, 47, 32)),
    8 to BallSpec(Color.rgb(17, 17, 17)),
    9 to BallSpec(Color.rgb(243, 197, 31)),
    10 to BallSpec(Color.rgb(32, 79, 189)),
    11 to BallSpec(Color.rgb(209, 40, 40)),
    12 to BallSpec(Color.rgb(90, 39, 143)),
    13 to BallSpec(Color.rgb(236, 118, 35)),
    14 to BallSpec(Color.rgb(35, 123, 75)),
    15 to BallSpec(Color.rgb(138, 47, 32)),
)

@Composable
fun PoolBall(
    ballNumber: Int,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    fillParent: Boolean = false,
) {
    require(ballNumber in 1..15) { "PoolBall supports standard numbered 8-ball balls from 1 to 15." }

    val spec = ballSpecs.getValue(ballNumber)
    val isStripe = ballNumber >= 9
    val sizedModifier = if (fillParent) {
        modifier
            .position(Position.Relative)
            .width(100.percent)
            .height(100.percent)
    } else {
        modifier
            .position(Position.Relative)
            .width(33.px)
            .height(33.px)
    }

    Div(
        attrs = sizedModifier
            .borderRadius(50.percent)
            .overflow(Overflow.Hidden)
            .backgroundColor(if (isStripe) Colors.White else spec.color)
            .border(1.px, LineStyle.Solid, if (selected) Color.rgb(255, 218, 87) else Color.rgba(8, 10, 14, 0.46f))
            .styleModifier {
                property("box-sizing", "border-box")
                property("box-shadow", ballShadow(selected))
                property("background-image", "radial-gradient(circle at 30% 24%, rgba(255,255,255,0.92), rgba(255,255,255,0.16) 23%, rgba(0,0,0,0.18) 78%)")
            }
            .toAttrs()
    ) {
        if (isStripe) {
            Div(
                attrs = Modifier
                    .position(Position.Absolute)
                    .left(0.percent)
                    .top(31.percent)
                    .width(100.percent)
                    .height(38.percent)
                    .backgroundColor(spec.color)
                    .styleModifier {
                        property("box-shadow", "inset 0 4px 7px rgba(255,255,255,0.22), inset 0 -5px 9px rgba(0,0,0,0.24)")
                    }
                    .toAttrs()
            )
        }
        BallNumber(number = ballNumber)
    }
}

@Composable
fun CueBall(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    fillParent: Boolean = false,
) {
    val sizedModifier = if (fillParent) {
        modifier
            .position(Position.Relative)
            .width(100.percent)
            .height(100.percent)
    } else {
        modifier
            .position(Position.Relative)
            .width(33.px)
            .height(33.px)
    }

    Div(
        attrs = sizedModifier
            .borderRadius(50.percent)
            .backgroundColor(Colors.White)
            .border(1.px, LineStyle.Solid, if (selected) Color.rgb(255, 218, 87) else Color.rgba(8, 10, 14, 0.36f))
            .styleModifier {
                property("box-sizing", "border-box")
                property("box-shadow", ballShadow(selected))
                property("background-image", "radial-gradient(circle at 30% 24%, #ffffff, #f4f0df 42%, #c7c3ae)")
            }
            .toAttrs()
    )
}

@Composable
private fun BallNumber(number: Int) {
    Span(
        attrs = Modifier
            .position(Position.Absolute)
            .left(50.percent)
            .top(50.percent)
            .width(64.percent)
            .height(64.percent)
            .borderRadius(50.percent)
            .backgroundColor(Colors.White)
            .color(Colors.Black)
            .display(DisplayStyle.Flex)
            .fontSize(12.px)
            .fontWeight(FontWeight.Bold)
            .lineHeight(1)
            .styleModifier {
                property("align-items", "center")
                property("justify-content", "center")
                property("transform", "translate(-50%, -50%)")
                property("box-shadow", "inset 0 -1px 2px rgba(0,0,0,0.22)")
            }
            .zIndex(2)
            .toAttrs()
    ) {
        Text(number.toString())
    }
}

private fun ballShadow(selected: Boolean): String {
    return if (selected) {
        "0 0 0 4px rgba(255,218,87,0.38), 0 12px 24px rgba(0,0,0,0.34), inset 0 9px 13px rgba(255,255,255,0.34), inset 0 -12px 17px rgba(0,0,0,0.30)"
    } else {
        "0 10px 20px rgba(0,0,0,0.32), inset 0 9px 13px rgba(255,255,255,0.30), inset 0 -12px 17px rgba(0,0,0,0.30)"
    }
}
