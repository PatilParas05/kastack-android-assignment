package dev.paraspatil.luminaai.presentation.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.paraspatil.luminaai.ui.theme.LuminaAITheme

@Composable
fun AuraCircle(
    isListening: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier.fillMaxSize() // Default to filling the available space
) {
    // 1. Idle State Animation (Breathing)
    val infiniteTransition = rememberInfiniteTransition(label = "aura_breathing")
    val idleScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_scale"
    )
    val idleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_alpha"
    )

    // 2. Listening State (Reactive)
    // If listening, we scale up significantly based on the mic amplitude.
    // If idle, we just use the breathing scale.
    val targetScale = if (isListening) 1f + (amplitude * 1.5f) else idleScale
    val targetAlpha = if (isListening) 0.5f + (amplitude * 0.5f) else idleAlpha

    // Smooth transition for the amplitude changes
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(150, easing = LinearOutSlowInEasing),
        label = "reactive_scale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(150),
        label = "reactive_alpha"
    )

    // 3. Pure Canvas Drawing
    Canvas(modifier = modifier) {
        val centerPoint = center
        val baseRadius = size.minDimension / 3f

        // Outer Glow (Reactive/Breathing)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF6200EA).copy(alpha = animatedAlpha),
                    Color(0xFF6200EA).copy(alpha = 0f)
                ),
                center = centerPoint,
                radius = baseRadius * animatedScale * 1.5f
            ),
            radius = baseRadius * animatedScale * 1.5f,
            center = centerPoint
        )

        // Middle Ring
        drawCircle(
            color = Color(0xFFBB86FC).copy(alpha = if (isListening) 0.8f else 0.4f),
            radius = baseRadius * animatedScale,
            center = centerPoint,
            style = Stroke(width = 8f)
        )

        // Solid Inner Core
        drawCircle(
            color = Color(0xFF3700B3),
            radius = baseRadius * 0.8f,
            center = centerPoint
        )
    }
}
@Preview(showBackground = true, name = "Aura Circle Listening")
@Composable
fun AuraCircleListeningPreview() {
    LuminaAITheme {
        AuraCircle(
            isListening = true,
            amplitude = 0.5f,
            modifier = Modifier.size(200.dp)
        )
    }
}

@Preview(showBackground = true, name = "Aura Circle Idle")
@Composable
fun AuraCircleIdlePreview() {
    LuminaAITheme {
        AuraCircle(
            isListening = false,
            amplitude = 0f,
            modifier = Modifier.size(200.dp)
        )
    }
}
