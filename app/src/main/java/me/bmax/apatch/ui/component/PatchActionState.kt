package me.bmax.apatch.ui.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/** UI-only one-shot submission per patch page; backend readiness checks remain authoritative. */
class PatchActionState : ViewModel() {
    var submitted by mutableStateOf(false)
        private set

    @Synchronized fun claimSubmission(): Boolean {
        if (submitted) return false
        submitted = true
        return true
    }
}
