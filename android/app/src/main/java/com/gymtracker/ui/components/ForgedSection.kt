// Purpose: The redesign's shared screen vocabulary, lifted from the prototype
//          (design/redesign-2026-07/prototype/"Forged Prototype.dc.html"). Every screen is the
//          same four parts: a 28sp screen title with a muted trailing caption, hairline-ruled
//          sections, stamped mono section labels, and flat rows with a chevron. No cards.
// Inputs: text + optional trailing slots
// Outputs: ForgedScreenTitle / SectionRule / RowRule / StampText / ForgedSectionHeader / ForgedListRow
package com.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.LocalForge
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.StampLabel
import com.gymtracker.ui.theme.forgedEntrance
import com.gymtracker.ui.theme.forgedPress
import androidx.compose.foundation.shape.RoundedCornerShape
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/** Screen title row: 28sp/-0.5 tracking, with an optional muted caption on the right. */
@Composable
fun ForgedScreenTitle(
    title: String,
    trailing: String? = null,
    modifier: Modifier = Modifier,
    /** Non-null on pushed destinations; tabs carry no back arrow (handoff §Interactions). */
    onBack: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(
                start = if (onBack != null) 8.dp else Dim.screenPadH,
                end = Dim.screenPadH,
                top = 4.dp,
                bottom = 18.dp,
            ),
        verticalAlignment = if (onBack != null) Alignment.CenterVertically else Alignment.Bottom,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = title,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.5).sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Bottom-sheet header. Sheets get the same 22sp/-0.4 title and muted subtitle everywhere, so a
 * sheet reads as the screen continuing rather than as a different app (handoff README §Sheets).
 */
@Composable
fun SheetTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(
            start = Dim.screenPadH,
            end = Dim.screenPadH,
            top = 4.dp,
            bottom = if (subtitle == null) 14.dp else 16.dp,
        )
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.4).sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Screen-entry flag for [ForgedBlock]. Motion §10: sections arrive once per screen entry and
 * never replay on a tab return or a back-navigation, which is what rememberSaveable buys.
 */
@Composable
fun rememberEntered(): Boolean {
    var entered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    return entered
}

/**
 * One staggered section. Flattening the screens onto hairline rules removed the glass cards that
 * used to carry [forgedEntrance], so the redesign shipped with no arrival motion at all — this
 * puts it back on the flat layout. Stagger and reduce-motion are handled inside forgedEntrance.
 */
@Composable
fun ForgedBlock(
    index: Int,
    entered: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().forgedEntrance(index, entered), content = content)
}

/** Section rule — the prototype separates blocks with a hairline, never a card edge. */
@Composable
fun SectionRule(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = Dim.hairline,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** Row rule — quieter than the section rule, so rows group under their heading. */
@Composable
fun RowRule(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = Dim.hairline,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
    )
}

/** Stamped mono label: "TODAY", "THIS WEEK", "SESSIONS". */
@Composable
fun StampText(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier,
) {
    Text(text = text, style = StampLabel, color = color, modifier = modifier)
}

/**
 * Stamped section heading, optionally with a trailing text link ("NEXT UP … Full plan")
 * or an arbitrary trailing slot (the Anton week counter).
 */
@Composable
fun ForgedSectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    linkLabel: String? = null,
    onClickLink: (() -> Unit)? = null,
    bottomPadding: Dp = Dim.sectionBottom,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(
                start = Dim.screenPadH,
                end = Dim.screenPadH,
                top = Dim.sectionTop,
                bottom = bottomPadding,
            ),
        verticalAlignment = Alignment.Bottom,
    ) {
        StampText(label)
        Spacer(Modifier.weight(1f))
        when {
            trailing != null -> trailing()
            linkLabel != null && onClickLink != null -> Text(
                text = linkLabel,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onClickLink),
            )
        }
    }
}

/**
 * The prototype's list row: title + caption on the left, caller-supplied trailing content,
 * optional chevron. Press physics come from [forgedPress] so contact reads in one frame.
 */
@Composable
fun ForgedListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    titleSize: Float = 15.5f,
    verticalPadding: Dp = 12.dp,
    chevron: Boolean = false,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .forgedPress(interaction, pressedScale = 0.99f)
                        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else Modifier
            )
            .padding(horizontal = Dim.screenPadH, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = titleSize.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        trailing?.invoke(this)
        if (chevron) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = GymTheme.colors.hint,
            )
        }
    }
}

/**
 * The prototype's week strip: 28dp squircles, olive when logged, ember-dim ringed for today.
 * Fixed roles, not heat — a logged day is `success`, never a temperature (INTEGRATION.md §1).
 */
