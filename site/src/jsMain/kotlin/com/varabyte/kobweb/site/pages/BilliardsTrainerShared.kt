package com.varabyte.kobweb.site.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.compose.css.AlignSelf
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.alignSelf
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.border
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.flexWrap
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.fontWeight
import com.varabyte.kobweb.compose.ui.modifiers.gap
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.left
import com.varabyte.kobweb.compose.ui.modifiers.lineHeight
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.maxWidth
import com.varabyte.kobweb.compose.ui.modifiers.overflow
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.position
import com.varabyte.kobweb.compose.ui.modifiers.top
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.modifiers.zIndex
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.navigation.Anchor
import com.varabyte.kobweb.silk.components.text.SpanText
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
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
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
private const val CROPPED_VERTICAL_WIDTH = TABLE_HEIGHT
private const val CROPPED_VERTICAL_HEIGHT = TABLE_WIDTH * 2.0 / 3.0
private const val CROPPED_LOWEST_DIAMOND_FRACTION = 4.0 / 5.0
private const val CROPPED_SIDE_POCKET_Y = CROPPED_VERTICAL_HEIGHT * CROPPED_LOWEST_DIAMOND_FRACTION
private const val BALL_RADIUS = 20.0
private const val MOBILE_TABLE_BALL_RADIUS = 24.0
private const val CORNER_POCKET_RADIUS = 42.0
private const val SIDE_POCKET_RADIUS = 34.0
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
private const val TABLE_BORDER_RADIUS = "clamp(18px, 4vw, 32px)"
private const val TABLE_INNER_RADIUS = "clamp(12px, 3vw, 22px)"
private const val CROPPED_TABLE_RADIUS = "clamp(18px, 4vw, 32px) clamp(18px, 4vw, 32px) 0 0"
private const val CROPPED_INNER_RADIUS = "clamp(12px, 3vw, 22px) clamp(12px, 3vw, 22px) 0 0"
private const val CUSHION_INSET = 18.0
private const val CUSHION_THICKNESS = 1.0

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

private data class SpawnZone(
    val xRange: ClosedFloatingPointRange<Double>,
    val yRange: ClosedFloatingPointRange<Double>,
)

private data class TableSpec(
    val width: Double,
    val height: Double,
    val pockets: List<Pocket>,
    val targetPockets: List<Pocket>,
    val cueZone: SpawnZone,
    val objectZone: SpawnZone,
    val horizontalRailFractions: List<Double>,
    val verticalRailFractions: List<Double>,
    val sharedXRange: ClosedFloatingPointRange<Double>? = null,
    val allowVerticalMirror: Boolean = false,
    val outerPaddingCss: String = TABLE_RAIL_PADDING,
    val outerBorderRadiusCss: String = TABLE_BORDER_RADIUS,
    val innerBorderRadiusCss: String = TABLE_INNER_RADIUS,
    val showBottomRail: Boolean = true,
    val showBottomCushion: Boolean = true,
)

private data class TrainerVariant(
    val label: String,
    val heading: String,
    val description: String,
    val tableSpec: TableSpec,
)

private data class TrainerLink(
    val label: String,
    val href: String,
)

private enum class RailSide {
    Top,
    Right,
    Bottom,
    Left,
}

private val fullTopLeftPocket = Pocket("Top left", Point(0.0, 0.0), Point(CORNER_POCKET_TARGET_INSET, CORNER_POCKET_TARGET_INSET))
private val fullTopMiddlePocket = Pocket("Top middle", Point(TABLE_WIDTH / 2.0, 0.0), Point(TABLE_WIDTH / 2.0, SIDE_POCKET_TARGET_INSET))
private val fullTopRightPocket = Pocket("Top right", Point(TABLE_WIDTH, 0.0), Point(TABLE_WIDTH - CORNER_POCKET_TARGET_INSET, CORNER_POCKET_TARGET_INSET))
private val fullBottomLeftPocket = Pocket("Bottom left", Point(0.0, TABLE_HEIGHT), Point(CORNER_POCKET_TARGET_INSET, TABLE_HEIGHT - CORNER_POCKET_TARGET_INSET))
private val fullBottomMiddlePocket = Pocket("Bottom middle", Point(TABLE_WIDTH / 2.0, TABLE_HEIGHT), Point(TABLE_WIDTH / 2.0, TABLE_HEIGHT - SIDE_POCKET_TARGET_INSET))
private val fullBottomRightPocket = Pocket("Bottom right", Point(TABLE_WIDTH, TABLE_HEIGHT), Point(TABLE_WIDTH - CORNER_POCKET_TARGET_INSET, TABLE_HEIGHT - CORNER_POCKET_TARGET_INSET))

