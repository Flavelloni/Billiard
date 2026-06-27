package com.varabyte.kobweb.site.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.varabyte.kobweb.site.components.widgets.FeaturedPlayers
import com.varabyte.kobweb.site.components.widgets.PoolBall
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
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

private const val PREDICTION_TABLE_HEIGHT_TO_WIDTH_RATIO = 1.0 / 0.5625
private const val SWIPE_LEFT_THRESHOLD_PX = 70.0
private const val PREDICTION_MOBILE_BREAKPOINT_PX = 768
private const val PREDICTION_TABLE_BALL_WIDTH = "3.93%"
private const val PREDICTION_MOBILE_TABLE_BALL_WIDTH = "4.4%"

private data class GameEntry(val id: String, val metaPath: String, val csvPath: String)
private data class GameMeta(
    val player: String,
    val opponent: String,
    val event: String,
    val date: String,
    val link: String,
    val comments: String,
)

private data class PredictionBall(val id: Int, val xPercent: Double, val yPercent: Double, val highlighted: Boolean)
private data class PredictionPoint(val order: Int, val xPercent: Double, val yPercent: Double)
private data class PredictionGame(
    val entry: GameEntry,
    val meta: GameMeta,
    val balls: List<PredictionBall>,
    val pockets: Set<String>,
    val rails: Set<String>,
    val cueTrajectory: List<PredictionPoint>,
    val objectTrajectory: List<PredictionPoint>,
)

private data class PocketTarget(val id: String, val label: String, val x: Double, val y: Double, val size: Double)
private data class RailTarget(val id: String, val x: Double, val y: Double, val width: Double, val height: Double)

private val predictionPockets = listOf(
    PocketTarget("top_left", "Top left", 8.4, 8.4, 10.5),
    PocketTarget("top_right", "Top right", 91.6, 8.4, 10.5),
    PocketTarget("bottom_left", "Bottom left", 8.4, 91.6, 10.5),
    PocketTarget("bottom_right", "Bottom right", 91.6, 91.6, 10.5),
    PocketTarget("left_side", "Left side", 8.4, 50.0, 10.0),
    PocketTarget("right_side", "Right side", 91.6, 50.0, 10.0),
)

private val predictionRails = listOf(
    RailTarget("top", 18.0, 3.2, 64.0, 9.0),
    RailTarget("bottom", 18.0, 87.8, 64.0, 9.0),
    RailTarget("left_upper", 2.8, 16.0, 9.0, 28.0),
    RailTarget("left_lower", 2.8, 56.0, 9.0, 28.0),
    RailTarget("right_upper", 88.2, 16.0, 9.0, 28.0),
    RailTarget("right_lower", 88.2, 56.0, 9.0, 28.0),
)

@InitRoute
fun initShotPredictionPage(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("Shot Prediction", "Predict the object ball, pocket, and cue ball finish before revealing the shot."))
}

