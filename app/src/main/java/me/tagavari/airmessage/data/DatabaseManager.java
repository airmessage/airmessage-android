package me.tagavari.airmessage.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Base64;
import android.util.LongSparseArray;
import android.util.Pair;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.mlkit.nl.smartreply.TextMessage;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import io.reactivex.rxjava3.annotations.CheckReturnValue;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.activity.Messaging;
import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.enums.ConversationItemType;
import me.tagavari.airmessage.enums.ConversationState;
import me.tagavari.airmessage.enums.MessagePreviewState;
import me.tagavari.airmessage.enums.MessageSendErrorCode;
import me.tagavari.airmessage.enums.MessageState;
import me.tagavari.airmessage.enums.ServiceHandler;
import me.tagavari.airmessage.enums.ServiceType;
import me.tagavari.airmessage.helper.AttachmentStorageHelper;
import me.tagavari.airmessage.helper.ConversationColorHelper;
import me.tagavari.airmessage.helper.ConversationHelper;
import me.tagavari.airmessage.helper.DataStreamHelper;
import me.tagavari.airmessage.helper.SmartReplyHelper;
import me.tagavari.airmessage.messaging.AttachmentInfo;
import me.tagavari.airmessage.messaging.AttachmentPreview;
import me.tagavari.airmessage.messaging.ChatCreateAction;
import me.tagavari.airmessage.messaging.ChatMemberAction;
import me.tagavari.airmessage.messaging.ChatRenameAction;
import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.ConversationItem;
import me.tagavari.airmessage.messaging.ConversationPreview;
import me.tagavari.airmessage.messaging.FileDraft;
import me.tagavari.airmessage.messaging.MemberInfo;
import me.tagavari.airmessage.messaging.MessageComponentText;
import me.tagavari.airmessage.messaging.MessageInfo;
import me.tagavari.airmessage.messaging.MessagePreviewInfo;
import me.tagavari.airmessage.messaging.StickerInfo;
import me.tagavari.airmessage.messaging.TapbackInfo;
import me.tagavari.airmessage.util.ModifierMetadata;
import me.tagavari.airmessage.util.ReplaceInsertResult;

public class DatabaseManager extends SQLiteOpenHelper {
	//If you change the database schema, you must increment the database version
	private static final String DATABASE_NAME = "messages.db";
	private static final int DATABASE_VERSION = 12;
	
	//Creating the fetch statements
	/* private static final String SQL_FETCH_CONVERSATIONS = "SELECT * FROM (" +
			"SELECT " + Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_SENDER + ", " +
			Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_OTHER + ", " +
			Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_DATE + ", " +
			Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_SERVICE + ", " +
			Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_ITEMTYPE + ", " +
			Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_ITEMSUBTYPE + ", " +
			Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT + ", " +
			Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_SENDSTYLE + ", " +
			Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_CHAT + ", " +
			Contract.ConversationEntry.TABLE_NAME + "." + Contract.ConversationEntry._ID + ", " +
			Contract.ConversationEntry.TABLE_NAME + "." + Contract.ConversationEntry.COLUMN_NAME_GUID + ", " +
			Contract.ConversationEntry.TABLE_NAME + "." + Contract.ConversationEntry.COLUMN_NAME_COMPLETE + ", " +
			Contract.ConversationEntry.TABLE_NAME + "." + Contract.ConversationEntry.COLUMN_NAME_NAME + ", " +
			Contract.ConversationEntry.TABLE_NAME + "." + Contract.ConversationEntry.COLUMN_NAME_ARCHIVED + ", " +
			Contract.ConversationEntry.TABLE_NAME + "." + Contract.ConversationEntry.COLUMN_NAME_MUTED +
			" FROM " + Contract.MessageEntry.TABLE_NAME +
			" JOIN " + Contract.ConversationEntry.TABLE_NAME + " " + Contract.ConversationEntry.TABLE_NAME + " ON " + Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_CHAT + "=" + Contract.ConversationEntry.TABLE_NAME + "." + Contract.ConversationEntry._ID +
			" ORDER BY " + Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_CHAT + ", " +
			Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_DATE + ")" +
			" x GROUP BY " + Contract.MessageEntry.COLUMN_NAME_CHAT + ";"; */
	private static final String[] sqlQueryConversationData = new String[] {
			Contract.ConversationEntry._ID,
			Contract.ConversationEntry.COLUMN_NAME_GUID,
			Contract.ConversationEntry.COLUMN_NAME_EXTERNALID,
			Contract.ConversationEntry.COLUMN_NAME_STATE,
			Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER,
			Contract.ConversationEntry.COLUMN_NAME_SERVICE,
			Contract.ConversationEntry.COLUMN_NAME_NAME,
			Contract.ConversationEntry.COLUMN_NAME_UNREADMESSAGECOUNT,
			Contract.ConversationEntry.COLUMN_NAME_ARCHIVED,
			Contract.ConversationEntry.COLUMN_NAME_MUTED,
			Contract.ConversationEntry.COLUMN_NAME_COLOR,
			Contract.ConversationEntry.COLUMN_NAME_DRAFTMESSAGE,
			Contract.ConversationEntry.COLUMN_NAME_DRAFTUPDATETIME
	};
	//private static final String messageSortOrder = "COALESCE(" + Contract.MessageEntry.TABLE_NAME + '.' + Contract.MessageEntry.COLUMN_NAME_SERVERID + ',' + Contract.MessageEntry.TABLE_NAME + '.' + Contract.MessageEntry._ID + ')';
	//private static final String messageSortOrder = "CASE WHEN " + Contract.MessageEntry.TABLE_NAME + '.' + Contract.MessageEntry.COLUMN_NAME_SERVERID + " IS NULL THEN " + Contract.MessageEntry.TABLE_NAME + '.' + Contract.MessageEntry._ID + " ELSE " + Contract.MessageEntry.TABLE_NAME + '.' + Contract.MessageEntry.COLUMN_NAME_SERVERID + " END";
	private static final String messageSortOrderDesc = Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED + " DESC, " + Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET + " DESC";
	private static final String messageSortOrderAsc = Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED + " ASC, " + Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET + " ASC";
	private static final String messageSortOrderDescSimple = Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_DATE + " DESC";
	
	//private static final String SQL_FETCH_CONVERSATION_MESSAGES = "SELECT * FROM " + Contract.MessageEntry.TABLE_NAME + " WHERE " + Contract.MessageEntry.COLUMN_NAME_CHAT + " = ? ORDER BY " + Contract.MessageEntry.COLUMN_NAME_DATE + " ASC;";
	
	//Creating the messages table creation statements
	private static final String SQL_CREATE_TABLE_MESSAGES = "CREATE TABLE " + Contract.MessageEntry.TABLE_NAME + " (" +
			Contract.MessageEntry._ID + " INTEGER PRIMARY KEY UNIQUE, " +
			Contract.MessageEntry.COLUMN_NAME_SERVERID + " INTEGER, " +
			Contract.MessageEntry.COLUMN_NAME_GUID + " TEXT, " +
			Contract.MessageEntry.COLUMN_NAME_SENDER + " TEXT, " +
			Contract.MessageEntry.COLUMN_NAME_OTHER + " TEXT, " +
			Contract.MessageEntry.COLUMN_NAME_DATE + " INTEGER NOT NULL, " +
			Contract.MessageEntry.COLUMN_NAME_ITEMTYPE + " INTEGER NOT NULL, " +
			Contract.MessageEntry.COLUMN_NAME_ITEMSUBTYPE + " INTEGER, " +
			Contract.MessageEntry.COLUMN_NAME_STATE + " INTEGER, " +
			Contract.MessageEntry.COLUMN_NAME_ERROR + " INTEGER, " +
			Contract.MessageEntry.COLUMN_NAME_ERRORDETAILS + " TEXT, " +
			Contract.MessageEntry.COLUMN_NAME_DATEREAD + " INTEGER, " +
			Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT + " TEXT, " +
			Contract.MessageEntry.COLUMN_NAME_MESSAGESUBJECT + " TEXT, " +
			Contract.MessageEntry.COLUMN_NAME_SENDSTYLE + " TEXT, " +
			Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED + " INTEGER NOT NULL DEFAULT 0, " +
			Contract.MessageEntry.COLUMN_NAME_CHAT + " INTEGER NOT NULL," +
			Contract.MessageEntry.COLUMN_NAME_PREVIEW_STATE + " INTEGER DEFAULT 0," +
			Contract.MessageEntry.COLUMN_NAME_PREVIEW_ID + " INTEGER," +
			Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED + " INTEGER," +
			Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET + " INTEGER" +
			");";
	private static final String SQL_CREATE_TABLE_CONVERSATIONS = "CREATE TABLE " + Contract.ConversationEntry.TABLE_NAME + " (" +
			Contract.ConversationEntry._ID + " INTEGER PRIMARY KEY UNIQUE, " +
			Contract.ConversationEntry.COLUMN_NAME_GUID + " TEXT, " +
			Contract.ConversationEntry.COLUMN_NAME_EXTERNALID + " INTEGER, " +
			Contract.ConversationEntry.COLUMN_NAME_STATE + " INTEGER NOT NULL, " +
			Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER + " INTEGER NOT NULL, " +
			Contract.ConversationEntry.COLUMN_NAME_SERVICE + " TEXT, " +
			Contract.ConversationEntry.COLUMN_NAME_NAME + " TEXT, " +
			Contract.ConversationEntry.COLUMN_NAME_UNREADMESSAGECOUNT + " INTEGER NOT NULL DEFAULT 0, " +
			Contract.ConversationEntry.COLUMN_NAME_ARCHIVED + " INTEGER NOT NULL DEFAULT 0, " +
			Contract.ConversationEntry.COLUMN_NAME_MUTED + " INTEGER NOT NULL DEFAULT 0," +
			Contract.ConversationEntry.COLUMN_NAME_COLOR + " INTEGER NOT NULL DEFAULT " + 0xFF000000 + ',' + //Black
			Contract.ConversationEntry.COLUMN_NAME_DRAFTMESSAGE + " TEXT," +
			Contract.ConversationEntry.COLUMN_NAME_DRAFTUPDATETIME + " INTEGER NOT NULL DEFAULT 0" +
			");";
	private static final String SQL_CREATE_TABLE_DRAFTS = "CREATE TABLE " + Contract.DraftFileEntry.TABLE_NAME + " (" +
			Contract.DraftFileEntry._ID + " INTEGER PRIMARY KEY UNIQUE," +
			Contract.DraftFileEntry.COLUMN_NAME_CHAT + " INTEGER NOT NULL," +
			Contract.DraftFileEntry.COLUMN_NAME_FILE + " TEXT NOT NULL," +
			Contract.DraftFileEntry.COLUMN_NAME_FILENAME + " TEXT NOT NULL," +
			Contract.DraftFileEntry.COLUMN_NAME_FILESIZE + " INTEGER NOT NULL," +
			Contract.DraftFileEntry.COLUMN_NAME_FILETYPE + " TEXT NOT NULL," +
			Contract.DraftFileEntry.COLUMN_NAME_MEDIASTOREID + " INTEGER," +
			Contract.DraftFileEntry.COLUMN_NAME_MODIFICATIONDATE + " INTEGER" +
			");";
	private static final String SQL_CREATE_TABLE_MEMBERS = "CREATE TABLE " + Contract.MemberEntry.TABLE_NAME + " (" +
			Contract.MemberEntry.COLUMN_NAME_MEMBER + " TEXT NOT NULL," +
			Contract.MemberEntry.COLUMN_NAME_CHAT + " INTEGER NOT NULL, " +
			Contract.MemberEntry.COLUMN_NAME_COLOR + " INTEGER NOT NULL" +
			");";
	private static final String SQL_CREATE_TABLE_ATTACHMENTS = "CREATE TABLE " + Contract.AttachmentEntry.TABLE_NAME + " (" +
			Contract.AttachmentEntry._ID + " INTEGER PRIMARY KEY UNIQUE, " +
			Contract.AttachmentEntry.COLUMN_NAME_GUID + " TEXT UNIQUE," +
			Contract.AttachmentEntry.COLUMN_NAME_MESSAGE + " INTEGER NOT NULL," +
			Contract.AttachmentEntry.COLUMN_NAME_FILENAME + " TEXT," +
			Contract.AttachmentEntry.COLUMN_NAME_FILESIZE + " INTEGER," +
			Contract.AttachmentEntry.COLUMN_NAME_FILETYPE + " TEXT NOT NULL," +
			Contract.AttachmentEntry.COLUMN_NAME_FILEPATH + " TEXT," +
			Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM + " TEXT," +
			Contract.AttachmentEntry.COLUMN_NAME_SORT + " INTEGER" +
			");";
	private static final String SQL_CREATE_TABLE_MESSAGEPREVIEW = "CREATE TABLE " + Contract.MessagePreviewEntry.TABLE_NAME + " (" +
			Contract.MessagePreviewEntry._ID + " INTEGER PRIMARY KEY UNIQUE," +
			Contract.MessagePreviewEntry.COLUMN_NAME_TYPE + " INTEGER NOT NULL, " +
			Contract.MessagePreviewEntry.COLUMN_NAME_DATA + " BLOB, " +
			Contract.MessagePreviewEntry.COLUMN_NAME_TARGET + " TEXT, " +
			Contract.MessagePreviewEntry.COLUMN_NAME_TITLE + " TEXT, " +
			Contract.MessagePreviewEntry.COLUMN_NAME_SUBTITLE + " TEXT, " +
			Contract.MessagePreviewEntry.COLUMN_NAME_CAPTION + " TEXT " +
			");";
	private static final String SQL_CREATE_TABLE_STICKER = "CREATE TABLE " + Contract.StickerEntry.TABLE_NAME + " (" +
			Contract.StickerEntry._ID + " INTEGER PRIMARY KEY UNIQUE, " +
			Contract.StickerEntry.COLUMN_NAME_GUID + " TEXT UNIQUE," +
			Contract.StickerEntry.COLUMN_NAME_MESSAGE + " INTEGER NOT NULL," +
			Contract.StickerEntry.COLUMN_NAME_MESSAGEINDEX + " INTEGER NOT NULL," +
			Contract.StickerEntry.COLUMN_NAME_SENDER + " TEXT," +
			Contract.StickerEntry.COLUMN_NAME_DATE + " INTEGER NOT NULL," +
			Contract.StickerEntry.COLUMN_NAME_FILEPATH + " TEXT" +
			");";
	private static final String SQL_CREATE_TABLE_TAPBACK = "CREATE TABLE " + Contract.TapbackEntry.TABLE_NAME + " (" +
			Contract.TapbackEntry._ID + " INTEGER PRIMARY KEY UNIQUE, " +
			Contract.TapbackEntry.COLUMN_NAME_MESSAGE + " INTEGER NOT NULL," +
			Contract.TapbackEntry.COLUMN_NAME_MESSAGEINDEX + " INTEGER NOT NULL," +
			Contract.TapbackEntry.COLUMN_NAME_SENDER + " TEXT," +
			Contract.TapbackEntry.COLUMN_NAME_CODE + " INTEGER NOT NULL" +
			");";
	/* private static final String SQL_CREATE_TABLE_BLOCKED = "CREATE TABLE " + Contract.BlockedEntry.TABLE_NAME + " (" +
			Contract.BlockedEntry.COLUMN_NAME_ADDRESS + " TEXT NOT NULL," +
			Contract.BlockedEntry.COLUMN_NAME_BLOCKCOUNT + " INTEGER NOT NULL DEFAULT 0" +
			");"; */
	
	//Creating the database instance variable
	private static DatabaseManager instance = null;
	