private val croppedTopLeftPocket = Pocket("Top left", Point(0.0, 0.0), Point(CORNER_POCKET_TARGET_INSET, CORNER_POCKET_TARGET_INSET))
private val croppedTopRightPocket = Pocket("Top right", Point(CROPPED_VERTICAL_WIDTH, 0.0), Point(CROPPED_VERTICAL_WIDTH - CORNER_POCKET_TARGET_INSET, CORNER_POCKET_TARGET_INSET))
private val croppedLeftSidePocket = Pocket("Left side", Point(0.0, CROPPED_SIDE_POCKET_Y), Point(SIDE_POCKET_TARGET_INSET, CROPPED_SIDE_POCKET_Y))
private val croppedRightSidePocket = Pocket("Right side", Point(CROPPED_VERTICAL_WIDTH, CROPPED_SIDE_POCKET_Y), Point(CROPPED_VERTICAL_WIDTH - SIDE_POCKET_TARGET_INSET, CROPPED_SIDE_POCKET_Y))

private val fullTablePockets = listOf(
    fullTopLeftPocket,
    fullTopMiddlePocket,
    fullTopRightPocket,
    fullBottomLeftPocket,
    fullBottomMiddlePocket,
    fullBottomRightPocket,
)

private val croppedVisiblePockets = listOf(
    croppedTopLeftPocket,
    croppedTopRightPocket,
    croppedLeftSidePocket,
    croppedRightSidePocket,
)

private val topPockets = listOf(fullTopLeftPocket, fullTopMiddlePocket, fullTopRightPocket)
private val croppedTargetPockets = listOf(croppedTopLeftPocket, croppedTopRightPocket)
private val longRailDiamondFractions = listOf(1, 2, 3, 5, 6, 7).map { it / 8.0 }
private val shortRailDiamondFractions = (1..3).map { it / 4.0 }
private val croppedVerticalRailFractions = listOf(1.0 / 5.0, 2.0 / 5.0, 3.0 / 5.0)
private val trainerLinks = listOf(
    TrainerLink("Billard", "/pool-trainer"),
    TrainerLink("Billiard2", "/billiard2"),
)

private val classicTableSpec = TableSpec(
    width = TABLE_WIDTH,
    height = TABLE_HEIGHT,
    pockets = fullTablePockets,
    targetPockets = topPockets,
    cueZone = SpawnZone(90.0..(TABLE_WIDTH - 90.0), 90.0..(TABLE_HEIGHT - 90.0)),
    objectZone = SpawnZone(130.0..(TABLE_WIDTH - 130.0), 110.0..(TABLE_HEIGHT - 110.0)),
    horizontalRailFractions = longRailDiamondFractions,
    verticalRailFractions = shortRailDiamondFractions,
    allowVerticalMirror = true,
)

private val croppedTableSpec = TableSpec(
    width = CROPPED_VERTICAL_WIDTH,
    height = CROPPED_VERTICAL_HEIGHT,
    pockets = croppedVisiblePockets,
    targetPockets = croppedTargetPockets,
    cueZone = SpawnZone(150.0..410.0, (CROPPED_VERTICAL_HEIGHT / 2.0 + 44.0)..(CROPPED_VERTICAL_HEIGHT - 92.0)),
    objectZone = SpawnZone(150.0..410.0, 84.0..(CROPPED_VERTICAL_HEIGHT / 2.0 - 44.0)),
    horizontalRailFractions = shortRailDiamondFractions,
    verticalRailFractions = croppedVerticalRailFractions,
    sharedXRange = 150.0..410.0,
    outerPaddingCss = "$TABLE_RAIL_PADDING $TABLE_RAIL_PADDING 0 $TABLE_RAIL_PADDING",
    outerBorderRadiusCss = CROPPED_TABLE_RADIUS,
    innerBorderRadiusCss = CROPPED_INNER_RADIUS,
    showBottomRail = false,
    showBottomCushion = false,
)

