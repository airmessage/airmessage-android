package me.tagavari.airmessage.coil

import android.graphics.drawable.Icon
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A fetcher that resolves [Icon] instances
 */
class IconFetcher private constructor(
	private val data: Icon,
	private val options: Options
) : Fetcher {
	override suspend fun fetch(): FetchResult? {
		val drawable = withContext(Dispatchers.IO) {
			data.loadDrawable(options.context)
		}
		
		return drawable?.let { DrawableResult(it, false, DataSource.MEMORY) }
	}
	
	class Factory : Fetcher.Factory<Icon> {
		override fun create(data: Icon, options: Options, imageLoader: ImageLoader): Fetcher {
			return IconFetcher(data, options)
		}
	}
}