package me.siddheshkothadi.codexdroid.feature.setup.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import me.siddheshkothadi.codexdroid.domain.model.Connection
import me.siddheshkothadi.codexdroid.ui.theme.CodexTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onSaveClick: (name: String, url: String, secret: String, sarvamApiKey: String) -> Unit,
    onBackClick: () -> Unit = {},
    canNavigateBack: Boolean = true,
    initialConnection: Connection? = null,
    initialSarvamApiKey: String = "",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = CodexTheme.colors
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf("") }
    var sarvamApiKey by rememberSaveable { mutableStateOf("") }
    var isSecretVisible by rememberSaveable { mutableStateOf(false) }
    var isSarvamApiKeyVisible by rememberSaveable { mutableStateOf(false) }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(initialConnection) {
        if (initialConnection != null) {
            name = initialConnection.name
            url = initialConnection.baseUrl
            secret = initialConnection.secret
        }
    }
    LaunchedEffect(initialSarvamApiKey) {
        sarvamApiKey = initialSarvamApiKey
    }

    val isEditMode = initialConnection != null

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colors.bgPrimary,
        topBar = {
            LargeTopAppBar(
                title = { Text(if (isEditMode) "Edit Connection" else "Setup Codex Connection") },
                navigationIcon = {
                    if (canNavigateBack) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = colors.bgPrimary,
                    scrolledContainerColor = colors.bgPrimary,
                    titleContentColor = colors.textPrimary,
                    navigationIconContentColor = colors.textPrimary,
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            Surface(
                color = colors.bgPrimary,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding() // Handle bottom nav bar insets
                        .imePadding() // Handle keyboard insets
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val canSave = url.isNotBlank() && name.isNotBlank()

                    Button(
                        onClick = {
                            if (!isLoading) {
                                onSaveClick(name, url, secret, sarvamApiKey)
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                        enabled = canSave,
                        shape = RoundedCornerShape(24.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = colors.monochromeActionBackground,
                                contentColor = colors.monochromeActionContent,
                                disabledContainerColor = colors.monochromeActionBackground.copy(alpha = 0.45f),
                                disabledContentColor = colors.monochromeActionContent.copy(alpha = 0.7f),
                            ),
                    ) {
                        if (isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.monochromeActionContent
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                        }
                        Text(
                            text = if (isEditMode) "Update Connection" else "Connect & Save",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = colors.accentError,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Connection Name (e.g. Local Server)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.controlStrong,
                    unfocusedBorderColor = colors.borderDefault
                )
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Base URL (e.g. http://192.168.1.3:8080)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.controlStrong,
                    unfocusedBorderColor = colors.borderDefault
                )
            )

            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it },
                label = { Text("Secret (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = if (isSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.controlStrong,
                    unfocusedBorderColor = colors.borderDefault
                ),
                trailingIcon = {
                    val image = if (isSecretVisible)
                        Icons.Filled.Visibility
                    else
                        Icons.Filled.VisibilityOff

                    IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                        Icon(imageVector = image, contentDescription = if (isSecretVisible) "Hide secret" else "Show secret")
                    }
                }
            )

            OutlinedTextField(
                value = sarvamApiKey,
                onValueChange = { sarvamApiKey = it },
                label = { Text("Sarvam API Key (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = if (isSarvamApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.controlStrong,
                    unfocusedBorderColor = colors.borderDefault
                ),
                trailingIcon = {
                    val image =
                        if (isSarvamApiKeyVisible) {
                            Icons.Filled.Visibility
                        } else {
                            Icons.Filled.VisibilityOff
                        }

                    IconButton(onClick = { isSarvamApiKeyVisible = !isSarvamApiKeyVisible }) {
                        Icon(
                            imageVector = image,
                            contentDescription =
                                if (isSarvamApiKeyVisible) {
                                    "Hide Sarvam API key"
                                } else {
                                    "Show Sarvam API key"
                                }
                        )
                    }
                }
            )
        }
    }
}
