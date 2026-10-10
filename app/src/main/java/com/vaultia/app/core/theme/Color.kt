package com.vaultia.app.core.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// PHASE 3 — EXTREMELY PREMIUM COLOR SYSTEM
// =========================================================================

// A. Default Light Theme: PEARL TITANIUM LUXURY (Mandatory first-launch theme)
val PearlBgMain = Color(0xFFF7F6F2)
val PearlBgSecondary = Color(0xFFEFEEE9)
val PearlSurfaceElevated = Color(0xFFFFFFFF)
val PearlSurfaceTint = Color(0xFFF4F1FF)
val PearlTextPrimary = Color(0xFF191A28)
val PearlTextSecondary = Color(0xFF727487)
val PearlPrimaryViolet = Color(0xFF6C4CE0)
val PearlDeepViolet = Color(0xFF5031B8)
val PearlElectricLavender = Color(0xFFB4A0FF)
val PearlIceCyan = Color(0xFF43C9D9)
val PearlSoftChampagne = Color(0xFFD5B77B)
val PearlBorderSubtle = Color(0xFFE6E3DC)
val PearlSuccessEmerald = Color(0xFF13896A)
val PearlWarningAmber = Color(0xFFC38A32)
val PearlErrorCrimson = Color(0xFFC74759)

// B. Dark Theme: OBSIDIAN AURORA
val ObsidianBgMain = Color(0xFF0B0D13)
val ObsidianBgSecondary = Color(0xFF11141E)
val ObsidianSurfaceElevated = Color(0xFF191D2A)
val ObsidianSurfaceSecondary = Color(0xFF202437)
val ObsidianTextPrimary = Color(0xFFF5F3FC)
val ObsidianTextSecondary = Color(0xFFA5A9BC)
val ObsidianPrimaryLavender = Color(0xFFA38AFF)
val ObsidianElectricCyan = Color(0xFF53DCE8)
val ObsidianVioletGlow = Color(0xFF7547FF)
val ObsidianChampagneAccent = Color(0xFFD7B77A)
val ObsidianBorder = Color(0xFF2B3042)
val ObsidianSuccessEmerald = Color(0xFF3AC6A0)
val ObsidianWarningAmber = Color(0xFFF59E0B)
val ObsidianErrorCrimson = Color(0xFFF43F5E)

// Legacy alias mappings for backwards compatibility across existing composables
val VioletDuskPlum = PearlDeepViolet
val VioletDuskRose = PearlPrimaryViolet
val VioletDuskPeach = PearlSoftChampagne
val VioletDuskCream = PearlBgMain
val VioletDuskNeonPink = PearlElectricLavender
val VioletDuskBg = ObsidianBgMain
val VioletDuskSurface = ObsidianSurfaceElevated
val VioletDuskCard = ObsidianSurfaceSecondary
val VioletDuskCardElevated = Color(0xFF282D42)
val VioletDuskBorder = ObsidianBorder

val ClayPrimary = PearlPrimaryViolet
val ClayPrimaryVariant = PearlDeepViolet
val ClaySecondary = PearlIceCyan
val ClayCyan = PearlIceCyan
val ClayAccentPink = PearlElectricLavender
val ClayAccentEmerald = PearlSuccessEmerald
val ClayAccentAmber = PearlSoftChampagne

val ClayBgLight = PearlBgMain
val ClaySurfaceLight = PearlSurfaceElevated
val ClayCardLight = PearlBgSecondary
val ClayShadowDarkLight = Color(0xFFD8CFBE).copy(alpha = 0.5f)
val ClayShadowLightLight = Color(0xFFFFFFFF)

val ClayBgDark = ObsidianBgMain
val ClaySurfaceDark = ObsidianSurfaceElevated
val ClayCardDark = ObsidianSurfaceSecondary
val ClayShadowDarkDark = Color(0xFF04060A).copy(alpha = 0.9f)
val ClayShadowLightDark = ObsidianVioletGlow.copy(alpha = 0.25f)

val TextPrimaryLight = PearlTextPrimary
val TextSecondaryLight = PearlTextSecondary
val TextTertiaryLight = Color(0xFF9E9FAE)

val TextPrimaryDark = ObsidianTextPrimary
val TextSecondaryDark = ObsidianTextSecondary
val TextTertiaryDark = Color(0xFF7E8398)

val StatusSuccess = PearlSuccessEmerald
val StatusWarning = PearlWarningAmber
val StatusError = PearlErrorCrimson
val StatusInfo = PearlIceCyan
