package me.tagavari.airmessage.common.helper

object SendStyleHelper {
	const val appleSendStyleBubbleSlam = "com.apple.MobileSMS.expressivesend.impact"
	const val appleSendStyleBubbleLoud = "com.apple.MobileSMS.expressivesend.loud"
	const val appleSendStyleBubbleGentle = "com.apple.MobileSMS.expressivesend.gentle"
	const val appleSendStyleBubbleInvisibleInk = "com.apple.MobileSMS.expressivesend.invisibleink"
	const val appleSendStyleScrnEcho = "com.apple.messages.effect.CKEchoEffect"
	const val appleSendStyleScrnSpotlight = "com.apple.messages.effect.CKSpotlightEffect"
	const val appleSendStyleScrnBalloons = "com.apple.messages.effect.CKHappyBirthdayEffect"
	const val appleSendStyleScrnConfetti = "com.apple.messages.effect.CKConfettiEffect"
	const val appleSendStyleScrnLove = "com.apple.messages.effect.CKHeartEffect"
	const val appleSendStyleScrnLasers = "com.apple.messages.effect.CKLasersEffect"
	const val appleSendStyleScrnFireworks = "com.apple.messages.effect.CKFireworksEffect"
	const val appleSendStyleScrnShootingStar = "com.apple.messages.effect.CKShootingStarEffect"
	const val appleSendStyleScrnCelebration = "com.apple.messages.effect.CKSparklesEffect"
	const val invisibleInkBlurRadius = 2
	const val invisibleInkBlurSampling = 80
	
	private val listScreenEffect = listOf(appleSendStyleScrnEcho, appleSendStyleScrnSpotlight, appleSendStyleScrnBalloons, appleSendStyleScrnConfetti, appleSendStyleScrnLove, appleSendStyleScrnLasers, appleSendStyleScrnFireworks, appleSendStyleScrnShootingStar, appleSendStyleScrnCelebration)
	private val listAnimatedEffect = listOf(appleSendStyleBubbleSlam, appleSendStyleBubbleLoud, appleSendStyleBubbleGentle)
	private val listPassiveEffect = listOf(appleSendStyleBubbleInvisibleInk)
	
	/**
	 * Gets if the provided effect string is a valid iMessage effect
	 */
	fun validateEffect(effect: String): Boolean {
		return validateScreenEffect(effect) || validateAnimatedBubbleEffect(effect) || validatePassiveBubbleEffect(effect)
	}
	
	/**
	 * Gets if the provided effect string is a valid screen effect
	 */
	@JvmStatic
	fun validateScreenEffect(effect: String) = listScreenEffect.contains(effect)
	
	/**
	 * Gets if the provided effect string is a valid animated bubble effect (that is played when the message is sent)
	 */
	@JvmStatic
	fun validateAnimatedBubbleEffect(effect: String) = listAnimatedEffect.contains(effect)
	
	/**
	 * Gets if the provided effect string is a valid passive bubble effect (that continuously affects the bubble's appearance)
	 */
	@JvmStatic
	fun validatePassiveBubbleEffect(effect: String) = listPassiveEffect.contains(effect)
}