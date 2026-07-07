package com.notikeep.presentation.common

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.notikeep.data.icons.AppIcon

/**
 * App launcher icon, loaded asynchronously via the app-wide Coil loader.
 * Never blocks the main thread; icons are memory-cached after first decode.
 */
@Composable
fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    AsyncImage(
        model = AppIcon(packageName),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp)),
    )
}
