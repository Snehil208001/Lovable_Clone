package com.snehil.auracode.mainui.auth.signup

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
import androidx.compose.material.icons.outlined.Person
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
fun SignupScreen(
    onSignedUp: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.signedUp.collect { onSignedUp() }
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
                text = "Create your account",
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Start building apps by describing them",
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
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = "Name",
                        leadingIcon = Icons.Outlined.Person,
                        enabled = !state.loading,
                        isError = state.error != null
                    )
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
                        text = "Create account",
                        onClick = viewModel::signup,
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
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinkButton(text = "Log in", onClick = onLoginClick)
            }
        }
    }
}
