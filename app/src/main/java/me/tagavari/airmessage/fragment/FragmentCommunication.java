package me.tagavari.airmessage.fragment;

import androidx.fragment.app.Fragment;

public class FragmentCommunication<A> extends Fragment {
	A callback;
	
	public void setCallback(A callback) {
		this.callback = callback;
	}
}