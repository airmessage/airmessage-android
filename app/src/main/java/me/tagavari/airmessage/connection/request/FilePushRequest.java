package me.tagavari.airmessage.connection.request;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import java.io.File;

import me.tagavari.airmessage.messaging.ConversationInfo;

public class FilePushRequest extends FileProcessingRequest {
	//Creating the reference values
	public static final int stateLinked = 0; //Has a link to the file, but no copy of it
	public static final int stateQueued = 1; //A copy of the file is stored and referenced in the chat's drafts
	public static final int stateAttached = 2; //The file is linked to an attachment
	public static final int stateFinished = 3; //The file processing request is completed, the file has been uploaded to the server
	
	//Creating the request values
	//final ConversationInfo conversationInfo;
	private long attachmentID;
	private long draftID;
	private File sendFile;
	private Uri sendUri;
	private String fileType;
	private String fileName;
	private long updateTime;
	private long fileModificationDate = -1;
	private boolean uploadRequested;
	private boolean compressionRequested;
	private int state;
	
	//Creating the conversation values
	private final boolean conversationExists;
	private final long conversationID;
	private final String conversationGUID;
	private final String[] conversationMembers;
	private final String conversationService;
	
	//Creating the handler values
	private Consumer<FilePushRequest> customUploadHandler = null;
	
	private FilePushRequest(ConversationInfo conversationInfo, long attachmentID, long draftID, int state, long updateTime, boolean uploadRequested, boolean compressionRequested) {
		//Setting the callbacks
		//this.callbacks = callbacks;
		
		//Setting the request values
		this.attachmentID = attachmentID;
		this.draftID = draftID;
		this.uploadRequested = uploadRequested;
		this.compressionRequested = compressionRequested;
		this.state = state;
		this.updateTime = updateTime;
		
		if(conversationInfo.getState() == ConversationInfo.ConversationState.READY) {
			conversationExists = true;
			conversationGUID = conversationInfo.getGuid();
			conversationMembers = null;
			conversationService = null;
		} else {
			conversationExists = false;
			conversationGUID = null;
			conversationMembers = conversationInfo.getConversationMembersAsArray();
			conversationService = conversationInfo.getService();
		}
		conversationID = conversationInfo.getLocalID();
	}
	
	public FilePushRequest(@NonNull File file, String fileType, String fileName, long fileModificationDate, ConversationInfo conversationInfo, long attachmentID, long draftID, int state, long updateTime, boolean uploadRequested, boolean compressionRequested) {
		//Calling the main constructor
		this(conversationInfo, attachmentID, draftID, state, updateTime, uploadRequested, compressionRequested);
		
		if(file == null) throw new NullPointerException("File reference cannot be null");
		
		//Setting the source values
		sendFile = file;
		sendUri = null;
		this.fileType = fileType;
		this.fileName = fileName;
		this.fileModificationDate = fileModificationDate;
		
		//Setting the state to queued if the file is in a queue folder
		//if(Paths.get(sendFile.toURI()).startsWith(MainApplication.getDraftDirectory(MainApplication.getInstance()).getPath())) state = stateQueued;
	}
	
	public FilePushRequest(@NonNull Uri uri, String fileType, String fileName, long fileModificationDate, ConversationInfo conversationInfo, long attachmentID, long draftID, int state, long updateTime, boolean uploadRequested, boolean compressionRequested) {
		//Calling the main constructor
		this(conversationInfo, attachmentID, draftID, state, updateTime, uploadRequested, compressionRequested);
		
		if(uri == null) throw new NullPointerException("URI reference cannot be null");
		
		//Setting the source values
		sendFile = null;
		sendUri = uri;
		this.fileType = fileType;
		this.fileName = fileName;
		this.fileModificationDate = fileModificationDate;
	}
	
	public void setAttachmentID(long value) {
		attachmentID = value;
	}
	
	public void setUploadRequested(boolean value) {
		uploadRequested = value;
	}
	
	public long getAttachmentID() {
		return attachmentID;
	}
	
	public void setDraftID(long draftID) {
		this.draftID = draftID;
	}
	
	public long getDraftID() {
		return draftID;
	}
	
	public void setSendFile(File sendFile) {
		this.sendFile = sendFile;
	}
	
	public File getSendFile() {
		return sendFile;
	}
	
	public Uri getSendUri() {
		return sendUri;
	}
	
	public String getFileType() {
		return fileType;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public long getUpdateTime() {
		return updateTime;
	}
	
	public long getFileModificationDate() {
		return fileModificationDate;
	}
	
	public boolean isUploadRequested() {
		return uploadRequested;
	}
	
	public void setCompressionRequested(boolean compressionRequested) {
		this.compressionRequested = compressionRequested;
	}
	
	public boolean isCompressionRequested() {
		return compressionRequested;
	}
	
	public void setState(int state) {
		this.state = state;
	}
	
	public int getState() {
		return state;
	}
	
	public boolean isConversationExists() {
		return conversationExists;
	}
	
	public long getConversationID() {
		return conversationID;
	}
	
	public String getConversationGUID() {
		return conversationGUID;
	}
	
	public String[] getConversationMembers() {
		return conversationMembers;
	}
	
	public String getConversationService() {
		return conversationService;
	}
	
	public Consumer<FilePushRequest> getCustomUploadHandler() {
		return customUploadHandler;
	}
	
	/**
	 * Assign this request a custom upload handler. This consumer will be called on a worker thread, and is expected to handle uploading this message to be sent to the receiver.
	 * If no custom uploader is provided, the file will be uploaded to the bridge server via the connection service.
	 * @param customUploadHandler The upload handler to use
	 */
	public void setCustomUploadHandler(Consumer<FilePushRequest> customUploadHandler) {
		this.customUploadHandler = customUploadHandler;
	}
}
