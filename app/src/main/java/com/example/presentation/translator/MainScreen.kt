package com.example.presentation.translator

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.HistoryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isSheetOpen by remember { mutableStateOf(false) }
    var currentSheet by remember { mutableStateOf<SheetType>(SheetType.Settings) }
    
    val layoutDirection = if (uiState.isEnglish) LayoutDirection.Ltr else LayoutDirection.Rtl

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Morse Translator", fontWeight = FontWeight.Medium) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleLanguage() }) {
                            Icon(Icons.Default.Language, contentDescription = "Language")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.saveToHistory(); Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("MT", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = true,
                        onClick = { },
                        icon = { Icon(Icons.Default.Translate, contentDescription = "Translate") },
                        label = { Text("Translate") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { onNavigateToHistory() },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { currentSheet = SheetType.CheatSheet; isSheetOpen = true },
                        icon = { Icon(Icons.Default.Info, contentDescription = "Cheat Sheet") },
                        label = { Text("Cheat Sheet") }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Input Card
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.toggleLanguage() }
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "INPUT • ${if (uiState.isEnglish) "ENGLISH" else "PERSIAN"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                "CLEAR",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.onInputTextChanged("") }
                                    .padding(4.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        TextField(
                            value = uiState.inputText,
                            onValueChange = { viewModel.onInputTextChanged(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 80.dp),
                            textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            placeholder = { Text("Enter text to translate...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Output Card
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        Modifier
                            .padding(20.dp)
                            .fillMaxSize()
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "OUTPUT • MORSE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(uiState.outputMorse))
                                    Toast.makeText(context, "Morse copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                text = uiState.outputMorse,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    letterSpacing = 4.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Playback Controls inside Output Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.togglePlayback() },
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                            ) {
                                Icon(
                                    if (uiState.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Stop",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(Modifier.width(16.dp))

                            Row(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { currentSheet = SheetType.Settings; isSheetOpen = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Box(
                                    Modifier
                                        .width(1.dp)
                                        .height(24.dp)
                                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                )
                                IconButton(
                                    onClick = { shareText(context, "${uiState.inputText}\n${uiState.outputMorse}") },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "WPM: ${uiState.wpm}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "PITCH: ${uiState.pitchHz}HZ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                // TAP Button
                TapButton(
                    onTapDown = { viewModel.onTapDown() },
                    onTapUp = { duration -> viewModel.appendTap(duration) }
                )
            }
        }
    }

    if (isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isSheetOpen = false },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            when (currentSheet) {
                SheetType.Settings -> SettingsSheetContent(viewModel, uiState)
                SheetType.CheatSheet -> CheatSheetContent(uiState.isEnglish)
            }
        }
    }
}

enum class SheetType { Settings, CheatSheet }

@Composable
fun LanguageToggle(isEnglish: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onToggle() }
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isEnglish) MaterialTheme.colorScheme.primary else Color.Transparent)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                "English",
                color = if (isEnglish) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (!isEnglish) MaterialTheme.colorScheme.primary else Color.Transparent)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                "فارسی",
                color = if (!isEnglish) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TapButton(onTapDown: () -> Unit, onTapUp: (Long) -> Unit) {
    var touchDownTime by remember { mutableStateOf(0L) }
    var isPressed by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (isPressed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(28.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        touchDownTime = System.currentTimeMillis()
                        onTapDown()
                        tryAwaitRelease()
                        isPressed = false
                        val duration = System.currentTimeMillis() - touchDownTime
                        onTapUp(duration)
                    }
                )
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(16.dp).background(if (isPressed) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape))
            Spacer(Modifier.height(8.dp))
            Text(
                text = "TAP FOR MANUAL MORSE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SettingsSheetContent(viewModel: MainViewModel, uiState: MainUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text("Playback Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("WPM (${uiState.wpm})", modifier = Modifier.width(80.dp))
            Slider(
                value = uiState.wpm.toFloat(),
                onValueChange = { viewModel.updateSettings(wpm = it.toInt()) },
                valueRange = 5f..40f,
                modifier = Modifier.weight(1f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Pitch (${uiState.pitchHz}Hz)", modifier = Modifier.width(80.dp))
            Slider(
                value = uiState.pitchHz.toFloat(),
                onValueChange = { viewModel.updateSettings(pitch = it.toInt()) },
                valueRange = 400f..1000f,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Audio", modifier = Modifier.weight(1f))
            Switch(checked = uiState.audioEnabled, onCheckedChange = { viewModel.updateSettings(audio = it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Haptic Feedback", modifier = Modifier.weight(1f))
            Switch(checked = uiState.hapticEnabled, onCheckedChange = { viewModel.updateSettings(haptic = it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Flashlight", modifier = Modifier.weight(1f))
            Switch(checked = uiState.flashEnabled, onCheckedChange = { viewModel.updateSettings(flash = it) })
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun CheatSheetContent(isEnglish: Boolean) {
    val items = if (isEnglish) {
        listOf(
            "A" to ".-", "B" to "-...", "C" to "-.-.", "D" to "-..", "E" to ".", "F" to "..-.",
            "G" to "--.", "H" to "....", "I" to "..", "J" to ".---", "K" to "-.-", "L" to ".-..",
            "M" to "--", "N" to "-.", "O" to "---", "P" to ".--.", "Q" to "--.-", "R" to ".-.",
            "S" to "...", "T" to "-", "U" to "..-", "V" to "...-", "W" to ".--", "X" to "-..-",
            "Y" to "-.--", "Z" to "--..",
            "0" to "-----", "1" to ".----", "2" to "..---", "3" to "...--", "4" to "....-", "5" to "....."
        )
    } else {
        listOf(
            "ا" to ".-", "ب" to "-...", "پ" to ".--.", "ت" to "-", "ث" to "-.-.", "ج" to ".---",
            "چ" to "---.", "ح" to "....", "خ" to "-..-", "د" to "-..", "ذ" to "--..", "ر" to ".-.",
            "ز" to "---", "س" to "...", "ش" to "----", "ص" to "-.-.", "ض" to ".--.", "ط" to "..-",
            "ع" to ".-.-", "غ" to "--.", "ف" to "..-.", "ق" to "--.-", "ک" to "-.-", "گ" to "--.",
            "ل" to ".-..", "م" to "--", "ن" to "-.", "و" to ".--", "ه" to "..-..", "ی" to ".."
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Morse Cheat Sheet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        LazyColumn {
            items(items.chunked(2)) { rowItems ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    rowItems.forEach { (char, morse) ->
                        Row(modifier = Modifier.weight(1f).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(char, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                            Text(morse, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

fun shareText(context: Context, text: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}
