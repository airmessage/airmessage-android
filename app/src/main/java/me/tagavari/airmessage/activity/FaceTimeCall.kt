package me.tagavari.airmessage.activity

import android.app.assist.AssistContent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentOnAttachListener
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import me.tagavari.airmessage.R
import me.tagavari.airmessage.connection.exception.AMRequestException
import me.tagavari.airmessage.extension.FragmentCommunicationFaceTime
import me.tagavari.airmessage.fragment.FragmentCallActive
import me.tagavari.airmessage.fragment.FragmentCallPending
import me.tagavari.airmessage.fragment.FragmentCommunication
import me.tagavari.airmessage.helper.ConnectionServiceLink
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventFaceTime

class FaceTimeCall : AppCompatActivity(R.layout.activity_facetimecall), FragmentCommunicationFaceTime {
	//State
	private val viewModel: ActivityViewModel by viewModels()
	private val csLink = ConnectionServiceLink(this)
	
	//A composite disposable, valid for the lifecycle of this call
	private val compositeDisposableCalls = CompositeDisposable()
	
	//Listeners
	private val fragmentOnAttachListener = FragmentOnAttachListener { _, fragment ->
		//Set the communications callback when new fragments are added
		if(fragment is FragmentCommunication<*>) {
			(fragment as FragmentCommunication<FragmentCommunicationFaceTime>).communicationsCallback = this
		}
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		//Render edge-to-edge
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		//Show on lockscreen
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
			setShowWhenLocked(true)
			setTurnScreenOn(true)
		} else {
			window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
		}
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		
		//Add the fragment on attach listener
		supportFragmentManager.addFragmentOnAttachListener(fragmentOnAttachListener)
		
		if(savedInstanceState == null) {
			//Read the parameters
			@Type val type = intent.extras!!.getInt(PARAM_TYPE)
			val participants = intent.extras!!.getStringArrayList(PARAM_PARTICIPANTS)
			val participantsRaw = intent.extras!!.getString(PARAM_PARTICIPANTS_RAW)
			val initiateCallOutgoing = intent.extras!!.getBoolean(PARAM_INITIATE_OUTGOING)
			
			//Set the initial state
			viewModel.state = if(type == Type.outgoing) State.outgoing else State.incoming
			viewModel.participantsRaw = participantsRaw
			
			//Add the fragment
			supportFragmentManager.commit {
				setReorderingAllowed(true)
				add<FragmentCallPending>(
					R.id.content,
					args = bundleOf(
						FragmentCallPending.PARAM_STATE to if(viewModel.state == State.outgoing) FragmentCallPending.State.outgoing else FragmentCallPending.State.incoming,
						FragmentCallPending.PARAM_PARTICIPANTS to participants,
						FragmentCallPending.PARAM_PARTICIPANTS_RAW to participantsRaw
					)
				)
			}
			
			//Initiate the outgoing call if asked
			if(type == Type.outgoing && initiateCallOutgoing) {
				csLink.onServiceConnection { connectionManager ->
					compositeDisposableCalls.add(
						connectionManager.initiateFaceTimeCall(participants!!)
							.subscribe({}, { error ->
								updateStateError((error as AMRequestException).errorDetails)
							})
					)
				}
			}
		}
		