@Page
@Composable
@Layout(".components.layouts.PageLayout")
fun ShotPredictionPage() {
    var games by remember { mutableStateOf<List<PredictionGame>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var gameIndex by remember { mutableStateOf(0) }
    var selectedObjectBall by remember { mutableStateOf<Int?>(null) }
    var selectedPocket by remember { mutableStateOf<String?>(null) }
    var predictedCueFinish by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var resolved by remember { mutableStateOf(false) }
    var tableElement by remember { mutableStateOf<HTMLElement?>(null) }
    var swipeStartX by remember { mutableStateOf<Double?>(null) }
    val isMobile = rememberIsPredictionMobileLayout()

    LaunchedEffect(Unit) {
        loadPredictionGames(
            onLoaded = {
                games = it
                loadError = null
            },
            onError = { loadError = it },
        )
    }

    val loadedGames = games
    val currentGame = loadedGames?.getOrNull(gameIndex)

    fun advanceToNextGame() {
        val gameCount = loadedGames?.size ?: return
        if (gameCount == 0) return
        gameIndex = (gameIndex + 1) % gameCount
        selectedObjectBall = null
        selectedPocket = null
        predictedCueFinish = null
        resolved = false
        swipeStartX = null
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(leftRight = 1.25.cssRem, top = 2.cssRem, bottom = 3.cssRem)
            .gap(1.cssRem),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        H1(
            attrs = Modifier
                .margin(0.px)
                .fontSize(2.2.cssRem)
                .fontWeight(FontWeight.Bold)
                .color(Colors.White)
                .toAttrs()
        ) {
            Text("Shot Prediction")
        }

        FeaturedPlayers()

        when {
            loadError != null -> PredictionMessage(loadError ?: "Could not load games.")
            loadedGames == null -> PredictionMessage("Loading games...")
            loadedGames.isEmpty() -> PredictionMessage("No games found in public/games.")
            currentGame != null -> {
                GameHeader(currentGame, gameIndex, loadedGames.size)
                PredictionControls(
                    game = currentGame,
                    selectedObjectBall = selectedObjectBall,
                    selectedPocket = selectedPocket,
                    predictedCueFinish = predictedCueFinish,
                    resolved = resolved,
                    onResolve = { resolved = true },
                    onNextLayout = ::advanceToNextGame,
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
                                property("touch-action", if (resolved) "pan-y" else "manipulation")
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
                                    if (resolved) return@onClick
                                    val position = tableClickPosition(
                                        element = tableElement,
                                        clientX = event.clientX.toDouble(),
                                        clientY = event.clientY.toDouble(),
                                    ) ?: return@onClick
                                    predictedCueFinish = position
                                }
                                onMouseDown { event ->
                                    if (resolved) {
                                        swipeStartX = event.clientX.toDouble()
                                    }
                                }
                                onMouseUp { event ->
                                    if (!resolved) return@onMouseUp
                                    val startX = swipeStartX ?: return@onMouseUp
                                    val deltaX = event.clientX.toDouble() - startX
                                    if (deltaX < -SWIPE_LEFT_THRESHOLD_PX) {
                                        advanceToNextGame()
                                    } else {
                                        swipeStartX = null
                                    }
                                }
                                onMouseLeave {
                                    swipeStartX = null
                                }
                                onTouchStart { event ->
                                    val touch = event.touches.item(0) ?: return@onTouchStart
                                    swipeStartX = touch.clientX.toDouble()
                                }
                                onTouchEnd { event ->
                                    if (!resolved || loadedGames.isEmpty()) return@onTouchEnd
                                    val startX = swipeStartX ?: return@onTouchEnd
                                    val touch = event.changedTouches.item(0) ?: return@onTouchEnd
                                    val deltaX = touch.clientX.toDouble() - startX
                                    if (deltaX < -SWIPE_LEFT_THRESHOLD_PX) {
                                        advanceToNextGame()
                                    } else {
                                        swipeStartX = null
                                    }
                                }
                            }
                    ) {
                        VerticalPredictionTableImage()
                        PredictionPocketLayer(
                            selectedPocket = selectedPocket,
                            revealedPockets = if (resolved) currentGame.pockets else emptySet(),
                            onSelectPocket = {
                                if (!resolved) selectedPocket = it
                            },
                        )
                        PredictionRailLayer(
                            rails = if (resolved) currentGame.rails else emptySet(),
                        )
                        if (resolved) {
                            TrajectoryLayer(currentGame.objectTrajectory, Color.rgb(255, 255, 255))
                            CueTrajectoryLayer(
                                cueBall = currentGame.balls.firstOrNull { it.id == 0 },
                                points = currentGame.cueTrajectory,
                                color = Color.rgb(255, 255, 255),
                            )
                            CorrectCueFinish(currentGame.cueTrajectory.lastOrNull(), isMobile)
                        }
                        predictedCueFinish?.let { point ->
                            PredictionCueFinish(point)
                        }
                        currentGame.balls.forEach { ball ->
                            PredictionBallView(
                                ball = ball,
                                selected = selectedObjectBall == ball.id,
                                revealCorrect = resolved,
                                isMobile = isMobile,
                                onSelect = {
                                    if (!resolved && ball.id != 0) selectedObjectBall = ball.id
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionMessage(message: String) {
    P(
        attrs = Modifier
            .margin(0.px)
            .color(Color.rgba(226, 238, 230, 0.74f))
            .toAttrs()
    ) {
        Text(message)
    }
}

@Composable
private fun rememberIsPredictionMobileLayout(): Boolean {
    var isMobile by remember { mutableStateOf(window.innerWidth <= PREDICTION_MOBILE_BREAKPOINT_PX) }

    DisposableEffect(Unit) {
        val listener: (dynamic) -> Unit = {
            isMobile = window.innerWidth <= PREDICTION_MOBILE_BREAKPOINT_PX
        }
        window.addEventListener("resize", listener)
        onDispose {
            window.removeEventListener("resize", listener)
        }
    }

    return isMobile
}

@Composable
private fun GameHeader(game: PredictionGame, index: Int, total: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .maxWidth(760.px)
            .gap(0.35.cssRem),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        P(
            attrs = Modifier
                .margin(0.px)
                .fontSize(1.25.cssRem)
                .fontWeight(FontWeight.Bold)
                .color(Colors.White)
                .toAttrs()
        ) {
            Text("${game.meta.player} to shoot")
        }
        P(
            attrs = Modifier
                .margin(0.px)
                .fontSize(0.95.cssRem)
                .lineHeight(1.5)
                .color(Color.rgba(226, 238, 230, 0.72f))
                .toAttrs()
        ) {
            Text("${game.meta.event} | ${game.meta.date} | ${game.meta.comments} | Game ${index + 1} of $total")
        }
    }
}

@Composable
private fun PredictionControls(
    game: PredictionGame,
    selectedObjectBall: Int?,
    selectedPocket: String?,
    predictedCueFinish: Pair<Double, Double>?,
    resolved: Boolean,
    onResolve: () -> Unit,
    onNextLayout: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .maxWidth(840.px)
            .gap(0.7.cssRem)
            .flexWrap(FlexWrap.Wrap)
            .styleModifier { property("justify-content", "center") }
    ) {
        ControlChip("object: ${selectedObjectBall?.toString() ?: "-"}")
        ControlChip("pocket: ${selectedPocket ?: "-"}")
        ControlChip(if (predictedCueFinish == null) "cue finish: -" else "cue finish: set")
        Button(
            attrs = Modifier
                .padding(leftRight = 1.05.cssRem, topBottom = 0.62.cssRem)
                .borderRadius(14.px)
                .backgroundColor(if (resolved) Color.rgba(255, 255, 255, 0.1f) else Color.rgb(211, 43, 43))
                .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.22f))
                .color(Colors.White)
                .fontWeight(FontWeight.Bold)
                .styleModifier { property("cursor", if (resolved) "default" else "pointer") }
                .toAttrs {
                    if (!resolved) {
                        onClick { onResolve() }
                    }
                }
        ) {
            Text(if (resolved) "resolved" else "resolve")
        }
        Button(
            attrs = Modifier
                .padding(leftRight = 1.05.cssRem, topBottom = 0.62.cssRem)
                .borderRadius(14.px)
                .backgroundColor(Color.rgba(255, 255, 255, 0.1f))
                .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.22f))
                .color(Colors.White)
                .fontWeight(FontWeight.Bold)
                .styleModifier { property("cursor", "pointer") }
                .toAttrs {
                    onClick { onNextLayout() }
                }
        ) {
            Text("next layout")
        }
    }
}

@Composable
private fun ControlChip(text: String) {
    Div(
        attrs = Modifier
            .padding(leftRight = 0.85.cssRem, topBottom = 0.55.cssRem)
            .borderRadius(999.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.08f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.14f))
            .color(Color.rgba(255, 255, 255, 0.84f))
            .fontSize(0.9.cssRem)
            .toAttrs()
    ) {
        Text(text)
    }
}

