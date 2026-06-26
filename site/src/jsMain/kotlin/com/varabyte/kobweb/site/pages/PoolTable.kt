package com.varabyte.kobweb.site.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.Overflow
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
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.data.add
import com.varabyte.kobweb.core.init.InitRoute
import com.varabyte.kobweb.core.init.InitRouteContext
import com.varabyte.kobweb.core.layout.Layout
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.site.components.layouts.PageLayoutData
import com.varabyte.kobweb.site.components.widgets.CueBall
import com.varabyte.kobweb.site.components.widgets.PoolBall
import kotlinx.browser.document
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
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLAnchorElement
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.round
import kotlin.math.sqrt

private const val LONG_PRESS_MS = 550
private const val TABLE_HEIGHT_TO_WIDTH_RATIO = 1.0 / 0.5625

private data class TableBall(val id: Int, val xPercent: Double, val yPercent: Double)
private data class TrajectoryPoint(val order: Int, val xPercent: Double, val yPercent: Double)

private enum class TrajectoryKind {
    CueBall,
    ObjectBall,
}

private data class Hotspot(
    val id: String,
    val label: String,
    val leftPercent: Double,
    val topPercent: Double,
    val widthPercent: Double,
    val heightPercent: Double,
)

private enum class ArrowDirection {
    Down,
    Up,
    Right,
    Left,
}

private data class RailHotspot(
    val id: String,
    val label: String,
    val leftPercent: Double,
    val topPercent: Double,
    val widthPercent: Double,
    val heightPercent: Double,
    val arrowLeftPercent: Double,
    val arrowTopPercent: Double,
    val arrowDirection: ArrowDirection,
)

private val ballIds = (0..15).toList()

private val pocketHotspots = listOf(
    Hotspot("top_left", "Top left corner", 8.4, 8.4, 10.5, 10.5),
    Hotspot("top_right", "Top right corner", 91.6, 8.4, 10.5, 10.5),
    Hotspot("bottom_left", "Bottom left corner", 8.4, 91.6, 10.5, 10.5),
    Hotspot("bottom_right", "Bottom right corner", 91.6, 91.6, 10.5, 10.5),
    Hotspot("left_side", "Left side pocket", 8.4, 50.0, 10.0, 9.0),
    Hotspot("right_side", "Right side pocket", 91.6, 50.0, 10.0, 9.0),
)

private val railHotspots = listOf(
    RailHotspot("top", "Top short rail", 18.0, 3.2, 64.0, 9.0, 50.0, -3.4, ArrowDirection.Down),
    RailHotspot("bottom", "Bottom short rail", 18.0, 87.8, 64.0, 9.0, 50.0, 103.4, ArrowDirection.Up),
    RailHotspot("left_upper", "Left upper long rail", 2.8, 16.0, 9.0, 28.0, -3.4, 29.0, ArrowDirection.Right),
    RailHotspot("left_lower", "Left lower long rail", 2.8, 56.0, 9.0, 28.0, -3.4, 71.0, ArrowDirection.Right),
    RailHotspot("right_upper", "Right upper long rail", 88.2, 16.0, 9.0, 28.0, 103.4, 29.0, ArrowDirection.Left),
    RailHotspot("right_lower", "Right lower long rail", 88.2, 56.0, 9.0, 28.0, 103.4, 71.0, ArrowDirection.Left),
)

@InitRoute
fun initPoolTablePage(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("Pool Table", "Interactive vertical pool table setup editor."))
}

