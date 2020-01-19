package me.tagavari.airmessage.connection.request;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.core.util.Consumer;

import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.util.Constants;

public class FileDownloadRequest {
	//Creating the callbacks
	private Callbacks callbacks;
	private final Consumer<FileDownloadRequest> deregistrationListener;
	
	//Creating the request values
	private final short requestID;
	private final String attachmentGUID;
	private final long attachmentID;
	private final String fileName;
	private long fileSize = 0;
	//private List<FileDownloadRequest> removalList;
	
	public FileDownloadRequest(Callbacks callbacks, Consumer<FileDownloadRequest> deregistrationListener, short requestID, long attachmentID, String attachmentGUID, String fileName) {
		//Setting the callbacks
		this.callbacks = callbacks;
		this.deregistrationListener = deregistrationListener;
		
		//Setting the request values
		this.requestID = requestID;
		this.attachmentID = attachmentID;
		this.attachmentGUID = attachmentGUID;
		this.fileName = fileName;
	}
	
	private static final long timeoutDelay = 20 * 1000; //20-second delay
	private final Handler handler = new Handler(Looper.getMainLooper());
	
	public void startTimer() {
		handler.postDelayed(timeoutRunnable, timeoutDelay);
	}
	
	public void stopTimer(boolean restart) {
		handler.removeCallbacks(timeoutRunnable);
		if(restart) handler.postDelayed(timeoutRunnable, timeoutDelay);
	}
	
	public void failDownload(int errorCode) {
		//Stopping the timer
		stopTimer(false);
		
		//Stopping the thread
		if(attachmentWriterThread != null) attachmentWriterThread.stopThread();
		
		//Telling the callback listeners
		callbacks.onFail(errorCode);
		
		//Deregistering the request
		deregistrationListener.accept(this);
	}
	
	public void finishDownload(File file) {
		stopTimer(false);
		callbacks.onFinish(file);
		deregistrationListener.accept(this);
	}
	
	public void setFileSize(long value) {
		fileSize = value;
	}
	
	public void onResponseReceived() {
		if(!isWaiting) return;
		isWaiting = false;
		callbacks.onResponseReceived();
	}
	
	public ProgressStruct getProgress() {
		return new ProgressStruct(isWaiting, lastProgress);
	}
	
	private void updateProgress(float progress) {
		lastProgress = 0;
		callbacks.onProgress(progress);
	}
	
	private AttachmentWriter attachmentWriterThread = null;
	private final Runnable timeoutRunnable = () -> failDownload(Callbacks.errorCodeTimeout);
	private boolean isWaiting = true;
	private int lastIndex = -1;
	private float lastProgress = 0;
	
	public void processFileFragment(Context context, final byte[] compressedBytes, int index, boolean isLast, ConnectionManager.Packager packager) {
		//Setting the state to receiving if it isn't already
		if(isWaiting) {
			isWaiting = false;
			callbacks.onResponseReceived();
		}
		
		//Checking if the index doesn't line up
		if(lastIndex + 1 != index) {
			//Failing the download
			failDownload(Callbacks.errorCodeBadResponse);
			
			//Returning
			return;
		}
		
		//Resetting the timer
		stopTimer(!isLast);
		
		//Setting the last index
		lastIndex = index;
		
		//Checking if there is no save thread
		if(attachmentWriterThread == null) {
			//Creating and starting the attachment writer thread
			attachmentWriterThread = new AttachmentWriter(context.getApplicationContext(), attachmentID, fileName, fileSize, packager);
			attachmentWriterThread.start();
			callbacks.onStart();
		}
		
		//Adding the data struct
		attachmentWriterThread.dataQueue.add(new AttachmentWriterDataStruct(compressedBytes, isLast));
	}
	
	public static class ProgressStruct {
		public final boolean isWaiting;
		public final float progress;
		
		public ProgressStruct(boolean isWaiting, float progress) {
			this.isWaiting = isWaiting;
			this.progress = progress;
		}
	}
	
	private class AttachmentWriter extends Thread {
		//Creating the references
		private final WeakReference<Context> contextReference;
		
		//Creating the queue
		private final BlockingQueue<AttachmentWriterDataStruct> dataQueue = new LinkedBlockingQueue<>();
		
		//Creating the request values
		private final long attachmentID;
		private final String fileName;
		private final long fileSize;
		
		private final ConnectionManager.Packager packager;
		
		//Creating the process values
		private long bytesWritten;
		private boolean isRunning = true;
		
		AttachmentWriter(Context context, long attachmentID, String fileName, long fileSize, ConnectionManager.Packager packager) {
			//Setting the references
			contextReference = new WeakReference<>(context);
			
			//Setting the request values
			this.attachmentID = attachmentID;
			this.fileName = fileName;
			this.fileSize = fileSize;
			
			this.packager = packager;
		}
		
