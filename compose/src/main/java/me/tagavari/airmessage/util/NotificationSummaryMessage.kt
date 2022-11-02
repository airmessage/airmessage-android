package me.tagavari.airmessage.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

//Represents an item in a summary notification
@Parcelize
data class NotificationSummaryMessage(val conversationID: Int, val title: String, val description: String) : Parcelable