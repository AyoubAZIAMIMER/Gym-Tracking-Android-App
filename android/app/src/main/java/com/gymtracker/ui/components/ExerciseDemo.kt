// Purpose: Two-frame exercise demonstration — start ↔ end position photos crossfading,
//          so every exercise *shows* its movement (public-domain free-exercise-db imagery)
// Inputs: asset paths from ExerciseMedia.imagesFor
// Outputs: pure visualization
package com.gymtracker.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.gymtracker.ui.theme.Motion
import kotlinx.coroutines.delay

@Composable
fun ExerciseDemo(frames: List<String>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmaps = remember(frames) {
        frames.mapNotNull { path ->
            runCatching {
                context.assets.open(path).use(BitmapFactory::decodeStream)?.asImageBitmap()
            }.getOrNull()
        }
    }
    if (bitmaps.isEmpty()) return

    // flip between start and end position — the movement, demonstrated
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(bitmaps.size) {
        while (bitmaps.size > 1) {
            delay(1_400)
            frame = (frame + 1) % bitmaps.size
        }
    }
    val aspect = bitmaps[0].width.toFloat() / bitmaps[0].height.toFloat()
    Crossfade(
        targetState = frame.coerceIn(0, bitmaps.lastIndex),
        animationSpec = Motion.settle(Motion.STANDARD),
        label = "exerciseDemo",
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Color.White),   // source photos are shot on white
    ) { i ->
        Image(
            bitmap = bitmaps[i],
            contentDescription = "Exercise demonstration",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect),
            contentScale = ContentScale.Fit,
        )
    }
}
