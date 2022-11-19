package me.tagavari.airmessage.common.messaging

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.core.os.ParcelCompat
import java.io.File

/**
 * An object that represents an attachment file
 * associated with a message
 */
@Immutable
class AttachmentInfo : MessageComponent {
	val fileName: String?
	val contentType: String?
	val fileSize: Long
	val sort: Long
	val file: File?
	val fileChecksum: ByteArray?
	val downloadFileName: String?
	val downloadFileType: String?
	val shouldAutoDownload: Boolean
	
	val computedFileName
		get() = downloadFileName ?: fileName
	val computedContentType
		get() = downloadFileType ?: contentType
	
	constructor(
		localID: Long,
		guid: String?,
		fileName: String?,
		contentType: String?,
		fileSize: Long,
		sort: Long = -1,
		file: File? = null,
		fileChecksum: ByteArray? = null,
		downloadFileName: String? = null,
		downloadFileType: String? = null,
		shouldAutoDownload: Boolean
	) : super(localID, guid) {
		this.fileName = fileName
		this.contentType = contentType
		this.fileSize = fileSize
		this.sort = sort
		this.file = file
		this.fileChecksum = fileChecksum
		this.downloadFileName = downloadFileName
		this.downloadFileType = downloadFileType
		this.shouldAutoDownload = shouldAutoDownload
	}
	
	constructor(
		localID: Long,
		guid: String?,
		stickers: MutableList<StickerInfo>,
		tapbacks: MutableList<TapbackInfo>,
		previewState: Int,
		previewID: Long,
		fileName: String?,
		contentType: String?,
		fileSize: Long,
		sort: Long = -1,
		file: File? = null,
		fileChecksum: ByteArray? = null,
		downloadFileName: String? = null,
		downloadFileType: String? = null,
		shouldAutoDownload: Boolean
	) : super(localID, guid, stickers, tapbacks, previewState, previewID) {
		this.fileName = fileName
		this.contentType = contentType
		this.fileSize = fileSize
		this.sort = sort
		this.file = file
		this.fileChecksum = fileChecksum
		this.downloadFileName = downloadFileName
		this.downloadFileType = downloadFileType
		this.shouldAutoDownload = shouldAutoDownload
	}
	
	override fun copy(
		localID: Long,
		guid: String?,
		stickers: MutableList<StickerInfo>,
		tapbacks: MutableList<TapbackInfo>,
		previewState: Int,
		previewID: Long
	) = AttachmentInfo(
		localID,
		guid,
		stickers,
		tapbacks,
		previewState,
		previewID,
		fileName,
		contentType,
		fileSize,
		sort,
		file,
		fileChecksum,
		downloadFileName,
		downloadFileType,
		shouldAutoDownload
	)
	
	fun copy(
		localID: Long = this.localID,
		guid: String? = this.guid,
		stickers: MutableList<StickerInfo> = this.stickers,
		tapbacks: MutableList<TapbackInfo> = this.tapbacks,
		previewState: Int = this.previewState,
		previewID: Long = this.previewID,
		fileName: String? = this.fileName,
		contentType: String? = this.contentType,
		fileSize: Long = this.fileSize,
		sort: Long = this.sort,
		file: File? = this.file,
		fileChecksum: ByteArray? = this.fileChecksum,
		downloadFileName: String? = this.downloadFileName,
		downloadFileType: String? = this.downloadFileType,
		shouldAutoDownload: Boolean = this.shouldAutoDownload
	) = AttachmentInfo(
		localID,
		guid,
		stickers,
		tapbacks,
		previewState,
		previewID,
		fileName,
		contentType,
		fileSize,
		sort,
		file,
		fileChecksum,
		downloadFileName,
		downloadFileType,
		shouldAutoDownload
	)
	
	override fun describeContents(): Int {
		return 0
	}
	
	override fun writeToParcel(parcel: Parcel, flags: Int) {
		super.writeToParcel(parcel, flags)
		
		parcel.writeString(fileName)
		parcel.writeString(contentType)
		parcel.writeLong(fileSize)
		parcel.writeLong(sort)
		parcel.writeString(file?.path)
		if(fileChecksum != null) {
			parcel.writeInt(fileChecksum.size)
			parcel.writeByteArray(fileChecksum)
		} else {
			parcel.writeInt(0)
		}
		parcel.writeString(downloadFileName)
		parcel.writeString(downloadFileType)
		ParcelCompat.writeBoolean(parcel, shouldAutoDownload)
	}
	
	private constructor(parcel: Parcel) : super(parcel) {
		fileName = parcel.readString()
		contentType = parcel.readString()
		fileSize = parcel.readLong()
		sort = parcel.readLong()
		
		file = parcel.readString()?.let { File(it) }
		fileChecksum = parcel.readInt().let { fileChecksumLength ->
			if(fileChecksumLength > 0) {
				ByteArray(fileChecksumLength).also { parcel.readByteArray(it) }
			} else {
				null
			}
		}
		downloadFileName = parcel.readString()
		downloadFileType = parcel.readString()
		shouldAutoDownload = ParcelCompat.readBoolean(parcel)
	}
	
	companion object {
		@JvmField val CREATOR: Parcelable.Creator<AttachmentInfo> = object : Parcelable.Creator<AttachmentInfo> {
			override fun createFromParcel(parcel: Parcel) = AttachmentInfo(parcel)
			override fun newArray(size: Int) = arrayOfNulls<AttachmentInfo>(size)
		}
	}
}