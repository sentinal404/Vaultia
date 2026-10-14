package com.vaultia.app.ui.player

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RawRes
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.vaultia.app.R
import com.vaultia.app.domain.model.VaultFile
import com.vaultia.app.ui.viewmodel.PlayerViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Vaultia Modern Audio Player Screen (Ditto Master Edition).
 *
 * Implements:
 * 1. Pixel-perfect arch container with dynamic random anime/character artwork (images 1-22)
 *    and alternating looping muted short videos (videos 1-4).
 * 2. Concentric curved arc scrubber with neon violet glow, dual-ring thumb, and continuous clamp-safe angle math.
 * 3. Proportional layout & top notch / camera cutout auto-resize with dedicated side gutters.
 * 4. Interactive floating transport dock anchored at the bottom with glowing Play/Pause halo that pops above the dock.
 * 5. Fullscreen edge-to-edge immersive experience with functional queue bottom sheet.
 */

// 22 artwork drawables imported from Downloads
val ARTWORK_POOL = listOf(
    R.drawable.artwork_1,
    R.drawable.artwork_2,
    R.drawable.artwork_3,
    R.drawable.artwork_4,
    R.drawable.artwork_5,
    R.drawable.artwork_6,
    R.drawable.artwork_7,
    R.drawable.artwork_8,
    R.drawable.artwork_9,
    R.drawable.artwork_10,
    R.drawable.artwork_11,
    R.drawable.artwork_12,
    R.drawable.artwork_13,
    R.drawable.artwork_14,
    R.drawable.artwork_15,
    R.drawable.artwork_16,
    R.drawable.artwork_17,
    R.drawable.artwork_18,
    R.drawable.artwork_19,
    R.drawable.artwork_20,
    R.drawable.artwork_21,
    R.drawable.artwork_22
)

// 4 short looping videos imported from Downloads
val VIDEO_POOL = listOf(
    R.raw.video_1,
    R.raw.video_2,
    R.raw.video_3,
    R.raw.video_4
)

object ArcScrubberMath {
    const val START_ANGLE = 165f
    const val TOTAL_SWEEP = -150f

    /**
     * Converts relative touch offsets (dx, dy) from the center of curvature (cx, cy)
     * into a clamped progress fraction [0.0f, 1.0f].
     *
     * Handles upward touch drifts (dy < 0) safely without jumps:
     * - Touching above horizontal on left side (dx < 0) clamps smoothly to 0.0f (start of track).
     * - Touching above horizontal on right side (dx >= 0) clamps smoothly to 1.0f (end of track).
     */
    fun calculateArcFraction(
        dx: Float,
        dy: Float,
        startAngle: Float = START_ANGLE,
        totalSweep: Float = TOTAL_SWEEP
    ): Float {
        val angleRad = atan2(dy, dx)
        var angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
        if (angleDeg < 0) {
            if (dx < 0) {
                // Left half: touch drifted above horizontal (past start of track)
                angleDeg += 360f
            }
            // Right half: touch drifted above horizontal (past end of track)
        }
        return ((startAngle - angleDeg) / (-totalSweep)).coerceIn(0f, 1f)
    }
}

