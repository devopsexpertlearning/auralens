package com.example.auralens.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import com.example.auralens.ui.theme.NeonCyan
import com.example.auralens.ui.theme.NeonPurple

@Composable
fun ScannerOverlay(
    isScanning: Boolean = true
) {
    if (!isScanning) return

    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    
    // Animate a vertical line moving up and down
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanY"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val height = size.height
        val width = size.width
        val yPos = height * scanY

        // Draw the scanning line
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    NeonCyan.copy(alpha = 0.8f),
                    Color.Transparent
                )
            ),
            start = Offset(0f, yPos),
            end = Offset(width, yPos),
            strokeWidth = 4f
        )
        
        // Draw a subtle "glow" area behind the line
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    NeonCyan.copy(alpha = 0.0f),
                    NeonCyan.copy(alpha = 0.2f),
                    NeonCyan.copy(alpha = 0.0f)
                ),
                startY = yPos - 100f,
                endY = yPos + 100f
            ),
            topLeft = Offset(0f, yPos - 100f),
            size = Size(width, 200f),
            blendMode = BlendMode.Plus // meaningful blend mode for "glow"
        )
        
        // Draw corner brackets for "tech" feel
        val bracketLen = 60f
        val stroke = 6f
        val color = NeonPurple.copy(alpha = 0.6f)
        
        // Top Left
        drawLine(color, Offset(20f, 20f), Offset(20f + bracketLen, 20f), stroke)
        drawLine(color, Offset(20f, 20f), Offset(20f, 20f + bracketLen), stroke)
        
        // Top Right
        drawLine(color, Offset(width - 20f, 20f), Offset(width - 20f - bracketLen, 20f), stroke)
        drawLine(color, Offset(width - 20f, 20f), Offset(width - 20f, 20f + bracketLen), stroke)

        // Bottom Left
        drawLine(color, Offset(20f, height - 20f), Offset(20f + bracketLen, height - 20f), stroke)
        drawLine(color, Offset(20f, height - 20f), Offset(20f, height - 20f - bracketLen), stroke)

        // Bottom Right
        drawLine(color, Offset(width - 20f, height - 20f), Offset(width - 20f - bracketLen, height - 20f), stroke)
        drawLine(color, Offset(width - 20f, height - 20f), Offset(width - 20f, height - 20f - bracketLen), stroke)
    }
}
