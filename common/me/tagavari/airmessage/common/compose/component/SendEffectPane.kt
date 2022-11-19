package me.tagavari.airmessage.common.compose.component

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import me.tagavari.airmessage.common.constants.ColorConstants
import me.tagavari.airmessage.common.helper.SendStyleHelper
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.*
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Size
import java.util.concurrent.TimeUnit

private val parties = listOf(
	Party(
		speed = 0f,
		maxSpeed = 15f,
		damping = 0.9f,
		size = listOf(Size(12, 5F, 0.2F), Size(16, 6F, 0.2F)),
		colors = ColorConstants.effectColors,
		angle = Angle.BOTTOM,
		spread = Spread.ROUND,
		emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(300),
		position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0)),
		timeToLive = 5000,
		fadeOutEnabled = false
	)
)

@Composable
fun SendEffectPane(
	modifier: Modifier = Modifier,
	activeEffect: String? = null,
	onFinishEffect: () -> Unit
) {
	when(activeEffect) {
		SendStyleHelper.appleSendStyleScrnConfetti -> {
			KonfettiView(
				modifier = modifier,
				parties = parties,
				updateListener = object : OnParticleSystemUpdateListener {
					override fun onParticleSystemEnded(system: PartySystem, activeSystems: Int) {
						if(activeSystems == 0) {
							onFinishEffect()
						}
					}
				}
			)
		}
		else -> {
			//Finish the effect immediately
			LaunchedEffect(activeEffect) {
				//TODO show a toast message
				onFinishEffect()
			}
		}
	}
}

data class SendEffectPaneState(
	val activeEffect: String?,
	val playEffect: (String) -> Unit,
	val clearEffect: () -> Unit
)

@Composable
fun rememberSendEffectPaneState(): SendEffectPaneState {
	//val activeEffectFlow = remember { MutableStateFlow<String?>(null) }
	var activeEffect by remember { mutableStateOf<String?>(null) }
	
	return SendEffectPaneState(
		activeEffect = activeEffect,
		playEffect = { effect ->
			//Ignore if we're already playing an effect
			if(activeEffect == null) {
				activeEffect = effect
			}
		},
		clearEffect = {
			activeEffect = null
		}
	)
}
