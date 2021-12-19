package me.tagavari.airmessage.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.R
import me.tagavari.airmessage.composite.AppCompatCompositeActivity
import me.tagavari.airmessage.extension.FragmentCommunicationFaceTime
import me.tagavari.airmessage.helper.ContactHelper
import me.tagavari.airmessage.helper.LanguageHelper
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventConnection

class FragmentCallPending : FragmentCommunication<FragmentCommunicationFaceTime>(R.layout.fragment_callpending) {
	//Views
	private lateinit var viewCameraPreview: PreviewView
	private lateinit var labelParticipants: TextView
	private lateinit var labelStatus: TextView
	private lateinit var buttonEndCall: FloatingActionButton
	private lateinit var buttonAcceptCall: FloatingActionButton
	
	//Parameters
	@State private var state: Int = State.outgoing
	private var callParticipants: List<String>? = null
	private var callParticipantsRaw: String? = null
	
	//Disposables
	private val compositeDisposable = CompositeDisposable()
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		//Get the parameters
		state = requireArguments().getInt(PARAM_STATE)
		callParticipants = requireArguments().getStringArrayList(PARAM_PARTICIPANTS)
		callParticipantsRaw = requireArguments().getString(PARAM_PARTICIPANTS_RAW)
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		//Get the views
		viewCameraPreview = view.findViewById(R.id.camera_preview)
		labelParticipants = view.findViewById(R.id.label_participants)
		labelStatus = view.findViewById(R.id.label_status)
		buttonEndCall = view.findViewById(R.id.button_endcall)
		buttonAcceptCall = view.findViewById(R.id.button_acceptcall)
		
		//Update the initial state
		updateState(state)
		
		callParticipantsRaw?.also { participants ->
			//Set the participant names directly
			labelParticipants.text = participants
		} ?: callParticipants?.also { participantsList ->
			//Load the participant names from the user's contacts
			labelParticipants.text = LanguageHelper.createLocalizedList(resources, participantsList)
			compositeDisposable.add(
				Observable.fromIterable(participantsList)
					.flatMapSingle { address ->
						ContactHelper.getUserDisplayName(MainApplication.getInstance(), address)
							.defaultIfEmpty(address)
					}
					.toList()
					.subscribe { participants ->
						labelParticipants.text = LanguageHelper.createLocalizedList(resources, participants)
					}
			)
		} ?: run {
			labelParticipants.setText(R.string.part_unknown)
		}
		
		//Set the callback listeners
		buttonEndCall.setOnClickListener {
			communicationsCallback?.exitCall()
		}
		buttonAcceptCall.setOnClickListener {
			communicationsCallback?.acceptCall()
		}
		
		//Start the camera preview
		val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
		cameraProviderFuture.addListener({
			//Used to bind the lifecycle of cameras to the lifecycle owner
			val cameraProvider = cameraProviderFuture.get()
			
			//Preview
			val preview = Preview.Builder()
				.build()
				.also {
					it.setSurfaceProvider(viewCameraPreview.surfaceProvider)
				}
			
			try {
				//Unbind use cases before rebinding
				cameraProvider.unbindAll()
				
				//Bind use cases to camera
				cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview)
			} catch(exception: Exception) {
				exception.printStackTrace()
			}
		}, ContextCompat.getMainExecutor(requireContext()))
		
		//Subscribe to connection updates
		compositeDisposable.add(
			ReduxEmitterNetwork.connectionStateSubject.subscribe { update ->
				buttonAcceptCall.isEnabled = update is ReduxEventConnection.Connected
			}
		)
	}
	
	/**
	 * Updates the display state of the UI
	 */
	fun updateState(@State state: Int) {
		this.state = state
		
		labelStatus.setText(when(state) {
			State.outgoing -> R.string.message_facetime_calling
			State.incoming -> R.string.title_facetime
			State.connecting -> R.string.message_facetime_connecting
			State.disconnected -> R.string.message_facetime_unavailable
			else -> R.string.message_facetime_unavailable
		})
		
		buttonEndCall.contentDescription = resources.getString(
			if(state == State.incoming) R.string.action_declinecall
			else R.string.action_endcall
		)
		buttonAcceptCall.visibility = if(state == State.incoming) View.VISIBLE else View.GONE
	}
	
	/**
	 * Shows an error dialog with the specified error details
	 */
	fun showError(errorDetails: String?) {
		MaterialAlertDialogBuilder(requireContext()).apply {
			setTitle(R.string.message_facetime_error_call)
			//Use a custom view with monospace font
			errorDetails?.let { errorDetails ->
				setView(
					layoutInflater.inflate(R.layout.dialog_simplescroll, null).apply {
						findViewById<TextView>(R.id.text).apply {
							typeface = Typeface.MONOSPACE
							text = errorDetails
						}
					}
				)
				
				//Add copy to clipboard button
				setNeutralButton(R.string.action_copy) { dialog, _ ->
					val clipboard = MainApplication.getInstance().getSystemService(
						AppCompatCompositeActivity.CLIPBOARD_SERVICE
					) as ClipboardManager
					clipboard.setPrimaryClip(ClipData.newPlainText("Error details", errorDetails))
					
					Toast.makeText(MainApplication.getInstance(), R.string.message_textcopied, Toast.LENGTH_SHORT).show()
					dialog.dismiss()
				}
			}
			setPositiveButton(R.string.action_dismiss) { dialog, _ ->
				dialog.dismiss()
			}
		}.create().show()
	}
	
	@Retention(AnnotationRetention.SOURCE)
	@IntDef(State.outgoing, State.incoming, State.connecting, State.disconnected)
	annotation class State {
		companion object {
			const val outgoing = 0
			const val incoming = 1
			const val connecting = 2
			const val disconnected = 3
		}
	}
	
	companion object {
		const val PARAM_STATE = "state"
		const val PARAM_PARTICIPANTS = "participants"
		const val PARAM_PARTICIPANTS_RAW = "participantsRaw"
	}
}