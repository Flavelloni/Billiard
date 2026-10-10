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
import com.varabyte.kobweb.silk.components.icons.fa.FaAnglesUp
import com.varabyte.kobweb.silk.components.icons.fa.FaCompress
import com.varabyte.kobweb.silk.components.icons.fa.FaExpand
import com.varabyte.kobweb.silk.components.icons.fa.FaHeart
import com.varabyte.kobweb.silk.components.icons.fa.FaPause
import com.varabyte.kobweb.silk.components.icons.fa.FaPlay
import com.varabyte.kobweb.silk.components.icons.fa.FaRotateRight
import com.varabyte.kobweb.silk.components.icons.fa.FaShare
import com.varabyte.kobweb.silk.components.icons.fa.FaThumbsDown
import com.varabyte.kobweb.silk.components.icons.fa.FaThumbsUp
import com.varabyte.kobweb.silk.components.icons.fa.IconStyle
import com.varabyte.kobweb.site.components.layouts.PageLayoutData
import com.varabyte.kobweb.site.components.widgets.PoolBall
import com.varabyte.kobweb.site.model.LocalSiteLanguage
import com.varabyte.kobweb.site.model.SiteLanguage
import com.varabyte.kobweb.site.model.text
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
private const val EIGHT_BALL_REEL_TOUCH_THRESHOLD = 64.0
private const val EIGHT_BALL_REEL_VIDEO_SIDE_CROP_PERCENT = 22.0
private const val EIGHT_BALL_REEL_VIDEO_ZOOM = 1.05
private const val EIGHT_BALL_REEL_VIDEO_SOURCE_ASPECT = 16.0 / 9.0
private const val EIGHT_BALL_REEL_VIDEO_AVAILABLE_HEIGHT_PX = 260.0

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

