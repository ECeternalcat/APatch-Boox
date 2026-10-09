package me.bmax.apatch

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests the fork's SuperKey resolution policy (APatchApp.resolveSuperKey):
 * stored custom key first, then the kernel's "su" alias when this UID is
 * already su-authorized, then nothing. The alias is what keeps the published
 * auth state coherent with what subsequent natives accept: nativeReady("")
 * substitutes "su" in the JNI layer, but every other native passes the empty
 * key through and the supercall rejects it with -EINVAL, so an empty resolved
 * key used to mean "UI shows installed while everything below fails".
 */
class ResolveSuperKeyPolicyTest {

    private fun ready(vararg readyKeys: String): (String) -> Boolean = { key -> key in readyKeys }

    @Test
    fun `kernel su alias is the literal string su`() {
        // Protocol contract: the kernel authorizes the literal key "su" by
        // caller UID; the resolution must adopt exactly this spelling.
        assertEquals("su", KERNEL_SU_ALIAS_KEY)
    }

    @Test
    fun `stored custom key that still authenticates wins over su alias`() {
        val resolved = resolveSuperKeyPolicy("abcdefgh1", ready("abcdefgh1", "su"))
        assertEquals("abcdefgh1", resolved)
    }

    @Test
    fun `stored custom key is preserved when it wins even though su also works`() {
        // The stored key must never be cleared or replaced merely because the
        // su alias would work too.
        val resolved = resolveSuperKeyPolicy("mycustom99k", ready("mycustom99k", "su"))
        assertEquals("mycustom99k", resolved)
    }

    @Test
    fun `no stored key and su authorized resolves to the su alias`() {
        val resolved = resolveSuperKeyPolicy(null, ready("su"))
        assertEquals(KERNEL_SU_ALIAS_KEY, resolved)
    }

    @Test
    fun `stale stored key with su authorized falls back to the su alias`() {
        // A different certificate does not make UID auth impossible: when the
        // kernel authorizes this UID directly, the alias keeps the app usable
        // and the stale stored key stays untouched for the mandatory-key
        // patching flow.
        val resolved = resolveSuperKeyPolicy("staleold1k", ready("su"))
        assertEquals(KERNEL_SU_ALIAS_KEY, resolved)
    }

    @Test
    fun `no stored key and su unauthorized resolves to empty so key entry is offered`() {
        val resolved = resolveSuperKeyPolicy(null, ready())
        assertEquals("", resolved)
    }

    @Test
    fun `stale stored key and su unauthorized resolves to empty`() {
        val resolved = resolveSuperKeyPolicy("staleold1k", ready())
        assertEquals("", resolved)
    }

    @Test
    fun `empty stored key is treated as absent`() {
        assertEquals("", resolveSuperKeyPolicy("", ready()))
        assertEquals(KERNEL_SU_ALIAS_KEY, resolveSuperKeyPolicy("", ready("su")))
    }

    @Test
    fun `arbitrary key is never accepted when nothing authenticates`() {
        // The policy must not accept any key the kernel does not verify.
        assertEquals("su", resolveSuperKeyPolicy("wrongkey9", ready("su")))
        assertEquals("", resolveSuperKeyPolicy("wrongkey9", ready("otherkey1")))
    }
}
