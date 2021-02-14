package me.tagavari.airmessage.extension;

import androidx.annotation.Nullable;

import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.fragment.FragmentCommunication;

public interface FragmentCommunicationNetworkConfig {
	void swapFragment(FragmentCommunication<FragmentCommunicationNetworkConfig> fragment);
	void popStack();
	void launchConversations();
	@Nullable
	ConnectionManager getConnectionManager();
}