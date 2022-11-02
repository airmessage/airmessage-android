package me.tagavari.airmessage.messaging

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

/**
 * A preview for an attachment file
 * @param name The name of this attachment file
 * @param type The MIME type of this attachment file
 */
@Immutable
@Parcelize
data class AttachmentPreview(val name: String?, val type: String?) : Serializable, Parcelable