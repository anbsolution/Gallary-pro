package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.MediaItem

@Composable
fun DeleteConfirmDialog(
    itemsToDelete: List<MediaItem>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (itemsToDelete.isEmpty()) return

    val totalSize = itemsToDelete.sumOf { it.size }
    val formattedSize = if (totalSize > 0) {
        val units = arrayOf("B", "KB", "MB", "GB")
        var s = totalSize.toDouble()
        var idx = 0
        while (s >= 1024.0 && idx < units.size - 1) {
            s /= 1024.0
            idx++
        }
        String.format(java.util.Locale.US, "%.1f %s", s, units[idx])
    } else "0 B"

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = if (itemsToDelete.size == 1) "Delete Media Item?" else "Delete ${itemsToDelete.size} Items?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = if (itemsToDelete.size == 1) {
                        "Are you sure you want to delete \"${itemsToDelete.first().displayName}\" ($formattedSize)?"
                    } else {
                        "Are you sure you want to delete ${itemsToDelete.size} selected items ($formattedSize)?"
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "On Android 11+, you may also be prompted by the system to approve this deletion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
