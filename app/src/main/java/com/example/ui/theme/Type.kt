package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Display face: Unbounded — headlines, big numbers, primary CTA labels
val Unbounded = FontFamily(
  Font(R.font.unbounded_medium, FontWeight.Medium),
  Font(R.font.unbounded_bold, FontWeight.Bold),
)

// Body face: Manrope — everything else
val Manrope = FontFamily(
  Font(R.font.manrope_regular, FontWeight.Normal),
  Font(R.font.manrope_medium, FontWeight.Medium),
  Font(R.font.manrope_semibold, FontWeight.SemiBold),
  Font(R.font.manrope_bold, FontWeight.Bold),
  Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
  Font(R.font.manrope_extrabold, FontWeight.Black),
)

private val base = Typography()

val Typography =
  Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Unbounded, fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontFamily = Unbounded, fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontFamily = Unbounded, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Unbounded, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Unbounded, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Unbounded, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold),
    titleMedium = base.titleMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
    bodyLarge =
      TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp,
      ),
    bodyMedium = base.bodyMedium.copy(fontFamily = Manrope),
    bodySmall = base.bodySmall.copy(fontFamily = Manrope),
    labelLarge = base.labelLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
    labelSmall = base.labelSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
  )

// Shared text styles from the mockups
val ApexScreenTitle = TextStyle(
  fontFamily = Unbounded,
  fontWeight = FontWeight.Bold,
  fontSize = 26.sp,
  lineHeight = 30.sp,
  letterSpacing = (-0.2).sp,
)

val ApexSectionLabel = TextStyle(
  fontFamily = Manrope,
  fontWeight = FontWeight.ExtraBold,
  fontSize = 12.sp,
  letterSpacing = 1.sp,
)

val ApexBigNumber = TextStyle(
  fontFamily = Unbounded,
  fontWeight = FontWeight.Bold,
  fontSize = 22.sp,
)
