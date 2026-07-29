package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isRtl: Boolean = true,
    onDismiss: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "alpha"
    )

    val badgeAlphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0f,
        animationSpec = tween(durationMillis = 1200, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "badgeAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val rotateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing)
        ),
        label = "rotate"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2400)
        onDismiss()
    }

    // Gold & Navy Theme Colors
    val navyDark = Color(0xFF070C18)
    val navyMidnight = Color(0xFF0F1A30)
    val onyxBlack = Color(0xFF030509)
    val goldPrimary = Color(0xFFD4AF37)
    val goldLight = Color(0xFFF3E5AB)
    val goldDark = Color(0xFFA38020)
    val crispWhite = Color(0xFFFFFFFF)
    val mutedGoldText = Color(0xFFC5A059)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        navyMidnight,
                        navyDark,
                        onyxBlack
                    )
                )
            )
            .clickable { onDismiss() }
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        // Luxury Radial Background Glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2, size.height / 2 - 50.dp.toPx())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        goldPrimary.copy(alpha = 0.15f),
                        goldDark.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = size.width * 0.85f
                ),
                center = centerOffset,
                radius = size.width * 0.85f
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(28.dp)
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value)
        ) {
            // Elegant Minimalist Gold Emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(130.dp)
                    .scale(pulseGlow)
            ) {
                // Gold Rotating Ring Arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                goldPrimary,
                                goldLight,
                                goldDark,
                                goldPrimary
                            )
                        ),
                        startAngle = rotateAnim,
                        sweepAngle = 300f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Inner Navy & Onyx Disc
                Surface(
                    color = navyMidnight,
                    shape = CircleShape,
                    shadowElevation = 20.dp,
                    tonalElevation = 10.dp,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF14223D),
                                        onyxBlack
                                    )
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Logo",
                            tint = goldPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main Title
            Text(
                text = if (isRtl) "سامانه مدیریت فاکتور و چوب پلاست" else "Wood Plastic & Invoicing Engine",
                color = crispWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = if (isRtl) "مدیریت پیش‌فاکتور، فاکتور فروش و خدمات نصب" else "Invoices, Estimates & Installation Services",
                color = mutedGoldText,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // MR.CODE Badge - Minimal Chic Style
            Surface(
                color = navyMidnight.copy(alpha = 0.9f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            goldPrimary.copy(alpha = 0.8f),
                            goldLight.copy(alpha = 0.4f),
                            goldDark.copy(alpha = 0.8f)
                        )
                    )
                ),
                modifier = Modifier
                    .alpha(badgeAlphaAnim.value)
                    .testTag("mr_code_subtitle_badge")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = goldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MR.CODE",
                        color = goldLight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Elegant Gold Loading Bar
            LinearProgressIndicator(
                color = goldPrimary,
                trackColor = navyMidnight,
                modifier = Modifier
                    .width(130.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }

        // Tap to skip
        Text(
            text = if (isRtl) "جهت ورود لمس کنید" else "Tap anywhere to skip",
            color = mutedGoldText.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

