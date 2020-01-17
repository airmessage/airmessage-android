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
		requestLoop:
		while(!isInterrupted() && (request = pushQueue()) != null) {
			//Getting the callbacks
			FileProcessingRequest.Callbacks finalCallbacks = request.getCallbacks();
			
			//Telling the callbacks that the process has started
			handler.post(finalCallbacks.onStart);
			request.setInProcessing(true);
			
			//Checking if a removal has been requested
			if(request instanceof FileRemovalRequest) {
				//Getting the request
				FileRemovalRequest removalRequest = (FileRemovalRequest) request;
				
				//Removing the file
				ConnectionManager.removeDraftFileSync(removalRequest.getDraftFile(), removalRequest.getUpdateTime());
				
				//Finishing the request
				request.setInProcessing(false);
				handler.post(finalCallbacks.onRemovalFinish);
				continue;
			}
			
			//Getting the request as a push request
			FilePushRequest pushRequest = (FilePushRequest) request;
			
			//Copying the request info
			boolean requestUpload = pushRequest.isUploadRequested();
			
			//Checking if the request has no send file
			//boolean copyFile = request.sendFile == null;
			boolean fileNeedsCopy = pushRequest.getState() == FilePushRequest.stateLinked;
			if(fileNeedsCopy) {
				//Checking if the URI is invalid
				/* if(request.sendUri == null) {
					//Calling the fail method
					handler.post(() -> finalCallbacks.onFail(messageSendInvalidContent));
					
					//Skipping the remainder of the iteration
					continue;
				} */
				
				//Getting the context
				Context context = contextReference.get();
				if(context == null) {
					//Calling the fail method
					pushRequest.setInProcessing(false);
					handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalReferences, null));
					
					//Skipping the remainder of the iteration
					continue;
				}
				
				//Creating the values
				String fileName = null;
				InputStream inputStream = null;
				File originalFile = pushRequest.getSendFile();
				
				try {
					//Checking if the request is using a URI
					if(pushRequest.getSendUri() != null) {
						//Verifying the file size
						try(Cursor cursor = context.getContentResolver().query(pushRequest.getSendUri(), null, null, null, null)) {
							if(cursor != null && cursor.moveToFirst()) {
								long fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
								
								//Checking if the file size is too large to send
								if(fileSize > ConnectionManager.largestFileSize) {
									//Calling the fail method
									pushRequest.setInProcessing(false);
									handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalFileTooLarge, null));
									
									//Skipping the remainder of the iteration
									continue;
								}
							}
						} catch(SecurityException | IllegalArgumentException exception) {
							exception.printStackTrace();
						}
						
						//Finding a valid file
						fileName = pushRequest.getFileName();
						if(fileName == null) {
							fileName = Constants.getUriName(context, pushRequest.getSendUri());
							if(fileName == null) fileName = Constants.defaultFileName;
						}
						
						//Opening the input stream
						try {
							inputStream = context.getContentResolver().openInputStream(pushRequest.getSendUri());
						} catch(IllegalArgumentException | SecurityException exception) {
							//Printing the stack trace
							exception.printStackTrace();
							
							//Calling the fail method
							pushRequest.setInProcessing(false);
							handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalIO, Constants.exceptionToString(exception)));
							
							//Skipping the remainder of the iteration
							continue;
						}
					}
					//Otherwise checking if the request is using a file
					else if(pushRequest.getSendFile() != null) {
						//Checking if the file is outside the target directory
						File targetFolder = requestUpload ? MainApplication.getUploadDirectory(context) : MainApplication.getDraftDirectory(context);
						if(!Constants.checkFileParent(targetFolder, pushRequest.getSendFile())) { //If the file is already in the target directory, there is nothing to do
							//Getting the file name
							fileName = pushRequest.getFileName();
							if(fileName == null) fileName = pushRequest.getSendFile().getName();
							//if(fileName == null) fileName = Constants.defaultFileName;
							
							//Verifying the file size
							if(pushRequest.getSendFile().length() > ConnectionManager.largestFileSize) {
								//Calling the fail method
								pushRequest.setInProcessing(false);
								handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalFileTooLarge, null));
								
								//Skipping the remainder of the iteration
								continue;
							}
							
							//Opening the input stream
							inputStream = new BufferedInputStream(new FileInputStream(pushRequest.getSendFile()));
						}
					} else {
						//Calling the fail method
						pushRequest.setInProcessing(false);
						handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalInternal, Constants.exceptionToString(new IllegalArgumentException("No URI or file reference available to send"))));
						
						//Skipping the remainder of the iteration
						continue;
					}
				} catch(FileNotFoundException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Calling the fail method
					pushRequest.setInProcessing(false);
					handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalIO, Constants.exceptionToString(exception)));
					
					//Closing the input stream
					//Input stream is always null
					/* if(inputStream != null) try {
						inputStream.close();
					} catch(IOException closeException) {
						closeException.printStackTrace();
					} */
					
					//Skipping the remainder of the iteration
					continue;
				}
				
				/* try {
					//Creating the targets
					if(!targetFile.getParentFile().mkdir()) throw new IOException("Couldn't make directory");
					//if(!targetFile.createNewFile()) throw new IOException();
				} catch(IOException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Deleting the parent directory
					targetFile.getParentFile().delete();
					
					//Calling the fail method
					handler.post(() -> finalCallbacks.onFail(messageSendIOException));
					
					//Skipping the remainder of the iteration
					continue;
				} */
				
				//Preparing to copy the file
				if(inputStream != null) {
					//Getting the target file
					File targetFile = requestUpload ?
							MainApplication.getUploadTarget(context, fileName) :
							MainApplication.getDraftTarget(context, pushRequest.getConversationID(), fileName);
					
					try(OutputStream outputStream = new FileOutputStream(targetFile)) {
						//Clearing the reference to the context
						context = null;
						
						//Checking if compression is required
						if(pushRequest.getCompressionTarget() != -1) {
							//Reading the file data
							ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
							DataTransformUtils.copyStream(inputStream, byteArrayOutputStream);
							byte[] fileBytes = byteArrayOutputStream.toByteArray();
							
							//Checking if the file needs to be compressed
							if(fileBytes.length > pushRequest.getCompressionTarget()) {
								//Checking if compression is not applicable
								if(!DataTransformUtils.isCompressable(pushRequest.getFileType())) {
									//Calling the fail method
									pushRequest.setInProcessing(false);
									handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalFileTooLarge, null));
									
									//Skipping the remainder of the iteration
									continue;
								}
								
								//Compressing the file
								byte[] compressedBytes = DataTransformUtils.compressFile(fileBytes, pushRequest.getFileType(), pushRequest.getCompressionTarget());
								
								//Replacing the input stream
								inputStream.close();
								inputStream = new ByteArrayInputStream(compressedBytes);
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
						pushRequest.setSendFile(targetFile);
						pushRequest.clearCompressionTarget();
					} catch(IOException exception) {
						//Printing the stack trace
						exception.printStackTrace();
						
						//Deleting the target file
						targetFile.delete();
						targetFile.getParentFile().delete();
						
						//Calling the fail method
						pushRequest.setInProcessing(false);
						handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalIO, Constants.exceptionToString(exception)));
						
						//Skipping the remainder of the iteration
						continue;
					} finally {
						try {
							inputStream.close();
						} catch(IOException exception) {
							exception.printStackTrace();
						}
					}
					
					//Setting the request's file
					pushRequest.setSendFile(targetFile);
				}
				
				if(requestUpload) {
					//Calling the listener
					handler.post(() -> finalCallbacks.onAttachmentPreparationFinished.accept(pushRequest.getSendFile()));
					
					//Setting the state
					pushRequest.setState(FilePushRequest.stateAttached);
				} else {
					DraftFile draft = DatabaseManager.getInstance().addDraftReference(pushRequest.getConversationID(), pushRequest.getSendFile(), pushRequest.getSendFile().getName(), pushRequest.getSendFile().length(), pushRequest.getFileType(), pushRequest.getFileModificationDate(), originalFile, pushRequest.getSendUri(), pushRequest.getUpdateTime());
					if(draft == null) {
						//Deleting the target file
						pushRequest.getSendFile().delete();
						pushRequest.getSendFile().getParentFile().delete();
						pushRequest.setSendFile(null);
						
						//Calling the fail method
						pushRequest.setInProcessing(false);
						handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalIO, null));
						
						//Skipping the remainder of the iteration
						continue;
					}
					pushRequest.setDraftID( draft.getLocalID());
					
					pushRequest.setInProcessing(false);
					handler.post(() -> finalCallbacks.onDraftPreparationFinished.accept(pushRequest.getSendFile(), draft));
					
					//Setting the state
					pushRequest.setState(FilePushRequest.stateQueued);
				}
				
				//Setting the state
				if(requestUpload) pushRequest.setState(FilePushRequest.stateAttached);
				else pushRequest.setState(FilePushRequest.stateQueued);
			} else {
				//Checking if compression is required
				if(pushRequest.getCompressionTarget() != -1 &&
				   pushRequest.getSendFile() != null &&
				   pushRequest.getSendFile().length() > pushRequest.getCompressionTarget()) {
					//Checking if compression is not applicable
					if(!DataTransformUtils.isCompressable(pushRequest.getFileType())) {
						//Calling the fail method
						pushRequest.setInProcessing(false);
						handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalFileTooLarge, null));
						
						//Skipping the remainder of the iteration
						continue;
					}
					
					//Getting the file
					File file = pushRequest.getSendFile();
					
					//Reading the file data
					ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
					try(FileInputStream fileInputStream = new FileInputStream(file)) {
						DataTransformUtils.copyStream(fileInputStream, byteArrayOutputStream);
					} catch(IOException exception) {
						//Printing the stack trace
						exception.printStackTrace();
						
						//Calling the fail method
						pushRequest.setInProcessing(false);
						handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalIO, null));
						
						//Skipping the remainder of the iteration
						continue;
					}
					byte[] fileBytes = byteArrayOutputStream.toByteArray();
					
					//Compressing the file
					byte[] compressedBytes = DataTransformUtils.compressFile(fileBytes, pushRequest.getFileType(), pushRequest.getCompressionTarget());
					
					//Overwriting the compressed file
					try(FileOutputStream fileOutputStream = new FileOutputStream(pushRequest.getSendFile(), false)) {
						fileOutputStream.write(compressedBytes);
					} catch(IOException exception) {
						exception.printStackTrace();
					}
				}
			}
			
			//Checking if an upload has been requested
			if(requestUpload) {
				//Checking if the state is queued
				if(pushRequest.getState() == FilePushRequest.stateQueued) {
					//Getting the context
					Context context = contextReference.get();
					if(context == null) {
						//Calling the fail method
						pushRequest.setInProcessing(false);
						handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalReferences, null));
						
						//Skipping the remainder of the iteration
						continue;
					}
					
					//Moving the file
					File targetFile = MainApplication.getUploadTarget(context, pushRequest.getSendFile().getName());
					boolean result = pushRequest.getSendFile().renameTo(targetFile);
					if(!result) {
						//Calling the fail method
						pushRequest.setInProcessing(false);
						handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalIO, null));
						
						//Skipping the remainder of the iteration
						continue;
					}
					
					//Deleting the parent directory (since each draft file is stored in its own folder to prevent name collisions)
					pushRequest.getSendFile().getParentFile().delete();
					
					//Updating the file reference
					pushRequest.setSendFile(targetFile);
					
					//Removing the draft reference from the database
					if(pushRequest.getDraftID() != -1) {
						DatabaseManager.getInstance().removeDraftReference(pushRequest.getDraftID(), -1);
						pushRequest.setDraftID(-1);
					}
					
					//Updating the database entry
					if(pushRequest.getAttachmentID() != -1) DatabaseManager.getInstance().updateAttachmentFile(pushRequest.getAttachmentID(), MainApplication.getInstance(), targetFile);
					
					//Setting the state
					pushRequest.setState(FilePushRequest.stateAttached);
					
					//Calling the listener
					handler.post(() -> finalCallbacks.onAttachmentPreparationFinished.accept(targetFile));
				}
				
				//Checking if the file is invalid
				/* if(request.sendFile == null || !request.sendFile.exists()) {
					//Calling the fail method
					handler.post(() -> finalCallbacks.onFail(messageSendInvalidContent));
					
					//Skipping the remainder of the iteration
					continue;
				} */
				
				//Checking if the request has a custom upload handler
				if(pushRequest.getCustomUploadHandler() != null) {
					//Marking the request as completed
					pushRequest.setInProcessing(false);
					pushRequest.setState(FilePushRequest.stateFinished);
					
					//Notifying the upload finished callback
					handler.post(() -> finalCallbacks.onUploadFinished.accept(null));
					
					//Passing the load onto the custom upload handler
					pushRequest.getCustomUploadHandler().accept(pushRequest);
					
					//Skipping the remainder of the iteration
					continue;
				}
				
				//Getting the connection service
				ConnectionManager connectionManager = ConnectionService.getConnectionManager();
				
				//Checking if the service isn't ready
				if(connectionManager == null || connectionManager.getCurrentState() != ConnectionManager.stateConnected) {
					//Calling the fail method
					pushRequest.setInProcessing(false);
					handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalNetwork, null));
					
					//Skipping the remainder of the iteration
					continue;
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
					pushRequest.setInProcessing(false);
					handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalIO, Constants.exceptionToString(exception)));
					
					//Skipping the remainder of the iteration
					continue;
				}
				
				//Setting up the streams
				try(FileInputStream srcIS = new FileInputStream(pushRequest.getSendFile()); DigestInputStream inputStream = new DigestInputStream(srcIS, messageDigest)) {
					//Preparing to read the file
					long totalLength = inputStream.available();
					byte[] buffer = new byte[DataTransformUtils.standardBuffer];
					int bytesRead;
					long totalBytesRead = 0;
					int requestIndex = 0;
					
					//Checking if the file size is too large to send
					if(totalLength > ConnectionManager.largestFileSize) {
						//Calling the fail method
						pushRequest.setInProcessing(false);
						handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalFileTooLarge, null));
						
						//Skipping the remainder of the iteration
						continue;
					}
					
					//Creating the response manager
					MessageResponseManager responseManager = new MessageResponseManager(new ConnectionManager.MessageResponseManagerDeregistrationListener(connectionManager)) {
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
					
					{
						//Getting the connection service
						connectionManager = ConnectionService.getConnectionManager();
						
						//Checking if the service isn't ready
						if(connectionManager == null || connectionManager.getCurrentState() != ConnectionManager.stateConnected) {
							//Calling the fail method
							pushRequest.setInProcessing(false);
							handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalNetwork, null));
							
							//Skipping the remainder of the iteration
							continue;
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
							pushRequest.setInProcessing(false);
							handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalNetwork, null));
							return;
						}
						connectionManager = null;
						
						//Preparing the data for upload
						byte[] preparedData = communicationsManager.getPackager().packageData(buffer, bytesRead);
						
						//Checking if the data couldn't be processed
						if(preparedData == null) {
							//Failing the request
							pushRequest.setInProcessing(false);
							handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalInternal, null));
							
							//Breaking from the loop
							continue requestLoop;
						}
						
						//Uploading the chunk
						boolean uploadResult;
						if(pushRequest.isConversationExists()) {
							uploadResult = communicationsManager.uploadFilePacket(requestID, requestIndex, pushRequest.getConversationGUID(), preparedData, pushRequest.getSendFile().getName(), totalBytesRead >= totalLength);
						} else {
							uploadResult = communicationsManager.uploadFilePacket(requestID, requestIndex, pushRequest.getConversationMembers(), preparedData, pushRequest.getSendFile().getName(), pushRequest.getConversationService(), totalBytesRead >= totalLength);
						}
						
						//Validating the result
						if(!uploadResult) {
							//Failing the request
							pushRequest.setInProcessing(false);
							handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalInternal, null));
							
							//Breaking from the loop
							continue requestLoop;
						}
						
						//Updating the progress
						final long finalTotalBytesRead = totalBytesRead;
						handler.post(() -> finalCallbacks.onUploadProgress.accept(fileNeedsCopy ?
								(float) (copyProgressValue + (double) finalTotalBytesRead / (double) totalLength * (1F - copyProgressValue)) :
								(float) finalTotalBytesRead / (float) totalLength));
						
						//Adding to the request index
						requestIndex++;
					}
					
					//Setting the request state to finished
					pushRequest.setState(FilePushRequest.stateFinished);
					
					//Getting the checksum
					byte[] checksum = messageDigest.digest();
					
					//Saving the checksum
					if(pushRequest.getAttachmentID() != -1) {
						DatabaseManager.getInstance().updateAttachmentChecksum(pushRequest.getAttachmentID(), checksum);
					}
					
					//Running on the main thread
					pushRequest.setInProcessing(false);
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
					pushRequest.setInProcessing(false);
					handler.post(() -> finalCallbacks.onFail.accept(Constants.messageErrorCodeLocalIO, Constants.exceptionToString(exception)));
					
					//Skipping the remainder of the iteration
					//continue;
				}
			}
			
			//Setting the processing flag to false
			pushRequest.setInProcessing(false);
		}
		
		//Calling the finish listener
		finishListener.run();
	}
	
	private FileProcessingRequest pushQueue() {
		FileProcessingRequest request = queue.poll();
		processListener.accept(request);
		return request;
	}
	
	/* private FileUploadRequest pushQueue() {
		//Getting the service
		ConnectionService connectionService = superclassReference.get();
		if(connectionService == null) return null;
		
		//Locking the queue
		synchronized(connectionService.fileUploadRequestQueue) {
			//Returning null if the queue is empty
			if(connectionService.fileUploadRequestQueue.isEmpty()) return null;
			
			//Removing the first item from the queue and returning it
			FileUploadRequest request = connectionService.fileUploadRequestQueue.get(0);
			connectionService.fileUploadRequestQueue.remove(0);
			return request;
		}
	} */
}
