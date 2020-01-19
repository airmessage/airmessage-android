package me.tagavari.airmessage.connection.thread;

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;

import me.tagavari.airmessage.connection.task.QueueTask;
import me.tagavari.airmessage.service.ConnectionService;

public class MessageProcessingThread extends Thread {
	private final BlockingQueue<QueueTask<?, ?>> queue;
	private final Runnable finishListener;
	
	public MessageProcessingThread(BlockingQueue<QueueTask<?, ?>> queue, Runnable finishListener) {
		this.queue = queue;
		this.finishListener = finishListener;
	}
	
	@Override
	public void run() {
		//Creating the handler
		Handler handler = new Handler(Looper.getMainLooper());
		
		//Looping while the thread is alive
		ConnectionService service = null;
		QueueTask<?, ?> task;
		while(!isInterrupted() && (task = pushQueue()) != null) {
			//Clearing the reference to the service
			service = null;
			
			//Running the task
			runTask(task, handler);
		}
		
		//Telling the service that the thread is finished
		finishListener.run();
	}
	
	private QueueTask<?, ?> pushQueue() {
		return queue.poll();
	}
	
	private <Result> void runTask(QueueTask<?, Result> task, Handler handler) {
		Result value = task.doInBackground();
		handler.post(() -> task.onPostExecute(value));
	}
}
