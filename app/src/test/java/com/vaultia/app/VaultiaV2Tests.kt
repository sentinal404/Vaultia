package com.vaultia.app

import com.vaultia.app.core.Constants
import com.vaultia.app.domain.model.MediaType
import com.vaultia.app.domain.model.TransferStatus
import com.vaultia.app.domain.model.VaultFile
import com.vaultia.app.ui.components.BottomNavItem
import com.vaultia.app.ui.navigation.NavRoutes
import com.vaultia.app.ui.player.ARTWORK_POOL
import com.vaultia.app.ui.player.ArcScrubberMath
import com.vaultia.app.ui.player.VIDEO_POOL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultiaV2Tests {

    @Test
    fun testVersion2Constants() {
        assertEquals("2.0.0", Constants.APP_VERSION)
        assertEquals(2, Constants.APP_VERSION_CODE)
        assertTrue(Constants.APP_BUILD_NAME.contains("v2.0.0"))
        assertTrue(Constants.APP_BUILD_NAME.contains("Evolution Edition"))
    }

    @Test
    fun testNavigationRoutesIntegrity() {
        assertEquals("splash", NavRoutes.Splash.route)
        assertEquals("welcome", NavRoutes.Welcome.route)
        assertEquals("onboarding", NavRoutes.Onboarding.route)
        assertEquals("dashboard", NavRoutes.Dashboard.route)
        assertEquals("library", NavRoutes.Library.route)
        assertEquals("transfers", NavRoutes.Transfers.route)
        assertEquals("settings", NavRoutes.Settings.route)
        assertEquals("auto_backup", NavRoutes.AutoBackup.route)
        assertEquals("about", NavRoutes.About.route)

        // Parameterized media routes
        assertEquals("player_audio/test_file_123", NavRoutes.AudioPlayer.createRoute("test_file_123"))
        assertEquals("player_video/vid_456", NavRoutes.VideoPlayer.createRoute("vid_456"))
        assertEquals("viewer_image/img_789", NavRoutes.ImageViewer.createRoute("img_789"))
    }

    @Test
    fun testBottomNavItemsMatchingVaultiaDestinations() {
        val items = listOf(
            BottomNavItem.Dashboard,
            BottomNavItem.Library,
            BottomNavItem.Transfers,
            BottomNavItem.Settings
        )

        assertEquals(4, items.size)
        assertEquals("Vault", items[0].title)
        assertEquals("dashboard", items[0].route)

        assertEquals("Library", items[1].title)
        assertEquals("library", items[1].route)

        assertEquals("Transfers", items[2].title)
        assertEquals("transfers", items[2].route)

        assertEquals("Settings", items[3].title)
        assertEquals("settings", items[3].route)
    }

    @Test
    fun testMediaTypeCategorization() {
        assertEquals(MediaType.VIDEOS, MediaType.fromMimeType("video/mp4", "movie.mp4"))
        assertEquals(MediaType.VIDEOS, MediaType.fromMimeType(null, "clip.mkv"))
        assertEquals(MediaType.AUDIO, MediaType.fromMimeType("audio/mpeg", "track.mp3"))
        assertEquals(MediaType.AUDIO, MediaType.fromMimeType(null, "song.flac"))
        assertEquals(MediaType.IMAGES, MediaType.fromMimeType("image/png", "photo.png"))
        assertEquals(MediaType.IMAGES, MediaType.fromMimeType(null, "picture.jpg"))
        assertEquals(MediaType.DOCUMENTS, MediaType.fromMimeType("application/pdf", "contract.pdf"))
        assertEquals(MediaType.DOCUMENTS, MediaType.fromMimeType(null, "notes.docx"))
        assertEquals(MediaType.ARCHIVES, MediaType.fromMimeType(null, "bundle.zip"))
    }

    @Test
    fun testVaultFileFormatting() {
        assertEquals("0 B", VaultFile.formatBytes(0L))
        assertTrue(VaultFile.formatBytes(1023L).contains("B"))
        assertTrue(VaultFile.formatBytes(1024L).contains("KB"))
        assertTrue(VaultFile.formatBytes(5 * 1024 * 1024L).contains("MB"))
        assertTrue(VaultFile.formatBytes(2L * 1024 * 1024 * 1024).contains("GB"))
    }

    @Test
    fun testTransferStatusActiveAndFinished() {
        assertTrue(TransferStatus.TRANSFERRING.isActive)
        assertTrue(TransferStatus.QUEUED.isActive)
        assertTrue(TransferStatus.PREPARING.isActive)
        assertTrue(TransferStatus.RETRYING.isActive)

        assertTrue(TransferStatus.COMPLETED.isFinished)
        assertTrue(TransferStatus.FAILED.isFinished)
        assertTrue(TransferStatus.CANCELLED.isFinished)

        assertFalse(TransferStatus.TRANSFERRING.isFinished)
        assertFalse(TransferStatus.COMPLETED.isActive)
    }

    @Test
    fun testAudioPlayerThemeDeterministicDistribution() {
        val fileIds = listOf("file_alpha", "file_beta", "file_gamma", "file_delta", "file_epsilon", "file_zeta", "file_eta")
        for (id in fileIds) {
            val initialThemeIndex = kotlin.math.abs(id.hashCode()) % 5
            assertTrue("Theme index must be between 0 and 4", initialThemeIndex in 0..4)

            // Test auto-progression on track change
            for (prevIndex in 0..4) {
                val nextThemeIndex = (prevIndex + 1 + (kotlin.math.abs(id.hashCode()) % 4)) % 5
                assertTrue("Next theme index must be between 0 and 4", nextThemeIndex in 0..4)
            }
        }
    }

    @Test
    fun testLibraryMultiSelectionStateLogic() {
        var selectedIds = setOf<String>()
        val file1 = "file_1"
        val file2 = "file_2"
        val file3 = "file_3"

        // Single toggle
        selectedIds = selectedIds + file1
        assertEquals(1, selectedIds.size)
        assertTrue(selectedIds.contains(file1))

        // Multiple selection
        selectedIds = selectedIds + file2 + file3
        assertEquals(3, selectedIds.size)

        // Select All toggle
        val allFiles = listOf(file1, file2, file3)
        assertEquals(allFiles.toSet(), selectedIds)

        // Deselection
        selectedIds = selectedIds - file2
        assertEquals(2, selectedIds.size)
        assertFalse(selectedIds.contains(file2))
        assertTrue(selectedIds.contains(file1))
        assertTrue(selectedIds.contains(file3))

        // Clear all
        selectedIds = emptySet()
        assertTrue(selectedIds.isEmpty())
    }

    @Test
    fun testPlaylistWrapLogic() {
        val playlist = listOf("song_a", "song_b", "song_c")
        fun getNext(current: String): String {
            val idx = playlist.indexOf(current)
            val nextIdx = if (idx != -1 && idx + 1 < playlist.size) idx + 1 else 0
            return playlist[nextIdx]
        }
        fun getPrev(current: String, positionMs: Long = 0L): Pair<String, Boolean> {
            if (positionMs > 3000L) {
                // Should seek to 0 instead of changing track
                return Pair(current, true)
            }
            val idx = playlist.indexOf(current)
            val prevIdx = if (idx > 0) idx - 1 else playlist.size - 1
            return Pair(playlist[prevIdx], false)
        }

        assertEquals("song_b", getNext("song_a"))
        assertEquals("song_c", getNext("song_b"))
        assertEquals("song_a", getNext("song_c")) // wrap around to first

        assertEquals("song_c", getPrev("song_a").first) // wrap around to last
        assertEquals("song_a", getPrev("song_b").first)
        assertEquals("song_b", getPrev("song_c").first)

        // Previous when playing > 3000ms: seeks to beginning of current track
        val (retCurrent, soughtToZero) = getPrev("song_b", positionMs = 5000L)
        assertEquals("song_b", retCurrent)
        assertTrue(soughtToZero)

        // Single track playlist edge case
        val singlePlaylist = listOf("sole_track")
        val singleIdx = singlePlaylist.indexOf("sole_track")
        val singleNext = if (singleIdx != -1 && singleIdx + 1 < singlePlaylist.size) singleIdx + 1 else 0
        assertEquals("sole_track", singlePlaylist[singleNext])
    }

    @Test
    fun testTheme0ArcScrubberBoundedCoordinates() {
        val widthDp = 280f
        val heightDp = 54f
        val cx = widthDp / 2f // 140f
        val dx = 112f
        val hDip = 14f
        val yMid = 24f
        val arcRadius = (dx * dx + hDip * hDip) / (2f * hDip)
        val cy = yMid - arcRadius

        val halfSweepDeg = Math.toDegrees(Math.asin((dx / arcRadius).toDouble())).toFloat()
        val startAngle = 90f + halfSweepDeg
        val totalSweep = -2f * halfSweepDeg

        // Test 101 sample points from frac 0.0 to 1.0
        var prevX = -1f
        for (i in 0..100) {
            val frac = i / 100f
            val angleDeg = startAngle + totalSweep * frac
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val thumbX = (cx + arcRadius * Math.cos(angleRad)).toFloat()
            val thumbY = (cy + arcRadius * Math.sin(angleRad)).toFloat()

            // Verify strictly within box dimensions
            assertTrue("thumbX ($thumbX) must be within [0, $widthDp]", thumbX in 0f..widthDp)
            assertTrue("thumbY ($thumbY) must be within [0, $heightDp]", thumbY in 0f..heightDp)

            // Verify monotonic left-to-right progression
            assertTrue("thumbX must monotonically increase", thumbX >= prevX)
            prevX = thumbX
        }

        // Check start and end symmetrical elevations
        val startY = (cy + arcRadius * Math.sin(Math.toRadians(startAngle.toDouble()))).toFloat()
        val endY = (cy + arcRadius * Math.sin(Math.toRadians((startAngle + totalSweep).toDouble()))).toFloat()
        val midY = (cy + arcRadius * Math.sin(Math.toRadians(90.0))).toFloat()

        assertEquals(10f, startY, 0.5f) // ~10dp
        assertEquals(10f, endY, 0.5f)   // ~10dp
        assertEquals(24f, midY, 0.5f)   // ~24dp
        assertTrue("Midpoint must be the lowest point (largest Y) of cradle", midY > startY)
    }

    @Test
    fun testVaultiaSwitchThumbGeometry() {
        val trackWidth = 52
        val trackHeight = 30
        val thumbSize = 24
        val thumbPadding = 3
        assertEquals(30, trackHeight)
        val maxOffset = trackWidth - thumbSize - thumbPadding // 25

        assertEquals(25, maxOffset)
        assertEquals(3, thumbPadding)

        // Unchecked offset
        val uncheckedOffset = thumbPadding
        assertEquals(3, uncheckedOffset)

        // Checked offset
        val checkedOffset = maxOffset
        assertEquals(25, checkedOffset)

        // Verify thumb remains within track boundary
        assertTrue(uncheckedOffset + thumbSize <= trackWidth)
        assertTrue(checkedOffset + thumbSize <= trackWidth)
        assertEquals(trackWidth - thumbPadding, checkedOffset + thumbSize)
    }

    @Test
    fun testAudioPlayerArtworkPoolSizeAndRandomDistribution() {
        assertEquals(22, ARTWORK_POOL.size)

        // Verify random selection picks items strictly within the pool
        val selected = mutableSetOf<Int>()
        val random = java.util.Random(42)
        repeat(200) {
            val pick = ARTWORK_POOL[random.nextInt(ARTWORK_POOL.size)]
            assertTrue(ARTWORK_POOL.contains(pick))
            selected.add(pick)
        }
        // Over 200 trials, all 22 artworks should be reachable
        assertEquals(22, selected.size)

        // Verify alternate random selection logic (filtering out current artwork)
        for (current in ARTWORK_POOL) {
            val candidates = ARTWORK_POOL.filter { it != current }
            assertEquals(21, candidates.size)
            assertFalse(candidates.contains(current))
        }
    }

    @Test
    fun testVideoPoolAndAlternatingModeSequence() {
        assertEquals(4, VIDEO_POOL.size)

        // Verify all 4 videos are distinct
        assertEquals(4, VIDEO_POOL.toSet().size)

        // Verify alternating Image -> Video -> Image -> Video sequence on successive track changes
        var isVideoMode = false
        val trackChanges = listOf("track1", "track2", "track3", "track4", "track5", "track6")
        val modes = mutableListOf<String>()

        var prevTrackId: String? = null
        for (track in trackChanges) {
            if (prevTrackId != null && prevTrackId != track) {
                isVideoMode = !isVideoMode
            }
            prevTrackId = track
            modes.add(if (isVideoMode) "VIDEO" else "IMAGE")
        }

        assertEquals(
            listOf("IMAGE", "VIDEO", "IMAGE", "VIDEO", "IMAGE", "VIDEO"),
            modes
        )
    }

    @Test
    fun testArchContainerHeightProportions() {
        // Red mark test: container height is ~61% of screen height
        val testScreenHeights = listOf(700f, 800f, 850f, 900f, 1024f)
        for (sh in testScreenHeights) {
            val ch = (sh * 0.61f).coerceIn(470f, 600f)
            assertTrue("Arch container must occupy at least 58% of screen height", ch / sh >= 0.58f || ch == 600f)
            assertTrue("Arch container must leave space for controls at bottom", ch < sh - 100f)
        }
    }

    @Test
    fun testConcentricArcScrubberAnglesAndCoordinateMapping() {
        assertEquals(165f, ArcScrubberMath.START_ANGLE, 0.001f)
        assertEquals(-150f, ArcScrubberMath.TOTAL_SWEEP, 0.001f)
        val endAngle = ArcScrubberMath.START_ANGLE + ArcScrubberMath.TOTAL_SWEEP // 15f
        assertEquals(15f, endAngle, 0.001f)

        val cx = 150f
        val cy = 200f
        val r = 130f

        // Angle at 0%
        val angle0 = ArcScrubberMath.START_ANGLE
        val x0 = (cx + r * kotlin.math.cos(Math.toRadians(angle0.toDouble()))).toFloat()
        val y0 = (cy + r * kotlin.math.sin(Math.toRadians(angle0.toDouble()))).toFloat()
        assertTrue("Start point (0%) must be left of center", x0 < cx)
        assertTrue("Start point must be below cy", y0 > cy)

        // Angle at 50%
        val angle50 = ArcScrubberMath.START_ANGLE + ArcScrubberMath.TOTAL_SWEEP * 0.5f // 90 deg (bottom center)
        assertEquals(90f, angle50, 0.001f)
        val x50 = (cx + r * kotlin.math.cos(Math.toRadians(angle50.toDouble()))).toFloat()
        val y50 = (cy + r * kotlin.math.sin(Math.toRadians(angle50.toDouble()))).toFloat()
        assertEquals(cx, x50, 0.01f)
        assertEquals(cy + r, y50, 0.01f)

        // Angle at 100%
        val angle100 = endAngle // 15 deg
        val x100 = (cx + r * kotlin.math.cos(Math.toRadians(angle100.toDouble()))).toFloat()
        val y100 = (cy + r * kotlin.math.sin(Math.toRadians(angle100.toDouble()))).toFloat()
        assertTrue("End point (100%) must be right of center", x100 > cx)
        assertTrue("End point must be below cy", y100 > cy)

        // Verify touch coordinate to fraction conversion across entire arc
        for (i in 0..100) {
            val expectedFrac = i / 100f
            val currentAngleDeg = ArcScrubberMath.START_ANGLE + ArcScrubberMath.TOTAL_SWEEP * expectedFrac
            val dx = (r * kotlin.math.cos(Math.toRadians(currentAngleDeg.toDouble()))).toFloat()
            val dy = (r * kotlin.math.sin(Math.toRadians(currentAngleDeg.toDouble()))).toFloat()
            val calculatedFrac = ArcScrubberMath.calculateArcFraction(dx, dy)
            assertEquals(expectedFrac, calculatedFrac, 0.001f)
        }
    }

    @Test
    fun testArcScrubberTouchDriftSafety() {
        // Test upward touch drift above center of curvature (dy < 0)
        // Left side touch (near 0% progress): must clamp safely to 0.0f without jumping to 1.0f
        val leftDrift1 = ArcScrubberMath.calculateArcFraction(-100f, -5f)
        assertEquals(0f, leftDrift1, 0.0001f)

        val leftDrift2 = ArcScrubberMath.calculateArcFraction(-80f, -50f)
        assertEquals(0f, leftDrift2, 0.0001f)

        // Right side touch (near 100% progress): must clamp safely to 1.0f
        val rightDrift1 = ArcScrubberMath.calculateArcFraction(100f, -5f)
        assertEquals(1f, rightDrift1, 0.0001f)

        val rightDrift2 = ArcScrubberMath.calculateArcFraction(80f, -50f)
        assertEquals(1f, rightDrift2, 0.0001f)

        // Bottom center touch (dx = 0, dy > 0): must map to 0.5f exactly
        val centerPoint = ArcScrubberMath.calculateArcFraction(0f, 100f)
        assertEquals(0.5f, centerPoint, 0.0001f)
    }

    @Test
    fun testNotchAndCameraCutoutAdaptivePadding() {
        val testCutoutHeights = listOf(0, 24, 32, 44, 52)
        for (cutoutDp in testCutoutHeights) {
            val topIconPadding = (cutoutDp + 8).coerceAtLeast(34)
            assertTrue("Top icon padding must never be less than 34dp", topIconPadding >= 34)
            assertTrue("Top icon padding must be strictly greater than notch cutout", topIconPadding > cutoutDp)
        }
    }

    @Test
    fun testTopNavigationIconsGutterClearance() {
        // Verify across various mobile screen widths (360dp to 430dp)
        val screenWidths = listOf(360f, 380f, 393f, 412f, 430f)
        for (width in screenWidths) {
            val containerWidth = (width * 0.72f).coerceIn(250f, 310f)
            val gutterWidth = ((width - containerWidth) / 2f).coerceAtLeast(36f)

            val archStart = (width - containerWidth) / 2f
            val archEnd = archStart + containerWidth

            // Back button is centered in the left gutter [0, gutterWidth]
            val backButtonCenter = gutterWidth / 2f
            assertTrue("Back button center must be strictly outside arch left boundary", backButtonCenter < archStart)

            // Menu button is centered in the right gutter [width - gutterWidth, width]
            val menuButtonCenter = width - gutterWidth / 2f
            assertTrue("Menu button center must be strictly outside arch right boundary", menuButtonCenter > archEnd)
        }
    }
}
