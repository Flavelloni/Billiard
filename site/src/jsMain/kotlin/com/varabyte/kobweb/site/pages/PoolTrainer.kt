package com.varabyte.kobweb.site.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.*
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.*
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.site.components.layouts.PageLayoutData
import com.varabyte.kobweb.site.components.style.MutedSpanTextVariant
import com.varabyte.kobweb.site.components.style.SiteTextSize
import com.varabyte.kobweb.site.components.style.siteText
import kotlinx.browser.window
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val TABLE_WIDTH = 1000.0
private const val TABLE_HEIGHT = 560.0
private const val BALL_RADIUS = 20.0
private const val MOBILE_TABLE_BALL_RADIUS = 24.0
private const val POCKET_RADIUS = 34.0
private const val CORNER_POCKET_TARGET_INSET = 28.0
private const val SIDE_POCKET_TARGET_INSET = 12.0
private const val MAX_REQUIRED_CUT_ANGLE = 60.0
private const val OVERLAP_WIDTH = 620.0
private const val OVERLAP_HEIGHT = 280.0
private const val OVERLAP_BALL_RADIUS = 68.0
private const val MOBILE_OVERLAP_BALL_RADIUS = 82.0
private const val MOBILE_BREAKPOINT_PX = 768
private const val CUE_BALL_ALPHA = 0.82f
private const val TABLE_RAIL_PADDING = "clamp(10px, 2.2vw, 22px)"
private const val TABLE_DIAMOND_SIZE = "clamp(5px, 1.2vw, 9px)"

private data class Point(val x: Double, val y: Double) {
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    operator fun minus(other: Point) = Point(x - other.x, y - other.y)
    operator fun times(scale: Double) = Point(x * scale, y * scale)
}

private data class Pocket(
    val label: String,
    val renderCenter: Point,
    val aimPoint: Point,
)

private data class ShotOption(
    val pocket: Pocket,
    val cutAngleDegrees: Double,
)

private data class ShotSetup(
    val id: Int,
    val cueBall: Point,
    val objectBall: Point,
    val target: ShotOption,
)

private val pockets = listOf(
    Pocket("Top left", Point(0.0, 0.0), Point(CORNER_POCKET_TARGET_INSET, CORNER_POCKET_TARGET_INSET)),
    Pocket("Top middle", Point(TABLE_WIDTH / 2.0, 0.0), Point(TABLE_WIDTH / 2.0, SIDE_POCKET_TARGET_INSET)),
    Pocket("Top right", Point(TABLE_WIDTH, 0.0), Point(TABLE_WIDTH - CORNER_POCKET_TARGET_INSET, CORNER_POCKET_TARGET_INSET)),
    Pocket("Bottom left", Point(0.0, TABLE_HEIGHT), Point(CORNER_POCKET_TARGET_INSET, TABLE_HEIGHT - CORNER_POCKET_TARGET_INSET)),
    Pocket("Bottom middle", Point(TABLE_WIDTH / 2.0, TABLE_HEIGHT), Point(TABLE_WIDTH / 2.0, TABLE_HEIGHT - SIDE_POCKET_TARGET_INSET)),
    Pocket("Bottom right", Point(TABLE_WIDTH, TABLE_HEIGHT), Point(TABLE_WIDTH - CORNER_POCKET_TARGET_INSET, TABLE_HEIGHT - CORNER_POCKET_TARGET_INSET)),
)

private val topPockets = pockets.take(3)
private val longRailDiamondFractions = listOf(1, 2, 3, 5, 6, 7).map { it / 8.0 }
private val shortRailDiamondFractions = (1..3).map { it / 4.0 }

@InitRoute
fun initPoolTrainerPage(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("Pool Trainer", "Fractional aiming trainer with legal random potting layouts."))
}

@Page
@Composable
@Layout(".components.layouts.PageLayout")
fun PoolTrainerPage() {
    PoolTrainerScreen()
}

