package org.yazhi.saigai.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The language toggle is the one control on every screen (design doc §4), so its
 * visibility rules are pinned down here rather than discovered on a demo device.
 */
class LanguageModeTest {

    @Test
    fun `TAMIL shows Tamil only`() {
        assertEquals(true, LanguageMode.TAMIL.showsTamil())
        assertEquals(false, LanguageMode.TAMIL.showsEnglish())
    }

    @Test
    fun `ENGLISH shows English only`() {
        assertEquals(false, LanguageMode.ENGLISH.showsTamil())
        assertEquals(true, LanguageMode.ENGLISH.showsEnglish())
    }

    @Test
    fun `BOTH shows both`() {
        assertEquals(true, LanguageMode.BOTH.showsTamil())
        assertEquals(true, LanguageMode.BOTH.showsEnglish())
    }

    @Test
    fun `every mode shows at least one language`() {
        LanguageMode.entries.forEach { mode ->
            assertEquals("$mode would render nothing", true, mode.showsTamil() || mode.showsEnglish())
        }
    }
}
