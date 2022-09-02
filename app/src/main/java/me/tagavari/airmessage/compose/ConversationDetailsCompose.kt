package me.tagavari.airmessage.compose

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import me.tagavari.airmessage.activity.Messaging
import me.tagavari.airmessage.compose.component.ConversationDetails
import me.tagavari.airmessage.compose.state.ConversationDetailsViewModel
import me.tagavari.airmessage.compose.state.ConversationDetailsViewModelFactory
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.helper.AddressHelper.validateEmail
import me.tagavari.airmessage.helper.AddressHelper.validatePhoneNumber

class ConversationDetailsCompose : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		val conversationID = intent.getLongExtra(Messaging.intentParamTargetID, -1)
		
		setContent {
			val application = LocalContext.current.applicationContext as Application
			val viewModel = viewModel<ConversationDetailsViewModel>(factory = ConversationDetailsViewModelFactory(application, conversationID))
			
			AirMessageAndroidTheme {
				viewModel.conversation?.let { conversation ->
					ConversationDetails(
						conversation = conversation,
						onClickMember = { memberInfo, userInfo ->
							if(userInfo != null) {
								//If the user exists, jump right to them
								Intent(Intent.ACTION_VIEW).apply {
									data = userInfo.contactLookupUri
								}
							} else {
								//If the user doesn't exist, prompt the user to create a new contact
								Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
									type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
									
									if(validateEmail(memberInfo.address)) {
										putExtra(ContactsContract.Intents.Insert.EMAIL, memberInfo.address)
									} else if(validatePhoneNumber(memberInfo.address)) {
										putExtra(ContactsContract.Intents.Insert.PHONE, memberInfo.address)
									}
								}
							}.let { startActivity(it) }
						},
						onFinish = {
							finish()
						}
					)
				}
			}
		}
	}
}