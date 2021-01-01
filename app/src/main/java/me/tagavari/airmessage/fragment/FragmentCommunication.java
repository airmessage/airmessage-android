package me.tagavari.airmessage.fragment;

import androidx.fragment.app.Fragment;

public class FragmentCommunication<A> extends Fragment {
	private A callback;
	
	public void setCommunicationsCallback(A callback) {
		this.callback = callback;
	}
	
	protected A getCommunicationsCallback() {
		return callback;
	}
}