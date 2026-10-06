package com.qoody.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.qoody.app.R
import com.qoody.app.ui.navigation.TopLevelDestination
import com.qoody.app.ui.theme.QoodyTheme

/** The brand lock-up: logo mark and lowercase wordmark. */
@Composable
private fun BrandMark() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = stringResource(R.string.cd_app_logo),
            modifier = Modifier.size(QoodyTheme.sizes.logo),
        )
        Text(
            text = stringResource(R.string.brand_wordmark),
            style = QoodyTheme.typography.wordmark,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Top bar of the three main tabs. */
@Composable
fun QoodyTopBar(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(
                    QoodyTheme.sizes.topBarHeight,
                ).padding(horizontal = QoodyTheme.spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark()
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSearchClick, modifier = Modifier.size(QoodyTheme.sizes.touchTarget)) {
                QoodyIcon(
                    id = R.drawable.ic_search,
                    contentDescription = stringResource(R.string.cd_search),
                    tint = MaterialTheme.colorScheme.secondary,
                    size = QoodyTheme.sizes.iconLg,
                )
            }
        }
    }
}

/** Top bar of pushed screens: back arrow, logo and screen title. */
@Composable
fun QoodyDetailTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(
                    QoodyTheme.sizes.topBarHeight,
                ).padding(horizontal = QoodyTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm),
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(QoodyTheme.sizes.touchTarget)) {
                QoodyIcon(
                    id = R.drawable.ic_arrow_back,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    size = QoodyTheme.sizes.iconLg,
                )
            }
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(QoodyTheme.sizes.logo),
            )
            Text(text = title, style = QoodyTheme.typography.wordmark, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Bottom navigation for the three main tabs. */
@Composable
fun QoodyBottomBar(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        HairlineDivider()
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 0.dp,
        ) {
            TopLevelDestination.entries.forEach { destination ->
                val isSelected = destination == selected
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelect(destination) },
                    icon = {
                        QoodyIcon(
                            id = if (isSelected) destination.selectedIcon else destination.icon,
                            contentDescription = null,
                            size = QoodyTheme.sizes.iconLg,
                        )
                    },
                    label = { Text(text = stringResource(destination.label), style = QoodyTheme.typography.bodySm) },
                    colors =
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = MaterialTheme.colorScheme.secondary,
                            indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                )
            }
        }
    }
}
