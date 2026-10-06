package com.qoody.app.ui.insights

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qoody.app.R
import com.qoody.app.ui.components.BadgePill
import com.qoody.app.ui.components.Dot
import com.qoody.app.ui.components.EmptyState
import com.qoody.app.ui.components.HairlineDivider
import com.qoody.app.ui.components.LoadingIndicator
import com.qoody.app.ui.components.QoodyCard
import com.qoody.app.ui.components.QoodyIcon
import com.qoody.app.ui.components.QoodyInset
import com.qoody.app.ui.components.QoodyTopBar
import com.qoody.app.ui.components.ScreenContainer
import com.qoody.app.ui.components.SectionLabel
import com.qoody.app.ui.components.SegmentedControl
import com.qoody.app.ui.components.uppercaseForLocale
import com.qoody.app.ui.format.DateFormats
import com.qoody.app.ui.format.StyledArg
import com.qoody.app.ui.format.rememberDateFormats
import com.qoody.app.ui.format.styledStringResource
import com.qoody.app.ui.theme.QoodyTheme
import com.qoody.app.ui.theme.categoryName
import com.qoody.shared.domain.format.MoneyFormatter
import com.qoody.shared.domain.format.PercentFormatter
import com.qoody.shared.feature.insights.BucketKind
import com.qoody.shared.feature.insights.CategorySpend
import com.qoody.shared.feature.insights.InsightsPeriod
import com.qoody.shared.feature.insights.InsightsUiState
import com.qoody.shared.feature.insights.InsightsViewModel
import com.qoody.shared.feature.insights.Reflection
import com.qoody.shared.feature.insights.SpendBucket
import com.qoody.shared.feature.ledger.TrendDirection
import org.koin.androidx.compose.koinViewModel

/** The insights tab: pace over the period, where the money went, and a quiet reflection. */
@Composable
fun InsightsScreen(
    onSearchClick: () -> Unit,
    viewModel: InsightsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsContent(
        state = state,
        onPeriodSelected = viewModel::onPeriodSelected,
        onSearchClick = onSearchClick,
    )
}

@Composable
fun InsightsContent(
    state: InsightsUiState,
    onPeriodSelected: (InsightsPeriod) -> Unit,
    onSearchClick: () -> Unit,
) {
    ScreenContainer {
        Column(modifier = Modifier.fillMaxSize()) {
            QoodyTopBar(onSearchClick = onSearchClick)
            when (state) {
                InsightsUiState.Loading -> LoadingIndicator()
                is InsightsUiState.Content -> InsightsBody(state, onPeriodSelected)
            }
        }
    }
}

@Composable
private fun InsightsBody(
    state: InsightsUiState.Content,
    onPeriodSelected: (InsightsPeriod) -> Unit,
) {
    val formats = rememberDateFormats()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = QoodyTheme.spacing.md,
                end = QoodyTheme.spacing.md,
                bottom = QoodyTheme.spacing.lg,
            ),
        verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.md),
    ) {
        item { Header(state, formats, onPeriodSelected) }
        if (state.categories.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.insights_empty_title),
                    body = stringResource(R.string.insights_empty_body),
                )
            }
        } else {
            item { PaceCard(state, formats) }
            item { CategoryBreakdownCard(state.categories) }
            state.reflection?.let { reflection -> item { ReflectionCard(reflection) } }
            item { VerifiedFooter() }
        }
    }
}

