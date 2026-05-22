package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.db.MessageEntity
import java.net.URLEncoder

import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val selectedPersona by viewModel.selectedPersona.collectAsState()
    val availableModels = viewModel.availableModels
    val personas = viewModel.personas
    val suggestions = viewModel.suggestions
    val followUps by viewModel.followUps.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var textInput by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }
    var personaDropdownExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }
    
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {}

    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.errorMessage.value = null
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "AURA History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { viewModel.createNewSession(); scope.launch { drawerState.close() } }) {
                        Icon(Icons.Filled.Add, contentDescription = "New Chat")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(allSessions) { session ->
                        NavigationDrawerItem(
                            label = { 
                                Column {
                                    Text(session.title, maxLines = 1, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(session.timestamp)), 
                                        style = MaterialTheme.typography.bodySmall, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            selected = session.id == currentSessionId,
                            onClick = { 
                                viewModel.loadSession(session.id)
                                scope.launch { drawerState.close() }
                            },
                            badge = {
                                IconButton(onClick = { viewModel.deleteSession(session.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete Session", modifier = Modifier.size(20.dp))
                                }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { modelDropdownExpanded = true }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = selectedModel.first,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Select Model", modifier = Modifier.size(20.dp))
                                }
                                DropdownMenu(
                                    expanded = modelDropdownExpanded,
                                    onDismissRequest = { modelDropdownExpanded = false }
                                ) {
                                    availableModels.forEach { model ->
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    model.first, 
                                                    fontWeight = if (selectedModel == model) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (selectedModel == model) MaterialTheme.colorScheme.primary else Color.Unspecified
                                                ) 
                                            },
                                            onClick = {
                                                viewModel.setSelectedModel(model)
                                                modelDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { personaDropdownExpanded = true }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = "Persona", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = selectedPersona.first,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                DropdownMenu(
                                    expanded = personaDropdownExpanded,
                                    onDismissRequest = { personaDropdownExpanded = false }
                                ) {
                                    personas.forEach { persona ->
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    persona.first, 
                                                    fontWeight = if (selectedPersona == persona) FontWeight.Bold else FontWeight.Normal
                                                ) 
                                            },
                                            onClick = {
                                                viewModel.setSelectedPersona(persona)
                                                personaDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open History")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.showSettings.value = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.showCodeEditor.value = true }) {
                            Icon(Icons.Filled.Code, contentDescription = "IDE Compiler", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.clearChat() }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Clear Chat")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                        .padding(8.dp)
                        .navigationBarsPadding()
                ) {
                    AnimatedVisibility(visible = messages.isEmpty() || followUps.isNotEmpty()) {
                        val chipsToShow = if (messages.isEmpty()) suggestions else followUps
                        LazyRow(
                            modifier = Modifier.padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chipsToShow) { text ->
                                SuggestionChip(
                                    onClick = { 
                                        viewModel.sendMessage(text, null) 
                                    },
                                    label = { Text(text) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = selectedImageUri != null) {
                        Box(modifier = Modifier.padding(bottom = 8.dp)) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected Image",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove Image", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Photo", tint = MaterialTheme.colorScheme.primary)
                        }
                        TextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Message Aura...") },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp)),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        if (textInput.isBlank() && selectedImageUri == null) {
                            IconButton(onClick = { 
                                // Request voice input via Intent here in real app
                            }) {
                                Icon(Icons.Filled.Mic, contentDescription = "Voice Mode", tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (textInput.isNotBlank() || selectedImageUri != null) {
                                        viewModel.sendMessage(textInput, selectedImageUri)
                                        textInput = ""
                                        selectedImageUri = null
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                if (isTyping) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.Top
            ) {
                if (messages.isEmpty() && !isTyping) {
                    item {
                        EmptyState()
                    }
                }
                items(messages) { message ->
                    MessageBubble(message)
                }
            }
        }
    }
    
    if (showSettings) {
        SettingsDialog(viewModel = viewModel)
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Welcome to Aura",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Ask me anything, share an image, or ask me to generate a picture.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun MessageBubble(message: MessageEntity) {
    val isUser = message.role == "user"
    val bgColor = if (isUser) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val textColor = MaterialTheme.colorScheme.onBackground
    
    // Check if the message is an image generation wrapper
    val isGeneratedImage = message.textContent.startsWith("[IMAGE:") && message.textContent.endsWith("]")
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(vertical = 16.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().widthIn(max = 800.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isUser) MaterialTheme.colorScheme.primary else Color(0xFFB066FE)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isUser) "U" else "A",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    if (isUser) "You" else "Aura",
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (message.imageLocalUri != null) {
                    AsyncImage(
                        model = message.imageLocalUri,
                        contentDescription = "User Attachment",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .padding(bottom = if (message.textContent.isNotEmpty()) 8.dp else 0.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                
                if (message.textContent.isNotEmpty()) {
                    if (isGeneratedImage) {
                        val promptText = message.textContent.removePrefix("[IMAGE:").removeSuffix("]").trim()
                        val encodedPrompt = URLEncoder.encode(promptText, "UTF-8")
                        val imageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt"
                        
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "AI Generated Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        MarkdownTextRenderer(message.textContent, textColor)
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownTextRenderer(text: String, defaultColor: Color) {
    val parts = text.split("```")
    Column {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) { // It's a code block
                val codeLines = part.trim().lines()
                val language = codeLines.firstOrNull() ?: ""
                val codeStartIdx = if (part.trim().startsWith(language)) language.length else 0
                val actualCode = part.trim().substring(codeStartIdx).trim()

                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF18181A))
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF2D2D2D)).padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(language.ifEmpty { "code" }, color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text(
                            text = actualCode,
                            color = Color(0xFF00FF41),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            } else {
                if (part.isNotEmpty()) {
                    Text(
                        text = buildMarkdownAnnotatedString(part),
                        color = defaultColor,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun buildMarkdownAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        // Covers headers, bold, italic, inline code
        val regex = "(?m)^(#{1,6})\\s+(.*?)$|\\*\\*(.*?)\\*\\*|\\*(.*?)\\*|`(.*?)`".toRegex()
        val matchResults = regex.findAll(text)

        for (match in matchResults) {
            append(text.substring(currentIndex, match.range.first))
            when {
                match.groups[1] != null -> { // Headers
                    val level = match.groups[1]!!.value.length
                    val fontSize = when(level) {
                        1 -> 24.sp
                        2 -> 20.sp
                        3 -> 18.sp
                        else -> 16.sp
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize)) {
                        append(match.groups[2]!!.value + "\n")
                    }
                }
                match.groups[3] != null -> { // **bold**
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groups[3]!!.value)
                    }
                }
                match.groups[4] != null -> { // *italic*
                    withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                        append(match.groups[4]!!.value)
                    }
                }
                match.groups[5] != null -> { // `code`
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0xFF333333), color = Color(0xFF00FF41))) {
                        append(match.groups[5]!!.value)
                    }
                }
            }
            currentIndex = match.range.last + 1
        }
        append(text.substring(currentIndex))
    }
}

@Composable
fun SettingsDialog(viewModel: ChatViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.showSettings.value = false },
        title = {
            Text("Settings")
        },
        text = {
            Column {
                Text("Theme", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = true, onClick = {})
                    Text("Dark Mode")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = false, onClick = {})
                    Text("Light Mode")
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Data & Privacy", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.clearChat() }) {
                    Text("Clear Current Chat")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Version", fontWeight = FontWeight.Bold)
                Text("Aura v1.0.0 (Bleeding Edge)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.showSettings.value = false }) {
                Text("Done")
            }
        }
    )
}
