package com.rolechat.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast

@Composable
fun SetupStep(number: Int, title: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$number. $title", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun OfficialApiLink(title: String, url: String) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    Column {
        TextButton(onClick = {
            runCatching { uriHandler.openUri(url) }.onFailure {
                Toast.makeText(context, "无法打开浏览器，请长按下方网址复制。", Toast.LENGTH_LONG).show()
            }
        }) { Text("$title ↗") }
        SelectionContainer {
            Text(url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth())
        }
    }
}
