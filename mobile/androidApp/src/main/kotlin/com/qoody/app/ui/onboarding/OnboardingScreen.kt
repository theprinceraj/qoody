package com.qoody.app.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.qoody.app.R
import com.qoody.app.ui.components.Dot
import com.qoody.app.ui.components.GhostButton
import com.qoody.app.ui.components.PerforationRule
import com.qoody.app.ui.components.PrimaryButton
import com.qoody.app.ui.components.QoodyCard
import com.qoody.app.ui.components.QoodyIcon
import com.qoody.app.ui.components.QoodyInset
import com.qoody.app.ui.components.QoodyModalSheet
import com.qoody.app.ui.theme.QoodyTheme
import com.qoody.shared.feature.onboarding.OnboardingViewModel
import org.koin.androidx.compose.koinViewModel

/** First-run screen: explains the idea and asks for notification access. */
@Composable
fun OnboardingScreen(
    onOpenNotificationAccessSettings: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    OnboardingContent(
        onEnableClick = {
            viewModel.onEnableNotificationAccess()
            onOpenNotificationAccessSettings()
        },
    )
}

@Composable
fun OnboardingContent(onEnableClick: () -> Unit) {
    var showPrivacySheet by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints(modifier = Modifier.safeDrawingPadding()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(horizontal = QoodyTheme.spacing.comfy, vertical = QoodyTheme.spacing.lg)
                        .widthIn(max = QoodyTheme.sizes.onboardingMaxWidth)
                        .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Hero()
                HowItWorksCard(modifier = Modifier.padding(vertical = QoodyTheme.spacing.lg))
                Actions(onEnableClick = onEnableClick, onLearnMoreClick = { showPrivacySheet = true })
            }
        }
    }

    if (showPrivacySheet) PrivacySheet(onDismiss = { showPrivacySheet = false })
}

@Composable
private fun Hero() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.ic_app_mark),
            contentDescription = stringResource(R.string.cd_app_logo),
            modifier = Modifier.size(QoodyTheme.sizes.appMark),
        )
        Row(
            modifier = Modifier.padding(top = QoodyTheme.spacing.md, bottom = QoodyTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.brand_wordmark),
                style = QoodyTheme.typography.headlineSm,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Dot(color = MaterialTheme.colorScheme.primaryContainer, size = QoodyTheme.sizes.dotXs)
        }
        Text(
            text = stringResource(R.string.onboarding_headline),
            style = QoodyTheme.typography.headlineLgMobile,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.onboarding_subhead),
            style = QoodyTheme.typography.bodyMd,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = QoodyTheme.spacing.cozy),
        )
    }
}

@Composable
private fun HowItWorksCard(modifier: Modifier = Modifier) {
    val steps =
        listOf(
            R.string.onboarding_step_1_title to R.string.onboarding_step_1_body,
            R.string.onboarding_step_2_title to R.string.onboarding_step_2_body,
            R.string.onboarding_step_3_title to R.string.onboarding_step_3_body,
        )
    QoodyCard(modifier = modifier, contentPadding = PaddingValues(QoodyTheme.spacing.comfy)) {
        PerforationRule(modifier = Modifier.padding(bottom = QoodyTheme.spacing.md))
        Column(verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.md)) {
            steps.forEachIndexed { index, (title, body) ->
                StepRow(number = index + 1, title = stringResource(title), body = stringResource(body))
            }
        }
        LivePreview(modifier = Modifier.padding(top = QoodyTheme.spacing.md))
    }
}

@Composable
private fun StepRow(
    number: Int,
    title: String,
    body: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.cozy)) {
        Surface(
            modifier = Modifier.size(QoodyTheme.sizes.stepBadge),
            shape = QoodyTheme.shapes.pill,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(text = number.toString(), style = QoodyTheme.typography.numericSm)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = QoodyTheme.typography.buttonSm, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = body,
                style = QoodyTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = QoodyTheme.spacing.xxs),
            )
        }
    }
}

/** A tiny simulated ledger row showing what a captured payment will look like. */
@Composable
private fun LivePreview(modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = QoodyTheme.alphas.disabled,
        animationSpec = infiniteRepeatable(tween(QoodyTheme.motion.chartMillis * 2), RepeatMode.Reverse),
    )
    QoodyInset(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = QoodyTheme.spacing.cozy, vertical = QoodyTheme.spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.sm),
            ) {
                Dot(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    size = QoodyTheme.sizes.dotSm,
                    modifier = Modifier.alpha(pulse),
                )
                Text(
                    text = stringResource(R.string.onboarding_preview_merchant),
                    style = QoodyTheme.typography.bodySm,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
            Text(
                text = stringResource(R.string.onboarding_preview_amount),
                style = QoodyTheme.typography.numericSm,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = QoodyTheme.spacing.sm),
            )
        }
    }
}

@Composable
private fun Actions(
    onEnableClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.cozy),
    ) {
        PrimaryButton(
            text = stringResource(R.string.onboarding_cta),
            onClick = onEnableClick,
            trailingIcon = R.drawable.ic_arrow_forward,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.onboarding_learn_more),
            style = QoodyTheme.typography.bodySm.copy(textDecoration = TextDecoration.Underline),
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .heightIn(min = QoodyTheme.sizes.touchTarget)
                    .padding(vertical = QoodyTheme.spacing.sm)
                    .clickable(role = Role.Button, onClick = onLearnMoreClick),
        )
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(QoodyTheme.spacing.xs),
        ) {
            QoodyIcon(
                id = R.drawable.ic_lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                size = QoodyTheme.sizes.iconSm,
            )
            Text(
                text = stringResource(R.string.onboarding_trust_footer),
                style = QoodyTheme.typography.bodySm,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
private fun PrivacySheet(onDismiss: () -> Unit) {
    QoodyModalSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.privacy_sheet_title),
            style = QoodyTheme.typography.headlineSm,
            color = MaterialTheme.colorScheme.onSurface,
        )
        PledgeBlock(
            title = stringResource(R.string.privacy_reads_title),
            body = stringResource(R.string.privacy_reads_body),
        )
        PledgeBlock(
            title = stringResource(R.string.privacy_never_title),
            body = stringResource(R.string.privacy_never_body),
        )
        GhostButton(
            text = stringResource(R.string.privacy_dismiss),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PledgeBlock(
    title: String,
    body: String,
) {
    QoodyInset(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = QoodyTheme.typography.buttonSm, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = body,
            style = QoodyTheme.typography.bodySm,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = QoodyTheme.spacing.xxs),
        )
    }
}