@Composable
private fun VerticalPredictionTableImage() {
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
private fun PredictionBallView(
    ball: PredictionBall,
    selected: Boolean,
    revealCorrect: Boolean,
    isMobile: Boolean,
    onSelect: () -> Unit,
) {
    val isCorrectObject = revealCorrect && ball.highlighted
    val ballWidth = if (isMobile) PREDICTION_MOBILE_TABLE_BALL_WIDTH else PREDICTION_TABLE_BALL_WIDTH
    Div(
        attrs = Modifier
            .position(Position.Absolute)
            .left(ball.xPercent.percent)
            .top(ball.yPercent.percent)
            .styleModifier {
                property("width", ballWidth)
                property("aspect-ratio", "1")
                property("transform", "translate(-50%, -50%)")
                property("cursor", if (ball.id == 0 || revealCorrect) "default" else "pointer")
                property("box-shadow", if (isCorrectObject) "0 0 0 4px rgba(255,218,87,0.52), 0 0 18px rgba(255,218,87,0.7)" else "none")
                property("border-radius", "50%")
            }
            .zIndex(8)
            .toAttrs {
                onClick { event ->
                    event.stopPropagation()
                    onSelect()
                }
            }
    ) {
        if (ball.id == 0) {
            CueBall(Modifier.width(100.percent).height(100.percent), selected, fillParent = true)
        } else {
            PoolBall(ball.id, Modifier.width(100.percent).height(100.percent), selected || isCorrectObject, fillParent = true)
        }
    }
}

@Composable
private fun PredictionPocketLayer(
    selectedPocket: String?,
    revealedPockets: Set<String>,
    onSelectPocket: (String) -> Unit,
) {
    predictionPockets.forEach { pocket ->
        val selected = selectedPocket == pocket.id
        val revealed = pocket.id in revealedPockets
        Div(
            attrs = Modifier
                .position(Position.Absolute)
                .left(pocket.x.percent)
                .top(pocket.y.percent)
                .width(pocket.size.percent)
                .styleModifier {
                    property("aspect-ratio", "1")
                    property("transform", "translate(-50%, -50%)")
                    property("border-radius", "50%")
                    property("cursor", "pointer")
                    property("background", when {
                        revealed -> "rgba(255, 218, 87, 0.30)"
                        selected -> "rgba(78, 156, 255, 0.24)"
                        else -> "transparent"
                    })
                    property("box-shadow", when {
                        revealed -> "0 0 0 3px rgba(255,218,87,0.9), 0 0 20px rgba(255,218,87,0.48)"
                        selected -> "0 0 0 3px rgba(78,156,255,0.86), 0 0 18px rgba(78,156,255,0.42)"
                        else -> "none"
                    })
                }
                .zIndex(7)
                .toAttrs {
                    title(pocket.label)
                    onClick { event ->
                        event.stopPropagation()
                        onSelectPocket(pocket.id)
                    }
                }
        )
    }
}

@Composable
private fun PredictionRailLayer(rails: Set<String>) {
    predictionRails.filter { it.id in rails }.forEach { rail ->
        Div(
            attrs = Modifier
                .position(Position.Absolute)
                .left(rail.x.percent)
                .top(rail.y.percent)
                .width(rail.width.percent)
                .styleModifier {
                    property("height", "${rail.height}%")
                    property("background", "rgba(211,43,43,0.18)")
                    property("box-shadow", "inset 0 0 0 2px rgba(211,43,43,0.72)")
                    property("pointer-events", "none")
                }
                .zIndex(5)
                .toAttrs()
        )
    }
}

@Composable
private fun TrajectoryLayer(points: List<PredictionPoint>, color: Color, dashed: Boolean = false) {
    points.zipWithNext().forEach { (start, end) ->
        TrajectorySegment(
            startXPercent = start.xPercent,
            startYPercent = start.yPercent,
            endXPercent = end.xPercent,
            endYPercent = end.yPercent,
            color = color,
            dashed = dashed,
        )
    }
}

@Composable
private fun CueTrajectoryLayer(cueBall: PredictionBall?, points: List<PredictionPoint>, color: Color) {
    val firstPoint = points.firstOrNull()
    if (cueBall != null && firstPoint != null) {
        TrajectorySegment(
            startXPercent = cueBall.xPercent,
            startYPercent = cueBall.yPercent,
            endXPercent = firstPoint.xPercent,
            endYPercent = firstPoint.yPercent,
            color = color,
            dashed = true,
        )
    }

    TrajectoryLayer(points, color, dashed = true)
}

@Composable
private fun TrajectorySegment(
    startXPercent: Double,
    startYPercent: Double,
    endXPercent: Double,
    endYPercent: Double,
    color: Color,
    dashed: Boolean,
) {
    val dx = endXPercent - startXPercent
    val dy = (endYPercent - startYPercent) * PREDICTION_TABLE_HEIGHT_TO_WIDTH_RATIO
    val length = sqrt(dx * dx + dy * dy)
    val angle = atan2(dy, dx) * 180.0 / PI
    val baseModifier = Modifier
        .position(Position.Absolute)
        .left(startXPercent.percent)
        .top(startYPercent.percent)
        .width(length.percent)
        .height(4.px)
        .borderRadius(999.px)

    Div(
        attrs = (if (dashed) baseModifier else baseModifier.backgroundColor(color))
            .styleModifier {
                property("transform", "translateY(-50%) rotate(${angle}deg)")
                property("transform-origin", "0 50%")
                property("box-shadow", "0 0 12px rgba(255,255,255,0.84), 0 1px 2px rgba(0,0,0,0.5)")
                property("pointer-events", "none")
                if (dashed) {
                    property("background-image", "repeating-linear-gradient(90deg, rgba(255,255,255,0.96) 0 10px, transparent 10px 16px)")
                }
            }
            .zIndex(6)
            .toAttrs()
    )
}

@Composable
private fun PredictionCueFinish(point: Pair<Double, Double>) {
    Div(
        attrs = Modifier
            .position(Position.Absolute)
            .left(point.first.percent)
            .top(point.second.percent)
            .width(18.px)
            .height(18.px)
            .borderRadius(50.percent)
            .border(3.px, LineStyle.Solid, Color.rgb(78, 156, 255))
            .styleModifier {
                property("transform", "translate(-50%, -50%)")
                property("box-shadow", "0 0 16px rgba(78,156,255,0.74)")
                property("pointer-events", "none")
            }
            .zIndex(9)
            .toAttrs()
    )
}

@Composable
private fun CorrectCueFinish(point: PredictionPoint?, isMobile: Boolean) {
    point ?: return
    val ballWidth = if (isMobile) PREDICTION_MOBILE_TABLE_BALL_WIDTH else PREDICTION_TABLE_BALL_WIDTH
    Div(
        attrs = Modifier
            .position(Position.Absolute)
            .left(point.xPercent.percent)
            .top(point.yPercent.percent)
            .styleModifier {
                property("width", ballWidth)
                property("aspect-ratio", "1")
                property("transform", "translate(-50%, -50%)")
                property("box-shadow", "0 0 18px rgba(255,255,255,0.84)")
                property("pointer-events", "none")
            }
            .borderRadius(50.percent)
            .border(3.px, LineStyle.Solid, Colors.White)
            .zIndex(9)
            .toAttrs()
    )
}

private fun tableClickPosition(element: HTMLElement?, clientX: Double, clientY: Double): Pair<Double, Double>? {
    val rect = element?.getBoundingClientRect() ?: return null
    if (rect.width <= 0.0 || rect.height <= 0.0) return null
    return Pair(
        ((clientX - rect.left) / rect.width * 100.0).coerceIn(0.0, 100.0),
        ((clientY - rect.top) / rect.height * 100.0).coerceIn(0.0, 100.0),
    )
}

private fun loadPredictionGames(onLoaded: (List<PredictionGame>) -> Unit, onError: (String) -> Unit) {
    fetchText(
        BasePath.prependTo("/games/index.json"),
        onSuccess = { manifestText ->
            val entries = parseGameEntries(manifestText)
            if (entries.isEmpty()) {
                onLoaded(emptyList())
                return@fetchText
            }

            val loaded = mutableListOf<PredictionGame>()
            var remaining = entries.size
            var failed = false

            entries.forEach { entry ->
                fetchText(
                    BasePath.prependTo(entry.metaPath),
                    onSuccess = { metaText ->
                        fetchText(
                            BasePath.prependTo(entry.csvPath),
                            onSuccess = { csvText ->
                                if (failed) return@fetchText
                                loaded += parsePredictionGame(entry, metaText, csvText)
                                remaining -= 1
                                if (remaining == 0) {
                                    onLoaded(loaded.sortedBy { it.entry.id })
                                }
                            },
                            onError = {
                                if (!failed) {
                                    failed = true
                                    onError("Could not load ${entry.csvPath}: $it")
                                }
                            },
                        )
                    },
                    onError = {
                        if (!failed) {
                            failed = true
                            onError("Could not load ${entry.metaPath}: $it")
                        }
                    },
                )
            }
        },
        onError = { onError("Could not load games manifest: $it") },
    )
}

private fun parseGameEntries(json: String): List<GameEntry> {
    val items = js("JSON.parse(json)")
    val length = items.length as Int
    return (0 until length).map { index ->
        val item = items[index]
        GameEntry(
            id = item.id as String,
            metaPath = item.metaPath as String,
            csvPath = item.csvPath as String,
        )
    }
}

private fun parsePredictionGame(entry: GameEntry, metaText: String, csvText: String): PredictionGame {
    val meta = parseGameMeta(metaText)
    val balls = mutableListOf<PredictionBall>()
    val pockets = mutableSetOf<String>()
    val rails = mutableSetOf<String>()
    val cueTrajectory = mutableListOf<PredictionPoint>()
    val objectTrajectory = mutableListOf<PredictionPoint>()

    csvText.lineSequence().drop(1).filter { it.isNotBlank() }.forEach { line ->
        val columns = line.split(',')
        val recordType = columns.getOrNull(0).orEmpty()
        val id = columns.getOrNull(1).orEmpty()
        val x = columns.getOrNull(2)?.toDoubleOrNull()
        val y = columns.getOrNull(3)?.toDoubleOrNull()
        val highlighted = columns.getOrNull(4) == "yes"

        when (recordType) {
            "ball" -> {
                val ballId = id.toIntOrNull()
                if (ballId != null && x != null && y != null) {
                    balls += PredictionBall(ballId, x, y, highlighted)
                }
            }
            "pocket" -> pockets += id
            "rail" -> rails += id
            "cue_ball_trajectory" -> {
                val order = id.toIntOrNull()
                if (order != null && x != null && y != null) {
                    cueTrajectory += PredictionPoint(order, x, y)
                }
            }
            "object_ball_trajectory" -> {
                val order = id.toIntOrNull()
                if (order != null && x != null && y != null) {
                    objectTrajectory += PredictionPoint(order, x, y)
                }
            }
        }
    }

    return PredictionGame(
        entry = entry,
        meta = meta,
        balls = balls.sortedBy { it.id },
        pockets = pockets,
        rails = rails,
        cueTrajectory = cueTrajectory.sortedBy { it.order },
        objectTrajectory = objectTrajectory.sortedBy { it.order },
    )
}

private fun parseGameMeta(json: String): GameMeta {
    val item = js("JSON.parse(json)")
    return GameMeta(
        player = (item.player as? String).orEmpty(),
        opponent = (item.opponent as? String).orEmpty(),
        event = (item.event as? String).orEmpty(),
        date = (item.date as? String).orEmpty(),
        link = (item.link as? String).orEmpty(),
        comments = (item.comments as? String).orEmpty(),
    )
}

private fun fetchText(url: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
    js("fetch(url).then(function(response) { if (!response.ok) { throw new Error(response.status + ' ' + response.statusText); } return response.text(); }).then(function(text) { onSuccess(text); }).catch(function(error) { onError(String(error)); });")
}
