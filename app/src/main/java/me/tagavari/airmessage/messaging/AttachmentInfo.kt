package me.tagavari.airmessage.messaging

import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.SingleSubject
import java.io.File
import java.util.function.Supplier

class AttachmentInfo : MessageComponent {
	val fileName: String?
	val contentType: String?
	var fileSize: Long
	var sort: Long
	var file: File?
	var fileChecksum: ByteArray?
	var shouldAutoDownload: Boolean
	
	constructor(
		localID: Long,
		guid: String?,
		fileName: String?,
		contentType: String?,
		fileSize: Long,
		sort: Long = -1,
		file: File? = null,
		fileChecksum: ByteArray? = null,
		shouldAutoDownload: Boolean
	) : super(localID, guid) {
		this.fileName = fileName
		this.contentType = contentType
		this.fileSize = fileSize
		this.sort = sort
		this.file = file
		this.fileChecksum = fileChecksum
		this.shouldAutoDownload = shouldAutoDownload
	}
	
	@Transient
	private var displayMetadataSubject: SingleSubject<FileDisplayMetadata>? = null
	
	fun <T : FileDisplayMetadata> getDisplayMetadata(supplier: Supplier<Single<T>>): Single<T> {
		if(displayMetadataSubject != null) return displayMetadataSubject as Single<T>
		displayMetadataSubject = SingleSubject.create()
		supplier.get().subscribe(displayMetadataSubject)
		return displayMetadataSubject as Single<T>
	}
	
	fun clone(): AttachmentInfo {
		return AttachmentInfo(localID, guid, fileName, contentType, fileSize, sort, file, fileChecksum, shouldAutoDownload)
	}
	
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
			parcel.writeInt(fileChecksum!!.size)
			parcel.writeByteArray(fileChecksum)
		} else {
			parcel.writeInt(0)
		}
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
		shouldAutoDownload = ParcelCompat.readBoolean(parcel)
	}
	
	companion object {
		@JvmField val CREATOR: Parcelable.Creator<AttachmentInfo> = object : Parcelable.Creator<AttachmentInfo> {
			override fun createFromParcel(parcel: Parcel) = AttachmentInfo(parcel)
			override fun newArray(size: Int) = arrayOfNulls<AttachmentInfo>(size)
		}
	}
}