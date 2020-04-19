package me.tagavari.airmessage.fragment;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.extension.FragmentBackOverride;
import me.tagavari.airmessage.extension.FragmentCommunicationSwap;
import me.tagavari.airmessage.util.Constants;

public class FragmentOnboardingConnect extends FragmentCommunication<FragmentCommunicationSwap> implements FragmentBackOverride {
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_onboarding_connect, container, false);
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		user.getIdToken(false).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
			@Override
			public void onComplete(@NonNull Task<GetTokenResult> task) {
				if(task.isSuccessful()) {
					task.getResult().getToken();
				} else {
					task.getException();
				}
			}
		});
		
		//Setting up the toolbar
		configureToolbar(view.findViewById(R.id.toolbar));
	}
	
	@Override
	public boolean onBackPressed() {
		promptExit();
		
		return true;
	}
	
	private void promptExit() {
		new MaterialAlertDialogBuilder(requireContext())
				.setMessage(R.string.dialog_signout_message)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(R.string.action_signout, (dialog, which) -> signOut())
				.create().show();
	}
	
	private void configureToolbar(Toolbar toolbar) {
		int colorSecondary = Constants.resolveColorAttr(getContext(), android.R.attr.textColorSecondary);
		{
			Drawable drawable = getResources().getDrawable(R.drawable.close_circle, null);
			drawable.setColorFilter(colorSecondary, PorterDuff.Mode.SRC_IN);
			toolbar.setNavigationIcon(drawable);
		}
		
		toolbar.setNavigationOnClickListener(navigationView -> promptExit());
	}
	
	private void signOut() {
		//Signing out
		FirebaseAuth.getInstance().signOut();
		
		//Returning to the previous stack
		callback.popStack();
	}
}