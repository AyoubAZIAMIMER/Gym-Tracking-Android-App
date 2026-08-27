// Purpose: Reward motion — a trophy PR banner, on-brand (gold) and physics-driven from a
//          single AnimatedVisibility so there are no per-frame allocations or animation leaks.
// Inputs: a `visible` trigger (Boolean); a label
// Outputs: none (pure overlay visualization)
package com.gymtracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.Motion

/**
 * A PR banner that pops in with a trophy and the new best, then can be dismissed by the
 * caller flipping [visible]. Gold, by the gold-for-PR law.
 */
@Composable
fun PrBanner(visible: Boolean, label: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(Motion.springMass(), initialScale = 0.8f) + fadeIn(Motion.settle(Motion.STANDARD)),
        exit = scaleOut(Motion.cool(Motion.FAST), targetScale = 0.9f) + fadeOut(Motion.cool(Motion.FAST)),
        modifier = modifier,
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(GymTheme.colors.prGold.copy(alpha = 0.16f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            // trophy taps in on a gentle infinite shimmer would be overkill here; the reveal
            // (scale+fade) carries it. Static gold glyph keeps it premium-calm.
            Icon(
                Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = GymTheme.colors.prGold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = GymTheme.colors.prGold,
            )
        }
    }
}
