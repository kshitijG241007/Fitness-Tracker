package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.DayStepPoint
import com.example.ui.theme.GoalEmerald
import com.example.ui.theme.ProgressBlue
import java.text.NumberFormat
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun StepProgressRing(
    steps: Int,
    goalSteps: Int,
    modifier: Modifier = Modifier,
    ringSize: Dp = 236.dp,
    strokeWidth: Dp = 20.dp
) {
    val safeGoal = max(goalSteps, 1)
    val rawProgress = (steps.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "StepRingProgress"
    )

    val percent = ((steps.toFloat() / safeGoal.toFloat()) * 100f).roundToInt()
    val remaining = (safeGoal - steps).coerceAtLeast(0)
    val formatter = remember { NumberFormat.getIntegerInstance() }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Foreground gradient arc
            if (animatedProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(primaryColor, secondaryColor, primaryColor)
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.DirectionsWalk,
                contentDescription = "Steps",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatter.format(steps),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "of ${formatter.format(safeGoal)} steps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = if (steps >= safeGoal) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Text(
                    text = if (steps >= safeGoal) {
                        "Goal reached! ($percent%)"
                    } else {
                        "${formatter.format(remaining)} to go • $percent%"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (steps >= safeGoal) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
fun StepsBarChart(
    points: List<DayStepPoint>,
    isWeekly: Boolean,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    var selectedIndex by remember(points, isWeekly) {
        mutableIntStateOf(points.lastIndex)
    }

    val formatter = remember { NumberFormat.getIntegerInstance() }
    val selectedPoint = points.getOrNull(selectedIndex) ?: points.last()

    val maxGoal = points.maxOfOrNull { it.goalSteps } ?: 6000
    val maxSteps = points.maxOfOrNull { it.steps } ?: 0
    val chartMax = max(max(maxGoal, maxSteps), 1000).toFloat() * 1.15f

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    val goalLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val metStart = GoalEmerald
    val metEnd = MaterialTheme.colorScheme.primary
    val belowStart = ProgressBlue
    val belowEnd = MaterialTheme.colorScheme.secondary

    Column(modifier = modifier.fillMaxWidth()) {
        // Selected Day Tooltip Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = selectedPoint.fullDateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${formatter.format(selectedPoint.steps)} steps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            val metGoal = selectedPoint.steps >= selectedPoint.goalSteps && selectedPoint.goalSteps > 0
            Surface(
                shape = RoundedCornerShape(50),
                color = if (metGoal) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Text(
                    text = if (metGoal) "Goal Met" else "Goal: ${formatter.format(selectedPoint.goalSteps)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (metGoal) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bar Chart Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.isNotEmpty() && size.width > 0) {
                                val slotWidth = size.width.toFloat() / points.size
                                val tapped = (offset.x / slotWidth).toInt()
                                    .coerceIn(0, points.lastIndex)
                                selectedIndex = tapped
                            }
                        }
                    }
            ) {
                val count = points.size
                val slotWidth = size.width / count
                val barWidth = if (isWeekly) {
                    (slotWidth * 0.48f).coerceAtMost(36.dp.toPx())
                } else {
                    (slotWidth * 0.62f).coerceAtLeast(4.dp.toPx())
                }
                val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

                // Goal dashed line
                val goalRatio = (maxGoal.toFloat() / chartMax).coerceIn(0f, 1f)
                val goalY = size.height * (1f - goalRatio)
                drawLine(
                    color = goalLineColor,
                    start = Offset(0f, goalY),
                    end = Offset(size.width, goalY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                points.forEachIndexed { index, point ->
                    val centerX = slotWidth * index + slotWidth / 2f
                    val left = centerX - barWidth / 2f

                    // Subtle full-height track for selected bar
                    if (index == selectedIndex) {
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(left - 2.dp.toPx(), 0f),
                            size = Size(barWidth + 4.dp.toPx(), size.height),
                            cornerRadius = cornerRadius
                        )
                    }

                    val ratio = (point.steps.toFloat() / chartMax).coerceIn(0f, 1f)
                    val barHeight = (size.height * ratio).coerceAtLeast(
                        if (point.steps > 0) 6.dp.toPx() else 3.dp.toPx()
                    )
                    val topY = size.height - barHeight
                    val metGoal = point.steps >= point.goalSteps && point.goalSteps > 0

                    val brush = if (point.steps == 0) {
                        Brush.verticalGradient(listOf(trackColor, trackColor))
                    } else if (metGoal) {
                        Brush.verticalGradient(listOf(metStart, metEnd))
                    } else {
                        Brush.verticalGradient(listOf(belowStart, belowEnd))
                    }

                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(left, topY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = cornerRadius,
                        alpha = if (index == selectedIndex) 1f else 0.78f
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X-axis Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isWeekly) {
                points.forEachIndexed { index, point ->
                    Text(
                        text = point.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == selectedIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Show 5 evenly spaced date labels for 30-day chart
                val indices = listOf(0, 7, 14, 21, points.lastIndex).distinct()
                indices.forEach { idx ->
                    points.getOrNull(idx)?.let { p ->
                        Text(
                            text = p.fullDateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
