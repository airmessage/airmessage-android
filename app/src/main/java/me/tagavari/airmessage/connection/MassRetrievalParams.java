package me.tagavari.airmessage.connection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MassRetrievalParams {
	public final boolean restrictMessages;
	public final long timeSinceMessages;
	public final boolean downloadAttachments;
	public final boolean restrictAttachments;
	public final long timeSinceAttachments;
	public final boolean restrictAttachmentSizes;
	public final long attachmentSizeLimit;
	public final List<String> attachmentFilterWhitelist;
	public final List<String> attachmentFilterBlacklist;
	public final boolean attachmentFilterDLOutside;
	
	public MassRetrievalParams() {
		restrictMessages = true;
		timeSinceMessages = System.currentTimeMillis() - 4 * 7 * 24 * 60 * 60 * 1000L; //1 month
		downloadAttachments = true;
		restrictAttachments = false;
		timeSinceAttachments = -1;
		restrictAttachmentSizes = true;
		attachmentSizeLimit = 16 * 1024 * 1024; //16 MiB
		attachmentFilterWhitelist = Arrays.asList("image/*", "video/*", "audio/*");
		attachmentFilterBlacklist = new ArrayList<>();
		attachmentFilterDLOutside = false;
	}
	
	public MassRetrievalParams(boolean restrictMessages, long timeSinceMessages, boolean downloadAttachments, boolean restrictAttachments, long timeSinceAttachments, boolean restrictAttachmentSizes, long attachmentSizeLimit, List<String> attachmentFilterWhitelist, List<String> attachmentFilterBlacklist, boolean attachmentFilterDLOutside) {
		this.restrictMessages = restrictMessages;
		this.timeSinceMessages = timeSinceMessages;
		this.downloadAttachments = downloadAttachments;
		this.restrictAttachments = restrictAttachments;
		this.timeSinceAttachments = timeSinceAttachments;
		this.restrictAttachmentSizes = restrictAttachmentSizes;
		this.attachmentSizeLimit = attachmentSizeLimit;
		this.attachmentFilterWhitelist = attachmentFilterWhitelist;
		this.attachmentFilterBlacklist = attachmentFilterBlacklist;
		this.attachmentFilterDLOutside = attachmentFilterDLOutside;
	}
}
