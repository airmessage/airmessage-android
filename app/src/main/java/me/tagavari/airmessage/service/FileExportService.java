package me.tagavari.airmessage.service;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
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
import me.tagavari.airmessage.helper.DataStreamHelper;
import me.tagavari.airmessage.helper.FileHelper;

/**
 * A service used to export and save attachments to disk
 */
public class FileExportService extends IntentService {
	public static final String intentParamPath = "path";
	
	private final Handler handler;
	
	public FileExportService() {
		super("File export service");
		handler = new Handler();
	}
	
	@Override
	protected void onHandleIntent(@Nullable Intent intent) {
		if(intent == null) return;
		
		//Getting the file
		File sourceFile = new File(intent.getStringExtra(intentParamPath));
		if(!sourceFile.exists()) {
			handler.post(() -> Toast.makeText(this, R.string.message_fileexport_fail, Toast.LENGTH_SHORT).show());
			return;
		}
		File targetFile = FileHelper.findFreeFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), sourceFile.getName(), true);
		
		//Writing the file
		try(InputStream in = new FileInputStream(sourceFile); OutputStream out = new FileOutputStream(targetFile)) {
			DataStreamHelper.copyStream(in, out);
		} catch(IOException exception) {
			exception.printStackTrace();
			handler.post(() -> Toast.makeText(this, R.string.message_fileexport_fail, Toast.LENGTH_SHORT).show());
			return;
		}
		
		handler.post(() -> {
			//Telling the download manager
			DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
			manager.addCompletedDownload(targetFile.getName(), "File copied from AirMessage chat", true, FileHelper.getMimeType(targetFile), targetFile.getPath(), targetFile.length(), true);
			
			//Displaying a toast
			Toast.makeText(this, R.string.message_fileexport_success, Toast.LENGTH_SHORT).show();
		});
	}
}