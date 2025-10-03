package io.github.amirbekashirbek.veil.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.amirbekashirbek.veil.VeilEffect
import io.github.amirbekashirbek.veil.VeilInteraction
import io.github.amirbekashirbek.veil.sample.ui.theme.VeilTheme
import io.github.amirbekashirbek.veil.veil

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val activity = this
        setContent {
            VeilTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    VeilObviousDemoScreen(
//                        onFlagSecure = { FlagSecure.setEnabled(activity, it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeilObviousDemoScreen() {
    // --- Defaults ---
    val defaultHeavyBlur = 24.dp
    val defaultDimDarken = 0.75f
    val defaultDimLighten = 0.8f
    val defaultScrimAlpha = 0.5f
    val defaultFrostedRadius = 18.dp
    val defaultFrostedTintAlpha = 0.15f

    // --- State (rememberSaveable so it survives rotation/process death where possible) ---
    var heavyBlur by remember { mutableStateOf(defaultHeavyBlur.value) } // store as Float
    var dimDarken by remember { mutableStateOf(defaultDimDarken) }
    var dimLighten by remember { mutableStateOf(defaultDimLighten) }
    var scrimAlpha by remember { mutableStateOf(defaultScrimAlpha) }
    var frostedRadius by remember { mutableStateOf(defaultFrostedRadius.value) }
    var frostedTintAlpha by remember { mutableStateOf(defaultFrostedTintAlpha) }

    var blurInteraction by rememberSaveable { mutableStateOf(VeilInteraction.TapToggle) }
    var dimDarkenInteraction by rememberSaveable { mutableStateOf(VeilInteraction.TapToggle) }
    var dimLightenInteraction by rememberSaveable { mutableStateOf(VeilInteraction.TapToggle) }
    var scrimInteraction by rememberSaveable { mutableStateOf(VeilInteraction.TapToggle) }
    var desaturateInteraction by rememberSaveable { mutableStateOf(VeilInteraction.TapToggle) }
    var censorBarsInteraction by rememberSaveable { mutableStateOf(VeilInteraction.TapToggle) }
    var frostedInteraction by rememberSaveable { mutableStateOf(VeilInteraction.TapToggle) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Veil – Obvious Effects Demo") }) },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {

            // 1) Heavy Blur
            Section(
                title = "Heavy Blur (${heavyBlur.toInt()}dp) over gradient",
                controls = {
                    DpSlider(
                        label = "Blur radius",
                        value = heavyBlur, onValueChange = { heavyBlur = it },
                        valueRange = 0f..40f
                    )
                }
            ) {
                DemoTile(
                    effect = VeilEffect.Blur(heavyBlur.dp),
                    subtitle = "Hold to peek",
                    interaction = blurInteraction
                ) { GradientCanvas() }
            }

            // 2) Dim (Darken)
            Section(
                title = "Dim Darken (amount = ${fmt1(dimDarken)})",
                controls = {
                    FloatSlider(
                        label = "Darken amount",
                        value = dimDarken, onValueChange = { dimDarken = it },
                        valueRange = 0f..1f
                    )
                }
            ) {
                DemoTile(
                    effect = VeilEffect.Dim(amount = dimDarken, mode = VeilEffect.Dim.Mode.Darken),
                    subtitle = "Hold to peek",
                    interaction = dimDarkenInteraction
                ) { VividColorsGrid() }
            }

            // 3) Dim (Lighten)
            Section(
                title = "Dim Lighten (amount = ${fmt1(dimLighten)})",
                controls = {
                    FloatSlider(
                        label = "Lighten amount",
                        value = dimLighten, onValueChange = { dimLighten = it },
                        valueRange = 0f..1f
                    )
                }
            ) {
                DemoTile(
                    effect = VeilEffect.Dim(amount = dimLighten, mode = VeilEffect.Dim.Mode.Lighten),
                    subtitle = "Hold to peek",
                    interaction = dimLightenInteraction
                ) { PhotoLikeContent() }
            }

            // 4) Scrim
            Section(
                title = "Scrim (Red ${(scrimAlpha * 100).toInt()}%)",
                controls = {
                    FloatSlider(
                        label = "Scrim alpha",
                        value = scrimAlpha, onValueChange = { scrimAlpha = it },
                        valueRange = 0f..1f
                    )
                }
            ) {
                DemoTile(
                    effect = VeilEffect.Scrim(color = Color.Red.copy(alpha = scrimAlpha)),
                    subtitle = "Hold to peek",
                    interaction = scrimInteraction
                ) { TextWall() }
            }

            // 5) Frosted
            Section(
                title = "Frosted (radius = ${frostedRadius.toInt()}dp, tint alpha = ${fmt2(frostedTintAlpha)})",
                controls = {
                    DpSlider(
                        label = "Blur radius",
                        value = frostedRadius, onValueChange = { frostedRadius = it },
                        valueRange = 0f..40f
                    )
                    FloatSlider(
                        label = "Tint alpha",
                        value = frostedTintAlpha, onValueChange = { frostedTintAlpha = it },
                        valueRange = 0f..0.35f
                    )
                }
            ) {
                DemoTile(
                    effect = VeilEffect.Frosted(
                        radius = frostedRadius.dp,
                        tint = Color.White.copy(alpha = frostedTintAlpha)
                    ),
                    subtitle = "Hold to peek",
                    interaction = frostedInteraction
                ) { GradientCanvas() }
            }
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
private fun Section(
    title: String,
    controls: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    controls()
    content()
}

@Composable
private fun InteractionSelector(
    title: String,
    value: VeilInteraction,
    onChange: (VeilInteraction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$title: ${value.name}", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }) {
            Text(value.name)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            VeilInteraction.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        onChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FloatSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Int = 0
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label: ${fmt2(value)}", style = MaterialTheme.typography.labelLarge)
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = step)
    }
}

@Composable
private fun DpSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Int = 0
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label: ${value.toInt()} dp", style = MaterialTheme.typography.labelLarge)
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = step)
    }
}

