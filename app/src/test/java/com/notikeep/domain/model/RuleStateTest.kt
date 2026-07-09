package com.notikeep.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The two-toggle UI (save? / notify?) must map losslessly onto the tri-state rule. */
class RuleStateTest {

    @Test
    fun `save on notify on is shade and archive`() {
        assertEquals(RuleState.SHADE_AND_ARCHIVE, RuleState.from(save = true, notify = true))
    }

    @Test
    fun `save on notify off is archive only`() {
        assertEquals(RuleState.ARCHIVE_ONLY, RuleState.from(save = true, notify = false))
    }

    @Test
    fun `save off is ignore regardless of notify`() {
        assertEquals(RuleState.IGNORE, RuleState.from(save = false, notify = true))
        assertEquals(RuleState.IGNORE, RuleState.from(save = false, notify = false))
    }

    @Test
    fun `toggle projections round-trip for saving states`() {
        for (state in listOf(RuleState.SHADE_AND_ARCHIVE, RuleState.ARCHIVE_ONLY)) {
            assertEquals(state, RuleState.from(save = state.saves, notify = state.notifies))
        }
    }

    @Test
    fun `ignore neither saves nor silences`() {
        assertFalse(RuleState.IGNORE.saves)
        assertTrue(RuleState.IGNORE.notifies)
    }
}
