package com.varabyte.kobweb.site.pages

import androidx.compose.runtime.Composable
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
import com.varabyte.kobweb.compose.ui.modifiers.lineHeight
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.maxWidth
import com.varabyte.kobweb.compose.ui.modifiers.overflow
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.position
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
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Video
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLVideoElement
import kotlin.math.abs
import kotlin.random.Random

private const val EIGHT_BALL_WHEEL_THRESHOLD = 56.0
private const val EIGHT_BALL_TOUCH_THRESHOLD = 64.0
private const val EIGHT_BALL_DRAG_COMMIT_PERCENT = 18.0
private const val EIGHT_BALL_SLIDE_DURATION_MS = 320

private data class EightBallEntry(
    val id: String,
    val metaPath: String,
    val imagePath: String,
    val videoPath: String,
)

private data class EightBallMeta(
    val player: String,
    val opponent: String,
    val group: Int,
    val ball: Int,
    val comments: String,
)

private data class EightBallShot(
    val entry: EightBallEntry,
    val meta: EightBallMeta,
)

private val positiveEightBallFeedback = listOf(
    "That's it!",
    "Exactly!",
    "Nice read.",
    "Right ball.",
    "You called it.",
    "Sharp pick.",
)

private val negativeEightBallFeedback = listOf(
    "Not this time.",
    "Different ball.",
    "Good try.",
    "Nope, another ball.",
    "The table had other plans."
)

@InitRoute
fun initEightBallPredictionPage(ctx: InitRouteContext) {
    ctx.data.add(PageLayoutData("8 Ball Prediction", "Predict the next 8-ball shot from real match layouts."))
}