@Composable
fun ForgedWeekStrip(doneWeekdays: Set<DayOfWeek>) {
    val today = LocalDate.now().dayOfWeek
    val cellShape = RoundedCornerShape(Dim.weekCellRadius)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = Dim.screenPadH, end = Dim.screenPadH, bottom = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DayOfWeek.entries.forEach { day ->
            val done = day in doneWeekdays
            val isToday = day == today
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(
                    text = day.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH).take(1),
                    fontSize = 11.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isToday) MaterialTheme.colorScheme.primary else GymTheme.colors.hint,
                    textAlign = TextAlign.Center,
                )
                Box(
                    Modifier
                        .size(Dim.weekCell)
                        .clip(cellShape)
                        .background(
                            when {
                                // fixed roles, not heat: olive = logged, ember dim = today
                                done -> GymTheme.colors.success
                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                else -> Color.Transparent
                            }
                        )
                        .border(
                            width = if (isToday && !done) 1.5.dp else Dim.hairline,
                            color = when {
                                done -> Color.Transparent
                                isToday -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = cellShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.background,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Every AlertDialog in the app, one shape: Anton title, 14sp body, surfaceContainer ground,
 * ember confirm / muted dismiss. Pass [destructive] when the confirm action loses data.
 */
@Composable
fun ForgedAlert(
    title: String,
    onDismissRequest: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String? = null,
    onDismissAction: (() -> Unit)? = null,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    body: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(Dim.heroRadius),
        containerColor = scheme.surfaceContainer,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = scheme.onSurface) },
        text = body,
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(
                    text = confirmLabel,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        !confirmEnabled -> GymTheme.colors.hint
                        destructive -> scheme.error
                        else -> scheme.primary
                    },
                )
            }
        },
        dismissButton = dismissLabel?.let {
            {
                TextButton(onClick = onDismissAction ?: onDismissRequest) {
                    Text(it, color = scheme.onSurfaceVariant)
                }
            }
        },
    )
}

/** Body copy inside a [ForgedAlert] — 14sp, muted, the same measure as a row subtitle. */
@Composable
fun AlertBody(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * The one primary button in the app: full-bleed ember slab, Anton label, 0.97 press.
 * Sheets and screens share it so "the committing action" always looks the same
 * (the Slate's Complete-set CTA is the same shape, plus its ember bloom).
 */
@Composable
fun ForgedCta(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme
    val forge = LocalForge.current
    Box(
        modifier
            .fillMaxWidth()
            .height(Dim.ctaHeight)
            // the halo the Slate's CTA has always had, now shared. Under Chalk & Iron this is a
            // colourless bloom — the button gains weight without the app gaining a hue.
            .then(
                if (enabled) {
                    Modifier.emberBloom(
                        color = forge.palette.action,
                        cornerRadius = Dim.ctaRadius,
                        spread = 20.dp,
                        intensity = forge.palette.glowIntensity,
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(Dim.ctaRadius))
            .background(if (enabled) scheme.primary else scheme.surfaceVariant)
            .then(if (enabled) Modifier.forgedPress(source) else Modifier)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) scheme.onPrimary else GymTheme.colors.hint,
        )
    }
}

/** Effort words, low → maximal (prototype `EFFORT_WORDS`). */
private val EffortWords = listOf("Easy", "Steady", "Hard", "Very hard", "All out")
/** Rising bar heights, the selector's whole visual idea (prototype `EFFORT_H`). */
private val EffortHeights = listOf(8, 11, 14, 17, 20)

/**
 * The prototype's EFFORT control: five rising bars, tap one. It replaces the tap-cycle chip —
 * five taps to reach "all out" was the wrong cost for a value you set every set.
 *
 * The app stores RPE 6..10 (nullable); the prototype's five steps map onto that range
 * one-to-one, so bar `i` (1..5) is RPE `i + 5`. Tapping the selected bar clears it.
 */
@Composable
fun EffortBars(
    rpe: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    showWord: Boolean = true,
) {
    val selected = rpe?.let { (it - 5).coerceIn(1, 5) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        StampText("EFFORT")
        Spacer(Modifier.width(14.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EffortHeights.forEachIndexed { index, barHeight ->
                val step = index + 1
                val on = selected == step
                Box(
                    Modifier
                        // visual bar is 6dp wide; the tap target is padded out to stay reachable
                        .padding(horizontal = 5.dp, vertical = 12.dp)
                        .width(6.dp)
                        .height(barHeight.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (on) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                        .clickable { onSelect(if (on) null else step + 5) }
                )
            }
        }
        if (showWord) {
            Spacer(Modifier.weight(1f))
            Text(
                text = selected?.let { EffortWords[it - 1] } ?: "Not set",
                fontSize = 14.sp,
                fontWeight = if (selected != null) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected != null) MaterialTheme.colorScheme.onSurfaceVariant
                else GymTheme.colors.hint,
            )
        }
    }
}
