package me.tagavari.airmessage.messaging;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import me.tagavari.airmessage.data.DatabaseManager;

public class ConversationAttachmentList {
	//Creating the parameters
	private final boolean loadFromDisk;
	private final long conversationID;
	private final String[] typeFilter;
	
	//Creating the data list
	private final List<UpdateListener> listenerList = new ArrayList<>();
	private final List<Item> itemList;
	
	//Creating the lazy loader variable
	private DatabaseManager.ConversationAttachmentLazyLoader lazyLoader = null;
	
	public ConversationAttachmentList(List<Item> itemList) {
		//Setting the parameters
		loadFromDisk = false;
		this.conversationID = -1;
		this.typeFilter = null;
		this.itemList = itemList;
	}
	
	public ConversationAttachmentList(long conversationID, String[] typeFilter) {
		//Setting the parameters
		loadFromDisk = true;
		this.conversationID = conversationID;
		this.typeFilter = typeFilter;
		itemList = new ArrayList<>();
	}
	
	public List<Item> getItemList() {
		return itemList;
	}
	
	@SuppressLint("StaticFieldLeak")
	public void requestLoad() {
		//Failing the request immediately if the data was not sourced form disk
		if(!loadFromDisk) {
			for(UpdateListener listener : listenerList) listener.onError();
			return;
		}
		
		new AsyncTask<Void, Void, List<ConversationAttachmentList.Item>>() {
			@Override
			protected List<ConversationAttachmentList.Item> doInBackground(Void... voids) {
				if(lazyLoader == null) lazyLoader = new DatabaseManager.ConversationAttachmentLazyLoader(DatabaseManager.getInstance(), conversationID, typeFilter);
				return lazyLoader.loadNextChunk();
			}
			
			@Override
			protected void onPostExecute(List<Item> items) {
				//Checking if there was an error
				if(items == null) {
					for(UpdateListener listener : listenerList) listener.onError();
				}
				//Checking if there are no items
				else if(items.isEmpty()) {
					for(UpdateListener listener : listenerList) listener.onFinish();
				} else {
					//Adding the items
					itemList.addAll(items);
					
					for(UpdateListener listener : listenerList) listener.onLoad(items);
				}
			}
		};
	}
	
	public void addListener(UpdateListener listener) {
		listenerList.add(listener);
	}
	
	public void removeListener(UpdateListener listener) {
		listenerList.add(listener);
	}
	
	public static class Item implements Parcelable {
		public final long localID;
		public final File file;
		public final String fileName;
		public final String type;
		
		public Item(long localID, File file, String fileName, String type) {
			this.localID = localID;
			this.file = file;
			this.fileName = fileName;
			this.type = type;
		}
		
		public Item(AttachmentInfo attachmentInfo) {
			this.localID = attachmentInfo.getLocalID();
			this.file = attachmentInfo.getFile();
			this.fileName = attachmentInfo.getFileName();
			this.type = attachmentInfo.getContentType();
		}
		
		private Item(Parcel in) {
			localID = in.readLong();
			file = (File) in.readSerializable();
			fileName = in.readString();
			type = in.readString();
		}
		
		@Override
		public void writeToParcel(Parcel out, int flags) {
			out.writeLong(localID);
			out.writeSerializable(file);
			out.writeString(fileName);
			out.writeString(type);
		}
		
		@Override
		public int describeContents() {
			return 0;
		}
		
		public static final Parcelable.Creator<Item> CREATOR = new Parcelable.Creator<Item>() {
			public Item createFromParcel(Parcel in) {
				return new Item(in);
			}
			
			public Item[] newArray(int size) {
				return new Item[size];
			}
		};
	}
	
	public interface UpdateListener {
		/**
		 * Called when items are loaded and available from the lazy loader
		 * @param newItems the new items that have just been loaded from disk
		 */
		void onLoad(List<Item> newItems);
		
		/**
		 * Called when a lazy load has been requested, but there are no more items to load
		 */
		void onFinish();
		
		/**
		 * Called when an error occurs while trying to load data
		 */
		void onError();
	}
	
	private static class CreateLazyLoaderTask extends AsyncTask<Void, Void, DatabaseManager.ConversationAttachmentLazyLoader> {
		private final WeakReference<ConversationAttachmentList> parentReference;
		private final long conversationID;
		private final String[] typeFilter;
		
		CreateLazyLoaderTask(ConversationAttachmentList parent, long conversationID, String[] typeFilter) {
			parentReference = new WeakReference<>(parent);
			
			this.conversationID = conversationID;
			this.typeFilter = typeFilter;
		}
		
		@Override
		protected DatabaseManager.ConversationAttachmentLazyLoader doInBackground(Void... voids) {
			return new DatabaseManager.ConversationAttachmentLazyLoader(DatabaseManager.getInstance(), conversationID, typeFilter);
		}
		
		@Override
		protected void onPostExecute(DatabaseManager.ConversationAttachmentLazyLoader conversationAttachmentLazyLoader) {
			ConversationAttachmentList parent = parentReference.get();
			if(parent == null) return;
			
			parent.lazyLoader = conversationAttachmentLazyLoader;
		}
	}
}