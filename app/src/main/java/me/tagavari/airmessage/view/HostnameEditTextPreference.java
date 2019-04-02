package me.tagavari.airmessage.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;
import me.tagavari.airmessage.Constants;

public class HostnameEditTextPreference extends EditTextPreference {
	public HostnameEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}
	
	public HostnameEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	
	public HostnameEditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public HostnameEditTextPreference(Context context) {
		super(context);
	}
	
	public static class HostnameEditTextPreferenceDialog extends EditTextPreferenceDialogFragmentCompat {
		private Button positiveButton;
		
		public static HostnameEditTextPreferenceDialog newInstance(String key) {
			final HostnameEditTextPreferenceDialog fragment = new HostnameEditTextPreferenceDialog();
			final Bundle b = new Bundle(1);
			b.putString(ARG_KEY, key);
			fragment.setArguments(b);
			return fragment;
		}
		
		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Dialog dialog =super.onCreateDialog(savedInstanceState);
			dialog.setOnShowListener(dialog1 -> positiveButton = ((AlertDialog) dialog1).getButton(AlertDialog.BUTTON_POSITIVE));
			return dialog;
		}
		
		@Override
		protected void onBindDialogView(View view) {
			super.onBindDialogView(view);
			
			EditText editText = view.findViewById(android.R.id.edit);
			
			editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			editText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				
				}
				
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					positiveButton.setEnabled(Constants.regExValidAddress.matcher(s).find());
				}
				
				@Override
				public void afterTextChanged(Editable s) {
				
				}
			});
		}
	}
}