@Composable
private fun Header(
    state: InsightsUiState.Content,
    formats: DateFormats,
    onPeriodSelected: (InsightsPeriod) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.insights_title),
                    style = QoodyTheme.typography.headlineLgMobile,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.insights_subtitle),
                    style = QoodyTheme.typography.bodySm,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            IconButton(
                onClick = { onPeriodSelected(InsightsPeriod.ThisMonth) },
                modifier = Modifier.size(QoodyTheme.sizes.touchTarget),
            ) {
                Surface(
                    modifier = Modifier.size(QoodyTheme.sizes.circleIconButton),
                    shape = QoodyTheme.shapes.pill,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        QoodyIcon(
                            id = R.drawable.ic_calendar_today,
                            contentDescription = stringResource(R.string.cd_jump_to_this_month),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        SegmentedControl(
            options = InsightsPeriod.entries,
            selected = state.period,
            onSelect = onPeriodSelected,
            label = { period ->
                when (period) {
                    InsightsPeriod.ThisMonth -> {
                        stringResource(
                            R.string.insights_period_this_month,
                            formats.monthAbbreviation(state.currentMonth),
                        )
                    }

                    InsightsPeriod.LastMonth -> {
                        stringResource(R.string.insights_period_last_month)
                    }

                    InsightsPeriod.AllTime -> {
                        stringResource(R.string.insights_period_all_time)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PaceCard(
    state: InsightsUiState.Content,
    formats: DateFormats,
) {
    val isWeekly = state.bucketKind == BucketKind.Week
    QoodyCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = QoodyTheme.spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs),
            ) {
                Dot(color = MaterialTheme.colorScheme.primaryContainer, size = QoodyTheme.sizes.dotSm)
                SectionLabel(
                    stringResource(if (isWeekly) R.string.insights_weekly_pace else R.string.insights_monthly_pace),
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs),
            ) {
                Text(
                    text = MoneyFormatter.format(state.total),
                    style = QoodyTheme.typography.numericMd,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.insights_total_suffix),
                    style = QoodyTheme.typography.bodySm,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        HairlineDivider()
        PaceChart(
            buckets = state.buckets,
            kind = state.bucketKind,
            formats = formats,
            modifier = Modifier.padding(vertical = QoodyTheme.spacing.md),
        )
        HairlineDivider()
        PaceFooter(state, isWeekly)
    }
}

@Composable
private fun PaceFooter(
    state: InsightsUiState.Content,
    isWeekly: Boolean,
) {
    val current = state.currentBucketNumber ?: return
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = QoodyTheme.spacing.cozy),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dot(color = MaterialTheme.colorScheme.primaryContainer, size = QoodyTheme.sizes.dotXs)
            Text(
                text =
                    stringResource(
                        if (isWeekly) R.string.insights_current_week_marker else R.string.insights_current_month_marker,
                    ),
                style = QoodyTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        SectionLabel(
            text =
                pluralStringResource(
                    if (isWeekly) R.plurals.insights_weeks_progress else R.plurals.insights_months_progress,
                    state.buckets.size,
                    current,
                    state.buckets.size,
                ),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(start = QoodyTheme.spacing.sm),
        )
    }
}

@Composable
private fun PaceChart(
    buckets: List<SpendBucket>,
    kind: BucketKind,
    formats: DateFormats,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.cozy),
        verticalAlignment = Alignment.Bottom,
    ) {
        buckets.forEachIndexed { index, bucket ->
            BucketColumn(
                bucket = bucket,
                title = bucketTitle(kind, index, bucket, formats),
                subtitle = bucketSubtitle(kind, bucket, formats),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun bucketTitle(
    kind: BucketKind,
    index: Int,
    bucket: SpendBucket,
    formats: DateFormats,
): String =
    when (kind) {
        BucketKind.Week -> stringResource(R.string.insights_week_label, index + 1)
        BucketKind.Month -> formats.monthAbbreviation(bucket.start.month)
    }

@Composable
private fun bucketSubtitle(
    kind: BucketKind,
    bucket: SpendBucket,
    formats: DateFormats,
): String? =
    when (kind) {
        BucketKind.Week -> {
            stringResource(
                R.string.insights_range,
                formats.monthDay(bucket.start),
                formats.dayOfMonth(bucket.end),
            )
        }

        BucketKind.Month -> {
            null
        }
    }

@Composable
private fun BucketColumn(
    bucket: SpendBucket,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primaryContainer
    val barColor = if (bucket.isCurrent) accent else MaterialTheme.colorScheme.onSurface
    val barAreaHeight = QoodyTheme.sizes.chartHeight - QoodyTheme.sizes.chartLabelReserve
    val barHeight by animateDpAsState(
        targetValue = barAreaHeight * bucket.height.fraction,
        animationSpec = tween(QoodyTheme.motion.chartMillis),
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.height(QoodyTheme.sizes.chartBadgeHeight).wrapContentWidth(unbounded = true),
            contentAlignment = Alignment.Center,
        ) {
            if (bucket.isCurrent) CurrentBadge()
        }
        Column(
            modifier = Modifier.height(QoodyTheme.sizes.chartHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = MoneyFormatter.formatWhole(bucket.amount),
                style = QoodyTheme.typography.numericSm,
                color = if (bucket.isCurrent) accent else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = QoodyTheme.spacing.sm),
            )
            Box(
                modifier =
                    Modifier
                        .widthIn(max = QoodyTheme.sizes.chartBarMaxWidth)
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(QoodyTheme.shapes.barTop)
                        .background(barColor),
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(QoodyTheme.sizes.chartBaseline)
                    .background(
                        if (bucket.isCurrent) {
                            accent.copy(
                                alpha = QoodyTheme.alphas.scrim,
                            )
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                    ),
        )
        Text(
            text = title,
            style = if (bucket.isCurrent) QoodyTheme.typography.bodySmMedium else QoodyTheme.typography.bodySm,
            color = if (bucket.isCurrent) accent else MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = QoodyTheme.spacing.sm),
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = QoodyTheme.typography.bodySm,
                color = if (bucket.isCurrent) accent else MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
                softWrap = false,
                modifier = Modifier.wrapContentWidth(unbounded = true),
            )
        }
    }
}

@Composable
private fun CurrentBadge() {
    Surface(
        shape = QoodyTheme.shapes.pill,
        color = QoodyTheme.colors.accentSoft,
        contentColor = QoodyTheme.colors.onAccentSoft,
    ) {
        Text(
            text = stringResource(R.string.insights_current_badge).uppercaseForLocale(),
            style = QoodyTheme.typography.labelCaps,
            softWrap = false,
            modifier = Modifier.padding(horizontal = QoodyTheme.spacing.sm, vertical = QoodyTheme.spacing.xxs),
        )
    }
}

@Composable
private fun CategoryBreakdownCard(categories: List<CategorySpend>) {
    QoodyCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.insights_category_breakdown),
                    style = QoodyTheme.typography.headlineSm,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.insights_category_subtitle),
                    style = QoodyTheme.typography.bodySm,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            BadgePill(
                text = pluralStringResource(R.plurals.insights_category_count, categories.size, categories.size),
                modifier = Modifier.padding(start = QoodyTheme.spacing.sm),
            )
        }
        Column(
            modifier = Modifier.padding(top = QoodyTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.md),
        ) {
            categories.forEach { CategoryRow(it) }
        }
    }
}

