package me.bmax.apatch.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import me.bmax.apatch.R

internal data class EinkButtonStyle(val background: Color, val foreground: Color, val border: Color = Color.Black)

internal fun einkButtonStyle(enabled: Boolean): EinkButtonStyle =
    if (enabled) EinkButtonStyle(Color.White, Color.Black)
    else EinkButtonStyle(Color.Black, Color.White)

/** Paper-white actions; a solid black face and lock mark identify unavailable actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EinkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val available = einkButtonStyle(true)
    val unavailable = einkButtonStyle(false)
    val unavailableDescription = stringResource(R.string.eink_action_unavailable)
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    // A thicker outline replaces the animated gray ripple on paper displays.
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        Button(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = 48.dp).semantics {
                if (!enabled) stateDescription = unavailableDescription
            },
            enabled = enabled,
            shape = shape,
            border = BorderStroke(if (enabled && pressed) 2.dp else 1.dp, available.border),
            interactionSource = interactions,
            colors = ButtonDefaults.buttonColors(
                containerColor = available.background,
                contentColor = available.foreground,
                disabledContainerColor = unavailable.background,
                disabledContentColor = unavailable.foreground,
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
                disabledElevation = 0.dp,
            ),
            contentPadding = contentPadding,
        ) {
            if (!enabled) {
                Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
            }
            content()
        }
    }
}

/** Keep dialog actions compact without falling back to borderless/gray text buttons. */
@Composable
fun EinkTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    EinkButton(onClick = onClick, modifier = modifier, contentPadding = ButtonDefaults.TextButtonContentPadding, content = content)
}

@Composable
fun EinkFloatingActionButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    EinkButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minWidth = 56.dp, minHeight = 56.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
fun EinkExtendedFloatingActionButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    EinkButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        icon()
        Spacer(Modifier.width(12.dp))
        text()
    }
}
