package me.tagavari.airmessage.connection.thread;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;

import androidx.core.util.Consumer;
import androidx.exifinterface.media.ExifInterface;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;

import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.connection.CommunicationsManager;
import me.tagavari.airmessage.connection.ConnectionManager;
import me.tagavari.airmessage.connection.request.MessageResponseManager;
import me.tagavari.airmessage.service.ConnectionService;
import me.tagavari.airmessage.connection.request.FileProcessingRequest;
import me.tagavari.airmessage.connection.request.FilePushRequest;
import me.tagavari.airmessage.connection.request.FileRemovalRequest;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.messaging.DraftFile;
import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.util.DataTransformUtils;

public class FileProcessingThread extends Thread {
	//Creating the constants
	private final float copyProgressValue = 0.2F;
	
	//Creating the reference values
	private final WeakReference<Context> contextReference;
	private final BlockingQueue<FileProcessingRequest> queue;
	private final Consumer<FileProcessingRequest> processListener; //When a new request has started processing
	private final Runnable finishListener;
	
	//Creating the other values
	private final Handler handler = new Handler(Looper.getMainLooper());
	
	public FileProcessingThread(Context context, BlockingQueue<FileProcessingRequest> queue, Consumer<FileProcessingRequest> processListener, Runnable finishListener) {
		contextReference = new WeakReference<>(context);
		this.queue = queue;
		this.processListener = processListener;
		this.finishListener = finishListener;
	}
	
	@Override
	public void run() {
		//Looping while there are requests in the queue
		FileProcessingRequest request;
		while(!isInterrupted() && (request = pushQueue()) != null) {
			//Getting the callbacks
			FileProcessingRequest.Callbacks finalCallbacks = request.getCallbacks();
			
			//Telling the callbacks that the process has started
			handler.post(finalCallbacks.onStart);
			request.setInProcessing(true);
			
			//Handling the request
			if(request instanceof FilePushRequest) handleFilePushRequest((FilePushRequest) request);
			else if(request instanceof FileRemovalRequest) handleFileRemovalRequest((FileRemovalRequest) request);
		}
		
		//Calling the finish listener
		finishListener.run();
	}
	
	private FileProcessingRequest pushQueue() {
		FileProcessingRequest request = queue.poll();
		processListener.accept(request);
		return request;
	}
	
	private void handleFileRemovalRequest(FileRemovalRequest request) {
		//Removing the file
		ConnectionManager.removeDraftFileSync(request.getDraftFile(), request.getUpdateTime());
		
		//Finishing the request
		request.setInProcessing(false);
		handler.post(request.getCallbacks().onRemovalFinish);
	}
	
	private void handleFilePushRequest(FilePushRequest request) {
		//Copying the request info
		boolean requestUpload = request.isUploadRequested();
		FileProcessingRequest.Callbacks finalCallbacks = request.getCallbacks();
		
		//Checking if the request has no send file (and only has an external reference that should be copied)
		boolean fileNeedsCopy = request.getState() == FilePushRequest.stateLinked;
		if(fileNeedsCopy) {
			//Copying the file to local storage
			boolean result = copyFilePush(request);
			if(!result) return;
		}
		//Checking if the request has a file readily available to use
		else if(request.getSendFile() != null) {
			//Checking if compression is required
			if(request.getCompressionTarget() != -1 &&
			   request.getSendFile().length() > request.getCompressionTarget()) {
				//Checking if compression is not applicable
				if(!DataTransformUtils.isCompressable(request.getFileType())) {
					//Calling the fail method
					request.setInProcessing(false);
					handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalFileTooLarge, null));
					
					//Returning
					return;
				}
				
				//Overwriting the compressed file
				try(FileInputStream fileInputStream = new FileInputStream(request.getSendFile())) {
					byte[] compressedData = compressFileInputStream(fileInputStream, request.getFileType(), request.getCompressionTarget());
					fileInputStream.close();
					try(FileOutputStream fileOutputStream = new FileOutputStream(request.getSendFile(), false)) {
						fileOutputStream.write(compressedData);
					}
				} catch(IOException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Calling the fail method
					request.setInProcessing(false);
					handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalIO, null));
					
