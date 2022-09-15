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
import androidx.lifecycle.viewmodel.compose.viewModel
import me.tagavari.airmessage.compose.state.NewConversationViewModel
import me.tagavari.airmessage.compose.state.SelectedRecipient
import me.tagavari.airmessage.enums.ConversationRecipientInputType
import me.tagavari.airmessage.enums.MessageServiceDescription
import me.tagavari.airmessage.helper.AddressHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConversationPane(
	modifier: Modifier = Modifier,
	navigationIcon: @Composable () -> Unit = {}
) {
	val viewModel = viewModel<NewConversationViewModel>()
	
	var inputRecipient by rememberSaveable { mutableStateOf("") }
	var inputRecipientType by rememberSaveable { mutableStateOf(ConversationRecipientInputType.EMAIL) }
	
	val inputRecipientValid by remember {
		derivedStateOf {
			AddressHelper.validateAddress(inputRecipient)
		}
	}
	
	var selectedService by rememberSaveable { mutableStateOf(MessageServiceDescription.IMESSAGE) }
	
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
			NewConversationAppBar(
				navigationIcon = navigationIcon,
				selectedService = selectedService,
				onSelectService = { selectedService = it },
				textInput = inputRecipient,
				onChangeTextInput = { inputRecipient = it },
				inputType = inputRecipientType,
				onChangeInputType = { inputRecipientType = it },
				recipients = viewModel.selectedRecipients,
				onAddRecipient = ::addInputRecipient,
				onRemoveRecipient = viewModel::removeSelectedRecipient
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
			}
		)
	}
}
