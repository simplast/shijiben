package com.doer.shijiben.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

// ============================================================
// Pixel UI Component Library
// ============================================================
// Pixel-styled components using double-line borders.
// These are the building blocks for the pixel UI system.
// ============================================================

// Card Level
enum class PixelCardLevel {
    Primary,
    Secondary,
    Tertiary,
}

@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    level: PixelCardLevel = PixelCardLevel.Secondary,
    backgroundColor: Color = MistBlue,
    borderOuterColor: Color = SeaBlue,
    borderInnerColor: Color = Color.White,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    val outerWidth = when (level) {
        PixelCardLevel.Primary -> PixelBorderPrimary.outerWidth
        PixelCardLevel.Secondary -> PixelBorderSecondary.outerWidth
        PixelCardLevel.Tertiary -> PixelBorderTertiary.outerWidth
    }
    val innerWidth = when (level) {
        PixelCardLevel.Primary -> PixelBorderPrimary.innerWidth
        PixelCardLevel.Secondary -> PixelBorderSecondary.innerWidth
        PixelCardLevel.Tertiary -> PixelBorderTertiary.innerWidth
    }

    Box(
        modifier = modifier
            .background(backgroundColor)
            .doublePixelBorder(
                outerColor = borderOuterColor,
                innerColor = borderInnerColor,
                outerWidth = outerWidth,
                innerWidth = innerWidth,
            )
            .padding(contentPadding),
    ) {
        content()
    }
}

@Composable
fun PixelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SeaBlue,
    pressedBackgroundColor: Color = SeaBlueDark,
    contentColor: Color = Color.White,
    borderColor: Color = DeepTeal,
    borderInnerColor: Color = Color.White,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    pressScale: Float = 0.94f,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor = if (isPressed) pressedBackgroundColor else backgroundColor
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f
        ),
        label = "buttonPress"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(bgColor)
            .tertiaryDoubleBorder(
                outerColor = borderColor,
                innerColor = borderInnerColor,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun PixelIconButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SeaBlue,
    pressedBackgroundColor: Color = SeaBlueDark,
    borderColor: Color = DeepTeal,
    size: Dp = 32.dp,
    pressScale: Float = 0.92f,
    borderless: Boolean = false,
    contentDescription: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor = if (isPressed) pressedBackgroundColor else backgroundColor
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f
        ),
        label = "iconButtonPress"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(size)
            .background(bgColor)
            .then(
                if (!borderless) {
                    Modifier.tertiaryDoubleBorder(
                        outerColor = borderColor,
                        innerColor = Color.White,
                    )
                } else Modifier
            )
            .semantics {
                contentDescription?.let { this.contentDescription = it }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
fun PixelBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = CoralOrange,
    contentColor: Color = Color.White,
    borderColor: Color = DeepTeal,
    borderInnerColor: Color = Color.White,
) {
    Box(
        modifier = modifier
            .background(backgroundColor)
            .tertiaryDoubleBorder(
                outerColor = borderColor,
                innerColor = borderInnerColor,
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = contentColor,
            style = PixelLabel,
        )
    }
}

@Composable
fun PixelSectionHeader(
    title: String,
    accentColor: Color = SeaBlue,
    modifier: Modifier = Modifier,
    chineseTitle: String? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(3.dp)
                .background(accentColor),
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = chineseTitle ?: title,
            color = DeepTeal,
            style = PixelLabel,
        )
    }
}

@Composable
fun PixelInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(CreamWhite)
            .secondaryDoubleBorder(
                outerColor = if (isFocused) SeaBlue else LightSeaBlue,
                innerColor = Color.White,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = WarmGray300,
                style = textStyle,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle.copy(color = DeepTeal),
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ============================================================
// Pixel Dialog
// ============================================================

@Composable
fun PixelDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = CreamWhite,
    borderOuterColor: Color = DeepTeal,
    borderInnerColor: Color = Color.White,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = modifier
                .background(backgroundColor)
                .primaryDoubleBorder(
                    outerColor = borderOuterColor,
                    innerColor = borderInnerColor,
                )
                .padding(20.dp),
        ) {
            content()
        }
    }
}

// ============================================================
// Pixel Animation Utilities
// ============================================================
// Frame-based animations that preserve the pixel aesthetic.
// No smooth interpolation — instant state changes like 8-bit games.
// ============================================================

/**
 * Frame-style fade-in — 3 discrete steps, no smooth interpolation.
 * Gives a "pixel pop" feel when items enter the screen.
 *
 * @param delayFrames delay before animation starts (each frame = 80ms)
 */
fun Modifier.pixelFadeInFrame(
    delayFrames: Int = 0,
): Modifier = this.composed {
    val frameDuration = 80
    val totalFrames = 3

    var frame by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        delay((delayFrames * frameDuration).toLong())
        for (i in 1..totalFrames) {
            delay(frameDuration.toLong())
            frame = i
        }
    }

    val fadeAlpha = when (frame) {
        0 -> 0f
        1 -> 0.33f
        2 -> 0.66f
        else -> 1f
    }

    this.graphicsLayer { alpha = fadeAlpha }
}

/**
 * Frame-style number hop animation — digit changes in discrete jumps.
 * Creates a "slot machine" like pixel number transition.
 *
 * @param targetValue the target number to display
 * @param frameCount number of hop frames (default: 4)
 * @param frameMs milliseconds per frame
 */
@Composable
fun PixelHopNumber(
    targetValue: Long,
    modifier: Modifier = Modifier,
    frameCount: Int = 4,
    frameMs: Int = 60,
    format: (Long) -> String = { it.toString() },
    textStyle: TextStyle = MaterialTheme.typography.titleLarge,
    contentColor: Color = DeepTeal,
) {
    var displayValue by remember { mutableStateOf(targetValue) }
    var hopStep by remember { mutableStateOf(0) }

    LaunchedEffect(targetValue) {
        if (targetValue == displayValue) return@LaunchedEffect

        val diff = targetValue - displayValue
        val step = (diff / frameCount.coerceAtLeast(1)).coerceAtLeast(1)

        for (i in 1 until frameCount) {
            delay(frameMs.toLong())
            displayValue = displayValue + step
            hopStep = i
        }
        delay(frameMs.toLong())
        displayValue = targetValue
        hopStep = frameCount
    }

    Text(
        text = format(displayValue),
        style = textStyle,
        color = contentColor,
        modifier = modifier,
        fontWeight = FontWeight.Bold,
    )
}

/**
 * Frame-style state transition animation — slides/color-shifts
 * between states in discrete pixel steps.
 *
 * Use with key state changes like "pending → active → completed".
 *
 * @param stateKey key that triggers the transition when it changes
 * @param transitionFrames number of frames in the transition
 */
fun Modifier.pixelStateTransition(
    stateKey: Any,
    transitionFrames: Int = 3,
): Modifier = this.composed {
    val frameDuration = 70
    var transitionProgress by remember { mutableStateOf(1f) }

    LaunchedEffect(stateKey) {
        transitionProgress = 0f
        for (i in 1..transitionFrames) {
            delay(frameDuration.toLong())
            transitionProgress = i.toFloat() / transitionFrames.toFloat()
        }
        transitionProgress = 1f
    }

    this.graphicsLayer {
        translationX = (1f - transitionProgress) * 20f
        alpha = transitionProgress
        scaleX = 0.9f + transitionProgress * 0.1f
        scaleY = 0.9f + transitionProgress * 0.1f
    }
}
