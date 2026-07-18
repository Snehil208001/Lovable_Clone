package com.snehil.auracode.mainui.splashscreen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snehil.auracode.mainui.splashscreen.viewmodel.SplashDestination
import com.snehil.auracode.mainui.splashscreen.viewmodel.SplashScreenViewModel
import com.snehil.auracode.ui.components.AuraBackground
import com.snehil.auracode.ui.components.BrandBadge
import com.snehil.auracode.ui.theme.Primary

@Composable
fun SplashScreen(
    onAuthenticated: () -> Unit,
    onUnauthenticated: () -> Unit,
    viewModel: SplashScreenViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        when (destination) {
            SplashDestination.AUTHENTICATED -> onAuthenticated()
            SplashDestination.UNAUTHENTICATED -> onUnauthenticated()
            null -> Unit
        }
    }

    AuraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrandBadge(size = 72)
            Text(
                text = "AuraCode",
                modifier = Modifier.padding(top = 20.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Build apps by describing them",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(top = 40.dp)
                    .size(28.dp),
                color = Primary,
                strokeWidth = 2.dp
            )
        }
    }
}
