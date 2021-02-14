package me.tagavari.airmessage.util;

public class ProcessProgress {
	private boolean isProcessing;
	private long progress, max;
	
	public boolean isProcessing() {
		return isProcessing;
	}
	
	public void setProcessing(boolean processing) {
		isProcessing = processing;
	}
	
	public long getProgress() {
		return progress;
	}
	
	public void setProgress(long progress) {
		this.progress = progress;
	}
	
	public long getMax() {
		return max;
	}
	
	public void setMax(long max) {
		this.max = max;
	}
	
	public float getProcessFloat() {
		return (float) progress / max;
	}
}