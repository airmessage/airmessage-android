package me.tagavari.airmessage.extension;

import me.tagavari.airmessage.fragment.FragmentCommunication;

public interface FragmentCommunicationSwap {
	void swapFragment(FragmentCommunication<FragmentCommunicationSwap> fragment);
	void popStack();
	void launchConversations();
}