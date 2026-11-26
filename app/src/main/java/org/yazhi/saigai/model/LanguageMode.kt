package org.yazhi.saigai.model

/** Design doc §4: single-tap header switch for தமிழ் / English / Both. */
enum class LanguageMode {
    TAMIL,
    ENGLISH,
    BOTH;

    fun showsTamil(): Boolean = this != ENGLISH

    fun showsEnglish(): Boolean = this != TAMIL
}