@Composable
private fun CategoryRow(spend: CategorySpend) {
    val color = QoodyTheme.colors.forCategory(spend.category).dot
    val fill by animateFloatAsState(spend.share.fraction, tween(QoodyTheme.motion.chartMillis))
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = QoodyTheme.spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm),
            ) {
                Dot(color = color, size = QoodyTheme.sizes.dot)
                Text(
                    text = categoryName(spend.category),
                    style = QoodyTheme.typography.bodyMdMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = PercentFormatter.formatWhole(spend.share),
                    style = QoodyTheme.typography.bodySm,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Text(
                text = MoneyFormatter.format(spend.amount),
                style = QoodyTheme.typography.numericMd,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        ShareBar(fraction = fill, color = color)
    }
}

@Composable
private fun ShareBar(
    fraction: Float,
    color: Color,
    height: Dp = QoodyTheme.sizes.categoryTrack,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height)
                .clip(QoodyTheme.shapes.pill)
                .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(fraction)
                    .height(height)
                    .clip(QoodyTheme.shapes.pill)
                    .background(color),
        )
    }
}

@Composable
private fun ReflectionCard(reflection: Reflection) {
    val average = MoneyFormatter.format(reflection.averageDaily)
    val accent = SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primaryContainer)
    val comparison = reflection.comparison
    val sentence =
        when (comparison?.direction) {
            TrendDirection.Lower -> {
                styledStringResource(
                    R.string.insights_reflection_lower,
                    StyledArg(average, SpanStyle(fontWeight = FontWeight.Medium)),
                    StyledArg(MoneyFormatter.formatWhole(comparison.difference), accent),
                )
            }

            TrendDirection.Higher -> {
                styledStringResource(
                    R.string.insights_reflection_higher,
                    StyledArg(average, SpanStyle(fontWeight = FontWeight.Medium)),
                    StyledArg(MoneyFormatter.formatWhole(comparison.difference), accent),
                )
            }

            TrendDirection.Same -> {
                styledStringResource(
                    R.string.insights_reflection_same,
                    StyledArg(average, SpanStyle(fontWeight = FontWeight.Medium)),
                )
            }

            null -> {
                styledStringResource(
                    R.string.insights_reflection_average,
                    StyledArg(average, SpanStyle(fontWeight = FontWeight.Medium)),
                )
            }
        }

    QoodyInset(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(QoodyTheme.spacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.cozy)) {
            Surface(
                modifier = Modifier.size(QoodyTheme.sizes.reflectionIconBox),
                shape = QoodyTheme.shapes.pill,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    QoodyIcon(
                        id =
                            if (comparison?.direction ==
                                TrendDirection.Higher
                            ) {
                                R.drawable.ic_trending_up
                            } else {
                                R.drawable.ic_trending_down
                            },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primaryContainer,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel(stringResource(R.string.insights_reflection_label))
                    Text(
                        text = stringResource(R.string.insights_per_day, average),
                        style = QoodyTheme.typography.numericSm.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(text = sentence, style = QoodyTheme.typography.bodyMd, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun VerifiedFooter() {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = QoodyTheme.sizes.touchTarget),
        horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QoodyIcon(
            id = R.drawable.ic_task_alt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            size = QoodyTheme.sizes.iconMd,
        )
        Text(
            text = stringResource(R.string.insights_verified_footer),
            style = QoodyTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
