package me.bmax.apatch.ui.theme

import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class EinkThemeTest {
    @Test
    fun `all paper containers are white including Material 3 surface roles`() {
        val scheme = EinkLightScheme
        listOf(
            scheme.background, scheme.surface, scheme.surfaceVariant,
            scheme.primaryContainer, scheme.secondaryContainer, scheme.tertiaryContainer,
            scheme.errorContainer, scheme.surfaceBright, scheme.surfaceDim,
            scheme.surfaceContainer, scheme.surfaceContainerLow, scheme.surfaceContainerLowest,
            scheme.surfaceContainerHigh, scheme.surfaceContainerHighest,
        ).forEach { assertEquals(Color.White, it) }
    }

    @Test
    fun `paper text and outlines are black rather than gray`() {
        val scheme = EinkLightScheme
        listOf(
            scheme.onBackground, scheme.onSurface, scheme.onSurfaceVariant,
            scheme.onPrimaryContainer, scheme.onSecondaryContainer, scheme.onTertiaryContainer,
            scheme.onErrorContainer, scheme.outline, scheme.outlineVariant,
        ).forEach { assertEquals(Color.Black, it) }
    }

    @Test
    fun `elevation cannot add a gray tonal overlay to banners or dialogs`() {
        val scheme = EinkLightScheme
        assertEquals(Color.White, scheme.surfaceTint)
        for (elevation in listOf(0, 1, 3, 6, 12)) {
            assertEquals(Color.White, scheme.surfaceColorAtElevation(elevation.dp))
        }
    }
}