@Composable
fun PoolTrainerScreen() {
    val isMobile = rememberIsMobileLayout()
    val tableBallRadius = if (isMobile) MOBILE_TABLE_BALL_RADIUS else BALL_RADIUS
    val overlapBallRadius = if (isMobile) MOBILE_OVERLAP_BALL_RADIUS else OVERLAP_BALL_RADIUS
    var setup by remember { mutableStateOf(generateShotSetup()) }
    val perfectOverlap = remember(setup, overlapBallRadius) {
        overlapOffsetFromAngle(setup.target.cutAngleDegrees, overlapBallRadius)
    }

    val overlapOffsetState = remember(setup.id, isMobile) { mutableStateOf(-overlapBallRadius * 2.0) }
    var overlapOffset by overlapOffsetState
    var submitted by remember(setup.id) { mutableStateOf(false) }
    var overlapArea by remember { mutableStateOf<HTMLElement?>(null) }
    val draggingState = remember(setup.id) { mutableStateOf(false) }
    var dragging by draggingState

    val userCutAngle = overlapOffsetToAngle(overlapOffset, overlapBallRadius)
    val angleError = userCutAngle - setup.target.cutAngleDegrees

    fun updateOverlap(clientX: Double) {
        val rect = overlapArea?.getBoundingClientRect() ?: return
        val localX = ((clientX - rect.left) / rect.width).coerceIn(0.0, 1.0) * OVERLAP_WIDTH
        overlapOffset = (localX - OVERLAP_WIDTH / 2.0).coerceIn(-overlapBallRadius * 2.0, overlapBallRadius * 2.0)
    }

    fun tryStartDrag(clientX: Double, clientY: Double) {
        val rect = overlapArea?.getBoundingClientRect() ?: return
        val localX = ((clientX - rect.left) / rect.width).coerceIn(0.0, 1.0) * OVERLAP_WIDTH
        val localY = ((clientY - rect.top) / rect.height).coerceIn(0.0, 1.0) * OVERLAP_HEIGHT
        val cueCenter = Point(OVERLAP_WIDTH / 2.0 + overlapOffsetState.value, OVERLAP_HEIGHT / 2.0 + 6.0)
        if (distance(Point(localX, localY), cueCenter) <= overlapBallRadius * 1.05) {
            draggingState.value = true
            dragging = true
            updateOverlap(clientX)
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(leftRight = 1.25.cssRem, top = 2.cssRem, bottom = 3.cssRem)
            .gap(1.5.cssRem),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .maxWidth(1100.px)
                .padding(topBottom = 0.75.cssRem)
                .gap(0.8.cssRem),
            horizontalAlignment = Alignment.Start,
        ) {
            H1 {
                Text("Billiards Trainer")
            }
            SpanText(
                "Study the random table, read the cut from the highlighted pocket, then build the cue-ball overlap below.",
                Modifier.maxWidth(75.cssRem).siteText(SiteTextSize.NORMAL),
                MutedSpanTextVariant
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .maxWidth(1100.px)
                .padding(1.1.cssRem)
                .borderRadius(28.px)
                .backgroundColor(Color.rgba(7, 18, 14, 0.78f))
                .styleModifier {
                    property("box-shadow", "0 28px 90px rgba(0, 0, 0, 0.34)")
                    property("backdrop-filter", "blur(12px)")
                }
                .gap(1.25.cssRem)
        ) {
            PoolTable(
                setup = setup,
                shownCutAngle = if (submitted) userCutAngle else null,
                ballRadius = tableBallRadius,
            )

            /*Row(
                Modifier
                    .fillMaxWidth()
                    .flexWrap(FlexWrap.Wrap)
                    .justifyContent(JustifyContent.SpaceBetween)
                    .alignItems(AlignItems.Center)
                    .gap(0.9.cssRem)
            ) {
                Column(Modifier.gap(0.45.cssRem)) {
                    StatusChip("Target pocket", setup.target.pocket.label, Color.rgb(240, 198, 92))
                    StatusChip("Required side", angleSideLabel(setup.target.cutAngleDegrees), Color.rgb(115, 192, 214))
                }


            }*/
        }

        Column(
            Modifier
                .fillMaxWidth()
                .maxWidth(1100.px)
                .padding(1.1.cssRem)
                .borderRadius(28.px)
                .backgroundColor(Color.rgba(10, 23, 33, 0.82f))
                .styleModifier {
                    property("box-shadow", "0 22px 70px rgba(0, 0, 0, 0.28)")
                }
                .gap(1.1.cssRem)
        ) {
            key(setup.id, submitted) {
                Div(
                    attrs = Modifier
                        .fillMaxWidth()
                        .styleModifier { property("touch-action", "none") }
                        .toAttrs {
                            ref { element ->
                                overlapArea = element

                                val touchStart: (dynamic) -> Unit = { event ->
                                    event.preventDefault()
                                    firstTouchClientPosition(event)?.let { (x, y) -> tryStartDrag(x, y) }
                                }
                                val touchMove: (dynamic) -> Unit = { event ->
                                    if (draggingState.value) {
                                        event.preventDefault()
                                        firstTouchClientPosition(event)?.first?.let(::updateOverlap)
                                    }
                                }
                                val touchEnd: (dynamic) -> Unit = {
                                    draggingState.value = false
                                    dragging = false
                                }

                                element.addEventListener("touchstart", touchStart, js("{ passive: false }"))
                                window.addEventListener("touchmove", touchMove, js("{ passive: false }"))
                                window.addEventListener("touchend", touchEnd)
                                window.addEventListener("touchcancel", touchEnd)

                                onDispose {
                                    if (overlapArea == element) overlapArea = null
                                    element.removeEventListener("touchstart", touchStart)
                                    window.removeEventListener("touchmove", touchMove)
                                    window.removeEventListener("touchend", touchEnd)
                                    window.removeEventListener("touchcancel", touchEnd)
                                }
                            }
                            onMouseDown { event ->
                                event.preventDefault()
                                tryStartDrag(event.clientX.toDouble(), event.clientY.toDouble())
                            }
                            onMouseMove { event ->
                                if (dragging) {
                                    event.preventDefault()
                                    updateOverlap(event.clientX.toDouble())
                                }
                            }
                            onMouseUp {
                                draggingState.value = false
                                dragging = false
                            }
                            onMouseLeave {
                                draggingState.value = false
                                dragging = false
                            }
                        }
                ) {
                    OverlapTrainer(
                        overlapOffset = overlapOffset,
                        perfectOverlap = perfectOverlap,
                        submitted = submitted,
                        dragging = dragging,
                        ballRadius = overlapBallRadius,
                    )
                }
            }

            Column(Modifier.gap(0.35.cssRem)) {
                Span(
                    attrs = Modifier
                        .fontSize(1.3.cssRem)
                        .fontWeight(FontWeight.Medium)
                        .color(Colors.White)
                        .toAttrs()
                ) {
                    Text("Fractional overlap")
                }
                SpanText(
                    "Click and drag the cue ball horizontally over the object ball to recreate the contact picture for the shot above.",
                    Modifier.siteText(SiteTextSize.SMALL),
                    MutedSpanTextVariant
                )
            }

            Row(Modifier.gap(0.75.cssRem).flexWrap(FlexWrap.Wrap)) {
                ActionButton("Check overlap") { submitted = true }
                ActionButton("New layout") { setup = generateShotSetup() }
            }


            ResultPanel(
                submitted = submitted,
                perfectAngle = setup.target.cutAngleDegrees,
                userAngle = userCutAngle,
                angleError = angleError,
            )
        }
    }

}

@Composable
private fun rememberIsMobileLayout(): Boolean {
    var isMobile by remember { mutableStateOf(window.innerWidth <= MOBILE_BREAKPOINT_PX) }

    DisposableEffect(Unit) {
        val listener: (dynamic) -> Unit = {
            isMobile = window.innerWidth <= MOBILE_BREAKPOINT_PX
        }
        window.addEventListener("resize", listener)
        onDispose {
            window.removeEventListener("resize", listener)
        }
    }

    return isMobile
}

@Composable
private fun PoolTable(setup: ShotSetup, shownCutAngle: Double?, ballRadius: Double) {
    val shotLineEnd = shownCutAngle?.let { calculateShotLineEnd(setup, it) }
    Div(
        attrs = Modifier
            .fillMaxWidth()
            .styleModifier { property("aspect-ratio", "${TABLE_WIDTH / TABLE_HEIGHT}") }
            .backgroundColor(Color.rgb(80, 43, 18))
            .position(Position.Relative)
            .overflow(Overflow.Hidden)
            .styleModifier {
                property("padding", TABLE_RAIL_PADDING)
                property("border-radius", "clamp(18px, 4vw, 32px)")
                property("box-shadow", "inset 0 0 0 2px rgba(255, 228, 166, 0.18), inset 0 18px 36px rgba(255, 255, 255, 0.06), 0 18px 40px rgba(0, 0, 0, 0.3)")
                property("background-image", "linear-gradient(145deg, rgba(122,76,34,0.92), rgba(67,34,11,0.94))")
            }
            .toAttrs()
    ) {
        longRailDiamondFractions.forEach { fraction ->
            TableDiamond(RailSide.Top, fraction)
            TableDiamond(RailSide.Bottom, fraction)
        }
        shortRailDiamondFractions.forEach { fraction ->
            TableDiamond(RailSide.Left, fraction)
            TableDiamond(RailSide.Right, fraction)
        }

        Div(
            attrs = Modifier
                .fillMaxSize()
                .position(Position.Relative)
                .overflow(Overflow.Hidden)
                .backgroundColor(Color.rgb(21, 93, 58))
                .styleModifier {
                    property("border-radius", "clamp(12px, 3vw, 22px)")
                    property("background-image", "radial-gradient(circle at 30% 20%, rgba(77, 173, 112, 0.28), transparent 38%), linear-gradient(180deg, rgba(17, 101, 62, 0.98), rgba(10, 73, 44, 0.98))")
                }
                .toAttrs()
        ) {
            if (shotLineEnd != null) {
                ShotLine(
                    start = setup.objectBall,
                    end = shotLineEnd,
                    fieldWidth = TABLE_WIDTH,
                    fieldHeight = TABLE_HEIGHT,
                )
            }

            pockets.forEach { pocket ->
                val isTarget = pocket == setup.target.pocket
                BallLike(
                    center = pocket.renderCenter,
                    radius = POCKET_RADIUS,
                    fieldWidth = TABLE_WIDTH,
                    fieldHeight = TABLE_HEIGHT,
                    fill = if (isTarget) Color.rgb(239, 195, 79) else Color.rgb(17, 19, 18),
                    borderColor = if (isTarget) Color.rgba(255, 240, 189, 0.9f) else Color.rgba(0, 0, 0, 0.6f),
                    glow = if (isTarget) "0 0 0 3px rgba(255, 231, 157, 0.38), 0 0 24px rgba(242, 201, 76, 0.55)" else "inset 0 6px 12px rgba(255,255,255,0.04)",
                )
            }

            BallLike(
                center = setup.objectBall,
                radius = ballRadius,
                fieldWidth = TABLE_WIDTH,
                fieldHeight = TABLE_HEIGHT,
                fill = Color.rgb(201, 43, 43),
                borderColor = Color.rgba(255, 255, 255, 0.72f),
                glow = "0 10px 20px rgba(0, 0, 0, 0.26), inset 0 8px 14px rgba(255,255,255,0.28)",
            )

            BallLike(
                center = setup.cueBall,
                radius = ballRadius,
                fieldWidth = TABLE_WIDTH,
                fieldHeight = TABLE_HEIGHT,
                fill = Color.rgba(244, 244, 239, CUE_BALL_ALPHA),
                borderColor = Color.rgba(255, 255, 255, 0.92f),
                glow = "0 10px 20px rgba(0, 0, 0, 0.2), inset 0 8px 14px rgba(255,255,255,0.72)",
            )
        }
    }
}

private enum class RailSide {
    Top,
    Right,
    Bottom,
    Left,
}

@Composable
private fun TableDiamond(side: RailSide, fraction: Double) {
    Div(
        attrs = Modifier
            .position(Position.Absolute)
            .backgroundColor(Color.rgba(246, 221, 174, 0.86f))
            .border(1.px, LineStyle.Solid, Color.rgba(104, 64, 24, 0.45f))
            .styleModifier {
                property("width", TABLE_DIAMOND_SIZE)
                property("height", TABLE_DIAMOND_SIZE)
                when (side) {
                    RailSide.Top -> {
                        property("left", "${fraction * 100}%")
                        property("top", "calc($TABLE_RAIL_PADDING / 2)")
                    }
                    RailSide.Bottom -> {
                        property("left", "${fraction * 100}%")
                        property("top", "calc(100% - ($TABLE_RAIL_PADDING / 2))")
                    }
                    RailSide.Left -> {
                        property("left", "calc($TABLE_RAIL_PADDING / 2)")
                        property("top", "${fraction * 100}%")
                    }
                    RailSide.Right -> {
                        property("left", "calc(100% - ($TABLE_RAIL_PADDING / 2))")
                        property("top", "${fraction * 100}%")
                    }
                }
                property("transform", "translate(-50%, -50%) rotate(45deg)")
                property("box-shadow", "0 1px 4px rgba(0, 0, 0, 0.22)")
                property("pointer-events", "none")
            }
            .toAttrs()
    )
}

@Composable
private fun ShotLine(
    start: Point,
    end: Point,
    fieldWidth: Double,
    fieldHeight: Double,
) {
    val lineThickness = 4.0
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = sqrt(dx * dx + dy * dy)
    val angle = atan2(dy, dx) * 180.0 / PI

    Div(
        attrs = Modifier
            .position(Position.Absolute)
            .left((start.x / fieldWidth * 100).percent)
            .top((((start.y - lineThickness / 2.0) / fieldHeight) * 100).percent)
            .width((length / fieldWidth * 100).percent)
            .height(lineThickness.px)
            .backgroundColor(Color.rgba(255, 232, 171, 0.92f))
            .borderRadius(999.px)
            .zIndex(1)
            .styleModifier {
                property("transform", "rotate(${angle}deg)")
                property("transform-origin", "0 50%")
                property("box-shadow", "0 0 12px rgba(255, 226, 139, 0.72)")
            }
            .toAttrs()
    )
}

@Composable
private fun OverlapTrainer(
    overlapOffset: Double,
    perfectOverlap: Double,
    submitted: Boolean,
    dragging: Boolean,
    ballRadius: Double,
) {
    val objectCenter = Point(OVERLAP_WIDTH / 2.0, OVERLAP_HEIGHT / 2.0 + 6.0)
    val cueCenter = Point(objectCenter.x + overlapOffset, objectCenter.y)
    val perfectCenter = Point(objectCenter.x + perfectOverlap, objectCenter.y)

    Div(
        attrs = Modifier
            .fillMaxWidth()
            .maxWidth(840.px)
            .alignSelf(AlignSelf.Center)
            .styleModifier { property("aspect-ratio", "${OVERLAP_WIDTH / OVERLAP_HEIGHT}") }
            .borderRadius(26.px)
            .position(Position.Relative)
            .overflow(Overflow.Hidden)
            .backgroundColor(Color.rgb(18, 36, 49))
            .styleModifier {
                property("background-image", "linear-gradient(180deg, rgba(35,64,82,0.98), rgba(14,28,39,0.98))")
                property("box-shadow", "inset 0 0 0 1px rgba(255,255,255,0.08)")
            }
            .toAttrs()
    ) {
        BallLike(
            center = objectCenter,
            radius = ballRadius,
            fieldWidth = OVERLAP_WIDTH,
            fieldHeight = OVERLAP_HEIGHT,
            fill = Color.rgb(201, 43, 43),
            borderColor = Color.rgba(255, 255, 255, 0.72f),
            glow = "0 12px 24px rgba(0, 0, 0, 0.24), inset 0 10px 16px rgba(255,255,255,0.22)",
            zIndex = 1,
        )

        if (submitted) {
            BallLike(
                center = perfectCenter,
                radius = ballRadius,
                fieldWidth = OVERLAP_WIDTH,
                fieldHeight = OVERLAP_HEIGHT,
                fill = Color.rgba(241, 197, 79, 0.24f),
                borderColor = Color.rgb(241, 197, 79),
                glow = "0 0 0 3px rgba(241,197,79,0.3), inset 0 0 0 2px rgba(255,255,255,0.35)",
                zIndex = 2,
                borderStyle = LineStyle.Dashed,
            )
        }

        Div(
            attrs = Modifier
                .position(Position.Absolute)
                .left((((cueCenter.x - ballRadius) / OVERLAP_WIDTH) * 100).percent)
                .top((((cueCenter.y - ballRadius) / OVERLAP_HEIGHT) * 100).percent)
                .width(((ballRadius * 2.0 / OVERLAP_WIDTH) * 100).percent)
                .styleModifier { property("aspect-ratio", "1") }
                .borderRadius(50.percent)
                .border(2.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.9f))
                .backgroundColor(Color.rgba(244, 244, 239, CUE_BALL_ALPHA))
                .styleModifier {
                    property("box-shadow", "0 12px 24px rgba(0, 0, 0, 0.24), inset 0 10px 18px rgba(255,255,255,0.75)")
                    property("cursor", if (dragging) "grabbing" else "grab")
                }
                .zIndex(3)
                .toAttrs()
        )
    }
}

