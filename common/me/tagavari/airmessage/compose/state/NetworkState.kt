package me.tagavari.airmessage.compose.state

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.rx3.asFlow
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.messaging.AttachmentInfo
import me.tagavari.airmessage.messaging.MessageInfo
import me.tagavari.airmessage.redux.ReduxEventAttachmentDownload

object NetworkState {
	val attachmentRequests = mutableStateMapOf<Long, StateFlow<Result<ReduxEventAttachmentDownload?>?>>()
	
	/**
	 * Downloads an attachment, and adds it to the request map
	 */
	fun downloadAttachment(connectionManager: ConnectionManager, message: MessageInfo, attachment: AttachmentInfo): StateFlow<Result<ReduxEventAttachmentDownload?>?>? {
		val attachmentGUID = attachment.guid ?: return null
		val attachmentName = attachment.fileName ?: return null
		
		//Ignore if there's already a matching request, and the request
		//isn't failed
		attachmentRequests[attachment.localID]?.let { existingRequest ->
			if(existingRequest.value?.isFailure != true) {
				return null
			}
		}
		
		//Make the request
		val responseFlow = connectionManager.fetchAttachment(
			message.localID,
			attachment.localID,
			attachmentGUID,
			attachmentName
		)
			.map { Result.success<ReduxEventAttachmentDownload?>(it) }
			.onErrorReturn { Result.failure(it) }
			.asFlow()
			.stateIn(
				scope = MainScope(),
				started = SharingStarted.Eagerly,
				initialValue = null
			)
		
		//Record the request
		attachmentRequests[attachment.localID] = responseFlow
		
		return responseFlow
	}
}