	private DatabaseManager(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase database) {
		//Creating the tables
		database.execSQL(SQL_CREATE_TABLE_MESSAGES);
		database.execSQL(SQL_CREATE_TABLE_CONVERSATIONS);
		database.execSQL(SQL_CREATE_TABLE_DRAFTS);
		database.execSQL(SQL_CREATE_TABLE_MEMBERS);
		database.execSQL(SQL_CREATE_TABLE_ATTACHMENTS);
		database.execSQL(SQL_CREATE_TABLE_MESSAGEPREVIEW);
		database.execSQL(SQL_CREATE_TABLE_STICKER);
		database.execSQL(SQL_CREATE_TABLE_TAPBACK);
		//database.execSQL(SQL_CREATE_TABLE_BLOCKED);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		switch(oldVersion) {
			case 1:
				//Adding the "date read" column
				database.execSQL("ALTER TABLE messages ADD date_read INTEGER;");
				
				//Adding the sticker and tapback tables
				database.execSQL("CREATE TABLE sticker (" +
						BaseColumns._ID + " INTEGER PRIMARY KEY UNIQUE," +
						"guid TEXT UNIQUE," +
						"message INTEGER NOT NULL," +
						"message_index INTEGER NOT NULL," +
						"sender TEXT," +
						"date INTEGER NOT NULL," +
						"data BLOB NOT NULL" +
						");");
				database.execSQL("CREATE TABLE tapback (" +
						BaseColumns._ID + " INTEGER PRIMARY KEY UNIQUE, " +
						"message INTEGER NOT NULL," +
						"message_index INTEGER NOT NULL," +
						"sender TEXT," +
						"code INTEGER NOT NULL" +
						");");
			case 2: {
				//Adding the "unread messages" column
				database.execSQL("ALTER TABLE conversations ADD unread_message_count INTEGER NOT NULL DEFAULT 0;");
				
				//Adding the sticker and tapback tables (because for some reason they don't exist sometimes)
				database.execSQL("CREATE TABLE IF NOT EXISTS sticker (" +
						BaseColumns._ID + " INTEGER PRIMARY KEY UNIQUE," +
						"guid TEXT UNIQUE," +
						"message INTEGER NOT NULL," +
						"message_index INTEGER NOT NULL," +
						"sender TEXT," +
						"date INTEGER NOT NULL," +
						"data BLOB NOT NULL" +
						");");
				database.execSQL("CREATE TABLE IF NOT EXISTS tapback (" +
						BaseColumns._ID + " INTEGER PRIMARY KEY UNIQUE, " +
						"message INTEGER NOT NULL," +
						"message_index INTEGER NOT NULL," +
						"sender TEXT," +
						"code INTEGER NOT NULL" +
						");");
				
				//Decompressing the sticker data
				{
					Cursor cursor = database.query("sticker", new String[]{BaseColumns._ID, "data"}, null, null, null, null, null);
					int indexID = cursor.getColumnIndexOrThrow(BaseColumns._ID);
					int indexData = cursor.getColumnIndexOrThrow("data");
					
					ContentValues contentValues;
					while(cursor.moveToNext()) {
						contentValues = new ContentValues();
						try {
							byte[] data = cursor.getBlob(indexData);
							Inflater inflater = new Inflater();
							inflater.setInput(data);
							ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
							byte[] buffer = new byte[DataStreamHelper.standardBuffer];
							while (!inflater.finished()) {
								int count = inflater.inflate(buffer);
								outputStream.write(buffer, 0, count);
							}
							outputStream.close();
							
							contentValues.put("data", outputStream.toByteArray());
						} catch(IOException | DataFormatException exception) {
							exception.printStackTrace();
							continue;
						}
						database.update("sticker", contentValues, BaseColumns._ID + " = ?", new String[]{Long.toString(cursor.getLong(indexID))});
					}
					
					cursor.close();
				}
			}
			case 3:
				//Removing the "last viewed" column (it is now obsolete)
				rebuildTable(database, "conversations", "CREATE TABLE conversations (" +
						BaseColumns._ID + " INTEGER PRIMARY KEY UNIQUE, " +
						"guid TEXT UNIQUE, " +
						"state INTEGER NOT NULL, " +
						"service TEXT, " +
						"name TEXT, " +
						//"last_viewed INTEGER DEFAULT 0, " + (removed column)
						"unread_message_count INTEGER NOT NULL DEFAULT 0," +
						"archived INTEGER DEFAULT 0, " +
						"muted INTEGER DEFAULT 0," +
						"color INTEGER DEFAULT " + 0xFF000000 +
						");", false);
			case 4: {
				//Adding the "send style viewed" column
				database.execSQL("ALTER TABLE messages ADD send_style_viewed INTEGER NOT NULL DEFAULT 0;");
				
				//Updating all applicable values to already seen
				{
					ContentValues contentValues = new ContentValues();
					contentValues.put("send_style_viewed", 1);
					database.update("messages", contentValues, "send_style_viewed != ?", new String[]{""});
				}
				
				//Rebuilding the messages table (to remove the "not null" modifier from the send style)
				rebuildTable(database, "messages", "CREATE TABLE messages (" +
						BaseColumns._ID + " INTEGER PRIMARY KEY UNIQUE, " +
						"guid TEXT UNIQUE, " +
						"sender TEXT, " +
						"other TEXT, " +
						"date INTEGER NOT NULL, " +
						"item_type INTEGER NOT NULL, " +
						"item_subtype INTEGER, " +
						"state INTEGER, " +
						"error INTEGER, " +
						"date_read INTEGER, " +
						"message_text TEXT, " +
						"send_style TEXT, " +
						"send_style_viewed INTEGER NOT NULL DEFAULT 0, " +
						"chat INTEGER NOT NULL" +
						");", false);
				
				//Setting all empty send style strings to null
				{
					ContentValues contentValues = new ContentValues();
					contentValues.putNull("send_style");
					database.update("messages", contentValues, "send_style = ?", new String[]{""});
				}
				
				{
					//Reading the message types
					LongSparseArray<String> attachmentTypeList = new LongSparseArray<>();
					try(Cursor cursor = database.query("attachments", new String[]{BaseColumns._ID, "type"}, null, null, null, null, null)) {
						int indexColumn = cursor.getColumnIndexOrThrow(BaseColumns._ID);
						int indexType = cursor.getColumnIndexOrThrow("type");
						
						//Converting the type ID to a content type
						while(cursor.moveToNext()) {
							String mimeType;
							switch(cursor.getInt(indexType)) {
								case 1:
									mimeType = "image";
									break;
								case 2:
									mimeType = "video";
									break;
								case 3:
									mimeType = "audio";
									break;
								default: //case 4
									mimeType = "other";
							}
							
							attachmentTypeList.append(cursor.getLong(indexColumn), mimeType);
						}
					}
					
					//Dropping the type ID column
					rebuildTable(database, "attachments", "CREATE TABLE attachments (" +
							BaseColumns._ID + " INTEGER PRIMARY KEY UNIQUE, " +
							"guid TEXT UNIQUE," +
							"message INTEGER NOT NULL," +
							//"type INTEGER NOT NULL," + (removed column)
							"name TEXT," +
							"path TEXT," +
							"checksum TEXT" +
							");", false);
					
					//Adding the type column (allowing null values)
					database.execSQL("ALTER TABLE attachments ADD type TEXT;");
					
					//Restoring the values
					ContentValues contentValues = new ContentValues();
					for(int i = 0; i < attachmentTypeList.size(); i++) {
						contentValues.put("type", attachmentTypeList.valueAt(i));
						database.update("attachments", contentValues, BaseColumns._ID + " = ?", new String[]{Long.toString(attachmentTypeList.keyAt(i))});
					}
					
					//Rebuilding the table (to disallow null values in the type column)
					rebuildTable(database, "attachments", "CREATE TABLE attachments (" +
							BaseColumns._ID + " INTEGER PRIMARY KEY UNIQUE, " +
							"guid TEXT UNIQUE," +
							"message INTEGER NOT NULL," +
							"name TEXT," +
							"type TEXT NOT NULL," +
							"path TEXT," +
							"checksum TEXT" +
							");", false);
				}
			}
			case 5:
				//Adding the drafts table
				database.execSQL("CREATE TABLE draft_files (" +
						BaseColumns._ID + " INTEGER PRIMARY KEY UNIQUE," +
						"chat INTEGER NOT NULL," +
						"file TEXT NOT NULL," +
						"file_name TEXT NOT NULL," +
						"file_size INTEGER NOT NULL," +
						"file_type TEXT NOT NULL," +
						"original_path TEXT," +
						"modification_date INTEGER" +
						");");
				
				//Adding the "draft message" column
				database.execSQL("ALTER TABLE conversations ADD draft_message TEXT;");
				
				//Adding the "draft update time" column
				database.execSQL("ALTER TABLE conversations ADD draft_update_time INTEGER NOT NULL DEFAULT 0;");
			case 6:
				//Adding the server ID column
				database.execSQL("ALTER TABLE messages ADD server_id INTEGER;");
				
				//Adding the file size column
				database.execSQL("ALTER TABLE attachments ADD size INTEGER;");
			case 7: {
				//Fixing null server IDs
				ContentValues contentValues = new ContentValues();
				contentValues.putNull("server_id");
				database.update("messages", contentValues, "server_id = -1", null);
			}
			case 8:
				//Deleting non-linked messages
				database.execSQL("DELETE FROM messages WHERE server_id IS NULL");
				
				//Adding the error details column
				database.execSQL("ALTER TABLE messages ADD error_details TEXT;");
				
				//Adding the sort columns
				database.execSQL("ALTER TABLE messages ADD sort_id_linked INTEGER NOT NULL DEFAULT 0;");
				database.execSQL("ALTER TABLE messages ADD sort_id_linked_offset INTEGER NOT NULL DEFAULT 0;");
				
				//Replacing all error codes
				database.execSQL("UPDATE messages SET error = 100 WHERE error IS NOT 0");
				
				//Updating the sort columns
				database.execSQL("UPDATE messages SET sort_id_linked = server_id");
				
				//Updating the group action subtype columns (0 is now UNKNOWN, JOIN and LEAVE have been shifted up by 1)
				database.execSQL("UPDATE messages SET item_subtype = item_subtype + 1 WHERE item_type = 1");
			case 9:
				//Rebuilding the messages table (to remove the "unique" modifier from the GUID column, and the "not null" modifier from the linked sort ID columns)
				rebuildTable(database, "messages", "CREATE TABLE messages (" +
												   BaseColumns._ID + " INTEGER PRIMARY KEY UNIQUE, " +
												   "server_id INTEGER, " +
												   "guid TEXT, " + //No more "unique"
												   "sender TEXT, " +
												   "other TEXT, " +
												   "date INTEGER NOT NULL, " +
												   "item_type INTEGER NOT NULL, " +
												   "item_subtype INTEGER, " +
												   "state INTEGER, " +
												   "error INTEGER, " +
												   "error_details TEXT, " +
												   "date_read INTEGER, " +
												   "message_text TEXT, " +
												   "send_style TEXT, " +
												   "send_style_viewed INTEGER NOT NULL DEFAULT 0, " +
												   "chat INTEGER NOT NULL, " +
												   "sort_id_linked INTEGER, " +
												   "sort_id_linked_offset INTEGER" +
												   ");", false);
				
				//Rebuilding the conversations table (to remove the "unique" modifier from the GUID column)
				rebuildTable(database, "conversations", "CREATE TABLE conversations (" +
														BaseColumns._ID + " INTEGER PRIMARY KEY UNIQUE, " +
														"guid TEXT, " + //No more "unique"
														"state INTEGER NOT NULL, " +
														"service TEXT, " +
														"name TEXT, " +
														"unread_message_count INTEGER NOT NULL DEFAULT 0," +
														"archived INTEGER DEFAULT 0, " +
														"muted INTEGER DEFAULT 0, " +
														"color INTEGER DEFAULT " + 0xFF000000 + ", " +
														"draft_message TEXT, " +
														"draft_update_time INTEGER NOT NULL DEFAULT 0" +
														");", false);
				
				//Adding the service handler and external ID columns
				database.execSQL("ALTER TABLE conversations ADD service_handler INTEGER NOT NULL DEFAULT 0;"); //All messages at this point have been over AM bridge
				database.execSQL("ALTER TABLE conversations ADD external_id INTEGER;");
				
				//Adding the message preview columns
				database.execSQL("ALTER TABLE messages ADD preview_state INTEGER DEFAULT 0;");
				database.execSQL("ALTER TABLE messages ADD preview_id INTEGER;");
				
				//Adding the subject field column
				database.execSQL("ALTER TABLE messages ADD message_subject TEXT;");
				
				//Adding the message preview table
				database.execSQL("CREATE TABLE message_preview (" +
						BaseColumns._ID + " INTEGER PRIMARY KEY UNIQUE," +
						"type INTEGER NOT NULL, " +
						"data BLOB, " +
						"target text, " +
						"title TEXT, " +
						"subtitle TEXT, " +
						"caption TEXT " +
						");");
				
				//Adding the original URI to the drafts table
				database.execSQL("ALTER TABLE draft_files ADD original_uri TEXT;");
			case 10:
				//Clearing and rebuilding the stickers table
				database.execSQL("DROP TABLE sticker");
				database.execSQL("CREATE TABLE sticker (" +
								 BaseColumns._ID + " INTEGER PRIMARY KEY UNIQUE, " +
								 "guid TEXT UNIQUE," +
								 "message INTEGER NOT NULL," +
								 "message_index INTEGER NOT NULL," +
								 "sender TEXT," +
								 "date INTEGER NOT NULL," +
								 "path TEXT NOT NULL" +
								 ");");
			case 11:
				//Rebuilding the drafts table (to remove "original_path" and "original_uri", and add "mediastore_id"
				rebuildTable(database, "draft_files", "CREATE TABLE draft_files (" +
						BaseColumns._ID + " INTEGER PRIMARY KEY UNIQUE," +
						"chat INTEGER NOT NULL," +
						"file TEXT NOT NULL," +
						"file_name TEXT NOT NULL," +
						"file_size INTEGER NOT NULL," +
						"file_type TEXT NOT NULL," +
						"modification_date INTEGER," +
						//"original_path TEXT," + (removed column)
						//"original_uri TEXT" + (removed column)
						");", false);
				
				//Adding the "mediastore_id" column to drafts
				database.execSQL("ALTER TABLE draft_files ADD mediastore_id INTEGER;");
				
				//Adding the "sort" column to attachments
				database.execSQL("ALTER TABLE attachments ADD sort INTEGER;");
		}
	}
	
	/* @Override
	public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		//Dropping all tables
		String[] tableNames = getTableNames(database);
		for(String table : tableNames) database.execSQL("DROP TABLE IF EXISTS " + table);
		
		//Rebuilding the database
		onCreate(database);
		
		//Shrinking the database
		//database.execSQL("VACUUM;");
	} */
	
	public static final class Contract {
		//Private constructor to avoid instantiation
		private Contract() {}
		
		static class MessageEntry implements BaseColumns {
			static final String TABLE_NAME = "messages";
			static final String COLUMN_NAME_SERVERID = "server_id";
			static final String COLUMN_NAME_GUID = "guid";
			static final String COLUMN_NAME_SENDER = "sender";
			static final String COLUMN_NAME_OTHER = "other";
			static final String COLUMN_NAME_DATE = "date";
			static final String COLUMN_NAME_ITEMTYPE = "item_type";
			static final String COLUMN_NAME_ITEMSUBTYPE = "item_subtype";
			static final String COLUMN_NAME_STATE = "state";
			static final String COLUMN_NAME_ERROR = "error";
			static final String COLUMN_NAME_ERRORDETAILS = "error_details";
			static final String COLUMN_NAME_DATEREAD = "date_read";
			static final String COLUMN_NAME_MESSAGETEXT = "message_text";
			static final String COLUMN_NAME_MESSAGESUBJECT = "message_subject";
			static final String COLUMN_NAME_SENDSTYLE = "send_style";
			static final String COLUMN_NAME_SENDSTYLEVIEWED = "send_style_viewed";
			static final String COLUMN_NAME_CHAT = "chat";
			static final String COLUMN_NAME_PREVIEW_STATE = "preview_state";
			static final String COLUMN_NAME_PREVIEW_ID = "preview_id";
			static final String COLUMN_NAME_SORTID_LINKED = "sort_id_linked"; //The last serverlinked (server_id is not null) item above this item
			static final String COLUMN_NAME_SORTID_LINKEDOFFSET = "sort_id_linked_offset"; //How many items away this item is from the last serverlinked item
		}
		
		static class ConversationEntry implements BaseColumns {
			static final String TABLE_NAME = "conversations";
			static final String COLUMN_NAME_GUID = "guid";
			static final String COLUMN_NAME_EXTERNALID = "external_id";
			static final String COLUMN_NAME_STATE = "state";
			static final String COLUMN_NAME_SERVICEHANDLER = "service_handler";
			static final String COLUMN_NAME_SERVICE = "service";
			static final String COLUMN_NAME_NAME = "name";
			static final String COLUMN_NAME_UNREADMESSAGECOUNT = "unread_message_count";
			static final String COLUMN_NAME_ARCHIVED = "archived";
			static final String COLUMN_NAME_MUTED = "muted";
			static final String COLUMN_NAME_COLOR = "color";
			static final String COLUMN_NAME_DRAFTMESSAGE = "draft_message";
			static final String COLUMN_NAME_DRAFTUPDATETIME = "draft_update_time";
		}
		
		static class DraftFileEntry implements BaseColumns {
			static final String TABLE_NAME = "draft_files";
			static final String COLUMN_NAME_CHAT = "chat";
			static final String COLUMN_NAME_FILE = "file";
			static final String COLUMN_NAME_FILENAME = "file_name";
			static final String COLUMN_NAME_FILESIZE = "file_size";
			static final String COLUMN_NAME_FILETYPE = "file_type";
			static final String COLUMN_NAME_MEDIASTOREID = "mediastore_id";
			static final String COLUMN_NAME_MODIFICATIONDATE = "modification_date";
		}
		
		static class MemberEntry implements BaseColumns {
			static final String TABLE_NAME = "users";
			static final String COLUMN_NAME_MEMBER = "member";
			static final String COLUMN_NAME_CHAT = "chat";
			static final String COLUMN_NAME_COLOR = "color";
		}
		
		static class AttachmentEntry implements BaseColumns {
			static final String TABLE_NAME = "attachments";
			static final String COLUMN_NAME_GUID = "guid";
			static final String COLUMN_NAME_MESSAGE = "message";
			static final String COLUMN_NAME_FILETYPE = "type"; //The MIME type of the file
			static final String COLUMN_NAME_FILENAME = "name";
			static final String COLUMN_NAME_FILESIZE = "size";
			static final String COLUMN_NAME_FILEPATH = "path";
			static final String COLUMN_NAME_FILECHECKSUM = "checksum";
			static final String COLUMN_NAME_SORT = "sort";
		}
		
		static class MessagePreviewEntry implements BaseColumns {
			static final String TABLE_NAME = "message_preview";
			static final String COLUMN_NAME_TYPE = "type";
			static final String COLUMN_NAME_DATA = "data";
			static final String COLUMN_NAME_TARGET = "target";
			static final String COLUMN_NAME_TITLE = "title";
			static final String COLUMN_NAME_SUBTITLE = "subtitle";
			static final String COLUMN_NAME_CAPTION = "caption";
		}
		
		static class StickerEntry implements BaseColumns {
			static final String TABLE_NAME = "sticker";
			static final String COLUMN_NAME_GUID = "guid";
			static final String COLUMN_NAME_MESSAGE = "message";
			static final String COLUMN_NAME_MESSAGEINDEX = "message_index";
			static final String COLUMN_NAME_SENDER = "sender";
			static final String COLUMN_NAME_DATE = "date";
			static final String COLUMN_NAME_FILEPATH = "path";
		}
		
		static class TapbackEntry implements BaseColumns {
			static final String TABLE_NAME = "tapback";
			static final String COLUMN_NAME_MESSAGE = "message";
			static final String COLUMN_NAME_MESSAGEINDEX = "message_index";
			static final String COLUMN_NAME_SENDER = "sender";
			static final String COLUMN_NAME_CODE = "code";
		}
		
		/* static class BlockedEntry implements BaseColumns {
			static final String TABLE_NAME = "blocked";
			static final String COLUMN_NAME_ADDRESS = "address";
			static final String COLUMN_NAME_BLOCKCOUNT = "block_count";
		} */
	}
	
	public static void createInstance(Context context) {
		instance = new DatabaseManager(context);
	}
	
	public static DatabaseManager getInstance() {
		return instance;
	}
	
	public static void disposeInstance() {
		instance.close();
	}
	
	/* private void dropColumn(SQLiteDatabase writableDatabase, String tableName, String creationCommand, String targetColumn, boolean useTransaction) {
		String columnSelection; //A comma-delimited list of the column names (no type or flag information)
		{
			//Extracting information from the table's creation command
			Pattern columnsPattern = Pattern.compile("(?<=\\().*?(?=\\))");
			String[] columnCodes;
			String tableCommandStart;
			String tableCommandEnd;
			{
				Matcher columnMatcher = columnsPattern.matcher(creationCommand);
				columnMatcher.find();
				tableCommandStart = creationCommand.substring(0, columnMatcher.start());
				tableCommandEnd = creationCommand.substring(columnMatcher.end(), creationCommand.length());
				columnCodes = columnMatcher.group().split(", ?");
			}
			
			//Sorting the column codes
			Arrays.sort(columnCodes);
			
			//Extracting the column targets
			{
				String[] tableColumns = new String[columnCodes.length];
				for(int i = 0; i < columnCodes.length; i++) tableColumns[i] = columnCodes[i].split(" ", 2)[0];
				
				StringBuilder columnTargetSB = new StringBuilder();
				if(tableColumns.length > 0) {
					String columnName;
					{
						columnName = tableColumns[0];
						if(!columnName.equals(targetColumn)) columnTargetSB.append(columnName);
					}
					for(int i = 1; i < tableColumns.length; i++) {
						columnName = tableColumns[i];
						if(!columnName.equals(targetColumn)) columnTargetSB.append(',').append(columnName);
					}
				}
				columnSelection = columnTargetSB.toString();
			}
			
			//Rebuilding the creation command
			StringBuilder creationCommandSB = new StringBuilder();
			creationCommandSB.append(tableCommandStart);
			if(columnCodes.length > 0) {
				String columnCode;
				{
					columnCode = columnCodes[0];
					if(!columnCode.startsWith(targetColumn)) creationCommandSB.append(columnCode);
				}
				for(int i = 1; i < columnCodes.length; i++) {
					columnCode = columnCodes[i];
					if(!columnCode.startsWith(targetColumn)) creationCommandSB.append(',').append(columnCode);
				}
			}
			creationCommandSB.append(tableCommandEnd);
			creationCommand = creationCommandSB.toString();
		}
		
		//Logging the operation
		Crashlytics.log("Column drop requested.\n" +
				"Requested column: " + targetColumn + '\n' +
				"Column target: " + columnSelection + '\n' +
				"Creation command: " + creationCommand);
		
		//Starting the operation
		if(useTransaction) writableDatabase.beginTransaction();
		try {
			writableDatabase.execSQL("CREATE TEMPORARY TABLE " + tableName + "_backup(" + columnSelection + ");");
			writableDatabase.execSQL("INSERT INTO " + tableName + "_backup SELECT " + columnSelection + " FROM " + tableName + ";");
			writableDatabase.execSQL("DROP TABLE " + tableName + ";");
			//writableDatabase.execSQL("CREATE TABLE " + tableName + "(" + columnTarget + ");");
			writableDatabase.execSQL(creationCommand);
			writableDatabase.execSQL("INSERT INTO " + tableName + " SELECT " + columnSelection + " FROM " + tableName + "_backup;");
			writableDatabase.execSQL("DROP TABLE " + tableName + "_backup;");
			if(useTransaction) writableDatabase.setTransactionSuccessful();
		} finally {
			if(useTransaction) writableDatabase.endTransaction();
		}
	} */
	
	private void rebuildTable(SQLiteDatabase writableDatabase, String tableName, String creationCommand, boolean useTransaction) {
		String columnSelection; //A comma-delimited list of the column names (no type or flag information)
		{
			//Extracting information from the table's creation command
			Pattern columnsPattern = Pattern.compile("(?<=\\().*?(?=\\))");
			String[] columnCodes;
			String tableCommandStart;
			String tableCommandEnd;
			{
				Matcher columnMatcher = columnsPattern.matcher(creationCommand);
				columnMatcher.find();
				tableCommandStart = creationCommand.substring(0, columnMatcher.start());
				tableCommandEnd = creationCommand.substring(columnMatcher.end(), creationCommand.length());
				columnCodes = columnMatcher.group().split(", ?");
			}
			
			//Sorting the column codes
			Arrays.sort(columnCodes);
			
			//Extracting the column targets
			{
				String[] tableColumns = new String[columnCodes.length];
				for(int i = 0; i < columnCodes.length; i++) tableColumns[i] = columnCodes[i].split(" ", 2)[0];
				
				StringBuilder columnTargetSB = new StringBuilder();
				if(tableColumns.length > 0) {
					columnTargetSB.append(tableColumns[0]);
					for(int i = 1; i < tableColumns.length; i++) columnTargetSB.append(',').append(tableColumns[i]);
				}
				columnSelection = columnTargetSB.toString();
			}
			
			//Rebuilding the creation command
			StringBuilder creationCommandSB = new StringBuilder();
			creationCommandSB.append(tableCommandStart);
			if(columnCodes.length > 0) {
				creationCommandSB.append(columnCodes[0]);
				for(int i = 1; i < columnCodes.length; i++) creationCommandSB.append(',').append(columnCodes[i]);
			}
			creationCommandSB.append(tableCommandEnd);
			creationCommand = creationCommandSB.toString();
		}
		
		//Logging the operation
		FirebaseCrashlytics.getInstance().log("Table rebuild requested.\n" +
											  "Column target: " + columnSelection + '\n' +
											  "Creation command: " + creationCommand);
		
		//Starting the operation
		if(useTransaction) writableDatabase.beginTransaction();
		try {
			writableDatabase.execSQL("CREATE TEMPORARY TABLE " + tableName + "_backup(" + columnSelection + ");");
			writableDatabase.execSQL("INSERT INTO " + tableName + "_backup SELECT " + columnSelection + " FROM " + tableName + ";");
			writableDatabase.execSQL("DROP TABLE " + tableName + ";");
			//writableDatabase.execSQL("CREATE TABLE " + tableName + "(" + columnTarget + ");");
			writableDatabase.execSQL(creationCommand);
			writableDatabase.execSQL("INSERT INTO " + tableName + " SELECT " + columnSelection + " FROM " + tableName + "_backup;");
			writableDatabase.execSQL("DROP TABLE " + tableName + "_backup;");
			if(useTransaction) writableDatabase.setTransactionSuccessful();
		} finally {
			if(useTransaction) writableDatabase.endTransaction();
		}
	}
	
	private String[] getTableNames(SQLiteDatabase readableDatabase) {
		List<String> tableNames = new ArrayList<>();
		Cursor cursor = readableDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
		//int indexName = cursor.getColumnIndexOrThrow("name");
		while(cursor.moveToNext()) tableNames.add(cursor.getString(0));
		cursor.close();
		return tableNames.toArray(new String[0]);
	}
	
	private String[] getColumnNames(SQLiteDatabase readableDatabase, String tableName) {
		//Android method, sometimes messes with column order which can cause data corruption
		/* try(Cursor cursor = readableDatabase.query(tableName, null, null, null, null, null, null)) {
			return cursor.getColumnNames();
		} */
		
		try(Cursor cursor = readableDatabase.rawQuery("PRAGMA table_info(" + tableName + ")", null)) {
			int nameIndex = cursor.getColumnIndexOrThrow("name");
			String[] columns = new String[cursor.getCount()];
			for(int i = 0; i < columns.length; i++) {
				cursor.moveToNext();
				columns[i] = cursor.getString(nameIndex);
			}
			return columns;
		}
	}
	
	public List<ConversationInfo> fetchConversationsWithState(Context context, @ConversationState int conversationState, int serviceHandler) {
		//Creating the conversation list
		List<ConversationInfo> conversationList = new ArrayList<>();
		
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Querying the database
		Cursor cursor = database.query(Contract.ConversationEntry.TABLE_NAME, sqlQueryConversationData,
				Contract.ConversationEntry.COLUMN_NAME_STATE + " = ? AND " + Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER + " = ?",
				new String[]{Integer.toString(conversationState), Integer.toString(serviceHandler)}, null, null, null);
		
		//Getting the indexes
		int indexChatID = cursor.getColumnIndexOrThrow(Contract.ConversationEntry._ID);
		int indexChatGUID = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_GUID);
		int indexChatExternalID = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_EXTERNALID);
		int indexChatService = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_SERVICE);
		int indexChatName = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_NAME);
		int indexChatUnreadMessages = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_UNREADMESSAGECOUNT);
		int indexChatArchived = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_ARCHIVED);
		int indexChatMuted = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_MUTED);
		int indexChatColor = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_COLOR);
		int indexDraftMessage = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_DRAFTMESSAGE);
		int indexDraftUpdateTime = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_DRAFTUPDATETIME);
		
		//Iterating over the results
		while(cursor.moveToNext()) {
			//Getting the conversation info
			long chatID = cursor.getLong(indexChatID);
			String chatGUID = cursor.getString(indexChatGUID);
			long externalID = cursor.getLong(indexChatExternalID);
			String service = cursor.getString(indexChatService);
			String chatTitle = cursor.getString(indexChatName);
			int chatUnreadMessages = cursor.getInt(indexChatUnreadMessages);
			boolean chatArchived = cursor.getInt(indexChatArchived) != 0;
			boolean chatMuted = cursor.getInt(indexChatMuted) != 0;
			int chatColor = cursor.getInt(indexChatColor);
			ConversationPreview preview = getConversationPreview(chatID, serviceHandler);
			String draftMessage = cursor.getString(indexDraftMessage);
			long draftUpdateTime = cursor.getLong(indexDraftUpdateTime);
			
			//Getting the members and drafts
			ArrayList<MemberInfo> conversationMembers = loadConversationMembers(database, chatID);
			ArrayList<FileDraft> draftFiles = loadDraftFiles(database, chatID, context);
			
			//Creating the conversation
			ConversationInfo conversationInfo = new ConversationInfo(chatID, chatGUID, externalID, conversationState, serviceHandler, service, chatColor, conversationMembers, chatTitle, chatUnreadMessages, chatArchived, chatMuted, preview, draftMessage, draftFiles, draftUpdateTime);
			
			//Adding the conversation to the list
			conversationList.add(conversationInfo);
		}
		
		//Closing the cursor
		cursor.close();
		
		//Returning the conversation list
		return conversationList;
	}
	
	/**
	 * Fetches a list of conversations along with their summary information
	 * This function will automatically filter out any server-incomplete conversations
	 * @param context The context to use
	 * @param onlyArchived TRUE to only return archived conversations, FALSE to only return non-archived conversations
	 * @return A list of conversations
	 */
	public List<ConversationInfo> fetchSummaryConversations(Context context, boolean onlyArchived) {
		return fetchSummaryConversations(context, onlyArchived, -1);
	}
	
	/**
	 * Fetches a list of conversations along with their summary information
	 * This function will automatically filter out any server-incomplete conversations
	 * @param context The context to use
	 * @param onlyArchived TRUE to only return archived conversations, FALSE to only return non-archived conversations
	 * @param limit The number of conversations to retrieve
	 * @return A list of conversations
	 */
	public List<ConversationInfo> fetchSummaryConversations(Context context, boolean onlyArchived, int limit) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Creating the conversation list
		List<ConversationInfo> conversationList = new ArrayList<>();
		
		//Querying the database
		Cursor cursor = database.query(Contract.ConversationEntry.TABLE_NAME, sqlQueryConversationData,
				Contract.ConversationEntry.COLUMN_NAME_STATE + " != ? AND " + Contract.ConversationEntry.COLUMN_NAME_ARCHIVED + (onlyArchived ? " != " : " = ") + "0", new String[]{Integer.toString(ConversationState.incompleteServer)},
				null, null, null, limit == -1 ? null : Integer.toString(limit));
		
		//Getting the indexes
		int indexChatID = cursor.getColumnIndexOrThrow(Contract.ConversationEntry._ID);
		int indexChatGUID = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_GUID);
		int indexChatExternalID = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_EXTERNALID);
		int indexChatState = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_STATE);
		int indexChatServiceHandler = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER);
		int indexChatService = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_SERVICE);
		int indexChatName = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_NAME);
		int indexChatUnreadMessages = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_UNREADMESSAGECOUNT);
		int indexChatArchived = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_ARCHIVED);
		int indexChatMuted = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_MUTED);
		int indexChatColor = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_COLOR);
		int indexDraftMessage = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_DRAFTMESSAGE);
		int indexDraftUpdateTime = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_DRAFTUPDATETIME);
		
		//Iterating over the results
		while(cursor.moveToNext()) {
			//Getting the conversation info
			long chatID = cursor.getLong(indexChatID);
			String chatGUID = cursor.getString(indexChatGUID);
			long externalID = cursor.getLong(indexChatExternalID);
			@ConversationState int conversationState = cursor.getInt(indexChatState);
			int serviceHandler = cursor.getInt(indexChatServiceHandler);
			String service = cursor.getString(indexChatService);
			String chatName = cursor.getString(indexChatName);
			int chatUnreadMessages = cursor.getInt(indexChatUnreadMessages);
			boolean chatArchived = cursor.getInt(indexChatArchived) != 0;
			boolean chatMuted = cursor.getInt(indexChatMuted) != 0;
			int chatColor = cursor.getInt(indexChatColor);
			String draftMessage = cursor.getString(indexDraftMessage);
			long draftUpdateTime = cursor.getLong(indexDraftUpdateTime);

			//Getting the members and drafts
			ArrayList<MemberInfo> conversationMembers = loadConversationMembers(database, chatID);
			ArrayList<FileDraft> draftFiles = loadDraftFiles(database, chatID, context);
			
			//Getting the preview
			ConversationPreview conversationPreview = getConversationPreview(chatID, serviceHandler);
			
			//Creating the conversation
			ConversationInfo conversationInfo = new ConversationInfo(chatID, chatGUID, externalID, conversationState, serviceHandler, service, chatColor, conversationMembers, chatName, chatUnreadMessages, chatArchived, chatMuted, conversationPreview, draftMessage, draftFiles, draftUpdateTime);
			
			//Adding the conversation to the list
			conversationList.add(conversationInfo);
		}
		
		//Closing the cursor
		cursor.close();
		
		//Sorting and returning the conversation list
		Collections.sort(conversationList, ConversationHelper.conversationComparator);
		return conversationList;
	}
	
	/* void switchMessageOwnership(long identifierFrom, long identifierTo) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_CHAT, identifierTo);
		
		//Updating the entries
		getWritableDatabase().update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.MessageEntry.COLUMN_NAME_CHAT + "=?", new String[]{Long.toString(identifierFrom)});
	} */
	
	/**
	 * Merges the messages from one conversation into another
	 * @param context The context to use
	 * @param conversationFromID The ID of the conversation to move messages from
	 * @param conversationToID The ID of the conversation to move messages to
	 * @return A list of changes made to the new conversation
	 */
	public List<ReplaceInsertResult> switchMessageOwnership(Context context, long conversationFromID, long conversationToID) {
		return loadConversationItems(context, conversationFromID).stream().map(item -> transferConversationItemReplaceGhost(context, conversationToID, item)).collect(Collectors.toList());
	}
	
	private static class ConversationItemIndices {
		final int iLocalID, iServerID, iGuid, iSender, iItemType, iDate, iState, iError, iErrorDetails, iDateRead, iSendStyle, iSendStyleViewed, iPreviewState, iPreviewID, iMessageText, iMessageSubject, iOther;
		
		ConversationItemIndices(int iLocalID, int iServerID, int iGuid, int iSender, int iItemType, int iDate, int iState, int iError, int iErrorDetails, int iDateRead, int iSendStyle, int iSendStyleViewed, int iPreviewState, int iPreviewID, int iMessageText, int iMessageSubject, int iOther) {
			this.iLocalID = iLocalID;
			this.iServerID = iServerID;
			this.iGuid = iGuid;
			this.iSender = iSender;
			this.iItemType = iItemType;
			this.iDate = iDate;
			this.iState = iState;
			this.iError = iError;
			this.iErrorDetails = iErrorDetails;
			this.iDateRead = iDateRead;
			this.iSendStyle = iSendStyle;
			this.iSendStyleViewed = iSendStyleViewed;
			this.iPreviewState = iPreviewState;
			this.iPreviewID = iPreviewID;
			this.iMessageText = iMessageText;
			this.iMessageSubject = iMessageSubject;
			this.iOther = iOther;
		}
		
		static ConversationItemIndices fromCursor(Cursor cursor) {
			int iLocalID = cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID);
			int iServerID = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SERVERID);
			int iGuid = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_GUID);
			int iSender = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDER);
			int iItemType = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE);
			int iDate = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_DATE);
			int iState = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_STATE);
			int iError = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_ERROR);
			int iErrorDetails = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_ERRORDETAILS);
			int iDateRead = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_DATEREAD);
			int iSendStyle = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDSTYLE);
			int iSendStyleViewed = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED);
			int iPreviewState = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_PREVIEW_STATE);
			int iPreviewID = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_PREVIEW_ID);
			int iMessageText = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT);
			int iMessageSubject = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_MESSAGESUBJECT);
			int iOther = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_OTHER);
			
			return new ConversationItemIndices(iLocalID, iServerID, iGuid, iSender, iItemType, iDate, iState, iError, iErrorDetails, iDateRead, iSendStyle, iSendStyleViewed, iPreviewState, iPreviewID, iMessageText, iMessageSubject, iOther);
		}
	}
	
	private static class AttachmentInfoIndices {
		final int iLocalID, iGuid, iFileName, iFileType, iFileSize, iFilePath, iChecksum, iSort;
		
		public AttachmentInfoIndices(int iLocalID, int iGuid, int iFileName, int iFileType, int iFileSize, int iFilePath, int iChecksum, int iSort) {
			this.iLocalID = iLocalID;
			this.iGuid = iGuid;
			this.iFileName = iFileName;
			this.iFileType = iFileType;
			this.iFileSize = iFileSize;
			this.iFilePath = iFilePath;
			this.iChecksum = iChecksum;
			this.iSort = iSort;
		}
		
		public static AttachmentInfoIndices fromCursor(Cursor cursor) {
			int iLocalID = cursor.getColumnIndexOrThrow(Contract.AttachmentEntry._ID);
			int iGuid = cursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_GUID);
			int iFileName = cursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILENAME);
			int iFileType = cursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILETYPE);
			int iFileSize = cursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILESIZE);
			int iFilePath = cursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH);
			int iChecksum = cursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM);
			int iSort = cursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_SORT);
			
			return new AttachmentInfoIndices(iLocalID, iGuid, iFileName, iFileType, iFileSize, iFilePath, iChecksum, iSort);
		}
	}
	
	/**
	 * Loads a conversation item from a cursor; for internal use only
	 */
	private ConversationItem loadConversationItem(Context context, ConversationItemIndices indices, Cursor cursor, SQLiteDatabase database) {
		//Getting the general message info
		long localID = cursor.getLong(indices.iLocalID);
		long serverID = cursor.isNull(indices.iServerID) ? -1 : cursor.getLong(indices.iServerID);
		String guid = cursor.getString(indices.iGuid);
		String sender = cursor.isNull(indices.iSender) ? null : cursor.getString(indices.iSender);
		int itemType = cursor.getInt(indices.iItemType);
		long date = cursor.getLong(indices.iDate);
		
		//Checking if the item is a message
		if(itemType == ConversationItemType.message) {
			//Getting the general message info
			int stateCode = cursor.getInt(indices.iState);
			int errorCode = cursor.getInt(indices.iError);
			boolean errorDetailsAvailable = !cursor.isNull(indices.iErrorDetails);
			long dateRead = cursor.getLong(indices.iDateRead);
			String sendStyle = cursor.getString(indices.iSendStyle);
			boolean sendStyleViewed = cursor.getInt(indices.iSendStyleViewed) != 0;
			String messageText = cursor.getString(indices.iMessageText);
			String messageSubject = cursor.getString(indices.iMessageSubject);
			int previewState = cursor.getInt(indices.iPreviewState);
			
			//Querying the database for attachments
			List<AttachmentInfo> attachments;
			try(Cursor attachmentCursor = database.query(Contract.AttachmentEntry.TABLE_NAME, null, Contract.AttachmentEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(localID)}, null, null, Contract.AttachmentEntry.COLUMN_NAME_SORT + " ASC")) {
				//Creating the attachments list
				attachments = new ArrayList<>(attachmentCursor.getCount());
				
				//Loading the attachments
				AttachmentInfoIndices attachmentIndices = AttachmentInfoIndices.fromCursor(attachmentCursor);
				while(attachmentCursor.moveToNext()) {
					attachments.add(loadAttachmentInfo(context, attachmentIndices, attachmentCursor));
				}
			}
			
			//Creating the conversation item
			MessageInfo messageInfo = new MessageInfo(localID, serverID, guid, date, sender, messageText, messageSubject, attachments, sendStyle, sendStyleViewed, dateRead, stateCode, errorCode, errorDetailsAvailable);
			loadApplyStickers(context, messageInfo);
			loadApplyTapbacks(messageInfo);
			
			//Setting the message preview state
			MessageComponentText messageTextInfo = messageInfo.getMessageTextInfo();
			if(messageTextInfo != null) {
				messageTextInfo.setMessagePreviewState(previewState);
				if(!cursor.isNull(indices.iPreviewID)) messageTextInfo.setMessagePreviewID(cursor.getLong(indices.iPreviewID));
			}
			
			//Returning the item
			return messageInfo;
		}
		//Otherwise checking if the item is a group action
		else if(itemType == ConversationItemType.member) {
			//Getting the other
			String other = cursor.getString(indices.iOther);
			
			//Getting the action type
			int subtype = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_ITEMSUBTYPE));
			
			//Creating and returning the conversation item
			return new ChatMemberAction(localID, serverID, guid, date, subtype, sender, other);
		}
		//Otherwise checking if the item is a chat rename
		else if(itemType == ConversationItemType.chatRename) {
			//Getting the name
			String title = cursor.getString(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_OTHER));
			
			//Creating and returning the conversation item
			return new ChatRenameAction(localID, serverID, guid, date, sender, title);
		}
		//Otherwise checking if the item is a chat creation message
		else if(itemType == ConversationItemType.chatCreate) {
			//Creating and returning the item
			return new ChatCreateAction(localID, date);
		}
		
		//Invalid item in database?
		throw new RuntimeException("Unknown item type: " + itemType);
	}
	
	/**
	 * Loads an attachment item from a cursor; for internal use only
	 */
	private static AttachmentInfo loadAttachmentInfo(Context context, AttachmentInfoIndices indices, Cursor cursor) {
		//Getting the attachment data
		File file = cursor.isNull(indices.iFilePath) ? null : AttachmentStorageHelper.getAbsolutePath(context, cursor.getString(indices.iFilePath));
		String fileName = cursor.getString(indices.iFileName);
		String fileType = cursor.getString(indices.iFileType);
		long fileSize = cursor.isNull(indices.iFileSize) ? -1 : cursor.getLong(indices.iFileSize);
		long sort = cursor.isNull(indices.iSort) ? -1 : cursor.getLong(indices.iSort);
		String stringChecksum = cursor.getString(indices.iChecksum);
		byte[] fileChecksum = stringChecksum == null ? null : Base64.decode(stringChecksum, Base64.NO_WRAP);
		
		//Getting the identifiers
		long fileID = cursor.getLong(indices.iLocalID);
		String fileGuid = cursor.getString(indices.iGuid);
		
		//Checking if the attachment has data
		if(file != null && file.exists() && file.isFile()) {
			//Adding the as a file
			return new AttachmentInfo(fileID, fileGuid, fileName, fileType, fileSize, sort, file);
		} else {
			//Adding the with its checksum
			return new AttachmentInfo(fileID, fileGuid, fileName, fileType, fileSize, sort, fileChecksum);
		}
	}
	
	/**
	 * Loads a list of all the conversation items from a specific conversation
	 */
	public List<ConversationItem> loadConversationItems(Context context, long conversationID) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Creating the message list
		List<ConversationItem> conversationItems = new ArrayList<>();
		
		//Querying the database
		Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, null, Contract.MessageEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(conversationID)}, null, null, messageSortOrderAsc, null);
		//Cursor cursor = database.rawQuery(SQL_FETCH_CONVERSATION_MESSAGES, new String[]{Long.toString(conversationInfo.getLocalID())});
		
		//Getting the indices
		ConversationItemIndices indices = ConversationItemIndices.fromCursor(cursor);
		
		//Getting the items
		while(cursor.moveToNext()) conversationItems.add(loadConversationItem(context, indices, cursor, database));
		
		//Closing the cursor
		cursor.close();
		
		//Returning the conversation items
		return conversationItems;
	}
	
	/**
	 * Loads stickers for a particular message and applies it to the message
	 */
	public void loadApplyStickers(Context context, MessageInfo messageInfo) {
		//Querying the database for stickers
		try(Cursor stickerCursor = getReadableDatabase().query(Contract.StickerEntry.TABLE_NAME, null,
				Contract.StickerEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(messageInfo.getLocalID())}, null, null, null)) {
			//Getting the indexes
			int sIdentifierIndex = stickerCursor.getColumnIndexOrThrow(Contract.StickerEntry._ID);
			int sIdentifierMessageIndex = stickerCursor.getColumnIndexOrThrow(Contract.StickerEntry.COLUMN_NAME_MESSAGEINDEX);
			int sIdentifierGuid = stickerCursor.getColumnIndexOrThrow(Contract.StickerEntry.COLUMN_NAME_GUID);
			int sIdentifierSender = stickerCursor.getColumnIndexOrThrow(Contract.StickerEntry.COLUMN_NAME_SENDER);
			int sIdentifierDate = stickerCursor.getColumnIndexOrThrow(Contract.StickerEntry.COLUMN_NAME_DATE);
			int sIdentifierPath = stickerCursor.getColumnIndexOrThrow(Contract.StickerEntry.COLUMN_NAME_FILEPATH);
			
			//Adding the results to the message
			while(stickerCursor.moveToNext()) {
				long stickerID = stickerCursor.getLong(sIdentifierIndex);
				String stickerGUID = stickerCursor.getString(sIdentifierGuid);
				int stickerComponentIndex = stickerCursor.getInt(sIdentifierMessageIndex);
				String stickerSender = stickerCursor.getString(sIdentifierSender);
				long stickerDate = stickerCursor.getLong(sIdentifierDate);
				File stickerFile = AttachmentStorageHelper.getAbsolutePath(context, stickerCursor.getString(sIdentifierPath));
				
				if(stickerComponentIndex >= messageInfo.getComponentCount()) continue;
				messageInfo.getComponentAt(stickerComponentIndex).getStickers().add(new StickerInfo(stickerID, stickerGUID, stickerSender, stickerDate, stickerFile));
			}
		}
	}
	
	/**
	 * Loads tapbacks for a particular message and applies it to the message
	 */
	public void loadApplyTapbacks(MessageInfo messageInfo) {
		//Querying the database for tapbacks
		try(Cursor tapbackCursor = getReadableDatabase().query(Contract.TapbackEntry.TABLE_NAME, new String[]{Contract.TapbackEntry._ID, Contract.TapbackEntry.COLUMN_NAME_MESSAGEINDEX, Contract.TapbackEntry.COLUMN_NAME_SENDER, Contract.TapbackEntry.COLUMN_NAME_CODE},
				Contract.TapbackEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(messageInfo.getLocalID())}, null, null, null)) {
			//Getting the indexes
			int tIdentifierIndex = tapbackCursor.getColumnIndexOrThrow(Contract.TapbackEntry._ID);
			int tIdentifierMessageIndex = tapbackCursor.getColumnIndexOrThrow(Contract.TapbackEntry.COLUMN_NAME_MESSAGEINDEX);
			int tIdentifierSender = tapbackCursor.getColumnIndexOrThrow(Contract.TapbackEntry.COLUMN_NAME_SENDER);
			int tIdentifierCode = tapbackCursor.getColumnIndexOrThrow(Contract.TapbackEntry.COLUMN_NAME_CODE);
			
			//Adding the results to the message
			while(tapbackCursor.moveToNext()) {
				long tapbackID = tapbackCursor.getLong(tIdentifierIndex);
				int tapbackComponentIndex = tapbackCursor.getInt(tIdentifierMessageIndex);
				String tapbackSender = tapbackCursor.getString(tIdentifierSender);
				int tapbackCode = tapbackCursor.getInt(tIdentifierCode);
				
				if(tapbackComponentIndex >= messageInfo.getComponentCount()) continue;
				messageInfo.getComponentAt(tapbackComponentIndex).getTapbacks().add(new TapbackInfo(tapbackID, tapbackSender, tapbackCode));
			}
		}
	}
	
	public ConversationItem loadConversationItem(Context context, long localID) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Querying the database
		try(Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, null, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(localID)}, null, null, null, "1")) {
			//Returning if there are no items
			if(!cursor.moveToFirst()) return null;
			
			//Getting the indices
			ConversationItemIndices indices = ConversationItemIndices.fromCursor(cursor);
			
			//Loading the item
			return loadConversationItem(context, indices, cursor, database);
		}
	}
	
	public Pair<ConversationItem, ConversationInfo> loadConversationItemWithChat(Context context, long messageID) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Querying the database
		try(Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, null, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(messageID)}, null, null, null, "1")) {
			//Returning if there are no items
			if(!cursor.moveToFirst()) return null;
			
			//Getting the indices
			ConversationItemIndices indices = ConversationItemIndices.fromCursor(cursor);
			
			//Loading the conversation
			long conversationID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_CHAT));
			ConversationInfo conversationInfo = fetchConversationInfo(context, conversationID);
			if(conversationInfo == null) return null;
			
			//Loading the item
			return new Pair<>(loadConversationItem(context, indices, cursor, database), conversationInfo);
		}
	}
	
	public MessagePreviewInfo loadMessagePreview(long previewID) {
		//Querying for the preview
		SQLiteDatabase database = getReadableDatabase();
		Cursor previewCursor = database.query(Contract.MessagePreviewEntry.TABLE_NAME, null,
				Contract.MessagePreviewEntry._ID + " = ?", new String[]{Long.toString(previewID)}, null, null, null, "1");
		
		if(!previewCursor.moveToFirst()) {
			previewCursor.close();
			return null;
		}
		//Getting the data
		MessagePreviewInfo preview = new MessagePreviewInfo(
				previewCursor.getInt(previewCursor.getColumnIndexOrThrow(Contract.MessagePreviewEntry.COLUMN_NAME_TYPE)),
				previewID,
				previewCursor.getBlob(previewCursor.getColumnIndexOrThrow(Contract.MessagePreviewEntry.COLUMN_NAME_DATA)),
				previewCursor.getString(previewCursor.getColumnIndexOrThrow(Contract.MessagePreviewEntry.COLUMN_NAME_TARGET)),
				previewCursor.getString(previewCursor.getColumnIndexOrThrow(Contract.MessagePreviewEntry.COLUMN_NAME_TITLE)),
				previewCursor.getString(previewCursor.getColumnIndexOrThrow(Contract.MessagePreviewEntry.COLUMN_NAME_SUBTITLE)),
				previewCursor.getString(previewCursor.getColumnIndexOrThrow(Contract.MessagePreviewEntry.COLUMN_NAME_CAPTION))
		);
		previewCursor.close();
		
		//Returning the preview
		return preview;
	}
	
	/**
	 * Returns the last 10 items of a conversation for quick reply or notification history
	 * @param context The context to use
	 * @param conversationID The conversation to load form
	 * @return The last 10 message items of the conversation
	 */
	public List<MessageInfo> loadConversationHistoryBit(Context context, long conversationID) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Creating the message list
		List<MessageInfo> messageList = new ArrayList<>();
		
		//Querying the database
		Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, null, Contract.MessageEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(conversationID)}, null, null, messageSortOrderDesc, Integer.toString(SmartReplyHelper.smartReplyHistoryLength));
		//Cursor cursor = database.rawQuery(SQL_FETCH_CONVERSATION_MESSAGES, new String[]{Long.toString(conversationInfo.getLocalID())});
		
		//Getting the indices
		ConversationItemIndices indices = ConversationItemIndices.fromCursor(cursor);
		
		//Getting the items
		while(cursor.moveToNext()) {
			//Filtering out non-message items
			if(cursor.getInt(indices.iItemType) != ConversationItemType.message) continue;
			messageList.add((MessageInfo) loadConversationItem(context, indices, cursor, database));
		}
		
		//Closing the cursor
		cursor.close();
		
		//Returning the conversation items
		return messageList;
	}
	
	/**
	 * Returns the last 10 text-based messages of a conversation for quick reply
	 * @param conversationID the ID of the conversation to load form
	 * @return the last 10 text-based message items of the conversation
	 */
	@CheckReturnValue
	public List<TextMessage> loadConversationForMLKit(long conversationID) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Creating the message list
		List<TextMessage> messageList = new ArrayList<>();
		
		//Querying the database
		Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry.COLUMN_NAME_SENDER, Contract.MessageEntry.COLUMN_NAME_DATE, Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT},
				Contract.MessageEntry.COLUMN_NAME_ITEMTYPE + " = " + ConversationItemType.message + " AND " + Contract.MessageEntry.COLUMN_NAME_CHAT + " = ? AND " + Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT + " IS NOT NULL", new String[]{Long.toString(conversationID)},
				null, null, Contract.MessageEntry.COLUMN_NAME_DATE + " DESC", Integer.toString(SmartReplyHelper.smartReplyHistoryLength));
		//Cursor cursor = database.rawQuery(SQL_FETCH_CONVERSATION_MESSAGES, new String[]{Long.toString(conversationInfo.getLocalID())});
		
		//Getting the indexes
		int iSender = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDER);
		int iDate = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_DATE);
		int iMessageText = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT);
		//int iOther = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_OTHER);
		
		//Looping while there are items (in reverse order, because Firebase wants newer messages at the start of the list)
		for(cursor.moveToLast(); !cursor.isBeforeFirst(); cursor.moveToPrevious()) {
			//Getting the message info
			String sender = cursor.isNull(iSender) ? null : cursor.getString(iSender);
			long date = cursor.getLong(iDate);
			String message = cursor.getString(iMessageText);
			
			//Adding the message to the list
			messageList.add(sender == null ? TextMessage.createForLocalUser(message, date) : TextMessage.createForRemoteUser(message, date, sender));
		}
		
		//Closing the cursor
		cursor.close();
		
		//Returning the conversation items
		return messageList;
	}
	
	public void invalidateAttachment(long localID) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.putNull(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH);
		
		//Updating the database
		getWritableDatabase().update(Contract.AttachmentEntry.TABLE_NAME, contentValues, Contract.AttachmentEntry._ID + " = ?", new String[]{Long.toString(localID)});
	}
	
	/**
	 * Removes all attachment files saved to disk under iMessage conversations
	 */
	public void clearDeleteAttachmentFilesAMBridge(Context context) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Selecting all attachments (ID and file path) under an AM Bridge conversation
		try(Cursor cursor = database.rawQuery("SELECT " + Contract.AttachmentEntry.TABLE_NAME + "." + Contract.AttachmentEntry._ID + ", " + Contract.AttachmentEntry.TABLE_NAME + "." + Contract.AttachmentEntry.COLUMN_NAME_FILEPATH + " FROM " + Contract.AttachmentEntry.TABLE_NAME + " " +
											  "JOIN " + Contract.MessageEntry.TABLE_NAME + " ON " + Contract.AttachmentEntry.TABLE_NAME + "." + Contract.AttachmentEntry.COLUMN_NAME_MESSAGE + " = " + Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry._ID + " " +
											  "JOIN " + Contract.ConversationEntry.TABLE_NAME + " ON " + Contract.MessageEntry.TABLE_NAME + "." + Contract.MessageEntry.COLUMN_NAME_CHAT + " = " + Contract.ConversationEntry.TABLE_NAME + "." + Contract.ConversationEntry._ID + " " +
											  "WHERE " + Contract.ConversationEntry.TABLE_NAME + "." + Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER +" = ?", new String[]{Integer.toString(ServiceHandler.appleBridge)})) {
			//Getting the indices
			int iLocalID = cursor.getColumnIndexOrThrow(Contract.AttachmentEntry._ID);
			int iPath = cursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH);
			
			while(cursor.moveToNext()) {
				//Getting the attachment details
				long localID = cursor.getLong(iLocalID);
				String path = cursor.getString(iPath);
				
				//Getting the attachment file
				File attachmentFile = path == null ? null : AttachmentStorageHelper.getAbsolutePath(context, path);
				if(attachmentFile != null) {
					//Deleting the file
					attachmentFile.delete();
					
					//Invalidating the attachment
					invalidateAttachment(localID);
				}
			}
		}
	}
	
	public void updateAttachmentFile(long localID, Context context, File file) {
		//Creating the content values variable
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH, AttachmentStorageHelper.getRelativePath(context, file));
		
		//Updating the data
		getWritableDatabase().update(Contract.AttachmentEntry.TABLE_NAME, contentValues, Contract.AttachmentEntry._ID + " = ?", new String[]{Long.toString(localID)});
	}
	
	public void updateAttachmentFile(String guid, Context context, File file) {
		//Creating the content values variable
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH, AttachmentStorageHelper.getRelativePath(context, file));
		
		//Updating the data
		getWritableDatabase().update(Contract.AttachmentEntry.TABLE_NAME, contentValues, Contract.AttachmentEntry.COLUMN_NAME_GUID + " = ?", new String[]{guid});
	}
	
	public void updateAttachmentFile(long localID, Context context, File file, long fileSize) {
		//Creating the content values variable
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH, AttachmentStorageHelper.getRelativePath(context, file));
		contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILESIZE, fileSize);
		
		//Updating the data
		getWritableDatabase().update(Contract.AttachmentEntry.TABLE_NAME, contentValues, Contract.AttachmentEntry._ID + " = ?", new String[]{Long.toString(localID)});
	}
	
	public void updateAttachmentChecksum(long localID, byte[] checksum) {
		//Creating the content values variable
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM, Base64.encodeToString(checksum, Base64.NO_WRAP));
		
		//Updating the data
		getWritableDatabase().update(Contract.AttachmentEntry.TABLE_NAME, contentValues, Contract.AttachmentEntry._ID + " = ?", new String[]{Long.toString(localID)});
	}
	
	/* static void createUpdateAttachmentFile(SQLiteDatabase writableDatabase, long localID, AttachmentInfo attachmentInfo, File file) {
		//Checking if there is a matching attachment
		Cursor cursor = writableDatabase.query(Contract.AttachmentEntry.TABLE_NAME, null, Contract.AttachmentEntry._ID + "=?", new String[]{Long.toString(localID)}, null, null, null, "1");
		if(cursor.moveToFirst()) {
			//Closing the cursor
			cursor.close();
			
			//Forwarding the event to the standard method
			updateAttachmentFile(writableDatabase, localID, file);
			return;
		}
		
		//Closing the cursor
		cursor.close();
		
		//Creating the content values variable
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH, file.getPath());
		
		//Updating the data
		writableDatabase.update(Contract.AttachmentEntry.TABLE_NAME, contentValues, Contract.AttachmentEntry._ID + "=?", new String[]{Long.toString(localID)});
	} */
	
	/**
	 * Adds a draft file to the database
	 * @param context The context to use
	 * @param conversationID The ID of the draft's conversation
	 * @param file The draft's file
	 * @param fileName The name of the draft's file
	 * @param fileSize The size of the draft's file
	 * @param fileType The type of the draft's file
	 * @param mediaStoreID The draft's media store ID, or -1 if unavailable
	 * @param modificationDate The draft's modification date, or -1 if unavailable
	 * @param updateTime The time to apply to the draft update
	 * @return The complete {@link FileDraft}
	 */
	public FileDraft addDraftReference(Context context, long conversationID, File file, String fileName, long fileSize, String fileType, long mediaStoreID, long modificationDate, long updateTime) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Correcting the file type
		if(fileType == null) fileType = "application/octet-stream";
		
		//Adding the file
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.DraftFileEntry.COLUMN_NAME_CHAT, conversationID);
		contentValues.put(Contract.DraftFileEntry.COLUMN_NAME_FILE, AttachmentStorageHelper.getRelativePath(context, file));
		contentValues.put(Contract.DraftFileEntry.COLUMN_NAME_FILENAME, fileName);
		contentValues.put(Contract.DraftFileEntry.COLUMN_NAME_FILESIZE, fileSize);
		contentValues.put(Contract.DraftFileEntry.COLUMN_NAME_FILETYPE, fileType);
		if(mediaStoreID != -1) contentValues.put(Contract.DraftFileEntry.COLUMN_NAME_MEDIASTOREID, mediaStoreID);
		if(modificationDate != -1) contentValues.put(Contract.DraftFileEntry.COLUMN_NAME_MODIFICATIONDATE, modificationDate);
		
		long localID;
		try {
			localID = database.insertOrThrow(Contract.DraftFileEntry.TABLE_NAME, null, contentValues);
		} catch(SQLiteException exception) {
			exception.printStackTrace();
			return null;
		}
		
		//Updating the draft update time
		updateConversationDraftUpdateTime(database, conversationID, updateTime);
		
		//Returning the new draft file information
		return new FileDraft(localID, file, fileName, fileSize, fileType, mediaStoreID, modificationDate);
	}
	
	/**
	 * Removes a draft from the database
	 * @param draftID The ID the of the draft to remove
	 * @param updateTime The time to apply to the draft update
	 */
	public void removeDraftReference(long draftID, long updateTime) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Checking if the update time should be updated
		if(updateTime != -1) {
			//Getting the conversation ID
			try(Cursor cursor = database.query(Contract.DraftFileEntry.TABLE_NAME, new String[]{Contract.DraftFileEntry.COLUMN_NAME_CHAT}, Contract.DraftFileEntry._ID + " = ?", new String[]{Long.toString(draftID)}, null, null, null, "1")) {
				if(cursor.moveToNext()) {
					long conversationID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_CHAT));
					
					//Updating the draft update time
					updateConversationDraftUpdateTime(database, conversationID, updateTime);
				}
			}
		}
		
		//Removing the item
		database.delete(Contract.DraftFileEntry.TABLE_NAME, Contract.DraftFileEntry._ID + " = ?", new String[]{Long.toString(draftID)});
	}
	
	/**
	 * Removes all drafts under a certain conversation
	 * @param conversationID The ID of the conversation
	 */
	public void clearDraftReferences(long conversationID) {
		SQLiteDatabase database = getWritableDatabase();
		database.delete(Contract.DraftFileEntry.TABLE_NAME, Contract.DraftFileEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(conversationID)});
	}
	
	private void updateConversationDraftUpdateTime(SQLiteDatabase database, long conversationID, long updateTime) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_DRAFTUPDATETIME, updateTime);
		
		database.update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " + ?", new String[]{Long.toString(conversationID)});
	}
	
	/**
	 * Gets the conversation preview information for a specified conversation
	 * @param conversationID The ID of the conversation
	 * @param serviceHandler The service handler of the conversation
	 * @return The conversation preview data for the conversation, or NULL if unavailable
	 */
	@Nullable
	private ConversationPreview getConversationPreview(long conversationID, @ServiceHandler int serviceHandler) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Getting the last item
		long date;
		long lastItemID;
		int itemType;
		try(Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME,
				new String[]{Contract.MessageEntry._ID, Contract.MessageEntry.COLUMN_NAME_ITEMTYPE, Contract.MessageEntry.COLUMN_NAME_DATE},
				Contract.MessageEntry.COLUMN_NAME_CHAT + " = ? AND " + Contract.MessageEntry.COLUMN_NAME_ITEMTYPE + " IN (?, ?)", new String[]{Long.toString(conversationID), Integer.toString(ConversationItemType.message), Integer.toString(ConversationItemType.chatCreate)},
				null, null,
				getConversationBySortDesc(serviceHandler),
				"1")) {
			//Returning null if there are no results
			if(!cursor.moveToNext()) return null;
			
			//Getting the data
			date = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_DATE));
			lastItemID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID));
			itemType = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE));
		}
		
		if(itemType == ConversationItemType.message) { //Message
			//Retrieving the message data
			try(Cursor messageCursor = database.query(Contract.MessageEntry.TABLE_NAME,
					new String[]{Contract.MessageEntry.COLUMN_NAME_SENDER, Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT, Contract.MessageEntry.COLUMN_NAME_MESSAGESUBJECT, Contract.MessageEntry.COLUMN_NAME_SENDSTYLE, Contract.MessageEntry.COLUMN_NAME_ERROR},
					Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(lastItemID)},
					null, null, null)) {
				//Returning null if there are no results
				if(!messageCursor.moveToNext()) return null;
				
				int currentIndex = messageCursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDER);
				String sender = messageCursor.isNull(currentIndex) ? null : messageCursor.getString(currentIndex);
				
				currentIndex = messageCursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT);
				String message = messageCursor.isNull(currentIndex) ? null : messageCursor.getString(currentIndex);
				
				currentIndex = messageCursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_MESSAGESUBJECT);
				String subject = messageCursor.isNull(currentIndex) ? null : messageCursor.getString(currentIndex);
				
				currentIndex = messageCursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDSTYLE);
				String sendStyle = messageCursor.isNull(currentIndex) ? null : messageCursor.getString(currentIndex);
				
				boolean hasError = messageCursor.getInt(messageCursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_ERROR)) != MessageSendErrorCode.none;
				
				//Closing the cursor
				messageCursor.close();
				
				//Checking if the message is valid
				if(message != null) {
					//Returning the conversation message preview (without the attachments)
					return new ConversationPreview.Message(date, sender == null, message, subject, new AttachmentPreview[0], sendStyle, hasError);
				}
				
				//Retrieving the attachments
				try(Cursor attachmentCursor = database.query(Contract.AttachmentEntry.TABLE_NAME,
						new String[]{Contract.AttachmentEntry.COLUMN_NAME_FILETYPE, Contract.AttachmentEntry.COLUMN_NAME_FILENAME},
						Contract.AttachmentEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(lastItemID)},
						null, null, null)) {
					//Getting the attachments
					AttachmentPreview[] attachments = new AttachmentPreview[attachmentCursor.getCount()];
					int indexType = attachmentCursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILETYPE);
					int indexName = attachmentCursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILENAME);
					for(int i = 0; attachmentCursor.moveToNext(); i++) {
						attachments[i] = new AttachmentPreview(attachmentCursor.getString(indexName), attachmentCursor.getString(indexType));
					}
					
					//Returning the conversation message preview
					return new ConversationPreview.Message(date, sender == null, null, subject, attachments, sendStyle, hasError);
				}
			}
		} else if(itemType == ConversationItemType.chatCreate) { //Chat creation
			return new ConversationPreview.ChatCreation(date);
		} else { //No compatible type
			return null;
		}
	}
	
	/**
	 * Tries to find a conversation with matching members in the database; otherwise creates a new client-incomplete conversation with the members
	 * @param context The context to use
	 * @param members The members of the conversation
	 * @param serviceHandler The service handler of the conversation
	 * @param service The service of the conversation
	 * @return A pair of the loaded or created conversation, and a boolean indicating whether this conversation is newly created
	 */
	public Pair<ConversationInfo, Boolean> addRetrieveClientCreatedConversationInfo(Context context, List<String> members, int serviceHandler, String service) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Returning an existing conversation if it exists
		ConversationInfo existingConversation = findConversationInfoWithMembers(context, members, serviceHandler, service, false);
		if(existingConversation != null) return new Pair<>(existingConversation, false);
		
		//Picking a random conversation color
		int conversationColor = ConversationColorHelper.getDefaultConversationColor();
		List<MemberInfo> coloredMembers = ConversationColorHelper.getColoredMembers(members.toArray(new String[0]), conversationColor);
		
		//Setting the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_STATE, ConversationState.incompleteClient);
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_SERVICE, service);
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER, serviceHandler);
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_COLOR, conversationColor);
		
		long localID;
		//Inserting the conversation into the database
		try {
			localID = database.insertOrThrow(Contract.ConversationEntry.TABLE_NAME, null, contentValues);
		} catch(SQLiteConstraintException exception) {
			//Printing the exception's stack trace
			exception.printStackTrace();
			
			//Returning null
			return null;
		}
		
		//Adding the conversation members
		for(MemberInfo member : coloredMembers) {
			contentValues = new ContentValues();
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_CHAT, localID);
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_MEMBER, member.getAddress());
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_COLOR, member.getColor());
			
			database.insert(Contract.MemberEntry.TABLE_NAME, null, contentValues);
		}
		
		//Adding the conversation created message
		ChatCreateAction chatCreateAction = addConversationCreatedMessage(localID, database);
		
		//Creating and returning the conversation info
		ConversationInfo conversationInfo = new ConversationInfo(localID, null, -1, ConversationState.incompleteClient, serviceHandler, service, conversationColor, coloredMembers, null, 0, false, false, new ConversationPreview.ChatCreation(chatCreateAction.getDate()), null, new ArrayList<>(), -1);
		return new Pair<>(conversationInfo, true);
	}
	
	/**
	 * Tries to find a conversation with a matching GUID database; otherwise creates a new server-incomplete AirMessage Bridge conversation
	 * @param context The context to use
	 * @param guid The GUID of the conversation
	 * @return The conversation
	 */
	public ConversationInfo addRetrieveServerCreatedConversationInfo(Context context, String guid) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Returning the existing conversation if one already exists
		Cursor cursor = database.query(Contract.ConversationEntry.TABLE_NAME, null,
				Contract.ConversationEntry.COLUMN_NAME_GUID + " = ? AND " + Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER + " = ?",
				new String[]{guid, Integer.toString(ServiceHandler.appleBridge)},
				null, null, null, "1");
		if(cursor.getCount() > 0) {
			cursor.close();
			return fetchConversationInfo(context, guid, ServiceHandler.appleBridge);
		}
		cursor.close();
		
		//Setting the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_GUID, guid);
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_STATE, ConversationState.incompleteServer);
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER, ServiceHandler.appleBridge);
		
		long localID;
		//Inserting the conversation into the database
		try {
			localID = database.insertOrThrow(Contract.ConversationEntry.TABLE_NAME, null, contentValues);
		} catch(SQLiteConstraintException exception) {
			//Printing the exception's stack trace
			exception.printStackTrace();
			
			//Returning null
			return null;
		}
		
		//Returning the conversation info
		return new ConversationInfo(localID, guid, -1, ConversationState.incompleteServer, ServiceHandler.appleBridge, null, 0xFF000000, new ArrayList<>(), null);
	}
	
	/**
	 * Creates a mixed source conversation (when starting a new conversation from the app). GUID comes from the server, members come from the client.
	 * @param context The context to use
	 * @param guid The GUID of the conversation
	 * @param members The members of the conversation
	 * @param service the conversation's AM bridge service type
	 * @return A pair of the loaded or created conversation, and a boolean indicating whether this conversation is newly created
	 */
	public Pair<ConversationInfo, Boolean> addRetrieveMixedConversationInfoAMBridge(Context context, String guid, List<String> members, @ServiceType String service) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Returning the existing conversation if one already exists
		Cursor cursor = database.query(Contract.ConversationEntry.TABLE_NAME, null,
				Contract.ConversationEntry.COLUMN_NAME_GUID + " = ? AND " + Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER + " = ? AND " + Contract.ConversationEntry.COLUMN_NAME_SERVICE + " = ?", new String[]{guid, Integer.toString(ServiceHandler.appleBridge), service},
				null, null, null, "1");
		if(cursor.getCount() > 0) {
			cursor.close();
			return new Pair<>(fetchConversationInfo(context, guid, ServiceHandler.appleBridge), false);
		}
		cursor.close();
		
		//Picking a color
		int conversationColor = ConversationColorHelper.getDefaultConversationColor(guid);
		List<MemberInfo> coloredMembers = ConversationColorHelper.getColoredMembers(members.toArray(new String[0]), conversationColor);
		
		//Inserting the conversation into the database
		long localID;
		try {
			ContentValues contentValues = new ContentValues();
			contentValues.put(Contract.ConversationEntry.COLUMN_NAME_GUID, guid);
			contentValues.put(Contract.ConversationEntry.COLUMN_NAME_STATE, ConversationState.ready);
			contentValues.put(Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER, ServiceHandler.appleBridge);
			contentValues.put(Contract.ConversationEntry.COLUMN_NAME_SERVICE, service);
			contentValues.put(Contract.ConversationEntry.COLUMN_NAME_COLOR, conversationColor);
			
			localID = database.insertOrThrow(Contract.ConversationEntry.TABLE_NAME, null, contentValues);
		} catch(SQLiteConstraintException exception) {
			//Printing the exception's stack trace
			exception.printStackTrace();
			
			//Returning null
			return null;
		}
		
		//Adding the conversation members
		for(MemberInfo member : coloredMembers) {
			//Setting the content values
			ContentValues contentValues = new ContentValues();
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_CHAT, localID);
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_MEMBER, member.getAddress());
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_COLOR, member.getColor());
			
			//Inserting the data
			database.insert(Contract.MemberEntry.TABLE_NAME, null, contentValues);
		}
		
		//Adding the conversation created message
		ChatCreateAction chatCreateAction = addConversationCreatedMessage(localID, database);
		
		//Creating and returning the conversation info
		ConversationInfo conversationInfo = new ConversationInfo(localID, guid, -1, ConversationState.ready, ServiceHandler.appleBridge, service, conversationColor, coloredMembers, null, 0, false, false, new ConversationPreview.ChatCreation(chatCreateAction.getDate()), null, new ArrayList<>(), -1);
		return new Pair<>(conversationInfo, true);
	}
	
	/**
	 * Writes a new network-received AirMessage Bridge conversation to disk
	 * @param structConversationInfo The conversation to add
	 * @return A complete instance of the conversation, or NULL if failed
	 */
	public ConversationInfo addReadyConversationInfoAMBridge(Blocks.ConversationInfo structConversationInfo) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Deleting the conversation if it exists in the database
		long existingLocalID = -1;
		try(Cursor cursor = database.query(Contract.ConversationEntry.TABLE_NAME, new String[]{Contract.ConversationEntry._ID},
				Contract.ConversationEntry.COLUMN_NAME_GUID + " = ? AND " + Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER + " = ?",
				new String[]{structConversationInfo.guid, Integer.toString(ServiceHandler.appleBridge)},
				null, null, null, "1")) {
			if(cursor.moveToNext()) {
				existingLocalID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.ConversationEntry._ID));
				database.delete(Contract.ConversationEntry.TABLE_NAME,
						Contract.ConversationEntry.COLUMN_NAME_GUID + " = ? AND " + Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER + " = ?",
						new String[]{structConversationInfo.guid, Integer.toString(ServiceHandler.appleBridge)});
			}
		}
		
		//Picking a color
		int conversationColor = ConversationColorHelper.getDefaultConversationColor(structConversationInfo.guid);
		List<MemberInfo> coloredMembers = ConversationColorHelper.getColoredMembers(structConversationInfo.members, conversationColor, structConversationInfo.guid);
		
		//Inserting the conversation into the database
		long localID;
		try {
			ContentValues contentValues = new ContentValues();
			contentValues.put(Contract.ConversationEntry.COLUMN_NAME_GUID, structConversationInfo.guid);
			contentValues.put(Contract.ConversationEntry.COLUMN_NAME_STATE, ConversationState.ready);
			contentValues.put(Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER, ServiceHandler.appleBridge);
			contentValues.put(Contract.ConversationEntry.COLUMN_NAME_SERVICE, structConversationInfo.service);
			contentValues.put(Contract.ConversationEntry.COLUMN_NAME_NAME, structConversationInfo.name);
			contentValues.put(Contract.ConversationEntry.COLUMN_NAME_COLOR, conversationColor);
			
			localID = database.insertOrThrow(Contract.ConversationEntry.TABLE_NAME, null, contentValues);
		} catch(SQLiteConstraintException exception) {
			//Printing the exception's stack trace
			exception.printStackTrace();
			
			//Returning null
			return null;
		}
		
		//Adding the members
		if(existingLocalID != -1) database.delete(Contract.MemberEntry.TABLE_NAME, Contract.MemberEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(existingLocalID)}); //Deleting the existing members
		for(MemberInfo member : coloredMembers) {
			ContentValues contentValues = new ContentValues();
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_MEMBER, member.getAddress());
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_COLOR, member.getColor());
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_CHAT, localID);
			
			database.insert(Contract.MemberEntry.TABLE_NAME, null, contentValues);
		}
		
		//Creating and returning the conversation
		return new ConversationInfo(localID, structConversationInfo.guid, -1, ConversationState.ready, ServiceHandler.appleBridge, structConversationInfo.service, conversationColor, coloredMembers, structConversationInfo.name);
	}
	
	/**
	 * Writes the provided conversation to disk
	 * @param conversationInfo The conversation to save
	 * @return Whether the process succeeded
	 */
	public boolean addConversationInfo(ConversationInfo conversationInfo) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Setting the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_EXTERNALID, conversationInfo.getExternalID());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_GUID, conversationInfo.getGUID());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_STATE, conversationInfo.getState());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER, conversationInfo.getServiceHandler());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_SERVICE, conversationInfo.getServiceType());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_COLOR, conversationInfo.getConversationColor());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_ARCHIVED, conversationInfo.isArchived());
		
		//Inserting the conversation into the database
		long localID;
		try {
			localID = database.insertOrThrow(Contract.ConversationEntry.TABLE_NAME, null, contentValues);
		} catch(SQLiteConstraintException exception) {
			//Printing the exception's stack trace
			exception.printStackTrace();
			
			//Returning
			return false;
		}
		
		//Setting the conversation's ID
		conversationInfo.setLocalID(localID);
		
		//Adding the conversation members
		for(MemberInfo member : conversationInfo.getMembers()) {
			//Setting the content values
			contentValues = new ContentValues();
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_CHAT, localID);
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_MEMBER, member.getAddress());
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_COLOR, member.getColor());
			
			//Inserting the data
			database.insert(Contract.MemberEntry.TABLE_NAME, null, contentValues);
		}
		
		//Returning true
		return true;
	}
	
	/**
	 * Creates and returns a new "chat created" message
	 * @param conversationID The ID of the conversation to add the message to
	 * @return A newly created chat create action
	 */
	public ChatCreateAction addConversationCreatedMessage(long conversationID) {
		return addConversationCreatedMessage(conversationID, getWritableDatabase());
	}
	
	/**
	 * Creates and returns a new "chat created" message
	 * @param conversationID The ID of the conversation to add the message to
	 * @param database The instance of the database to use
	 * @return A newly created chat create action
	 */
	private ChatCreateAction addConversationCreatedMessage(long conversationID, SQLiteDatabase database) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_DATE, System.currentTimeMillis());
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE, ConversationItemType.chatCreate);
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_CHAT, conversationID);
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED, -1);
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET, 0);
		
		long createdMessageLocalID = database.insert(Contract.MessageEntry.TABLE_NAME, null, contentValues);
		
		return new ChatCreateAction(createdMessageLocalID, System.currentTimeMillis());
	}
	
	public ConversationInfo findConversationInfoWithMembers(Context context, List<String> members, int serviceHandler, String service, boolean clientIncompleteOnly) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Getting all conversation identifiers
		Cursor conversationCursor = database.query(Contract.ConversationEntry.TABLE_NAME, new String[]{Contract.ConversationEntry._ID},
				Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER + " = ? AND " + Contract.ConversationEntry.COLUMN_NAME_SERVICE + " = ?" + (clientIncompleteOnly ? " AND " + Contract.ConversationEntry.COLUMN_NAME_STATE + " = ?" : ""),
				clientIncompleteOnly ? new String[]{Integer.toString(serviceHandler), service, Integer.toString(ConversationState.incompleteClient)} : new String[]{Integer.toString(serviceHandler), service},
				null, null, null);
		
		//Iterating over the results
		while(conversationCursor.moveToNext()) {
			//Getting the conversation identifier
			long conversationID = conversationCursor.getLong(conversationCursor.getColumnIndexOrThrow(Contract.ConversationEntry._ID));
			
			//Getting the conversation's members
			List<String> conversationMembers = new ArrayList<>();
			Cursor memberCursor = database.query(Contract.MemberEntry.TABLE_NAME, new String[]{Contract.MemberEntry.COLUMN_NAME_MEMBER}, Contract.MemberEntry.COLUMN_NAME_CHAT + "=?", new String[]{Long.toString(conversationID)}, null, null, null);
			while(memberCursor.moveToNext()) conversationMembers.add(memberCursor.getString(memberCursor.getColumnIndexOrThrow(Contract.MemberEntry.COLUMN_NAME_MEMBER)));
			memberCursor.close();
			//Constants.normalizeAddresses(conversationMembers);
			
			//Checking if the members match
			if(members.size() == conversationMembers.size() && members.containsAll(conversationMembers)) {
				//Returning the complete conversation info
				conversationCursor.close();
				return fetchConversationInfo(context, conversationID);
			}
		}
		
		//Closing the conversation cursor
		conversationCursor.close();
		
		//Returning null
		return null;
	}
	
	public ConversationInfo findConversationByExternalID(Context context, long externalID, int serviceHandler, String service) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Querying the database
		try(Cursor cursor = database.query(Contract.ConversationEntry.TABLE_NAME, sqlQueryConversationData,
				Contract.ConversationEntry.COLUMN_NAME_EXTERNALID + " = ? AND " + Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER + " = ? AND " + Contract.ConversationEntry.COLUMN_NAME_SERVICE + " = ?",
				new String[]{Long.toString(externalID), Integer.toString(serviceHandler), service},
				null, null, null)) {
			//Returning null if there are no results
			if(!cursor.moveToNext()) return null;
			
			//Returning the conversation
			return fetchConversationInfo(cursor, database, context);
		}
	}
	
	public ConversationInfo fetchConversationInfo(Context context, String conversationGUID, int serviceHandler) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Querying the database
		try(Cursor cursor = database.query(Contract.ConversationEntry.TABLE_NAME, sqlQueryConversationData,
				Contract.ConversationEntry.COLUMN_NAME_GUID + " = ? AND " + Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER + " = ?",
				new String[]{conversationGUID, Integer.toString(serviceHandler)},
				null, null, null)) {
			//Returning null if there are no results
			if(!cursor.moveToNext()) return null;
			
			//Returning the conversation
			return fetchConversationInfo(cursor, database, context);
		}
	}
	
	public ConversationInfo fetchConversationInfo(Context context, long localID) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Querying the database
		try(Cursor cursor = database.query(Contract.ConversationEntry.TABLE_NAME, sqlQueryConversationData, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(localID)}, null, null, null)) {
			//Returning null if there are no results
			if(!cursor.moveToNext()) return null;
			
			//Returning the conversation
			return fetchConversationInfo(cursor, database, context);
		}
	}
	
	/**
	 * Fetches a conversation using a cursor from the database
	 * @param cursor The cursor to read from
	 * @param database The database to use for subsequent queries
	 * @param context The context to use
	 * @return The conversation loaded from the cursor
	 */
	private ConversationInfo fetchConversationInfo(Cursor cursor, SQLiteDatabase database, Context context) {
		//Getting the conversation info
		long localID  = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.ConversationEntry._ID));
		long externalID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_EXTERNALID));
		String conversationGUID = cursor.getString(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_GUID));
		int conversationState = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_STATE));
		int serviceHandler = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER));
		String service = cursor.getString(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_SERVICE));
		String chatTitle = cursor.getString(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_NAME));
		int chatUnreadMessages = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_UNREADMESSAGECOUNT));
		boolean chatArchived = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_ARCHIVED)) != 0;
		boolean chatMuted = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_MUTED)) != 0;
		int chatColor = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_COLOR));
		ConversationPreview preview = getConversationPreview(localID, serviceHandler);
		String draftMessage = cursor.getString(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_DRAFTMESSAGE));
		long draftUpdateTime = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_DRAFTUPDATETIME));
		
		//Getting the members and drafts
		ArrayList<MemberInfo> conversationMembers = loadConversationMembers(database, localID);
		ArrayList<FileDraft> draftFiles = loadDraftFiles(database, localID, context);
		
		//Creating and returning the conversation info
		return new ConversationInfo(localID, conversationGUID, externalID, conversationState, serviceHandler, service, chatColor, conversationMembers, chatTitle, chatUnreadMessages, chatArchived, chatMuted, preview, draftMessage, draftFiles, draftUpdateTime);
	}
	
	/**
	 * Fetches a list of members for a conversation
	 * @param database The database to use for this query
	 * @param conversationID The ID of the conversation of which to load the members
	 * @return A list of members of the conversation
	 */
	private ArrayList<MemberInfo> loadConversationMembers(SQLiteDatabase database, long conversationID) {
		ArrayList<MemberInfo> conversationMembers = new ArrayList<>();
		try(Cursor cursor = database.query(Contract.MemberEntry.TABLE_NAME, new String[]{Contract.MemberEntry.COLUMN_NAME_MEMBER, Contract.MemberEntry.COLUMN_NAME_COLOR}, Contract.MemberEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(conversationID)}, null, null, null)) {
			int indexMember = cursor.getColumnIndexOrThrow(Contract.MemberEntry.COLUMN_NAME_MEMBER);
			int indexColor = cursor.getColumnIndexOrThrow(Contract.MemberEntry.COLUMN_NAME_COLOR);
			while(cursor.moveToNext()) conversationMembers.add(new MemberInfo(cursor.getString(indexMember), cursor.getInt(indexColor)));
		}
		return conversationMembers;
	}
	
	/**
	 * Fetches a list of draft files for a conversation
	 * @param database The database to use for this query
	 * @param conversationID The ID of the conversation of which to load the draft files
	 * @param context The context to use
	 * @return A list of draft files of the conversation
	 */
	private ArrayList<FileDraft> loadDraftFiles(SQLiteDatabase database, long conversationID, Context context) {
		ArrayList<FileDraft> draftFiles = new ArrayList<>();
		try(Cursor cursor = database.query(Contract.DraftFileEntry.TABLE_NAME, new String[]{Contract.DraftFileEntry._ID, Contract.DraftFileEntry.COLUMN_NAME_FILE, Contract.DraftFileEntry.COLUMN_NAME_FILENAME, Contract.DraftFileEntry.COLUMN_NAME_FILESIZE, Contract.DraftFileEntry.COLUMN_NAME_FILETYPE, Contract.DraftFileEntry.COLUMN_NAME_MEDIASTOREID, Contract.DraftFileEntry.COLUMN_NAME_MODIFICATIONDATE}, Contract.DraftFileEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(conversationID)}, null, null, null)) {
			int indexIdentifier = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry._ID);
			int indexFile = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_FILE);
			int indexFileName = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_FILENAME);
			int indexFileSize = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_FILESIZE);
			int indexFileType = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_FILETYPE);
			int indexMediaStoreID = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_MEDIASTOREID);
			int indexModificationDate = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_MODIFICATIONDATE);
			while(cursor.moveToNext()) {
				draftFiles.add(new FileDraft(
						cursor.getLong(indexIdentifier),
						AttachmentStorageHelper.getAbsolutePath(context, cursor.getString(indexFile)),
						cursor.getString(indexFileName),
						cursor.getLong(indexFileSize),
						cursor.getString(indexFileType),
						cursor.isNull(indexMediaStoreID) ? -1 : cursor.getLong(indexMediaStoreID),
						cursor.isNull(indexModificationDate) ? -1 : cursor.getLong(indexModificationDate)
				));
			}
		}
		return draftFiles;
	}
	
	/**
	 * Determines what changes need to be made to merge an outgoing completed text message into a ghost message from the conversation.
	 * Does not perform any modifications to the database.
	 * @param conversationID The ID of the conversation to use for the message
	 * @param messageText The text of the message to try and merge
	 * @return A result of updated and deleted messages, or NULL if a merge could not be completed
	 */
	@Nullable
	public <A> GhostMergeResult<A> tryMergeMessageIntoGhost(long conversationID, String messageText) {
		if(messageText == null) return null;
		
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Finding a message with matching text
		try(Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry._ID},
				Contract.MessageEntry.COLUMN_NAME_STATE + " = ? AND " + Contract.MessageEntry.COLUMN_NAME_SENDER + " IS NULL AND " + Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT + " = ? AND " + Contract.MessageEntry.COLUMN_NAME_CHAT + " = ?",
				new String[]{Integer.toString(MessageState.ghost), messageText, Long.toString(conversationID)},
				null, null, messageSortOrderDesc, "1")) {
			//Checking if there are any results
			if(!cursor.moveToFirst()) return null;
			
			//Getting the message identifier
			long matchedMessageID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID));
			
			//Returning the result
			return new GhostMergeResult<>(matchedMessageID, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		}
	}
	
	/**
	 * Determines what changes need to be made to merge a completed outgoing attachment message into a ghost message from the conversation.
	 * Does not perform any modifications to the database.
	 * @param conversationID The ID of the conversation to use for the message
	 * @param attachments The message struct to try and merge
	 * @return A result of updated and deleted messages, or NULL if a merge could not be completed
	 */
	@Nullable
	public <A> GhostMergeResult<A> tryMergeMessageIntoGhost(long conversationID, List<Pair<byte[], A>> attachments) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		/*
		 * AirMessage must send multiple attachment files as separate messages, though the Apple Messages app will sometimes merge these outgoing messages into one.
		 * To account for this, we find the first message with a matching attachment checksum, apply the new server-provided message to that local message,
		 * and then delete any subsequent messages with matching attachment checksums.
		 */
		
		//Creating the matching values
		long matchedMessageID = -1;
		List<Long> deletedMessageIDs = new ArrayList<>(); //Messages that should be deleted
		List<Pair<Long, A>> matchedAttachments = new ArrayList<>(); //Attachments transferred from subsequently matched messages
		List<A> unmatchedAttachments = new ArrayList<>(); //Attachments that were delivered as a part of the source message, but couldn't be matched in the database
		
		//Iterating over the attachments
		for(Pair<byte[], A> pair : attachments) {
			//Ignoring if the attachment has no checksum
			if(pair.first == null) {
				unmatchedAttachments.add(pair.second);
				continue;
			}
			
			//Finding a matching row
			try(Cursor cursor = database.rawQuery("SELECT " + Contract.AttachmentEntry.TABLE_NAME + '.' + Contract.AttachmentEntry._ID + ", " + Contract.AttachmentEntry.TABLE_NAME + '.' + Contract.AttachmentEntry.COLUMN_NAME_MESSAGE + " FROM " + Contract.AttachmentEntry.TABLE_NAME +
							" JOIN " + Contract.MessageEntry.TABLE_NAME + " ON " + Contract.AttachmentEntry.COLUMN_NAME_MESSAGE + " = " + Contract.MessageEntry.TABLE_NAME + '.' + Contract.MessageEntry._ID + //Query for message + its attachment
							" WHERE " + Contract.MessageEntry.TABLE_NAME + '.' + Contract.MessageEntry.COLUMN_NAME_STATE + " = " + MessageState.ghost + //Only select ghost messages
							" AND " + Contract.MessageEntry.TABLE_NAME + '.' + Contract.MessageEntry.COLUMN_NAME_SENDER + " IS NULL" + //Only select outgoing messages
							" AND " + Contract.AttachmentEntry.TABLE_NAME + '.' + Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM + " = '" + Base64.encodeToString(pair.first, Base64.NO_WRAP) + "'" + //Only select attachments with a matching checksum
							" AND " + Contract.MessageEntry.TABLE_NAME + '.' + Contract.MessageEntry.COLUMN_NAME_CHAT + " = " + conversationID + //Only select messages in the current conversation
							(matchedMessageID == -1 ? "" : " AND " + Contract.MessageEntry.TABLE_NAME + '.' + Contract.MessageEntry._ID + " != " + matchedMessageID) + //Don't match the target message, if we have one
							" ORDER BY " + messageSortOrderDesc + //Find the most recent item
							" LIMIT 1;",
					null)) {
				//Ignoring if there are no results
				if(!cursor.moveToFirst()) {
					unmatchedAttachments.add(pair.second);
					continue;
				}
				
				//Getting the result data
				long attachmentID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.AttachmentEntry._ID));
				long messageID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_MESSAGE));
				
				//Checking if we already have a target message
				if(matchedMessageID != -1) {
					//Transferring the attachment to the target message and deleting the message
					matchedAttachments.add(new Pair<>(attachmentID, pair.second));
					deletedMessageIDs.add(messageID);
				} else {
					//Setting the target message
					matchedMessageID = messageID;
				}
			}
		}
		
		if(matchedMessageID == -1) return null;
		else return new GhostMergeResult<>(matchedMessageID, deletedMessageIDs, matchedAttachments, unmatchedAttachments);
	}
	
	/**
	 * Adds a message to a conversation by first attempting to merge it into a ghost message, and otherwise simply writing it to disk normally
	 * @param context The context to use
	 * @param conversationID The ID of the conversation to use for the message
	 * @param conversationItem The conversation item to add
	 * @return A result containing created, updated and deleted messages
	 */
	public ReplaceInsertResult mergeOrWriteConversationItem(Context context, long conversationID, Blocks.ConversationItem conversationItem) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Checking if the item is a message
		if(conversationItem instanceof Blocks.MessageInfo) {
			Blocks.MessageInfo messageStruct = (Blocks.MessageInfo) conversationItem;
			//Checking if the message is outgoing
			if(messageStruct.sender == null) {
				GhostMergeResult<Blocks.AttachmentInfo> result = null;
				if(messageStruct.text != null) {
					result = tryMergeMessageIntoGhost(conversationID, messageStruct.text);
				} else if(!messageStruct.attachments.isEmpty()) {
					result = tryMergeMessageIntoGhost(conversationID, messageStruct.attachments.stream().map(attachment -> new Pair<>(attachment.checksum, attachment)).collect(Collectors.toList()));
				}
				
				if(result != null) {
					//Reading the target message's send style value
					boolean sendStyleViewed;
					try(Cursor messageCursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED},
							Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(result.getTargetMessageID())}, null, null, null, "1")) {
						messageCursor.moveToNext();
						sendStyleViewed = messageCursor.getInt(messageCursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED)) != 0;
					}
					
					//Deleting discarded messages
					if(!result.getDiscardedMessageIDs().isEmpty()) {
						database.delete(Contract.MessageEntry.TABLE_NAME, Contract.MessageEntry._ID + " IN (" + result.getDiscardedMessageIDs().stream().map(value -> Long.toString(value)).collect(Collectors.joining(",")) + ")", null);
					}
					
					//Creating the content values
					ContentValues messageContentValues = new ContentValues();
					if(messageStruct.serverID != -1) {
						messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_SERVERID, messageStruct.serverID);
						messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED, messageStruct.serverID);
						messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET, 0);
					}
					messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_DATE, messageStruct.date);
					messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_GUID, messageStruct.guid);
					messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_STATE, messageStruct.stateCode);
					messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_ERROR, messageStruct.errorCode);
					messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_DATEREAD, messageStruct.dateRead);
					
					//Updating the message values
					database.update(Contract.MessageEntry.TABLE_NAME, messageContentValues, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(result.getTargetMessageID())});
					
					List<AttachmentInfo> messageAttachments = new ArrayList<>();
					
					//Transferring existing attachments
					for(Pair<Long, Blocks.AttachmentInfo> pair : result.getTransferAttachments()) {
						ContentValues attachmentContentValues = new ContentValues();
						attachmentContentValues.put(Contract.AttachmentEntry.COLUMN_NAME_MESSAGE, result.getTargetMessageID());
						attachmentContentValues.put(Contract.AttachmentEntry.COLUMN_NAME_GUID, pair.second.guid);
						if(pair.second.sort != -1) attachmentContentValues.put(Contract.AttachmentEntry.COLUMN_NAME_SORT, pair.second.sort);
						
						database.update(Contract.AttachmentEntry.TABLE_NAME, attachmentContentValues, Contract.AttachmentEntry._ID + " = ?", new String[]{Long.toString(pair.first)});
						
						try(Cursor attachmentCursor = database.query(Contract.AttachmentEntry.TABLE_NAME, null, Contract.AttachmentEntry._ID + " = ?", new String[]{Long.toString(pair.first)}, null, null, null, "1")) {
							if(attachmentCursor.moveToNext()) {
								AttachmentInfo attachmentInfo = loadAttachmentInfo(context, AttachmentInfoIndices.fromCursor(attachmentCursor), attachmentCursor);
								messageAttachments.add(attachmentInfo);
							}
						}
					}
					
					//Writing new attachments
					for(Blocks.AttachmentInfo attachmentStruct : result.getNewAttachments()) {
						AttachmentInfo attachmentInfo = addMessageAttachment(result.getTargetMessageID(), attachmentStruct);
						messageAttachments.add(attachmentInfo);
					}
					
					Collections.sort(messageAttachments, (attachment1, attachment2) -> Long.compare(attachment1.getSort(), attachment2.getSort()));
					
					//Writing the modifiers
					List<Pair<StickerInfo, ModifierMetadata>> stickers = addMessageStickers(context, result.getTargetMessageID(), messageStruct.stickers);
					List<Pair<TapbackInfo, ModifierMetadata>> tapbacks = addMessageTapbacks(result.getTargetMessageID(), messageStruct.tapbacks);
					
					//Creating the final message
					MessageInfo messageInfo = new MessageInfo(result.getTargetMessageID(), messageStruct.serverID, messageStruct.guid, messageStruct.date, null, messageStruct.text, messageStruct.subject, messageAttachments, messageStruct.sendEffect, sendStyleViewed, messageStruct.dateRead, messageStruct.stateCode, messageStruct.errorCode, false);
					for(Pair<StickerInfo, ModifierMetadata> pair : stickers) messageInfo.getComponentAt(pair.second.getComponentIndex()).getStickers().add(pair.first);
					for(Pair<TapbackInfo, ModifierMetadata> pair : tapbacks) messageInfo.getComponentAt(pair.second.getComponentIndex()).getTapbacks().add(pair.first);
					
					//Returning the details
					return new ReplaceInsertResult(messageInfo, Collections.emptyList(), Collections.singletonList(messageInfo), result.getDiscardedMessageIDs());
				}
			}
		}
		
		//Adding the conversation item normally
		ConversationItem addedItem = addConversationStruct(context, conversationID, conversationItem);
		return new ReplaceInsertResult(addedItem, Collections.singletonList(addedItem), Collections.emptyList(), Collections.emptyList());
	}
	
	/**
	 * Transfers an existing message from one conversation to another by first attempting to merge it into a ghost message, and otherwise simply rewriting its conversation
	 * @param context The context to use
	 * @param conversationID The ID of the conversation to use for the message
	 * @param conversationItem The conversation item to add
	 * @return A result containing created, updated and deleted messages
	 */
	public ReplaceInsertResult transferConversationItemReplaceGhost(Context context, long conversationID, ConversationItem conversationItem) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Checking if the item is a message
		if(conversationItem.getItemType() == ConversationItemType.message) {
			MessageInfo messageItem = (MessageInfo) conversationItem;
			
			//Checking if the message is outgoing and is not a ghost item
			if(messageItem.isOutgoing() && messageItem.getMessageState() != MessageState.ghost) {
				GhostMergeResult<AttachmentInfo> result = null;
				if(messageItem.getMessageText() != null) {
					result = tryMergeMessageIntoGhost(conversationID, messageItem.getMessageText());
				} else if(!messageItem.getAttachments().isEmpty() && messageItem.getAttachments().stream().allMatch(attachment -> attachment.getFileChecksum() != null)) {
					result = tryMergeMessageIntoGhost(conversationID, messageItem.getAttachments().stream().map(attachment -> new Pair<>(attachment.getFileChecksum(), attachment)).collect(Collectors.toList()));
				}
				
				if(result != null) {
					//Reading the target message's send style viewed value
					boolean sendStyleViewed;
					try(Cursor messageCursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED},
							Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(result.getTargetMessageID())}, null, null, null, "1")) {
						messageCursor.moveToNext();
						sendStyleViewed = messageCursor.getInt(messageCursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED)) != 0;
					}
					
					//Reading the source message's error value
					String sendErrorDetails;
					try(Cursor messageCursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry.COLUMN_NAME_ERRORDETAILS},
							Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(messageItem.getLocalID())}, null, null, null, "1")) {
						messageCursor.moveToNext();
						sendErrorDetails = messageCursor.getString(messageCursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_ERRORDETAILS));
					}
					
					//Deleting discarded messages
					if(!result.getDiscardedMessageIDs().isEmpty()) {
						database.delete(Contract.MessageEntry.TABLE_NAME, Contract.MessageEntry._ID + " IN (" + result.getDiscardedMessageIDs().stream().map(value -> Long.toString(value)).collect(Collectors.joining(",")) + ")", null);
					}
					
					//Creating the content values
					ContentValues messageContentValues = new ContentValues();
					if(conversationItem.getServerID() == -1) {
						messageContentValues.putNull(Contract.MessageEntry.COLUMN_NAME_SERVERID);
						try(Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED, Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET}, Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED + " = (SELECT MAX(" + Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED + ") FROM " + Contract.MessageEntry.TABLE_NAME + ")", null, null, null, Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET + " DESC", "1")) {
							if(cursor.moveToNext()) {
								//Same message, +1 offset
								messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED, cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED)));
								messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET, cursor.getInt(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET)) + 1);
							} else {
								messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED, -1);
								messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET, 0);
							}
						}
					} else {
						messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_SERVERID, messageItem.getServerID());
						messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED, messageItem.getServerID());
						messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET, 0);
					}
					messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_DATE, messageItem.getDate());
					messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_GUID, messageItem.getGuid());
					messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_STATE, messageItem.getMessageState());
					messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_ERROR, messageItem.getErrorCode());
					messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_ERRORDETAILS, sendErrorDetails);
					messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_DATEREAD, messageItem.getDateRead());
					messageContentValues.put(Contract.MessageEntry.COLUMN_NAME_CHAT, conversationID);
					
					//Updating the message values
					database.update(Contract.MessageEntry.TABLE_NAME, messageContentValues, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(result.getTargetMessageID())});
					
					List<AttachmentInfo> messageAttachments = new ArrayList<>();
					
					//Transferring existing attachments
					for(Pair<Long, AttachmentInfo> pair : result.getTransferAttachments()) {
						ContentValues attachmentContentValues = new ContentValues();
						attachmentContentValues.put(Contract.AttachmentEntry.COLUMN_NAME_MESSAGE, result.getTargetMessageID());
						attachmentContentValues.put(Contract.AttachmentEntry.COLUMN_NAME_GUID, pair.second.getGUID());
						if(pair.second.getSort() != -1) attachmentContentValues.put(Contract.AttachmentEntry.COLUMN_NAME_SORT, pair.second.getSort());
						
						database.update(Contract.AttachmentEntry.TABLE_NAME, attachmentContentValues, Contract.AttachmentEntry._ID + " = ?", new String[]{Long.toString(pair.first)});
						
						try(Cursor attachmentCursor = database.query(Contract.AttachmentEntry.TABLE_NAME, null, Contract.AttachmentEntry._ID + " = ?", new String[]{Long.toString(pair.first)}, null, null, null, "1")) {
							if(attachmentCursor.moveToNext()) {
								AttachmentInfo attachmentInfo = loadAttachmentInfo(context, AttachmentInfoIndices.fromCursor(attachmentCursor), attachmentCursor);
								messageAttachments.add(attachmentInfo);
							}
						}
					}
					
					//Transferring new attachments
					for(AttachmentInfo attachmentInfo : result.getNewAttachments()) {
						ContentValues attachmentContentValues = new ContentValues();
						attachmentContentValues.put(Contract.AttachmentEntry.COLUMN_NAME_MESSAGE, result.getTargetMessageID());
						
						database.update(Contract.AttachmentEntry.TABLE_NAME, attachmentContentValues, Contract.AttachmentEntry._ID + " = ?", new String[]{Long.toString(attachmentInfo.getLocalID())});
					}
					messageAttachments.addAll(result.getNewAttachments());
					
					Collections.sort(messageAttachments, (attachment1, attachment2) -> Long.compare(attachment1.getSort(), attachment2.getSort()));
					
					//Transferring the modifiers
					{
						ContentValues stickerContentValues = new ContentValues();
						stickerContentValues.put(Contract.StickerEntry.COLUMN_NAME_MESSAGE, result.getTargetMessageID());
						database.update(Contract.StickerEntry.TABLE_NAME, stickerContentValues, Contract.StickerEntry._ID + " = ?", new String[]{Long.toString(messageItem.getLocalID())});
					}
					{
						ContentValues tapbackContentValues = new ContentValues();
						tapbackContentValues.put(Contract.TapbackEntry.COLUMN_NAME_MESSAGE, result.getTargetMessageID());
						database.update(Contract.TapbackEntry.TABLE_NAME, tapbackContentValues, Contract.TapbackEntry._ID + " = ?", new String[]{Long.toString(messageItem.getLocalID())});
					}
					
					//Deleting the original message from the old conversation
					database.delete(Contract.MessageEntry.TABLE_NAME, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(messageItem.getLocalID())});
					
					//Creating the final message
					MessageInfo messageInfo = new MessageInfo(result.getTargetMessageID(), messageItem.getServerID(), messageItem.getGuid(), messageItem.getDate(), null, messageItem.getMessageTextInfo(), messageAttachments, messageItem.getSendStyle(), sendStyleViewed, messageItem.getDateRead(), messageItem.getMessageState(), messageItem.getErrorCode(), messageItem.isErrorDetailsAvailable(), null);
					loadApplyStickers(context, messageInfo);
					loadApplyTapbacks(messageInfo);
					
					//Returning the details
					return new ReplaceInsertResult(messageInfo, Collections.emptyList(), Collections.singletonList(messageInfo), result.getDiscardedMessageIDs());
				}
			}
		}
		
		//We couldn't replace the item, so just do a standard conversation item ownership transfer
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_CHAT, conversationID);
		getWritableDatabase().update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(conversationItem.getLocalID())});
		
		return new ReplaceInsertResult(conversationItem, Collections.singletonList(conversationItem), Collections.emptyList(), Collections.emptyList());
	}
	
	/**
	 * Represents the result of a 'smart insert' where updated messages are merged into an existing conversation thread
	 * @param <A> The attachment data as represented in memory, used to apply ghost merge changes in response to this result
	 */
	public static class GhostMergeResult<A> {
		private final long targetMessageID;
		private final List<Long> discardedMessageIDs;
		private final List<Pair<Long, A>> transferAttachments;
		private final List<A> newAttachments;
		
		/**
		 * Represents the result of a 'smart insert' where updated messages are merged into an existing conversation thread
		 * @param targetMessageID The ID of the message that is having its result updated
		 * @param discardedMessageIDs A list of message IDs of messages that should be discarded
		 * @param transferAttachments A list of attachment IDs that should be transferred to the target message
		 * @param newAttachments A list of attachments that should be newly written under the target message
		 */
		public GhostMergeResult(long targetMessageID, List<Long> discardedMessageIDs, List<Pair<Long, A>> transferAttachments, List<A> newAttachments) {
			this.targetMessageID = targetMessageID;
			this.discardedMessageIDs = discardedMessageIDs;
			this.transferAttachments = transferAttachments;
			this.newAttachments = newAttachments;
		}
		
		public long getTargetMessageID() {
			return targetMessageID;
		}
		
		public List<Long> getDiscardedMessageIDs() {
			return discardedMessageIDs;
		}
		
		public List<Pair<Long, A>> getTransferAttachments() {
			return transferAttachments;
		}
		
		public List<A> getNewAttachments() {
			return newAttachments;
		}
	}
	
	/**
	 * Writes a conversation struct to the database
	 * @param context The context to use
	 * @param conversationID The ID of the conversation to add the message to
	 * @param conversationItem The message to add
	 * @return A completed conversation item
	 */
	public ConversationItem addConversationStruct(Context context, long conversationID, Blocks.ConversationItem conversationItem) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the content values and adding the common data
		ContentValues contentValues = new ContentValues();
		if(conversationItem.serverID == -1) {
			contentValues.putNull(Contract.MessageEntry.COLUMN_NAME_SERVERID);
			try(Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED, Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET}, Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED + " = (SELECT MAX(" + Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED + ") FROM " + Contract.MessageEntry.TABLE_NAME + ")", null, null, null, Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET + " DESC", "1")) {
				if(cursor.moveToNext()) {
					//Same message, +1 offset
					contentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED, cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED)));
					contentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET, cursor.getInt(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET)) + 1);
				} else {
					contentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED, -1);
					contentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET, 0);
				}
			}
		} else {
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_SERVERID, conversationItem.serverID);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED, conversationItem.serverID);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET, 0);
		}
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_GUID, conversationItem.guid);
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_DATE, conversationItem.date);
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_CHAT, conversationID);
		
		//Checking if the item is a message
		if(conversationItem instanceof Blocks.MessageInfo) {
			//Casting the item
			Blocks.MessageInfo messageInfoStruct = (Blocks.MessageInfo) conversationItem;
			
			//Putting the content values
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_SENDER, messageInfoStruct.sender);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE, ConversationItemType.message);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT, messageInfoStruct.text);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_MESSAGESUBJECT, messageInfoStruct.subject);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_STATE, messageInfoStruct.stateCode);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ERROR, messageInfoStruct.errorCode);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_DATEREAD, messageInfoStruct.dateRead);
			if(messageInfoStruct.sendEffect != null) contentValues.put(Contract.MessageEntry.COLUMN_NAME_SENDSTYLE, messageInfoStruct.sendEffect);
			
			//Inserting the conversation into the database
			long messageLocalID;
			try {
				messageLocalID = database.insertOrThrow(Contract.MessageEntry.TABLE_NAME, null, contentValues);
			} catch(SQLiteConstraintException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Returning null
				return null;
			}
			
			//Adding the attachments
			List<AttachmentInfo> attachments = messageInfoStruct.attachments.stream().map(attachment -> addMessageAttachment(messageLocalID, attachment)).filter(Objects::nonNull).collect(Collectors.toList());
			
			//Adding the modifiers
			List<Pair<StickerInfo, ModifierMetadata>> stickers = addMessageStickers(context, messageLocalID, messageInfoStruct.stickers);
			List<Pair<TapbackInfo, ModifierMetadata>> tapbacks = addMessageTapbacks(messageLocalID, messageInfoStruct.tapbacks);
			
			//Creating the message info
			MessageInfo messageInfo = new MessageInfo(messageLocalID, messageInfoStruct.serverID, messageInfoStruct.guid, messageInfoStruct.date, messageInfoStruct.sender, messageInfoStruct.text, messageInfoStruct.subject, attachments, messageInfoStruct.sendEffect, false, messageInfoStruct.dateRead, messageInfoStruct.stateCode, messageInfoStruct.errorCode, false);
			for(Pair<StickerInfo, ModifierMetadata> pair : stickers) messageInfo.getComponentAt(pair.second.getComponentIndex()).getStickers().add(pair.first);
			for(Pair<TapbackInfo, ModifierMetadata> pair : tapbacks) messageInfo.getComponentAt(pair.second.getComponentIndex()).getTapbacks().add(pair.first);
			
			//Returning the message info
			return messageInfo;
		}
		//Otherwise checking if the item is a group action
		else if(conversationItem instanceof Blocks.GroupActionInfo) {
			//Casting the item
			Blocks.GroupActionInfo groupActionInfoStruct = (Blocks.GroupActionInfo) conversationItem;
			
			//Putting the content values
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_SENDER, groupActionInfoStruct.agent);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE, ConversationItemType.member);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ITEMSUBTYPE, groupActionInfoStruct.groupActionType);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_OTHER, groupActionInfoStruct.other);
			
			//Inserting the action into the database
			long localID;
			try {
				localID = database.insertOrThrow(Contract.MessageEntry.TABLE_NAME, null, contentValues);
			} catch(SQLiteConstraintException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Returning null
				return null;
			}
			
			//Returning the event
			return new ChatMemberAction(localID, groupActionInfoStruct.serverID, groupActionInfoStruct.guid, groupActionInfoStruct.date, groupActionInfoStruct.groupActionType, groupActionInfoStruct.agent, groupActionInfoStruct.other);
		}
		//Otherwise checking if the item is a chat rename
		else if(conversationItem instanceof Blocks.ChatRenameActionInfo) {
			//Casting the item
			Blocks.ChatRenameActionInfo chatRenameInfoStruct = (Blocks.ChatRenameActionInfo) conversationItem;
			
			//Putting the content values
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_SENDER, chatRenameInfoStruct.agent);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE, ConversationItemType.chatRename);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_OTHER, chatRenameInfoStruct.newChatName);
			
			//Inserting the action into the database
			long localID;
			try {
				localID = database.insertOrThrow(Contract.MessageEntry.TABLE_NAME, null, contentValues);
			} catch(SQLiteConstraintException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Returning null
				return null;
			}
			
			//Returning the event
			return new ChatRenameAction(localID, chatRenameInfoStruct.serverID, chatRenameInfoStruct.guid, chatRenameInfoStruct.date, chatRenameInfoStruct.agent, chatRenameInfoStruct.newChatName);
		}
		
		//Returning null
		return null;
	}
	
	/**
	 * Adds a new conversation item to a conversation
	 * @param conversationID The ID of the conversation
	 * @param conversationItem The conversation item to add
	 * @param offsetRequired Whether to use offsets conversation items' server IDs
	 * @return The ID the item was inserted at
	 */
	public long addConversationItem(long conversationID, ConversationItem conversationItem, boolean offsetRequired) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the content values and adding the common data
		ContentValues contentValues = new ContentValues();
		if(offsetRequired) {
			if(conversationItem.getServerID() == -1) {
				contentValues.putNull(Contract.MessageEntry.COLUMN_NAME_SERVERID);
				try(Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED, Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET},
						Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED + " = (SELECT MAX(" + Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED + ") FROM " + Contract.MessageEntry.TABLE_NAME + ")", null,
						null, null, Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET + " DESC", "1")) {
					if(cursor.moveToNext()) {
						//Same message, +1 offset
						contentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED, cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED)));
						contentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET, cursor.getInt(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET)) + 1);
					} else {
						contentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED, -1);
						contentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET, 0);
					}
				}
			} else {
				contentValues.put(Contract.MessageEntry.COLUMN_NAME_SERVERID, conversationItem.getServerID());
				contentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKED, conversationItem.getServerID());
				contentValues.put(Contract.MessageEntry.COLUMN_NAME_SORTID_LINKEDOFFSET, 0);
			}
		}
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_GUID, conversationItem.getGuid());
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_DATE, conversationItem.getDate());
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_CHAT, conversationID);
		
		long itemLocalID;
		
		//Checking if the item is a message
		if(conversationItem.getItemType() == ConversationItemType.message) {
			//Casting the item
			MessageInfo messageInfo = (MessageInfo) conversationItem;
			
			//Putting the content values
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_SENDER, messageInfo.getSender());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE, messageInfo.getItemType());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT, messageInfo.getMessageText());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_MESSAGESUBJECT, messageInfo.getMessageSubject());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_STATE, messageInfo.getMessageState());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ERROR, messageInfo.getErrorCode());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ERRORDETAILS, messageInfo.getErrorDetails());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_DATEREAD, messageInfo.getDateRead());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_SENDSTYLE, messageInfo.getSendStyle());
			
			//Inserting the conversation into the database
			try {
				itemLocalID = database.insertOrThrow(Contract.MessageEntry.TABLE_NAME, null, contentValues);
			} catch(SQLiteConstraintException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Returning
				return -1;
			}
			
			//Iterating over the attachments
			for(AttachmentInfo attachment : messageInfo.getAttachments()) {
				//Creating the content values
				contentValues.clear();
				contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_GUID, attachment.getGUID());
				contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_MESSAGE, itemLocalID);
				contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILENAME, attachment.getFileName());
				contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILETYPE, attachment.getContentType());
				if(attachment.getFileSize() != -1) contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILESIZE, attachment.getFileSize());
				if(attachment.getFile() != null) contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH, AttachmentStorageHelper.getRelativePath(MainApplication.getInstance(), attachment.getFile()));
				contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM, attachment.getFileChecksum());
				if(attachment.getSort() != -1) contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_SORT, attachment.getSort());
				
				//Inserting the attachment into the database
				long attachmentLocalID;
				try {
					attachmentLocalID = database.insertOrThrow(Contract.AttachmentEntry.TABLE_NAME, null, contentValues);
				} catch(SQLiteConstraintException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Skipping the remainder of the iteration
					continue;
				}
				
				//Setting the local ID
				attachment.setLocalID(attachmentLocalID);
			}
		}
		//Otherwise checking if the item is a group action
		else if(conversationItem.getItemType() == ConversationItemType.member) {
			//Casting the item
			ChatMemberAction chatMemberItem = (ChatMemberAction) conversationItem;
			
			//Putting the content values
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_SENDER, chatMemberItem.getAgent());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE, chatMemberItem.getItemType());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ITEMSUBTYPE, chatMemberItem.getActionType());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_OTHER, chatMemberItem.getOther());
			
			//Inserting the action into the database
			try {
				itemLocalID = database.insertOrThrow(Contract.AttachmentEntry.TABLE_NAME, null, contentValues);
			} catch(SQLiteConstraintException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				return -1;
			}
		}
		//Otherwise checking if the item is a chat rename
		else if(conversationItem.getItemType() == ConversationItemType.chatRename) {
			//Casting the item
			ChatRenameAction chatRenameItem = (ChatRenameAction) conversationItem;
			
			//Putting the content values
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_SENDER, chatRenameItem.getAgent());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE, chatRenameItem.getItemType());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_OTHER, chatRenameItem.getTitle());
			
			//Inserting the action into the database
			try {
				itemLocalID = database.insertOrThrow(Contract.AttachmentEntry.TABLE_NAME, null, contentValues);
			} catch(SQLiteConstraintException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				return -1;
			}
		} else {
			throw new IllegalArgumentException("Cannot accept conversation item type " + conversationItem.getItemType());
		}
		
		return itemLocalID;
	}
	
	/**
	 * Writes a message attachment block to the database
	 * @param messageID The ID of the message to add the attachment to
	 * @param attachmentStruct The attachment to write
	 * @return The complete attachment info
	 */
	private AttachmentInfo addMessageAttachment(long messageID, Blocks.AttachmentInfo attachmentStruct) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_GUID, attachmentStruct.guid);
		contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_MESSAGE, messageID);
		contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILENAME, attachmentStruct.name);
		contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILETYPE, attachmentStruct.type);
		if(attachmentStruct.size != -1) contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILESIZE, attachmentStruct.size);
		if(attachmentStruct.checksum != null) contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM, Base64.encodeToString(attachmentStruct.checksum, Base64.NO_WRAP));
		if(attachmentStruct.sort != -1) contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_SORT, attachmentStruct.sort);
		
		//Inserting the attachment into the database
		long localID;
		try {
			localID = getWritableDatabase().insertOrThrow(Contract.AttachmentEntry.TABLE_NAME, null, contentValues);
		} catch(SQLiteConstraintException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning null
			return null;
		}
		
		//Creating and returning the attachment
		return new AttachmentInfo(localID, attachmentStruct.guid, attachmentStruct.name, attachmentStruct.type, attachmentStruct.size, attachmentStruct.sort, attachmentStruct.checksum);
	}
	
	/**
	 * Writes a message attachment to the database
	 * @param context The context to use
	 * @param messageID The ID of the message to add the attachment to
	 * @param attachment The attachment to write
	 * @return The complete attachment info
	 */
	private boolean addMessageAttachment(Context context, long messageID, AttachmentInfo attachment) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_GUID, attachment.getGUID());
		contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_MESSAGE, messageID);
		contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILENAME, attachment.getFileName());
		contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILETYPE, attachment.getContentType());
		if(attachment.getFileSize() != -1) contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILESIZE, attachment.getFileSize());
		if(attachment.getFile() != null) contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH, AttachmentStorageHelper.getRelativePath(context, attachment.getFile()));
		contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM, attachment.getFileChecksum());
		if(attachment.getSort() != -1) contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_SORT, attachment.getSort());
		
		//Inserting the attachment into the database
		long attachmentLocalID;
		try {
			attachmentLocalID = getWritableDatabase().insertOrThrow(Contract.AttachmentEntry.TABLE_NAME, null, contentValues);
		} catch(SQLiteConstraintException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning false
			return false;
		}
		
		//Setting the local ID
		attachment.setLocalID(attachmentLocalID);
		
		//Returning true
		return true;
	}
	
	/**
	 * Gets the local ID of a message from its GUID
	 * @param messageGUID The GUID of the message
	 * @return The message's local ID, or -1 if not found
	 */
	public long messageGUIDToLocalID(String messageGUID) {
		try(Cursor cursor = getReadableDatabase().query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry._ID},
				Contract.MessageEntry.COLUMN_NAME_GUID + " = ?", new String[]{messageGUID}, null, null, null)) {
			if(!cursor.moveToNext()) return -1;
			return cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID));
		}
	}
	
	public void setUnreadMessageCount(long conversationID, int count) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_UNREADMESSAGECOUNT, count);
		
		//Updating the database
		getWritableDatabase().update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(conversationID)});
	}
	
	public void setAllUnreadClear() {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_UNREADMESSAGECOUNT, 0);
		
		//Updating the database
		getWritableDatabase().update(Contract.ConversationEntry.TABLE_NAME, contentValues, null, null);
	}
	
	public void incrementUnreadMessageCount(long conversationID) {
		getWritableDatabase().execSQL("UPDATE " + Contract.ConversationEntry.TABLE_NAME + " SET " + Contract.ConversationEntry.COLUMN_NAME_UNREADMESSAGECOUNT + " = " + Contract.ConversationEntry.COLUMN_NAME_UNREADMESSAGECOUNT + " + 1 WHERE " + Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(conversationID)});
	}
	
	/**
	 * Writes a sticker message to the database
	 * @param sticker The sticker message to write
	 * @return A pair of sticker's complete object and its positioning metadata
	 */
	public Pair<StickerInfo, ModifierMetadata> addMessageSticker(Context context, Blocks.StickerModifierInfo sticker) {
		//Fetching the local ID of the associated message
		long messageID;
		try(Cursor cursor = getReadableDatabase().query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry._ID}, Contract.MessageEntry.COLUMN_NAME_GUID + " = ?", new String[]{sticker.message},
				null, null, null, "1")) {
			//Returning null if the cursor is empty (the associated message could not be found)
			if(!cursor.moveToNext()) return null;
			
			//Getting the ID
			messageID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID));
		}
		
		return addMessageSticker(context, messageID, sticker);
	}
	
	/**
	 * Writes a sticker message to the database
	 * @param messageID The ID of the message to add the sticker to
	 * @param sticker The sticker message to write
	 * @return A pair of sticker's complete object and its positioning metadata
	 */
	public Pair<StickerInfo, ModifierMetadata> addMessageSticker(Context context, long messageID, Blocks.StickerModifierInfo sticker) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.StickerEntry.COLUMN_NAME_GUID, sticker.fileGuid);
		contentValues.put(Contract.StickerEntry.COLUMN_NAME_MESSAGE, messageID);
		contentValues.put(Contract.StickerEntry.COLUMN_NAME_MESSAGEINDEX, sticker.messageIndex);
		contentValues.put(Contract.StickerEntry.COLUMN_NAME_SENDER, sticker.sender);
		contentValues.put(Contract.StickerEntry.COLUMN_NAME_DATE, sticker.date);
		
		//Inserting the entry
		long stickerID;
		try {
			stickerID = database.insert(Contract.StickerEntry.TABLE_NAME, null, contentValues);
		} catch(SQLiteConstraintException exception) {
			exception.printStackTrace();
			return null;
		}
		
		//Saving the sticker data to disk
		String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(sticker.type);
		File targetFile = AttachmentStorageHelper.prepareContentFile(context, AttachmentStorageHelper.dirNameSticker, stickerID + (extension != null ? '.' + extension : ""));
		
		try(FileOutputStream outputStream = new FileOutputStream(targetFile, false)) {
			outputStream.write(sticker.data);
		} catch(IOException exception) {
			exception.printStackTrace();
			AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameSticker, targetFile);
			return null;
		}
		
		//Updating the sticker's file path
		contentValues = new ContentValues();
		contentValues.put(Contract.StickerEntry.COLUMN_NAME_FILEPATH, AttachmentStorageHelper.getRelativePath(context, targetFile));
		database.update(Contract.StickerEntry.TABLE_NAME, contentValues, Contract.StickerEntry._ID + " = ?", new String[]{Long.toString(stickerID)});
		
		//Returning the sticker
		return new Pair<>(new StickerInfo(stickerID, sticker.fileGuid, sticker.sender, sticker.date, targetFile), new ModifierMetadata(messageID, sticker.messageIndex));
	}
	
	/**
	 * Writes a list of stickers to disk
	 * @param context The context to use
	 * @param messageID The ID of the message to add the stickers to
	 * @param stickers The list of stickers to add
	 * @return A list of added stickers with their metadata
	 */
	private List<Pair<StickerInfo, ModifierMetadata>> addMessageStickers(Context context, long messageID, List<Blocks.StickerModifierInfo> stickers) {
		return stickers.stream().map(sticker -> Optional.ofNullable(addMessageSticker(context, messageID, sticker)))
				.filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
	}
	
	/**
	 * Writes a tapback message to the database
	 * @param tapback The tapback message to write
	 * @return A pair of tapback's complete object and its positioning metadata
	 */
	public Pair<TapbackInfo, ModifierMetadata> addMessageTapback(Blocks.TapbackModifierInfo tapback) {
		//Fetching the local ID of the associated message
		long messageID;
		try(Cursor cursor = getReadableDatabase().query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry._ID}, Contract.MessageEntry.COLUMN_NAME_GUID + " = ?", new String[]{tapback.message},
				null, null, null, "1")) {
			//Returning null if the cursor is empty (the associated message could not be found)
			if(!cursor.moveToNext()) return null;
			
			//Getting the ID
			messageID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID));
		}
		
		return addMessageTapback(messageID, tapback);
	}
	
	/**
	 * Writes a tapback message to the database
	 * @param messageID The ID of the message to add the tapback to
	 * @param tapback The tapback message to write
	 * @return A pair of tapback's complete object and its positioning metadata
	 */
	public Pair<TapbackInfo, ModifierMetadata> addMessageTapback(long messageID, Blocks.TapbackModifierInfo tapback) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.TapbackEntry.COLUMN_NAME_MESSAGE, messageID);
		contentValues.put(Contract.TapbackEntry.COLUMN_NAME_MESSAGEINDEX, tapback.messageIndex);
		contentValues.put(Contract.TapbackEntry.COLUMN_NAME_SENDER, tapback.sender);
		contentValues.put(Contract.TapbackEntry.COLUMN_NAME_CODE, tapback.tapbackType);
		
		//Inserting the entry
		long tapbackID;
		try {
			tapbackID = database.insert(Contract.TapbackEntry.TABLE_NAME, null, contentValues);
		} catch(SQLiteConstraintException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			return null;
		}
		
		//Returning the tapback
		return new Pair<>(new TapbackInfo(tapbackID, tapback.sender, tapback.tapbackType), new ModifierMetadata(messageID, tapback.messageIndex));
	}
	
	/**
	 * Writes a list of tapbacks to disk
	 * @param messageID The ID of the message to add the tapback to
	 * @param tapbacks The list of tapbacks to add
	 * @return A list of added tapbacks with their metadata
	 */
	private List<Pair<TapbackInfo, ModifierMetadata>> addMessageTapbacks(long messageID, List<Blocks.TapbackModifierInfo> tapbacks) {
		return tapbacks.stream().map(tapback -> Optional.ofNullable(addMessageTapback(messageID, tapback)))
				.filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
	}
	
	/**
	 * Reads the raw data of a sticker to memory
	 * @param context The context to use
	 * @param stickerID The ID of the sticker to read
	 * @return The sticker's data, or NULL if unavailable
	 */
	public byte[] getStickerData(Context context, long stickerID) {
		try(Cursor cursor = getReadableDatabase().query(Contract.StickerEntry.TABLE_NAME, new String[]{Contract.StickerEntry.COLUMN_NAME_FILEPATH}, Contract.StickerEntry._ID + " = ?", new String[]{Long.toString(stickerID)}, null, null, null, "1")) {
			if(!cursor.moveToNext()) return null;
			String path = cursor.getString(cursor.getColumnIndexOrThrow(Contract.StickerEntry.COLUMN_NAME_FILEPATH));
			if(path == null) return null;
			File file = AttachmentStorageHelper.getAbsolutePath(context, path);
			byte[] fileBytes = new byte[(int) file.length()];
			try(InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
				inputStream.read(fileBytes, 0, fileBytes.length);
			} catch(IOException exception) {
				exception.printStackTrace();
				return null;
			}
			return fileBytes;
		}
	}
	
	public void updateConversationInfo(ConversationInfo conversationInfo, boolean updateMembers) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		
		//Updating the conversation data
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_GUID, conversationInfo.getGUID());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_STATE, conversationInfo.getState());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_SERVICE, conversationInfo.getServiceType());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_NAME, conversationInfo.getTitle());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_COLOR, conversationInfo.getConversationColor());
		
		try {
			database.update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(conversationInfo.getLocalID())});
		} catch(SQLiteConstraintException exception) {
			exception.printStackTrace();
			return;
		}
		
		//Checking if members should be updated
		if(updateMembers) {
			//Looping through all members
			for(MemberInfo member : conversationInfo.getMembers()) {
				//Putting the data
				contentValues = new ContentValues();
				contentValues.put(Contract.MemberEntry.COLUMN_NAME_MEMBER, member.getAddress());
				contentValues.put(Contract.MemberEntry.COLUMN_NAME_CHAT, conversationInfo.getLocalID());
				contentValues.put(Contract.MemberEntry.COLUMN_NAME_COLOR, member.getColor());
				
				//Inserting the values into the conversation / users join table
				database.insert(Contract.MemberEntry.TABLE_NAME, null, contentValues);
			}
		}
	}
	
	public void copyConversationInfo(ConversationInfo sourceConversation, ConversationInfo targetConversation, boolean updateMembers) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		
		//Updating the conversation data
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_GUID, sourceConversation.getGUID());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_STATE, sourceConversation.getState());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER, sourceConversation.getServiceHandler());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_SERVICE, sourceConversation.getServiceType());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_NAME, sourceConversation.getTitle());
		
		database.update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(targetConversation.getLocalID())});
		
		//Checking if members should be updated
		if(updateMembers) {
			//Looping through all members
			for(MemberInfo member : sourceConversation.getMembers()) {
				//Putting the data
				contentValues = new ContentValues();
				contentValues.put(Contract.MemberEntry.COLUMN_NAME_MEMBER, member.getAddress());
				contentValues.put(Contract.MemberEntry.COLUMN_NAME_CHAT, targetConversation.getLocalID());
				contentValues.put(Contract.MemberEntry.COLUMN_NAME_COLOR, member.getColor());
				
				//Inserting the values into the conversation / users join table
				database.insert(Contract.MemberEntry.TABLE_NAME, null, contentValues);
			}
		}
	}
	
	public void updateConversationColor(long conversationID, int newColor) {
		//Creating and setting the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_COLOR, newColor);
		
		//Updating the conversation's color in the database
		getWritableDatabase().update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(conversationID)});
	}
	
	public void updateMemberColor(long conversationID, String member, int color) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the content values
		ContentValues contentValues;
		
		//Updating the user's color in the database
		contentValues = new ContentValues();
		contentValues.put(Contract.MemberEntry.COLUMN_NAME_COLOR, color);
		database.update(Contract.MemberEntry.TABLE_NAME, contentValues, Contract.MemberEntry.COLUMN_NAME_CHAT + " = ? AND " + Contract.MemberEntry.COLUMN_NAME_MEMBER + " = ?", new String[]{Long.toString(conversationID), member});
	}
	
	public void updateMemberColors(long conversationID, MemberInfo[] members) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the content values
		ContentValues contentValues;
		
		//Iterating over the members
		for(MemberInfo member : members) {
			//Updating the user's color in the database
			contentValues = new ContentValues();
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_COLOR, member.getColor());
			database.update(Contract.MemberEntry.TABLE_NAME, contentValues, Contract.MemberEntry.COLUMN_NAME_CHAT + " = ? AND " + Contract.MemberEntry.COLUMN_NAME_MEMBER + " = ?", new String[]{Long.toString(conversationID), member.getAddress()});
		}
	}
	
	public void updateConversationTitle(long conversationID, String title) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_NAME, title);
		
		//Updating the entry
		getWritableDatabase().update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(conversationID)});
	}
	
	public void addConversationMember(long chatID, String memberName, int memberColor) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Inserting the content
		/* writableDatabase.execSQL("INSERT INTO " + Contract.MemberEntry.TABLE_NAME + '(' + Contract.MemberEntry.COLUMN_NAME_MEMBER + ',' + Contract.MemberEntry.COLUMN_NAME_CHAT + ',' + Contract.MemberEntry.COLUMN_NAME_COLOR + ')' +
				" SELECT " + chatID + ',' + member.getServiceColor() + ',' + escapedMemberName +
				" WHERE NOT EXISTS (SELECT 1 FROM " + Contract.MemberEntry.TABLE_NAME + " WHERE " + Contract.MemberEntry.COLUMN_NAME_CHAT + '=' + chatID + " AND " + Contract.MemberEntry.COLUMN_NAME_MEMBER + '=' + escapedMemberName); */
		
		//Checking if the member is already listed
		try(Cursor cursor = database.query(Contract.MemberEntry.TABLE_NAME,
				new String[]{Contract.MemberEntry.COLUMN_NAME_MEMBER}, //Any value to reduce number of columns selected (null returns all columns)
				Contract.MemberEntry.COLUMN_NAME_CHAT + "=? AND " + Contract.MemberEntry.COLUMN_NAME_MEMBER + "=?",
				new String[]{Long.toString(chatID), memberName},
				null, null, null, "1")) {
			//Returning if there are results
			if(cursor.getCount() > 0) return;
		}
		
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.MemberEntry.COLUMN_NAME_MEMBER, memberName);
		contentValues.put(Contract.MemberEntry.COLUMN_NAME_CHAT, chatID);
		contentValues.put(Contract.MemberEntry.COLUMN_NAME_COLOR, memberColor);
		
		//Inserting the member
		database.insert(Contract.MemberEntry.TABLE_NAME, null, contentValues);
	}
	
	public void removeConversationMember(long chatID, String member) {
		//Removing the content
		getWritableDatabase().delete(Contract.MemberEntry.TABLE_NAME, Contract.MemberEntry.COLUMN_NAME_CHAT + " = ? AND " + Contract.MemberEntry.COLUMN_NAME_MEMBER + " = ?", new String[]{Long.toString(chatID), member});
	}
	
	/**
	 * Deletes a conversation from the database, as well as any associated data saved on disk
	 * @param context The context to use
	 * @param conversationID The ID of the conversation to delete
	 */
	public void deleteConversation(Context context, long conversationID) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Deleting the conversation
		database.delete(Contract.ConversationEntry.TABLE_NAME, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(conversationID)});
		
		//Deleting all related messages
		try(Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry._ID}, Contract.MessageEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(conversationID)}, null, null, null)) {
			int columnIndexID = cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID);
			while(cursor.moveToNext()) deleteMessage(context, cursor.getLong(columnIndexID));
		}
		
		//Deleting all related members
		database.delete(Contract.MemberEntry.TABLE_NAME, Contract.MemberEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(conversationID)});
	}
	
	/**
	 * Deletes all conversations under a specified service handler
	 * @param context The context to use
	 * @param serviceHandler The service handler to target
	 * @return An array of the local IDs of all deleted conversations
	 */
	public long[] deleteConversationsByServiceHandler(Context context, @ServiceHandler int serviceHandler) {
		//Deleting all conversations meeting the selection
		SQLiteDatabase database = getReadableDatabase();
		long[] deletedConversations;
		try(Cursor cursor = database.query(Contract.ConversationEntry.TABLE_NAME, new String[]{Contract.ConversationEntry._ID},
				Contract.ConversationEntry.COLUMN_NAME_SERVICEHANDLER + " = ?", new String[]{Integer.toString(serviceHandler)},
				null, null, null)) {
			int columnIndexID = cursor.getColumnIndexOrThrow(Contract.ConversationEntry._ID);
			
			deletedConversations = new long[cursor.getCount()];
			
			int i = 0;
			while(cursor.moveToNext()) {
				long conversationID = cursor.getLong(columnIndexID);
				deletedConversations[i++] = conversationID;
				deleteConversation(context, conversationID);
			}
		}
		
		return deletedConversations;
	}
	
	/**
	 * Deletes a message from the database, as well as any associated data saved on disk
	 * @param context The context to use
	 * @param messageID The ID of the message to delete
	 */
	public void deleteMessage(Context context, long messageID) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Deleting the message entries
		database.delete(Contract.MessageEntry.TABLE_NAME, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(messageID)});
		
		//Deleting associated attachment files
		try(Cursor cursor = database.query(Contract.AttachmentEntry.TABLE_NAME, new String[]{Contract.AttachmentEntry.COLUMN_NAME_FILEPATH},
				Contract.AttachmentEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(messageID)},
				null, null, null, null)) {
			int iPath = cursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH);
			
			while(cursor.moveToNext()) {
				String path = cursor.getString(iPath);
				if(path != null) {
					AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameAttachment, AttachmentStorageHelper.getAbsolutePath(context, path));
				}
			}
		}
		//Deleting associated attachment entries
		database.delete(Contract.AttachmentEntry.TABLE_NAME, Contract.AttachmentEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(messageID)});
		
		//Deleting associated sticker files
		try(Cursor cursor = database.query(Contract.StickerEntry.TABLE_NAME, new String[]{Contract.StickerEntry.COLUMN_NAME_FILEPATH},
				Contract.StickerEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(messageID)},
				null, null, null, null)) {
			int iPath = cursor.getColumnIndexOrThrow(Contract.StickerEntry.COLUMN_NAME_FILEPATH);
			
			while(cursor.moveToNext()) {
				String path = cursor.getString(iPath);
				AttachmentStorageHelper.deleteContentFile(AttachmentStorageHelper.dirNameSticker, AttachmentStorageHelper.getAbsolutePath(context, path));
			}
		}
		//Deleting associated sticker entries
		database.delete(Contract.StickerEntry.TABLE_NAME, Contract.StickerEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(messageID)});
		
		//Deleting associated tapback entries
		database.delete(Contract.TapbackEntry.TABLE_NAME, Contract.TapbackEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(messageID)});
	}
	
	/**
	 * Sets the muted state of a conversation
	 */
	public void updateConversationMuted(long conversationID, boolean muted) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_MUTED, muted);
		
		//Updating the conversation
		getWritableDatabase().update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(conversationID)});
	}
	
	/**
	 * Sets the archived state of a conversation
	 */
	public void updateConversationArchived(long conversationID, boolean archived) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_ARCHIVED, archived);
		
		//Updating the conversation
		getWritableDatabase().update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(conversationID)});
	}
	
	/**
	 * Sets the draft message of a conversation
	 * @param conversationID The ID of the conversation to update
	 * @param value The draft message's text value
	 * @param time The time this update was made
	 */
	public void updateConversationDraftMessage(long conversationID, String value, long time) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_DRAFTMESSAGE, value);
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_DRAFTUPDATETIME, time);
		
		//Updating the conversation
		getWritableDatabase().update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(conversationID)});
	}
	
	/* void setConversationLastViewTime(long conversationID, long lastViewTime) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_LASTVIEWED, lastViewTime);
		
		//Updating the database
		database.update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(conversationID)});
	} */
	
	public void updateMessageErrorCode(long messageID, int errorCode, String details) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_ERROR, errorCode);
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_ERRORDETAILS, details);
		
		//Updating the database
		getWritableDatabase().update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(messageID)});
	}
	
	public String getMessageErrorDetails(long messageID) {
		try(Cursor cursor = getReadableDatabase().query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry.COLUMN_NAME_ERRORDETAILS}, Contract.StickerEntry._ID + " = ?", new String[]{Long.toString(messageID)}, null, null, null, "1")) {
			if(cursor.moveToNext()) return cursor.getString(0);
			return null;
		}
	}
	
	/**
	 * Updates the state of a message
	 * @param localID The local ID of the message
	 * @param state The state code of the message
	 */
	public void updateMessageState(long localID, int state) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_STATE, state);
		
		//Updating the entry
		getWritableDatabase().update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(localID)});
	}
	
	/**
	 * Updates the state of a message
	 * @param localID The local ID of the message
	 * @param state The state code of the message
	 * @param dateRead The date the message was read by its recipient
	 */
	public void updateMessageState(long localID, int state, long dateRead) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_STATE, state);
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_DATEREAD, dateRead);
		
		//Updating the entry
		getWritableDatabase().update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(localID)});
	}
	
	/**
	 * Removes a tapback
	 * @param tapback The tapback block to remove
	 * @return The removed tapback details, or NULL if unavailable
	 */
	public Pair<TapbackInfo, ModifierMetadata> removeMessageTapback(Blocks.TapbackModifierInfo tapback) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Fetching the local ID of the associated message
		long messageID;
		try(Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry._ID}, Contract.MessageEntry.COLUMN_NAME_GUID + " = ?", new String[]{tapback.message}, null, null, null, "1")) {
			if(!cursor.moveToNext()) return null;
			messageID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID));
		}
		
		//Fetching the local ID of the tapback
		long tapbackID;
		try(Cursor cursor = database.query(Contract.TapbackEntry.TABLE_NAME, new String[]{Contract.TapbackEntry._ID}, Contract.TapbackEntry.COLUMN_NAME_MESSAGE + " = ? AND " + Contract.TapbackEntry.COLUMN_NAME_MESSAGEINDEX + " = ? AND " + Contract.TapbackEntry.COLUMN_NAME_SENDER + " = ?", new String[]{Long.toString(messageID), Integer.toString(tapback.messageIndex), tapback.sender}, null, null, null, "1")) {
			if(!cursor.moveToNext()) return null;
			tapbackID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.TapbackEntry._ID));
		}
		
		//Deleting the tapback
		database.delete(Contract.TapbackEntry.TABLE_NAME, Contract.TapbackEntry._ID + " = ?", new String[]{Long.toString(tapbackID)});
		
		//Returning the tapback information
		return new Pair<>(new TapbackInfo(tapbackID, tapback.sender, tapback.tapbackType), new ModifierMetadata(messageID, tapback.messageIndex));
	}
	
	public void markSendStyleViewed(long messageID) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED, 1);
		getWritableDatabase().update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(messageID)});
	}
	
	public void setMessagePreviewState(long messageID, int state) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_PREVIEW_STATE, state);
		
		//Updating the database
		database.update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(messageID)});
	}
	
	public void setMessagePreviewData(long messageID, MessagePreviewInfo messagePreview) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		ContentValues contentValues = new ContentValues();
		
		//Adding the preview information
		long previewID;
		{
			contentValues.put(Contract.MessagePreviewEntry.COLUMN_NAME_TYPE, messagePreview.getType());
			contentValues.put(Contract.MessagePreviewEntry.COLUMN_NAME_DATA, messagePreview.getData());
			contentValues.put(Contract.MessagePreviewEntry.COLUMN_NAME_TARGET, messagePreview.getTarget());
			contentValues.put(Contract.MessagePreviewEntry.COLUMN_NAME_TITLE, messagePreview.getTitle());
			contentValues.put(Contract.MessagePreviewEntry.COLUMN_NAME_SUBTITLE, messagePreview.getSubtitle());
			contentValues.put(Contract.MessagePreviewEntry.COLUMN_NAME_CAPTION, messagePreview.getCaption());
			
			previewID = database.insert(Contract.MessagePreviewEntry.TABLE_NAME, null, contentValues);
		}
		
		//Updating the message
		{
			contentValues.clear();
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_PREVIEW_STATE, MessagePreviewState.available);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_PREVIEW_ID, previewID);
			
			database.update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(messageID)});
		}
	}
	
	private static String getConversationSortByDesc(ConversationInfo conversationInfo) {
		return getConversationBySortDesc(conversationInfo.getServiceHandler());
	}
	
	private static String getConversationBySortDesc(@ServiceHandler int serviceHandler) {
		//When using AM bridge, a more advanced ordering system must be used to ensure messages are properly displayed. Otherwise, they can simply be sorted by date.
		return serviceHandler == ServiceHandler.appleBridge ? messageSortOrderDesc : messageSortOrderDescSimple;
	}
	
	/* static List<BlockedAddresses.BlockedAddress> fetchBlockedAddresses(SQLiteDatabase readableDatabase) {
		//Querying the database
		Cursor cursor = readableDatabase.query(Contract.BlockedEntry.TABLE_NAME, new String[]{Contract.BlockedEntry.COLUMN_NAME_ADDRESS, Contract.BlockedEntry.COLUMN_NAME_BLOCKCOUNT}, null, null, null, null, null);
		
		//Reading the results
		int indexAddress = cursor.getColumnIndexOrThrow(Contract.BlockedEntry.COLUMN_NAME_ADDRESS);
		int indexBlockCount = cursor.getColumnIndexOrThrow(Contract.BlockedEntry.COLUMN_NAME_BLOCKCOUNT);
		
		//Compiling the results into a list
		List<BlockedAddresses.BlockedAddress> list = new ArrayList<>();
		while(cursor.moveToNext()) {
			String address = cursor.getString(indexAddress);
			int blockCount = cursor.getInt(indexBlockCount);
			list.add(new BlockedAddresses.BlockedAddress(address, Constants.normalizeAddress(address), blockCount));
		}
		
		//Cleaning up and returning
		cursor.close();
		return list;
	} */
	
	public static abstract class LazyLoader<T> {
		SQLiteDatabase database;
		Cursor cursor;
		
		public void initialize(SQLiteDatabase database, Cursor cursor) {
			this.database = database;
			this.cursor = cursor;
		}
		
		public void setCursorPosition(int cursorPosition) {
			cursor.moveToPosition(cursorPosition);
		}
		
		public abstract List<T> loadNextChunk(Context context);
	}
	
	public static class ConversationLazyLoader extends LazyLoader<ConversationItem> {
		private final DatabaseManager databaseManager;
		private final ConversationItemIndices conversationItemIndices;
		
		public ConversationLazyLoader(DatabaseManager databaseManager, ConversationInfo conversationInfo) {
			this.databaseManager = databaseManager;
			
			//Building the query
			SQLiteDatabase database = databaseManager.getReadableDatabase();
			Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, null,
					Contract.MessageEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(conversationInfo.getLocalID())},
					null, null, getConversationSortByDesc(conversationInfo), null);
			
			//Getting the indices
			conversationItemIndices = ConversationItemIndices.fromCursor(cursor);
			
			//Initializing the loader
			super.initialize(database, cursor);
		}
		
		@Override
		public List<ConversationItem> loadNextChunk(Context context) {
			//Creating the message list
			List<ConversationItem> conversationItems = new ArrayList<>();
			
			//Loading the messages
			for(int i = 0; i < Messaging.messageChunkSize; i++) {
				if(!super.cursor.moveToNext()) break;
				conversationItems.add(databaseManager.loadConversationItem(context, conversationItemIndices, super.cursor, super.database));
			}
			
			//Reversing the list
			Collections.reverse(conversationItems);
			
			//Returning the list
			return conversationItems;
		}
	}
}