@Composable
private fun ActionButton(label: String, onActivate: () -> Unit) {
    Button(
        attrs = Modifier
            .padding(leftRight = 1.1.cssRem, topBottom = 0.65.cssRem)
            .borderRadius(14.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.1f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.18f))
            .color(Colors.White)
            .fontWeight(FontWeight.Medium)
            .styleModifier {
                property("cursor", "pointer")
                property("user-select", "none")
                property("touch-action", "manipulation")
            }
            .toAttrs {
                onClick { onActivate() }
                onTouchStart {
                    it.preventDefault()
                    onActivate()
                }
            }
    ) {
        Text(label)
    }
}

@Composable
private fun ResultPanel(
    submitted: Boolean,
    perfectAngle: Double,
    userAngle: Double,
    angleError: Double,
) {
    if (!submitted) {
        P(
            attrs = Modifier
                .margin(0.px)
                .fontSize(0.98.cssRem)
                .lineHeight(1.55)
                .color(Color.rgba(255, 255, 255, 0.68f))
                .toAttrs()
        ) {
            Text("When you check the overlap, the trainer will reveal the correct contact picture and show how many degrees your shot would miss the required cut.")
        }
        return
    }

    val absError = abs(angleError)
    val feedback = when {
        absError < 1 -> "Dead on."
        absError < 7.0 -> "Nice shot!"
        absError < 15.0 -> "Not bad."
        absError < 20 -> "That was a bit off."
        else -> "That was clearly off."
    }

    val directionalMiss = when {
        angleError > 0.15 -> "Your overlap sends the object ball ${angleDirectionLabel(angleError)} of the target line."
        angleError < -0.15 -> "Your overlap sends the object ball ${angleDirectionLabel(angleError)} of the target line."
        else -> "Your overlap sends the object ball on the target line."
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(1.cssRem)
            .borderRadius(20.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.06f))
            .gap(0.45.cssRem)
    ) {
        Span(
            attrs = Modifier
                .fontSize(1.15.cssRem)
                .fontWeight(FontWeight.Medium)
                .color(Colors.White)
                .toAttrs()
        ) {
            Text("${formatDegreesText(absError)} off. $feedback")
        }
        P(
            attrs = Modifier
                .margin(0.px)
                .fontSize(0.98.cssRem)
                .lineHeight(1.6)
                .color(Color.rgba(255, 255, 255, 0.78f))
                .toAttrs()
        ) {
            Text("$directionalMiss Required cut: ${formatSignedDegreesText(perfectAngle)}. Your overlap built: ${formatSignedDegreesText(userAngle)}.")
        }
    }
}

