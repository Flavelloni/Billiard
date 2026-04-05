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
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.site.components.layouts.PageLayoutData
import kotlinx.browser.window
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
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
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val TABLE_WIDTH = 1000.0
private const val TABLE_HEIGHT = 560.0
private const val BALL_RADIUS = 20.0
private const val POCKET_RADIUS = 34.0
private const val OVERLAP_WIDTH = 520.0
private const val OVERLAP_HEIGHT = 220.0
private const val OVERLAP_BALL_RADIUS = 40.0

private data class Point(val x: Double, val y: Double) {
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    operator fun minus(other: Point) = Point(x - other.x, y - other.y)
    operator fun times(scale: Double) = Point(x * scale, y * scale)
}

private data class Pocket(val label: String, val center: Point)

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
    Pocket("Top left", Point(0.0, 0.0)),
    Pocket("Top middle", Point(TABLE_WIDTH / 2.0, 0.0)),
    Pocket("Top right", Point(TABLE_WIDTH, 0.0)),
    Pocket("Bottom left", Point(0.0, TABLE_HEIGHT)),
    Pocket("Bottom middle", Point(TABLE_WIDTH / 2.0, TABLE_HEIGHT)),
    Pocket("Bottom right", Point(TABLE_WIDTH, TABLE_HEIGHT)),
)

private val topPockets = pockets.take(3)

@InitRoute
fun initPoolTrainerPage(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("Pool Trainer", "Fractional aiming trainer with legal random potting layouts."))
}

