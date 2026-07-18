package com.snehil.auracode.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.snehil.auracode.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val spaceGrotesk = GoogleFont("Space Grotesk")
private val manrope = GoogleFont("Manrope")
private val jetBrainsMono = GoogleFont("JetBrains Mono")

// Headings — Space Grotesk.
val DisplayFontFamily = FontFamily(
    Font(googleFont = spaceGrotesk, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = spaceGrotesk, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = spaceGrotesk, fontProvider = provider, weight = FontWeight.Bold)
)

// Body — Manrope.
val BodyFontFamily = FontFamily(
    Font(googleFont = manrope, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = manrope, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = manrope, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = manrope, fontProvider = provider, weight = FontWeight.Bold)
)

// Code — JetBrains Mono.
val MonoFontFamily = FontFamily(
    Font(googleFont = jetBrainsMono, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = jetBrainsMono, fontProvider = provider, weight = FontWeight.Medium)
)

private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = BodyFontFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = BodyFontFamily),
    bodySmall = base.bodySmall.copy(fontFamily = BodyFontFamily),
    labelLarge = base.labelLarge.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Medium),
    labelMedium = base.labelMedium.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

val CodeTextStyle = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 20.sp
)
