package me.tagavari.airmessage.connection;

import androidx.annotation.Nullable;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import me.tagavari.airmessage.redux.ReduxEventAttachmentDownload;
import me.tagavari.airmessage.util.TaskManagerLongBehavior;

public class ConnectionTaskManager {
	private static final TaskManagerLongBehavior<ReduxEventAttachmentDownload> downloadAttachmentTaskManager = new TaskManagerLongBehavior<>();
	
	public static Observable<ReduxEventAttachmentDownload> downloadAttachment(ConnectionManager connectionManager, long messageLocalID, long attachmentLocalID, String attachmentGUID, String attachmentName) {
		return downloadAttachmentTaskManager.run(attachmentLocalID, () -> connectionManager.fetchAttachment(messageLocalID, attachmentLocalID, attachmentGUID, attachmentName));
	}
	
	public static boolean isAttachmentDownloading(long attachmentLocalID) {
		BehaviorSubject<ReduxEventAttachmentDownload> observable = downloadAttachmentTaskManager.get(attachmentLocalID);
		return observable != null && !(observable.getValue() instanceof ReduxEventAttachmentDownload.Complete);
	}
	
	@Nullable
	@CheckReturnValue
	public static BehaviorSubject<ReduxEventAttachmentDownload> getDownload(long attachmentLocalID) {
		return downloadAttachmentTaskManager.get(attachmentLocalID);
	}
	
	public static void removeDownload(long attachmentLocalID) {
		downloadAttachmentTaskManager.remove(attachmentLocalID);
	}
	
	public static void clearDownloads() {
		downloadAttachmentTaskManager.clear();
	}
}