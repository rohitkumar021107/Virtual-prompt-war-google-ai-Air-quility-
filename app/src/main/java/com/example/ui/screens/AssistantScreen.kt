package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.domain.model.ChatMessage
import com.example.domain.model.LocationCity
import com.example.ui.AirQualityUiState
import com.example.ui.components.AiStatusBanner

private val SUGGESTED_QUESTIONS = listOf(
    "How can I protect my family from winter smog?",
    "Is it safe to exercise outside today?",
    "How can I reduce indoor PM2.5?",
    "What should children do on high AQI days?",
    "Which transport option is more sustainable?",
    "How can my community reduce pollution?"
)

@Composable
fun AssistantScreen(
    city: LocationCity,
    airQualityState: AirQualityUiState,
    messages: List<ChatMessage>,
    isStreaming: Boolean,
    onSendQuestion: (String) -> Unit,
    onClearChat: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    fun submitInput() {
        val q = inputText.trim()
        if (q.isNotEmpty() && !isStreaming) {
            inputText = ""
            keyboardController?.hide()
            onSendQuestion(q)
        }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val liveLabel = when (airQualityState) {
        is AirQualityUiState.Success -> "Live context: ${airQualityState.snapshot.location.name} US AQI ${airQualityState.snapshot.usAqi ?: "N/A"}"
        AirQualityUiState.Loading -> "Fetching live AQI context for ${city.name}..."
        is AirQualityUiState.Error -> "Live AQI unavailable — responses will disclose missing live readings."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 960.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AirCare Assistant",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = liveLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = onClearChat,
                enabled = messages.size > 1 || isStreaming,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("assistant_clear_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = "Clear conversation"
                )
            }
        }

        AiStatusBanner(
            text = "Powered by Gemini (gemini-3.8-flash) via Firebase AI Logic • Informational guidance only (not medical advice)",
            isLiveGemini = true
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(SUGGESTED_QUESTIONS) { index, prompt ->
                AssistChip(
                    onClick = { if (!isStreaming) onSendQuestion(prompt) },
                    label = { Text(prompt, maxLines = 1) },
                    modifier = Modifier.testTag("assistant_chip_$index")
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(messages, key = { _, msg -> msg.id }) { _, msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .semantics(mergeDescendants = true) {
                                contentDescription = if (msg.isUser) {
                                    "Your question: ${msg.text}"
                                } else {
                                    "AirCare Assistant reply: ${msg.text}"
                                }
                            },
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (msg.isUser) 18.dp else 4.dp,
                            bottomEnd = if (msg.isUser) 4.dp else 18.dp
                        ),
                        color = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        tonalElevation = if (msg.isUser) 0.dp else 2.dp
                    ) {
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (msg.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("assistant_input"),
                placeholder = { Text("Ask AirCare Assistant...") },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submitInput() })
            )
            FilledIconButton(
                onClick = { submitInput() },
                enabled = inputText.isNotBlank() && !isStreaming,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("assistant_send_button")
            ) {
                if (isStreaming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send question")
                }
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}
