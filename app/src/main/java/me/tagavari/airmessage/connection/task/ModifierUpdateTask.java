package me.tagavari.airmessage.connection.task;

import android.content.Context;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Pair;
import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.messaging.StickerInfo;
import me.tagavari.airmessage.messaging.TapbackInfo;
import me.tagavari.airmessage.util.ActivityStatusUpdate;
import me.tagavari.airmessage.util.ModifierMetadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ModifierUpdateTask {
	/**
	 * Asynchronously processes a modifier update
	 * @param context The context to use
	 * @param structModifiers A list of modifiers to process
	 * @return A completable representing this task
	 */
	@CheckReturnValue
	public static Single<Response> create(Context context, Collection<Blocks.ModifierInfo> structModifiers) {
		return Single.create((SingleEmitter<Response> emitter) -> {
			//Creating the result lists
			
			List<ActivityStatusUpdate> activityStatusUpdates = new ArrayList<>();
			List<Pair<StickerInfo, ModifierMetadata>> stickerModifiers = new ArrayList<>();
			List<Pair<TapbackInfo, ModifierMetadata>> tapbackModifiers = new ArrayList<>();
			List<Pair<TapbackInfo, ModifierMetadata>> tapbackRemovals = new ArrayList<>();
			
			//Iterating over the modifiers
			for(Blocks.ModifierInfo modifierInfo : structModifiers) {
				//Checking if the modifier is an activity status modifier
				if(modifierInfo instanceof Blocks.ActivityStatusModifierInfo) {
					//Casting to the activity status modifier
					Blocks.ActivityStatusModifierInfo activityStatusModifierInfo = (Blocks.ActivityStatusModifierInfo) modifierInfo;
					
					//Finding the message in the database
					long messageID = DatabaseManager.getInstance().messageGUIDToLocalID(activityStatusModifierInfo.message);
					if(messageID == -1) continue;
					
					//Updating the modifier in the database
					DatabaseManager.getInstance().updateMessageState(messageID, activityStatusModifierInfo.state, activityStatusModifierInfo.dateRead);
					
					activityStatusUpdates.add(new ActivityStatusUpdate(messageID, activityStatusModifierInfo.state, activityStatusModifierInfo.dateRead));
				}
				//Otherwise checking if the modifier is a sticker update
				else if(modifierInfo instanceof Blocks.StickerModifierInfo) {
					//Updating the modifier in the database
					Blocks.StickerModifierInfo stickerInfo = (Blocks.StickerModifierInfo) modifierInfo;
					try {
						Pair<StickerInfo, ModifierMetadata> pair = DatabaseManager.getInstance().addMessageSticker(context, stickerInfo);
						if(pair != null) stickerModifiers.add(pair);
					} catch(OutOfMemoryError exception) {
						exception.printStackTrace();
						FirebaseCrashlytics.getInstance().recordException(exception);
					}
				}
				//Otherwise checking if the modifier is a tapback update
				else if(modifierInfo instanceof Blocks.TapbackModifierInfo) {
					//Getting the tapback modifier
					Blocks.TapbackModifierInfo tapbackModifierInfo = (Blocks.TapbackModifierInfo) modifierInfo;
					
					//Updating the modifier in the database
					if(tapbackModifierInfo.isAddition) {
						Pair<TapbackInfo, ModifierMetadata> result = DatabaseManager.getInstance().addMessageTapback(tapbackModifierInfo);
						if(result != null) tapbackModifiers.add(result);
					}
					//Deleting the modifier from the database
					else {
						Pair<TapbackInfo, ModifierMetadata> result = DatabaseManager.getInstance().removeMessageTapback(tapbackModifierInfo);
						if(result != null) tapbackRemovals.add(result);
					}
				}
			}
			
			emitter.onSuccess(new Response(activityStatusUpdates, stickerModifiers, tapbackModifiers, tapbackRemovals));
		}).subscribeOn(Schedulers.single()).observeOn(AndroidSchedulers.mainThread());
	}
	
	public static class Response {
		private final List<ActivityStatusUpdate> activityStatusUpdates; //A list of activity status updates
		private final List<Pair<StickerInfo, ModifierMetadata>> stickerModifiers; //New stickers that have been added
		private final List<Pair<TapbackInfo, ModifierMetadata>> tapbackModifiers; //New tapbacks that have been added
		private final List<Pair<TapbackInfo, ModifierMetadata>> tapbackRemovals; //Tapbacks that have been removed
		
		public Response(List<ActivityStatusUpdate> activityStatusUpdates, List<Pair<StickerInfo, ModifierMetadata>> stickerModifiers, List<Pair<TapbackInfo, ModifierMetadata>> tapbackModifiers, List<Pair<TapbackInfo, ModifierMetadata>> tapbackRemovals) {
			this.activityStatusUpdates = activityStatusUpdates;
			this.stickerModifiers = stickerModifiers;
			this.tapbackModifiers = tapbackModifiers;
			this.tapbackRemovals = tapbackRemovals;
		}
		
		public List<ActivityStatusUpdate> getActivityStatusUpdates() {
			return activityStatusUpdates;
		}
		
		public List<Pair<StickerInfo, ModifierMetadata>> getStickerModifiers() {
			return stickerModifiers;
		}
		
		public List<Pair<TapbackInfo, ModifierMetadata>> getTapbackModifiers() {
			return tapbackModifiers;
		}
		
		public List<Pair<TapbackInfo, ModifierMetadata>> getTapbackRemovals() {
			return tapbackRemovals;
		}
	}
}
