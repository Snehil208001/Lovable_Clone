package com.snehil.auracode.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.snehil.auracode.ui.theme.BorderColor
import com.snehil.auracode.ui.theme.EmeraldDeep
import com.snehil.auracode.ui.theme.InputBackground
import com.snehil.auracode.ui.theme.Primary
import com.snehil.auracode.ui.theme.PrimaryForeground

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled && !loading) {
                    Brush.horizontalGradient(listOf(Primary, EmeraldDeep))
                } else {
                    Brush.horizontalGradient(
                        listOf(Primary.copy(alpha = 0.4f), Primary.copy(alpha = 0.4f))
                    )
                }
            )
    ) {
        Button(
            onClick = onClick,
            enabled = enabled && !loading,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = PrimaryForeground,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = PrimaryForeground.copy(alpha = 0.6f)
            )
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = PrimaryForeground,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = text,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun AuraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = isError,
        leadingIcon = leadingIcon?.let {
            { Icon(imageVector = it, contentDescription = null) }
        },
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = InputBackground,
            unfocusedContainerColor = InputBackground,
            disabledContainerColor = InputBackground,
            focusedBorderColor = Primary,
            unfocusedBorderColor = BorderColor,
            cursorColor = Primary,
            focusedLeadingIconColor = Primary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLabelColor = Primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
fun LinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text = text, color = Primary, style = MaterialTheme.typography.labelLarge)
    }
}
