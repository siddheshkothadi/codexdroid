package me.siddheshkothadi.codexdroid.feature.settings.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
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
    VoiceOption("Ritu", "Ritu"),
    VoiceOption("Amelia", "Amelia"),
    VoiceOption("Sophia", "Sophia"),
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
    var temperatureDraft by remember(uiState.settings.temperature) { mutableFloatStateOf(uiState.settings.temperature) }

    LaunchedEffect(uiState.settings.pace) {
        paceDraft = uiState.settings.pace
    }
    LaunchedEffect(uiState.settings.temperature) {
        temperatureDraft = uiState.settings.temperature
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
            VoiceSelectorField(
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
                        thumbColor = colors.controlStrong,
                        activeTrackColor = colors.controlStrong,
                        inactiveTrackColor = colors.borderDefault,
                    ),
            )

            Text(
                text = "Temperature: ${"%.2f".format(temperatureDraft)}",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary
            )
            Slider(
                value = temperatureDraft,
                onValueChange = { temperatureDraft = it },
                valueRange = 0.01f..2.0f,
                onValueChangeFinished = { viewModel.onTemperatureChanged(temperatureDraft) },
                colors =
                    SliderDefaults.colors(
                        thumbColor = colors.controlStrong,
                        activeTrackColor = colors.controlStrong,
                        inactiveTrackColor = colors.borderDefault,
                    ),
            )

            Text(
                text = "Sarvam API Key",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary
            )
            TextField(
                value = uiState.sarvamApiKeyDraft,
                onValueChange = viewModel::onSarvamApiKeyDraftChanged,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                placeholder = { Text("Optional API key", color = colors.textSecondary) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(width = 1.dp, color = colors.inputFieldBorder, shape = RoundedCornerShape(24.dp)),
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
            Text(
                text = "Auto-saved while typing.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            Text(
                text = "Codex Features",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text =
                    uiState.activeConnectionName
                        ?.let { "Connected server: $it" }
                        ?: "Connect a server from Session to manage feature flags.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
            OutlinedButton(
                onClick = { viewModel.refreshCodexFeatures() },
                enabled = uiState.activeConnectionName != null && !uiState.featuresLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.monochromeActionBackground
                )
            ) {
                Text("Refresh features")
            }

            if (uiState.featuresLoading) {
                Text(
                    text = "Loading feature flags...",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            if (!uiState.featuresError.isNullOrBlank()) {
                Text(
                    text = uiState.featuresError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.accentError,
                )
            }

            if (uiState.stableFeatures.isNotEmpty()) {
                Text(
                    text = "Stable features",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 6.dp),
                )
                uiState.stableFeatures.forEach { feature ->
                    FeatureToggleRow(
                        feature = feature,
                        isUpdating = uiState.featureUpdatingKey == feature.name,
                        colors = colors,
                        onToggle = { viewModel.onToggleCodexFeature(feature) },
                    )
                }
            }

            if (uiState.experimentalFeatures.isNotEmpty()) {
                Text(
                    text = "Experimental features",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 6.dp),
                )
                uiState.experimentalFeatures.forEach { feature ->
                    FeatureToggleRow(
                        feature = feature,
                        isUpdating = uiState.featureUpdatingKey == feature.name,
                        colors = colors,
                        onToggle = { viewModel.onToggleCodexFeature(feature) },
                    )
                }
            }

            if (
                !uiState.featuresLoading &&
                uiState.featuresError.isNullOrBlank() &&
                uiState.activeConnectionName != null &&
                uiState.stableFeatures.isEmpty() &&
                uiState.experimentalFeatures.isEmpty()
            ) {
                Text(
                    text = "No stable or experimental features were returned by the server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun FeatureToggleRow(
    feature: CodexFeatureUi,
    isUpdating: Boolean,
    colors: CodexColors,
    onToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = !isUpdating, onClick = onToggle)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = featureLabel(feature),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
            Text(
                text = featureSubtitle(feature),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.padding(horizontal = 6.dp))
        Switch(
            checked = feature.enabled,
            enabled = !isUpdating,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.controlStrongOn,
                checkedTrackColor = colors.controlStrong,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.bgSecondary
            )
        )
    }
}

private fun featureLabel(feature: CodexFeatureUi): String {
    val display = feature.displayName?.trim().orEmpty()
    if (display.isNotBlank()) return display
    return feature.name
        .split("_")
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.replaceFirstChar { c -> c.uppercase() }
        }
}

private fun featureSubtitle(feature: CodexFeatureUi): String {
    val description = feature.description?.trim().orEmpty()
    if (description.isNotBlank()) return description
    val announcement = feature.announcement?.trim().orEmpty()
    if (announcement.isNotBlank()) return announcement
    return "Feature key: features.${feature.name}"
}

@Composable
private fun VoiceSelectorField(
    selectedLabel: String,
    options: List<VoiceOption>,
    colors: CodexColors,
    onSelect: (VoiceOption) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .clickable { expanded = true }
                    .border(width = 1.dp, color = colors.inputFieldBorder, shape = shape),
            color = colors.inputFieldBackground,
            shape = shape,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand voice options",
                    tint = colors.textSecondary
                )
            }
        }

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
