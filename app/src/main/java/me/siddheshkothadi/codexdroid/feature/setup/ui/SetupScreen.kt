package me.siddheshkothadi.codexdroid.feature.setup.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
        containerColor = CodexTheme.colors.bgPrimary,
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
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            Surface(
                color = CodexTheme.colors.bgPrimary,
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
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = { onSaveClick(name, url, secret, sarvamApiKey) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = url.isNotBlank() && name.isNotBlank()
                        ) {
                            Text(if (isEditMode) "Update Connection" else "Connect & Save")
                        }
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
                    color = CodexTheme.colors.accentError,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Connection Name (e.g. Local Server)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Base URL (e.g. http://192.168.1.3:8080)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
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