private fun fmt1(v: Float) = String.format("%.1f", v)
private fun fmt2(v: Float) = String.format("%.2f", v)

/* ---------- Reusable demo tile ---------- */

@Composable
private fun DemoTile(
    effect: VeilEffect,
    interaction: VeilInteraction,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .veil(effect = effect, interaction = interaction),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            content()
            LabelHint(subtitle)
        }
    }
}

/* ---------- Content blocks chosen to emphasize differences ---------- */

@Composable
private fun GradientCanvas() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6A11CB), // purple
                        Color(0xFF2575FC), // blue
                        Color(0xFFFEB692), // peach
                        Color(0xFFFF5F6D)  // coral
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
    )
}

@Composable
private fun VividColorsGrid() {
    val colors = listOf(
        Color(0xFFFF5252), Color(0xFFFFD740), Color(0xFF69F0AE),
        Color(0xFF40C4FF), Color(0xFF7C4DFF), Color(0xFFFF4081)
    )
    val size = 5
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(size) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(size) { col ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(20.dp)
                            .background(colors[(row + col) % colors.size], RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoLikeContent() {
    // Simulate a photo: gradient + noise-ish stripes + overlay text
    Box(Modifier.fillMaxSize()) {
        GradientCanvas()
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sunset over City", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("Vibrant colors to show lightening effect", color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun TextWall() {
    val paragraph = """
        This is a block of text with varying emphasis to make overlays obvious.
        Adding more lines increases density and contrast so the scrim reads clearly.
        Tap and hold to see the original brightness and color under the veil.
    """.trimIndent()
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text("Sensitive Document", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(paragraph, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmojiCollage() {
    val emojis = listOf("🍉", "🌈", "🧡", "🎨", "🏝️", "🚗", "🐯", "🌸", "🍰", "⚽")
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                emojis.shuffled().take(8).forEach { e ->
                    Text(e, fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CreditCardMock() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bank of Compose", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Cardholder: JOHN DOE", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Text("Number: 4111 1111 1111 1111", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Expires: 12/29   CVC: 123", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LabelHint(text: String) {
    Box(
        modifier = Modifier
            .padding(12.dp)
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoScreen(onFlagSecure: (Boolean) -> Unit) {

    val effects = listOf(
        VeilEffect.Blur(8.dp),
        VeilEffect.Dim()
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Veil Modifier Demo") })
        },
        containerColor = Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column {
                effects.forEach {
                    Card(
                        modifier = Modifier
                            .size(250.dp)
                            .veil(
                                effect = it,
                                interaction = VeilInteraction.HoldToReveal
                            ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Sensitive Info", style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Long press to toggle veil")
                        }
                    }
                }
            }
        }
    }
}