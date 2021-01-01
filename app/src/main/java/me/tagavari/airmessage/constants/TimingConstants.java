package me.tagavari.airmessage.constants;

public class TimingConstants {
	//Message burst - Sending single messages one after the other
	public static final long conversationBurstTimeMillis = 30 * 1000; //30 seconds
	
	//Message session - A conversation session, where conversation participants are active
	public static final long conversationSessionTimeMillis = 5 * 60 * 1000; //5 minutes
	
	//Just now - A message sent just now
	public static final long conversationJustNowTimeMillis = 60 * 1000; //1 minute
}