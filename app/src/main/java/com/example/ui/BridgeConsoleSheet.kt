package com.example.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.Violet400

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BridgeConsoleSheet(
    viewModel: ArushiViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var bridgeConsoleLog by remember { mutableStateOf("Android JavaScript Bridge inicializado com sucesso.\nwindow.AndroidBridge está ativo para comandos nativos.") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Slate900,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Bridge Code",
                        tint = Cyan400
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Inspetor da Ponte Nativa Android (Bridge)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("bridge_sheet_close_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.LightGray)
                }
            }

            Text(
                text = "Ponte JavaScript para ações nativas do Android (Web / APK). Expõe openApp, makeCall, callContact, openWhatsApp e openUrl através de window.AndroidBridge.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Test Action Buttons
            Text(
                text = "Comandos de Teste da Ponte",
                style = MaterialTheme.typography.labelMedium,
                color = Cyan400,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val result = viewModel.actionBridge.openWhatsApp()
                        bridgeConsoleLog += "\n> window.AndroidBridge.openWhatsApp()\n$result"
                    },
                    modifier = Modifier.weight(1f).testTag("bridge_btn_whatsapp"),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = Cyan400)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("openWhatsApp()", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = {
                        val result = viewModel.actionBridge.callContact("Mãe")
                        bridgeConsoleLog += "\n> window.AndroidBridge.callContact('Mãe')\n$result"
                    },
                    modifier = Modifier.weight(1f).testTag("bridge_btn_call_mummy"),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = Violet400)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("call('Mãe')", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val result = viewModel.actionBridge.openApp("YouTube")
                        bridgeConsoleLog += "\n> window.AndroidBridge.openApp('YouTube')\n$result"
                    },
                    modifier = Modifier.weight(1f).testTag("bridge_btn_youtube"),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = Cyan400)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("openApp('YouTube')", fontSize = 11.sp)
                }

                FilledTonalButton(
                    onClick = {
                        val result = viewModel.actionBridge.openApp("Settings")
                        bridgeConsoleLog += "\n> window.AndroidBridge.openApp('Settings')\n$result"
                    },
                    modifier = Modifier.weight(1f).testTag("bridge_btn_settings"),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Slate800, contentColor = Cyan400)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("openApp('Settings')", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Console output display
            Text(
                text = "Console de Saída da Ponte (Logs em Tempo Real)",
                style = MaterialTheme.typography.labelMedium,
                color = Color.LightGray,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp)),
                color = Slate950
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    Text(
                        text = bridgeConsoleLog,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Cyan400,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Embedded live WebView verifying JavascriptInterface execution
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate950)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = WebViewClient()
                            addJavascriptInterface(viewModel.actionBridge, "AndroidBridge")
                            val html = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                <style>
                                  body { background:#080C14; color:#38BDF8; font-family:monospace; margin:0; padding:8px; font-size:11px; }
                                  button { background:#1E293B; color:#F8FAFC; border:1px solid #38BDF8; padding:3px 8px; border-radius:4px; font-size:10px; }
                                </style>
                                </head>
                                <body>
                                  <div>WebView com window.AndroidBridge</div>
                                  <button onclick="if(window.AndroidBridge){alert(window.AndroidBridge.isBridgeAvailable());}">Verificar Bridge</button>
                                </body>
                                </html>
                            """.trimIndent()
                            loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