@Page
@Composable
@Layout(".components.layouts.PageLayout")
fun PoolTablePage() {
    var selectedBallId by remember { mutableStateOf<Int?>(null) }
    var placedBalls by remember { mutableStateOf(emptyList<TableBall>()) }
    var highlightedBallId by remember { mutableStateOf<Int?>(null) }
    var selectedPockets by remember { mutableStateOf(emptySet<String>()) }
    var selectedRails by remember { mutableStateOf(emptySet<String>()) }
    var recordingTrajectoryKind by remember { mutableStateOf<TrajectoryKind?>(null) }
    var cueBallTrajectoryPoints by remember { mutableStateOf(emptyList<TrajectoryPoint>()) }
    var objectBallTrajectoryPoints by remember { mutableStateOf(emptyList<TrajectoryPoint>()) }
    var tableElement by remember { mutableStateOf<HTMLElement?>(null) }

    val availableBallIds = ballIds.filterNot { id -> placedBalls.any { it.id == id } }
    val selectedBallIsAvailable = selectedBallId in availableBallIds

    Column(
        Modifier
            .fillMaxWidth()
            .padding(leftRight = 1.25.cssRem, top = 2.cssRem, bottom = 3.cssRem)
            .gap(1.15.cssRem),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .maxWidth(760.px)
                .gap(0.45.cssRem),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            H1(
                attrs = Modifier
                    .margin(0.px)
                    .fontSize(2.2.cssRem)
                    .lineHeight(1.08)
                    .fontWeight(FontWeight.Bold)
                    .color(Colors.White)
                    .toAttrs()
            ) {
                Text("Pool Table Setup")
            }
        }

        BallTray(
            availableBallIds = availableBallIds,
            selectedBallId = selectedBallId.takeIf { selectedBallIsAvailable },
            onSelectBall = { selectedBallId = it },
        )

        Div(
            attrs = Modifier
                .fillMaxWidth()
                .maxWidth(840.px)
                .padding(0.6.cssRem)
                .borderRadius(30.px)
                .backgroundColor(Color.rgb(9, 11, 18))
                .styleModifier {
                    property("box-shadow", "0 34px 100px rgba(0,0,0,0.52), inset 0 0 0 1px rgba(255,255,255,0.08)")
                }
                .toAttrs()
        ) {
            Div(
                attrs = Modifier
                    .position(Position.Relative)
                    .fillMaxWidth()
                    .borderRadius(22.px)
                    .overflow(Overflow.Hidden)
                    .styleModifier {
                        property("aspect-ratio", "0.5625")
                        property("touch-action", "manipulation")
                        property("background", "#050713")
                    }
                    .toAttrs {
                        ref { element ->
                            tableElement = element
                            onDispose {
                                if (tableElement == element) tableElement = null
                            }
                        }
                        onClick { event ->
                            val ballId = selectedBallId ?: return@onClick
                            if (ballId !in availableBallIds) return@onClick
                            val position = tableClickPosition(
                                element = tableElement,
                                clientX = event.clientX.toDouble(),
                                clientY = event.clientY.toDouble(),
                            ) ?: return@onClick

                            placedBalls = placedBalls + TableBall(ballId, position.first, position.second)
                            selectedBallId = null
                        }
                    }
            ) {
                VerticalTableImage()
                PocketOverlays(selectedPockets) { pocketId ->
                    selectedPockets = toggleValue(selectedPockets, pocketId)
                }
                RailOverlays(selectedRails) { railId ->
                    selectedRails = toggleValue(selectedRails, railId)
                }
                RailSelectionArrows(selectedRails)
                TrajectoryLayer(
                    points = cueBallTrajectoryPoints,
                    showLines = recordingTrajectoryKind != TrajectoryKind.CueBall,
                )
                TrajectoryLayer(
                    points = objectBallTrajectoryPoints,
                    showLines = recordingTrajectoryKind != TrajectoryKind.ObjectBall,
                )
                PlacedBallLayer(
                    placedBalls = placedBalls,
                    highlightedBallId = highlightedBallId,
                    onLongPress = { ballId ->
                        highlightedBallId = ballId
                    },
                    onDoubleClick = { ballId ->
                        placedBalls = placedBalls.filterNot { it.id == ballId }
                        if (highlightedBallId == ballId) {
                            highlightedBallId = null
                        }
                    },
                )
                if (recordingTrajectoryKind != null) {
                    TrajectoryRecordingSurface(
                        tableElement = tableElement,
                        onRecordPoint = { xPercent, yPercent ->
                            recordingTrajectoryKind?.let { kind ->
                            when (kind) {
                                TrajectoryKind.CueBall -> {
                                    cueBallTrajectoryPoints = cueBallTrajectoryPoints + TrajectoryPoint(
                                        order = cueBallTrajectoryPoints.size + 1,
                                        xPercent = xPercent,
                                        yPercent = yPercent,
                                    )
                                }
                                TrajectoryKind.ObjectBall -> {
                                    objectBallTrajectoryPoints = objectBallTrajectoryPoints + TrajectoryPoint(
                                        order = objectBallTrajectoryPoints.size + 1,
                                        xPercent = xPercent,
                                        yPercent = yPercent,
                                    )
                                }
                            }
                            }
                        },
                    )
                }
            }
        }

        Row(Modifier.gap(0.75.cssRem).flexWrap(FlexWrap.Wrap)) {
            TrajectoryButton(
                kind = TrajectoryKind.CueBall,
                recordingKind = recordingTrajectoryKind,
                recordLabel = "record cue ball trajectory",
                onStart = {
                    cueBallTrajectoryPoints = emptyList()
                    selectedBallId = null
                    recordingTrajectoryKind = TrajectoryKind.CueBall
                },
                onStop = { recordingTrajectoryKind = null },
            )
            TrajectoryButton(
                kind = TrajectoryKind.ObjectBall,
                recordingKind = recordingTrajectoryKind,
                recordLabel = "record object ball trajectory",
                onStart = {
                    objectBallTrajectoryPoints = emptyList()
                    selectedBallId = null
                    recordingTrajectoryKind = TrajectoryKind.ObjectBall
                },
                onStop = { recordingTrajectoryKind = null },
            )
        }

        Button(
            attrs = Modifier
                .padding(leftRight = 1.15.cssRem, topBottom = 0.72.cssRem)
                .borderRadius(14.px)
                .backgroundColor(Color.rgb(211, 43, 43))
                .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.22f))
                .color(Colors.White)
                .fontWeight(FontWeight.Bold)
                .styleModifier {
                    property("cursor", "pointer")
                    property("box-shadow", "0 12px 28px rgba(0,0,0,0.28)")
                }
                .toAttrs {
                    onClick {
                        downloadSetupCsv(
                            placedBalls = placedBalls,
                            selectedPockets = selectedPockets,
                            selectedRails = selectedRails,
                            highlightedBallId = highlightedBallId,
                            cueBallTrajectoryPoints = cueBallTrajectoryPoints,
                            objectBallTrajectoryPoints = objectBallTrajectoryPoints,
                        )
                    }
                }
        ) {
            Text("Save setup")
        }
    }
}

