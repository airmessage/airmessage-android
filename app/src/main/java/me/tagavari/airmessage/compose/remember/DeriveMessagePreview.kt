package me.tagavari.airmessage.compose.remember

import android.util.Patterns
import androidx.collection.LruCache
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tagavari.airmessage.activity.Preferences
import me.tagavari.airmessage.constants.DataSizeConstants
import me.tagavari.airmessage.data.DatabaseManager
import me.tagavari.airmessage.enums.MessagePreviewState
import me.tagavari.airmessage.enums.MessagePreviewType
import me.tagavari.airmessage.helper.DataCompressionHelper
import me.tagavari.airmessage.helper.DataStreamHelper
import me.tagavari.airmessage.helper.LanguageHelper
import me.tagavari.airmessage.messaging.MessageInfo
import me.tagavari.airmessage.messaging.MessagePreviewInfo
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventMessaging
import me.tagavari.airmessage.task.fetchRichMetadata
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

private val activeJobs = mutableSetOf<String>()
private val activeJobsMutex = Mutex()

private val messagePreviewCache = LruCache<Long, MessagePreviewInfo>(4)
private val messagePreviewCacheMutex = Mutex()

/**
 * Resolves a message preview for a message.
 * If the message already has a preview, it will be loaded into memory.
 * If the message hasn't been checked for a preview, one will be fetched.
 *
 * @param messageInfo The message to check for previews for
 * @return The message preview to display, or null if none should be displayed
 */
@Composable
fun deriveMessagePreview(messageInfo: MessageInfo): State<MessagePreviewInfo?> {
	//Ignore if previews are disabled
	if(!Preferences.getPreferenceMessagePreviews(LocalContext.current)) {
		return remember { mutableStateOf(null) }
	}
	
	val messagePreview = remember(messageInfo.localID) { mutableStateOf<MessagePreviewInfo?>(null) }
	
	LaunchedEffect(messageInfo.localID) {
		//Get the message's text component
		val textComponent = messageInfo.messageTextComponent ?: return@LaunchedEffect
		val text = textComponent.text ?: return@LaunchedEffect
		
		when(textComponent.previewState) {
			//If we have an available preview, fetch it
			MessagePreviewState.available -> {
				val preview = messagePreviewCacheMutex.withLock {
					//If we have an existing value, use it
					messagePreviewCache[textComponent.previewID]?.let {
						return@withLock it
					}
					
					//Load the preview from disk
					val preview = withContext(Dispatchers.IO) {
						DatabaseManager.getInstance().loadMessagePreview(textComponent.previewID)
					} ?: return@withLock null
					
					//Update the cache
					messagePreviewCache.put(textComponent.previewID, preview)
					
					return@withLock preview
				}
				
				//Update the preview state
				preview?.let {
					messagePreview.value = it
				}
			}
			//If we haven't tried for a preview, fetch a preview
			MessagePreviewState.notTried -> {
				//Check if the entire message is a URL
				if(!text.matches(Patterns.WEB_URL.toRegex())) return@LaunchedEffect
				
				activeJobsMutex.withLock {
					//Ignore if we're already fetching this text
					if(activeJobs.contains(text)) return@LaunchedEffect
					
					//Register the job
					activeJobs.add(text)
				}
				
				try {
					//Fetch the link preview
					val metadata = fetchRichMetadata(text)
					
					//Validate generic information
					val title: String
					val domain: String
					try {
						title = metadata.title
							?: throw IllegalStateException("No title available")
						domain = LanguageHelper.getDomainName(text)
							?: throw IllegalStateException("No domain available for $text")
					} catch(exception: IllegalStateException) {
						//Mark the preview as unavailable
						DatabaseManager.getInstance().setMessagePreviewState(messageInfo.localID, MessagePreviewState.unavailable)
						
						//Emit an update
						withContext(Dispatchers.Main) {
							ReduxEmitterNetwork.messageUpdateSubject.onNext(
								ReduxEventMessaging.PreviewUpdate(messageInfo.localID, Result.failure(exception))
							)
						}
						
						return@LaunchedEffect
					}
					
					val description = metadata.description
					
					//Download the image
					val imageBytes: ByteArray? = metadata.imageURL?.let { url ->
						withContext(Dispatchers.IO) {
							try {
								BufferedInputStream(URL(url).openStream()).use { inputStream ->
									ByteArrayOutputStream().use { outputStream ->
										DataStreamHelper.copyStream(inputStream, outputStream)
										val downloadedImageBytes = outputStream.toByteArray()
										
										//Compress the image if it is too large
										if(downloadedImageBytes.size <= DataSizeConstants.previewImageMaxSize) {
											downloadedImageBytes
										} else {
											DataCompressionHelper.compressBitmap(
												downloadedImageBytes,
												"image/webp",
												DataSizeConstants.previewImageMaxSize
											)
										}
									}
								}
							} catch(exception: IOException) {
								exception.printStackTrace()
								null
							} catch(exception: MalformedURLException) {
								exception.printStackTrace()
								null
							}
						}
					}
					
					//Create the preview info
					val preview = MessagePreviewInfo(
						type = MessagePreviewType.link,
						localID = -1,
						data = imageBytes,
						target = text,
						title = title,
						subtitle = description ?: "",
						caption = domain
					)
					
					//Write the metadata to disk
					val previewLocalID = DatabaseManager.getInstance().setMessagePreviewData(messageInfo.localID, preview)
					preview.localID = previewLocalID
					
					//Emit an update
					withContext(Dispatchers.Main) {
						ReduxEmitterNetwork.messageUpdateSubject.onNext(
							ReduxEventMessaging.PreviewUpdate(messageInfo.localID, Result.success(preview))
						)
					}
					
					//Save the item in the cache
					messagePreviewCacheMutex.withLock {
						messagePreviewCache.put(preview.localID, preview)
					}
					
					//Set the preview state
					messagePreview.value = preview
				} finally {
					//Unregister the job
					activeJobsMutex.withLock {
						activeJobs.remove(text)
					}
				}
			}
			//No preview to show :(
			MessagePreviewState.unavailable -> {}
		}
	}
	
	return messagePreview
}
