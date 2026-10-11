package me.bmax.apatch.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.bmax.apatch.R

enum class NoticeKind { WARNING, INFO }

/** Meaning is carried by a heading and an icon, never by a gray/color fill. */
@Composable
fun WarningCard(
    message: String,
    kind: NoticeKind = NoticeKind.WARNING,
    onClick: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    OutlinedCard(
        modifier = onClick?.let {
            Modifier.clickable(interactionSource = interactions, indication = null, role = Role.Button, onClick = it)
        } ?: Modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (pressed) 2.dp else 1.dp, Color.Black),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White, contentColor = Color.Black),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) icon() else Icon(
                imageVector = if (kind == NoticeKind.WARNING) Icons.Outlined.Warning else Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(if (kind == NoticeKind.WARNING) R.string.eink_notice_warning else R.string.eink_notice_info),
                    fontWeight = FontWeight.Bold,
                )
                Text(message)
            }
            if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(android.R.string.cancel))
                }
            }
        }
    }
}

@Preview
@Composable
private fun WarningCardPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WarningCard(message = "Warning message")
        WarningCard(message = "Dismissible warning", onClose = {})
        WarningCard(message = "Information", kind = NoticeKind.INFO, onClick = {})
    }
}
