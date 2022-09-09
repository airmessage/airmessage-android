package me.tagavari.airmessage.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import me.tagavari.airmessage.compose.component.onboarding.OnboardingConnect
import me.tagavari.airmessage.compose.component.onboarding.OnboardingManual
import me.tagavari.airmessage.compose.component.onboarding.OnboardingManualState
import me.tagavari.airmessage.compose.component.onboarding.OnboardingWelcome
import me.tagavari.airmessage.connection.comm5.ProxyConnect
import soup.compose.material.motion.MaterialSharedAxisX

class OnboardingCompose : ComponentActivity() {
	@OptIn(ExperimentalAnimationApi::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		setContent {
			var screen by rememberSaveable { mutableStateOf(OnboardingComposeScreen.WELCOME) }
			
			//Navigate backwards when back is pressed
			BackHandler(enabled = screen != OnboardingComposeScreen.WELCOME) {
				screen = OnboardingComposeScreen.WELCOME
			}
			
			MaterialSharedAxisX(
				targetState = screen,
				forward = screen != OnboardingComposeScreen.WELCOME,
			) { animatedScreen ->
				when(animatedScreen) {
					OnboardingComposeScreen.WELCOME -> {
						OnboardingWelcome(
							modifier = Modifier.fillMaxSize(),
							showGoogle = ProxyConnect.isAvailable,
							onClickGoogle = { screen = OnboardingComposeScreen.CONNECT },
							onClickManual = { screen = OnboardingComposeScreen.MANUAL }
						)
					}
					OnboardingComposeScreen.CONNECT -> {
						OnboardingConnect(
							modifier = Modifier.fillMaxSize(),
							onEnterPassword = {},
							onReconnect = {}
						)
					}
					OnboardingComposeScreen.MANUAL -> {
						OnboardingManual(
							modifier = Modifier.fillMaxSize(),
							state = OnboardingManualState.Idle,
							onConnect = {},
							onReset = {},
							onCancel = {
								screen = OnboardingComposeScreen.WELCOME
							},
							onFinish = {}
						)
					}
				}
			}
		}
	}
}

private enum class OnboardingComposeScreen {
	WELCOME,
	MANUAL,
	CONNECT
}