@Page("/8-ball-prediction")
@Composable
@Layout(".components.layouts.PageLayout")
fun EightBallPredictionPage() {
    var shots by remember { mutableStateOf<List<EightBallShot>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var shotIndex by remember { mutableStateOf(0) }
    var selectedBall by remember { mutableStateOf<Int?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var videoEnded by remember { mutableStateOf(false) }
    var showStartLayout by remember { mutableStateOf(false) }
    var videoElement by remember { mutableStateOf<HTMLVideoElement?>(null) }
    var slideDelta by remember { mutableStateOf(0) }
    var isSliding by remember { mutableStateOf(false) }
    var dragOffsetPercent by remember { mutableStateOf(0.0) }
    var isDragSettling by remember { mutableStateOf(false) }
    var swipeRegionElement by remember { mutableStateOf<HTMLElement?>(null) }
    var touchStart by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var touchIsHorizontal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loadEightBallShots(
            onLoaded = {
                shots = it
                loadError = null
            },
            onError = { loadError = it },
        )
    }

    val loadedShots = shots
    val currentShot = loadedShots?.getOrNull(shotIndex)

    fun resetPrediction() {
        videoElement?.let { video ->
            video.pause()
            video.currentTime = 0.0
        }
        selectedBall = null
        feedback = null
        videoEnded = false
        showStartLayout = false
        dragOffsetPercent = 0.0
        isDragSettling = false
        touchStart = null
        touchIsHorizontal = false
    }

    fun slideToShot(delta: Int) {
        val shotCount = loadedShots?.size ?: return
        if (shotCount == 0 || isSliding) return
        resetPrediction()
        slideDelta = if (delta < 0) -1 else 1
        isSliding = true
        window.setTimeout({
            shotIndex = (shotIndex + delta + shotCount) % shotCount
            slideDelta = 0
            dragOffsetPercent = 0.0
            isSliding = false
        }, EIGHT_BALL_SLIDE_DURATION_MS)
    }

    fun snapDragBack() {
        isDragSettling = true
        dragOffsetPercent = 0.0
        window.setTimeout({
            isDragSettling = false
        }, EIGHT_BALL_SLIDE_DURATION_MS)
    }

    fun predictBall(ball: Int, shot: EightBallShot) {
        videoElement?.let { video ->
            video.currentTime = 0.0
            video.play()
        }
        selectedBall = ball
        videoEnded = false
        showStartLayout = false
        feedback = buildEightBallFeedback(ball == shot.meta.ball, shot.meta.ball)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(leftRight = 1.25.cssRem, top = 1.5.cssRem, bottom = 3.cssRem)
            .gap(1.cssRem)
            .styleModifier {
                property("min-height", "calc(100vh - 64px)")
                property("background", "linear-gradient(180deg, #071012 0%, #111711 46%, #17131a 100%)")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        H1(
            attrs = Modifier
                .margin(0.px)
                .fontSize(2.15.cssRem)
                .lineHeight(1.05)
                .fontWeight(FontWeight.Bold)
                .color(Colors.White)
                .toAttrs()
        ) {
            Text("8 Ball Prediction")
        }

        when {
            loadError != null -> EightBallMessage(loadError ?: "Could not load 8-ball shots.")
            loadedShots == null -> EightBallMessage("Loading shots...")
            loadedShots.isEmpty() -> EightBallMessage("No 8-ball shots found.")
            currentShot != null -> {
                EightBallShotHeader(currentShot, shotIndex, loadedShots.size)

                EightBallSwipeRegion(
                    onRegionElement = { swipeRegionElement = it },
                    onPrevious = { slideToShot(-1) },
                    onNext = { slideToShot(1) },
                    onTouchStart = { x, y ->
                        if (!isSliding) {
                            touchStart = Pair(x, y)
                            touchIsHorizontal = false
                            isDragSettling = false
                        }
                    },
                    onTouchMove = { x, y, preventDefault ->
                        val start = touchStart ?: return@EightBallSwipeRegion
                        val deltaX = x - start.first
                        val deltaY = y - start.second
                        if (!touchIsHorizontal && abs(deltaX) > 10.0 && abs(deltaX) > abs(deltaY)) {
                            touchIsHorizontal = true
                        }
                        if (touchIsHorizontal) {
                            preventDefault()
                            val width = swipeRegionElement?.getBoundingClientRect()?.width ?: 1.0
                            dragOffsetPercent = (deltaX / width * 100.0).coerceIn(-100.0, 100.0)
                        }
                    },
                    onTouchEnd = { x, y ->
                        val start = touchStart
                        touchStart = null
                        if (start == null || !touchIsHorizontal) {
                            touchIsHorizontal = false
                            snapDragBack()
                            return@EightBallSwipeRegion
                        }

                        val deltaX = x - start.first
                        val deltaY = y - start.second
                        touchIsHorizontal = false
                        when {
                            dragOffsetPercent <= -EIGHT_BALL_DRAG_COMMIT_PERCENT ||
                                (abs(deltaX) >= EIGHT_BALL_TOUCH_THRESHOLD && abs(deltaX) > abs(deltaY) && deltaX < 0) -> slideToShot(1)
                            dragOffsetPercent >= EIGHT_BALL_DRAG_COMMIT_PERCENT ||
                                (abs(deltaX) >= EIGHT_BALL_TOUCH_THRESHOLD && abs(deltaX) > abs(deltaY) && deltaX > 0) -> slideToShot(-1)
                            else -> snapDragBack()
                        }
                    },
                    onTouchCancel = {
                        touchStart = null
                        touchIsHorizontal = false
                        snapDragBack()
                    },
                )
                {
                    EightBallCarousel(
                        shots = loadedShots,
                        currentIndex = shotIndex,
                        slideDelta = slideDelta,
                        isSliding = isSliding,
                        dragOffsetPercent = dragOffsetPercent,
                        isDragSettling = isDragSettling,
                        hasPrediction = selectedBall != null,
                        showStartLayout = showStartLayout && videoEnded,
                        canShowStartLayout = selectedBall != null && videoEnded,
                        onShowStartLayoutChange = { showStartLayout = it },
                        onVideoEnded = { videoEnded = true },
                        onVideoPlay = {
                            videoEnded = false
                            showStartLayout = false
                        },
                        onVideoElement = { videoElement = it },
                    )

                    EightBallNavigation(
                        index = shotIndex,
                        total = loadedShots.size,
                        onPrevious = { slideToShot(-1) },
                        onNext = { slideToShot(1) },
                    )
                }

                EightBallFeedback(feedback, selectedBall == currentShot.meta.ball)

                EightBallBallTray(
                    selectedBall = selectedBall,
                    actualBall = currentShot.meta.ball,
                    onBallSelected = { ball ->
                        if (selectedBall == null) {
                            predictBall(ball, currentShot)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun EightBallMessage(message: String) {
    P(
        attrs = Modifier
            .margin(0.px)
            .fontSize(1.cssRem)
            .color(Color.rgba(236, 242, 236, 0.78f))
            .toAttrs()
    ) {
        Text(message)
    }
}

@Composable
private fun EightBallShotHeader(shot: EightBallShot, index: Int, total: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .maxWidth(920.px)
            .gap(0.55.cssRem),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        P(
            attrs = Modifier
                .margin(0.px)
                .fontSize(1.2.cssRem)
                .fontWeight(FontWeight.Bold)
                .color(Colors.White)
                .toAttrs()
        ) {
            Text("${shot.meta.player} vs ${shot.meta.opponent}")
        }
        Row(
            Modifier
                .fillMaxWidth()
                .gap(0.5.cssRem)
                .flexWrap(FlexWrap.Wrap)
                .styleModifier { property("justify-content", "center") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EightBallInfoChip(shootingGroupLabel(shot.meta))
            if (shot.meta.comments.isNotBlank()) {
                EightBallInfoChip(shot.meta.comments)
            }
        }
    }
}

@Composable
private fun EightBallInfoChip(text: String) {
    Span(
        attrs = Modifier
            .padding(leftRight = 0.72.cssRem, topBottom = 0.42.cssRem)
            .borderRadius(999.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.08f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.15f))
            .color(Color.rgba(245, 248, 244, 0.86f))
            .fontSize(0.88.cssRem)
            .toAttrs()
    ) {
        Text(text)
    }
}

@Composable
private fun EightBallSwipeRegion(
    onRegionElement: (HTMLElement?) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTouchStart: (Double, Double) -> Unit,
    onTouchMove: (Double, Double, () -> Unit) -> Unit,
    onTouchEnd: (Double, Double) -> Unit,
    onTouchCancel: () -> Unit,
    content: @Composable () -> Unit,
) {
    Div(
        attrs = Modifier
            .fillMaxWidth()
            .maxWidth(980.px)
            .padding(topBottom = 0.2.cssRem)
            .styleModifier {
                property("touch-action", "pan-y pinch-zoom")
                property("-webkit-tap-highlight-color", "transparent")
                property("-webkit-user-select", "none")
                property("user-select", "none")
            }
            .toAttrs {
                ref { element ->
                    onRegionElement(element)
                    onDispose { onRegionElement(null) }
                }
                onWheel { event ->
                    val horizontalDelta = when {
                        abs(event.deltaX) >= EIGHT_BALL_WHEEL_THRESHOLD -> event.deltaX
                        event.shiftKey && abs(event.deltaY) >= EIGHT_BALL_WHEEL_THRESHOLD -> event.deltaY
                        else -> 0.0
                    }
                    if (horizontalDelta != 0.0) {
                        event.preventDefault()
                        if (horizontalDelta < 0) onPrevious() else onNext()
                    }
                }
                onTouchStart { event ->
                    val touch = event.touches.item(0) ?: return@onTouchStart
                    onTouchStart(touch.clientX.toDouble(), touch.clientY.toDouble())
                }
                onTouchMove { event ->
                    val touch = event.touches.item(0) ?: return@onTouchMove
                    onTouchMove(touch.clientX.toDouble(), touch.clientY.toDouble()) {
                        event.preventDefault()
                    }
                }
                onTouchEnd { event ->
                    val touch = event.changedTouches.item(0)
                    if (touch == null) {
                        onTouchCancel()
                    } else {
                        onTouchEnd(touch.clientX.toDouble(), touch.clientY.toDouble())
                    }
                }
                onTouchCancel {
                    onTouchCancel()
                }
            }
    ) {
        content()
    }
}

@Composable
private fun EightBallCarousel(
    shots: List<EightBallShot>,
    currentIndex: Int,
    slideDelta: Int,
    isSliding: Boolean,
    dragOffsetPercent: Double,
    isDragSettling: Boolean,
    hasPrediction: Boolean,
    showStartLayout: Boolean,
    canShowStartLayout: Boolean,
    onShowStartLayoutChange: (Boolean) -> Unit,
    onVideoEnded: () -> Unit,
    onVideoPlay: () -> Unit,
    onVideoElement: (HTMLVideoElement?) -> Unit,
) {
    val previousShot = shots[(currentIndex - 1 + shots.size) % shots.size]
    val currentShot = shots[currentIndex]
    val nextShot = shots[(currentIndex + 1) % shots.size]
    val transform = when {
        !isSliding -> "translateX(${(-100.0 + dragOffsetPercent).coerceIn(-200.0, 0.0)}%)"
        slideDelta > 0 -> "translateX(-200%)"
        else -> "translateX(0)"
    }

    Div(
        attrs = Modifier
            .fillMaxWidth()
            .maxWidth(960.px)
            .padding(0.48.cssRem)
            .borderRadius(20.px)
            .backgroundColor(Color.rgb(6, 9, 10))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.12f))
            .styleModifier {
                property("box-shadow", "0 26px 80px rgba(0,0,0,0.46)")
            }
            .toAttrs()
    ) {
        Div(
            attrs = Modifier
                .position(Position.Relative)
                .fillMaxWidth()
                .overflow(Overflow.Hidden)
                .borderRadius(14.px)
                .backgroundColor(Color.rgb(2, 4, 5))
                .styleModifier {
                    property("aspect-ratio", "16 / 9")
                    property("touch-action", "pan-y pinch-zoom")
                }
                .toAttrs()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(100.percent)
                    .styleModifier {
                        property("transform", transform)
                        property("transition", if (isSliding || isDragSettling) "transform ${EIGHT_BALL_SLIDE_DURATION_MS}ms cubic-bezier(0.22, 0.8, 0.22, 1)" else "none")
                    }
            ) {
                EightBallCarouselPanel(previousShot, isActive = false)
                EightBallCarouselPanel(
                    shot = currentShot,
                    isActive = true,
                    hasPrediction = hasPrediction,
                    showStartLayout = showStartLayout,
                    canShowStartLayout = canShowStartLayout,
                    onShowStartLayoutChange = onShowStartLayoutChange,
                    onVideoEnded = onVideoEnded,
                    onVideoPlay = onVideoPlay,
                    onVideoElement = onVideoElement,
                )
                EightBallCarouselPanel(nextShot, isActive = false)
            }
        }
    }
}

@Composable
private fun EightBallCarouselPanel(
    shot: EightBallShot,
    isActive: Boolean,
    hasPrediction: Boolean = false,
    showStartLayout: Boolean = false,
    canShowStartLayout: Boolean = false,
    onShowStartLayoutChange: (Boolean) -> Unit = {},
    onVideoEnded: () -> Unit = {},
    onVideoPlay: () -> Unit = {},
    onVideoElement: (HTMLVideoElement?) -> Unit = {},
) {
    Div(
        attrs = Modifier
            .position(Position.Relative)
            .width(100.percent)
            .height(100.percent)
            .styleModifier {
                property("flex", "0 0 100%")
                property("background", "#020405")
                property("-webkit-tap-highlight-color", "transparent")
                property("-webkit-user-select", "none")
                property("user-select", "none")
            }
            .toAttrs {
                if (isActive) {
                    onMouseEnter {
                        if (canShowStartLayout) onShowStartLayoutChange(true)
                    }
                    onMouseLeave {
                        onShowStartLayoutChange(false)
                    }
                    onTouchStart {
                        if (canShowStartLayout) onShowStartLayoutChange(true)
                    }
                    onTouchEnd {
                        onShowStartLayoutChange(false)
                    }
                    onTouchCancel {
                        onShowStartLayoutChange(false)
                    }
                }
            }
    ) {
        if (isActive) {
            Video(
                attrs = Modifier
                    .fillMaxWidth()
                    .height(100.percent)
                    .styleModifier {
                        property("object-fit", "contain")
                        property("background", "#020405")
                        property("outline", "none")
                        property("-webkit-tap-highlight-color", "transparent")
                        property("-webkit-user-select", "none")
                        property("user-select", "none")
                        property("-webkit-user-drag", "none")
                    }
                    .toAttrs {
                        attr("src", BasePath.prependTo(shot.entry.videoPath))
                        attr("poster", BasePath.prependTo(shot.entry.imagePath))
                        attr("playsinline", "")
                        //attr("controls", "")              // temporarily restore
                        attr("disablepictureinpicture", "")
                        attr("preload", "auto")
                        attr("tabindex", "-1")
                        ref { element ->
                            onVideoElement(element)
                            onDispose { onVideoElement(null) }
                        }
                        addEventListener("ended") { onVideoEnded() }
                        addEventListener("play") { onVideoPlay() }
                    }
            )
        } else {
            EightBallLayoutImage(shot)
        }

        if (isActive && (!hasPrediction || showStartLayout)) {
            Div(
                attrs = Modifier
                    .position(Position.Absolute)
                    .width(100.percent)
                    .height(100.percent)
                    .zIndex(2)
                    .styleModifier {
                        property("inset", "0")
                        property("pointer-events", "none")
                    }
                    .toAttrs()
            ) {
                EightBallLayoutImage(shot)
            }
        }
    }
}

@Composable
private fun EightBallLayoutImage(shot: EightBallShot) {
    Img(
        src = BasePath.prependTo(shot.entry.imagePath),
        attrs = Modifier
            .fillMaxWidth()
            .height(100.percent)
            .styleModifier {
                property("object-fit", "contain")
                property("background", "#020405")
                property("user-select", "none")
            }
            .toAttrs {
                attr("alt", "Initial 8-ball layout")
                attr("draggable", "false")
            }
    )
}

@Composable
private fun EightBallFeedback(feedback: String?, correct: Boolean) {
    Div(
        attrs = Modifier
            .fillMaxWidth()
            .maxWidth(920.px)
            .height(2.2.cssRem)
            .toAttrs()
    ) {
        if (feedback != null) {
            P(
                attrs = Modifier
                    .margin(0.px)
                    .fontSize(1.cssRem)
                    .fontWeight(FontWeight.Bold)
                    .lineHeight(1.4)
                    .color(if (correct) Color.rgb(126, 218, 116) else Color.rgb(255, 195, 101))
                    .styleModifier { property("text-align", "center") }
                    .toAttrs()
            ) {
                Text(feedback)
            }
        }
    }
}

@Composable
private fun EightBallBallTray(
    selectedBall: Int?,
    actualBall: Int,
    onBallSelected: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .maxWidth(760.px)
            .gap(0.55.cssRem)
            .flexWrap(FlexWrap.Wrap)
            .styleModifier { property("justify-content", "center") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        (1..15).forEach { ball ->
            val answered = selectedBall != null
            val isSelected = selectedBall == ball
            val isActual = answered && actualBall == ball
            val borderColor = when {
                isActual -> Color.rgb(126, 218, 116)
                isSelected -> Color.rgb(224, 72, 72)
                else -> Color.rgba(255, 255, 255, 0.18f)
            }
            Button(
                attrs = Modifier
                    .width(52.px)
                    .height(52.px)
                    .padding(7.px)
                    .borderRadius(999.px)
                    .backgroundColor(Color.rgba(255, 255, 255, if (isSelected || isActual) 0.14f else 0.07f))
                    .border(2.px, LineStyle.Solid, borderColor)
                    .styleModifier {
                        property("cursor", if (answered) "default" else "pointer")
                        property("box-sizing", "border-box")
                    }
                    .toAttrs {
                        title("${ballLabel(ball)} ball")
                        attr("aria-label", "${ballLabel(ball)} ball")
                        if (!answered) {
                            onClick { onBallSelected(ball) }
                        }
                    }
            ) {
                PoolBall(ball, Modifier.width(100.percent).height(100.percent), selected = isSelected || isActual, fillParent = true)
            }
        }
    }
}

@Composable
private fun EightBallNavigation(index: Int, total: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    Div(
        attrs = Modifier
            .fillMaxWidth()
            .maxWidth(560.px)
            .styleModifier {
                property("margin-left", "auto")
                property("margin-right", "auto")
            }
            .toAttrs {
                onWheel { event ->
                    val horizontalDelta = when {
                        abs(event.deltaX) >= EIGHT_BALL_WHEEL_THRESHOLD -> event.deltaX
                        event.shiftKey && abs(event.deltaY) >= EIGHT_BALL_WHEEL_THRESHOLD -> event.deltaY
                        else -> 0.0
                    }
                    if (horizontalDelta != 0.0) {
                        event.preventDefault()
                        if (horizontalDelta < 0) onPrevious() else onNext()
                    }
                }
            }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .gap(0.7.cssRem)
                .styleModifier { property("justify-content", "center") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EightBallNavButton("<- Previous", onPrevious)
            Span(
                attrs = Modifier
                    .color(Color.rgba(245, 248, 244, 0.72f))
                    .fontSize(0.92.cssRem)
                    .toAttrs()
            ) {
                Text("${index + 1} / $total")
            }
            EightBallNavButton("Next ->", onNext)
        }
    }
}

@Composable
private fun EightBallNavButton(label: String, onClick: () -> Unit) {
    Button(
        attrs = Modifier
            .width(8.cssRem)
            .padding(topBottom = 0.72.cssRem)
            .borderRadius(999.px)
            .backgroundColor(Color.rgba(255, 255, 255, 0.1f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.18f))
            .color(Colors.White)
            .fontWeight(FontWeight.Bold)
            .styleModifier { property("cursor", "pointer") }
            .toAttrs {
                onClick { onClick() }
            }
    ) {
        Text(label)
    }
}

private fun buildEightBallFeedback(correct: Boolean, actualBall: Int): String {
    val lead = if (correct) {
        positiveEightBallFeedback[Random.nextInt(positiveEightBallFeedback.size)]
    } else {
        negativeEightBallFeedback[Random.nextInt(negativeEightBallFeedback.size)]
    }
    return "$lead It was the ${ballLabel(actualBall)} ball."
}

private fun shootingGroupLabel(meta: EightBallMeta): String {
    return when (meta.group) {
        0 -> "${meta.player}, open table"
        1 -> "${meta.player} shoots solids"
        2 -> "${meta.player} shoots stripes"
        else -> "${meta.player} shoots group ${meta.group}"
    }
}

private fun ballLabel(ball: Int): String {
    return if (ball in 1..15) ball.toString() else "unknown"
}

private fun loadEightBallShots(onLoaded: (List<EightBallShot>) -> Unit, onError: (String) -> Unit) {
    fetchEightBallText(
        BasePath.prependTo("/8ball/index.json"),
        onSuccess = { manifestText ->
            val entries = parseEightBallEntries(manifestText)
            if (entries.isEmpty()) {
                onLoaded(emptyList())
                return@fetchEightBallText
            }

            val loaded = mutableListOf<EightBallShot>()
            var remaining = entries.size
            var failed = false

            entries.forEach { entry ->
                fetchEightBallText(
                    BasePath.prependTo(entry.metaPath),
                    onSuccess = { metaText ->
                        if (failed) return@fetchEightBallText
                        loaded += EightBallShot(entry, parseEightBallMeta(metaText))
                        remaining -= 1
                        if (remaining == 0) {
                            onLoaded(loaded.sortedBy { it.entry.id })
                        }
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
        onError = { onError("Could not load 8-ball manifest: $it") },
    )
}

private fun parseEightBallEntries(json: String): List<EightBallEntry> {
    val items = js("JSON.parse(json)")
    val length = items.length as Int
    return (0 until length).map { index ->
        val item = items[index]
        EightBallEntry(
            id = item.id as String,
            metaPath = item.metaPath as String,
            imagePath = item.imagePath as String,
            videoPath = item.videoPath as String,
        )
    }
}

private fun parseEightBallMeta(text: String): EightBallMeta {
    val item = js("JSON.parse(text)")
    return EightBallMeta(
        player = (item.player as? String).orEmpty(),
        opponent = (item.opponent as? String).orEmpty(),
        group = (item.group as? Int) ?: 0,
        ball = (item.ball as? Int) ?: -1,
        comments = (item.comments as? String).orEmpty(),
    )
}

private fun fetchEightBallText(url: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
    js("fetch(url).then(function(response) { if (!response.ok) { throw new Error(response.status + ' ' + response.statusText); } return response.text(); }).then(function(text) { onSuccess(text); }).catch(function(error) { onError(String(error)); });")
}
