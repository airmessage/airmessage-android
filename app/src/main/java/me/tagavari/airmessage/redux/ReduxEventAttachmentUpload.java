package me.tagavari.airmessage.redux;

//An event to represent the status of an attachment upload
public abstract class ReduxEventAttachmentUpload {
	//While this file is being uploaded
	public static class Progress extends ReduxEventAttachmentUpload {
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
	
	//When this file has finished being uploaded
	public static class Complete extends ReduxEventAttachmentUpload {
		private final byte[] fileHash;
		
		public Complete(byte[] fileHash) {
			this.fileHash = fileHash;
		}
		
		public byte[] getFileHash() {
			return fileHash;
		}
	}
}