/**
 * Plays a short background video in a continuous loop inside the arch container.
 *
 * Strict requirements:
 * 1. 100% MUTED (volume = 0f) so it never merges with or interferes with playing music.
 * 2. REPEAT MODE (Player.REPEAT_MODE_ONE) so it loops continuously until the song changes.
 * 3. SYNC WITH AUDIO: pauses when the audio player is paused, plays when playing.
 * 4. ZOOM CROP: fills the arch container bounds seamlessly matching image artwork.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun LoopingMutedVideoBackground(
    @RawRes videoResId: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val packageName = context.packageName

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f // Strictly muted at all times
            repeatMode = Player.REPEAT_MODE_ONE // Repeat endlessly
        }
    }

    LaunchedEffect(videoResId) {
        val uri = android.net.Uri.parse("android.resource://$packageName/$videoResId")
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = isPlaying
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    fileId: String,
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val window = activity?.window

    val currentFile by viewModel.currentFile.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val playlist by viewModel.playlist.collectAsState()

    var isShuffle by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }

    // Media mode: alternates Image -> Video -> Image -> Video on successive tracks
    var isVideoMode by remember { mutableStateOf(false) }
    var currentArtworkRes by remember { mutableIntStateOf(ARTWORK_POOL.random()) }
    var currentVideoRes by remember { mutableIntStateOf(VIDEO_POOL.random()) }
    var previousTrackId by remember { mutableStateOf<String?>(null) }

    // On track transition, alternate between Image and Video and pick randomly from the pool
    LaunchedEffect(currentFile?.id) {
        val trackId = currentFile?.id
        if (trackId != null) {
            if (previousTrackId != null && previousTrackId != trackId) {
                // Strict requirement: alternate mode on each music track change
                isVideoMode = !isVideoMode
            }
            previousTrackId = trackId

            if (isVideoMode) {
                val candidates = VIDEO_POOL.filter { it != currentVideoRes }
                currentVideoRes = if (candidates.isNotEmpty()) candidates.random() else VIDEO_POOL.random()
            } else {
                val candidates = ARTWORK_POOL.filter { it != currentArtworkRes }
                currentArtworkRes = if (candidates.isNotEmpty()) candidates.random() else ARTWORK_POOL.random()
            }
        }
    }

    // Enforce True Fullscreen Sticky Immersive Mode with Cutout Support
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation

    fun enforceTrueFullscreen() {
        if (window == null) return
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    DisposableEffect(orientation) {
        enforceTrueFullscreen()
        @Suppress("DEPRECATION")
        val listener = android.view.View.OnSystemUiVisibilityChangeListener { visibility ->
            if ((visibility and android.view.View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                enforceTrueFullscreen()
            }
        }
        @Suppress("DEPRECATION")
        window?.decorView?.setOnSystemUiVisibilityChangeListener(listener)
        onDispose {
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                WindowCompat.setDecorFitsSystemWindows(window, true)
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                @Suppress("DEPRECATION")
                window.decorView.setOnSystemUiVisibilityChangeListener(null)
            }
        }
    }

    LaunchedEffect(orientation) {
        enforceTrueFullscreen()
    }

    // Load media on fileId change
    LaunchedEffect(fileId) {
        viewModel.loadMedia(fileId)
    }

    // Back handling: dismiss queue sheet if open, else stop playback and back out
    BackHandler {
        if (showQueueSheet) {
            showQueueSheet = false
        } else {
            viewModel.stopPlayback()
            onBackClick()
        }
    }

    // Shuffle toggle handler
    val onToggleShuffle: () -> Unit = {
        isShuffle = !isShuffle
        Toast.makeText(context, if (isShuffle) "Shuffle: ON" else "Shuffle: OFF", Toast.LENGTH_SHORT).show()
    }

    // Progress fraction
    val progressFraction = if (playerState.durationMs > 0) {
        (playerState.currentPositionMs.toFloat() / playerState.durationMs).coerceIn(0f, 1f)
    } else 0f

    // UI geometry & notch / display cutout adaptive scaling
    val density = LocalDensity.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    // Safe padding for camera notch / display cutout
    val statusBarInsets = WindowInsets.statusBars.asPaddingValues()
    val cutoutInsets = WindowInsets.displayCutout.asPaddingValues()
    val topCutoutInset = maxOf(
        statusBarInsets.calculateTopPadding(),
        cutoutInsets.calculateTopPadding()
    )
    val topIconPadding = (topCutoutInset + 8.dp).coerceAtLeast(34.dp)

    // Dynamic container dimensions matching reference design
    val containerHeight = (screenHeight * 0.61f).coerceIn(470.dp, 600.dp)
    val containerWidth = (screenWidth * 0.73f).coerceIn(260.dp, 320.dp)
    val cornerRadius = containerWidth / 2f
    val gutterWidth = ((screenWidth - containerWidth) / 2f).coerceAtLeast(36.dp)

    // Concentric arc geometry
    val arcMargin = 16.dp
    val arcRadius = cornerRadius + arcMargin
    val arcRadiusPx = with(density) { arcRadius.toPx() }
    val containerHeightPx = with(density) { containerHeight.toPx() }
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }
    val arcTolerancePx = with(density) { 45.dp.toPx() }

    val headerBoxHeight = containerHeight + arcMargin + 10.dp

    // Scrubber interaction state
    var isDraggingArc by remember { mutableStateOf(false) }
    var localArcFraction by remember { mutableFloatStateOf(0f) }
    val displayArcFraction = if (isDraggingArc) localArcFraction else progressFraction

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)) // Minimalist Pearl Clean background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // =========================================================================
            // TOP ARCH CONTAINER & CONCENTRIC ARC SCRUBBER
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerBoxHeight),
                contentAlignment = Alignment.TopCenter
            ) {
                // 1. Central Arch Container (Extends to the top edge behind the notch)
                Box(
                    modifier = Modifier
                        .width(containerWidth)
                        .height(containerHeight)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(
                                topStart = 0.dp,
                                topEnd = 0.dp,
                                bottomStart = cornerRadius,
                                bottomEnd = cornerRadius
                            ),
                            spotColor = Color(0x33000000),
                            ambientColor = Color(0x1A000000)
                        )
                        .clip(
                            RoundedCornerShape(
                                topStart = 0.dp,
                                topEnd = 0.dp,
                                bottomStart = cornerRadius,
                                bottomEnd = cornerRadius
                            )
                        )
                        .background(Color(0xFF1E1128))
                        .clickable {
                            // Tap arch to dynamically cycle to another random media in current mode
                            if (isVideoMode) {
                                val candidates = VIDEO_POOL.filter { it != currentVideoRes }
                                currentVideoRes = if (candidates.isNotEmpty()) candidates.random() else VIDEO_POOL.random()
                            } else {
                                val candidates = ARTWORK_POOL.filter { it != currentArtworkRes }
                                currentArtworkRes = if (candidates.isNotEmpty()) candidates.random() else ARTWORK_POOL.random()
                            }
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Media background: Alternating Looping Muted Video or Artwork Image
                    if (isVideoMode) {
                        LoopingMutedVideoBackground(
                            videoResId = currentVideoRes,
                            isPlaying = playerState.isPlaying,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Crossfade(
                            targetState = currentArtworkRes,
                            animationSpec = tween(400),
                            label = "ArtworkFade"
                        ) { resId ->
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = "Track Artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Smooth dark gradient overlay at the bottom for crisp text legibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Transparent,
                                        0.50f to Color.Transparent,
                                        0.74f to Color(0x990A0A14),
                                        1.0f to Color(0xEE07070F)
                                    )
                                )
                            )
                    )

                    // Song title & artist centered inside the bottom curve of the arch
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp, start = 30.dp, end = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentFile?.name?.substringBeforeLast('.') ?: "Track Title",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Vaultia Cloud Master • ${currentFile?.formattedSize ?: "Audio"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.82f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 2. Interactive Concentric Arc Scrubber
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(playerState.durationMs) {
                            val cx = size.width / 2f
                            val cy = containerHeightPx - cornerRadiusPx
                            detectTapGestures { offset ->
                                val dist = hypot(offset.x - cx, offset.y - cy)
                                if (kotlin.math.abs(dist - arcRadiusPx) < arcTolerancePx) {
                                    val frac = ArcScrubberMath.calculateArcFraction(offset.x - cx, offset.y - cy)
                                    viewModel.seekTo((frac * playerState.durationMs).toLong())
                                }
                            }
                        }
                        .pointerInput(playerState.durationMs) {
                            val cx = size.width / 2f
                            val cy = containerHeightPx - cornerRadiusPx
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val dist = hypot(offset.x - cx, offset.y - cy)
                                    if (kotlin.math.abs(dist - arcRadiusPx) < arcTolerancePx * 1.5f) {
                                        isDraggingArc = true
                                        localArcFraction = ArcScrubberMath.calculateArcFraction(offset.x - cx, offset.y - cy)
                                    }
                                },
                                onDragEnd = {
                                    if (isDraggingArc) {
                                        viewModel.seekTo((localArcFraction * playerState.durationMs).toLong())
                                        isDraggingArc = false
                                    }
                                },
                                onDragCancel = {
                                    isDraggingArc = false
                                },
                                onDrag = { change, _ ->
                                    if (isDraggingArc) {
                                        change.consume()
                                        localArcFraction = ArcScrubberMath.calculateArcFraction(
                                            change.position.x - cx,
                                            change.position.y - cy
                                        )
                                    }
                                }
                            )
                        }
                ) {
                    val cx = size.width / 2f
                    val cy = containerHeightPx - cornerRadiusPx
                    val r = arcRadiusPx

                    // Inactive Arc Track (Sleek soft grey curve)
                    drawArc(
                        color = Color(0xFFE4E4EB),
                        startAngle = ArcScrubberMath.START_ANGLE,
                        sweepAngle = ArcScrubberMath.TOTAL_SWEEP,
                        useCenter = false,
                        topLeft = Offset(cx - r, cy - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(width = 3.6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Active Progress Arc Track
                    val activeSweep = ArcScrubberMath.TOTAL_SWEEP * displayArcFraction

                    // Neon violet ambient halo
                    drawArc(
                        color = Color(0xFF8B5CF6).copy(alpha = 0.35f),
                        startAngle = ArcScrubberMath.START_ANGLE,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = Offset(cx - r, cy - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Electric violet primary line
                    drawArc(
                        color = Color(0xFF6C47FF),
                        startAngle = ArcScrubberMath.START_ANGLE,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = Offset(cx - r, cy - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Scrubber Thumb with dual-ring violet neon glow
                    val currentAngleRad = Math.toRadians((ArcScrubberMath.START_ANGLE + activeSweep).toDouble())
                    val thumbX = (cx + r * cos(currentAngleRad)).toFloat()
                    val thumbY = (cy + r * sin(currentAngleRad)).toFloat()

                    // Thumb ambient glow
                    drawCircle(
                        color = Color(0xFF8B5CF6).copy(alpha = 0.45f),
                        radius = 13.dp.toPx(),
                        center = Offset(thumbX, thumbY)
                    )

                    // Thumb purple ring
                    drawCircle(
                        color = Color(0xFF6C47FF),
                        radius = 7.5.dp.toPx(),
                        center = Offset(thumbX, thumbY)
                    )

                    // Thumb white center dot
                    drawCircle(
                        color = Color.White,
                        radius = 3.5.dp.toPx(),
                        center = Offset(thumbX, thumbY)
                    )
                }

                // 3. Top Navigation Icons: Back `<` on left gutter, Menu `≡` on right gutter
                // Centered within the side gutter width, completely clear of the central arch and camera notch
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = topIconPadding)
                        .width(gutterWidth),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            viewModel.stopPlayback()
                            onBackClick()
                        },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                            contentDescription = "Back",
                            tint = Color(0xFF1E1E24),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = topIconPadding)
                        .width(gutterWidth),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { showQueueSheet = true },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Queue / Menu",
                            tint = Color(0xFF1E1E24),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // =========================================================================
            // TIME READOUT (Centered cleanly below arc curve)
            // =========================================================================
            val displayMs = if (isDraggingArc) {
                (displayArcFraction * playerState.durationMs).toLong()
            } else {
                playerState.currentPositionMs
            }

            Text(
                text = formatAudioTime(displayMs),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF18181B),
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // =========================================================================
            // TRANSPORT CONTROLS DOCK
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle button on the left
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) Color(0xFF6C47FF) else Color(0xFF18181B),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                // Floating Capsule Dock with overlapping glowing Play/Pause button
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Capsule Dock Background Pill
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(52.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(26.dp),
                                spotColor = Color(0x18000000),
                                ambientColor = Color(0x0C000000)
                            )
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color.White)
                            .padding(horizontal = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { viewModel.playPrevious() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = Color(0xFF18181B),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(58.dp)) // space for central play button

                            IconButton(
                                onClick = { viewModel.playNext() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    tint = Color(0xFF18181B),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Central Play/Pause button with glowing violet halo
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(
                                elevation = 14.dp,
                                shape = CircleShape,
                                spotColor = Color(0xFF8B5CF6),
                                ambientColor = Color(0xFF7C3AED)
                            )
                            .clip(CircleShape)
                            .background(Color(0xFF0F0F12))
                            .border(
                                width = 1.5.dp,
                                color = Color(0xFF8B5CF6).copy(alpha = 0.65f),
                                shape = CircleShape
                            )
                            .clickable { viewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                    ) {
                        if (playerState.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(18.dp))

                // Queue / Playlist button on the right
                IconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = "Queue",
                        tint = Color(0xFF18181B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // =========================================================================
        // QUEUE / PLAYLIST MODAL BOTTOM SHEET
        // =========================================================================
        if (showQueueSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF130A1F),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Queue / Playlist",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDBA74)
                            )
                            Text(
                                text = "${playlist.size} audio tracks available in cloud",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = { showQueueSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (playlist.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No other audio files found in library",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(playlist, key = { index, item -> "${item.id}_$index" }) { index, track ->
                                val isCurrent = track.id == currentFile?.id
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isCurrent) Color(0xFF8B5CF6).copy(alpha = 0.25f)
                                            else Color.White.copy(alpha = 0.05f)
                                        )
                                        .border(
                                            width = if (isCurrent) 1.2.dp else 0.5.dp,
                                            color = if (isCurrent) Color(0xFFA855F7) else Color.White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            viewModel.playFile(track)
                                            showQueueSheet = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isCurrent) Color(0xFFFDBA74)
                                                        else Color.White.copy(alpha = 0.1f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isCurrent && playerState.isPlaying) {
                                                    Icon(
                                                        imageVector = Icons.Default.Equalizer,
                                                        contentDescription = null,
                                                        tint = Color(0xFF451A03),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else {
                                                    Text(
                                                        text = "${index + 1}",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isCurrent) Color(0xFF451A03) else Color.White
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column {
                                                Text(
                                                    text = track.name.substringBeforeLast('.'),
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isCurrent) Color(0xFFFDBA74) else Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = track.formattedSize,
                                                    fontSize = 11.sp,
                                                    color = Color.White.copy(alpha = 0.5f)
                                                )
                                            }
                                        }

                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFA855F7))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text("PLAYING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun formatAudioTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
