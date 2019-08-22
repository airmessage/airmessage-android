package me.tagavari.airmessage.connection.request;

import androidx.core.util.Consumer;

import com.google.android.gms.common.util.BiConsumer;

import java.io.File;

import me.tagavari.airmessage.messaging.DraftFile;

public abstract class FileProcessingRequest {
	//Creating the callbacks
	private final Callbacks callbacks = new Callbacks();
	private boolean isInProcessing = false;
	
	public Callbacks getCallbacks() {
		return callbacks;
	}
	
	public boolean isInProcessing() {
		return isInProcessing;
	}
	
	public void setInProcessing(boolean inProcessing) {
		isInProcessing = inProcessing;
	}
	
	public static class Callbacks {
		/* final Constants.WeakRunnable onPlay = new Constants.WeakRunnable();
		final Constants.WeakBiConsumer<File, DraftFile> onDraftPreparationFinished = new Constants.WeakBiConsumer<>();
		final Constants.WeakConsumer<File> onAttachmentPreparationFinished = new Constants.WeakConsumer<>();
		final Constants.WeakConsumer<Float> onUploadProgress = new Constants.WeakConsumer<>();
		final Constants.WeakConsumer<byte[]> onUploadFinished = new Constants.WeakConsumer<>();
		final Constants.WeakRunnable onUploadResponseReceived = new Constants.WeakRunnable();
		final Constants.WeakConsumer<Byte> onFail = new Constants.WeakConsumer<>();
		final Constants.WeakRunnable onRemovalFinish = new Constants.WeakRunnable(); */
		
		public Runnable onStart = new RunnableImpl();
		public BiConsumer<File, DraftFile> onDraftPreparationFinished = new BiConsumerImpl<>();
		public Consumer<File> onAttachmentPreparationFinished = new ConsumerImpl<>();
		public Consumer<Float> onUploadProgress = new ConsumerImpl<>();
		public Consumer<byte[]> onUploadFinished = new ConsumerImpl<>();
		public Runnable onUploadResponseReceived = new RunnableImpl();
		public BiConsumer<Integer, String> onFail = new BiConsumerImpl<>();
		public Runnable onRemovalFinish = new RunnableImpl();
		
		private static class RunnableImpl implements Runnable {
			@Override
			public void run() {}
		}
		
		private static class ConsumerImpl<T> implements Consumer<T> {
			@Override
			public void accept(T t) {}
		}
		
		private static class BiConsumerImpl<T, U> implements BiConsumer<T, U> {
			@Override
			public void accept(T t, U u) {}
		}
	}
}
