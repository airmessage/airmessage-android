package me.tagavari.airmessage.coil

import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import ezvcard.Ezvcard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tagavari.airmessage.helper.FileHelper
import okio.Buffer
import java.io.File

/**
 * A fetcher that resolves vcard photo instances
 */
class VCardFetcher private constructor(
	private val data: File,
	private val options: Options
) : Fetcher {
	override suspend fun fetch(): FetchResult? {
		val vcard = withContext(Dispatchers.IO) {
			//Parse the vcard
			Ezvcard.parse(data).first()
		} ?: return null
		
		//Get the first photo
		val photo = vcard.photos.firstOrNull() ?: return null
		
		//Create an image source
		val source = Buffer().apply { write(photo.data) }
		return SourceResult(
			source = ImageSource(source, options.context),
			mimeType = photo.type,
			dataSource = DataSource.MEMORY
		)
	}
	
	class Factory : Fetcher.Factory<File> {
		override fun create(data: File, options: Options, imageLoader: ImageLoader): Fetcher? {
			//Don't support non-vcard files
			if(FileHelper.getMimeType(data) != "text/vcard") return null
			return VCardFetcher(data, options)
		}
	}
}
