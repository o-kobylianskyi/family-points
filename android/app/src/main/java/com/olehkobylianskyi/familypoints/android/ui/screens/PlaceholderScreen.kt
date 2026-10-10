package com.olehkobylianskyi.familypoints.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olehkobylianskyi.familypoints.android.i18n.AppLanguage
import com.olehkobylianskyi.familypoints.android.i18n.dashboardStrings

@Composable
fun PlaceholderScreen(
    language: AppLanguage,
    title: String,
    onBack: () -> Unit
) {
    val text = dashboardStrings(language)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title)
        Text(
            text = text.notConvertedYet,
            modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
        )
        Button(onClick = onBack) {
            Text("←")
        }
    }
}
