package org.yazhi.saigai.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.yazhi.saigai.R
import org.yazhi.saigai.model.LanguageMode
import org.yazhi.saigai.model.Phrase
import org.yazhi.saigai.model.PhraseCatalog
import org.yazhi.saigai.model.PhraseCategory

/**
 * Quick Cards screen (design doc §4): categorised, high-contrast cards that speak
 * immediately on tap. This is also the documented rollback path (§8) when camera
 * recognition degrades in field lighting, so it must never depend on the camera
 * or on the network.
 */
@Composable
fun QuickCardsScreen(
    phrases: List<Phrase>,
    languageMode: LanguageMode,
    onSpeak: (Phrase) -> Unit,
) {
    var category by rememberSaveable { mutableStateOf(PhraseCategory.HOSPITAL) }
    val visible = PhraseCatalog.byCategory(phrases, category)

    Column(modifier = Modifier.fillMaxSize()) {
        CategoryRow(
            selected = category,
            onSelect = { category = it },
        )
        Text(
            text = stringResource(R.string.tap_card_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(visible, key = { it.id }) { phrase ->
                PhraseCard(
                    phrase = phrase,
                    languageMode = languageMode,
                    onClick = { onSpeak(phrase) },
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    selected: PhraseCategory,
    onSelect: (PhraseCategory) -> Unit,
) {
    val label = stringResource(R.string.categories_label)
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PhraseCategory.entries.forEach { category ->
                FilterChip(
                    selected = category == selected,
                    onClick = { onSelect(category) },
                    label = {
                        Text(
                            text = stringResource(category.labelRes()),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhraseCard(
    phrase: Phrase,
    languageMode: LanguageMode,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val description = stringResource(R.string.card_speaks, phraseLabel(phrase, languageMode))
    Card(
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            // min, not fixed: a three-line Tamil phrase plus its English line needs
            // more than 132dp at 20sp, and a fixed height clipped the English text.
            .heightIn(min = 132.dp)
            .clip(shape)
            .border(2.dp, MaterialTheme.colorScheme.primary, shape)
            .semantics { contentDescription = description },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (languageMode.showsTamil()) {
                Text(
                    text = phrase.ta,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (languageMode.showsEnglish()) {
                Text(
                    text = phrase.en,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/** Both scripts, always — used for the spoken accessibility description. */
private fun phraseLabel(phrase: Phrase, languageMode: LanguageMode): String = when (languageMode) {
    LanguageMode.TAMIL -> phrase.ta
    LanguageMode.ENGLISH -> phrase.en
    LanguageMode.BOTH -> "${phrase.ta} · ${phrase.en}"
}

private fun PhraseCategory.labelRes(): Int = when (this) {
    PhraseCategory.HOSPITAL -> R.string.category_hospital
    PhraseCategory.BANK -> R.string.category_bank
    PhraseCategory.EMERGENCY -> R.string.category_emergency
    PhraseCategory.GENERAL -> R.string.category_general
}
