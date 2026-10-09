package com.vaultia.app.core.theme

import androidx.compose.ui.graphics.Color

// Claymorphic Theme Palette - 2026 Modern Soft UI
val ClayPrimary = Color(0xFF6366F1)       // Indigo Accent
val ClayPrimaryVariant = Color(0xFF4F46E5)
val ClaySecondary = Color(0xFF06B6D4)     // Cyan Accent
val ClayAccentPink = Color(0xFFEC4899)
val ClayAccentEmerald = Color(0xFF10B981)
val ClayAccentAmber = Color(0xFFF59E0B)

// Light Clay Surface & Depth
val ClayBgLight = Color(0xFFF1F5F9)       // Soft Slate 100
val ClaySurfaceLight = Color(0xFFFFFFFF)
val ClayCardLight = Color(0xFFF8FAFC)
val ClayShadowDarkLight = Color(0xFFCBD5E1).copy(alpha = 0.6f)
val ClayShadowLightLight = Color(0xFFFFFFFF)

// Dark Clay Surface & Depth
val ClayBgDark = Color(0xFF0F172A)        // Deep Slate 900
val ClaySurfaceDark = Color(0xFF1E293B)   // Slate 800
val ClayCardDark = Color(0xFF1E293B)
val ClayShadowDarkDark = Color(0xFF020617).copy(alpha = 0.8f)
val ClayShadowLightDark = Color(0xFF334155).copy(alpha = 0.4f)

// Text Colors
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF64748B)
val TextTertiaryLight = Color(0xFF94A3B8)

val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextTertiaryDark = Color(0xFF64748B)

// Functional Colors
val StatusSuccess = Color(0xFF10B981)
val StatusWarning = Color(0xFFF59E0B)
val StatusError = Color(0xFFEF4444)
val StatusInfo = Color(0xFF3B82F6)
