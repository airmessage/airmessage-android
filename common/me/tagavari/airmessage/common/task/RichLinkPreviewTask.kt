package me.tagavari.airmessage.common.task

import android.webkit.URLUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

/**
 * Fetches rich link preview data for a given URL,
 * or throws an error if the operation failed
 */
@Throws(IOException::class)
suspend fun fetchRichMetadata(url: String): RichLinkPreview {
	val document = withContext(Dispatchers.IO) {
		try {
			Jsoup.connect(url)
				.timeout(15 * 1000)
				.get()
		} catch(exception: IllegalArgumentException) {
			//IllegalArgumentException is thrown for malformed URLs
			throw IOException(exception)
		}
	}
	
	//Find meta elements
	val elements = document.getElementsByTag("meta")
	
	//Get the article title
	val title = document.select("meta[property=og:title]").attr("content").ifEmpty { null }
		?: document.select("meta[name=og:title]").attr("content").ifEmpty { null }
		?: document.title()
	
	//Get the article description
	val description = document.select("meta[property=description]").attr("content").ifEmpty { null }
		?: document.select("meta[name=og:description]").attr("content").ifEmpty { null }
		?: document.select("meta[name=Description]").attr("content").ifEmpty { null }
	
	//Get image
	val imageURL = (
			document.select("meta[property=og:image]").attr("content").ifEmpty { null }
				?: document.select("meta[name=og:image]").attr("content").ifEmpty { null }
				?: document.select("link[rel=image_src]").attr("href").ifEmpty { null }
				?: document.select("link[rel=apple-touch-icon]").attr("href").ifEmpty { null }
				?: document.select("link[rel=icon]").attr("href").ifEmpty { null }
			)
		?.let { resolveURL(url, it) }
			
	//Get the favicon
	val faviconURL = (
			document.select("link[rel=apple-touch-icon]").attr("content").ifEmpty { null }
				?: document.select("link[rel=icon]").attr("content").ifEmpty { null }
			)
		?.let { resolveURL(url, it) }
	
	//Resolve basic site information
	val canonicalURL = elements
		.firstOrNull { it.attr("property") == "og:url" }?.attr("content")?.ifEmpty { null }
	
	val siteName = elements
		.firstOrNull { it.attr("property") == "og:site_name" }?.attr("content")?.ifEmpty { null }
	
	return RichLinkPreview(
		title = title,
		description = description,
		imageURL = imageURL,
		faviconURL = faviconURL,
		siteName = siteName,
		canonicalURL = canonicalURL
	)
}

/**
 * Resolves a URL path against a base URL
 */
private fun resolveURL(base: String, part: String): String? {
	if(URLUtil.isValidUrl(part)) {
		return part
	}
	
	try {
		return URI(base).resolve(part).toString()
	} catch(exception: URISyntaxException) {
		exception.printStackTrace()
	} catch(exception: IllegalArgumentException) {
		exception.printStackTrace()
	}
	
	return null
}

data class RichLinkPreview(
	val title: String?,
	val description: String?,
	val faviconURL: String?,
	val imageURL: String?,
	val siteName: String?,
	val canonicalURL: String?
)