@Page
@Composable
@Layout(".components.layouts.PageLayout")
fun PoolTrainerPage() {
    var setup by remember { mutableStateOf(generateShotSetup()) }
    val perfectOverlap = remember(setup) { overlapOffsetFromAngle(setup.target.cutAngleDegrees, OVERLAP_BALL_RADIUS) }

    var overlapOffset by remember(setup.id) { mutableStateOf(-OVERLAP_BALL_RADIUS * 2.0) }
    var submitted by remember(setup.id) { mutableStateOf(false) }
    var overlapArea by remember { mutableStateOf<HTMLElement?>(null) }
    var dragging by remember(setup.id) { mutableStateOf(false) }

    val userCutAngle = overlapOffsetToAngle(overlapOffset, OVERLAP_BALL_RADIUS)
    val angleError = userCutAngle - setup.target.cutAngleDegrees

    fun updateOverlap(clientX: Double) {
        val rect = overlapArea?.getBoundingClientRect() ?: return
        val localX = ((clientX - rect.left) / rect.width).coerceIn(0.0, 1.0) * OVERLAP_WIDTH
        overlapOffset = (localX - OVERLAP_WIDTH / 2.0).coerceIn(-OVERLAP_BALL_RADIUS * 2.0, OVERLAP_BALL_RADIUS * 2.0)
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
                Text("Pool Potting Trainer")
            }
            P(
                attrs = Modifier
                    .maxWidth(75.cssRem)
                    .margin(0.px)
                    .fontSize(1.05.cssRem)
                    .lineHeight(1.65)
                    .color(Color.rgba(255, 255, 255, 0.74f))
                    .toAttrs()
            ) {
                Text("Study the random table, read the cut from the highlighted pocket, then build the cue-ball overlap below. No guide lines are shown. The side of the overlap matters.")
            }
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
            PoolTable(setup)

            Row(
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

                Row(Modifier.gap(0.75.cssRem).flexWrap(FlexWrap.Wrap)) {
                    Button(
                        onClick = { submitted = true },
                        modifier = Modifier.padding(leftRight = 1.1.cssRem, topBottom = 0.65.cssRem)
                    ) {
                        Text("Check overlap")
                    }
                    Button(
                        onClick = { setup = generateShotSetup() },
                        modifier = Modifier.padding(leftRight = 1.1.cssRem, topBottom = 0.65.cssRem)
                    ) {
                        Text("New layout")
                    }
                }
            }
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
                P(
                    attrs = Modifier
                        .margin(0.px)
                        .fontSize(1.cssRem)
                        .lineHeight(1.6)
                        .color(Color.rgba(255, 255, 255, 0.72f))
                        .toAttrs()
                ) {
                    Text("Click and drag the cue ball horizontally over the object ball to recreate the contact picture for the shot above.")
                }
            }

            Div(
                attrs = Modifier
                    .fillMaxWidth()
                    .styleModifier { property("touch-action", "none") }
                    .toAttrs {
                        ref { element ->
                            overlapArea = element
                            onDispose {
                                if (overlapArea == element) overlapArea = null
                            }
                        }
                        onMouseMove { event ->
                            if (dragging) {
                                event.preventDefault()
                                updateOverlap(event.clientX.toDouble())
                            }
                        }
                        onMouseUp { dragging = false }
                        onMouseLeave { dragging = false }
                    }
            ) {
                OverlapTrainer(
                    overlapOffset = overlapOffset,
                    perfectOverlap = perfectOverlap,
                    submitted = submitted,
                    dragging = dragging,
                    onCueMouseDown = { clientX ->
                        dragging = true
                        updateOverlap(clientX)
                    }
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

    DisposableEffect(dragging) {
        if (!dragging) return@DisposableEffect onDispose { }
        val moveListener: (dynamic) -> Unit = { event ->
            updateOverlap((event.clientX as Number).toDouble())
        }
        val upListener: (dynamic) -> Unit = {
            dragging = false
        }

        window.addEventListener("mousemove", moveListener)
        window.addEventListener("mouseup", upListener)
        onDispose {
            window.removeEventListener("mousemove", moveListener)
            window.removeEventListener("mouseup", upListener)
        }
    }
}

@Composable
private fun PoolTable(setup: ShotSetup) {
    Div(
        attrs = Modifier
            .fillMaxWidth()
            .styleModifier { property("aspect-ratio", "${TABLE_WIDTH / TABLE_HEIGHT}") }
            .borderRadius(32.px)
            .padding(22.px)
            .backgroundColor(Color.rgb(80, 43, 18))
            .position(Position.Relative)
            .overflow(Overflow.Hidden)
            .styleModifier {
                property("box-shadow", "inset 0 0 0 2px rgba(255, 228, 166, 0.18), inset 0 18px 36px rgba(255, 255, 255, 0.06), 0 18px 40px rgba(0, 0, 0, 0.3)")
                property("background-image", "linear-gradient(145deg, rgba(122,76,34,0.92), rgba(67,34,11,0.94))")
            }
            .toAttrs()
    ) {
        Div(
            attrs = Modifier
                .fillMaxSize()
                .borderRadius(22.px)
                .position(Position.Relative)
                .overflow(Overflow.Hidden)
                .backgroundColor(Color.rgb(21, 93, 58))
                .styleModifier {
                    property("background-image", "radial-gradient(circle at 30% 20%, rgba(77, 173, 112, 0.28), transparent 38%), linear-gradient(180deg, rgba(17, 101, 62, 0.98), rgba(10, 73, 44, 0.98))")
                }
                .toAttrs()
        ) {
            pockets.forEach { pocket ->
                val isTarget = pocket == setup.target.pocket
                BallLike(
                    center = pocket.center,
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
                radius = BALL_RADIUS,
                fieldWidth = TABLE_WIDTH,
                fieldHeight = TABLE_HEIGHT,
                fill = Color.rgb(201, 43, 43),
                borderColor = Color.rgba(255, 255, 255, 0.72f),
                glow = "0 10px 20px rgba(0, 0, 0, 0.26), inset 0 8px 14px rgba(255,255,255,0.28)",
            )

            BallLike(
                center = setup.cueBall,
                radius = BALL_RADIUS,
                fieldWidth = TABLE_WIDTH,
                fieldHeight = TABLE_HEIGHT,
                fill = Color.rgb(244, 244, 239),
                borderColor = Color.rgba(255, 255, 255, 0.92f),
                glow = "0 10px 20px rgba(0, 0, 0, 0.2), inset 0 8px 14px rgba(255,255,255,0.72)",
            )
        }
    }
}

@Composable
private fun OverlapTrainer(
    overlapOffset: Double,
    perfectOverlap: Double,
    submitted: Boolean,
    dragging: Boolean,
    onCueMouseDown: (Double) -> Unit,
) {
    val objectCenter = Point(OVERLAP_WIDTH / 2.0, OVERLAP_HEIGHT / 2.0 + 6.0)
    val cueCenter = Point(objectCenter.x + overlapOffset, objectCenter.y)
    val perfectCenter = Point(objectCenter.x + perfectOverlap, objectCenter.y)

    Div(
        attrs = Modifier
            .fillMaxWidth()
            .maxWidth(720.px)
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
            radius = OVERLAP_BALL_RADIUS,
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
                radius = OVERLAP_BALL_RADIUS,
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
                .left((((cueCenter.x - OVERLAP_BALL_RADIUS) / OVERLAP_WIDTH) * 100).percent)
                .top((((cueCenter.y - OVERLAP_BALL_RADIUS) / OVERLAP_HEIGHT) * 100).percent)
                .width(((OVERLAP_BALL_RADIUS * 2.0 / OVERLAP_WIDTH) * 100).percent)
                .styleModifier { property("aspect-ratio", "1") }
                .borderRadius(50.percent)
                .border(2.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.9f))
                .backgroundColor(if (submitted) Color.rgba(244, 244, 239, 0.82f) else Color.rgb(244, 244, 239))
                .styleModifier {
                    property("box-shadow", "0 12px 24px rgba(0, 0, 0, 0.24), inset 0 10px 18px rgba(255,255,255,0.75)")
                    property("cursor", if (dragging) "grabbing" else "grab")
                }
                .zIndex(3)
                .toAttrs {
                    onMouseDown { event ->
                        event.preventDefault()
                        onCueMouseDown(event.clientX.toDouble())
                    }
                }
        )
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
        absError < 0.5 -> "Dead on."
        absError < 2.0 -> "Very close."
        absError < 5.0 -> "Potting line was there, but the overlap drifted."
        else -> "The overlap side or amount was clearly off."
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
            Text("${formatDegrees(absError)} off. $feedback")
        }
        P(
            attrs = Modifier
                .margin(0.px)
                .fontSize(0.98.cssRem)
                .lineHeight(1.6)
                .color(Color.rgba(255, 255, 255, 0.78f))
                .toAttrs()
        ) {
            Text("$directionalMiss Required cut: ${formatSignedDegrees(perfectAngle)}. Your overlap built: ${formatSignedDegrees(userAngle)}.")
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
    val cueToObject = objectBall - cueBall
    val objectToPocket = pocket.center - objectBall

    if (distance(Point(0.0, 0.0), objectToPocket) < BALL_RADIUS * 4.0) return null

    val pocketDirection = normalized(objectToPocket) ?: return null
    val ghostBall = objectBall - pocketDirection * (BALL_RADIUS * 2.0)

    if (!isInsidePlayableArea(ghostBall)) return null

    val cutAngleDegrees = signedAngleDegrees(cueToObject, objectToPocket)
    if (cutAngleDegrees <= -90.0 || cutAngleDegrees >= 90.0) return null

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

private fun signedAngleDegrees(from: Point, to: Point): Double {
    val cross = from.x * to.y - from.y * to.x
    val dot = from.x * to.x + from.y * to.y
    return atan2(cross, dot) * 180.0 / PI
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

private fun angleDirectionLabel(angleDegrees: Double): String {
    return if (angleDegrees > 0) "to the right" else "to the left"
}

private fun formatDegrees(value: Double): String = "${(value * 10.0).toInt() / 10.0}°"

private fun formatSignedDegrees(value: Double): String {
    val rounded = (value * 10.0).toInt() / 10.0
    return "${if (rounded > 0) "+" else ""}$rounded°"
}

private fun flipVertically(point: Point): Point = Point(point.x, TABLE_HEIGHT - point.y)
