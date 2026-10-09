package me.bmax.apatch.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuperkeyHandoffTest {
    private val keyA = "abcdefgh1"
    private val keyB = "zzzzzzzz9"

    private class HandoffState {
        var viewModelKey: String = ""
        var displayed: String = ""
        private var edited = false

        val startReady: Boolean
            get() = viewModelKey.isNotEmpty() &&
                PatchesViewModel.checkSuperKeyValidation(viewModelKey)

        fun userEdit(text: String) {
            edited = true
            displayed = text
            viewModelKey = if (PatchesViewModel.checkSuperKeyValidation(text)) text else ""
        }

        fun parseArrives(parsed: String) {
            if (PatchesViewModel.parsedSuperkeyAdoptable(edited, parsed)) {
                viewModelKey = parsed
                displayed = parsed
            }
        }
    }

    @Test
    fun `valid parsed key is adopted when field is untouched`() {
        val state = HandoffState()
        state.parseArrives(keyA)
        assertEquals(keyA, state.viewModelKey)
        assertEquals(keyA, state.displayed)
        assertTrue(state.startReady)
    }

    @Test
    fun `any user edit including incomplete and cleared input wins over late parse`() {
        for (input in listOf(keyA, "partial", "")) {
            val state = HandoffState()
            state.userEdit(input)
            state.parseArrives(keyB)
            assertEquals(input, state.displayed)
            assertEquals(if (PatchesViewModel.checkSuperKeyValidation(input)) input else "", state.viewModelKey)
        }
    }

    @Test
    fun `invalid parsed key is not adopted`() {
        val state = HandoffState()
        state.parseArrives("short")
        assertEquals("", state.viewModelKey)
        assertFalse(state.startReady)
    }

    @Test
    fun `valid key may be edited after late parse`() {
        val state = HandoffState()
        state.parseArrives(keyB)
        state.userEdit(keyA)
        assertEquals(keyA, state.viewModelKey)
        assertEquals(keyA, state.displayed)
        assertTrue(state.startReady)
    }
}
