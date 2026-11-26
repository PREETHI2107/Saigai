package org.yazhi.saigai.model

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tests the shipped phrase catalog (app/src/main/res/raw/phrases.json) and the
 * parser behind it.
 *
 * These run on the JVM with no device: `./gradlew :app:testDebugUnitTest`.
 * They are the part of M1 that can be proved without hardware, so they cover the
 * contract the rest of the app relies on — unique ids, all four categories, Tamil
 * in `ta` and none in `en`.
 */
class PhraseCatalogTest {

    private val tamilRange = Regex("[\\u0B80-\\u0BFF]")

    @Test
    fun `shipped catalog parses into 20 unique phrases`() {
        val phrases = PhraseCatalog.parse(shippedCatalog())

        assertEquals("M1 scope is 20 phrases", 20, phrases.size)
        val ids = phrases.map { it.id }
        assertEquals("phrase ids must be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun `shipped catalog covers all four categories`() {
        val byCategory = PhraseCatalog.parse(shippedCatalog()).groupingBy { it.category }.eachCount()

        assertEquals(setOf(*PhraseCategory.entries.toTypedArray()), byCategory.keys)
        byCategory.forEach { (category, count) ->
            assertTrue("$category needs at least one card", count > 0)
        }
    }

    @Test
    fun `phrase ids are kebab-case`() {
        val kebab = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")

        PhraseCatalog.parse(shippedCatalog()).forEach { phrase ->
            assertTrue("'${phrase.id}' is not kebab-case", kebab.matches(phrase.id))
        }
    }

    @Test
    fun `tamil column holds Tamil script and english column does not`() {
        PhraseCatalog.parse(shippedCatalog()).forEach { phrase ->
            assertTrue(
                "'${phrase.id}' has no Tamil in its ta field",
                tamilRange.containsMatchIn(phrase.ta),
            )
            assertTrue(
                "'${phrase.id}' has Tamil in its en field",
                !tamilRange.containsMatchIn(phrase.en),
            )
        }
    }

    @Test
    fun `unknown category fails loudly instead of guessing`() {
        try {
            PhraseCategory.fromKey("pharmacy")
            fail("expected an unknown category to throw")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("pharmacy"))
        }
    }

    @Test
    fun `catalog version mismatch is rejected`() {
        val wrongVersion = """
            {"version": 99, "phrases": []}
        """.trimIndent()

        try {
            PhraseCatalog.parse(wrongVersion)
            fail("expected a version mismatch to throw")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("99"))
        }
    }

    @Test
    fun `byId returns null for an unknown id`() {
        val phrases = PhraseCatalog.parse(shippedCatalog())

        assertEquals(null, PhraseCatalog.byId(phrases, "not-a-phrase"))
        assertEquals(null, PhraseCatalog.byId(phrases, null))
        assertEquals("thank-you", PhraseCatalog.byId(phrases, "thank-you")?.id)
    }

    /** Unit tests run with the `app` module as working directory. */
    private fun shippedCatalog(): String {
        val file = File("src/main/res/raw/phrases.json")
        assertTrue("catalog not found at ${file.absolutePath}", file.isFile)
        return file.readText()
    }
}
