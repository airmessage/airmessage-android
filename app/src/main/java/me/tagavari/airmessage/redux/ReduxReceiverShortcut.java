package me.tagavari.airmessage.redux;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Pair;
import me.tagavari.airmessage.data.DatabaseManager;
import me.tagavari.airmessage.helper.ConversationHelper;
import me.tagavari.airmessage.helper.ShortcutHelper;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.util.ReplaceInsertResult;

//A receiver that handles creating and updating shortcuts
@RequiresApi(api = Build.VERSION_CODES.N_MR1)
public final class ReduxReceiverShortcut {
	private final Context context;
	private final CompositeDisposable compositeDisposable = new CompositeDisposable();
	
	public ReduxReceiverShortcut(Context context) {
		this.context = context;
	}
	
	public void initialize() {
		compositeDisposable.addAll(
				ReduxEmitterNetwork.getMessageUpdateSubject().subscribe(this::handleMessaging),
				ReduxEmitterNetwork.getMassRetrievalUpdateSubject().subscribe(this::handleMassRetrieval),
				ReduxEmitterNetwork.getTextImportUpdateSubject().subscribe(this::handleTextImport)
		);
	}
	
	private void handleMessaging(ReduxEventMessaging event) {
		if(event instanceof ReduxEventMessaging.Message) {
			pushConversations(((ReduxEventMessaging.Message) event).getConversationItems().stream().map(Pair::getFirst).collect(Collectors.toList()));
		} else if(event instanceof ReduxEventMessaging.ConversationUpdate) {
			List<ConversationInfo> conversationList = new ArrayList<>(((ReduxEventMessaging.ConversationUpdate) event).getNewConversations().keySet());
			Collections.sort(conversationList, ConversationHelper.conversationComparator);
			pushConversations(conversationList);
		} else if(event instanceof ReduxEventMessaging.ConversationTitle || event instanceof ReduxEventMessaging.ConversationMember || event instanceof ReduxEventMessaging.ConversationMemberColor) {
			ShortcutHelper.updateShortcut(context, ((ReduxEventMessaging.ReduxConversationAction) event).getConversationInfo()).subscribe();
		} else if(event instanceof ReduxEventMessaging.ConversationDelete) {
			ShortcutHelper.disableShortcuts(context, Collections.singletonList(((ReduxEventMessaging.ConversationDelete) event).getConversationInfo().getLocalID()));
		} else if(event instanceof ReduxEventMessaging.ConversationServiceHandlerDelete) {
			ReduxEventMessaging.ConversationServiceHandlerDelete deleteEvent = (ReduxEventMessaging.ConversationServiceHandlerDelete) event;
			ShortcutHelper.disableShortcuts(context, Arrays.stream(deleteEvent.getDeletedIDs()).boxed().collect(Collectors.toList()));
		}
	}
	
	private void handleMassRetrieval(ReduxEventMassRetrieval event) {
		if(event instanceof ReduxEventMassRetrieval.Complete) {
			updateTopConversations();
		}
	}
	
	private void handleTextImport(ReduxEventTextImport event) {
		if(event instanceof ReduxEventTextImport.Complete) {
			updateTopConversations();
		}
	}
	
	private void pushConversations(List<ConversationInfo> conversationList) {
		//Push new received conversations to the top of the dynamic shortcut list, ignoring archived conversations
		for(ConversationInfo conversationInfo : conversationList) {
			if(conversationInfo.isArchived()) continue;
			ShortcutHelper.pushShortcut(context, conversationInfo).subscribe();
		}
	}
	
	private void updateTopConversations() {
		//Pull the most recent conversations from the database and assign them as dynamic shortcuts
		Single.fromCallable(() -> DatabaseManager.getInstance().fetchSummaryConversations(context, false, ShortcutHelper.dynamicShortcutLimit))
				.subscribeOn(Schedulers.single())
				.observeOn(AndroidSchedulers.mainThread())
				.flatMapCompletable(conversations -> {
					return ShortcutHelper.assignShortcuts(context, conversations);
				})
				.subscribe();
	}
	
	public void dispose() {
		compositeDisposable.clear();
	}
}