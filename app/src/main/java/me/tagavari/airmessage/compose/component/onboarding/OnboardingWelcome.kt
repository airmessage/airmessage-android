package me.tagavari.airmessage.compose.component.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.tagavari.airmessage.R
import me.tagavari.airmessage.compose.ui.theme.AirMessageAndroidTheme
import me.tagavari.airmessage.compose.util.annotatedStringResource

@Composable
fun OnboardingWelcome(
	modifier: Modifier = Modifier,
	showGoogle: Boolean = true,
	showManual: Boolean = true,
	onClickGoogle: () -> Unit = {},
	onClickManual: () -> Unit = {}
) {
	Column(
		modifier = modifier
			.verticalScroll(rememberScrollState())
			.padding(24.dp)
	) {
		Spacer(modifier = Modifier.height(30.dp))
		
		Image(
			modifier = Modifier.fillMaxWidth(),
			painter = painterResource(id = R.drawable.promo_logo),
			contentDescription = stringResource(id = R.string.app_name)
		)
		
		Spacer(modifier = Modifier.height(50.dp))
		
		//Title
		Text(
			text = stringResource(R.string.message_onboardinginstructions_title),
			style = MaterialTheme.typography.headlineSmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		
		Spacer(modifier = Modifier.height(30.dp))
		
		//Step 1
		Row {
			Image(
				modifier = Modifier
					.size(72.dp)
					.clip(RoundedCornerShape(16.dp)),
				painter = painterResource(id = R.drawable.ic_feature_macmini_square),
				contentDescription = null
			)
			
			Spacer(modifier = Modifier.width(30.dp))
			
			Column(modifier = Modifier.weight(1F)) {
				Text(
					text = stringResource(R.string.message_onboardinginstructions_macmini_title),
					style = MaterialTheme.typography.bodyLarge
				)
				
				Spacer(modifier = Modifier.height(10.dp))
				
				Text(
					text = annotatedStringResource(R.string.message_onboardinginstructions_macmini_desc),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
		
		Spacer(modifier = Modifier.height(30.dp))
		
		//Step 2
		Row {
			Image(
				modifier = Modifier
					.size(72.dp)
					.clip(RoundedCornerShape(16.dp)),
				painter = painterResource(id = R.drawable.ic_feature_airmessage_square),
				contentDescription = null
			)
			
			Spacer(modifier = Modifier.width(30.dp))
			
			Column(modifier = Modifier.weight(1F)) {
				Text(
					text = stringResource(R.string.message_onboardinginstructions_airmessage_title),
					style = MaterialTheme.typography.bodyLarge
				)
				
				Spacer(modifier = Modifier.height(10.dp))
				
				Text(
					text = stringResource(R.string.message_onboardinginstructions_airmessage_desc),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
		
		Spacer(modifier = Modifier.height(30.dp))
		
		Column(
			verticalArrangement = Arrangement.spacedBy(10.dp)
		) {
			//Sign in with Google
			if(showGoogle) {
				ElevatedButton(
					modifier = Modifier.fillMaxWidth(),
					onClick = onClickGoogle,
					colors = buttonColors(
						containerColor = Color.White,
						contentColor = Color.DarkGray
					)
				) {
					Icon(
						painter = painterResource(R.drawable.ic_google),
						contentDescription = null,
						tint = Color.Unspecified,
					)
					
					Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
					
					Text(stringResource(R.string.action_signin_google))
				}
			}
			
			//Use manual configuration
			if(showManual) {
				TextButton(
					modifier = Modifier.fillMaxWidth(),
					onClick = onClickManual
				) {
					Icon(
						Icons.Default.SettingsEthernet,
						contentDescription = null
					)
					
					Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
					
					Text(stringResource(R.string.action_manualconfiguration))
				}
			}
		}
	}
}

@Preview
@Composable
private fun PreviewOnboardingWelcome() {
	AirMessageAndroidTheme {
		Surface(
			color = MaterialTheme.colorScheme.background
		) {
			OnboardingWelcome()
		}
	}
}
