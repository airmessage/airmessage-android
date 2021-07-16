package me.tagavari.airmessage.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.helper.ExternalStorageHelper;

public class CrashReport extends AppCompatActivity {
	//Creating the constants
	public static final String PARAM_STACKTRACE = "stacktrace";
	
	private static final int activityResultSaveFileSAF = 0;
	
	//Creating the values
	private String stackTrace;
	private boolean isInSAF = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Setting the content view
		setContentView(R.layout.activity_crashreport);
		
		//Getting the stack trace
		stackTrace = getIntent().getStringExtra(PARAM_STACKTRACE);
		
		//Setting the stack trace text
		TextView textViewStackTrace = findViewById(R.id.label_stacktrace);
		textViewStackTrace.setText(stackTrace);
		textViewStackTrace.setMovementMethod(new ScrollingMovementMethod());
		
		//For some reason this is needed to enable text selection
		textViewStackTrace.setTextIsSelectable(false);
		textViewStackTrace.setTextIsSelectable(true);
		
		findViewById(R.id.button_copy).setOnClickListener(this::buttonCopy);
		findViewById(R.id.button_export).setOnClickListener(this::buttonExport);
		findViewById(R.id.button_restart).setOnClickListener(this::buttonRestart);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		//if(!isInSAF) finish();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == activityResultSaveFileSAF) {
			isInSAF = false;
			if(resultCode == RESULT_OK) ExternalStorageHelper.exportText(this, stackTrace, data.getData());
		}
	}
	
	private void buttonCopy(View view) {
		//Getting the clipboard manager
		ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		
		//Applying the clip data
		clipboardManager.setPrimaryClip(ClipData.newPlainText("Crash report stack trace", stackTrace));
		
		//Showing a confirmation toast
		Toast.makeText(this, R.string.message_textcopied, Toast.LENGTH_SHORT).show();
	}
	
	private void buttonExport(View view) {
		isInSAF = true;
		ExternalStorageHelper.createFileSAF(this, activityResultSaveFileSAF, "text/plain", "stacktrace.txt");
	}
	
	private void buttonRestart(View view) {
		startActivity(new Intent(this, Conversations.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		finish();
	}
}