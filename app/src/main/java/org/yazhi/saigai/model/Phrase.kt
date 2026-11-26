package org.yazhi.saigai.model

import android.content.Context
import org.json.JSONObject
import org.yazhi.saigai.R

/**
 * Quick Cards categories. Design doc §4: Hospital, Bank, Emergency, General.
 * The `key` matches the `category` field in res/raw/phrases.json and is validated
 * by scripts/check_m1.py.
 */
enum class PhraseCategory(val key: String) {
    HOSPITAL("hospital"),
    BANK("bank"),
    EMERGENCY("emergency"),
    GENERAL("general");

    companion object {
        fun fromKey(key: String): PhraseCategory =
            entries.firstOrNull { it.key == key }
                ?: throw IllegalArgumentException("Unknown phrase category: $key")
    }
}

/**
 * One Quick Card. Tamil and English wording both live in the catalog so a single
 * card can speak either or both; UI chrome strings live in res/values(-ta).
 */
data class Phrase(
    val id: String,
    val category: PhraseCategory,
    val ta: String,
    val en: String,
)

object PhraseCatalog {

    private const val CATALOG_VERSION = 1

    fun load(context: Context): List<Phrase> {
        val json = context.resources.openRawResource(R.raw.phrases).use { stream ->
            stream.bufferedReader().readText()
        }
        val root = JSONObject(json)
        val declaredVersion = root.optInt("version", -1)
        require(declaredVersion == CATALOG_VERSION) {
            "phrases.json version $declaredVersion does not match catalog version $CATALOG_VERSION"
        }
        val array = root.getJSONArray("phrases")
        return (0 until array.length()).map { index ->
            val entry = array.getJSONObject(index)
            Phrase(
                id = entry.getString("id"),
                category = PhraseCategory.fromKey(entry.getString("category")),
                ta = entry.getString("ta"),
                en = entry.getString("en"),
            )
        }
    }

    fun byCategory(phrases: List<Phrase>, category: PhraseCategory): List<Phrase> =
        phrases.filter { it.category == category }

    fun byId(phrases: List<Phrase>, id: String?): Phrase? =
        id?.let { wanted -> phrases.firstOrNull { it.id == wanted } }
}