@Composable
private fun StatusChip(label: String, value: String, accent: Color) {
    Row(
        Modifier
            .padding(leftRight = 0.9.cssRem, topBottom = 0.55.cssRem)
            .borderRadius(999.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.06f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.09f))
            .gap(0.55.cssRem)
            .alignItems(AlignItems.Center)
    ) {
        Span(
            attrs = Modifier
                .color(Color.rgba(255, 255, 255, 0.58f))
                .fontSize(0.92.cssRem)
                .toAttrs()
        ) {
            Text(label)
        }
        Span(
            attrs = Modifier
                .color(accent)
                .fontWeight(FontWeight.Medium)
                .fontSize(0.95.cssRem)
                .toAttrs()
        ) {
            Text(value)
        }
    }
}

@Composable
private fun BallLike(
    center: Point,
    radius: Double,
    fieldWidth: Double,
    fieldHeight: Double,
    fill: Color,
    borderColor: Color,
    glow: String,
    zIndex: Int = 0,
    borderStyle: LineStyle = LineStyle.Solid,
) {
    Div(
        attrs = Modifier
            .position(Position.Absolute)
            .left((((center.x - radius) / fieldWidth) * 100).percent)
            .top((((center.y - radius) / fieldHeight) * 100).percent)
            .width(((radius * 2.0 / fieldWidth) * 100).percent)
            .styleModifier { property("aspect-ratio", "1") }
            .borderRadius(50.percent)
            .backgroundColor(fill)
            .border(2.px, borderStyle, borderColor)
            .zIndex(zIndex)
            .styleModifier { property("box-shadow", glow) }
            .toAttrs()
    )
}