		@Override
		public void run() {
			//Getting the file paths
			File targetFileDir;
			{
				//Getting the context
				Context context = contextReference.get();
				if(context == null) {
					new Handler(Looper.getMainLooper()).post(() -> failDownload(Callbacks.errorCodeReferencesLost));
					return;
				}
				
				targetFileDir = new File(MainApplication.getDownloadDirectory(context), Long.toString(attachmentID));
				if(!targetFileDir.exists()) targetFileDir.mkdir();
				else if(targetFileDir.isFile()) {
					Constants.recursiveDelete(targetFileDir);
					targetFileDir.mkdir();
				}
			}
			
			//Preparing to write to the file
			File targetFile = new File(targetFileDir, fileName);
			try(OutputStream outputStream = new FileOutputStream(targetFile)) {
				while(isRunning) {
					//Getting the data struct
					AttachmentWriterDataStruct dataStruct = dataQueue.poll(timeoutDelay, TimeUnit.MILLISECONDS);
					
					//Skipping the remainder of the iteration if the data struct is invalid
					if(dataStruct == null) continue;
					
					//Decompressing the bytes
					byte[] decompressedBytes = packager.unpackageData(dataStruct.compressedBytes);
					
					//Writing the bytes
					outputStream.write(decompressedBytes);
					
					//Adding to the bytes written
					bytesWritten += decompressedBytes.length;
					
					//Checking if the data is the last group
					if(dataStruct.isLast) {
						//Cleaning the thread
						//cleanThread();
						
						//Updating the database entry
						DatabaseManager.getInstance().updateAttachmentFile(attachmentID, MainApplication.getInstance(), targetFile);
						
						//Updating the state
						new Handler(Looper.getMainLooper()).post(() -> finishDownload(targetFile));
						
						//Returning
						return;
					} else {
						//Updating the progress
						new Handler(Looper.getMainLooper()).post(() -> updateProgress(((float) bytesWritten / fileSize)));
					}
					
					/* //Checking if the thread is still running
					if(isRunning) {
						dataStructsLock.lock();
						try {
							//Waiting for entries to appear
							if(dataStructs.isEmpty()) dataStructsCondition.await(timeoutDelay, TimeUnit.MILLISECONDS);
							
							//Checking if there are still no new items
							if(isRunning && dataStructs.isEmpty()) {
								//Stopping the thread
								isRunning = false;
								
								//Failing the download
								new Handler(Looper.getMainLooper()).post(FileDownloadRequest.this::failDownload);
							}
						} catch(InterruptedException exception) {
							//Stopping the thread
							isRunning = false;
							
							//Returning
							//return;
						} finally {
							dataStructsLock.unlock();
						}
					} */
				}
			} catch(IOException | OutOfMemoryError exception) {
				//Printing the stack trace
				exception.printStackTrace();
				Crashlytics.logException(exception);
				
				//Failing the download
				new Handler(Looper.getMainLooper()).post(() -> failDownload(Callbacks.errorCodeIO));
				
				//Setting the thread as not running
				isRunning = false;
			} catch(InterruptedException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Failing the download
				new Handler(Looper.getMainLooper()).post(() -> failDownload(Callbacks.errorCodeCancelled));
				
				//Setting the thread as not running
				isRunning = false;
			}
			
			//Checking if the thread was stopped
			if(!isRunning) {
				//Cleaning up
				Constants.recursiveDelete(targetFileDir);
			}
		}
		
		void stopThread() {
			isRunning = false;
		}
	}
	
	public static class AttachmentWriterDataStruct {
		final byte[] compressedBytes;
		final boolean isLast;
		
		AttachmentWriterDataStruct(byte[] compressedBytes, boolean isLast) {
			this.compressedBytes = compressedBytes;
			this.isLast = isLast;
		}
	}
	
	public interface Callbacks {
		int errorCodeUnknown = -1;
		int errorCodeCancelled = 0; //Request cancelled
		int errorCodeTimeout = 1; //Request timed out
		int errorCodeBadResponse = 2; //Bad response (packets out of order)
		int errorCodeReferencesLost = 3; //Reference to context lost
		int errorCodeIO = 4; //IO error
		int errorCodeServerNotFound = 5; //Server file GUID not found
		int errorCodeServerNotSaved = 6; //Server file (on disk) not found
		int errorCodeServerUnreadable = 7; //Server no access to file
		int errorCodeServerIO = 8; //Server IO error
		
		void onResponseReceived();
		
		void onStart();
		
		void onProgress(float progress);
		
		void onFinish(File file);
		
		void onFail(int errorCode);
	}
	
	public void setCallbacks(Callbacks callbacks) {
		this.callbacks = callbacks;
	}
	
	public short getRequestID() {
		return requestID;
	}
	
	public String getAttachmentGUID() {
		return attachmentGUID;
	}
	
	public long getAttachmentID() {
		return attachmentID;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public long getFileSize() {
		return fileSize;
	}
}
