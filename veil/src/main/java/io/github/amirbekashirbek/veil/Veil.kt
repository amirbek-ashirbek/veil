package io.github.amirbekashirbek.veil

import android.graphics.Bitmap
import android.graphics.RuntimeShader
import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import androidx.core.view.drawToBitmap
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.Language
import java.util.function.Consumer
import kotlin.math.roundToInt
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers

sealed interface VeilEffect {
    data class Blur(
        val radius: Dp = 16.dp,
        val edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
    ) : VeilEffect

    data class Pixelate(
        val pixelSize: Dp = 8.dp,
        val isGrayscale: Boolean = false
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
            is VeilEffect.Blur -> base.blurCompat(
                radius = effect.radius,
                edgeTreatment = effect.edgeTreatment
            )
            is VeilEffect.Pixelate -> base.pixelate(
                pixelSize = effect.pixelSize,
                isGrayscale = effect.isGrayscale
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

/**
 * Backward-compatible blur:
 * - API 31+: uses Modifier.blur (RenderEffect, GPU)
 * - API 21–30: snapshots the window region under this composable, fast CPU StackBlur, then draws it
 *
 * NOTE: BlurredEdgeTreatment is honored exactly only on API 31+.
 * On older APIs, apply your own clip(shape) *around* blurCompat if you need rounded edges.
 */
fun Modifier.blurCompat(
    radius: Dp,
    edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Rectangle,
    downscale: Int = 4 // 4–8 is a huge perf win on old devices
): Modifier = composed {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Native GPU blur
        return@composed this.blur(radius = radius, edgeTreatment = edgeTreatment)
    }

    // Pre-31 fallback
    val view = LocalView.current
    val density = LocalDensity.current
    val radiusPx = with(density) { radius.toPx().coerceAtLeast(0.5f) }
    var windowBounds by remember { mutableStateOf(Rect.Zero) }

    var baseCrop by remember { mutableStateOf<Bitmap?>(null) }
    var blurred by remember { mutableStateOf<ImageBitmap?>(null) }
    var blurredWindowRect by remember { mutableStateOf<IntRect?>(null) }

    LaunchedEffect(windowBounds) {

        if (windowBounds.isEmpty) {
            baseCrop?.recycle()
            baseCrop = null
            return@LaunchedEffect
        }
        withFrameNanos {  }

        val snapshot = try {
            view.drawToBitmap(Bitmap.Config.ARGB_8888)
        } catch (_: Exception) {
            return@LaunchedEffect
        }

        val left = windowBounds.left.roundToInt().coerceIn(0, snapshot.width - 1)
        val top = windowBounds.top.roundToInt().coerceIn(0, snapshot.height - 1)
        val right = windowBounds.right.roundToInt().coerceIn(left + 1, snapshot.width)
        val bottom = windowBounds.bottom.roundToInt().coerceIn(top + 1, snapshot.height)

        blurredWindowRect = IntRect(left, top, right, bottom)

        val cropW = (right - left).coerceAtLeast(1)
        val cropH = (bottom - top).coerceAtLeast(1)

        val cropped = try {
            Bitmap.createBitmap(snapshot, left, top, cropW, cropH)
        } catch (_: Exception) {
            snapshot.recycle()
            blurred = null
            return@LaunchedEffect
        } finally {
            snapshot.recycle()
        }

        baseCrop?.recycle()
        baseCrop = cropped
    }

    LaunchedEffect(baseCrop, radiusPx, downscale) {
        val src = baseCrop
        if (src == null || radiusPx <= 0.5f) {
            blurred = null
            return@LaunchedEffect
        }
        val rInt = (radiusPx / downscale).roundToInt().coerceAtLeast(1)
        val bmp = withContext(Dispatchers.Default) {
            stackBlur(src, rInt, downscale)
        }
        blurred = bmp.asImageBitmap()
    }

    this
        .onGloballyPositioned { coordinates ->
            windowBounds = coordinates.boundsInWindow()
        }
        .drawWithContent {
            drawContent()
            val img = blurred ?: return@drawWithContent
            val sourceRect = blurredWindowRect ?: return@drawWithContent
            val currentWin = windowBounds

            val destinationOffset = IntOffset(
                x = (sourceRect.left - currentWin.left).roundToInt(),
                y = (sourceRect.top - currentWin.top ).roundToInt()
            )
            val destinationSize = IntSize(
                width = sourceRect.right - sourceRect.left,
                height = sourceRect.bottom - sourceRect.top
            )

            drawImage(
                image = img,
                dstSize = destinationSize,
                dstOffset = destinationOffset
            )
        }
}
/**
 * Very small, fast two-pass StackBlur with downscale.
 * - Downscales by [downscale], blurs with [radius] in that space, then caller draws upscaled.
 * - Blurs RGB; sets alpha to 255 (opaque) which is fine for a veil overlay.
 */
private fun stackBlur(
    src: Bitmap,
    radius: Int,
    downscale: Int
): Bitmap {
    val w = (src.width / downscale).coerceAtLeast(1)
    val h = (src.height / downscale).coerceAtLeast(1)

    val small = src.scale(w, h)
    val inPix = IntArray(w * h)
    small.getPixels(inPix, 0, w, 0, 0, w, h)

    val win = 2 * radius + 1

    // Horizontal pass
    val horiz = IntArray(w * h)
    for (y in 0 until h) {
        var r = 0; var g = 0; var b = 0
        // prime window
        for (x in -radius..radius) {
            val xx = x.coerceIn(0, w - 1)
            val c = inPix[y * w + xx]
            r += (c shr 16) and 0xFF
            g += (c shr 8) and 0xFF
            b += c and 0xFF
        }
        for (x in 0 until w) {
            horiz[y * w + x] =
                (0xFF shl 24) or ((r / win) shl 16) or ((g / win) shl 8) or (b / win)
            val xOut = (x - radius).coerceIn(0, w - 1)
            val xIn  = (x + radius + 1).coerceIn(0, w - 1)
            val cOut = inPix[y * w + xOut]
            val cIn  = inPix[y * w + xIn]
            r += ((cIn shr 16) and 0xFF) - ((cOut shr 16) and 0xFF)
            g += ((cIn shr 8) and 0xFF) - ((cOut shr 8) and 0xFF)
            b += ( cIn and 0xFF)       - ( cOut and 0xFF)
        }
    }

    // Vertical pass
    val vert = IntArray(w * h)
    for (x in 0 until w) {
        var r = 0; var g = 0; var b = 0
        for (y in -radius..radius) {
            val yy = y.coerceIn(0, h - 1)
            val c = horiz[yy * w + x]
            r += (c shr 16) and 0xFF
            g += (c shr 8) and 0xFF
            b += c and 0xFF
        }
        for (y in 0 until h) {
            vert[y * w + x] =
                (0xFF shl 24) or ((r / win) shl 16) or ((g / win) shl 8) or (b / win)
            val yOut = (y - radius).coerceIn(0, h - 1)
            val yIn  = (y + radius + 1).coerceIn(0, h - 1)
            val cOut = horiz[yOut * w + x]
            val cIn  = horiz[yIn  * w + x]
            r += ((cIn shr 16) and 0xFF) - ((cOut shr 16) and 0xFF)
            g += ((cIn shr 8) and 0xFF) - ((cOut shr 8) and 0xFF)
            b += ( cIn and 0xFF)       - ( cOut and 0xFF)
        }
    }

    return createBitmap(w, h).apply {
        setPixels(vert, 0, w, 0, 0, w, h)
    }
}

fun Modifier.pixelate(
    pixelSize: Dp = 8.dp,
    shape: Shape = RectangleShape,
    isGrayscale: Boolean
): Modifier = composed {

    val density = LocalDensity.current
    val pixelSizePx = with(density) { pixelSize.toPx().coerceAtLeast(1f) }

    // Track the actual layer size in px so we can feed it to the shader & crop
    var sizePx by remember { mutableStateOf(IntSize.Zero) }

    val renderEffect = remember(pixelSizePx, isGrayscale, sizePx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && sizePx != IntSize.Zero)
            createPixelateEffect(
                pixelSizePx = pixelSizePx,
                isGrayscale = isGrayscale,
                widthPx = sizePx.width,
                heightPx = sizePx.height
            )
        else
            null
    }

    this
        .onSizeChanged {
            sizePx = it
        }
        .graphicsLayer {
            this.renderEffect = renderEffect
            this.shape = shape
            clip = true
        }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createPixelateEffect(
    pixelSizePx: Float,
    isGrayscale: Boolean,
    widthPx: Int,
    heightPx: Int
): RenderEffect {

    @Language("AGSL")
    val shaderSrc = if (isGrayscale) {
        """
        uniform shader content;
        uniform float2 pixelSize;
        uniform float2 contentSize;

        half4 main(vec2 p) {
            vec2 halfPx = 0.5 * pixelSize;
            vec2 cell = floor(p / pixelSize) * pixelSize + halfPx;

            // keep the sample point INSIDE the content
            cell = clamp(cell, halfPx, contentSize - halfPx);

            half4 color = content.eval(cell);
            half lum = dot(color.rgb, half3(0.299, 0.587, 0.114));
            return half4(lum, lum, lum, color.a);
        }
    """.trimIndent()
    } else {
        """
        uniform shader content;
        uniform float2 pixelSize;
        uniform float2 contentSize;

        half4 main(vec2 p) {
            vec2 halfPx = 0.5 * pixelSize;
            vec2 cell = floor(p / pixelSize) * pixelSize + halfPx;
            cell = clamp(cell, halfPx, contentSize - halfPx);
            return content.eval(cell);
        }
    """.trimIndent()
    }

    val shader = RuntimeShader(shaderSrc).apply {
        setFloatUniform("pixelSize", floatArrayOf(pixelSizePx, pixelSizePx))
        setFloatUniform("contentSize", floatArrayOf(widthPx.toFloat(), heightPx.toFloat()))
    }

    // "content" here refers to whatever is behind the layer this effect is applied to.
    return android.graphics.RenderEffect
        .createRuntimeShaderEffect(shader, "content")
        .asComposeRenderEffect()
}

private data class IntRect(val left: Int, val top: Int, val right: Int, val bottom: Int)