@Composable
private fun TrajectoryButton(
    kind: TrajectoryKind,
    recordingKind: TrajectoryKind?,
    recordLabel: String,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val isRecordingThisKind = recordingKind == kind

    Button(
        attrs = Modifier
            .padding(leftRight = 1.15.cssRem, topBottom = 0.72.cssRem)
            .borderRadius(14.px)
            .backgroundColor(if (isRecordingThisKind) Color.rgb(255, 255, 255) else Color.rgba(255, 255, 255, 0.1f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.22f))
            .color(if (isRecordingThisKind) Color.rgb(12, 18, 16) else Colors.White)
            .fontWeight(FontWeight.Bold)
            .styleModifier {
                property("cursor", "pointer")
                property("box-shadow", "0 12px 28px rgba(0,0,0,0.22)")
            }
            .toAttrs {
                onClick {
                    if (isRecordingThisKind) {
                        onStop()
                    } else {
                        onStart()
                    }
                }
            }
    ) {
        Text(if (isRecordingThisKind) "stop recording" else recordLabel)
    }
}

@Composable
private fun BallTray(
    availableBallIds: List<Int>,
    selectedBallId: Int?,
    onSelectBall: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .maxWidth(680.px)
            .padding(0.9.cssRem)
            .gap(0.55.cssRem)
            .flexWrap(FlexWrap.Wrap)
            .borderRadius(18.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.06f))
            .styleModifier {
                property("justify-content", "center")
                property("box-shadow", "inset 0 0 0 1px rgba(255,255,255,0.08)")
            }
    ) {
        availableBallIds.forEach { ballId ->
            Div(
                attrs = Modifier
                    .styleModifier {
                        property("cursor", "pointer")
                        property("transition", "transform 140ms ease")
                    }
                    .toAttrs {
                        onClick {
                            onSelectBall(ballId)
                        }
                    }
            ) {
                RenderBall(ballId = ballId, selected = selectedBallId == ballId)
            }
        }
    }
}

