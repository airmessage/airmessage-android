package me.tagavari.airmessage.connection.listener;

import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.connection.exception.AMRemoteUpdateException;
import me.tagavari.airmessage.enums.AttachmentReqErrorCode;
import me.tagavari.airmessage.enums.ConnectionErrorCode;
import me.tagavari.airmessage.util.CompoundErrorDetails;
import me.tagavari.airmessage.util.ServerUpdateData;

import java.io.OutputStream;
import java.util.Collection;

/**
 * A listener for communicating from a communications manager to a connection manager
 */
public interface CommunicationsManagerListener {
	void onOpen(String installationID, String deviceName, String systemVersion, String softwareVersion);
	void onClose(@ConnectionErrorCode int errorCode);
	void onPacket();
	void onMessageUpdate(Collection<Blocks.ConversationItem> data);
	void onMassRetrievalStart(short requestID, Collection<Blocks.ConversationInfo> conversations, int messageCount);
	void onMassRetrievalUpdate(short requestID, int responseIndex, Collection<Blocks.ConversationItem> data);
	void onMassRetrievalComplete(short requestID);
	void onMassRetrievalFail(short requestID);
	void onMassRetrievalFileStart(short requestID, String fileGUID, String fileName, @Nullable String downloadFileName, @Nullable String downloadFileType, @Nullable Function<OutputStream, OutputStream> streamWrapper);
	void onMassRetrievalFileProgress(short requestID, int responseIndex, String fileGUID, byte[] fileData);
	void onMassRetrievalFileComplete(short requestID, String fileGUID);
	void onConversationUpdate(Collection<Blocks.ConversationInfo> data);
	void onModifierUpdate(Collection<Blocks.ModifierInfo> data);
	void onFileRequestStart(short requestID, @Nullable String downloadFileName, @Nullable String downloadFileType, long fileLength, @Nullable Function<OutputStream, OutputStream> streamWrapper);
	void onFileRequestData(short requestID, int responseIndex, byte[] data);
	void onFileRequestComplete(short requestID);
	void onFileRequestFail(short requestID, @AttachmentReqErrorCode int errorCode);
	void onIDUpdate(long messageID);
	void onSendMessageSuccess(short requestID);
	void onSendMessageFail(short requestID, CompoundErrorDetails.MessageSend error);
	void onCreateChatSuccess(short requestID, String chatGUID);
	void onCreateChatError(short requestID, CompoundErrorDetails.ChatCreate error);
	void onSoftwareUpdateListing(@Nullable ServerUpdateData updateData);
	void onSoftwareUpdateInstall(boolean installing);
	void onSoftwareUpdateError(AMRemoteUpdateException exception);
}