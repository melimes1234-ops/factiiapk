package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// --- TAILWIND CSS COLOR SYSTEM PALETTE ---
object TailwindColors {
    // Slate
    val Slate50 = Color(0xFFF8FAFC)
    val Slate100 = Color(0xFFF1F5F9)
    val Slate200 = Color(0xFFE2E8F0)
    val Slate300 = Color(0xFFCBD5E1)
    val Slate400 = Color(0xFF94A3B8)
    val Slate500 = Color(0xFF64748B)
    val Slate600 = Color(0xFF475569)
    val Slate700 = Color(0xFF334155)
    val Slate800 = Color(0xFF1E293B)
    val Slate900 = Color(0xFF0F172A)
    val Slate950 = Color(0xFF020617)

    // Indigo (Brand)
    val Indigo50 = Color(0xFFEEF2FF)
    val Indigo100 = Color(0xFFE0E7FF)
    val Indigo200 = Color(0xFFC7D2FE)
    val Indigo500 = Color(0xFF6366F1)
    val Indigo600 = Color(0xFF4F46E5)
    val Indigo700 = Color(0xFF4338CA)
    val Indigo900 = Color(0xFF312E81)

    // Emerald (Success)
    val Emerald50 = Color(0xFFECFDF5)
    val Emerald100 = Color(0xFFD1FAE5)
    val Emerald500 = Color(0xFF10B981)
    val Emerald600 = Color(0xFF059669)
    val Emerald900 = Color(0xFF064E3B)

    // Amber (Warning)
    val Amber50 = Color(0xFFFFFBEB)
    val Amber100 = Color(0xFFFEF3C7)
    val Amber500 = Color(0xFFF59E0B)
    val Amber600 = Color(0xFFD97706)
    val Amber900 = Color(0xFF78350F)

    // Rose (Danger / Cancelled)
    val Rose50 = Color(0xFFFFF1F2)
    val Rose100 = Color(0xFFFFE4E6)
    val Rose500 = Color(0xFFF43F5E)
    val Rose600 = Color(0xFFE11D48)
    val Rose900 = Color(0xFF4C0519)

    // Cyan (Pending / Refined Blue)
    val Cyan50 = Color(0xFFECFEFF)
    val Cyan100 = Color(0xFFCFFAFE)
    val Cyan500 = Color(0xFF06B6D4)
    val Cyan600 = Color(0xFF0891B2)
}

// Retro-compatibility fields mapping to Tailwind values to avoid breakage
val Purple80 = TailwindColors.Indigo200
val PurpleGrey80 = TailwindColors.Slate400
val Pink80 = TailwindColors.Rose100

val Purple40 = TailwindColors.Indigo600
val PurpleGrey40 = TailwindColors.Slate600
val Pink40 = TailwindColors.Rose600

// High Density Theme definitions (using Tailwind Colors)
val HighDensityPrimary = TailwindColors.Indigo600
val HighDensityOnPrimary = Color.White
val HighDensityPrimaryContainer = TailwindColors.Indigo100
val HighDensityOnPrimaryContainer = TailwindColors.Indigo700

val HighDensityBackground = TailwindColors.Slate50
val HighDensityOnBackground = TailwindColors.Slate900

val HighDensitySurface = Color.White
val HighDensityOnSurface = TailwindColors.Slate900
val HighDensitySurfaceVariant = TailwindColors.Slate100
val HighDensityOnSurfaceVariant = TailwindColors.Slate500

val HighDensityOutline = TailwindColors.Slate200

// Dark Mode Colors mapping to Tailwind Colors
val HighDensityPrimaryDark = TailwindColors.Indigo500
val HighDensityOnPrimaryDark = TailwindColors.Slate950
val HighDensityPrimaryContainerDark = TailwindColors.Slate900
val HighDensityOnPrimaryContainerDark = TailwindColors.Indigo100

val HighDensityBackgroundDark = TailwindColors.Slate950
val HighDensityOnBackgroundDark = TailwindColors.Slate100

val HighDensitySurfaceDark = TailwindColors.Slate900
val HighDensityOnSurfaceDark = TailwindColors.Slate100
val HighDensitySurfaceVariantDark = TailwindColors.Slate800
val HighDensityOnSurfaceVariantDark = TailwindColors.Slate300

val HighDensityOutlineDark = TailwindColors.Slate700

// Status Colors
val HighDensitySuccess = TailwindColors.Emerald600
val HighDensitySuccessContainer = TailwindColors.Emerald100

val HighDensityWarning = TailwindColors.Amber600
val HighDensityWarningContainer = TailwindColors.Amber100

val HighDensityError = TailwindColors.Rose600
val HighDensityErrorContainer = TailwindColors.Rose100

val HighDensityNeutral = TailwindColors.Slate500
val HighDensityNeutralContainer = TailwindColors.Slate200

