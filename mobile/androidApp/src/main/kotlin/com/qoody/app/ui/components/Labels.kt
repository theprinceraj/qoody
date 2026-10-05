package com.qoody.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import com.qoody.app.ui.theme.QoodyTheme

/** Uppercases using the language the user currently has selected (so Turkish "i" and the like are right). */
@Composable
fun String.uppercaseForLocale(): String = uppercase(Locale.current.platformLocale)

/** Tracked, uppercase micro-heading, like a column head in a printed journal. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
) {
    Text(
        text = text.uppercaseForLocale(),
        style = QoodyTheme.typography.labelCaps,
        color = color,
        modifier = modifier,
    )
}

/** Centred explanatory message for a screen or card with nothing to show. */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = QoodyTheme.spacing.xl, horizontal = QoodyTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs),
    ) {
        Text(
            text = title,
            style = QoodyTheme.typography.headlineSm,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (body != null) {
            Text(
                text = body,
                style = QoodyTheme.typography.bodyMd,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primaryContainer)
    }
}