private data class EightBallReelStats(
    val answered: Int = 0,
    val correct: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
) {
    fun next(wasCorrect: Boolean): EightBallReelStats {
        val nextStreak = if (wasCorrect) streak + 1 else 0
        return copy(
            answered = answered + 1,
            correct = correct + if (wasCorrect) 1 else 0,
            streak = nextStreak,
            bestStreak = maxOf(bestStreak, nextStreak),
        )
    }
}

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
    val language = LocalSiteLanguage.current
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
    var imageFullscreen by remember { mutableStateOf(false) }

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
        feedback = buildEightBallFeedback(ball == shot.meta.ball, shot.meta.ball, language)
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
            Text(language.text("8 Ball Prediction", "8-ball prediksjon"))
        }

        when {
            loadError != null -> EightBallMessage(loadError ?: language.text("Could not load 8-ball shots.", "Kunne ikke laste 8-ball-oppsett."))
            loadedShots == null -> EightBallMessage(language.text("Loading shots...", "Laster oppsett..."))
            loadedShots.isEmpty() -> EightBallMessage(language.text("No 8-ball shots found.", "Fant ingen 8-ball-oppsett."))
            currentShot != null -> {
                EightBallShotHeader(currentShot, shotIndex, loadedShots.size, language)

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
                        onImageFullscreen = { imageFullscreen = true }
                    )

                    if (imageFullscreen) {
                        EightBallFullscreenImage(
                            shot = currentShot,
                            onClose = { imageFullscreen = false }
                        )
                    }

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

@Page("/8-ball-prediction-reels")
@Composable
fun EightBallPredictionReelsPage() {
    val initialShotId = remember { currentEightBallReelsShotId() }
    var shots by remember { mutableStateOf<List<EightBallShot>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var shotIndex by remember { mutableStateOf(0) }
    var selectedBall by remember { mutableStateOf<Int?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var videoEnded by remember { mutableStateOf(false) }
    var videoPaused by remember { mutableStateOf(false) }
    var videoElement by remember { mutableStateOf<HTMLVideoElement?>(null) }
    var touchStartY by remember { mutableStateOf<Double?>(null) }
    var isAdvancing by remember { mutableStateOf(false) }
    var showIntro by remember { mutableStateOf(true) }
    var imageFullscreen by remember { mutableStateOf(false) }
    var likedShotIds by remember { mutableStateOf(emptySet<String>()) }
    var shareShot by remember { mutableStateOf<EightBallShot?>(null) }
    var shareCopied by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf(EightBallReelStats()) }

    LaunchedEffect(Unit) {
        ensureEightBallReelsStyles()
        loadEightBallShots(
            onLoaded = {
                shots = it
                val requestedIndex = it.indexOfFirst { shot -> shot.entry.id == initialShotId }
                if (requestedIndex >= 0) {
                    shotIndex = requestedIndex
                    showIntro = false
                }
                loadError = null
            },
            onError = { loadError = it },
        )
    }

    val loadedShots = shots
    val currentShot = loadedShots?.getOrNull(shotIndex)

    LaunchedEffect(showIntro, shotIndex, loadedShots) {
        if (loadedShots == null) return@LaunchedEffect
        val shot = if (showIntro) null else loadedShots?.getOrNull(shotIndex)
        updateEightBallReelsUrl(shot?.entry?.id)
    }

    fun resetPrediction() {
        videoElement?.let { video ->
            video.pause()
            video.currentTime = 0.0
        }
        selectedBall = null
        feedback = null
        videoEnded = false
        videoPaused = false
        touchStartY = null
    }

    fun moveReel(delta: Int) {
        val shotCount = loadedShots?.size ?: return
        if (shotCount == 0 || isAdvancing) return
        isAdvancing = true
        resetPrediction()
        if (showIntro) {
            if (delta > 0) {
                showIntro = false
                shotIndex = 0
            }
        } else {
            val nextIndex = shotIndex + delta
            when {
                nextIndex < 0 -> showIntro = true
                nextIndex >= shotCount -> shotIndex = 0
                else -> shotIndex = nextIndex
            }
        }
        window.setTimeout({ isAdvancing = false }, EIGHT_BALL_SLIDE_DURATION_MS)
    }

    LaunchedEffect(shotIndex, selectedBall, videoElement) {
        if (selectedBall != null) {
            videoElement?.let { video ->
                video.currentTime = 0.0
                videoPaused = false
                video.play()
            }
        }
    }

    fun toggleVideoPlayback() {
        val video = videoElement ?: return
        if (videoEnded) {
            video.currentTime = 0.0
            videoEnded = false
            videoPaused = false
            video.play()
        } else if (video.paused) {
            videoPaused = false
            video.play()
        } else {
            videoPaused = true
            video.pause()
        }
    }

    EightBallReelsShell(
        onNext = { moveReel(1) },
        onPrevious = { moveReel(-1) },
        onTouchStart = { y -> touchStartY = y },
        onTouchEnd = { y ->
            val startY = touchStartY
            touchStartY = null
            if (startY != null) {
                val deltaY = y - startY
                when {
                    deltaY <= -EIGHT_BALL_REEL_TOUCH_THRESHOLD -> moveReel(1)
                    deltaY >= EIGHT_BALL_REEL_TOUCH_THRESHOLD -> moveReel(-1)
                }
            }
        },
    ) {
        when {
            loadError != null -> EightBallReelMessage(loadError ?: "Could not load shots.")
            loadedShots == null -> EightBallReelMessage("Loading...")
            loadedShots.isEmpty() -> EightBallReelMessage("No shots.")
            showIntro -> EightBallReelIntro(onStart = { moveReel(1) })
            currentShot != null -> {
                EightBallReelShot(
                    shot = currentShot,
                    selectedBall = selectedBall,
                    feedback = feedback,
                    videoEnded = videoEnded,
                    videoPaused = videoPaused,
                    stats = stats,
                    liked = currentShot.entry.id in likedShotIds,
                    likesLabel = eightBallReelLikeLabel(currentShot.entry.id),
                    onImageFullscreen = { imageFullscreen = true },
                    onLike = {
                        likedShotIds = if (currentShot.entry.id in likedShotIds) {
                            likedShotIds - currentShot.entry.id
                        } else {
                            likedShotIds + currentShot.entry.id
                        }
                    },
                    onShare = {
                        if (!shareEightBallShotNative(currentShot)) {
                            shareShot = currentShot
                            shareCopied = false
                        }
                    },
                    onBallSelected = { ball ->
                        val correct = ball == currentShot.meta.ball
                        selectedBall = ball
                        videoEnded = false
                        videoPaused = false
                        feedback = buildEightBallReelFeedback(correct)
                        stats = stats.next(correct)
                        videoElement?.let { video ->
                            video.currentTime = 0.0
                            video.play()
                        }
                    },
                    onVideoElement = { videoElement = it },
                    onVideoEnded = {
                        videoEnded = true
                        videoPaused = false
                    },
                    onVideoPlay = {
                        videoEnded = false
                        videoPaused = false
                    },
                    onVideoPause = {
                        if (!videoEnded) videoPaused = true
                    },
                    onVideoToggle = { toggleVideoPlayback() },
                )

                if (imageFullscreen) {
                    EightBallFullscreenImage(
                        shot = currentShot,
                        onClose = { imageFullscreen = false }
                    )
                }
            }
        }

        shareShot?.let { shot ->
            EightBallShareSheet(
                shot = shot,
                copied = shareCopied,
                onCopy = {
                    copyEightBallShareUrl(shot)
                    shareCopied = true
                },
                onClose = {
                    shareShot = null
                    shareCopied = false
                },
            )
        }
    }
}

private fun ensureEightBallReelsStyles() {
    if (document.getElementById("eight-ball-reels-styles") != null) return

    val style = document.createElement("style")
    style.setAttribute("id", "eight-ball-reels-styles")
    style.textContent = """
        @keyframes eightBallReelPulse {
          0%, 100% { opacity: .42; transform: translateY(7px); }
          50% { opacity: 1; transform: translateY(-7px); }
        }
    """.trimIndent()
    document.head?.appendChild(style)
}

@Composable
private fun EightBallReelIntro(onStart: () -> Unit) {
    Column(
        Modifier
            .width(100.percent)
            .height(100.percent)
            .styleModifier {
                property("min-height", "100dvh")
                property("justify-content", "center")
                property("gap", "26px")
                property("padding", "34px")
                property("box-sizing", "border-box")
                property("color", "rgba(246,250,246,.9)")
                property("text-align", "center")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        H1(
            attrs = Modifier
                .margin(0.px)
                .fontSize(2.05.cssRem)
                .lineHeight(1.05)
                .fontWeight(FontWeight.Bold)
                .color(Colors.White)
                .toAttrs()
        ) {
            Text("8-Ball Buddy")
        }
        P(
            attrs = Modifier
                .margin(0.px)
                .styleModifier {
                    property("max-width", "320px")
                    property("font-size", "1.16rem")
                    property("font-weight", "750")
                    property("line-height", "1.32")
                }
                .toAttrs()
        ) {
            Text("Can you predict the pro's next move?")
        }
        P(
            attrs = Modifier
                .margin(0.px)
                .styleModifier {
                    property("max-width", "320px")
                    property("font-size", ".92rem")
                    property("font-weight", "600")
                    property("line-height", "1.42")
                    property("color", "rgba(246,250,246,.68)")
                }
                .toAttrs()
        ) {
            Text("Read the layout, pick the ball, then watch the shot reveal.")
        }
/*
        Button(
            attrs = Modifier
                .borderRadius(999.px)
                .border(1.px, LineStyle.Solid, Color.rgba(126, 218, 116, 0.34f))
                .backgroundColor(Color.rgba(126, 218, 116, 0.14f))
                .color(Colors.White)
                .padding(leftRight = 1.15.cssRem, topBottom = 0.72.cssRem)
                .fontWeight(FontWeight.Bold)
                .styleModifier {
                    property("cursor", "pointer")
                    property("box-shadow", "0 16px 40px rgba(0,0,0,.28)")
                }
                .toAttrs {
                    onClick { onStart() }
                }
        ) {
            Text("Start")
        }
*/
        EightBallReelSwipeHint("Swipe to start")
    }
}

@Composable
private fun EightBallReelsShell(
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onTouchStart: (Double) -> Unit,
    onTouchEnd: (Double) -> Unit,
    content: @Composable () -> Unit,
) {
    Div(
        attrs = Modifier
            .position(Position.Relative)
            .width(100.percent)
            .height(100.percent)
            .styleModifier {
                property("min-height", "100dvh")
                property("background", "radial-gradient(circle at 50% 18%, rgba(32,64,49,.32), transparent 36%), #050606")
                property("display", "flex")
                property("justify-content", "center")
                property("overflow", "hidden")
                property("touch-action", "none")
            }
            .toAttrs {
                onWheel { event ->
                    if (event.deltaY >= EIGHT_BALL_WHEEL_THRESHOLD) {
                        event.preventDefault()
                        onNext()
                    } else if (event.deltaY <= -EIGHT_BALL_WHEEL_THRESHOLD) {
                        event.preventDefault()
                        onPrevious()
                    }
                }
                onTouchStart { event ->
                    val touch = event.touches.item(0) ?: return@onTouchStart
                    onTouchStart(touch.clientY.toDouble())
                }
                onTouchEnd { event ->
                    val touch = event.changedTouches.item(0) ?: return@onTouchEnd
                    onTouchEnd(touch.clientY.toDouble())
                }
                onTouchCancel {
                    onTouchStart(0.0)
                }
            }
    ) {
        Div(
            attrs = Modifier
                .position(Position.Relative)
                .height(100.percent)
                .styleModifier {
                    property("width", "min(100vw, 460px)")
                    property("min-height", "100dvh")
                    property("background", "#020303")
                    property("overflow", "hidden")
                    property("box-shadow", "0 0 80px rgba(0,0,0,.58)")
                }
                .toAttrs()
        ) {
            content()
        }
    }
}

@Composable
private fun EightBallReelShot(
    shot: EightBallShot,
    selectedBall: Int?,
    feedback: String?,
    videoEnded: Boolean,
    videoPaused: Boolean,
    stats: EightBallReelStats,
    liked: Boolean,
    likesLabel: String,
    onImageFullscreen: () -> Unit,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onBallSelected: (Int) -> Unit,
    onVideoElement: (HTMLVideoElement?) -> Unit,
    onVideoEnded: () -> Unit,
    onVideoPlay: () -> Unit,
    onVideoPause: () -> Unit,
    onVideoToggle: () -> Unit,
) {
    val hasPrediction = selectedBall != null
    val correct = selectedBall == shot.meta.ball

    Div(
        attrs = Modifier
            .position(Position.Relative)
            .width(100.percent)
            .height(100.percent)
            .styleModifier {
                property("min-height", "100dvh")
                property("-webkit-tap-highlight-color", "transparent")
                property("-webkit-user-select", "none")
                property("user-select", "none")
            }
            .toAttrs()
    ) {
        EightBallReelVideoLayer(
            shot = shot,
            correct = correct,
            selectedBall = selectedBall,
            feedback = feedback,
            showFeedback = hasPrediction,
            showSwipeHint = hasPrediction && videoEnded,
            videoPaused = videoPaused,
            onVideoElement = onVideoElement,
            onVideoEnded = onVideoEnded,
            onVideoPlay = onVideoPlay,
            onVideoPause = onVideoPause,
            onVideoToggle = onVideoToggle,
        )

        if (!hasPrediction) {
            EightBallReelLayoutOverlay(
                shot = shot,
                onImageFullscreen = onImageFullscreen,
                onBallSelected = onBallSelected,
            )
        }

        EightBallReelActions(
            stats = stats,
            liked = liked,
            likesLabel = likesLabel,
            onLike = onLike,
            onShare = onShare,
        )
    }
}

@Composable
private fun EightBallReelMessage(message: String) {
    Column(
        Modifier
            .width(100.percent)
            .height(100.percent)
            .styleModifier {
                property("min-height", "100dvh")
                property("justify-content", "center")
                property("color", "rgba(246,250,246,.72)")
                property("font-size", ".95rem")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message)
    }
}

@Composable
private fun EightBallReelVideoLayer(
    shot: EightBallShot,
    correct: Boolean,
    selectedBall: Int?,
    feedback: String?,
    showFeedback: Boolean,
    showSwipeHint: Boolean,
    videoPaused: Boolean,
    onVideoElement: (HTMLVideoElement?) -> Unit,
    onVideoEnded: () -> Unit,
    onVideoPlay: () -> Unit,
    onVideoPause: () -> Unit,
    onVideoToggle: () -> Unit,
) {
    Div(
        attrs = Modifier
            .position(Position.Absolute)
            .width(100.percent)
            .height(100.percent)
            .styleModifier {
                property("inset", "0")
                property("display", "grid")
                property("grid-template-rows", "112px minmax(0, 1fr) 112px")
                property("background", "#020303")
            }
            .toAttrs()
    ) {
        Div(
            attrs = Modifier
                .styleModifier {
                    property("display", "flex")
                    property("flex-direction", "column")
                    property("align-items", "center")
                    property("justify-content", "end")
                    property("gap", "4px")
                    property("padding", "max(14px, env(safe-area-inset-top)) 18px 10px")
                    property("opacity", if (showFeedback) "1" else "0")
                }
                .toAttrs()
        ) {
            Span(
                attrs = Modifier
                    .styleModifier {
                        property("width", "46px")
                        property("height", "46px")
                        property("border-radius", "999px")
                        property("display", "flex")
                        property("align-items", "center")
                        property("justify-content", "center")
                        property("font-size", "1.45rem")
                        property("background", if (correct) "rgba(31, 151, 78, .24)" else "rgba(203, 50, 50, .23)")
                        property("border", if (correct) "1px solid rgba(102, 232, 151, .45)" else "1px solid rgba(255, 115, 115, .45)")
                        property("color", if (correct) "#79e69c" else "#ff7373")
                    }
                    .toAttrs()
            ) {
                if (correct) {
                    FaThumbsUp()
                } else {
                    FaThumbsDown()
                }
            }
            Span(
                attrs = Modifier
                    .styleModifier {
                        property("color", if (correct) "rgba(126,218,116,.92)" else "rgba(255,195,101,.92)")
                        property("font-size", ".88rem")
                        property("font-weight", "750")
                        property("text-align", "center")
                        property("line-height", "1.25")
                    }
                    .toAttrs()
            ) {
                Text(eightBallReelResultText(selectedBall, shot.meta.ball))
            }
            if (feedback != null) {
                Span(
                    attrs = Modifier
                        .styleModifier {
                            property("color", "rgba(245,248,245,.62)")
                            property("font-size", ".78rem")
                            property("font-weight", "600")
                            property("text-align", "center")
                            property("line-height", "1.25")
                        }
                        .toAttrs()
                ) {
                    Text(feedback)
                }
            }
        }

        Div(
            attrs = Modifier
                .styleModifier {
                    property("display", "flex")
                    property("align-items", "center")
                    property("justify-content", "center")
                    property("min-height", "0")
                    property("overflow", "hidden")
                }
                .toAttrs()
        ) {
            Div(
                attrs = Modifier
                    .position(Position.Relative)
                    .styleModifier {
                        property("width", eightBallReelVideoFrameWidth())
                        property("aspect-ratio", eightBallReelVideoFrameAspect().toString())
                        property("overflow", "hidden")
                        property("background", "#020303")
                    }
                    .toAttrs()
            ) {
                Video(
                    attrs = Modifier
                        .styleModifier {
                            property("width", "100%")
                            property("height", "100%")
                            property("max-width", "none")
                            property("max-height", "none")
                            property("object-fit", "cover")
                            property("transform", "scale($EIGHT_BALL_REEL_VIDEO_ZOOM)")
                            property("transform-origin", "center center")
                            property("background", "#020303")
                            property("outline", "none")
                        }
                        .toAttrs {
                            attr("src", BasePath.prependTo(shot.entry.videoPath))
                            attr("poster", BasePath.prependTo(shot.entry.imagePath))
                            attr("playsinline", "")
                            attr("disablepictureinpicture", "")
                            attr("preload", "auto")
                            attr("tabindex", "-1")
                            ref { element ->
                                onVideoElement(element)
                                onDispose { onVideoElement(null) }
                            }
                            addEventListener("ended") { onVideoEnded() }
                            addEventListener("play") { onVideoPlay() }
                            addEventListener("pause") { onVideoPause() }
                        }
                )
                EightBallReelVideoControl(
                    videoEnded = showSwipeHint,
                    videoPaused = videoPaused,
                    onClick = onVideoToggle,
                )
            }
        }

        Div(
            attrs = Modifier
                .styleModifier {
                    property("display", "flex")
                    property("align-items", "start")
                    property("justify-content", "center")
                    property("padding", "12px 18px max(18px, env(safe-area-inset-bottom))")
                    property("opacity", if (showSwipeHint) "1" else "0")
                    property("pointer-events", "none")
                }
                .toAttrs()
        ) {
            EightBallReelSwipeHint("Swipe next")
        }
    }
}

@Composable
private fun EightBallReelVideoControl(
    videoEnded: Boolean,
    videoPaused: Boolean,
    onClick: () -> Unit,
) {
    val label = when {
        videoEnded -> "Replay shot"
        videoPaused -> "Play shot"
        else -> "Pause shot"
    }

    Button(
        attrs = Modifier
            .position(Position.Absolute)
            .zIndex(3)
            .styleModifier {
                property("left", "12px")
                property("bottom", "12px")
                property("width", "42px")
                property("height", "42px")
                property("padding", "0")
                property("border", "1px solid rgba(255,255,255,.18)")
                property("border-radius", "999px")
                property("background", "rgba(0,0,0,.48)")
                property("color", "white")
                property("font-size", "1rem")
                property("cursor", "pointer")
                property("display", "flex")
                property("align-items", "center")
                property("justify-content", "center")
                property("backdrop-filter", "blur(8px)")
            }
            .toAttrs {
                attr("aria-label", label)
                title(label)
                onClick { onClick() }
            }
    ) {
        when {
            videoEnded -> FaRotateRight()
            videoPaused -> FaPlay()
            else -> FaPause()
        }
    }
}

@Composable
private fun EightBallReelActions(
    stats: EightBallReelStats,
    liked: Boolean,
    likesLabel: String,
    onLike: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        Modifier
            .position(Position.Absolute)
            .zIndex(5)
            .styleModifier {
                property("right", "12px")
                property("top", "50%")
                property("transform", "translateY(-50%)")
                property("gap", "18px")
                property("align-items", "center")
                property("pointer-events", "auto")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EightBallReelStatsBadge(stats)
        EightBallReelActionButton(
            label = likesLabel,
            color = if (liked) "#ff405f" else "rgba(255,255,255,.94)",
            ariaLabel = "Like shot",
            onClick = onLike,
        ) {
            FaHeart(style = if (liked) IconStyle.FILLED else IconStyle.OUTLINE)
        }
        EightBallReelActionButton(
            label = "Share",
            color = "rgba(255,255,255,.94)",
            ariaLabel = "Share shot",
            onClick = onShare,
        ) {
            FaShare()
        }
    }
}

@Composable
private fun EightBallReelStatsBadge(stats: EightBallReelStats) {
    Column(
        Modifier
            .padding(leftRight = 0.55.cssRem, topBottom = 0.42.cssRem)
            .borderRadius(10.px)
            .backgroundColor(Color.rgba(0, 0, 0, 0.42f))
            .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.14f))
            .styleModifier {
                property("min-width", "58px")
                property("box-shadow", "0 12px 28px rgba(0,0,0,.32)")
                property("backdrop-filter", "blur(8px)")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Span(
            attrs = Modifier
                .fontSize(0.72.cssRem)
                .fontWeight(FontWeight.Bold)
                .lineHeight(1.15)
                .color(Color.rgba(255, 255, 255, 0.9f))
                .toAttrs()
        ) {
            Text("${stats.correct}/${stats.answered}")
        }
        Span(
            attrs = Modifier
                .fontSize(0.62.cssRem)
                .lineHeight(1.15)
                .color(Color.rgba(255, 255, 255, 0.62f))
                .toAttrs()
        ) {
            Text("Streak ${stats.streak}")
        }
        if (stats.bestStreak > 1) {
            Span(
                attrs = Modifier
                    .fontSize(0.62.cssRem)
                    .lineHeight(1.15)
                    .color(Color.rgba(126, 218, 116, 0.82f))
                    .toAttrs()
            ) {
                Text("Best ${stats.bestStreak}")
            }
        }
    }
}

@Composable
private fun EightBallReelActionButton(
    label: String,
    color: String,
    ariaLabel: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            attrs = Modifier
                .styleModifier {
                    property("width", "48px")
                    property("height", "48px")
                    property("padding", "0")
                    property("border", "1px solid rgba(255,255,255,.18)")
                    property("border-radius", "999px")
                    property("background", "rgba(0,0,0,.38)")
                    property("color", color)
                    property("font-size", "1.72rem")
                    property("line-height", "1")
                    property("cursor", "pointer")
                    property("display", "flex")
                    property("align-items", "center")
                    property("justify-content", "center")
                    property("text-shadow", "0 8px 18px rgba(0,0,0,.74)")
                    property("backdrop-filter", "blur(8px)")
                }
                .toAttrs {
                    attr("aria-label", ariaLabel)
                    title(ariaLabel)
                    onClick { onClick() }
                }
        ) {
            icon()
        }
        Span(
            attrs = Modifier
                .styleModifier {
                    property("margin-top", "5px")
                    property("color", "rgba(255,255,255,.9)")
                    property("font-size", ".72rem")
                    property("font-weight", "750")
                    property("line-height", "1")
                    property("text-shadow", "0 6px 14px rgba(0,0,0,.72)")
                }
                .toAttrs()
        ) {
            Text(label)
        }
    }
}

