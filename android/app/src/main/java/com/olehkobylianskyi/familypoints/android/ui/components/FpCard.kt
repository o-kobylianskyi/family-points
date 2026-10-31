package com.olehkobylianskyi.familypoints.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Soft Family outlined/elevated card shared by screens.
 *
 * Content is real Compose UI; never rasterized screenshots. Accepts Material3
 * Card-compatible named arguments, so existing screen behavior stays intact.
 */
@Composable
fun FpCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    colors: CardColors = fpCardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    border: BorderStroke? = fpCardBorder(),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        content = content
    )
}

@Composable
private fun fpCardColors(): CardColors {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.red < 0.25f
    return CardDefaults.cardColors(
        containerColor = if (dark) Color(0xFF1C2C3D) else Color(0xFFF3FAFF),
        contentColor = scheme.onSurface
    )
}

@Composable
private fun fpCardBorder(): BorderStroke {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.red < 0.25f
    return BorderStroke(
        width = 1.dp,
        color = if (dark) Color(0xFF5E9BD3) else Color(0xFF8DCCF5)
    )
}
