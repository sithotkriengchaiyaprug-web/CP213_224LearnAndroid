package com.example.zerotouchbudget.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Design System: Refined Monochromatic Minimal ───────────────────────────

// Backgrounds
val Background    = Color(0xFFF5F5F8)   // Warm off-white screen background
val Surface       = Color(0xFFFFFFFF)   // Cards / elevated surfaces
val SurfaceVariant = Color(0xFFF0F0F5)  // Subtle surface distinction

// Text
val TextPrimary   = Color(0xFF0D0D14)   // Headings, amounts, primary text
val TextSecondary = Color(0xFF8C8CA1)   // Labels, metadata, hints

// Accent — Deep Indigo
val Accent        = Color(0xFF3730F5)   // Buttons, active states, links
val AccentLight   = Color(0xFFEEEDFF)   // Accent background container
val OnAccent      = Color(0xFFFFFFFF)   // Text/icons on accent bg

// Semantic
val Success       = Color(0xFF00C97A)   // Under-budget, positive, AI active
val SuccessLight  = Color(0xFFDFFFEF)   // Success background container
val Danger        = Color(0xFFE8433A)   // Over-budget, delete, error
val DangerLight   = Color(0xFFFFEDEC)   // Danger background container
val Warning       = Color(0xFFFF9F1C)   // Near-budget warning
val WarningLight  = Color(0xFFFFF4E0)   // Warning background container

// Budget Card (stays dark for premium feel)
val BudgetCardBg  = Color(0xFF0D0D14)   // Dark hero card background
val BudgetCardSurface = Color(0xFF1A1A26) // Slightly lighter for inner elements