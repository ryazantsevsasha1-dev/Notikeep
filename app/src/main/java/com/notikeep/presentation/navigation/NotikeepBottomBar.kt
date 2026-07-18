package com.notikeep.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom navigation bar in the "Sber" style: every tab shows an icon over an
 * always-visible label, and the selected tab gets a pill-shaped tinted background
 * plus a coloured icon/label. The selected accent is per-tab so the "saved"
 * Favorites tab wears the teal accent while the rest use the primary blue.
 *
 * Kept as its own component (not inlined into the NavHost) so the navigation shell
 * stays a thin router and the bar's look/animation live in one place.
 */
@Composable
fun NotikeepBottomBar(
    tabs: List<TopTab>,
    isSelected: (TopTab) -> Boolean,
    onSelect: (TopTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = modifier,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                BottomBarItem(
                    tab = tab,
                    selected = isSelected(tab),
                    accent = tab.accent(),
                    onClick = { onSelect(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Teal for the "saved" Favorites tab, primary blue for everything else. */
@Composable
private fun TopTab.accent(): Color = when (this) {
    TopTab.FAVORITES -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun BottomBarItem(
    tab: TopTab,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val idle = MaterialTheme.colorScheme.onSurfaceVariant

    // Smoothly cross-fade content colour and grow the pill/icon on selection.
    val contentColor by animateColorAsState(
        targetValue = if (selected) accent else idle,
        animationSpec = tween(durationMillis = 220),
        label = "navItemColor",
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "navPillColor",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "navIconScale",
    )

    Column(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(pillColor)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                tab.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp).scale(iconScale),
            )
        }
        AutoShrinkLabel(
            text = stringResource(tab.labelRes),
            color = contentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * One-line tab label that shrinks its font until the text fits, instead of
 * truncating ("Избранно…") or overlapping neighbours at large accessibility
 * font scales. Invisible until the fitting pass settles, so the shrink never
 * flickers on screen.
 */
@Composable
private fun AutoShrinkLabel(
    text: String,
    color: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    val base = MaterialTheme.typography.labelMedium
    var style by remember(text, base) { mutableStateOf(base) }
    var fitted by remember(text, base) { mutableStateOf(false) }
    Text(
        text = text,
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier.drawWithContent { if (fitted) drawContent() },
        onTextLayout = { result ->
            if (result.didOverflowWidth && style.fontSize > MIN_LABEL_SIZE) {
                style = style.copy(fontSize = style.fontSize * 0.9f)
            } else {
                fitted = true
            }
        },
    )
}

private val MIN_LABEL_SIZE = 9.sp
