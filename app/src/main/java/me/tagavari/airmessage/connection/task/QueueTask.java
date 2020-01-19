package me.tagavari.airmessage.connection.task;

import android.os.Handler;
import android.os.Looper;

public abstract class QueueTask<Progress, Result> {
	//abstract void onPreExecute();
	public abstract Result doInBackground();
	
	public void onPostExecute(Result value) {}
	
	public void publishProgress(Progress progress) {
		new Handler(Looper.getMainLooper()).post(() -> onProgressUpdate(progress));
	}
	
	public void onProgressUpdate(Progress progress) {}
}
