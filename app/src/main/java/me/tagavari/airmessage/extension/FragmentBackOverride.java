package me.tagavari.airmessage.extension;

public interface FragmentBackOverride {
	/**
	 * Handle back button presses
	 * @return TRUE if the event is consumed by this listener
	 */
	boolean onBackPressed();
}