@Composable
private fun VerticalTableImage() {
    Img(
        src = BasePath.prependTo("/images/pool_table.jpg"),
        attrs = Modifier
            .position(Position.Absolute)
            .left(50.percent)
            .top(50.percent)
            .styleModifier {
                property("width", "177.7778%")
                property("height", "auto")
                property("max-width", "none")
                property("transform", "translate(-50%, -50%) rotate(90deg)")
                property("transform-origin", "50% 50%")
                property("user-select", "none")
                property("pointer-events", "none")
            }
            .toAttrs()
    )
}

@Composable
private fun PocketOverlays(
    selectedPockets: Set<String>,
    onToggle: (String) -> Unit,
) {
    pocketHotspots.forEach { pocket ->
        val selected = pocket.id in selectedPockets
        Div(
            attrs = Modifier
                .position(Position.Absolute)
                .left(pocket.leftPercent.percent)
                .top(pocket.topPercent.percent)
                .width(pocket.widthPercent.percent)
                .styleModifier {
                    property("aspect-ratio", "1")
                    property("transform", "translate(-50%, -50%)")
                    property("border-radius", "50%")
                    property("cursor", "pointer")
                    property("box-sizing", "border-box")
                    property("background", if (selected) "rgba(255, 218, 87, 0.26)" else "transparent")
                    property("box-shadow", if (selected) "0 0 0 3px rgba(255,218,87,0.9), 0 0 20px rgba(255,218,87,0.48)" else "none")
                }
                .zIndex(5)
                .toAttrs {
                    title(pocket.label)
                    onClick { event ->
                        event.stopPropagation()
                        onToggle(pocket.id)
                    }
                }
        )
    }
}

@Composable
private fun RailOverlays(
    selectedRails: Set<String>,
    onToggle: (String) -> Unit,
) {
    railHotspots.forEach { rail ->
        Div(
            attrs = Modifier
                .position(Position.Absolute)
                .left(rail.leftPercent.percent)
                .top(rail.topPercent.percent)
                .width(rail.widthPercent.percent)
                .styleModifier {
                    property("height", "${rail.heightPercent}%")
                    property("cursor", "pointer")
                    property("background", if (rail.id in selectedRails) "rgba(211,43,43,0.12)" else "transparent")
                }
                .zIndex(4)
                .toAttrs {
                    title(rail.label)
                    onClick { event ->
                        event.stopPropagation()
                        onToggle(rail.id)
                    }
                }
        )
    }
}

@Composable
private fun RailSelectionArrows(selectedRails: Set<String>) {
    railHotspots.filter { it.id in selectedRails }.forEach { rail ->
        RailArrow(
            leftPercent = rail.arrowLeftPercent,
            topPercent = rail.arrowTopPercent,
            direction = rail.arrowDirection,
        )
    }
}

@Composable
private fun RailArrow(leftPercent: Double, topPercent: Double, direction: ArrowDirection) {
    val rotation = when (direction) {
        ArrowDirection.Down -> "0deg"
        ArrowDirection.Right -> "90deg"
        ArrowDirection.Up -> "180deg"
        ArrowDirection.Left -> "270deg"
    }

    Div(
        attrs = Modifier
            .position(Position.Absolute)
            .left(leftPercent.percent)
            .top(topPercent.percent)
            .width(42.px)
            .height(48.px)
            .styleModifier {
                property("transform", "translate(-50%, -50%) rotate($rotation)")
                property("pointer-events", "none")
                property("filter", "drop-shadow(0 2px 6px rgba(0,0,0,0.62))")
            }
            .zIndex(8)
            .toAttrs()
    ) {
        Div(
            attrs = Modifier
                .position(Position.Absolute)
                .left(50.percent)
                .top(0.percent)
                .width(10.px)
                .height(26.px)
                .borderRadius(999.px)
                .backgroundColor(Color.rgb(211, 43, 43))
                .styleModifier {
                    property("transform", "translateX(-50%)")
                    property("box-shadow", "inset 0 1px 0 rgba(255,255,255,0.28)")
                }
                .toAttrs()
        )
        Div(
            attrs = Modifier
                .position(Position.Absolute)
                .left(50.percent)
                .top(24.px)
                .styleModifier {
                    property("width", "0")
                    property("height", "0")
                    property("transform", "translateX(-50%)")
                    property("border-left", "17px solid transparent")
                    property("border-right", "17px solid transparent")
                    property("border-top", "22px solid #d32b2b")
                }
                .toAttrs()
        )
    }
}