private fun calculateShotLineEnd(setup: ShotSetup, cutAngleDegrees: Double): Point {
    val shotDirection = solveObjectBallDirection(setup.cueBall, setup.objectBall, cutAngleDegrees) ?: return setup.objectBall
    return rayToPocketOrTableBounds(setup.objectBall, shotDirection, setup.target.pocket)
}

private fun generateShotSetup(random: Random = Random.Default): ShotSetup {
    repeat(3000) {
        val initialObjectBall = Point(
            x = random.nextDouble(130.0, TABLE_WIDTH - 130.0),
            y = random.nextDouble(110.0, TABLE_HEIGHT - 110.0),
        )
        val initialCueBall = Point(
            x = random.nextDouble(90.0, TABLE_WIDTH - 90.0),
            y = random.nextDouble(90.0, TABLE_HEIGHT - 90.0),
        )

        if (distance(initialCueBall, initialObjectBall) < BALL_RADIUS * 4.0) return@repeat

        val directTopTargets = topPockets.mapNotNull { pocket ->
            evaluateShot(initialCueBall, initialObjectBall, pocket)
        }

        if (directTopTargets.isNotEmpty()) {
            return ShotSetup(
                id = random.nextInt(),
                cueBall = initialCueBall,
                objectBall = initialObjectBall,
                target = directTopTargets.random(random),
            )
        }

        val flippedCueBall = flipVertically(initialCueBall)
        val flippedObjectBall = flipVertically(initialObjectBall)
        val flippedTopTargets = topPockets.mapNotNull { pocket ->
            evaluateShot(flippedCueBall, flippedObjectBall, pocket)
        }

        if (flippedTopTargets.isNotEmpty()) {
            return ShotSetup(
                id = random.nextInt(),
                cueBall = flippedCueBall,
                objectBall = flippedObjectBall,
                target = flippedTopTargets.random(random),
            )
        }
    }

    error("Could not generate a legal shot layout.")
}

