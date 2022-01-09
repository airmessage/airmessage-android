package me.tagavari.airmessage.service

import android.os.Build
import android.util.Base64
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import me.tagavari.airmessage.activity.Messaging
import me.tagavari.airmessage.common.Blocks
import me.tagavari.airmessage.common.Blocks.ModifierInfo
import me.tagavari.airmessage.connection.comm5.AirUnpacker
import me.tagavari.airmessage.connection.comm5.ClientProtocol3
import me.tagavari.airmessage.connection.comm5.ClientProtocol4
import me.tagavari.airmessage.connection.comm5.ClientProtocol5
import me.tagavari.airmessage.connection.encryption.EncryptionAES
import me.tagavari.airmessage.connection.task.MessageUpdateTask
import me.tagavari.airmessage.connection.task.ModifierUpdateTask
import me.tagavari.airmessage.data.SharedPreferencesManager.getDirectConnectionPassword
import me.tagavari.airmessage.helper.ConnectionServiceLaunchHelper.launchTemporary
import me.tagavari.airmessage.helper.NotificationHelper.sendDecryptErrorNotification
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEmitterNetwork.messageUpdateSubject
import me.tagavari.airmessage.redux.ReduxEventMessaging
import me.tagavari.airmessage.redux.ReduxEventMessaging.StickerAdd
import me.tagavari.airmessage.redux.ReduxEventMessaging.TapbackUpdate
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.*

class FCMService : FirebaseMessagingService() {
	override fun onMessageReceived(remoteMessage: RemoteMessage) {
		val remoteMessageData = remoteMessage.data
		
		//If we have no data, connect to the server to retrieve it
		if(remoteMessageData.isEmpty()) {
			startConnectionService(remoteMessage)
			return
		}
		
		//Read the payload version
		val payloadVersionString = remoteMessageData["payload_version"] ?: run {
			Log.w(TAG, "No version string for FCM data")
			return
		}
		val payloadVersion = try {
			payloadVersionString.toInt()
		} catch(exception: NumberFormatException) {
			exception.printStackTrace()
			return
		}
		
		//Read the protocol version
		val protocolVersionString = remoteMessageData["protocol_version"] ?: run {
			Log.w(TAG, "No payload for FCM data version $payloadVersion")
			return
		}
		val protocolVersion = try {
			protocolVersionString.splitToSequence('.')
				.map { it.toInt() }
				.toList()
		} catch(exception: NumberFormatException) {
			exception.printStackTrace()
			return
		}
		
		//Read the payload
		val fcmPayloadEncoded = remoteMessageData["payload"] ?: run {
			Log.w(TAG, "No payload for FCM data version $payloadVersion")
			return
		}
		val fcmPayload = try {
			Base64.decode(fcmPayloadEncoded, Base64.DEFAULT)
		} catch(exception: IllegalArgumentException) {
			exception.printStackTrace()
			return
		}
		
		if(payloadVersion == 3) {
			//Unpack the payload
			val payload = run {
				val airUnpacker = AirUnpacker(fcmPayload)
				val isEncrypted = airUnpacker.unpackBoolean()
				if(isEncrypted) {
					try {
						val password = getDirectConnectionPassword(this)
							?: throw GeneralSecurityException("No password available")
						
						val encryptionAES = EncryptionAES(password)
						encryptionAES.decrypt(airUnpacker.unpackPayload())
					} catch(exception: GeneralSecurityException) {
						exception.printStackTrace()
						
						//Notify the user
						sendDecryptErrorNotification(this)
						return
					} catch(exception: IOException) {
						exception.printStackTrace()
						sendDecryptErrorNotification(this)
						return
					}
				} else {
					airUnpacker.unpackPayload()
				}
			}
			
			//Read the payload
			val airUnpacker = AirUnpacker(payload)
			val payloadType = airUnpacker.unpackInt()
			if(payloadType == 0) { //Message payload
				handleStandardMessagePayload(remoteMessage, protocolVersion, airUnpacker)
			} else if(payloadType == 1) { //FaceTime payload
				handleFaceTimePayload(remoteMessage, protocolVersion, airUnpacker)
			}
		} else if(payloadVersion == 2 || payloadVersion == 1) {
			//If the payload version is 2, the first byte represents whether the data is encrypted, and the data is included as a payload
			val payload = if(payloadVersion == 2) {
				val airUnpacker = AirUnpacker(fcmPayload)
				val isEncrypted = airUnpacker.unpackBoolean()
				if(isEncrypted) {
					try {
						val password = getDirectConnectionPassword(this)
							?: throw GeneralSecurityException("No password available")
						
						val encryptionAES = EncryptionAES(password)
						encryptionAES.decrypt(airUnpacker.unpackPayload())
					} catch(exception: GeneralSecurityException) {
						exception.printStackTrace()
						
						//Notify the user
						sendDecryptErrorNotification(this)
						return
					} catch(exception: IOException) {
						exception.printStackTrace()
						sendDecryptErrorNotification(this)
						return
					}
				} else {
					airUnpacker.unpackPayload()
				}
			} else {
				fcmPayload
			}
			
			//Handle the message payload
			handleStandardMessagePayload(remoteMessage, protocolVersion, AirUnpacker(payload))
		}
	}
	