@Composable
private fun EightBallShareSheet(
    shot: EightBallShot,
    copied: Boolean,
    onCopy: () -> Unit,
    onClose: () -> Unit,
) {
    val shareUrl = eightBallReelsShareUrl(shot)
    Div(
        attrs = Modifier
            .position(Position.Fixed)
            .zIndex(10001)
            .styleModifier {
                property("inset", "0")
                property("display", "flex")
                property("align-items", "end")
                property("justify-content", "center")
                property("background", "rgba(0,0,0,.58)")
                property("padding", "20px")
                property("box-sizing", "border-box")
            }
            .toAttrs {
                onClick { onClose() }
            }
    ) {
        Div(
            attrs = Modifier
                .width(100.percent)
                .styleModifier {
                    property("max-width", "420px")
                    property("border-radius", "22px")
                    property("background", "#101414")
                    property("border", "1px solid rgba(255,255,255,.12)")
                    property("box-shadow", "0 28px 90px rgba(0,0,0,.56)")
                    property("padding", "18px")
                    property("box-sizing", "border-box")
                }
                .toAttrs {
                    onClick { it.stopPropagation() }
                }
        ) {
            P(
                attrs = Modifier
                    .margin(0.px)
                    .styleModifier {
                        property("color", "rgba(255,255,255,.92)")
                        property("font-size", ".98rem")
                        property("font-weight", "750")
                        property("line-height", "1.3")
                    }
                    .toAttrs()
            ) {
                Text(eightBallReelTitle(shot.meta))
            }
            P(
                attrs = Modifier
                    .margin(top = 0.38.cssRem, bottom = 1.cssRem)
                    .styleModifier {
                        property("color", "rgba(255,255,255,.58)")
                        property("font-size", ".78rem")
                        property("line-height", "1.35")
                        property("overflow-wrap", "anywhere")
                    }
                    .toAttrs()
            ) {
                Text(shareUrl)
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .gap(0.6.cssRem)
                    .flexWrap(FlexWrap.Wrap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EightBallShareButton("WhatsApp") {
                    openEightBallWhatsAppShare(shot)
                }
                EightBallShareButton("Email") {
                    openEightBallEmailShare(shot)
                }
                EightBallShareButton(if (copied) "Copied" else "Copy") {
                    onCopy()
                }
            }
        }
    }
}

@Composable
private fun EightBallShareButton(label: String, onClick: () -> Unit) {
    Button(
        attrs = Modifier
            .styleModifier {
                property("flex", "1 1 104px")
                property("min-height", "42px")
                property("border", "1px solid rgba(255,255,255,.14)")
                property("border-radius", "999px")
                property("background", "rgba(255,255,255,.08)")
                property("color", "white")
                property("font-weight", "750")
                property("cursor", "pointer")
            }
            .toAttrs {
                onClick { onClick() }
            }
    ) {
        Text(label)
    }
}

@Composable
private fun EightBallReelLayoutOverlay(
    shot: EightBallShot,
    onImageFullscreen: () -> Unit,
    onBallSelected: (Int) -> Unit,
) {
    Div(
        attrs = Modifier
            .position(Position.Absolute)
            .width(100.percent)
            .height(100.percent)
            .styleModifier {
                property("inset", "0")
                property("z-index", "2")
                property("display", "grid")
                property("grid-template-rows", "auto minmax(0, 1fr) auto")
                property("background", "#020303")
            }
            .toAttrs()
    ) {
        Div(
            attrs = Modifier
                .position(Position.Relative)
                .styleModifier {
                    property("display", "flex")
                    property("align-items", "center")
                    property("justify-content", "center")
                    property("min-height", "56px")
                    property("padding", "max(12px, env(safe-area-inset-top)) 64px 8px 18px")
                    property("box-sizing", "border-box")
                }
                .toAttrs()
        ) {
            P(
                attrs = Modifier
                    .margin(0.px)
                    .styleModifier {
                        property("color", "rgba(245,248,245,.76)")
                        property("font-size", ".84rem")
                        property("font-weight", "650")
                        property("line-height", "1.25")
                        property("text-align", "center")
                    }
                    .toAttrs()
            ) {
                Text(eightBallReelTitle(shot.meta))
            }

            Button(
                attrs = Modifier
                    .position(Position.Absolute)
                    .zIndex(4)
                    .styleModifier {
                        property("top", "max(10px, env(safe-area-inset-top))")
                        property("right", "14px")
                        property("width", "40px")
                        property("height", "40px")
                        property("padding", "0")
                        property("border", "1px solid rgba(255,255,255,.18)")
                        property("border-radius", "999px")
                        property("background", "rgba(0,0,0,.56)")
                        property("color", "white")
                        property("font-size", "20px")
                        property("cursor", "pointer")
                        property("display", "flex")
                        property("align-items", "center")
                        property("justify-content", "center")
                        property("backdrop-filter", "blur(8px)")
                    }
                    .toAttrs {
                        attr("aria-label", "View image fullscreen")
                        title("View image fullscreen")
                        onClick { onImageFullscreen() }
                    }
            ) {
                FaExpand()
            }
        }

        Div(
            attrs = Modifier
                .position(Position.Relative)
                .styleModifier {
                    property("min-height", "0")
                    property("display", "flex")
                    property("align-items", "center")
                    property("justify-content", "center")
                    property("overflow", "hidden")
                }
                .toAttrs()
        ) {
            Div(
                attrs = Modifier
                    .position(Position.Relative)
                    .styleModifier {
                        property("width", "min(calc(100% - 28px), 38dvh, 460px)")
                        property("aspect-ratio", "9 / 16")
                        property("overflow", "hidden")
                        property("display", "flex")
                        property("align-items", "center")
                        property("justify-content", "center")
                    }
                    .toAttrs()
            ) {
                Img(
                    src = BasePath.prependTo(shot.entry.imagePath),
                    attrs = Modifier
                        .styleModifier {
                            property("width", "177.777%")
                            property("height", "auto")
                            property("max-width", "none")
                            property("max-height", "none")
                            property("transform", "rotate(90deg)")
                            property("user-select", "none")
                            property("-webkit-user-select", "none")
                            property("-webkit-user-drag", "none")
                        }
                        .toAttrs {
                            attr("alt", "Initial 8-ball layout")
                            attr("draggable", "false")
                        }
                )
            }
        }

        EightBallReelBallTray(
            balls = eightBallReelPickerBalls(shot),
            onBallSelected = onBallSelected,
        )
    }
}

@Composable
private fun EightBallReelSwipeHint(text: String) {
    Div(
        attrs = Modifier
            .styleModifier {
                property("min-width", "96px")
                property("height", "64px")
                property("display", "flex")
                property("flex-direction", "column")
                property("align-items", "center")
                property("justify-content", "center")
                property("gap", "7px")
                property("animation", "eightBallReelPulse 1.15s ease-in-out infinite")
            }
            .toAttrs()
    ) {
        FaAnglesUp(
            Modifier
                .styleModifier {
                    property("font-size", "1.9rem")
                    property("line-height", "1")
                    property("color", "rgba(235,247,238,.88)")
                    property("filter", "drop-shadow(0 8px 18px rgba(0,0,0,.68))")
                }
        )
        Span(
            attrs = Modifier
                .styleModifier {
                    property("color", "rgba(235,247,238,.76)")
                    property("font-size", ".78rem")
                    property("font-weight", "700")
                    property("letter-spacing", "0")
                    property("text-transform", "uppercase")
                    property("white-space", "nowrap")
                }
                .toAttrs()
        ) {
            Text(text)
        }
    }
}

/*
@Composable
private fun EightBallReelSwipeArrow(pointsUp: Boolean) {
    Span(
        attrs = Modifier
            .styleModifier {
                property("width", "12px")
                property("height", "12px")
                property("border-right", "3px solid rgba(235,247,238,.8)")
                property("border-bottom", "3px solid rgba(235,247,238,.8)")
                property("transform", if (pointsUp) "rotate(225deg)" else "rotate(45deg)")
                property("filter", "drop-shadow(0 8px 14px rgba(0,0,0,.62))")
            }
            .toAttrs()
    )
}
*/

/*
@Composable
private fun EightBallReelChevron() {
    Span(
        attrs = Modifier
            .styleModifier {
                property("width", "19px")
                property("height", "19px")
                property("border-right", "3px solid rgba(235,247,238,.8)")
                property("border-bottom", "3px solid rgba(235,247,238,.8)")
                property("transform", "rotate(225deg)")
                property("filter", "drop-shadow(0 8px 14px rgba(0,0,0,.62))")
            }
            .toAttrs()
    )
}
*/

@Composable
private fun EightBallReelBallTray(balls: List<Int>, onBallSelected: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(leftRight = 12.px)
            .gap(0.45.cssRem)
            .styleModifier {
                property("box-sizing", "border-box")
                property("padding-bottom", "max(10px, env(safe-area-inset-bottom))")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Span(
            attrs = Modifier
                .fontSize(0.72.cssRem)
                .fontWeight(FontWeight.Bold)
                .lineHeight(1)
                .color(Color.rgba(235, 247, 238, 0.72f))
                .styleModifier { property("text-transform", "uppercase") }
                .toAttrs()
        ) {
            Text("Pick the ball the pro pots next")
        }
        Row(
            Modifier
                .fillMaxWidth()
                .gap(0.46.cssRem)
                .flexWrap(FlexWrap.Wrap)
                .styleModifier { property("justify-content", "center") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            balls.forEach { ball ->
                Button(
                    attrs = Modifier
                        .width(44.px)
                        .height(44.px)
                        .padding(5.px)
                        .borderRadius(999.px)
                        .backgroundColor(Color.rgba(255, 255, 255, 0.08f))
                        .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.18f))
                        .styleModifier {
                            property("box-sizing", "border-box")
                            property("cursor", "pointer")
                        }
                        .toAttrs {
                            title("${ballLabel(ball)} ball")
                            attr("aria-label", "${ballLabel(ball)} ball")
                            onClick { onBallSelected(ball) }
                        }
                ) {
                    PoolBall(ball, Modifier.width(100.percent).height(100.percent), fillParent = true)
                }
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
private fun EightBallShotHeader(shot: EightBallShot, index: Int, total: Int, language: SiteLanguage) {
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
            EightBallInfoChip(shootingGroupLabel(shot.meta, language))
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
    onImageFullscreen: () -> Unit,
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
                    onImageFullscreen = onImageFullscreen,
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
    onImageFullscreen: () -> Unit = {},
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
            // Image + swipe hint
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

            // Fullscreen button — deliberately outside the pointer-events:none overlay
            Button(
                attrs = Modifier
                    .position(Position.Absolute)
                    .zIndex(10)
                    .styleModifier {
                        property("top", "12px")
                        property("right", "12px")
                        property("width", "44px")
                        property("height", "44px")
                        property("padding", "0")
                        property("border", "none")
                        property("border-radius", "50%")
                        property("background", "rgba(0,0,0,0.65)")
                        property("color", "white")
                        property("font-size", "22px")
                        property("cursor", "pointer")
                        property("display", "flex")
                        property("align-items", "center")
                        property("justify-content", "center")
                        property("backdrop-filter", "blur(8px)")
                    }
                    .toAttrs {
                        attr("aria-label", "View image fullscreen")
                        title("View image fullscreen")
                        onClick {
                            onImageFullscreen()
                        }
                    }
            ) {
                FaExpand()
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Span(
                attrs = Modifier
                    .padding(topBottom = 0.4.cssRem)
                    .color(Color.rgba(245, 248, 244, 0.55f))
                    .fontSize(0.82.cssRem)
                    .toAttrs()
            ) {
                Text("← Swipe to change setup →")
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .gap(0.7.cssRem)
                    .styleModifier { property("justify-content", "center") },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EightBallNavButton("‹ Previous", onPrevious)
                Span(
                    attrs = Modifier
                        .color(Color.rgba(245, 248, 244, 0.72f))
                        .fontSize(0.92.cssRem)
                        .toAttrs()
                ) {
                    Text("${index + 1} / $total")
                }
                EightBallNavButton("Next ›", onNext)
            }
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


@Composable
private fun EightBallFullscreenImage(
    shot: EightBallShot?,
    onClose: () -> Unit,
) {
    if (shot == null) return

    Div(
        attrs = Modifier
            .position(Position.Fixed)
            .styleModifier {
                property("inset", "0")
                property("width", "100vw")
                property("height", "100vh")
                property("background", "#000")
                property("z-index", "10000")
                property("display", "flex")
                property("align-items", "center")
                property("justify-content", "center")
                property("overflow", "hidden")
            }
            .toAttrs()
    ) {
        Img(
            src = BasePath.prependTo(shot.entry.imagePath),
            attrs = Modifier
                .styleModifier {
                    property("width", "min(100dvh, 177.777vw)")
                    property("height", "auto")
                    property("max-width", "none")
                    property("max-height", "none")
                    property("transform", "rotate(90deg)")
                    property("user-select", "none")
                    property("-webkit-user-select", "none")
                    property("-webkit-user-drag", "none")
                }
                .toAttrs {
                    attr("alt", "8-ball layout")
                    attr("draggable", "false")
                }
        )

        Button(
            attrs = Modifier
                .position(Position.Absolute)
                .styleModifier {
                    property("right", "18px")
                    property("top", "18px")
                    property("width", "48px")
                    property("height", "48px")
                    property("padding", "0")
                    property("border", "none")
                    property("border-radius", "50%")
                    property("background", "rgba(0,0,0,0.65)")
                    property("color", "white")
                    property("font-size", "28px")
                    property("cursor", "pointer")
                    property("display", "flex")
                    property("align-items", "center")
                    property("justify-content", "center")
                }
                .toAttrs {
                    attr("aria-label", "Close fullscreen image")
                    title("Close fullscreen image")
                    onClick { onClose() }
                }
        ) {
            FaCompress()
        }
    }
}

private fun buildEightBallFeedback(correct: Boolean, actualBall: Int, language: SiteLanguage): String {
    val lead = if (correct) {
        positiveEightBallFeedback[Random.nextInt(positiveEightBallFeedback.size)]
    } else {
        negativeEightBallFeedback[Random.nextInt(negativeEightBallFeedback.size)]
    }
    return language.text(
        "$lead It was the ${ballLabel(actualBall)} ball.",
        "Riktig ball var ${ballLabel(actualBall)}.",
    )
}

private fun buildEightBallReelFeedback(correct: Boolean): String {
    return if (correct) {
        positiveEightBallFeedback[Random.nextInt(positiveEightBallFeedback.size)]
    } else {
        negativeEightBallFeedback[Random.nextInt(negativeEightBallFeedback.size)]
    }
}

private fun eightBallReelVideoFrameAspect(): Double {
    val visibleWidth = 1.0 - EIGHT_BALL_REEL_VIDEO_SIDE_CROP_PERCENT / 50.0
    return EIGHT_BALL_REEL_VIDEO_SOURCE_ASPECT * EIGHT_BALL_REEL_VIDEO_ZOOM * visibleWidth
}

private fun eightBallReelVideoFrameWidth(): String {
    val aspect = eightBallReelVideoFrameAspect()
    val heightLimitedDvh = aspect * 100.0
    val heightLimitedPx = aspect * EIGHT_BALL_REEL_VIDEO_AVAILABLE_HEIGHT_PX
    return "min(calc(100% - 28px), calc(${heightLimitedDvh}dvh - ${heightLimitedPx}px), 460px)"
}

private fun eightBallReelResultText(selectedBall: Int?, actualBall: Int): String {
    val actual = "${ballLabel(actualBall)} ball"
    return if (selectedBall == actualBall) {
        "Correct: $actual"
    } else {
        "You picked ${ballLabel(selectedBall ?: 0)}. Correct: $actual"
    }
}

private fun eightBallReelPickerBalls(shot: EightBallShot): List<Int> {
    return when (shot.meta.group) {
        1 -> (1..7).toList()
        2 -> (9..15).toList()
        0 -> {
            val actualBall = shot.meta.ball.takeIf { it in 1..15 && it != 8 }
            val seed = shot.entry.id.fold(shot.meta.ball * 97) { acc, char ->
                acc * 31 + char.code
            }
            val filler = (1..15)
                .filter { it != 8 && it != actualBall }
                .shuffled(Random(seed))
                .take(if (actualBall == null) 7 else 6)

            (if (actualBall == null) filler else filler + actualBall)
                .shuffled(Random(seed * 31 + 17))
        }
        else -> (8..10).toList()
    }
}

private fun shootingGroupLabel(meta: EightBallMeta, language: SiteLanguage): String {
    return when (meta.group) {
        0 -> language.text("${meta.player}, open table", "${meta.player}, åpent bord")
        1 -> language.text("${meta.player} shoots solids", "${meta.player} skyter lave")
        2 -> language.text("${meta.player} shoots stripes", "${meta.player} skyter høye")
        else -> language.text("${meta.player} shoots group ${meta.group}", "${meta.player} skyter gruppe ${meta.group}")
    }
}

private fun eightBallReelTitle(meta: EightBallMeta): String {
    return when (meta.group) {
        0 -> "${meta.player} has an open table"
        1 -> "${meta.player} has solids"
        2 -> "${meta.player} has stripes"
        else -> "${meta.player} has group ${meta.group}"
    }
}

private fun eightBallReelLikeLabel(shotId: String): String {
    var value = 0
    shotId.forEach { char ->
        value = (value * 31 + char.code) % 76000
    }
    val count = 3600 + value
    return if (count < 10000) {
        count.toString()
    } else {
        "${count / 1000}.${(count % 1000) / 100}k"
    }
}

private fun currentEightBallReelsShotId(): String? {
    return js("new URLSearchParams(window.location.search).get('shot')") as? String
}

private fun updateEightBallReelsUrl(shotId: String?) {
    val baseUrl = "${window.location.origin}${BasePath.prependTo("/8-ball-prediction-reels")}"
    val nextUrl = if (shotId == null) baseUrl else "$baseUrl?shot=${encodeUriComponent(shotId)}"
    js("window.history.replaceState(null, '', nextUrl)")
}

private fun eightBallReelsShareUrl(shot: EightBallShot): String {
    return "${window.location.origin}${BasePath.prependTo("/8-ball-prediction-reels")}?shot=${encodeUriComponent(shot.entry.id)}"
}

private fun copyEightBallShareUrl(shot: EightBallShot) {
    val url = eightBallReelsShareUrl(shot)
    js("if (navigator.clipboard) { navigator.clipboard.writeText(url); }")
}

private fun shareEightBallShotNative(shot: EightBallShot): Boolean {
    val title = eightBallReelTitle(shot.meta)
    val url = eightBallReelsShareUrl(shot)
    return js("""
        (function() {
            if (!navigator.share) return false;
            navigator.share({ title: title, text: title, url: url }).catch(function() {});
            return true;
        })()
    """) as Boolean
}

private fun openEightBallWhatsAppShare(shot: EightBallShot) {
    val text = encodeUriComponent("${eightBallReelTitle(shot.meta)} ${eightBallReelsShareUrl(shot)}")
    window.open("https://wa.me/?text=$text", "_blank")
}

private fun openEightBallEmailShare(shot: EightBallShot) {
    val subject = encodeUriComponent(eightBallReelTitle(shot.meta))
    val body = encodeUriComponent(eightBallReelsShareUrl(shot))
    js("window.location.href = 'mailto:?subject=' + subject + '&body=' + body")
}

private fun encodeUriComponent(value: String): String {
    return js("encodeURIComponent(value)") as String
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
