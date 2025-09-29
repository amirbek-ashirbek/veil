package io.github.amirbekashirbek.veil

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Veil(
    modifier: Modifier = Modifier,
    isObscured: Boolean,
    blurRadius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Box(modifier = if (isObscured) modifier.blur(blurRadius) else modifier) {
        content()
    }
}
