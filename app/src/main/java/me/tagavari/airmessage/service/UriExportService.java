package me.tagavari.airmessage.service;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.helper.DataStreamHelper;

/**
 * A service used to export and save attachments to disk
 */
public class UriExportService extends IntentService {
	public static final String intentParamPath = "file";
	public static final String intentParamText = "text";
	public static final String intentParamDestination = "destination";
	
	private final Handler handler;
	
	public UriExportService() {
		super("URI export service");
		handler = new Handler();
	}
	
	@Override
	protected void onHandleIntent(@Nullable Intent intent) {
		if(intent == null) return;
		
		//Getting the data
		InputStream in;
		try {
			if(intent.hasExtra(intentParamPath)) {
				//Getting the file
				File sourceFile = (File) intent.getSerializableExtra(intentParamPath);
				if(!sourceFile.exists()) {
					handler.post(() -> Toast.makeText(this, R.string.message_fileexport_fail, Toast.LENGTH_SHORT).show());
					return;
				}
				
				//Opening the input stream
				in = new FileInputStream(sourceFile);
			} else if(intent.hasExtra(intentParamText)) {
				String sourceText = intent.getStringExtra(intentParamText);
				in = new ByteArrayInputStream(sourceText.getBytes());
			} else {
				handler.post(() -> Toast.makeText(this, R.string.message_fileexport_fail, Toast.LENGTH_SHORT).show());
				return;
			}
		} catch(IOException exception) {
			exception.printStackTrace();
			handler.post(() -> Toast.makeText(this, R.string.message_fileexport_fail, Toast.LENGTH_SHORT).show());
			return;
		}
		
		Uri targetUri = intent.getParcelableExtra(intentParamDestination);
		
		//Writing the file
		try(OutputStream out = getApplication().getContentResolver().openOutputStream(targetUri)) {
			DataStreamHelper.copyStream(in, out);
		} catch(IOException exception) {
			exception.printStackTrace();
			handler.post(() -> Toast.makeText(this, R.string.message_fileexport_fail, Toast.LENGTH_SHORT).show());
			return;
		} finally {
			try {
				in.close();
			} catch(IOException exception) {
				exception.printStackTrace();
			}
		}
		
		//Displaying a toast
		handler.post(() -> Toast.makeText(this, R.string.message_fileexport_success, Toast.LENGTH_SHORT).show());
	}
}