private val classicVariant = TrainerVariant(
    label = "Billard",
    heading = "Billiards Trainer",
    description = "Study the random layout, read the cut from the highlighted pocket, then recreate the cue-ball overlap below.",
    tableSpec = classicTableSpec,
)

private val billiard2Variant = TrainerVariant(
    label = "Billiard2",
    heading = "Billiard2",
    description = "Vertical two-thirds layout with top corner targets, visible side pockets, no bottom rail, and vertically aligned ball positions.",
    tableSpec = croppedTableSpec,
)

@Composable
fun PoolTrainerScreen() {
    BilliardsTrainerScreen(classicVariant)
}

@Composable
fun Billiard2Screen() {
    BilliardsTrainerScreen(billiard2Variant)
}

@Composable
private fun BilliardsTrainerScreen(variant: TrainerVariant) {
    val isMobile = rememberIsMobileLayout()
    val tableBallRadius = if (isMobile) MOBILE_TABLE_BALL_RADIUS else BALL_RADIUS
    val overlapBallRadius = if (isMobile) MOBILE_OVERLAP_BALL_RADIUS else OVERLAP_BALL_RADIUS
    var setup by remember(variant.label) { mutableStateOf(generateShotSetup(variant.tableSpec)) }
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
                Text(variant.heading)
            }
            SpanText(
                variant.description,
                Modifier.maxWidth(75.cssRem).siteText(SiteTextSize.NORMAL),
                MutedSpanTextVariant
            )
            TrainerModeTabs(activeLabel = variant.label)
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
                tableSpec = variant.tableSpec,
            )
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
            key(setup.id, submitted, variant.label) {
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

            Row(Modifier.gap(0.75.cssRem).flexWrap(FlexWrap.Wrap)) {
                ActionButton("Check overlap") { submitted = true }
                ActionButton("New layout") { setup = generateShotSetup(variant.tableSpec) }
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
private fun TrainerModeTabs(activeLabel: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .flexWrap(FlexWrap.Wrap)
            .gap(0.65.cssRem)
    ) {
        trainerLinks.forEach { link ->
            val isActive = link.label == activeLabel
            Anchor(
                href = link.href,
                attrs = Modifier
                    .padding(leftRight = 0.9.cssRem, topBottom = 0.5.cssRem)
                    .borderRadius(999.px)
                    .backgroundColor(if (isActive) Color.rgba(241, 197, 79, 0.18f) else Color.rgba(255, 255, 255, 0.06f))
                    .border(
                        1.px,
                        LineStyle.Solid,
                        if (isActive) Color.rgba(241, 197, 79, 0.55f) else Color.rgba(255, 255, 255, 0.1f)
                    )
                    .color(if (isActive) Color.rgb(246, 217, 120) else Color.rgba(255, 255, 255, 0.78f))
                    .styleModifier { property("text-decoration", "none") }
                    .toAttrs()
            ) {
                Text(link.label)
            }
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
private fun PoolTable(
    setup: ShotSetup,
    shownCutAngle: Double?,
    ballRadius: Double,
    tableSpec: TableSpec,
) {
    val shotLineEnd = shownCutAngle?.let { calculateShotLineEnd(setup, it, tableSpec) }
    Div(
        attrs = Modifier
            .fillMaxWidth()
            .styleModifier { property("aspect-ratio", "${tableSpec.width / tableSpec.height}") }
            .backgroundColor(Color.rgb(80, 43, 18))
            .position(Position.Relative)
            .overflow(Overflow.Hidden)
            .styleModifier {
                property("padding", tableSpec.outerPaddingCss)
                property("border-radius", tableSpec.outerBorderRadiusCss)
                property("box-shadow", "inset 0 0 0 2px rgba(255, 228, 166, 0.18), inset 0 18px 36px rgba(255, 255, 255, 0.06), 0 18px 40px rgba(0, 0, 0, 0.3)")
                property("background-image", "linear-gradient(145deg, rgba(122,76,34,0.92), rgba(67,34,11,0.94))")
            }
            .toAttrs()
    ) {
        tableSpec.horizontalRailFractions.forEach { fraction ->
            TableDiamond(RailSide.Top, fraction)
            if (tableSpec.showBottomRail) {
                TableDiamond(RailSide.Bottom, fraction)
            }
        }
        tableSpec.verticalRailFractions.forEach { fraction ->
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
                    property("border-radius", tableSpec.innerBorderRadiusCss)
                    property("background-image", "radial-gradient(circle at 30% 20%, rgba(77, 173, 112, 0.28), transparent 38%), linear-gradient(180deg, rgba(17, 101, 62, 0.98), rgba(10, 73, 44, 0.98))")
                }
                .toAttrs()
        ) {
            CushionLine(RailSide.Top, tableSpec.width, tableSpec.height)
            CushionLine(RailSide.Left, tableSpec.width, tableSpec.height)
            CushionLine(RailSide.Right, tableSpec.width, tableSpec.height)
            if (tableSpec.showBottomCushion) {
                CushionLine(RailSide.Bottom, tableSpec.width, tableSpec.height)
            }

            if (shotLineEnd != null) {
                ShotLine(
                    start = setup.objectBall,
                    end = shotLineEnd,
                    fieldWidth = tableSpec.width,
                    fieldHeight = tableSpec.height,
                )
            }

            tableSpec.pockets.forEach { pocket ->
                val isTarget = pocket == setup.target.pocket
                val pocketRadius = when (pocket.label) {
                    "Top left", "Top right", "Bottom left", "Bottom right" -> CORNER_POCKET_RADIUS
                    else -> SIDE_POCKET_RADIUS
                }
                BallLike(
                    center = pocket.renderCenter,
                    radius = pocketRadius,
                    fieldWidth = tableSpec.width,
                    fieldHeight = tableSpec.height,
                    fill = if (isTarget) Color.rgb(239, 195, 79) else Color.rgb(17, 19, 18),
                    borderColor = if (isTarget) Color.rgba(255, 240, 189, 0.9f) else Color.rgba(0, 0, 0, 0.6f),
                    glow = if (isTarget) "0 0 0 3px rgba(255, 231, 157, 0.38), 0 0 24px rgba(242, 201, 76, 0.55)" else "inset 0 6px 12px rgba(255,255,255,0.04)",
                    zIndex = 2,
                )
            }

            BallLike(
                center = setup.objectBall,
                radius = ballRadius,
                fieldWidth = tableSpec.width,
                fieldHeight = tableSpec.height,
                fill = Color.rgb(201, 43, 43),
                borderColor = Color.rgba(255, 255, 255, 0.72f),
                glow = "0 10px 20px rgba(0, 0, 0, 0.26), inset 0 8px 14px rgba(255,255,255,0.28)",
                zIndex = 3,
            )

            BallLike(
                center = setup.cueBall,
                radius = ballRadius,
                fieldWidth = tableSpec.width,
                fieldHeight = tableSpec.height,
                fill = Color.rgba(244, 244, 239, CUE_BALL_ALPHA),
                borderColor = Color.rgba(255, 255, 255, 0.92f),
                glow = "0 10px 20px rgba(0, 0, 0, 0.2), inset 0 8px 14px rgba(255,255,255,0.72)",
                zIndex = 3,
            )
        }
    }
}

@Composable
private fun CushionLine(side: RailSide, fieldWidth: Double, fieldHeight: Double) {
    val inset = CUSHION_INSET
    val thickness = CUSHION_THICKNESS
    Div(
        attrs = Modifier
            .position(Position.Absolute)
            .backgroundColor(Color.rgba(0, 0, 0, 0.26f))
            .zIndex(0)
            .styleModifier {
                when (side) {
                    RailSide.Top -> {
                        property("left", "${(inset / fieldWidth) * 100}%")
                        property("top", "${(inset / fieldHeight) * 100}%")
                        property("width", "${((fieldWidth - inset * 2.0) / fieldWidth) * 100}%")
                        property("height", "${(thickness / fieldHeight) * 100}%")
                    }
                    RailSide.Bottom -> {
                        property("left", "${(inset / fieldWidth) * 100}%")
                        property("top", "${((fieldHeight - inset - thickness) / fieldHeight) * 100}%")
                        property("width", "${((fieldWidth - inset * 2.0) / fieldWidth) * 100}%")
                        property("height", "${(thickness / fieldHeight) * 100}%")
                    }
                    RailSide.Left -> {
                        property("left", "${(inset / fieldWidth) * 100}%")
                        property("top", "${(inset / fieldHeight) * 100}%")
                        property("width", "${(thickness / fieldWidth) * 100}%")
                        property("height", "${((fieldHeight - inset * 2.0) / fieldHeight) * 100}%")
                    }
                    RailSide.Right -> {
                        property("left", "${((fieldWidth - inset - thickness) / fieldWidth) * 100}%")
                        property("top", "${(inset / fieldHeight) * 100}%")
                        property("width", "${(thickness / fieldWidth) * 100}%")
                        property("height", "${((fieldHeight - inset * 2.0) / fieldHeight) * 100}%")
                    }
                }
                property("pointer-events", "none")
            }
            .toAttrs()
    )
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
    lineThickness: Double = 4.0,
) {
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
        absError < 2.0 -> "Dead on."
        absError < 7.0 -> "Nice shot!"
        absError < 15.0 -> "Not bad."
        absError < 20.0 -> "That was a bit off."
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

private fun calculateShotLineEnd(setup: ShotSetup, cutAngleDegrees: Double, tableSpec: TableSpec): Point {
    val shotDirection = solveObjectBallDirection(setup.cueBall, setup.objectBall, cutAngleDegrees) ?: return setup.objectBall
    return rayToPocketOrTableBounds(setup.objectBall, shotDirection, setup.target.pocket, tableSpec)
}

private fun generateShotSetup(tableSpec: TableSpec, random: Random = Random.Default): ShotSetup {
    repeat(12000) {
        val (initialCueBall, initialObjectBall) = generateBallPositions(tableSpec, random)

        if (distance(initialCueBall, initialObjectBall) < BALL_RADIUS * 4.0) return@repeat

        val directTargets = tableSpec.targetPockets.mapNotNull { pocket ->
            evaluateShot(initialCueBall, initialObjectBall, pocket, tableSpec)
        }

        if (directTargets.isNotEmpty()) {
            return ShotSetup(
                id = random.nextInt(),
                cueBall = initialCueBall,
                objectBall = initialObjectBall,
                target = directTargets.random(random),
            )
        }

        if (tableSpec.allowVerticalMirror) {
            val flippedCueBall = flipVertically(initialCueBall, tableSpec.height)
            val flippedObjectBall = flipVertically(initialObjectBall, tableSpec.height)
            val flippedTargets = tableSpec.targetPockets.mapNotNull { pocket ->
                evaluateShot(flippedCueBall, flippedObjectBall, pocket, tableSpec)
            }

            if (flippedTargets.isNotEmpty()) {
                return ShotSetup(
                    id = random.nextInt(),
                    cueBall = flippedCueBall,
                    objectBall = flippedObjectBall,
                    target = flippedTargets.random(random),
                )
            }
        }
    }

    error("Could not generate a legal shot layout.")
}

private fun generateBallPositions(tableSpec: TableSpec, random: Random): Pair<Point, Point> {
    val sharedX = tableSpec.sharedXRange?.let { random.nextDouble(it.start, it.endInclusive) }
    val cueBall = Point(
        x = sharedX ?: random.nextDouble(tableSpec.cueZone.xRange.start, tableSpec.cueZone.xRange.endInclusive),
        y = random.nextDouble(tableSpec.cueZone.yRange.start, tableSpec.cueZone.yRange.endInclusive),
    )
    val objectBall = Point(
        x = sharedX ?: random.nextDouble(tableSpec.objectZone.xRange.start, tableSpec.objectZone.xRange.endInclusive),
        y = random.nextDouble(tableSpec.objectZone.yRange.start, tableSpec.objectZone.yRange.endInclusive),
    )
    return cueBall to objectBall
}

private fun evaluateShot(cueBall: Point, objectBall: Point, pocket: Pocket, tableSpec: TableSpec): ShotOption? {
    val objectToPocket = pocket.aimPoint - objectBall

    if (distance(Point(0.0, 0.0), objectToPocket) < BALL_RADIUS * 4.0) return null

    val pocketDirection = normalized(objectToPocket) ?: return null
    val ghostBall = objectBall - pocketDirection * (BALL_RADIUS * 2.0)

    if (!isInsidePlayableArea(ghostBall, tableSpec)) return null

    val cueToGhost = ghostBall - cueBall
    if (distance(Point(0.0, 0.0), cueToGhost) < 1e-6) return null

    val cutAngleDegrees = signedAngleDegrees(cueToGhost, objectToPocket)
    if (cutAngleDegrees <= -90.0 || cutAngleDegrees >= 90.0) return null
    if (abs(cutAngleDegrees) >= MAX_REQUIRED_CUT_ANGLE) return null

    return ShotOption(pocket, cutAngleDegrees)
}

private fun isInsidePlayableArea(point: Point, tableSpec: TableSpec): Boolean {
    val margin = BALL_RADIUS + 10.0
    return point.x in margin..(tableSpec.width - margin) && point.y in margin..(tableSpec.height - margin)
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
    val cosValue = cos(radians)
    val sinValue = sin(radians)
    return Point(
        x = vector.x * cosValue - vector.y * sinValue,
        y = vector.x * sinValue + vector.y * cosValue,
    )
}

private fun rayToTableBounds(origin: Point, direction: Point, tableSpec: TableSpec): Point {
    val candidates = buildList {
        if (direction.x > 0.0) add((tableSpec.width - origin.x) / direction.x)
        if (direction.x < 0.0) add((0.0 - origin.x) / direction.x)
        if (direction.y > 0.0) add((tableSpec.height - origin.y) / direction.y)
        if (direction.y < 0.0) add((0.0 - origin.y) / direction.y)
    }.filter { it > 0.0 }

    val t = candidates.minOrNull() ?: 0.0
    return Point(
        x = (origin.x + direction.x * t).coerceIn(0.0, tableSpec.width),
        y = (origin.y + direction.y * t).coerceIn(0.0, tableSpec.height),
    )
}

private fun rayToPocketOrTableBounds(origin: Point, direction: Point, pocket: Pocket, tableSpec: TableSpec): Point {
    val normalizedDirection = normalized(direction) ?: return origin
    val pocketRadius = when (pocket.label) {
        "Top left", "Top right", "Bottom left", "Bottom right" -> CORNER_POCKET_RADIUS
        else -> SIDE_POCKET_RADIUS
    }
    return rayToCircle(origin, normalizedDirection, pocket.renderCenter, pocketRadius)
        ?: rayToTableBounds(origin, normalizedDirection, tableSpec)
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

private fun formatDegreesText(value: Double): String = "${(value * 10.0).toInt() / 10.0} deg"

private fun formatSignedDegreesText(value: Double): String {
    val rounded = (value * 10.0).toInt() / 10.0
    return "${if (rounded > 0) "+" else ""}$rounded deg"
}

private fun angleDirectionLabel(angleDegrees: Double): String {
    return if (angleDegrees > 0) "to the right" else "to the left"
}

private fun flipVertically(point: Point, height: Double): Point = Point(point.x, height - point.y)

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
