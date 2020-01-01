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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.util.Constants;

/**
 * A service used to export and save attachments to disk
 */
public class UriExportService extends IntentService {
	public static final String PARAM_INPUTFILE = "input_file";
	public static final String PARAM_INPUTTEXT = "input_text";
	public static final String PARAM_OUTPUTURI = "output_uri";
	
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
			if(intent.hasExtra(PARAM_INPUTFILE)) {
				//Getting the file
				File sourceFile = (File) intent.getSerializableExtra(PARAM_INPUTFILE);
				if(!sourceFile.exists()) {
					handler.post(() -> Toast.makeText(this, R.string.message_fileexport_fail, Toast.LENGTH_SHORT).show());
					return;
				}
				
				//Opening the input stream
				in = new FileInputStream(sourceFile);
			} else if(intent.hasExtra(PARAM_INPUTTEXT)) {
				String sourceText = intent.getStringExtra(PARAM_INPUTTEXT);
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
		
		Uri targetUri = intent.getParcelableExtra(PARAM_OUTPUTURI);
		
		//Writing the file
		try(OutputStream out = getApplication().getContentResolver().openOutputStream(targetUri)) {
			byte[] buf = new byte[1024];
			int len;
			while((len = in.read(buf)) > 0) out.write(buf, 0, len);
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