package com.qoody.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.qoody.app.ui.theme.QoodyTheme

/** The app's modal bottom sheet: white paper with a grabber, sliding over the ledger. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QoodyModalSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = QoodyTheme.shapes.sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        scrimColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = QoodyTheme.alphas.scrim),
        dragHandle = { SheetHandle(Modifier.padding(vertical = QoodyTheme.spacing.cozy)) },
    ) {
        Column(
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = QoodyTheme.spacing.lg)
                    .padding(bottom = QoodyTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.md),
            horizontalAlignment = Alignment.Start,
            content = content,
        )
    }
}
