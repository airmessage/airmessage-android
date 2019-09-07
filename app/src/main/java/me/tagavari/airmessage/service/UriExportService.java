package me.tagavari.airmessage.service;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.Nullable;

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
	private final Handler handler;
	
	public UriExportService() {
		super("File export service");
		handler = new Handler();
	}
	
	@Override
	protected void onHandleIntent(@Nullable Intent intent) {
		if(intent == null) return;
		
		//Getting the file
		File sourceFile = (File) intent.getSerializableExtra(Constants.intentParamData);
		if(!sourceFile.exists()) {
			handler.post(() -> Toast.makeText(this, R.string.message_fileexport_fail, Toast.LENGTH_SHORT).show());
			return;
		}
		Uri targetUri = intent.getParcelableExtra(Constants.intentParamTarget);
		
		//Writing the file
		try(InputStream in = new FileInputStream(sourceFile); OutputStream out = getApplication().getContentResolver().openOutputStream(targetUri)) {
			byte[] buf = new byte[1024];
			int len;
			while((len = in.read(buf)) > 0) out.write(buf, 0, len);
		} catch(IOException exception) {
			exception.printStackTrace();
			handler.post(() -> Toast.makeText(this, R.string.message_fileexport_fail, Toast.LENGTH_SHORT).show());
			return;
		}
		
		//Displaying a toast
		handler.post(() -> Toast.makeText(this, R.string.message_fileexport_success, Toast.LENGTH_SHORT).show());
		
		/* handler.post(() -> {
			//Telling the download manager
			DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
			manager.addCompletedDownload(targetFile.getName(), "File copied from AirMessage chat", true, Constants.getMimeType(targetFile), targetFile.getPath(), targetFile.length(), true);
			
			//Displaying a toast
			Toast.makeText(this, R.string.message_fileexport_success, Toast.LENGTH_SHORT).show();
		}); */
	}
}