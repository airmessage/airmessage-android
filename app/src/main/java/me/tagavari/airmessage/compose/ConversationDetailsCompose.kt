package me.tagavari.airmessage.compose

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import me.tagavari.airmessage.compose.component.ConversationDetails
import me.tagavari.airmessage.compose.state.ConversationDetailsViewModel
import me.tagavari.airmessage.compose.state.ConversationDetailsViewModelFactory
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.exception.LocationDisabledException
import me.tagavari.airmessage.exception.LocationUnavailableException
import me.tagavari.airmessage.flavor.ResolvableApiException
import me.tagavari.airmessage.helper.AddressHelper.validateEmail
import me.tagavari.airmessage.helper.AddressHelper.validatePhoneNumber
import me.tagavari.airmessage.helper.PlatformHelper
import me.tagavari.airmessage.helper.getParcelableExtraCompat
import me.tagavari.airmessage.util.LatLngInfo

class ConversationDetailsCompose : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		WindowCompat.setDecorFitsSystemWindows(window, false)
		
		val conversationID = intent.getLongExtra(ConversationsCompose.INTENT_TARGET_ID, -1)
		
		setContent {
			val application = LocalContext.current.applicationContext as Application
			val viewModel = viewModel<ConversationDetailsViewModel>(factory = ConversationDetailsViewModelFactory(application, conversationID))
			
			val currentLocationResult by viewModel.currentLocation.collectAsState()
			LaunchedEffect(viewModel.currentLocation) {
				viewModel.currentLocation.collect { result ->
					//Ignore idle result
					if(result == null) return@collect
					
					result.onSuccess { location ->
						//Return the location
						val data = Intent().apply {
							this.putExtra(intentParamLocation, location)
						}
						setResult(RESULT_OK, data)
						finish()
					}
					
					result.onFailure { exception ->
						//If the problem is resolvable, launch the resolution immediately
						if(exception is ResolvableApiException) {
							exception.startResolutionForResult(this@ConversationDetailsCompose, 0)
							viewModel.clearCurrentLocation()
						}
					}
				}
			}
			
			val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
				if(permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
					|| permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) {
					//Continue the request
					viewModel.loadCurrentLocation(this)
				}
			}
			
			AirMessageAndroidTheme {
				PlatformHelper.updateChromeOSTopBarCompose(this)
				
				Box(
					modifier = Modifier
						.background(MaterialTheme.colorScheme.background)
						.fillMaxSize()
				) {
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
							onSendCurrentLocation = {
								//Check if the app has access to the user's location
								if(ContextCompat.checkSelfPermission(this@ConversationDetailsCompose, android.Manifest.permission.ACCESS_COARSE_LOCATION)
									!= PackageManager.PERMISSION_GRANTED) {
									locationPermissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION))
									return@ConversationDetails
								}
								
								viewModel.loadCurrentLocation(this@ConversationDetailsCompose)
							},
							isLoadingCurrentLocation = viewModel.isLoadingCurrentLocation,
							noLocationDialog = currentLocationResult?.exceptionOrNull() is LocationUnavailableException,
							onHideNoLocationDialog = { viewModel.clearCurrentLocation() },
							locationDisabledDialog = currentLocationResult?.exceptionOrNull() is LocationDisabledException,
							onHideLocationDisabledDialog = { viewModel.clearCurrentLocation() },
							onPromptEnableLocationServices = {
								startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
							},
							onFinish = {
								setResult(RESULT_CANCELED)
								finish()
							}
						)
					}
				}
			}
		}
	}
	
	object ResultContract : ActivityResultContract<Long, LatLngInfo?>() {
		override fun createIntent(context: Context, input: Long) =
			Intent(context, ConversationDetailsCompose::class.java).apply {
				putExtra(ConversationsCompose.INTENT_TARGET_ID, input)
			}
		
		override fun parseResult(resultCode: Int, intent: Intent?): LatLngInfo? {
			return if(resultCode != RESULT_OK || intent == null) {
				null
			} else {
				intent.getParcelableExtraCompat(intentParamLocation)
			}
		}
	}
	
	companion object {
		const val intentParamLocation = "location"
	}
}