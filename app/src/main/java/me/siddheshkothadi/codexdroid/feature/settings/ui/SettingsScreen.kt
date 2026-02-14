package me.siddheshkothadi.codexdroid.feature.settings.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.siddheshkothadi.codexdroid.ui.theme.CodexColors
import me.siddheshkothadi.codexdroid.ui.theme.CodexTheme

private data class VoiceOption(
    val label: String,
    val value: String,
)

private val SarvamVoiceOptions = listOf(
    VoiceOption("Shubh (default)", "Shubh"),
    VoiceOption("Aditya", "Aditya"),
    VoiceOption("Ritu", "Ritu"),
    VoiceOption("Priya", "Priya"),
    VoiceOption("Neha", "Neha"),
    VoiceOption("Rahul", "Rahul"),
    VoiceOption("Pooja", "Pooja"),
    VoiceOption("Rohan", "Rohan"),
    VoiceOption("Simran", "Simran"),
    VoiceOption("Kavya", "Kavya"),
    VoiceOption("Amit", "Amit"),
    VoiceOption("Dev", "Dev"),
    VoiceOption("Ishita", "Ishita"),
    VoiceOption("Shreya", "Shreya"),
    VoiceOption("Ratan", "Ratan"),
    VoiceOption("Varun", "Varun"),
    VoiceOption("Manan", "Manan"),
    VoiceOption("Sumit", "Sumit"),
    VoiceOption("Roopa", "Roopa"),
    VoiceOption("Kabir", "Kabir"),
    VoiceOption("Aayan", "Aayan"),
    VoiceOption("Ashutosh", "Ashutosh"),
    VoiceOption("Advait", "Advait"),
    VoiceOption("Amelia", "Amelia"),
    VoiceOption("Sophia", "Sophia"),
    VoiceOption("Anand", "Anand"),
    VoiceOption("Tanya", "Tanya"),
    VoiceOption("Tarun", "Tarun"),
    VoiceOption("Sunny", "Sunny"),
    VoiceOption("Mani", "Mani"),
    VoiceOption("Gokul", "Gokul"),
    VoiceOption("Vijay", "Vijay"),
    VoiceOption("Shruti", "Shruti"),
    VoiceOption("Suhani", "Suhani"),
    VoiceOption("Mohit", "Mohit"),
    VoiceOption("Kavitha", "Kavitha"),
    VoiceOption("Rehan", "Rehan"),
    VoiceOption("Soham", "Soham"),
    VoiceOption("Rupali", "Rupali"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = CodexTheme.colors
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var paceDraft by remember(uiState.settings.pace) { mutableFloatStateOf(uiState.settings.pace) }

    LaunchedEffect(uiState.settings.pace) {
        paceDraft = uiState.settings.pace
    }

    val selectedVoiceLabel =
        remember(uiState.settings.voice) {
            SarvamVoiceOptions.firstOrNull { it.value == uiState.settings.voice }?.label ?: uiState.settings.voice
        }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors =
                    TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = colors.bgPrimary,
                        scrolledContainerColor = colors.bgPrimary,
                        titleContentColor = colors.textPrimary,
                        navigationIconContentColor = colors.textPrimary,
                    ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = colors.bgPrimary
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Voice",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary
            )
            VoiceDropdownField(
                selectedLabel = selectedVoiceLabel,
                options = SarvamVoiceOptions,
                colors = colors,
                onSelect = { viewModel.onVoiceChanged(it.value) },
            )

            Text(
                text = "Pace: ${"%.2f".format(paceDraft)}",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary
            )
            Slider(
                value = paceDraft,
                onValueChange = { paceDraft = it },
                valueRange = 0.5f..2.0f,
                onValueChangeFinished = { viewModel.onPaceChanged(paceDraft) },
                colors =
                    SliderDefaults.colors(
                        thumbColor = colors.accentPrimary,
                        activeTrackColor = colors.accentPrimary,
                        inactiveTrackColor = colors.borderDefault,
                    ),
            )

            Text(
                text = "Sarvam API Key",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary
            )
            ApiKeyEditor(
                value = uiState.sarvamApiKeyDraft,
                onValueChange = viewModel::onSarvamApiKeyDraftChanged,
                onSave = viewModel::saveSarvamApiKey,
                colors = colors,
            )

            uiState.statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun VoiceDropdownField(
    selectedLabel: String,
    options: List<VoiceOption>,
    colors: CodexColors,
    onSelect: (VoiceOption) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(shape)
                    .border(width = 1.dp, color = colors.inputFieldBorder, shape = shape),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand voice options",
                    tint = colors.textSecondary
                )
            },
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = colors.inputFieldBackground,
                    unfocusedContainerColor = colors.inputFieldBackground,
                    disabledContainerColor = colors.inputFieldBackground,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    disabledTextColor = colors.textPrimary,
                )
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.95f),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ApiKeyEditor(
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    colors: CodexColors,
) {
    val shape = RoundedCornerShape(24.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            placeholder = { Text("Optional API key", color = colors.textSecondary) },
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .clip(shape)
                    .border(width = 1.dp, color = colors.inputFieldBorder, shape = shape),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = colors.inputFieldBackground,
                    unfocusedContainerColor = colors.inputFieldBackground,
                    disabledContainerColor = colors.inputFieldBackground,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    disabledTextColor = colors.textPrimary,
                    disabledPlaceholderColor = colors.textSecondary,
                )
        )
        FilledIconButton(
            onClick = onSave,
            colors =
                IconButtonDefaults.filledIconButtonColors(
                    containerColor = colors.bgSecondary,
                    contentColor = colors.textPrimary,
                )
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Update API key",
            )
        }
    }
}
