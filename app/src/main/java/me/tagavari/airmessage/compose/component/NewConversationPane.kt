package me.tagavari.airmessage.compose.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import me.tagavari.airmessage.compose.state.NewConversationViewModel
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.enums.ConversationRecipientInputType
import me.tagavari.airmessage.enums.MessageServiceDescription

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
	
	}
}

@Preview
@Composable
private fun PreviewNewConversationPane() {
	AirMessageAndroidTheme {
		NewConversationPane()
	}
}