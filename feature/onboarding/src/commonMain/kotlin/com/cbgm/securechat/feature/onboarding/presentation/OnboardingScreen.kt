package com.cbgm.securechat.feature.onboarding.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.feature.identity.presentation.model.IdentityUiState

private val Background = Color(0xFF071A2E)
private val Card = Color(0xFF0D223A)
private val Field = Color(0xFF102A46)
private val Accent = Color(0xFF35E6FF)

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    identityState: IdentityUiState,
    onNext: () -> Unit,
    onRequestPermissions: () -> Unit,
    onChooseAnotherNumber: () -> Unit,
    onRetryAutomaticNumber: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onApproveAndCreate: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Background).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SC", style = MaterialTheme.typography.displayMedium, color = Accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Text("SecureChat", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Private. Encrypted. Yours.", color = Color.White.copy(alpha = .72f))
            Spacer(Modifier.height(28.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Card,
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp
            ) {
                AnimatedContent(
                    targetState = state.page,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "onboardingPage"
                ) { page ->
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        when (page) {
                            OnboardingPage.WELCOME -> WelcomePage(onNext)
                            OnboardingPage.PRIVACY -> PrivacyPage(onNext)
                            OnboardingPage.PERMISSIONS -> PermissionsPage(onRequestPermissions)
                            OnboardingPage.PHONE -> PhonePage(
                                identityState = identityState,
                                isCreating = state.isCreatingIdentity,
                                canRetryAutomatic = state.phonePermissionGranted,
                                onChooseAnotherNumber = onChooseAnotherNumber,
                                onRetryAutomaticNumber = onRetryAutomaticNumber,
                                onPhoneNumberChanged = onPhoneNumberChanged,
                                onApproveAndCreate = onApproveAndCreate
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    Text("Welcome to SecureChat", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(12.dp))
    Text("A private messenger built around end-to-end encryption and an identity that stays on your device.", color = Color.White.copy(alpha = .74f), textAlign = TextAlign.Center)
    Spacer(Modifier.height(24.dp))
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
}

@Composable
private fun PrivacyPage(onNext: () -> Unit) {
    Text("Your privacy comes first", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(18.dp))
    PermissionRow("01", "End-to-end encryption", "Messages are encrypted automatically when both identities are available.")
    PermissionRow("02", "Your contacts stay local", "Contacts are used to match people by phone number on your device.")
    PermissionRow("03", "Your identity belongs to you", "Private identity keys remain protected on this device.")
    Spacer(Modifier.height(20.dp))
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
}

@Composable
private fun PermissionsPage(onRequestPermissions: () -> Unit) {
    Text("Permissions", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Text("SecureChat asks only for features you choose to use.", color = Color.White.copy(alpha = .72f), textAlign = TextAlign.Center)
    Spacer(Modifier.height(18.dp))
    PermissionRow("01", "Notifications", "Receive new-message alerts.")
    PermissionRow("02", "Contacts", "Find existing phone-book contacts.")
    PermissionRow("03", "Camera", "Scan SecureChat identity QR codes.")
    PermissionRow("04", "Phone number", "Try to fill your SIM number automatically without opening a picker.")
    Spacer(Modifier.height(20.dp))
    Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) { Text("Allow and continue") }
    Spacer(Modifier.height(8.dp))
    Text("Denied permissions can be enabled later in system settings.", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .58f), textAlign = TextAlign.Center)
}

@Composable
private fun PermissionRow(index: String, title: String, description: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Text(index, color = Accent, fontWeight = FontWeight.Bold)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .66f))
        }
    }
}

@Composable
private fun PhonePage(
    identityState: IdentityUiState,
    isCreating: Boolean,
    canRetryAutomatic: Boolean,
    onChooseAnotherNumber: () -> Unit,
    onRetryAutomaticNumber: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onApproveAndCreate: () -> Unit
) {
    when (identityState) {
        IdentityUiState.Loading -> {
            CircularProgressIndicator(color = Accent)
            Spacer(Modifier.height(14.dp))
            Text(if (isCreating) "Generating secure identity…" else "Preparing phone setup…", color = Color.White)
        }
        is IdentityUiState.NoIdentity -> {
            Text("Approve your phone number", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("We use it as your stable contact and routing identity. You can edit it before continuing.", color = Color.White.copy(alpha = .72f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = identityState.phoneNumber,
                onValueChange = onPhoneNumberChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Phone number") },
                placeholder = { Text("+491701234567") },
                supportingText = { Text(identityState.phoneNumberError ?: if (identityState.phoneNumber.isBlank()) "No automatic number found. Enter it manually or choose one." else "Detected automatically. Confirm or change it.") },
                isError = identityState.phoneNumberError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                textStyle = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Field,
                    unfocusedContainerColor = Field,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Color.White.copy(alpha = .18f),
                    focusedLabelColor = Accent,
                    unfocusedLabelColor = Color.White.copy(alpha = .72f),
                    cursorColor = Accent
                )
            )
            Spacer(Modifier.height(10.dp))
            if (canRetryAutomatic) {
                OutlinedButton(onClick = onRetryAutomaticNumber, modifier = Modifier.fillMaxWidth()) { Text("Try SIM number again") }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(onClick = onChooseAnotherNumber, modifier = Modifier.fillMaxWidth()) {
                Text(if (identityState.phoneNumber.isBlank()) "Choose phone number" else "Choose another number")
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick = onApproveAndCreate, enabled = identityState.phoneNumber.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Approve and create identity") }
        }
        is IdentityUiState.Ready -> {
            CircularProgressIndicator(color = Accent)
            Spacer(Modifier.height(14.dp))
            Text("Identity ready. Opening SecureChat…", color = Color.White)
        }
        IdentityUiState.IncompleteIdentity -> Text("The local identity is incomplete. SecureChat will not silently replace existing keys.", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        is IdentityUiState.Error -> Text(identityState.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
    }
}