private fun evaluateShot(cueBall: Point, objectBall: Point, pocket: Pocket): ShotOption? {
    val objectToPocket = pocket.aimPoint - objectBall

    if (distance(Point(0.0, 0.0), objectToPocket) < BALL_RADIUS * 4.0) return null

    val pocketDirection = normalized(objectToPocket) ?: return null
    val ghostBall = objectBall - pocketDirection * (BALL_RADIUS * 2.0)

    if (!isInsidePlayableArea(ghostBall)) return null

    val cueToGhost = ghostBall - cueBall
    if (distance(Point(0.0, 0.0), cueToGhost) < 1e-6) return null

    val cutAngleDegrees = signedAngleDegrees(cueToGhost, objectToPocket)
    if (cutAngleDegrees <= -90.0 || cutAngleDegrees >= 90.0) return null
    if (abs(cutAngleDegrees) >= MAX_REQUIRED_CUT_ANGLE) return null

    return ShotOption(pocket, cutAngleDegrees)
}

private fun isInsidePlayableArea(point: Point): Boolean {
    val margin = BALL_RADIUS + 10.0
    return point.x in margin..(TABLE_WIDTH - margin) && point.y in margin..(TABLE_HEIGHT - margin)
}

private fun normalized(vector: Point): Point? {
    val length = distance(Point(0.0, 0.0), vector)
    if (length == 0.0) return null
    return Point(vector.x / length, vector.y / length)
}

