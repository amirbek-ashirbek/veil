package io.github.amirbekashirbek.veil.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.amirbekashirbek.veil.FlagSecure
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
                    DemoScreen(
                        onFlagSecure = { FlagSecure.setEnabled(activity, it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoScreen(onFlagSecure: (Boolean) -> Unit) {
//    var obscured by remember { mutableStateOf(false) }
//    var flagSecure by remember { mutableStateOf(false) }
//    var flashBlur by remember { mutableStateOf(false) }

//    LaunchedEffect(flashBlur) { if (flashBlur) { delay(2000); flashBlur = false } }

//    LaunchedEffect(flagSecure) { onFlagSecure(flagSecure) }

//    val isObscured = obscured || flashBlur

//    Surface(Modifier.fillMaxSize()) {
//        Column(
//            modifier = Modifier.fillMaxSize().padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text("Veil Sample", style = MaterialTheme.typography.headlineSmall)
//
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                Switch(obscured, { obscured = it })
//                Spacer(Modifier.width(8.dp))
//                Text("Force blur (demo)")
//            }
//
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                Switch(flagSecure, { flagSecure = it })
//                Spacer(Modifier.width(8.dp))
//                Text("Enable FLAG_SECURE")
//            }
//
//            Button(onClick = { flashBlur = true }) { Text("Simulate screenshot (2s blur)") }
//
//            Card(
//                modifier = Modifier
//                    .veil(),
//                shape = RoundedCornerShape(16.dp)
//            ) {
//                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                    Text("Payment details", style = MaterialTheme.typography.titleMedium)
//                    Text("Card: 4321 •••• •••• 1234")
//                    Text("CVV: 7• •")
//                    Text("Email: user@example.com")
//                }
//            }
//
//        }
//    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Veil Modifier Demo") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .size(250.dp)
                    .veil(
                        effect = VeilEffect.Blur(8.dp),
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