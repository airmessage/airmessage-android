package me.tagavari.airmessage.redux

sealed interface ReduxEventFaceTime {
	//Outgoing calls
	class OutgoingAccepted(val faceTimeLink: String) : ReduxEventFaceTime
	object OutgoingRejected : ReduxEventFaceTime
	class OutgoingError(val errorDetails: String?) : ReduxEventFaceTime
	
	//Incoming calls
	class IncomingHandled(val faceTimeLink: String) : ReduxEventFaceTime
	class IncomingHandleError(val errorDetails: String?) : ReduxEventFaceTime
}