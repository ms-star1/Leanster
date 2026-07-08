package com.beispiel.ridetracker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beispiel.ridetracker.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun DisclaimerScreen(
    highlightColor: Color,
    onAccepted: () -> Unit
) {
    var timerSeconds by remember { mutableIntStateOf(5) }
    var checked by remember { mutableStateOf(false) }
    val canAccept = timerSeconds == 0 && checked
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        repeat(5) {
            delay(1000L)
            timerSeconds -= 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCarbon)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Scrollable body ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Spacer(Modifier.height(48.dp))

                // Header
                Text(
                    "BEFORE YOU RIDE",
                    fontSize = 11.sp, letterSpacing = 3.sp,
                    color = highlightColor, fontFamily = Inter,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Please read this before using RideTracker",
                    fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    color = PureWhite, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(), lineHeight = 26.sp
                )
                Spacer(Modifier.height(28.dp))
                HorizontalDivider(color = BorderDivider)
                Spacer(Modifier.height(24.dp))

                DisclaimerSection(
                    number = "1",
                    title = "Measurement Accuracy",
                    highlightColor = highlightColor,
                    body = "RideTracker estimates lean angle, pitch, and speed using your device's built-in sensors and GPS. " +
                           "All values are approximations, subject to the quality and placement of the hardware and the accuracy " +
                           "of the calibration. This app does not produce certified measurements and must not be relied upon " +
                           "for engineering analysis, legal proceedings, insurance claims, or any purpose that requires " +
                           "validated instrumentation."
                )

                DisclaimerSection(
                    number = "2",
                    title = "Calibration Requirement",
                    highlightColor = highlightColor,
                    body = "Accurate readings require correct calibration every time the device is mounted, repositioned, " +
                           "or after an orientation change (portrait ↔ landscape). Always complete the full calibration " +
                           "procedure before recording a session. Skipping or incorrectly performing calibration will " +
                           "result in unreliable data. A calibration guide will follow after you accept these terms."
                )

                DisclaimerSection(
                    number = "3",
                    title = "Safety",
                    highlightColor = highlightColor,
                    body = "Motorcycling is an inherently high-risk activity. Always ride within your personal skill level, " +
                           "comply with all applicable traffic laws and speed limits in your location, and wear appropriate " +
                           "protective equipment at all times. Never interact with this app while in motion — this is a " +
                           "data-logging tool only. All data review must be done when stationary."
                )

                DisclaimerSection(
                    number = "4",
                    title = "Limitation of Liability",
                    highlightColor = highlightColor,
                    body = "To the fullest extent permitted by applicable law: this application, its developers, and all " +
                           "associated parties provide this software \"as is\" and without warranty of any kind. No " +
                           "responsibility is accepted for any injury, loss of life, property damage, vehicle damage, " +
                           "financial loss, legal consequences, penalties, or any other harm arising from the use, " +
                           "misuse, or inability to use this application. By continuing, you acknowledge that " +
                           "motorcycle riding is a high-risk activity and accept full personal responsibility for " +
                           "your own behaviour."
                )

                DisclaimerSection(
                    number = "5",
                    title = "Responsible Riding",
                    highlightColor = highlightColor,
                    body = "This app and all parties involved in its development actively discourage unsafe or unlawful " +
                           "riding on public roads. RideTracker is intended solely as a training and data analysis " +
                           "tool for responsible use. Ride safely, ride within your limits, and respect others."
                )

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = BorderDivider)
                Spacer(Modifier.height(32.dp))
            }

            // ── Fixed bottom ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1118))
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Checkbox row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = highlightColor,
                            uncheckedColor = MutedGrey,
                            checkmarkColor = Color(0xFF070908)
                        )
                    )
                    Text(
                        "I have read and understood this disclaimer",
                        fontSize = 13.sp, color = if (checked) PureWhite else MutedGrey,
                        fontFamily = Inter, lineHeight = 18.sp
                    )
                }

                // Timer hint
                if (timerSeconds > 0) {
                    Text(
                        "Please continue reading — $timerSeconds second${if (timerSeconds == 1) "" else "s"} remaining",
                        fontSize = 11.sp, color = MutedGrey, fontFamily = Inter,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )
                }

                // OK button
                Button(
                    onClick = onAccepted,
                    enabled = canAccept,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = highlightColor,
                        disabledContainerColor = SurfaceCard,
                        contentColor = Color(0xFF070908),
                        disabledContentColor = MutedGrey
                    )
                ) {
                    Text(
                        if (timerSeconds > 0) "I Understand  ($timerSeconds)" else "I Understand",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DisclaimerSection(
    number: String,
    title: String,
    body: String,
    highlightColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(highlightColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .border(1.dp, highlightColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    number, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = highlightColor, fontFamily = Inter
                )
            }
            Text(
                title.uppercase(),
                fontSize = 11.sp, letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold, color = PureWhite, fontFamily = Inter
            )
        }
        Text(
            body,
            fontSize = 13.sp, color = MidGrey, lineHeight = 20.sp, fontFamily = Inter
        )
    }
}