@Composable
private fun TrajectoryLayer(
    points: List<TrajectoryPoint>,
    showLines: Boolean,
) {
    if (showLines) {
        points.zipWithNext().forEach { (start, end) ->
            TrajectorySegment(start = start, end = end)
        }
    }

    points.forEach { point ->
        Div(
            attrs = Modifier
                .position(Position.Absolute)
                .left(point.xPercent.percent)
                .top(point.yPercent.percent)
                .width(9.px)
                .height(9.px)
                .borderRadius(50.percent)
                .backgroundColor(Colors.White)
                .border(1.px, LineStyle.Solid, Color.rgba(15, 20, 18, 0.62f))
                .styleModifier {
                    property("transform", "translate(-50%, -50%)")
                    property("box-shadow", "0 0 10px rgba(255,255,255,0.72)")
                    property("pointer-events", "none")
                }
                .zIndex(6)
                .toAttrs()
        )
    }
}

@Composable
private fun TrajectorySegment(start: TrajectoryPoint, end: TrajectoryPoint) {
    val dx = end.xPercent - start.xPercent
    val dy = (end.yPercent - start.yPercent) * TABLE_HEIGHT_TO_WIDTH_RATIO
    val length = sqrt(dx * dx + dy * dy)
    val angle = atan2(dy, dx) * 180.0 / PI

    Div(
        attrs = Modifier
            .position(Position.Absolute)
            .left(start.xPercent.percent)
            .top(start.yPercent.percent)
            .width(length.percent)
            .height(4.px)
            .borderRadius(999.px)
            .backgroundColor(Colors.White)
            .styleModifier {
                property("transform", "translateY(-50%) rotate(${angle}deg)")
                property("transform-origin", "0 50%")
                property("box-shadow", "0 0 12px rgba(255,255,255,0.84), 0 1px 2px rgba(0,0,0,0.5)")
                property("pointer-events", "none")
            }
            .zIndex(6)
            .toAttrs()
    )
}

@Composable
private fun TrajectoryRecordingSurface(
    tableElement: HTMLElement?,
    onRecordPoint: (Double, Double) -> Unit,
) {
    Div(
        attrs = Modifier
            .position(Position.Absolute)
            .left(0.percent)
            .top(0.percent)
            .width(100.percent)
            .height(100.percent)
            .styleModifier {
                property("cursor", "crosshair")
                property("touch-action", "manipulation")
            }
            .zIndex(20)
            .toAttrs {
                onClick { event ->
                    event.stopPropagation()
                    val position = tableClickPosition(
                        element = tableElement,
                        clientX = event.clientX.toDouble(),
                        clientY = event.clientY.toDouble(),
                    ) ?: return@onClick
                    onRecordPoint(position.first, position.second)
                }
            }
    )
}

@Composable
private fun PlacedBallLayer(
    placedBalls: List<TableBall>,
    highlightedBallId: Int?,
    onLongPress: (Int) -> Unit,
    onDoubleClick: (Int) -> Unit,
) {
    placedBalls.forEach { ball ->
        LongPressBall(
            ball = ball,
            highlighted = ball.id == highlightedBallId,
            onLongPress = onLongPress,
            onDoubleClick = onDoubleClick,
        )
    }
}

@Composable
private fun LongPressBall(
    ball: TableBall,
    highlighted: Boolean,
    onLongPress: (Int) -> Unit,
    onDoubleClick: (Int) -> Unit,
) {
    var timerId by remember(ball.id, ball.xPercent, ball.yPercent) { mutableStateOf<Int?>(null) }

    fun cancelTimer() {
        timerId?.let(window::clearTimeout)
        timerId = null
    }

    DisposableEffect(ball.id, ball.xPercent, ball.yPercent) {
        onDispose { cancelTimer() }
    }

    Div(
        attrs = Modifier
            .position(Position.Absolute)
            .left(ball.xPercent.percent)
            .top(ball.yPercent.percent)
            .styleModifier {
                property("width", "3.93%")
                property("aspect-ratio", "1")
                property("transform", "translate(-50%, -50%)")
                property("cursor", "pointer")
                property("touch-action", "none")
            }
            .zIndex(7)
            .toAttrs {
                onClick { event ->
                    event.stopPropagation()
                }
                onDoubleClick { event ->
                    event.stopPropagation()
                    cancelTimer()
                    onDoubleClick(ball.id)
                }
                onMouseDown { event ->
                    event.stopPropagation()
                    cancelTimer()
                    timerId = window.setTimeout({
                        onLongPress(ball.id)
                    }, LONG_PRESS_MS)
                }
                onMouseUp { event ->
                    event.stopPropagation()
                    cancelTimer()
                }
                onMouseLeave {
                    cancelTimer()
                }
                onTouchStart { event ->
                    event.stopPropagation()
                    event.preventDefault()
                    cancelTimer()
                    timerId = window.setTimeout({
                        onLongPress(ball.id)
                    }, LONG_PRESS_MS)
                }
                onTouchEnd { event ->
                    event.stopPropagation()
                    event.preventDefault()
                    cancelTimer()
                }
                onTouchCancel {
                    cancelTimer()
                }
            }
    ) {
        RenderBall(
            ballId = ball.id,
            selected = highlighted,
            fillParent = true,
        )
    }
}

