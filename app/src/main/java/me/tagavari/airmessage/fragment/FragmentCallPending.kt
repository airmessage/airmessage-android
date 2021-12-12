package me.tagavari.airmessage.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IntDef
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

class FragmentCallPending : FragmentCommunication<FragmentCommunicationFaceTime>(R.layout.fragment_calloutgoing) {
	//Views
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
			}
			
			//Copy to clipboard
			setNeutralButton(R.string.action_copy) { dialog, _ ->
				val clipboard = MainApplication.getInstance().getSystemService(
					AppCompatCompositeActivity.CLIPBOARD_SERVICE
				) as ClipboardManager
				clipboard.setPrimaryClip(ClipData.newPlainText("Error details", errorDetails))
				
				Toast.makeText(MainApplication.getInstance(), R.string.message_textcopied, Toast.LENGTH_SHORT).show()
				dialog.dismiss()
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