		//Subscribe to updates
		if(viewModel.state != State.rejected) {
			compositeDisposableCalls.addAll(
				ReduxEmitterNetwork.faceTimeUpdateSubject.subscribe(this::handleFaceTimeUpdate),
				ReduxEmitterNetwork.faceTimeIncomingCallerSubject.subscribe { caller ->
					//If there's no more call and we're in an incoming state, end the task
					if(!caller.isPresent && viewModel.state == State.incoming) {
						finishAndRemoveTask()
					}
				}
			)
		}
	}
	
	override fun onDestroy() {
		super.onDestroy()
		
		//Remove the fragment on attach listener
		supportFragmentManager.removeFragmentOnAttachListener(fragmentOnAttachListener)
		
		//Make sure we clean up any listeners
		compositeDisposableCalls.dispose()
	}
	
	override fun onProvideAssistContent(outContent: AssistContent) {
		super.onProvideAssistContent(outContent)
		outContent.webUri = viewModel.faceTimeLink?.let { Uri.parse(it) }
	}
	
	private fun updateStateRejected() {
		//Update the state to rejected
		viewModel.state = State.rejected
		
		//Update the fragment
		val fragment = supportFragmentManager.findFragmentById(R.id.content) as FragmentCallPending
		fragment.updateState(FragmentCallPending.State.disconnected)
		
		//Clean up, call lifecycle is finished
		compositeDisposableCalls.dispose()
	}
	
	private fun updateStateError(errorDetails: String?) {
		//Update the state to rejected
		updateStateRejected()
		
		//Show an error dialog
		val fragment = supportFragmentManager.findFragmentById(R.id.content) as FragmentCallPending
		fragment.showError(errorDetails)
	}
	
	private fun updateStateConnecting() {
		//Update the state to connecting
		viewModel.state = State.connecting
		
		//Update the fragment
		val fragment = supportFragmentManager.findFragmentById(R.id.content) as FragmentCallPending
		fragment.updateState(FragmentCallPending.State.connecting)
	}
	
	private fun updateStateCalling(faceTimeLink: String) {
		//Update the state to calling
		viewModel.state = State.inCall
		viewModel.faceTimeLink = faceTimeLink
		
		//Switch to the calling fragment
		supportFragmentManager.commit {
			setReorderingAllowed(true)
			replace<FragmentCallActive>(
				R.id.content,
				args = bundleOf(
					FragmentCallActive.PARAM_LINK to faceTimeLink
				)
			)
		}
	}
	
	private fun handleFaceTimeUpdate(update: ReduxEventFaceTime) {
		if(viewModel.state == State.outgoing) {
			when(update) {
				is ReduxEventFaceTime.OutgoingAccepted -> updateStateCalling(update.faceTimeLink)
				is ReduxEventFaceTime.OutgoingRejected -> updateStateRejected()
				is ReduxEventFaceTime.OutgoingError -> updateStateError(update.errorDetails)
				else -> {}
			}
		} else if(viewModel.state == State.connecting) {
			when(update) {
				is ReduxEventFaceTime.IncomingHandled -> updateStateCalling(update.faceTimeLink)
				is ReduxEventFaceTime.IncomingHandleError -> updateStateError(update.errorDetails)
				else -> {}
			}
		}
	}
	
	override fun exitCall() {
		if(viewModel.state != State.rejected) {
			//Tell the server to drop the call
			if(viewModel.state == State.incoming) {
				csLink.connectionManager?.handleIncomingFaceTimeCall(viewModel.participantsRaw!!, false)
			} else {
				csLink.connectionManager?.dropFaceTimeCallServer()
			}
		}
		
		finishAndRemoveTask()
	}
	
	override fun acceptCall() {
		csLink.connectionManager?.also { connectionManager ->
			//Accept the call
			connectionManager.handleIncomingFaceTimeCall(viewModel.participantsRaw!!, true)
			
			//Update the state
			updateStateConnecting()
		} ?: run {
			updateStateError(resources.getString(R.string.error_noconnection))
		}
	}
	
	class ActivityViewModel : ViewModel() {
		@State var state: Int = State.outgoing
		var faceTimeLink: String? = null
		var participantsRaw: String? = null
	}
	
	@Retention(AnnotationRetention.SOURCE)
	@IntDef(Type.outgoing, Type.incoming)
	annotation class Type {
		companion object {
			const val outgoing = 0
			const val incoming = 1
		}
	}
	
	@Retention(AnnotationRetention.SOURCE)
	@IntDef(State.outgoing, State.incoming, State.connecting, State.rejected, State.inCall)
	private annotation class State {
		companion object {
			const val outgoing = 0 //We're waiting for an outgoing call to go through
			const val incoming = 1 //We're asking the user if they want to accept an incoming call
			const val connecting = 2 //We're waiting for an incoming call to connect
			const val rejected = 3 //A call failed to connect
			const val inCall = 4 //We're currently in a call
		}
	}
	
	companion object {
		const val PARAM_TYPE = "type" //The activity call type, either incoming or outgoing
		const val PARAM_PARTICIPANTS = "participants" //An array of participant addresses
		const val PARAM_PARTICIPANTS_RAW = "participantsRaw" //A raw string of participants, if their addresses aren't available
		const val PARAM_INITIATE_OUTGOING = "initiateOutgoing" //If the type is set to outgoing, initiate that call with the server
	}
}