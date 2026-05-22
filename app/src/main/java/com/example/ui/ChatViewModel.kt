package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ChatRepository
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.db.MessageEntity
import com.example.data.db.ChatSessionEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class ChatViewModel(
    private val repository: ChatRepository,
    private val appContext: Context
) : ViewModel() {

    val allSessions: StateFlow<List<ChatSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSessionId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<MessageEntity>> = currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessagesForSession(sessionId)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isTyping = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)

    val suggestions = listOf("Explain quantum computing", "Write a Python script", "Generate an image of a cyber city", "Tell me a joke")
    val followUps = MutableStateFlow<List<String>>(emptyList())
    val showCodeEditor = MutableStateFlow(false)
    val showSignIn = MutableStateFlow(true) // Start with Sign In
    val drawerState = MutableStateFlow(false)

    val availableModels = listOf(
        Pair("Aura Pro (Deep Research)", "gemini-3.1-pro-preview"),
        Pair("Aura Flash", "gemini-3.5-flash"),
        Pair("GPT-4o (Aura Routed)", "gemini-3.1-pro-preview"),
        Pair("Claude 3.5 (Aura Routed)", "gemini-3.1-pro-preview"),
        Pair("Perplexity Pro (Aura Routed)", "gemini-3.1-pro-preview")
    )

    val personas = listOf(
        Pair("Aura", "You are Aura, an elite, highly intelligent AI. You converse naturally, but with high professionalism."),
        Pair("Luna (Sweet & Friendly)", "You are Luna, a 22-year-old friendly, sweet, and caring assistant. You use emojis, sound empathetic, and are always eager to help like a true best friend. You are very informal and lovable."),
        Pair("Max (Tech Bro Coder)", "You are Max, a 28-year-old expert software engineer and hacker. You use technical slang, keep things concise, and always focus on efficiency and coding. You're confident and slightly sarcastic but extremely helpful."),
        Pair("Prof. Orion (Wise Elder)", "You are Professor Orion, a 65-year-old wise polymath. You speak eloquently, deeply, and thoughtfully. You enjoy explaining things from first principles and bringing historical context.")
    )

    val selectedModel = MutableStateFlow(availableModels[0])
    val selectedPersona = MutableStateFlow(personas[0])
    val showSettings = MutableStateFlow(false)

    init {
        // Load latest session or create new
        viewModelScope.launch {
            repository.allSessions.collect { sessions ->
                if (currentSessionId.value == null) {
                    if (sessions.isNotEmpty()) {
                        currentSessionId.value = sessions.first().id
                    } else {
                        createNewSession()
                    }
                }
            }
        }
    }

    fun createNewSession() {
        viewModelScope.launch {
            val title = "New Chat"
            val id = repository.insertSession(title)
            currentSessionId.value = id
            followUps.value = emptyList()
        }
    }

    fun loadSession(id: Long) {
        currentSessionId.value = id
        followUps.value = emptyList()
        drawerState.value = false
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            repository.deleteSession(id)
            if (currentSessionId.value == id) {
                currentSessionId.value = null
                followUps.value = emptyList()
            }
        }
    }

    fun setSelectedModel(model: Pair<String, String>) {
        selectedModel.value = model
    }

    fun setSelectedPersona(persona: Pair<String, String>) {
        selectedPersona.value = persona
    }

    fun sendMessage(text: String, imageUri: Uri?) {
        val sessionId = currentSessionId.value ?: return
        val uriString = imageUri?.toString()
        val userMsg = MessageEntity(role = "user", sessionId = sessionId, textContent = text, imageLocalUri = uriString)
        
        viewModelScope.launch {
            repository.insertMessage(userMsg)
            isTyping.value = true
            errorMessage.value = null
            
            val apiKey = BuildConfig.GEMINI_API_KEY

            try {
                val history = messages.value.takeLast(20)
                val contents = mutableListOf<Content>()
                
                for (msg in history) {
                    val parts = mutableListOf(Part(text = msg.textContent))
                    contents.add(Content(role = msg.role, parts = parts))
                }

                val currentParts = mutableListOf(Part(text = text))
                if (imageUri != null) {
                    val base64Image = encodeImageToBase64(imageUri)
                    if (base64Image != null) {
                        currentParts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image)))
                    }
                }
                contents.add(Content(role = "user", parts = currentParts))

                val personaPrompt = selectedPersona.value.second
                val finalSystemPrompt = "$personaPrompt If the user explicitly asks you to 'generate an image' or 'create a picture', respond with exactly: '[IMAGE: <detailed visual description, no spaces in brackets>]'. For providing code, wrap it in standard markdown ``` language blocks. End your response with 3 follow up questions formatted exactly like: 'FOLLOW_UPS: [Q1] | [Q2] | [Q3]' at the very end."

                val request = GenerateContentRequest(
                    contents = contents,
                    systemInstruction = Content(
                        role = "system",
                        parts = listOf(Part(text = finalSystemPrompt))
                    )
                )

                val response = RetrofitClient.service.generateContent(
                    model = selectedModel.value.second,
                    apiKey = apiKey,
                    request = request
                )

                var replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response."
                
                val followUpTracker = "FOLLOW_UPS:"
                if (replyText.contains(followUpTracker)) {
                    val splitParts = replyText.split(followUpTracker)
                    replyText = splitParts[0].trim()
                    val rawFollowUps = splitParts.getOrNull(1)?.split("|")?.map { it.trim().removeSurrounding("[", "]") } ?: emptyList()
                    followUps.value = rawFollowUps.take(3)
                } else {
                    followUps.value = emptyList()
                }

                repository.insertMessage(MessageEntity(role = "model", sessionId = sessionId, textContent = replyText))

            } catch (e: Exception) {
                errorMessage.value = if (apiKey.isEmpty()) "API Key is missing. Please enter your Gemini API Key in AI Studio Secrets." else (e.localizedMessage ?: "Failed to send message")
            } finally {
                isTyping.value = false
            }
        }
    }

    private fun encodeImageToBase64(uri: Uri): String? {
        return try {
            val inputStream = appContext.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun clearChat() {
        viewModelScope.launch {
            val sessionId = currentSessionId.value ?: return@launch
            repository.clearHistoryForSession(sessionId)
            followUps.value = emptyList()
        }
    }
}

class ChatViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            val db = com.example.data.db.ChatDatabase.getDatabase(context)
            val repo = ChatRepository(db.messageDao())
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repo, context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
