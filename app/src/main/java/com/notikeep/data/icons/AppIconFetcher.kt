package com.notikeep.data.icons

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options

/**
 * Coil model for an installed app's launcher icon. Using a dedicated type (not a
 * plain String) keeps the fetcher from hijacking every string request.
 */
data class AppIcon(val packageName: String)

/** Stable memory-cache key so each icon is decoded once per process. */
class AppIconKeyer : Keyer<AppIcon> {
    override fun key(data: AppIcon, options: Options): String = "appicon:${data.packageName}"
}

/**
 * Loads app icons from PackageManager on Coil's background dispatcher and
 * rasterizes them so they land in the memory cache. This is the core of the
 * "no jank" fix: icons never touch the main thread and are decoded at most once.
 */
class AppIconFetcher(
    private val context: Context,
    private val data: AppIcon,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val drawable = context.packageManager.getApplicationIcon(data.packageName)
        // Adaptive/vector icons are not memory-cacheable as-is; rasterize once.
        val bitmap = drawable.toBitmap(ICON_SIZE_PX, ICON_SIZE_PX)
        return DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = false,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<AppIcon> {
        override fun create(data: AppIcon, options: Options, imageLoader: ImageLoader): Fetcher =
            AppIconFetcher(context, data)
    }

    private companion object {
        /** 96px covers a 40dp icon up to ~2.4x density without visible blur. */
        const val ICON_SIZE_PX = 96
    }
}
