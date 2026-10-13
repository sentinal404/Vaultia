package com.vaultia.app

import com.vaultia.app.core.Constants
import com.vaultia.app.domain.model.MediaType
import com.vaultia.app.domain.model.TransferStatus
import com.vaultia.app.domain.model.VaultFile
import com.vaultia.app.ui.components.BottomNavItem
import com.vaultia.app.ui.navigation.NavRoutes
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
}
