package me.tagavari.airmessage.task;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import me.tagavari.airmessage.helper.FileHelper;
import me.tagavari.airmessage.messaging.FileLinked;
import me.tagavari.airmessage.util.Union;

public class FileQueueTask {
	public static FileLinked uriToFileLinkedSync(Context context, Uri uri) {
		String displayName = "file";
		long fileSize = -1;
		
		//Getting the type
		String fileType = FileHelper.getMimeType(context, uri);
		
		//Querying the URI
		try(Cursor cursor = context.getContentResolver().query(uri, null, null, null, null, null)) {
			if(cursor != null && cursor.moveToFirst()) {
				int columnDisplayName = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
				if(!cursor.isNull(columnDisplayName)) displayName = cursor.getString(columnDisplayName);
				
				int columnFileSize = cursor.getColumnIndex(OpenableColumns.SIZE);
				if(!cursor.isNull(columnDisplayName)) fileSize = cursor.getLong(columnFileSize);
			}
		}
		
		return new FileLinked(Union.ofB(uri), displayName, fileSize, fileType);
	}
}