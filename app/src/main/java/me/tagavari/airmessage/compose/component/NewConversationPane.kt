package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import me.tagavari.airmessage.compose.state.NewConversationContactsState
import me.tagavari.airmessage.compose.state.NewConversationViewModel
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
	
	var selectedService by rememberSaveable { mutableStateOf(MessageServiceDescription.IMESSAGE) }
	
	val inputRecipientValid by remember {
		derivedStateOf {
			AddressHelper.validateAddress(inputRecipient)
		}
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
				recipients = viewModel.selectedRecipients
			)
		}
	) { contentPadding ->
		when(viewModel.contactsState) {
			is NewConversationContactsState.Loading -> {
			
			}
			is NewConversationContactsState.NeedsPermission -> {
			
			}
			is NewConversationContactsState.Error -> {
			
			}
			is NewConversationContactsState.Loaded -> {
				LazyColumn(
					contentPadding = contentPadding
				) {
					if(inputRecipientValid) {
						item {
							AddressRowDirect(
								address = inputRecipient,
								onClick = {
									viewModel.addSelectedRecipient(inputRecipient)
								}
							)
						}
					}
				}
			}
		}
	}
}
