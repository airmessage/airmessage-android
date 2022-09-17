package me.tagavari.airmessage.compose.component

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.filterNotNull
import me.tagavari.airmessage.activity.Preferences
import me.tagavari.airmessage.compose.provider.LocalConnectionManager
import me.tagavari.airmessage.compose.state.NewConversationViewModel
import me.tagavari.airmessage.compose.state.SelectedRecipient
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
	
	///Adds the current input text as a recipient
	fun addInputRecipient() {
		//Ignore if the current input text isn't valid
		if(!viewModel.recipientInputValid) return
		
		//Add the recipient
		viewModel.addSelectedRecipient(
			SelectedRecipient(address = AddressHelper.formatAddress(viewModel.recipientInput))
		)
		
		//Clear the input text
		viewModel.recipientInput = ""
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
				textInput = viewModel.recipientInput,
				onChangeTextInput = { viewModel.recipientInput = it },
				inputType = viewModel.recipientInputType,
				onChangeInputType = { viewModel.recipientInputType = it },
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
			requiredService = viewModel.selectedService,
			onRequestPermission = { launchContactsPermission.launch(Manifest.permission.READ_CONTACTS) },
			onReloadContacts = { viewModel.loadContacts() },
			directAddText = if(viewModel.recipientInputValid) viewModel.recipientInput else null,
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