private fun distance(a: Point, b: Point): Double {
    val dx = b.x - a.x
    val dy = b.y - a.y
    return sqrt(dx * dx + dy * dy)
}

private fun dot(a: Point, b: Point): Double = a.x * b.x + a.y * b.y

private fun signedAngleDegrees(from: Point, to: Point): Double {
    val cross = from.x * to.y - from.y * to.x
    return atan2(cross, dot(from, to)) * 180.0 / PI
}

private fun solveObjectBallDirection(cueBall: Point, objectBall: Point, cutAngleDegrees: Double): Point? {
    val cueToObject = objectBall - cueBall
    val distanceToObject = distance(cueBall, objectBall)
    if (distanceToObject <= 1e-6) return null

    val alphaRadians = cutAngleDegrees * PI / 180.0
    val twoBallRadii = BALL_RADIUS * 2.0
    val lateralOffset = twoBallRadii * sin(alphaRadians)
    val longitudinalSquared = distanceToObject * distanceToObject - lateralOffset * lateralOffset
    if (longitudinalSquared < 0.0) return null

    val ghostDistanceFromCue = -twoBallRadii * cos(alphaRadians) + sqrt(longitudinalSquared)
    val forwardComponent = ghostDistanceFromCue + twoBallRadii * cos(alphaRadians)
    val rotationDegrees = atan2(lateralOffset, forwardComponent) * 180.0 / PI

    val incomingDirection = rotate(normalized(cueToObject) ?: return null, -rotationDegrees)
    return normalized(rotate(incomingDirection, cutAngleDegrees))
}

