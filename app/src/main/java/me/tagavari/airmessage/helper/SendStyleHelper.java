package me.tagavari.airmessage.helper;

public class SendStyleHelper {
	public static final String appleSendStyleBubbleSlam = "com.apple.MobileSMS.expressivesend.impact";
	public static final String appleSendStyleBubbleLoud = "com.apple.MobileSMS.expressivesend.loud";
	public static final String appleSendStyleBubbleGentle = "com.apple.MobileSMS.expressivesend.gentle";
	public static final String appleSendStyleBubbleInvisibleInk = "com.apple.MobileSMS.expressivesend.invisibleink";
	public static final String appleSendStyleScrnEcho = "com.apple.messages.effect.CKEchoEffect";
	public static final String appleSendStyleScrnSpotlight = "com.apple.messages.effect.CKSpotlightEffect";
	public static final String appleSendStyleScrnBalloons = "com.apple.messages.effect.CKHappyBirthdayEffect";
	public static final String appleSendStyleScrnConfetti = "com.apple.messages.effect.CKConfettiEffect";
	public static final String appleSendStyleScrnLove = "com.apple.messages.effect.CKHeartEffect";
	public static final String appleSendStyleScrnLasers = "com.apple.messages.effect.CKLasersEffect";
	public static final String appleSendStyleScrnFireworks = "com.apple.messages.effect.CKFireworksEffect";
	public static final String appleSendStyleScrnShootingStar = "com.apple.messages.effect.CKShootingStarEffect";
	public static final String appleSendStyleScrnCelebration = "com.apple.messages.effect.CKSparklesEffect";
	
	public static final int invisibleInkBlurRadius = 2;
	public static final int invisibleInkBlurSampling = 80;
	
	/**
	 * Gets if the provided effect string is a valid iMessage effect
	 */
	public static boolean validateEffect(String effect) {
		return validateScreenEffect(effect) || validateAnimatedBubbleEffect(effect) || validatePassiveBubbleEffect(effect);
	}
	
	/**
	 * Gets if the provided effect string is a valid screen effect
	 */
	public static boolean validateScreenEffect(String effect) {
		return appleSendStyleScrnEcho.equals(effect) ||
				appleSendStyleScrnSpotlight.equals(effect) ||
				appleSendStyleScrnBalloons.equals(effect) ||
				appleSendStyleScrnConfetti.equals(effect) ||
				appleSendStyleScrnLove.equals(effect) ||
				appleSendStyleScrnLasers.equals(effect) ||
				appleSendStyleScrnFireworks.equals(effect) ||
				appleSendStyleScrnShootingStar.equals(effect) ||
				appleSendStyleScrnCelebration.equals(effect);
	}
	
	/**
	 * Gets if the provided effect string is a valid animated bubble effect (that is played when the message is sent)
	 */
	public static boolean validateAnimatedBubbleEffect(String effect) {
		return appleSendStyleBubbleSlam.equals(effect) ||
				appleSendStyleBubbleLoud.equals(effect) ||
				appleSendStyleBubbleGentle.equals(effect);
	}
	
	/**
	 * Gets if the provided effect string is a valid passive bubble effect (that continuously affects the bubble's appearance)
	 */
	public static boolean validatePassiveBubbleEffect(String effect) {
		return appleSendStyleBubbleInvisibleInk.equals(effect);
	}
}