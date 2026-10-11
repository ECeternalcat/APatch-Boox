package me.bmax.apatch.ui.component

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class EinkButtonStyleTest {
    @Test
    fun `available action has white face black text and black border`() {
        val style = einkButtonStyle(true)
        assertEquals(Color.White, style.background)
        assertEquals(Color.Black, style.foreground)
        assertEquals(Color.Black, style.border)
    }

    @Test
    fun `unavailable action has opaque black face and white text`() {
        val style = einkButtonStyle(false)
        assertEquals(Color.Black, style.background)
        assertEquals(Color.White, style.foreground)
        assertEquals(1f, style.background.alpha, 0f)
        assertEquals(1f, style.foreground.alpha, 0f)
    }

    @Test
    fun `enabling and disabling invert contrast without changing the border`() {
        val available = einkButtonStyle(true)
        val unavailable = einkButtonStyle(false)
        assertEquals(available.background, unavailable.foreground)
        assertEquals(available.foreground, unavailable.background)
        assertEquals(available.border, unavailable.border)
    }
}