private fun rotate(vector: Point, angleDegrees: Double): Point {
    val radians = angleDegrees * PI / 180.0
    val cosValue = kotlin.math.cos(radians)
    val sinValue = kotlin.math.sin(radians)
    return Point(
        x = vector.x * cosValue - vector.y * sinValue,
        y = vector.x * sinValue + vector.y * cosValue,
    )
}

private fun rayToTableBounds(origin: Point, direction: Point): Point {
    val candidates = buildList {
        if (direction.x > 0.0) add((TABLE_WIDTH - origin.x) / direction.x)
        if (direction.x < 0.0) add((0.0 - origin.x) / direction.x)
        if (direction.y > 0.0) add((TABLE_HEIGHT - origin.y) / direction.y)
        if (direction.y < 0.0) add((0.0 - origin.y) / direction.y)
    }.filter { it > 0.0 }

    val t = candidates.minOrNull() ?: 0.0
    return Point(
        x = (origin.x + direction.x * t).coerceIn(0.0, TABLE_WIDTH),
        y = (origin.y + direction.y * t).coerceIn(0.0, TABLE_HEIGHT),
    )
}

private fun rayToPocketOrTableBounds(origin: Point, direction: Point, pocket: Pocket): Point {
    val normalizedDirection = normalized(direction) ?: return origin
    return rayToCircle(origin, normalizedDirection, pocket.renderCenter, POCKET_RADIUS) ?: rayToTableBounds(origin, normalizedDirection)
}

private fun rayToCircle(origin: Point, direction: Point, center: Point, radius: Double): Point? {
    val offset = origin - center
    val b = 2.0 * dot(direction, offset)
    val c = dot(offset, offset) - radius * radius
    val discriminant = b * b - 4.0 * c
    if (discriminant < 0.0) return null

    val sqrtDiscriminant = sqrt(discriminant)
    val firstT = (-b - sqrtDiscriminant) / 2.0
    val secondT = (-b + sqrtDiscriminant) / 2.0
    val t = listOf(firstT, secondT).filter { it > 0.0 }.minOrNull() ?: return null
    return origin + direction * t
}

private fun overlapOffsetFromAngle(angleDegrees: Double, ballRadius: Double): Double {
    return -sin(angleDegrees * PI / 180.0) * ballRadius * 2.0
}

private fun overlapOffsetToAngle(offset: Double, ballRadius: Double): Double {
    val ratio = (offset / (ballRadius * 2.0)).coerceIn(-1.0, 1.0)
    return -asin(ratio) * 180.0 / PI
}

private fun angleSideLabel(angleDegrees: Double): String {
    return when {
        angleDegrees > 2.0 -> "Right-side overlap"
        angleDegrees < -2.0 -> "Left-side overlap"
        else -> "Near center-ball"
    }
}

private fun formatDegreesText(value: Double): String = "${(value * 10.0).toInt() / 10.0} deg"

private fun formatSignedDegreesText(value: Double): String {
    val rounded = (value * 10.0).toInt() / 10.0
    return "${if (rounded > 0) "+" else ""}$rounded deg"
}

private fun angleDirectionLabel(angleDegrees: Double): String {
    return if (angleDegrees > 0) "to the right" else "to the left"
}

private fun formatDegrees(value: Double): String = "${(value * 10.0).toInt() / 10.0}°"

private fun formatSignedDegrees(value: Double): String {
    val rounded = (value * 10.0).toInt() / 10.0
    return "${if (rounded > 0) "+" else ""}$rounded°"
}

private fun flipVertically(point: Point): Point = Point(point.x, TABLE_HEIGHT - point.y)

private fun firstTouchClientPosition(event: dynamic): Pair<Double, Double>? {
    val touches = event.touches
    if (touches != null && touches.length as Int > 0) {
        return Pair(
            (touches[0].clientX as Number).toDouble(),
            (touches[0].clientY as Number).toDouble(),
        )
    }
    val changedTouches = event.changedTouches
    if (changedTouches != null && changedTouches.length as Int > 0) {
        return Pair(
            (changedTouches[0].clientX as Number).toDouble(),
            (changedTouches[0].clientY as Number).toDouble(),
        )
    }
    return null
}
