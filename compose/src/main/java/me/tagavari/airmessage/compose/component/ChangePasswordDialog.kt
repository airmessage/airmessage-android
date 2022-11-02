package me.tagavari.airmessage.compose.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import me.tagavari.airmessage.compose.R
import me.tagavari.airmessage.compose.provider.LocalConnectionManager
import me.tagavari.airmessage.compose.state.ChangePasswordDialogViewModel
import me.tagavari.airmessage.helper.ErrorDetailsHelper

/**
 * A dialog that asks the user to update their password
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordDialog(
	onDismissRequest: () -> Unit,
	onComplete: (String) -> Unit
) {
	val context = LocalContext.current
	val connectionManager = LocalConnectionManager.current
	val viewModel = viewModel<ChangePasswordDialogViewModel>()
	
	//Pass the complete event upwards
	LaunchedEffect(viewModel.isDone) {
		viewModel.isDone
			.filter { it }
			.collect { onComplete(viewModel.inputPassword) }
	}
	
	val showError = viewModel.connectionError != null
	
	AlertDialog(
		onDismissRequest = onDismissRequest,
		title = { Text(stringResource(R.string.action_updatepassword)) },
		text = {
			Column {
				val focusRequester = remember { FocusRequester() }
				val keyboardController = LocalSoftwareKeyboardController.current
				
				OutlinedTextField(
					modifier = Modifier.focusRequester(focusRequester),
					value = viewModel.inputPassword,
					onValueChange = { viewModel.inputPassword = it },
					singleLine = true,
					enabled = !viewModel.isLoading,
					label = {
						Text(stringResource(R.string.message_setup_connect_password))
					},
					visualTransformation = if(viewModel.passwordHidden) PasswordVisualTransformation() else VisualTransformation.None,
					keyboardOptions = KeyboardOptions(
						keyboardType = KeyboardType.Password,
						imeAction = ImeAction.Go,
						autoCorrect = false
					),
					keyboardActions = KeyboardActions(
						onGo = { viewModel.submit(connectionManager) }
					),
					trailingIcon = {
						IconButton(onClick = { viewModel.passwordHidden = !viewModel.passwordHidden }) {
							Icon(
								imageVector = if(viewModel.passwordHidden) Icons.Filled.Visibility
								else Icons.Filled.VisibilityOff,
								contentDescription = stringResource(
									if(viewModel.passwordHidden) R.string.action_showpassword
									else R.string.action_hidepassword
								)
							)
						}
					},
					isError = showError
				)
				
				Text(
					modifier = Modifier
						.padding(start = 16.dp, top = 4.dp)
						.alpha(if(showError) 1f else 0f),
					text = viewModel.connectionError?.let {
						context.resources.getString(ErrorDetailsHelper.getErrorDetails(it, true).label)
					} ?: "",
					color = MaterialTheme.colorScheme.error,
					style = MaterialTheme.typography.bodySmall
				)
				
				
				LaunchedEffect(Unit) {
					delay(100)
					focusRequester.requestFocus()
					keyboardController?.show()
				}
			}
		},
		confirmButton = {
			TextButton(
				onClick = { viewModel.submit(connectionManager) },
				enabled = viewModel.inputPassword.isNotEmpty()
			) {
				Text(stringResource(R.string.action_continue))
			}
		},
		dismissButton = {
			TextButton(
				onClick = onDismissRequest
			) {
				Text(stringResource(android.R.string.cancel))
			}
		}
	)
}