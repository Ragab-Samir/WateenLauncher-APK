package com.wateen.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.wateen.launcher.data.AppItem
import kotlinx.coroutines.delay

@Composable
fun AppCard(
    app: AppItem,
    isFav: Boolean,
    isLocked: Boolean,
    focusRequester: FocusRequester?,
    onOpen: () -> Unit,
    onMenu: () -> Unit
) {
    FocusBox(
        Modifier.width(150.dp).height(122.dp),
        focusRequester = focusRequester,
        onClick = onOpen,
        onLongClick = onMenu
    ) {
        Column(
            Modifier.align(Alignment.Center).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (app.icon != null) {
                Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(58.dp))
            } else {
                Box(Modifier.size(58.dp).background(Gold, CircleShape), contentAlignment = Alignment.Center) {
                    W(app.label.take(1), 26, Bg, true)
                }
            }
            W(app.label, 15, TextW, align = TextAlign.Center, maxLines = 1)
        }
        Row(Modifier.align(Alignment.TopStart).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (isFav) W("★", 14, Gold)
            if (isLocked) W("🔒", 14)
        }
    }
}

@Composable
fun ActionCard(label: String, symbol: String, sub: String?, onClick: () -> Unit) {
    FocusBox(Modifier.width(150.dp).height(110.dp), onClick = onClick) {
        Column(
            Modifier.align(Alignment.Center).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            W(symbol, 30)
            W(label, 15, TextW, align = TextAlign.Center)
            if (sub != null) W(sub, 12, Gold, align = TextAlign.Center)
        }
    }
}

@Composable
fun MenuDialog(title: String, items: List<Pair<String, () -> Unit>>, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { first.requestFocus() }
    }
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                Modifier.width(380.dp)
                    .background(Panel, RoundedCornerShape(16.dp))
                    .border(2.dp, Gold, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                W(title, 20, Gold, true, maxLines = 1)
                items.forEachIndexed { i, it ->
                    FocusBox(
                        Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        focusRequester = if (i == 0) first else null,
                        onClick = { it.second() }
                    ) {
                        W(it.first, 17, modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PinDialog(title: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { first.requestFocus() }
    }
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(
                Modifier.background(Panel, RoundedCornerShape(18.dp))
                    .border(2.dp, Gold, RoundedCornerShape(18.dp))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                W(title, 18, Gold, true, align = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (i in 0 until 4) {
                        Box(
                            Modifier.size(18.dp)
                                .background(if (i < pin.length) Gold else Color(0x44FFFFFF), CircleShape)
                        )
                    }
                }
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "✕")
                keys.chunked(3).forEachIndexed { r, rowKeys ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowKeys.forEachIndexed { c, k ->
                            FocusBox(
                                Modifier.size(68.dp),
                                shape = RoundedCornerShape(12.dp),
                                focusRequester = if (r == 0 && c == 0) first else null,
                                onClick = {
                                    when (k) {
                                        "⌫" -> { pin = pin.dropLast(1) }
                                        "✕" -> { onDismiss() }
                                        else -> {
                                            if (pin.length < 4) {
                                                val np = pin + k
                                                pin = np
                                                if (np.length == 4) onDone(np)
                                            }
                                        }
                                    }
                                }
                            ) {
                                W(k, 24, modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            }
        }
    }
}
