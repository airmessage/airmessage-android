package me.tagavari.airmessage.compose.component

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.filterNotNull
import me.tagavari.airmessage.activity.Preferences
import me.tagavari.airmessage.compose.provider.LocalConnectionManager
import me.tagavari.airmessage.compose.state.NewConversationViewModel
import me.tagavari.airmessage.compose.state.SelectedRecipient
import me.tagavari.airmessage.enums.ConversationRecipientInputType
import me.tagavari.airmessage.helper.AddressHelper
import me.tagavari.airmessage.messaging.ConversationInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConversationPane(
	modifier: Modifier = Modifier,
	navigationIcon: @Composable () -> Unit = {},
	onSelectConversation: (ConversationInfo) -> Unit
) {
	val viewModel = viewModel<NewConversationViewModel>()
	
	//Listen for selection events
	LaunchedEffect(viewModel.launchFlow) {
		viewModel.launchFlow
			.filterNotNull()
			.collect { conversation ->
				//Submit the conversation
				onSelectConversation(conversation)
				
				//Reset the state
				viewModel.launchFlow.emit(null)
			}
	}
	
	var inputRecipient by rememberSaveable { mutableStateOf("") }
	var inputRecipientType by rememberSaveable { mutableStateOf(ConversationRecipientInputType.EMAIL) }
	
	val inputRecipientValid by remember {
		derivedStateOf {
			AddressHelper.validateAddress(inputRecipient)
		}
	}
	
	///Adds the current input text as a recipient
	fun addInputRecipient() {
		//Ignore if the current input text isn't valid
		if(!inputRecipientValid) return
		
		//Add the recipient
		viewModel.addSelectedRecipient(
			SelectedRecipient(address = AddressHelper.normalizeAddress(inputRecipient))
		)
		
		//Clear the input text
		inputRecipient = ""
	}
	
	Scaffold(
		modifier = modifier,
		topBar = {
			val context = LocalContext.current
			val connectionManager = LocalConnectionManager.current
			
			NewConversationAppBar(
				navigationIcon = navigationIcon,
				onDone = { viewModel.createConversation(connectionManager) },
				showServiceSelector = remember { Preferences.getPreferenceTextMessageIntegration(context) },
				selectedService = viewModel.selectedService,
				onSelectService = { viewModel.selectedService = it },
				textInput = inputRecipient,
				onChangeTextInput = { inputRecipient = it },
				inputType = inputRecipientType,
				onChangeInputType = { inputRecipientType = it },
				recipients = viewModel.selectedRecipients,
				onAddRecipient = ::addInputRecipient,
				onRemoveRecipient = viewModel::removeSelectedRecipient,
				isLoading = viewModel.isLoading
			)
		}
	) { innerPadding ->
		val launchContactsPermission = rememberLauncherForActivityResult(
			contract = ActivityResultContracts.RequestPermission(),
		) { permissionGranted ->
			//Reload contacts after the user grants permission
			if(permissionGranted) {
				viewModel.loadContacts()
			}
		}
		
		NewConversationBody(
			modifier = Modifier.fillMaxSize(),
			contentPadding = innerPadding,
			contactsState = viewModel.contactsState,
			onRequestPermission = { launchContactsPermission.launch(Manifest.permission.READ_CONTACTS) },
			onReloadContacts = { viewModel.loadContacts() },
			directAddText = if(inputRecipientValid) inputRecipient else null,
			onDirectAdd = ::addInputRecipient,
			onAddRecipient = { contactInfo, addressInfo ->
				viewModel.addSelectedRecipient(
					SelectedRecipient(
						address = addressInfo.address,
						name = contactInfo.name
					)
				)
			},
			isLoading = viewModel.isLoading
		)
	}
}
