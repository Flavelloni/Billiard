package com.varabyte.kobweb.site.components.sections

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.CSSLengthNumericValue
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.StyleVariable
import com.varabyte.kobweb.compose.css.functions.blur
import com.varabyte.kobweb.compose.css.functions.saturate
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.foundation.layout.Spacer
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.navigation.Anchor
import com.varabyte.kobweb.silk.init.InitSilk
import com.varabyte.kobweb.silk.init.InitSilkContext
import com.varabyte.kobweb.silk.style.CssStyle
import com.varabyte.kobweb.silk.style.breakpoint.Breakpoint
import com.varabyte.kobweb.silk.style.common.SmoothColorStyle
import com.varabyte.kobweb.silk.style.extendedByBase
import com.varabyte.kobweb.silk.style.selectors.hover
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import com.varabyte.kobweb.silk.theme.colors.palette.color
import com.varabyte.kobweb.silk.theme.colors.palette.toPalette
import com.varabyte.kobweb.silk.theme.colors.shifted
import com.varabyte.kobweb.site.components.style.dividerBoxShadow
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

val NavHeaderHeight by StyleVariable<CSSLengthNumericValue>()

@InitSilk
fun initNavHeaderHeight(ctx: InitSilkContext) = with(ctx.stylesheet) {
    registerStyle("html") {
        base { Modifier.setVariable(NavHeaderHeight, 56.px) }
        Breakpoint.MD { Modifier.setVariable(NavHeaderHeight, 64.px) }
    }
}

val NavHeaderBackgroundStyle = SmoothColorStyle.extendedByBase {
    Modifier
        .backgroundColor(getNavBackgroundColor(colorMode))
        .backdropFilter(saturate(180.percent), blur(5.px))
        .dividerBoxShadow()
}

val NavHeaderDarkenedBackgroundStyle = NavHeaderBackgroundStyle.extendedByBase {
    Modifier
        .backgroundColor(getNavBackgroundColor(colorMode).copyf(alpha = 0.8f))
}

val NavHeaderStyle = NavHeaderBackgroundStyle.extendedByBase {
    Modifier
        .fillMaxWidth()
        .position(Position.Sticky)
        .top(0.percent)
        .height(NavHeaderHeight.value())
}

val HoverBrightenStyle = CssStyle {
    val color = colorMode.toPalette().color
    base {
        Modifier.color(color.shifted(colorMode.opposite, 0.2f))
    }
    hover {
        Modifier.color(color)
    }
}

private fun getNavBackgroundColor(colorMode: ColorMode): Color.Rgb {
    return when (colorMode) {
        ColorMode.DARK -> Colors.Black
        ColorMode.LIGHT -> Colors.White
    }.copyf(alpha = 0.65f)
}

// The nav header needs a higher z-index to be shown above elements with `position: sticky`
fun Modifier.navHeaderZIndex() = this.zIndex(10)

@OptIn(ExperimentalJsCollectionsApi::class, ExperimentalJsExport::class)
@Composable
fun NavHeader() {
    Box(NavHeaderStyle.toModifier().navHeaderZIndex(), contentAlignment = Alignment.Center) {
        Row(
            Modifier.fillMaxWidth(90.percent),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrainerBrand()
            Spacer()

            Row(
                Modifier
                    .margin(0.px, 12.px)
                    .gap(1.cssRem)
                    .fontSize(1.5.cssRem),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderToolLink("AIM", "/pool-trainer", "Fractional aiming")
                HeaderToolLink("OBK", "/obk-fargo", "OBK Fargo rating")
                HeaderToolLink("8", "/8-ball-prediction", "8 ball prediction")
            }
        }
    }
}

@Composable
private fun HeaderToolLink(label: String, href: String, title: String) {
    Anchor(
        href = href,
        attrs = Modifier
            .padding(leftRight = 0.72.cssRem, topBottom = 0.45.cssRem)
            .borderRadius(999.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.07f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.14f))
            .color(Color.rgba(245, 248, 244, 0.86f))
            .styleModifier {
                property("text-decoration", "none")
                property("display", "inline-flex")
                property("align-items", "center")
                property("justify-content", "center")
                property("min-width", "42px")
                property("height", "34px")
                property("box-sizing", "border-box")
                property("touch-action", "manipulation")
            }
            .toAttrs {
                attr("aria-label", title)
                attr("title", title)
            }
    ) {
        Span(
            attrs = Modifier
                .fontSize(if (label == "8") 1.1.cssRem else 0.78.cssRem)
                .fontWeight(FontWeight.Bold)
                .toAttrs()
        ) {
            Text(label)
        }
    }
}
