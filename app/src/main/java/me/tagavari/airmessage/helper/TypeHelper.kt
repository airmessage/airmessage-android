package me.tagavari.airmessage.helper

import me.tagavari.airmessage.constants.MIMEConstants
import me.tagavari.airmessage.enums.MessageComponentType
import me.tagavari.airmessage.helper.FileHelper.compareMimeTypes
import me.tagavari.airmessage.messaging.AttachmentInfo
import me.tagavari.airmessage.messaging.MessageComponent
import me.tagavari.airmessage.messaging.MessageComponentText

object TypeHelper {
	/**
	 * Gets a MessageComponentType from a MessageComponent
	 */
	@MessageComponentType
	fun getMessageComponentType(component: MessageComponent): Int {
		return if(component is MessageComponentText) MessageComponentType.text
		else if(component is AttachmentInfo) {
			if(compareMimeTypes(component.contentType!!, MIMEConstants.mimeTypeImage) ||
				compareMimeTypes(component.contentType, MIMEConstants.mimeTypeVideo)) MessageComponentType.attachmentVisual
			else if(compareMimeTypes(component.contentType, MIMEConstants.mimeTypeAudio)) MessageComponentType.attachmentAudio
			else if(compareMimeTypes(component.contentType, MIMEConstants.mimeTypeVCard)) MessageComponentType.attachmentContact
			else if(compareMimeTypes(component.contentType, MIMEConstants.mimeTypeVLocation)) MessageComponentType.attachmentLocation
			else MessageComponentType.attachmentDocument
		} else {
			throw IllegalArgumentException("Unknown component type")
		}
	}
}