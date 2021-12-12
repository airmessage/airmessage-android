package me.tagavari.airmessage.extension

sealed interface FragmentCommunicationFaceTime {
	fun startCall(faceTimeLink: String)
	fun exitCall()
}