@Composable
private fun RenderBall(
    ballId: Int,
    selected: Boolean,
    fillParent: Boolean = false,
) {
    val modifier = if (fillParent) {
        Modifier
            .width(100.percent)
            .height(100.percent)
    } else {
        Modifier
    }

    if (ballId == 0) {
        CueBall(modifier = modifier, selected = selected, fillParent = fillParent)
    } else {
        PoolBall(ballNumber = ballId, modifier = modifier, selected = selected, fillParent = fillParent)
    }
}

private fun tableClickPosition(
    element: HTMLElement?,
    clientX: Double,
    clientY: Double,
): Pair<Double, Double>? {
    val rect = element?.getBoundingClientRect() ?: return null
    if (rect.width <= 0.0 || rect.height <= 0.0) return null
    return Pair(
        ((clientX - rect.left) / rect.width * 100.0).coerceIn(0.0, 100.0),
        ((clientY - rect.top) / rect.height * 100.0).coerceIn(0.0, 100.0),
    )
}

private fun toggleValue(values: Set<String>, value: String): Set<String> {
    return if (value in values) values - value else values + value
}

private fun downloadSetupCsv(
    placedBalls: List<TableBall>,
    selectedPockets: Set<String>,
    selectedRails: Set<String>,
    highlightedBallId: Int?,
    cueBallTrajectoryPoints: List<TrajectoryPoint>,
    objectBallTrajectoryPoints: List<TrajectoryPoint>,
) {
    val csv = buildString {
        appendLine("record_type,id,x_percent,y_percent,highlighted")
        placedBalls.sortedBy { it.id }.forEach { ball ->
            val highlighted = if (ball.id == highlightedBallId) "yes" else "no"
            appendLine("ball,${ball.id},${ball.xPercent.toCsvNumber()},${ball.yPercent.toCsvNumber()},$highlighted")
        }
        selectedPockets.sorted().forEach { pocketId -> appendLine("pocket,$pocketId,,,") }
        selectedRails.sorted().forEach { railId -> appendLine("rail,$railId,,,") }
        cueBallTrajectoryPoints.sortedBy { it.order }.forEach { point ->
            appendLine("cue_ball_trajectory,${point.order},${point.xPercent.toCsvNumber()},${point.yPercent.toCsvNumber()},")
        }
        objectBallTrajectoryPoints.sortedBy { it.order }.forEach { point ->
            appendLine("object_ball_trajectory,${point.order},${point.xPercent.toCsvNumber()},${point.yPercent.toCsvNumber()},")
        }
    }

    val anchor = document.createElement("a") as HTMLAnchorElement
    val objectUrl = createObjectUrl(csv)
    anchor.href = objectUrl
    anchor.download = "pool-table-setup.csv"
    document.body?.appendChild(anchor)
    anchor.click()
    document.body?.removeChild(anchor)
    revokeObjectUrl(objectUrl)
}

private fun Double.toCsvNumber(): String = (round(this * 100.0) / 100.0).toString()

private fun createObjectUrl(content: String): String = js(
    """
    URL.createObjectURL(new Blob([content], { type: "text/csv;charset=utf-8" }))
    """
)

private fun revokeObjectUrl(url: String) {
    js("URL.revokeObjectURL(url)")
}
