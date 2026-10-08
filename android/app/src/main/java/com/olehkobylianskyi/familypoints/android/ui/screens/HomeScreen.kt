package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.strings

@Composable
fun HomeScreen(
    language: AppLanguage,
    onLogout: () -> Unit
) {
    val text = strings(language)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "FamilyPoints",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = text.loginSuccessful,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
        )

        Button(onClick = onLogout) {
            Text(text.logout)
        }
    }
}
