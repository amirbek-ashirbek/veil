package io.github.amirbekashirbek.veil

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.function.Consumer

sealed interface VeilEffect {
    data class Blur(
        val radius: Dp = 16.dp,
        val edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
    ) : VeilEffect

    /** Simple color overlay on top of content. */
    data class Scrim(val color: Color = Color(0x99000000)) : VeilEffect

    /** Darken or lighten by amount (0f..1f). */
    data class Dim(
        val amount: Float = 0.6f,
        val mode: Mode = Mode.Darken
    ) : VeilEffect {
        enum class Mode { Darken, Lighten }
    }

    /** Blur + subtle tint for a frosted-glass look. */
    data class Frosted(
        val radius: Dp = 16.dp,
        val tint: Color = Color.White.copy(alpha = 0.15f),
        val edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
    ) : VeilEffect

}

enum class VeilInteraction {
    TapToggle,
    HoldToReveal,
    LongPressToggle
}

@Composable
fun Modifier.veil(
    effect: VeilEffect = VeilEffect.Blur(),
    interaction: VeilInteraction = VeilInteraction.TapToggle,
    enabled: Boolean = true
): Modifier = composed {

//    val isRecording by rememberIsScreenRecording()
    var isRevealed by remember { mutableStateOf(false) }
    val shouldVeil = enabled && !isRevealed

    val gestureModifier = when (interaction) {
        VeilInteraction.TapToggle -> Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { isRevealed = !isRevealed },
            onLongClick = {}
        )
        VeilInteraction.LongPressToggle -> Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
            onLongClick = { isRevealed = !isRevealed }
        )
        VeilInteraction.HoldToReveal -> Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isRevealed = true
                    tryAwaitRelease()
                    isRevealed = false
                }
            )
        }
    }

    val base = this.then(gestureModifier)

    if (!shouldVeil) {
        base
    } else {
        when (effect) {
            is VeilEffect.Blur -> base.blur(
                radius = effect.radius,
                edgeTreatment = effect.edgeTreatment
            )
            is VeilEffect.Scrim -> base.drawWithContent {
                drawContent()
                drawRect(effect.color)
            }
            is VeilEffect.Dim -> base.drawWithContent {
                drawContent()
                val overlayColor = when (effect.mode) {
                    VeilEffect.Dim.Mode.Darken  -> Color.Black.copy(alpha = effect.amount.coerceIn(0f, 1f))
                    VeilEffect.Dim.Mode.Lighten -> Color.White.copy(alpha = effect.amount.coerceIn(0f, 1f))
                }
                drawRect(overlayColor)
            }
            is VeilEffect.Frosted -> base
                .blur(effect.radius, effect.edgeTreatment)
                .drawWithContent {
                    drawContent()
                    drawRect(effect.tint)
                }
        }
    }
}

@Composable
fun rememberIsScreenRecording(): State<Boolean> {
    val context = LocalContext.current
    val isRecording = remember { mutableStateOf(false) }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        DisposableEffect(Unit) {
            val windowManager = context.getSystemService(WindowManager::class.java)
            val callback = Consumer<Int> { state ->
                isRecording.value = when (state) {
                    WindowManager.SCREEN_RECORDING_STATE_VISIBLE -> true
                    else -> false
                }
            }
            windowManager.addScreenRecordingCallback(context.mainExecutor, callback)
            onDispose { windowManager.removeScreenRecordingCallback(callback) }
        }
    }
    return isRecording
}