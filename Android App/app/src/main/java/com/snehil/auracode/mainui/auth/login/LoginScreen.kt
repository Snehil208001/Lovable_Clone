package com.snehil.auracode.mainui.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snehil.auracode.ui.components.AuraBackground
import com.snehil.auracode.ui.components.AuraTextField
import com.snehil.auracode.ui.components.BrandBadge
import com.snehil.auracode.ui.components.GlassCard
import com.snehil.auracode.ui.components.LinkButton
import com.snehil.auracode.ui.components.PrimaryButton
import com.snehil.auracode.ui.theme.Destructive

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onSignUpClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loggedIn.collect { onLoggedIn() }
    }

    AuraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrandBadge(size = 56)
            Text(
                text = "Welcome back",
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Log in to your AuraCode workspace",
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AuraTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        label = "Email",
                        leadingIcon = Icons.Outlined.MailOutline,
                        keyboardType = KeyboardType.Email,
                        enabled = !state.loading,
                        isError = state.error != null
                    )
                    AuraTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = "Password",
                        leadingIcon = Icons.Outlined.Lock,
                        isPassword = true,
                        keyboardType = KeyboardType.Password,
                        enabled = !state.loading,
                        isError = state.error != null
                    )

                    state.error?.let {
                        Text(
                            text = it,
                            color = Destructive,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    PrimaryButton(
                        text = "Log in",
                        onClick = viewModel::login,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canSubmit,
                        loading = state.loading
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New to AuraCode?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinkButton(text = "Create account", onClick = onSignUpClick)
            }
        }
    }
}
