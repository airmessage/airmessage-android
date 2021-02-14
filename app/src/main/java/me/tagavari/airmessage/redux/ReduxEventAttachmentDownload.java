package me.tagavari.airmessage.redux;

import java.io.File;

//An event to represent the status of an attachment download
public abstract class ReduxEventAttachmentDownload {
	public static class Start extends ReduxEventAttachmentDownload {
		private final long fileLength;
		
		public Start(long fileLength) {
			this.fileLength = fileLength;
		}
		
		public long getFileLength() {
			return fileLength;
		}
	}
	
	public static class Progress extends ReduxEventAttachmentDownload {
		private final long bytesProgress, bytesTotal;
		
		public Progress(long bytesProgress, long bytesTotal) {
			this.bytesProgress = bytesProgress;
			this.bytesTotal = bytesTotal;
		}
		
		public long getBytesProgress() {
			return bytesProgress;
		}
		
		public long getBytesTotal() {
			return bytesTotal;
		}
	}
	
	public static class Complete extends ReduxEventAttachmentDownload {
		private final File file;
		
		public Complete(File file) {
			this.file = file;
		}
		
		public File getFile() {
			return file;
		}
	}
}