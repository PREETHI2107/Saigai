package org.yazhi.saigai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.yazhi.saigai.R
import org.yazhi.saigai.model.LanguageMode
import org.yazhi.saigai.model.Phrase
import org.yazhi.saigai.model.PhraseCatalog
import org.yazhi.saigai.tts.PhraseSpeaker

private enum class AppTab { LIVE, CARDS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaigaiApp() {
    val context = LocalContext.current
    val phrases = remember { PhraseCatalog.load(context) }
    val speaker = remember { PhraseSpeaker(context) }

    var languageMode by rememberSaveable { mutableStateOf(LanguageMode.BOTH) }
    var tab by rememberSaveable { mutableStateOf(AppTab.LIVE) }
    var spokenId by rememberSaveable { mutableStateOf<String?>(null) }

    DisposableEffect(speaker) {
        speaker.start()
        onDispose { speaker.stop() }
    }

    val spoken = PhraseCatalog.byId(phrases, spokenId)
    val speak: (Phrase) -> Unit = { phrase ->
        spokenId = phrase.id
        speaker.speak(phrase, languageMode)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    },
                )
                LanguageToggle(
                    selected = languageMode,
                    onSelect = { languageMode = it },
                )
                TabRow(selectedTabIndex = tab.ordinal) {
                    Tab(
                        selected = tab == AppTab.LIVE,
                        onClick = { tab = AppTab.LIVE },
                        text = { Text(stringResource(R.string.tab_live)) },
                    )
                    Tab(
                        selected = tab == AppTab.CARDS,
                        onClick = { tab = AppTab.CARDS },
                        text = { Text(stringResource(R.string.tab_cards)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (tab) {
                AppTab.LIVE -> LiveBridgeScreen(
                    caption = spoken,
                    languageMode = languageMode,
                    speakerStatus = speaker.status,
                    onReplay = { spoken?.let { speak(it) } },
                )
                AppTab.CARDS -> QuickCardsScreen(
                    phrases = phrases,
                    languageMode = languageMode,
                    onSpeak = speak,
                )
            }
        }
    }
}

/**
 * Single-tap header switch for தமிழ் / English / Both (design doc §4).
 * Chips instead of a segmented control so the labels wrap safely with the
 * 30–60% Tamil expansion described in the tamil-localization skill.
 */
@Composable
private fun LanguageToggle(
    selected: LanguageMode,
    onSelect: (LanguageMode) -> Unit,
) {
    val toggleLabel = stringResource(R.string.language_toggle_label)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics {
                contentDescription = toggleLabel
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LanguageChip(
            label = stringResource(R.string.lang_tamil),
            selected = selected == LanguageMode.TAMIL,
            onClick = { onSelect(LanguageMode.TAMIL) },
        )
        LanguageChip(
            label = stringResource(R.string.lang_english),
            selected = selected == LanguageMode.ENGLISH,
            onClick = { onSelect(LanguageMode.ENGLISH) },
        )
        LanguageChip(
            label = stringResource(R.string.lang_both),
            selected = selected == LanguageMode.BOTH,
            onClick = { onSelect(LanguageMode.BOTH) },
        )
    }
}

@Composable
private fun LanguageChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
        },
    )
}
