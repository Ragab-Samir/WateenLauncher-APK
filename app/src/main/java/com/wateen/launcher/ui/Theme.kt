package com.wateen.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

val Bg = Color(0xFF0B0F1A)
val Panel = Color(0xFF151B2B)
val CardBg = Color(0xFF1C2333)
val CardFocus = Color(0xFF26304A)
val Gold = Color(0xFFFFC107)
val TextW = Color(0xFFF2F2F2)
val Muted = Color(0xFF8A93A6)
val Green = Color(0xFF4CAF50)

@Composable
fun W(
    text: String,
    size: Int = 16,
    color: Color = TextW,
    bold: Boolean = false,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = size.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            textAlign = align
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun rememberNow(interval: Long): State<Long> {
    val s = remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(interval) {
        while (true) {
            delay(interval)
            s.value = System.currentTimeMillis()
        }
    }
    return s
}

// بطاقة قابلة للتركيز بالريموت: تكبير 1.05 + حدود وتوهج ذهبي + ضغطة مطولة على OK
@Composable
fun FocusBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.(Boolean) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    var longFired by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "focusScale")
    val curClick by rememberUpdatedState(onClick)
    val curLong by rememberUpdatedState(onLongClick)

    var m = modifier
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .shadow(if (focused) 18.dp else 0.dp, shape, ambientColor = Gold, spotColor = Gold)
        .background(if (focused) CardFocus else CardBg, shape)
        .border(if (focused) 3.dp else 1.dp, if (focused) Gold else Color(0x22FFFFFF), shape)
    if (focusRequester != null) m = m.focusRequester(focusRequester)
    m = m
        .onFocusChanged { focused = it.isFocused }
        .onPreviewKeyEvent { e ->
            val k = e.key
            if (k == Key.DirectionCenter || k == Key.Enter || k == Key.NumPadEnter) {
                when (e.type) {
                    KeyEventType.KeyDown -> {
                        if (e.nativeKeyEvent.repeatCount == 0) {
                            pressed = true
                            longFired = false
                        } else if (pressed && !longFired && curLong != null) {
                            longFired = true
                            curLong?.invoke()
                        }
                        true
                    }
                    KeyEventType.KeyUp -> {
                        if (pressed && !longFired) curClick()
                        pressed = false
                        longFired = false
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }
        .focusable()
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { curClick() },
                onLongPress = { curLong?.invoke() }
            )
        }

    Box(m) { content(focused) }
}