	/**
	 * Handles a standard push notification payload for message items
	 */
	private fun handleStandardMessagePayload(remoteMessage: RemoteMessage, protocolVersion: List<Int>, airUnpacker: AirUnpacker) {
		var conversationItems: List<Blocks.ConversationItem>? = null
		var modifiers: List<ModifierInfo?>? = null
		var dataLoaded = false
		
		//Protocol version 5
		try {
			if(protocolVersion.size == 2 && protocolVersion[0] == 5) {
				when(protocolVersion[1]) {
					3 -> { //Protocol 5.3
						conversationItems = ClientProtocol3.unpackConversationItems(airUnpacker)
						modifiers = ClientProtocol3.unpackModifiers(airUnpacker)
						dataLoaded = true
					}
					4 -> { //Protocol 5.4
						conversationItems = ClientProtocol4.unpackConversationItems(airUnpacker)
						modifiers = ClientProtocol4.unpackModifiers(airUnpacker)
						dataLoaded = true
					}
					5 -> { //Protocol 5.5
						conversationItems = ClientProtocol5.unpackConversationItems(airUnpacker)
						modifiers = ClientProtocol5.unpackModifiers(airUnpacker)
						dataLoaded = true
					}
				}
			}
		} catch(exception: Exception) {
			exception.printStackTrace()
			return
		}
		
		if(!dataLoaded) {
			//Failed to parse payload; fetch messages from the connection service
			startConnectionService(remoteMessage)
			return
		}
		
		//Load the foreground conversations (needs to be done on the main thread)
		Single.fromCallable { Messaging.getForegroundConversations() }
			.subscribeOn(AndroidSchedulers.mainThread())
			.flatMap { foregroundConversations: List<Long> ->
				MessageUpdateTask.create(this, foregroundConversations, conversationItems!!, false)
			}
			.observeOn(AndroidSchedulers.mainThread())
			.doOnSuccess { response: MessageUpdateTask.Response ->
				//Emit any generated events
				for(event in response.events) {
					messageUpdateSubject.onNext(event)
				}
				
				//If we have incomplete conversations, query the server to complete them
				if(response.incompleteServerConversations.isNotEmpty()) {
					val connectionManager = ConnectionService.getConnectionManager()
					if(connectionManager != null) {
						connectionManager.addPendingConversations(response.incompleteServerConversations)
					} else {
						startConnectionService(remoteMessage)
					}
				}
			}.subscribe()
		
		//Write modifiers to disk
		ModifierUpdateTask.create(this, modifiers!!)
			.doOnSuccess { result: ModifierUpdateTask.Response ->
				//Push emitter updates
				for((messageID, messageState, dateRead) in result.activityStatusUpdates) {
					messageUpdateSubject.onNext(
						ReduxEventMessaging.MessageState(messageID, messageState, dateRead)
					)
				}
				for((first, second) in result.stickerModifiers) messageUpdateSubject.onNext(
					StickerAdd(first, second)
				)
				for((first, second) in result.tapbackModifiers) messageUpdateSubject.onNext(
					TapbackUpdate(first, second, true)
				)
				for((first, second) in result.tapbackRemovals) messageUpdateSubject.onNext(
					TapbackUpdate(first, second, false)
				)
			}.subscribe()
	}
	
	/**
	 * Handles a FaceTime notification payload
	 */
	private fun handleFaceTimePayload(remoteMessage: RemoteMessage, protocolVersion: List<Int>, airUnpacker: AirUnpacker) {
		//Get the caller
		val caller: String? = run {
			try {
				//Protocol version 5
				if(protocolVersion.size == 2 && protocolVersion[0] == 5) {
					if(protocolVersion[1] == 5) { //Protocol 5.5
						return@run airUnpacker.unpackNullableString()
					}
				}
				
				Log.w(TAG, "Unreadable FaceTime payload received for protocol version ${protocolVersion.joinToString(".")}")
				return@run null
			} catch(exception: Exception) {
				exception.printStackTrace()
				return
			}
		}
		
		//Emit an update
		ReduxEmitterNetwork.faceTimeIncomingCallerSubject.onNext(Optional.ofNullable(caller))
	}
	
	override fun onDeletedMessages() = Unit
	override fun onNewToken(token: String) {
		//Update Connect servers with the new token
		ConnectionService.getConnectionManager()?.sendPushToken(token)
	}
	
	private fun startConnectionService(remoteMessage: RemoteMessage) {
		/* Only starting the service if it isn't already running (for example, if it's bound to an activity, we'll want to leave it that way).
		 *
		 * Android 12 also introduced restrictions as to when a foreground service can be launched from the background,
		 * so we should check if we are allowed to launch one before doing so.
		 */
		if(ConnectionService.getInstance() == null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || remoteMessage.priority == RemoteMessage.PRIORITY_HIGH)) {
			launchTemporary(this)
		}
	}
	
	companion object {
		private val TAG = FCMService::class.java.simpleName
	}
}