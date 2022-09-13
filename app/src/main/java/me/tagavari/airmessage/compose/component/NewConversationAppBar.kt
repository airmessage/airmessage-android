package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import me.tagavari.airmessage.R
import me.tagavari.airmessage.component.ContactChip
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.enums.ConversationRecipientInputType
import me.tagavari.airmessage.enums.MessageServiceDescription

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConversationAppBar(
	navigationIcon: @Composable () -> Unit = {},
	selectedService: MessageServiceDescription,
	onSelectService: (MessageServiceDescription) -> Unit,
	textInput: String,
	onChangeTextInput: (String) -> Unit,
	inputType: ConversationRecipientInputType,
	onChangeInputType: (ConversationRecipientInputType) -> Unit,
	recipients: List<ContactChip>
) {
	fun toggleInputRecipientType() {
		onChangeInputType(
			if(inputType == ConversationRecipientInputType.EMAIL) {
				ConversationRecipientInputType.PHONE
			} else {
				ConversationRecipientInputType.EMAIL
			}
		)
	}
	
	Surface(
		tonalElevation = 3.dp
	) {
		Column {
			TopAppBar(
				title = {
					Text(stringResource(R.string.screen_newconversation))
				},
				navigationIcon = navigationIcon,
				colors = TopAppBarDefaults.smallTopAppBarColors(
					containerColor = Color.Transparent,
					scrolledContainerColor = Color.Transparent
				)
			)
			
			//Via
			Row(
				modifier = Modifier.height(48.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					modifier = Modifier.width(60.dp),
					textAlign = TextAlign.Center,
					text = stringResource(R.string.part_via)
				)
				
				Row(
					modifier = Modifier
						.weight(1F)
						.horizontalScroll(rememberScrollState()),
					horizontalArrangement = Arrangement.spacedBy(8.dp)
				) {
					for(service in MessageServiceDescription.availableServices) {
						val selected = selectedService == service
						
						FilterChip(
							selected = selected,
							onClick = { onSelectService(service) },
							label = { Text(stringResource(service.title)) },
							leadingIcon = {
								if(selected) {
									Icon(
										imageVector = Icons.Default.Check,
										contentDescription = null
									)
								} else {
									Icon(
										painter = painterResource(id = service.icon),
										contentDescription = null
									)
								}
							}
						)
					}
				}
			}
			
			//To
			Row(
				modifier = Modifier.heightIn(min = 48.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					modifier = Modifier.width(60.dp),
					textAlign = TextAlign.Center,
					text = stringResource(R.string.part_to)
				)
				
				FlowRow(
					modifier = Modifier.weight(1F),
					mainAxisSpacing = 8.dp,
					crossAxisSpacing = 8.dp
				) {
					for(recipient in recipients) {
						InputChip(
							selected = false,
							onClick = {},
							label = {
								Text(recipient.display)
							}
						)
					}
					
					BasicTextField(
						modifier = Modifier.widthIn(min = 50.dp),
						value = textInput,
						onValueChange = onChangeTextInput,
						decorationBox = { innerTextField ->
							if(textInput.isEmpty() && recipients.isEmpty()) {
								Text(
									text = stringResource(R.string.imperative_userinput),
									color = MaterialTheme.colorScheme.onSurfaceVariant
								)
							}
							
							innerTextField()
						},
						keyboardOptions = KeyboardOptions(
							keyboardType = inputType.keyboardType
						),
						singleLine = true
					)
				}
				
				IconButton(
					onClick = ::toggleInputRecipientType
				) {
					Icon(
						imageVector = when(inputType) {
							ConversationRecipientInputType.PHONE -> Icons.Outlined.Keyboard
							ConversationRecipientInputType.EMAIL -> Icons.Outlined.Dialpad
						},
						contentDescription = null
					)
				}
			}
		}
	}
}

@Preview
@Composable
private fun PreviewNewConversationAppBar() {
	AirMessageAndroidTheme {
		NewConversationAppBar(
			selectedService = MessageServiceDescription.IMESSAGE,
			onSelectService = {},
			textInput = "",
			onChangeTextInput = {},
			inputType = ConversationRecipientInputType.EMAIL,
			onChangeInputType = {},
			recipients = listOf(ContactChip("Cool Guy", "cool@guy.com"))
		)
	}
}