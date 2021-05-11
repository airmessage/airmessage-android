package me.tagavari.airmessage.messaging

import android.os.Parcel
import android.os.Parcelable
import java.io.File
import java.io.Serializable

/**
 * Represents a file queued to be sent
 */
data class FileDraft constructor(
	val localID: Long,
	val file: File,
	val fileName: String,
	val fileSize: Long,
	val fileType: String?,
	val mediaStoreID: Long? = null,
	val modificationDate: Long? = null
) : Serializable, Parcelable {
	override fun equals(other: Any?): Boolean {
		if(other == null || other.javaClass != this.javaClass) return false
		
		//Require other draft files to have a defined MediaStore ID, and have the MediaStore ID and modification date match
		other as FileDraft
		return other.mediaStoreID != -1L && mediaStoreID == other.mediaStoreID && modificationDate == other.modificationDate
	}
	
	override fun describeContents(): Int {
		return 0
	}
	
	override fun writeToParcel(out: Parcel, flags: Int) {
		out.writeLong(localID)
		out.writeString(file.path)
		out.writeString(fileName)
		out.writeLong(fileSize)
		out.writeString(fileType)
		out.writeLong(mediaStoreID ?: -1)
		out.writeLong(modificationDate ?: -1)
	}
	
	private constructor(parcel: Parcel): this(
		localID = parcel.readLong(),
		file = File(parcel.readString()!!),
		fileName = parcel.readString()!!,
		fileSize = parcel.readLong(),
		fileType = parcel.readString(),
		mediaStoreID = parcel.readLong().let { if(it == -1L) null else it },
		modificationDate = parcel.readLong().let { if(it == -1L) null else it }
	)
	
	companion object {
		@JvmField val CREATOR: Parcelable.Creator<FileDraft> = object : Parcelable.Creator<FileDraft> {
			override fun createFromParcel(`in`: Parcel): FileDraft? {
				return FileDraft(`in`)
			}
			
			override fun newArray(size: Int): Array<FileDraft?> {
				return arrayOfNulls(size)
			}
		}
	}
}