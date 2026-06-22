package com.doer.shijiben.ui.theme

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

// ============================================================
// Pixel UI Component Library
// ============================================================
// Building blocks for the 8-bit pixel design system.
// All components use pixel-dashed borders and solid fills.
// ============================================================

// ═══════════════════════════════════════════════════════════
// PixelCard
// ═══════════════════════════════════════════════════════════

enum class PixelCardLevel { Primary, Secondary, Tertiary }

@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    level: PixelCardLevel = PixelCardLevel.Secondary,
    backgroundColor: Color = PixelCream,
    borderColor: Color = PixelBorder,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val borderModifier = when (level) {
        PixelCardLevel.Primary -> Modifier.pixelBorderPrimary(borderColor)
        PixelCardLevel.Secondary -> Modifier.pixelBorderSecondary(borderColor)
        PixelCardLevel.Tertiary -> Modifier.pixelBorderTertiary(borderColor)
    }

    val clickModifier = if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .pressScaleEffect(interactionSource)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .background(backgroundColor)
            .then(borderModifier)
            .then(clickModifier)
            .padding(contentPadding),
    ) {
        content()
    }
}

// ═══════════════════════════════════════════════════════════
// PixelButton
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = PixelTeal,
    pressedBackgroundColor: Color = PixelTeal,
    contentColor: Color = Color.White,
    borderColor: Color = PixelBorder,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor = if (isPressed) pressedBackgroundColor else backgroundColor
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "btnPress"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(bgColor)
            .pixelBorderTertiary(borderColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// ═══════════════════════════════════════════════════════════
// PixelIconButton
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelIconButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    pressedBackgroundColor: Color = PixelGrayLight,
    borderColor: Color = PixelBorder,
    size: Dp = 36.dp,
    borderless: Boolean = false,
    contentDescription: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor = if (isPressed) pressedBackgroundColor else backgroundColor
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "iconPress"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(size)
            .background(bgColor)
            .then(if (!borderless) Modifier.pixelBorderTertiary(borderColor) else Modifier)
            .semantics { contentDescription?.let { this.contentDescription = it } }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

// ═══════════════════════════════════════════════════════════
// PixelBadge
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = PixelCoral,
    textColor: Color = Color.White,
    borderColor: Color = PixelBorder,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickModifier = if (onClick != null) {
        Modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .pressScaleEffect(interactionSource)
    } else Modifier

    Box(
        modifier = modifier
            .background(backgroundColor)
            .pixelBorderTertiary(borderColor)
            .then(clickModifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = textColor, style = PixelLabel)
    }
}

// ═══════════════════════════════════════════════════════════
// PixelSectionHeader
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelSectionHeader(
    title: String,
    accentColor: Color = PixelTeal,
    modifier: Modifier = Modifier,
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
        Spacer(Modifier.width(8.dp))
        Text(text = title, color = PixelText, style = PixelLabel)
    }
}

// ═══════════════════════════════════════════════════════════
// PixelInput
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    textStyle: TextStyle = PixelBody,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(PixelCream)
            .pixelBorderTertiary(if (isFocused) PixelTeal else PixelGrayLight)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (value.isEmpty()) {
            Text(text = placeholder, color = PixelGray, style = textStyle)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle.copy(color = PixelText),
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ═══════════════════════════════════════════════════════════
// PixelBlinkIndicator
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelBlinkIndicator(
    modifier: Modifier = Modifier,
    color: Color = PixelYellow,
    size: Dp = 8.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pixelBlink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 800
                0f at 0
                1f at 400
                0f at 401 // Instant cut — no smooth interpolation
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "pixelBlink",
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { alpha = blinkAlpha }
            .background(color),
    )
}

// ═══════════════════════════════════════════════════════════
// PixelDivider
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PixelGrayLight),
    )
}

// ============================================================
// Animation Utilities
// ============================================================

/**
 * Frame-style fade-in: 3 discrete alpha steps (0 → 0.33 → 0.66 → 1.0).
 * No smooth interpolation — pure pixel pop.
 */
fun Modifier.pixelFadeInFrame(delayFrames: Int = 0): Modifier = this.composed {
    val frameDuration = 80L
    val totalFrames = 3

    var frame by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        delay((delayFrames * frameDuration))
        for (i in 1..totalFrames) {
            delay(frameDuration)
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
 * Frame-style number hop: instant digit change, no smooth interpolation.
 * Creates a "slot machine" pixel number transition.
 */
@Composable
fun PixelHopNumber(
    targetValue: Long,
    modifier: Modifier = Modifier,
    frameMs: Int = 60,
    format: (Long) -> String = { it.toString() },
    textStyle: TextStyle = PixelBody,
    contentColor: Color = PixelText,
) {
    var displayValue by remember { mutableStateOf(targetValue) }

    LaunchedEffect(targetValue) {
        if (targetValue == displayValue) return@LaunchedEffect
        // Instant jump — no smooth steps
        displayValue = targetValue
    }

    Text(
        text = format(displayValue),
        style = textStyle,
        color = contentColor,
        modifier = modifier,
    )
}

/**
 * Press scale effect — shared across interactive components.
 */
@Composable
fun Modifier.pressScaleEffect(interactionSource: MutableInteractionSource): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "pressScale"
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

// ═══════════════════════════════════════════════════════════
// PixelDialog
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = PixelCream,
    borderColor: Color = PixelBorder,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = modifier
                .background(backgroundColor)
                .pixelBorderPrimary(borderColor)
                .padding(20.dp),
        ) {
            content()
        }
    }
}
