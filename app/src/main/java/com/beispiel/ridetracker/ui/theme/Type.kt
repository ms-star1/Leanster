package com.beispiel.ridetracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.beispiel.ridetracker.R

// Rajdhani for Telemetry Data & Numbers
val Rajdhani = FontFamily(
    // Fallback to system fonts if the specific ones aren't available yet
    // In a real scenario, you'd add Rajdhani-Bold.ttf etc. to res/font
    Font(R.font.michroma_regular, FontWeight.Bold) 
)

// Inter for Menus, Labels & Body Text
val Inter = FontFamily.SansSerif

// Custom Typography for RideTracker
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 84.sp,
        color = PureWhite
    ),
    displayMedium = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.Bold,
        fontSize = 54.sp,
        color = PureWhite
    ),
    headlineMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        color = PureWhite
    ),
    titleLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = PureWhite
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = PureWhite
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = MutedGrey
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = MutedGrey
    )
)