					//Returning
					return;
				}
				
				//Clearing the compression target (no need to compress anymore)
				request.clearCompressionTarget();
			}
		}
		//This attachment has no data?
		else {
			//Calling the fail method
			request.setInProcessing(false);
			handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalInvalidContent, null));
			
			//Returning
			return;
		}
		
		//Checking if an upload has been requested
		if(requestUpload) {
			//Checking if the state is queued
			if(request.getState() == FilePushRequest.stateQueued) {
				//Moving the file from queue to upload
				boolean result = moveFileQueueUpload(request);
				if(!result) return;
			}
			
			//Checking if the request has a custom upload handler
			if(request.getCustomUploadHandler() != null) {
				//Marking the request as completed
				request.setInProcessing(false);
				request.setState(FilePushRequest.stateFinished);
				
				//Passing the load onto the custom upload handler
				request.getCustomUploadHandler().accept(request);
				
				//Returning
				return;
			} else {
				//Handling the request via AirMessage Bridge
				boolean result = uploadFileAMBridge(request, fileNeedsCopy);
				if(result) return;
			}
		}
		
		//Setting the processing flag to false
		request.setInProcessing(false);
	}
	
	private boolean copyFilePush(FilePushRequest request) {
		//Copying the request info
		boolean requestUpload = request.isUploadRequested();
		FileProcessingRequest.Callbacks finalCallbacks = request.getCallbacks();
		
		//Getting the context
		Context context = contextReference.get();
		if(context == null) {
			//Calling the fail method
			request.setInProcessing(false);
			handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalReferences, null));
			
			//Returning
			return false;
		}
		
		//Creating the values
		String fileName = null;
		InputStream inputStream = null;
		File originalFile = request.getSendFile();
		
		try {
			//Checking if the request is using a URI
			if(request.getSendUri() != null) {
				//Verifying the file size
				try(Cursor cursor = context.getContentResolver().query(request.getSendUri(), null, null, null, null)) {
					if(cursor != null && cursor.moveToFirst()) {
						long fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
						
						//Checking if the file size is too large to send
						if(fileSize > ConnectionManager.largestFileSize) {
							//Calling the fail method
							request.setInProcessing(false);
							handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalFileTooLarge, null));
							
							//Returning
							return false;
						}
					}
				} catch(SecurityException | IllegalArgumentException exception) {
					exception.printStackTrace();
				}
				
				//Finding a valid file
				fileName = request.getFileName();
				if(fileName == null) {
					fileName = Constants.getUriName(context, request.getSendUri());
					if(fileName == null) fileName = Constants.defaultFileName;
				}
				
				//Opening the input stream
				try {
					inputStream = context.getContentResolver().openInputStream(request.getSendUri());
				} catch(IllegalArgumentException | SecurityException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Calling the fail method
					request.setInProcessing(false);
					handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalIO, Constants.exceptionToString(exception)));
					
					//Returning
					return false;
				}
			}
			//Otherwise checking if the request is using a file
			else if(request.getSendFile() != null) {
				//Getting the target folder for the file
				File targetFolder = requestUpload ? MainApplication.getUploadDirectory(context) : MainApplication.getDraftDirectory(context);
				
				//Checking if the file is already in the target directory
				if(Constants.checkFileParent(targetFolder, request.getSendFile())) {
					//Verifying the file size
					long fileSize = request.getSendFile().length();
					if(fileSize > ConnectionManager.largestFileSize) {
						//Calling the fail method
						request.setInProcessing(false);
						handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalFileTooLarge, null));
						
						//Returning
						return false;
					}
					
					//Checking if compression is required
					if(request.getCompressionTarget() != -1 &&
					   request.getSendFile().length() > request.getCompressionTarget()) {
						//Checking if compression is not applicable
						if(!DataTransformUtils.isCompressable(request.getFileType())) {
							//Calling the fail method
							request.setInProcessing(false);
							handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalFileTooLarge, null));
							
							//Returning
							return false;
						}
						
						//Overwriting the compressed file
						try(FileInputStream fileInputStream = new FileInputStream(request.getSendFile())) {
							byte[] compressedData = compressFileInputStream(fileInputStream, request.getFileType(), request.getCompressionTarget());
							fileInputStream.close();
							try(FileOutputStream fileOutputStream = new FileOutputStream(request.getSendFile(), false)) {
								fileOutputStream.write(compressedData);
							}
						} catch(IOException exception) {
							//Printing the stack trace
							exception.printStackTrace();
							
							//Calling the fail method
							request.setInProcessing(false);
							handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalIO, null));
							
							//Returning
							return false;
						}
						
						//Clearing the compression target (no need to compress anymore)
						request.clearCompressionTarget();
					}
				} else {
					//Getting the file name
					fileName = request.getFileName();
					if(fileName == null) fileName = request.getSendFile().getName();
					//if(fileName == null) fileName = Constants.defaultFileName;
					
					//Verifying the file size
					long fileSize = request.getSendFile().length();
					if(fileSize > ConnectionManager.largestFileSize) {
						//Calling the fail method
						request.setInProcessing(false);
						handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalFileTooLarge, null));
						
						//Returning
						return false;
					}
					
					//Opening the input stream
					inputStream = new BufferedInputStream(new FileInputStream(request.getSendFile()));
				}
			} else {
				//Calling the fail method
				request.setInProcessing(false);
				handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalInternal, Constants.exceptionToString(new IllegalArgumentException("No URI or file reference available to send"))));
				
				//Returning
				return false;
			}
		} catch(FileNotFoundException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Calling the fail method
			request.setInProcessing(false);
			handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalIO, Constants.exceptionToString(exception)));
			
			//Returning
			return false;
		}
		
		//Preparing to copy the file
		if(inputStream != null) {
			//Getting the target file
			File targetFile = requestUpload ?
							  MainApplication.getUploadTarget(context, fileName) :
							  MainApplication.getDraftTarget(context, request.getConversationID(), fileName);
			
			try(OutputStream outputStream = new FileOutputStream(targetFile)) {
				//Clearing the reference to the context
				context = null;
				
				//Checking if compression is required
				if(request.getCompressionTarget() != -1) {
					//Reading the file data
					ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
					DataTransformUtils.copyStream(inputStream, byteArrayOutputStream);
					byte[] fileBytes = byteArrayOutputStream.toByteArray();
					
					//Checking if the file needs to be compressed
					if(fileBytes.length > request.getCompressionTarget()) {
						//Checking if compression is not applicable
						if(!DataTransformUtils.isCompressable(request.getFileType())) {
							//Calling the fail method
							request.setInProcessing(false);
							handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalFileTooLarge, null));
							
							//Returning
							return false;
						}
						
						//Compressing the file
						byte[] compressedBytes = DataTransformUtils.compressFile(fileBytes, request.getFileType(), request.getCompressionTarget());
						
						//Replacing the input stream
						inputStream.close();
						inputStream = new ByteArrayInputStream(compressedBytes);
					} else {
						//Resetting the input stream with the data in memory
						inputStream.close();
						inputStream = new ByteArrayInputStream(fileBytes);
					}
				}
				
				//Preparing to read the file
				long totalLength = inputStream.available();
				byte[] buffer = new byte[DataTransformUtils.standardBuffer];
				int bytesRead;
				long totalBytesRead = 0;
				
				//Looping while there is data to read
				while((bytesRead = inputStream.read(buffer)) != -1) {
					//Writing the data to the output stream
					outputStream.write(buffer, 0, bytesRead);
					
					//Adding to the total bytes read
					totalBytesRead += bytesRead;
					
					//Updating the progress
					final long finalTotalBytesRead = totalBytesRead;
					handler.post(() -> finalCallbacks.onUploadProgress.accept((float) ((double) finalTotalBytesRead / (double) totalLength * copyProgressValue)));
				}
				
				//Flushing the output stream
				outputStream.flush();
				
				//Setting the send file
				request.setSendFile(targetFile);
				
				//Clearing the compression target (no need to compress anymore)
				request.clearCompressionTarget();
			} catch(IOException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Deleting the target file
				targetFile.delete();
				targetFile.getParentFile().delete();
				
				//Calling the fail method
				request.setInProcessing(false);
				handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalIO, Constants.exceptionToString(exception)));
				
				//Returning
				return false;
			} finally {
				try {
					inputStream.close();
				} catch(IOException exception) {
					exception.printStackTrace();
				}
			}
			
			//Setting the request's file
			request.setSendFile(targetFile);
		}
		
		//Checking if this file is going to be uploaded
		if(requestUpload) {
			//Calling the listener
			handler.post(() -> finalCallbacks.onAttachmentPreparationFinished.accept(request.getSendFile()));
			
			//Setting the state
			request.setState(FilePushRequest.stateAttached);
		} else {
			//Writing the draft file to the database
			DraftFile draft = DatabaseManager.getInstance().addDraftReference(request.getConversationID(), request.getSendFile(), request.getSendFile().getName(), request.getSendFile().length(), request.getFileType(), request.getFileModificationDate(), originalFile, request.getSendUri(), request.getUpdateTime());
			
			//If the insertion failed
			if(draft == null) {
				//Deleting the target file
				request.getSendFile().delete();
				request.getSendFile().getParentFile().delete();
				request.setSendFile(null);
				
				//Calling the fail method
				request.setInProcessing(false);
				handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalIO, null));
				
				//Returning
				return false;
			}
			
			//Updating the draft information
			request.setDraftID(draft.getLocalID());
			
			//Completing the request
			request.setInProcessing(false);
			handler.post(() -> finalCallbacks.onDraftPreparationFinished.accept(request.getSendFile(), draft));
			
			//Setting the state
			request.setState(FilePushRequest.stateQueued);
		}
		
		//Returning true
		return true;
	}
	
	private boolean moveFileQueueUpload(FilePushRequest request) {
		//Copying the request info
		FileProcessingRequest.Callbacks finalCallbacks = request.getCallbacks();
		
		//Getting the context
		Context context = contextReference.get();
		if(context == null) {
			//Calling the fail method
			request.setInProcessing(false);
			handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalReferences, null));
			
			//Returning
			return false;
		}
		
		//Moving the file
		File targetFile = MainApplication.getUploadTarget(context, request.getSendFile().getName());
		boolean result = request.getSendFile().renameTo(targetFile);
		if(!result) {
			//Calling the fail method
			request.setInProcessing(false);
			handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalIO, null));
			
			//Returning
			return false;
		}
		
		//Deleting the parent directory (since each draft file is stored in its own folder to prevent name collisions)
		request.getSendFile().getParentFile().delete();
		
		//Updating the file reference
		request.setSendFile(targetFile);
		
		//Removing the draft reference from the database
		if(request.getDraftID() != -1) {
			DatabaseManager.getInstance().removeDraftReference(request.getDraftID(), -1);
			request.setDraftID(-1);
		}
		
		//Updating the database entry
		if(request.getAttachmentID() != -1)
			DatabaseManager.getInstance().updateAttachmentFile(request.getAttachmentID(), MainApplication.getInstance(), targetFile);
		
		//Setting the state
		request.setState(FilePushRequest.stateAttached);
		
		//Calling the listener
		handler.post(() -> finalCallbacks.onAttachmentPreparationFinished.accept(targetFile));
		
		//Returning true
		return true;
	}
	
	private boolean uploadFileAMBridge(FilePushRequest request, boolean filePreviouslyCopied) {
		//Copying the request info
		FileProcessingRequest.Callbacks finalCallbacks = request.getCallbacks();
		
		//Getting the connection service
		ConnectionManager connectionManager = ConnectionService.getConnectionManager();
		
		//Checking if the service isn't ready
		if(connectionManager == null || connectionManager.getCurrentState() != ConnectionManager.stateConnected) {
			//Calling the fail method
			request.setInProcessing(false);
			handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalNetwork, null));
			
			//Returning
			return false;
		}
		
		//Getting the request ID and the hash algorithm
		short requestID = connectionManager.getNextRequestID();
		String hashAlgorithm = connectionManager.getCurrentCommunicationsManager().getHashAlgorithm();
		
		//Invalidating the reference to the connection manager
		connectionManager = null;
		
		//Getting the message digest
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance(hashAlgorithm);
		} catch(NoSuchAlgorithmException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Calling the fail method
			request.setInProcessing(false);
			handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalIO, Constants.exceptionToString(exception)));
			
			//Returning
			return false;
		}
		
		//Setting up the streams
		try(FileInputStream srcIS = new FileInputStream(request.getSendFile()); DigestInputStream inputStream = new DigestInputStream(srcIS, messageDigest)) {
			//Preparing to read the file
			long totalLength = inputStream.available();
			byte[] buffer = new byte[ConnectionManager.attachmentChunkSize];
			int bytesRead;
			long totalBytesRead = 0;
			int requestIndex = 0;
			
			//Checking if the file size is too large to send
			if(totalLength > ConnectionManager.largestFileSize || (request.getCompressionTarget() != -1 && totalLength > request.getCompressionTarget())) {
				//Calling the fail method
				request.setInProcessing(false);
				handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalFileTooLarge, null));
				
				//Returning
				return false;
			}
			
			MessageResponseManager responseManager;
			
			{
				//Getting the connection service
				connectionManager = ConnectionService.getConnectionManager();
				
				//Creating the response manager
				responseManager = new MessageResponseManager(new ConnectionManager.MessageResponseManagerDeregistrationListener(connectionManager)) {
					//Forwarding the event to the callbacks
					@Override
					public void onSuccess() {
						finalCallbacks.onUploadResponseReceived.run();
					}
					
					@Override
					public void onFail(int resultCode, String reason) {
						finalCallbacks.onFail.accept(resultCode, reason);
					}
				};
				
				//Checking if the service isn't ready
				if(connectionManager == null || connectionManager.getCurrentState() != ConnectionManager.stateConnected) {
					//Calling the fail method
					request.setInProcessing(false);
					handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalNetwork, null));
					
					//Returning
					return false;
				}
				
				//Adding the request and starting the timer
				connectionManager.addMessageSendRequest(requestID, responseManager);
				
				//Invalidating the reference to the connection manager
				connectionManager = null;
			}
			
			//Looping while there is data to read
			while((bytesRead = inputStream.read(buffer)) != -1) {
				//Adding to the total bytes read
				totalBytesRead += bytesRead;
				
				//Getting the communications manager
				connectionManager = ConnectionService.getConnectionManager();
				CommunicationsManager communicationsManager;
				if(connectionManager == null || (communicationsManager = connectionManager.getCurrentCommunicationsManager()) == null || communicationsManager.getPackager() == null) {
					//Failing the request
					request.setInProcessing(false);
					handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalNetwork, null));
					return false;
				}
				connectionManager = null;
				
				//Preparing the data for upload
				byte[] preparedData = communicationsManager.getPackager().packageData(buffer, bytesRead);
				
				//Checking if the data couldn't be processed
				if(preparedData == null) {
					//Failing the request
					request.setInProcessing(false);
					handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalInternal, null));
					
					//Returning
					return false;
				}
				
				//Uploading the chunk
				boolean uploadResult;
				if(request.isConversationExists()) {
					uploadResult = communicationsManager.uploadFilePacket(requestID, requestIndex, request.getConversationGUID(), preparedData, request.getSendFile().getName(), totalBytesRead >= totalLength);
				} else {
					uploadResult = communicationsManager.uploadFilePacket(requestID, requestIndex, request.getConversationMembers(), preparedData, request.getSendFile().getName(), request.getConversationService(), totalBytesRead >= totalLength);
				}
				
				//Validating the result
				if(!uploadResult) {
					//Failing the request
					request.setInProcessing(false);
					handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalInternal, null));
					
					//Returning
					return false;
				}
				
				//Updating the progress
				final long finalTotalBytesRead = totalBytesRead;
				handler.post(() -> finalCallbacks.onUploadProgress.accept(filePreviouslyCopied ?
																		  (float) (copyProgressValue + (double) finalTotalBytesRead / (double) totalLength * (1F - copyProgressValue)) :
																		  (float) finalTotalBytesRead / (float) totalLength));
				
				//Adding to the request index
				requestIndex++;
			}
			
			//Setting the request state to finished
			request.setState(FilePushRequest.stateFinished);
			
			//Getting the checksum
			byte[] checksum = messageDigest.digest();
			
			//Saving the checksum
			if(request.getAttachmentID() != -1) {
				DatabaseManager.getInstance().updateAttachmentChecksum(request.getAttachmentID(), checksum);
			}
			
			//Running on the main thread
			request.setInProcessing(false);
			handler.post(() -> {
						/* //Getting the connection service
						ConnectionService newConnectionService = ConnectionService.getInstance();
						if(newConnectionService == null) {
							finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalNetwork, null);
							return;
						} */
				
				//Notifying the callback listener
				finalCallbacks.onUploadFinished.accept(checksum);
				
				//Starting the response timer
				responseManager.startTimer();
			});
		} catch(IOException | OutOfMemoryError exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Calling the fail method
			request.setInProcessing(false);
			handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalIO, Constants.exceptionToString(exception)));
			
			//Returning
			return false;
		}
		
		//Returning true
		return true;
	}
	
	private byte[] compressFileInputStream(InputStream inputStream, String fileType, int maxBytes) throws IOException {
		//Reading the file data
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		DataTransformUtils.copyStream(inputStream, byteArrayOutputStream);
		byte[] fileBytes = byteArrayOutputStream.toByteArray();
		
		//Compressing the file and returning the bytes
		return DataTransformUtils.compressFile(fileBytes, fileType, maxBytes);
	}
}
