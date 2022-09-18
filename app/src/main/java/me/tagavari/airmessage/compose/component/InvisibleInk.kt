package me.tagavari.airmessage.compose.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

//How long a particle lasts in milliseconds
private const val particleLifetime = 3 * 1000

//How many particles to display for each pixel
private const val pixelsPerParticle = 150

//How far to move each particle across its lifetime
private val particleSpeed = 4.dp

//How large to display each particle
private val particleRadius = 0.7.dp

/**
 * A view that renders an invisible ink effect
 */
@Composable
fun InvisibleInk(
	modifier: Modifier = Modifier,
	particleColor: Color = LocalContentColor.current
) {
	val scope = rememberCoroutineScope()
	
	var frameTime by remember { mutableStateOf<Long?>(null) }
	LaunchedEffect(Unit) {
		while(true) {
			withFrameMillis { frameMs ->
				frameTime = frameMs
			}
		}
	}
	
	val particleList = remember { mutableStateListOf<Particle>() }
	
	//Make sure we have the right number of particles
	val sizeFlow = remember { MutableSharedFlow<IntSize>() }
	LaunchedEffect(sizeFlow) {
		snapshotFlow { frameTime != null }
			.filter { it }
			.combine(sizeFlow) { _, size -> size }
			.distinctUntilChanged()
			.collect { size ->
				val targetListSize = (size.width * size.height) / pixelsPerParticle
				val missingListEntries = targetListSize - particleList.size
				
				if(missingListEntries < 0) {
					//Remove excess entries
					particleList.removeRange(-targetListSize, particleList.size)
				} else {
					val prewarm = particleList.isEmpty()
					
			 		//Add missing entries
					repeat(missingListEntries) {
						val time = if(prewarm) {
							frameTime!! - Random.nextInt(particleLifetime)
						} else {
							frameTime!!
						}
						
						particleList.add(Particle(time))
					}
				}
			}
	}
	
	//Recycle dead particles
	LaunchedEffect(frameTime) {
		val time = frameTime ?: return@LaunchedEffect
		
		val iterator = particleList.listIterator()
		while(iterator.hasNext()) {
			val particle = iterator.next()
			if(particle.isDead(time)) {
				iterator.set(Particle(time))
			}
		}
	}
	
	Canvas(
		modifier = modifier
			.onGloballyPositioned { layout ->
				scope.launch {
					sizeFlow.emit(layout.size)
				}
			}
	) {
		val canvasWidth = size.width
		val canvasHeight = size.height
		
		frameTime?.let { frameTime ->
			for(particle in particleList) {
				val state = particle.getState(frameTime, canvasWidth, canvasHeight, particleSpeed.toPx())
				
				drawCircle(
					color = particleColor,
					radius = particleRadius.toPx(),
					center = Offset(state.x, state.y),
					alpha = state.alpha
				)
			}
		}
	}
}

private data class ParticleState(
	val x: Float,
	val y: Float,
	val alpha: Float,
	val lifetime: Float
)

private class Particle(val creationTime: Long) {
	val x: Float
	val y: Float
	
	val velX: Float
	val velY: Float
	
	init {
		//Pick a random location
		x = Random.nextFloat()
		y = Random.nextFloat()
		
		//Pick a new velocity
		val direction = Random.nextFloat() * PI.toFloat() * 2F
		velX = sin(direction)
		velY = cos(direction)
	}
	
	/**
	 * Gets if this particle should be removed
	 */
	fun isDead(time: Long) = time > creationTime + particleLifetime
	
	/**
	 * Gets the location of this particle at the current point in time
	 */
	fun getState(time: Long, canvasWidth: Float, canvasHeight: Float, speed: Float): ParticleState {
		val lifetime = (time - creationTime).toFloat() / particleLifetime.toFloat()
		
		val alpha = lifetime.coerceIn(0F, 1F).let { progress ->
			if(progress < 0.1F) {
				progress * 10F
			} else {
				1F - ((progress - 0.1F) * (10F / 9F))
			}
		}
		
		return ParticleState(
			x = (x * canvasWidth) + (velX * lifetime * speed),
			y = (y * canvasHeight) + (velY * lifetime * speed),
			alpha = alpha,
			lifetime = lifetime
		)
	}
}

@OptIn(FlowPreview::class)
@Composable
fun rememberInvisibleInkState(enable: Boolean): InvisibleInkState {
	val invisibleInkRevealFlow = remember { MutableSharedFlow<Unit>() }
	var invisibleInkReveal by remember { mutableStateOf(false) }
	
	LaunchedEffect(invisibleInkRevealFlow) {
		invisibleInkRevealFlow
			.onEach { invisibleInkReveal = true }
			.debounce(InvisibleInkConstants.timeRevealStay)
			.collect { invisibleInkReveal = false }
	}
	
	val scope = rememberCoroutineScope()
	fun triggerReveal() {
		scope.launch {
			invisibleInkRevealFlow.emit(Unit)
		}
	}
	
	val contentAlpha by animateFloatAsState(
		targetValue = if(!enable || invisibleInkReveal) 1F else 0F,
		animationSpec = InvisibleInkConstants.animationSpec
	)
	
	return InvisibleInkState(
		isRevealed = invisibleInkReveal,
		reveal = ::triggerReveal,
		contentAlpha = contentAlpha
	)
}

@Immutable
data class InvisibleInkState(
	val isRevealed: Boolean,
	val reveal: () -> Unit,
	val contentAlpha: Float
)

object InvisibleInkConstants {
	const val timeRevealTransition = 500 //0.5 seconds
	const val timeRevealStay = 9L * 1000 //9 seconds
	
	val animationSpec = tween<Float>(durationMillis = timeRevealTransition, easing = LinearEasing)
	val enterTransition = fadeIn(animationSpec = animationSpec)
	val exitTransition = fadeOut(animationSpec = animationSpec)
}
