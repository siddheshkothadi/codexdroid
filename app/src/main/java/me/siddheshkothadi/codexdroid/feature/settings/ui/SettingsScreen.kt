package me.siddheshkothadi.codexdroid.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.siddheshkothadi.codexdroid.domain.model.SarvamTtsSettings
import me.siddheshkothadi.codexdroid.ui.theme.CodexTheme

private val SarvamVoices = listOf(
    "Shubh",
    "Aditya",
    "Ritu",
    "Priya",
    "Neha",
    "Rahul",
    "Pooja",
    "Rohan",
    "Simran",
    "Kavya",
    "Amit",
    "Dev",
    "Ishita",
    "Shreya",
    "Ratan",
    "Varun",
    "Manan",
    "Sumit",
    "Roopa",
    "Kabir",
    "Aayan",
    "Ashutosh",
    "Advait",
    "Amelia",
    "Sophia",
    "Anand",
    "Tanya",
    "Tarun",
    "Sunny",
    "Mani",
    "Gokul",
    "Vijay",
    "Shruti",
    "Suhani",
    "Mohit",
    "Kavitha",
    "Rehan",
    "Soham",
    "Rupali",
)

private val LanguageOptions = listOf(
    "en-IN",
    "hi-IN",
    "bn-IN",
    "gu-IN",
    "kn-IN",
    "ml-IN",
    "mr-IN",
    "od-IN",
    "pa-IN",
    "ta-IN",
    "te-IN",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var paceDraft by remember(uiState.settings.pace) { mutableFloatStateOf(uiState.settings.pace) }
    var temperatureDraft by remember(uiState.settings.temperature) { mutableFloatStateOf(uiState.settings.temperature) }

    LaunchedEffect(uiState.settings.pace) {
        paceDraft = uiState.settings.pace
    }
    LaunchedEffect(uiState.settings.temperature) {
        temperatureDraft = uiState.settings.temperature
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
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = CodexTheme.colors.bgPrimary
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = CodexTheme.colors.bgSecondary,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Sarvam",
                        style = MaterialTheme.typography.titleLarge,
                        color = CodexTheme.colors.textPrimary
                    )
                    Text(
                        text = "Model: bulbul:v3",
                        style = MaterialTheme.typography.bodySmall,
                        color = CodexTheme.colors.textSecondary
                    )

                    OutlinedTextField(
                        value = uiState.sarvamApiKeyDraft,
                        onValueChange = viewModel::onSarvamApiKeyDraftChanged,
                        label = { Text("Sarvam API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation =
                            if (uiState.isSarvamApiKeyVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val icon =
                                if (uiState.isSarvamApiKeyVisible) {
                                    Icons.Default.Visibility
                                } else {
                                    Icons.Default.VisibilityOff
                                }
                            IconButton(onClick = viewModel::toggleSarvamApiKeyVisibility) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription =
                                        if (uiState.isSarvamApiKeyVisible) {
                                            "Hide Sarvam API key"
                                        } else {
                                            "Show Sarvam API key"
                                        }
                                )
                            }
                        }
                    )

                    Button(
                        onClick = viewModel::saveSarvamApiKey,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Update Sarvam API Key")
                    }

                    uiState.statusMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = CodexTheme.colors.textSecondary
                        )
                    }

                    DropdownSelector(
                        label = "Voice",
                        selected = uiState.settings.voice,
                        options = SarvamVoices,
                        onSelect = viewModel::onVoiceChanged,
                    )

                    DropdownSelector(
                        label = "Target language",
                        selected = uiState.settings.targetLanguageCode,
                        options = LanguageOptions,
                        onSelect = viewModel::onTargetLanguageChanged,
                    )

                    Text(
                        text = "Pace: ${"%.2f".format(paceDraft)}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = paceDraft,
                        onValueChange = { paceDraft = it },
                        valueRange = 0.3f..3.0f,
                        onValueChangeFinished = { viewModel.onPaceChanged(paceDraft) },
                    )

                    Text(
                        text = "Temperature: ${"%.2f".format(temperatureDraft)}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = temperatureDraft,
                        onValueChange = { temperatureDraft = it },
                        valueRange = 0.01f..1.0f,
                        onValueChangeFinished = { viewModel.onTemperatureChanged(temperatureDraft) },
                    )

                    Text(
                        text = "Speech sample rate",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SarvamTtsSettings.SUPPORTED_SAMPLE_RATES.toList().sorted().forEach { rate ->
                            val selected = uiState.settings.speechSampleRate == rate
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.onSpeechSampleRateChanged(rate) },
                                label = { Text("${rate / 1000f} kHz") },
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by rememberSaveable(label) { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand $label"
                )
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
