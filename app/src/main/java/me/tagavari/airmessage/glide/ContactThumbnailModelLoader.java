package me.tagavari.airmessage.glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;

import java.io.InputStream;

public class ContactThumbnailModelLoader implements ModelLoader<Long, InputStream> {
	@Nullable
	@Override
	public LoadData<InputStream> buildLoadData(@NonNull Long contactID, int width, int height, @NonNull Options options) {
		return null;
	}
	
	@Override
	public boolean handles(@NonNull Long contactID) {
		return true;
	}
}