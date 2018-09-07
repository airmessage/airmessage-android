package me.tagavari.airmessage;

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

import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

import me.tagavari.airmessage.common.Blocks;
import me.tagavari.airmessage.common.SharedValues;

class DatabaseManager extends SQLiteOpenHelper {
	//If you change the database schema, you must increment the database version
	private static final String DATABASE_NAME = "messages.db";
	private static final int DATABASE_VERSION = 7;
	
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
			Contract.ConversationEntry.COLUMN_NAME_STATE,
			Contract.ConversationEntry.COLUMN_NAME_SERVICE,
			Contract.ConversationEntry.COLUMN_NAME_NAME,
			Contract.ConversationEntry.COLUMN_NAME_UNREADMESSAGECOUNT,
			Contract.ConversationEntry.COLUMN_NAME_ARCHIVED,
			Contract.ConversationEntry.COLUMN_NAME_MUTED,
			Contract.ConversationEntry.COLUMN_NAME_COLOR,
			Contract.ConversationEntry.COLUMN_NAME_DRAFTMESSAGE,
			Contract.ConversationEntry.COLUMN_NAME_DRAFTUPDATETIME
	};
	private static final String messageSortOrder = "COALESCE(" + Contract.MessageEntry.TABLE_NAME + '.' + Contract.MessageEntry.COLUMN_NAME_SERVERID + ',' + Contract.MessageEntry.TABLE_NAME + '.' + Contract.MessageEntry._ID + ')';
	
	//private static final String SQL_FETCH_CONVERSATION_MESSAGES = "SELECT * FROM " + Contract.MessageEntry.TABLE_NAME + " WHERE " + Contract.MessageEntry.COLUMN_NAME_CHAT + " = ? ORDER BY " + Contract.MessageEntry.COLUMN_NAME_DATE + " ASC;";
	
	//Creating the messages table creation statements
	private static final String SQL_CREATE_TABLE_MESSAGES = "CREATE TABLE " + Contract.MessageEntry.TABLE_NAME + " (" +
			Contract.MessageEntry._ID + " INTEGER PRIMARY KEY UNIQUE, " +
			Contract.MessageEntry.COLUMN_NAME_SERVERID + " INTEGER, " +
			Contract.MessageEntry.COLUMN_NAME_GUID + " TEXT UNIQUE, " +
			Contract.MessageEntry.COLUMN_NAME_SENDER + " TEXT, " +
			Contract.MessageEntry.COLUMN_NAME_OTHER + " TEXT, " +
			Contract.MessageEntry.COLUMN_NAME_DATE + " INTEGER NOT NULL, " +
			Contract.MessageEntry.COLUMN_NAME_ITEMTYPE + " INTEGER NOT NULL, " +
			Contract.MessageEntry.COLUMN_NAME_ITEMSUBTYPE + " INTEGER, " +
			Contract.MessageEntry.COLUMN_NAME_STATE + " INTEGER, " +
			Contract.MessageEntry.COLUMN_NAME_ERROR + " INTEGER, " +
			Contract.MessageEntry.COLUMN_NAME_DATEREAD + " INTEGER, " +
			Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT + " TEXT, " +
			Contract.MessageEntry.COLUMN_NAME_SENDSTYLE + " TEXT, " +
			Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED + " INTEGER NOT NULL DEFAULT 0, " +
			Contract.MessageEntry.COLUMN_NAME_CHAT + " INTEGER NOT NULL" +
			");";
	private static final String SQL_CREATE_TABLE_CONVERSATIONS = "CREATE TABLE " + Contract.ConversationEntry.TABLE_NAME + " (" +
			Contract.ConversationEntry._ID + " INTEGER PRIMARY KEY UNIQUE, " +
			Contract.ConversationEntry.COLUMN_NAME_GUID + " TEXT UNIQUE, " +
			Contract.ConversationEntry.COLUMN_NAME_STATE + " INTEGER NOT NULL, " +
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
			Contract.DraftFileEntry.COLUMN_NAME_ORIGINALPATH + " TEXT," +
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
			Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM + " TEXT" +
			");";
	private static final String SQL_CREATE_TABLE_STICKER = "CREATE TABLE " + Contract.StickerEntry.TABLE_NAME + " (" +
			Contract.StickerEntry._ID + " INTEGER PRIMARY KEY UNIQUE, " +
			Contract.StickerEntry.COLUMN_NAME_GUID + " TEXT UNIQUE," +
			Contract.StickerEntry.COLUMN_NAME_MESSAGE + " INTEGER NOT NULL," +
			Contract.StickerEntry.COLUMN_NAME_MESSAGEINDEX + " INTEGER NOT NULL," +
			Contract.StickerEntry.COLUMN_NAME_SENDER + " TEXT," +
			Contract.StickerEntry.COLUMN_NAME_DATE + " INTEGER NOT NULL," +
			Contract.StickerEntry.COLUMN_NAME_DATA + " BLOB NOT NULL" +
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
					int indexID = cursor.getColumnIndex(BaseColumns._ID);
					int indexData = cursor.getColumnIndex("data");
					
					ContentValues contentValues;
					while(cursor.moveToNext()) {
						contentValues = new ContentValues();
						try {
							contentValues.put("data", SharedValues.decompressLegacyV2(cursor.getBlob(indexData)));
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
	
	static final class Contract {
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
			static final String COLUMN_NAME_DATEREAD = "date_read";
			static final String COLUMN_NAME_MESSAGETEXT = "message_text";
			static final String COLUMN_NAME_SENDSTYLE = "send_style";
			static final String COLUMN_NAME_SENDSTYLEVIEWED = "send_style_viewed";
			static final String COLUMN_NAME_CHAT = "chat";
		}
		
		static class ConversationEntry implements BaseColumns {
			static final String TABLE_NAME = "conversations";
			static final String COLUMN_NAME_GUID = "guid";
			static final String COLUMN_NAME_STATE = "state";
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
			static final String COLUMN_NAME_ORIGINALPATH = "original_path";
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
		}
		
		static class StickerEntry implements BaseColumns {
			static final String TABLE_NAME = "sticker";
			static final String COLUMN_NAME_GUID = "guid";
			static final String COLUMN_NAME_MESSAGE = "message";
			static final String COLUMN_NAME_MESSAGEINDEX = "message_index";
			static final String COLUMN_NAME_SENDER = "sender";
			static final String COLUMN_NAME_DATE = "date";
			static final String COLUMN_NAME_DATA = "data";
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
	
	static void createInstance(Context context) {
		instance = new DatabaseManager(context);
	}
	
	static DatabaseManager getInstance() {
		return instance;
	}
	
	static void disposeInstance() {
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
		Crashlytics.log("Table rebuild requested.\n" +
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
		//int indexName = cursor.getColumnIndex("name");
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
	
	List<ConversationManager.ConversationInfo> fetchConversationsWithState(Context context, ConversationManager.ConversationInfo.ConversationState conversationState) {
		//Creating the conversation list
		List<ConversationManager.ConversationInfo> conversationList = new ArrayList<>();
		
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Querying the database
		Cursor cursor = database.query(Contract.ConversationEntry.TABLE_NAME, sqlQueryConversationData, Contract.ConversationEntry.COLUMN_NAME_STATE + " = ?", new String[]{Integer.toString(conversationState.getIdentifier())}, null, null, null);
		
		//Getting the indexes
		int indexChatID = cursor.getColumnIndexOrThrow(Contract.ConversationEntry._ID);
		int indexChatGUID = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_GUID);
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
			String service = cursor.getString(indexChatService);
			String chatTitle = cursor.getString(indexChatName);
			int chatUnreadMessages = cursor.getInt(indexChatUnreadMessages);
			boolean chatArchived = cursor.getInt(indexChatArchived) != 0;
			boolean chatMuted = cursor.getInt(indexChatMuted) != 0;
			int chatColor = cursor.getInt(indexChatColor);
			ConversationManager.LightConversationItem lightItem = getLightItem(context, chatID);
			String draftMessage = cursor.getString(indexDraftMessage);
			long draftUpdateTime = cursor.getLong(indexDraftUpdateTime);
			
			//Getting the members and drafts
			ArrayList<ConversationManager.MemberInfo> conversationMembers = loadConversationMembers(database, chatID);
			ArrayList<ConversationManager.DraftFile> draftFiles = loadDraftFiles(database, context, chatID);
			
			//Creating and adding the conversation info
			ConversationManager.ConversationInfo conversationInfo = new ConversationManager.ConversationInfo(chatID, chatGUID, conversationState, service, conversationMembers, chatTitle, chatUnreadMessages, chatColor, draftMessage, draftFiles, draftUpdateTime);
			conversationInfo.setArchived(chatArchived);
			conversationInfo.setMuted(chatMuted);
			conversationInfo.trySetLastItem(lightItem, false);
			
			//Adding the conversation to the list
			conversationList.add(conversationInfo);
		}
		
		//Closing the cursor
		cursor.close();
		
		//Returning the conversation list
		return conversationList;
	}
	
	MainApplication.LoadFlagArrayList<ConversationManager.ConversationInfo> fetchSummaryConversations(Context context) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Creating the conversation list
		MainApplication.LoadFlagArrayList<ConversationManager.ConversationInfo> conversationList = new MainApplication.LoadFlagArrayList<>(true);
		
		//Querying the database
		Cursor cursor = database.query(Contract.ConversationEntry.TABLE_NAME, sqlQueryConversationData, Contract.ConversationEntry.COLUMN_NAME_STATE + " != ?", new String[]{Integer.toString(ConversationManager.ConversationInfo.ConversationState.INCOMPLETE_SERVER.getIdentifier())}, null, null, null);
		
		//Getting the indexes
		int indexChatID = cursor.getColumnIndexOrThrow(Contract.ConversationEntry._ID);
		int indexChatGUID = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_GUID);
		int indexChatState = cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_STATE);
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
			ConversationManager.ConversationInfo.ConversationState conversationState = ConversationManager.ConversationInfo.ConversationState.fromIdentifier(cursor.getInt(indexChatState));
			String service = cursor.getString(indexChatService);
			String chatName = cursor.getString(indexChatName);
			int chatUnreadMessages = cursor.getInt(indexChatUnreadMessages);
			boolean chatArchived = cursor.getInt(indexChatArchived) != 0;
			boolean chatMuted = cursor.getInt(indexChatMuted) != 0;
			int chatColor = cursor.getInt(indexChatColor);
			String draftMessage = cursor.getString(indexDraftMessage);
			long draftUpdateTime = cursor.getLong(indexDraftUpdateTime);
			
			//Getting the members and drafts
			ArrayList<ConversationManager.MemberInfo> conversationMembers = loadConversationMembers(database, chatID);
			ArrayList<ConversationManager.DraftFile> draftFiles = loadDraftFiles(database, context, chatID);
			
			//Getting the light item
			ConversationManager.LightConversationItem lightItem;
			if(draftMessage != null) {
				lightItem = new ConversationManager.LightConversationItem(context.getResources().getString(R.string.prefix_draft, draftMessage), draftUpdateTime, true);
			} else if(!draftFiles.isEmpty()) {
				//Converting the draft list to a string resource list
				ArrayList<Integer> draftStringRes = new ArrayList<>();
				for(ConversationManager.DraftFile draft : draftFiles) draftStringRes.add(ConversationManager.getNameFromContentType(draft.getFileType()));
				
				String summary;
				if(draftStringRes.size() == 1) summary = context.getResources().getString(draftStringRes.get(0));
				else summary = context.getResources().getQuantityString(R.plurals.message_multipleattachments, draftStringRes.size(), draftStringRes.size());
				lightItem = new ConversationManager.LightConversationItem(context.getResources().getString(R.string.prefix_draft, summary), draftUpdateTime, true);
			} else lightItem = getLightItem(context, chatID);
			
			//Creating and adding the conversation info
			ConversationManager.ConversationInfo conversationInfo = new ConversationManager.ConversationInfo(chatID, chatGUID, conversationState, service, conversationMembers, chatName, chatUnreadMessages, chatColor, draftMessage, draftFiles, draftUpdateTime);
			conversationInfo.setArchived(chatArchived);
			conversationInfo.setMuted(chatMuted);
			conversationInfo.trySetLastItem(lightItem, false);
			
			//Adding the conversation to the list
			conversationList.add(conversationInfo);
		}
		
		//Closing the cursor
		cursor.close();
		
		//Sorting and returning the conversation list
		Collections.sort(conversationList, ConversationManager.conversationComparator);
		return conversationList;
	}
	
	/* void switchMessageOwnership(long identifierFrom, long identifierTo) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_CHAT, identifierTo);
		
		//Updating the entries
		getWritableDatabase().update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.MessageEntry.COLUMN_NAME_CHAT + "=?", new String[]{Long.toString(identifierFrom)});
	} */
	
	void switchMessageOwnership(ConversationManager.ConversationInfo conversationFrom, ConversationManager.ConversationInfo conversationTo) {
		//Transferring the messages from the old conversation to the new one
		for(ConversationManager.ConversationItem item : loadConversationItems(conversationFrom)) transferConversationItemReplaceGhost(item, conversationTo);
		
		/* ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_CHAT, conversationTo.getLocalID());
		
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		database.update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.MessageEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(conversationFrom.getLocalID())}); */
	}
	
	List<ConversationManager.ConversationItem> loadConversationItems(ConversationManager.ConversationInfo conversationInfo) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Creating the message list
		List<ConversationManager.ConversationItem> conversationItems = new ArrayList<>();
		
		//Querying the database
		Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, null, Contract.MessageEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(conversationInfo.getLocalID())}, null, null, messageSortOrder + " ASC", null);
		//Cursor cursor = database.rawQuery(SQL_FETCH_CONVERSATION_MESSAGES, new String[]{Long.toString(conversationInfo.getLocalID())});
		
		//Getting the indexes
		int iLocalID = cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID);
		int iServerID = cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID);
		int iGuid = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_GUID);
		int iSender = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDER);
		int iItemType = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE);
		int iDate = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_DATE);
		int iState = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_STATE);
		int iError = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_ERROR);
		int iDateRead = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_DATEREAD);
		int iSendStyle = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDSTYLE);
		int iSendStyleViewed = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED);
		int iMessageText = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT);
		int iOther = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_OTHER);
		
		//Looping while there are items
		while(cursor.moveToNext()) {
			//Getting the general message info
			long localID = cursor.getLong(iLocalID);
			long serverID = cursor.isNull(iServerID) ? -1 : cursor.getLong(iServerID);
			String guid = cursor.getString(iGuid);
			String sender = cursor.isNull(iSender) ? null : cursor.getString(iSender);
			int itemType = cursor.getInt(iItemType);
			long date = cursor.getLong(iDate);
			
			//Checking if the item is a message
			if(itemType == ConversationManager.MessageInfo.itemType) {
				//Getting the general message info
				int stateCode = cursor.getInt(iState);
				int errorCode = cursor.getInt(iError);
				long dateRead = cursor.getLong(iDateRead);
				String sendStyle = cursor.getString(iSendStyle);
				boolean sendStyleViewed = cursor.getInt(iSendStyleViewed) != 0;
				String message = cursor.getString(iMessageText);
				
				//Creating the conversation item
				ConversationManager.MessageInfo messageInfo = new ConversationManager.MessageInfo(localID, serverID, guid, conversationInfo, sender, message, sendStyle, sendStyleViewed, date, stateCode, errorCode, dateRead);
				
				{
					//Querying the database for attachments
					Cursor attachmentCursor = database.query(Contract.AttachmentEntry.TABLE_NAME, new String[]{Contract.AttachmentEntry._ID, Contract.AttachmentEntry.COLUMN_NAME_GUID, Contract.AttachmentEntry.COLUMN_NAME_FILENAME, Contract.AttachmentEntry.COLUMN_NAME_FILETYPE, Contract.AttachmentEntry.COLUMN_NAME_FILESIZE, Contract.AttachmentEntry.COLUMN_NAME_FILEPATH, Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM},
							Contract.AttachmentEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(localID)}, null, null, null);
					
					//Getting the indexes
					int aLocalID = attachmentCursor.getColumnIndexOrThrow(Contract.AttachmentEntry._ID);
					int aGuid = attachmentCursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_GUID);
					int aFileName = attachmentCursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILENAME);
					int aFileType = attachmentCursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILETYPE);
					int aFileSize = attachmentCursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILESIZE);
					int aFilePath = attachmentCursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH);
					int aChecksum = attachmentCursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM);
					
					//Iterating over the results
					while(attachmentCursor.moveToNext()) {
						//Getting the attachment data
						File file = attachmentCursor.isNull(aFilePath) ? null : ConversationManager.AttachmentInfo.getAbsolutePath(MainApplication.getInstance(), attachmentCursor.getString(aFilePath));
						String fileName = attachmentCursor.getString(aFileName);
						String fileType = attachmentCursor.getString(aFileType);
						long fileSize = attachmentCursor.isNull(aFileSize) ? -1 : attachmentCursor.getLong(aFileSize);
						String stringChecksum = attachmentCursor.getString(aChecksum);
						byte[] fileChecksum = stringChecksum == null ? null : Base64.decode(stringChecksum, Base64.NO_WRAP);
						
						//Getting the identifiers
						long fileID = attachmentCursor.getLong(aLocalID);
						String fileGuid = attachmentCursor.getString(aGuid);
						
						//Checking if the attachment has data
						if(file != null && file.exists() && file.isFile()) {
							//Adding the attachment to the message
							messageInfo.addAttachment(ConversationManager.createAttachmentInfoFromType(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, file));
						} else {
							//Deleting the file if it is a directory
							if(file != null && file.exists() && file.isDirectory())
								Constants.recursiveDelete(file);
							
							//Creating the attachment
							ConversationManager.AttachmentInfo attachment = ConversationManager.createAttachmentInfoFromType(fileID, fileGuid, messageInfo, fileName, fileType, fileSize);
							if(fileChecksum != null) attachment.setFileChecksum(fileChecksum);
							
							//Adding the attachment to the message
							attachment.setLocalID(fileID);
							messageInfo.addAttachment(attachment);
						}
					}
					
					//Closing the attachment cursor
					attachmentCursor.close();
				}
				
				{
					//Querying the database for stickers
					Cursor stickerCursor = database.query(Contract.StickerEntry.TABLE_NAME, new String[]{Contract.StickerEntry._ID, Contract.StickerEntry.COLUMN_NAME_MESSAGEINDEX, Contract.StickerEntry.COLUMN_NAME_GUID, Contract.StickerEntry.COLUMN_NAME_SENDER, Contract.StickerEntry.COLUMN_NAME_DATE},
							Contract.StickerEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(localID)}, null, null, null);
					
					//Getting the indexes
					int sIdentifierIndex = stickerCursor.getColumnIndexOrThrow(Contract.StickerEntry._ID);
					int sIdentifierMessageIndex = stickerCursor.getColumnIndexOrThrow(Contract.StickerEntry.COLUMN_NAME_MESSAGEINDEX);
					int sIdentifierGuid = stickerCursor.getColumnIndexOrThrow(Contract.StickerEntry.COLUMN_NAME_GUID);
					int sIdentifierSender = stickerCursor.getColumnIndexOrThrow(Contract.StickerEntry.COLUMN_NAME_SENDER);
					int sIdentifierDate = stickerCursor.getColumnIndexOrThrow(Contract.StickerEntry.COLUMN_NAME_DATE);
					
					//Adding the results to the message
					while(stickerCursor.moveToNext()) messageInfo.addSticker(new ConversationManager.StickerInfo(stickerCursor.getLong(sIdentifierIndex), stickerCursor.getString(sIdentifierGuid), localID, stickerCursor.getInt(sIdentifierMessageIndex), stickerCursor.getString(sIdentifierSender), stickerCursor.getLong(sIdentifierDate)));
					
					//Closing the sticker cursor
					stickerCursor.close();
				}
				
				{
					//Querying the database for tapbacks
					Cursor tapbackCursor = database.query(Contract.TapbackEntry.TABLE_NAME, new String[]{Contract.TapbackEntry._ID, Contract.TapbackEntry.COLUMN_NAME_MESSAGEINDEX, Contract.TapbackEntry.COLUMN_NAME_SENDER, Contract.TapbackEntry.COLUMN_NAME_CODE},
							Contract.TapbackEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(localID)}, null, null, null);
					
					//Getting the indexes
					int tIdentifierIndex = tapbackCursor.getColumnIndexOrThrow(Contract.TapbackEntry._ID);
					int tIdentifierMessageIndex = tapbackCursor.getColumnIndexOrThrow(Contract.TapbackEntry.COLUMN_NAME_MESSAGEINDEX);
					int tIdentifierSender = tapbackCursor.getColumnIndexOrThrow(Contract.TapbackEntry.COLUMN_NAME_SENDER);
					int tIdentifierCode = tapbackCursor.getColumnIndexOrThrow(Contract.TapbackEntry.COLUMN_NAME_CODE);
					
					//Adding the results to the message
					while(tapbackCursor.moveToNext()) messageInfo.addTapback(new ConversationManager.TapbackInfo(tapbackCursor.getLong(tIdentifierIndex), localID, tapbackCursor.getInt(tIdentifierMessageIndex), tapbackCursor.getString(tIdentifierSender), tapbackCursor.getInt(tIdentifierCode)));
					
					//Closing the tapback cursor
					tapbackCursor.close();
				}
				
				//Adding the conversation item
				conversationItems.add(messageInfo);
			}
			//Otherwise checking if the item is a group action
			else if(itemType == ConversationManager.GroupActionInfo.itemType) {
				//Getting the other
				String other = cursor.getString(iOther);
				
				//Getting the action type
				int subtype = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_ITEMSUBTYPE));
				
				//Creating the conversation item
				ConversationManager.GroupActionInfo conversationItem = new ConversationManager.GroupActionInfo(localID, serverID, guid, conversationInfo, subtype, sender, other, date);
				
				//Adding the conversation item
				conversationItems.add(conversationItem);
			}
			//Otherwise checking if the item is a chat rename
			else if(itemType == ConversationManager.ChatRenameActionInfo.itemType) {
				//Getting the name
				String title = cursor.getString(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_OTHER));
				
				//Creating the conversation item
				ConversationManager.ChatRenameActionInfo conversationItem = new ConversationManager.ChatRenameActionInfo(localID, serverID, guid, conversationInfo, sender, title, date);
				
				//Adding the conversation item
				conversationItems.add(conversationItem);
			}
			//Otherwise checking if the item is a chat creation message
			else if(itemType == ConversationManager.ChatCreationMessage.itemType) {
				//Adding the conversation item
				conversationItems.add(new ConversationManager.ChatCreationMessage(localID, date, conversationInfo));
			}
		}
		
		//Closing the cursor
		cursor.close();
		
		//Returning the conversation items
		return conversationItems;
	}
	
	List<ConversationManager.ConversationItem> loadConversationChunk(ConversationManager.ConversationInfo conversationInfo, boolean useFirstDate, long firstMessageDate) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Creating the message list
		List<ConversationManager.ConversationItem> conversationItems = new ArrayList<>();
		
		//Querying the database
		String selection;
		String[] selectionArgs;
		if(useFirstDate) {
			selection = Contract.MessageEntry.COLUMN_NAME_CHAT + " = ? AND " + Contract.MessageEntry.COLUMN_NAME_DATE + " < ?";
			selectionArgs = new String[]{Long.toString(conversationInfo.getLocalID()), Long.toString(firstMessageDate)};
		} else {
			selection = Contract.MessageEntry.COLUMN_NAME_CHAT + " = ?";
			selectionArgs = new String[]{Long.toString(conversationInfo.getLocalID())};
		}
		Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, null, selection, selectionArgs, null, null, messageSortOrder + " DESC", Integer.toString(Messaging.messageChunkSize));
		//Cursor cursor = database.rawQuery(SQL_FETCH_CONVERSATION_MESSAGES, new String[]{Long.toString(conversationInfo.getLocalID())});
		
		//Getting the indexes
		int iLocalID = cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID);
		int iServerID = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SERVERID);
		int iGuid = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_GUID);
		int iSender = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDER);
		int iItemType = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE);
		int iDate = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_DATE);
		int iState = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_STATE);
		int iError = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_ERROR);
		int iDateRead = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_DATEREAD);
		int iSendStyle = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDSTYLE);
		int iSendStyleViewed = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED);
		int iMessageText = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT);
		int iOther = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_OTHER);
		
		//Looping while there are items (backwards, since the items are sorted in descending order of date for chunk limiting purposes)
		for(cursor.moveToLast(); !cursor.isBeforeFirst(); cursor.moveToPrevious()) {
			//Getting the general message info
			long localID = cursor.getLong(iLocalID);
			long serverID = cursor.isNull(iServerID) ? -1 : cursor.getLong(iServerID);
			String guid = cursor.getString(iGuid);
			String sender = cursor.isNull(iSender) ? null : cursor.getString(iSender);
			int itemType = cursor.getInt(iItemType);
			long date = cursor.getLong(iDate);
			
			//Checking if the item is a message
			if(itemType == ConversationManager.MessageInfo.itemType) {
				//Getting the general message info
				int stateCode = cursor.getInt(iState);
				int errorCode = cursor.getInt(iError);
				long dateRead = cursor.getLong(iDateRead);
				String sendStyle = cursor.getString(iSendStyle);
				boolean sendStyleViewed = cursor.getInt(iSendStyleViewed) != 0;
				String message = cursor.getString(iMessageText);
				
				//Creating the conversation item
				ConversationManager.MessageInfo messageInfo = new ConversationManager.MessageInfo(localID, serverID, guid, conversationInfo, sender, message, sendStyle, sendStyleViewed, date, stateCode, errorCode, dateRead);
				
				{
					//Querying the database for attachments
					Cursor attachmentCursor = database.query(Contract.AttachmentEntry.TABLE_NAME, new String[]{Contract.AttachmentEntry._ID, Contract.AttachmentEntry.COLUMN_NAME_GUID, Contract.AttachmentEntry.COLUMN_NAME_FILENAME, Contract.AttachmentEntry.COLUMN_NAME_FILETYPE, Contract.AttachmentEntry.COLUMN_NAME_FILESIZE, Contract.AttachmentEntry.COLUMN_NAME_FILEPATH, Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM},
							Contract.AttachmentEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(localID)}, null, null, null);
					
					//Getting the indexes
					int aLocalID = attachmentCursor.getColumnIndexOrThrow(Contract.AttachmentEntry._ID);
					int aGuid = attachmentCursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_GUID);
					int aFileName = attachmentCursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILENAME);
					int aFileType = attachmentCursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILETYPE);
					int aFileSize = attachmentCursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILESIZE);
					int aFilePath = attachmentCursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH);
					int aChecksum = attachmentCursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM);
					
					//Iterating over the results
					while(attachmentCursor.moveToNext()) {
						//Getting the attachment data
						File file = attachmentCursor.isNull(aFilePath) ? null : ConversationManager.AttachmentInfo.getAbsolutePath(MainApplication.getInstance(), attachmentCursor.getString(aFilePath));
						String fileName = attachmentCursor.getString(aFileName);
						String fileType = attachmentCursor.getString(aFileType);
						long fileSize = attachmentCursor.isNull(aFileSize) ? -1 : attachmentCursor.getLong(aFileSize);
						String stringChecksum = attachmentCursor.getString(aChecksum);
						byte[] fileChecksum = stringChecksum == null ? null : Base64.decode(stringChecksum, Base64.NO_WRAP);
						
						//Getting the identifiers
						long fileID = attachmentCursor.getLong(aLocalID);
						String fileGuid = attachmentCursor.getString(aGuid);
						
						//Checking if the attachment has data
						if(file != null && file.exists() && file.isFile()) {
							//Adding the attachment to the message
							messageInfo.addAttachment(ConversationManager.createAttachmentInfoFromType(fileID, fileGuid, messageInfo, fileName, fileType, fileSize, file));
						} else {
							//Deleting the file if it is a directory
							if(file != null && file.exists() && file.isDirectory())
								Constants.recursiveDelete(file);
							
							//Creating the attachment
							ConversationManager.AttachmentInfo attachment = ConversationManager.createAttachmentInfoFromType(fileID, fileGuid, messageInfo, fileName, fileType, fileSize);
							if(fileChecksum != null) attachment.setFileChecksum(fileChecksum);
							
							//Adding the attachment to the message
							attachment.setLocalID(fileID);
							messageInfo.addAttachment(attachment);
						}
					}
					
					//Closing the attachment cursor
					attachmentCursor.close();
				}
				
				{
					//Querying the database for stickers
					Cursor stickerCursor = database.query(Contract.StickerEntry.TABLE_NAME, new String[]{Contract.StickerEntry._ID, Contract.StickerEntry.COLUMN_NAME_MESSAGEINDEX, Contract.StickerEntry.COLUMN_NAME_GUID, Contract.StickerEntry.COLUMN_NAME_SENDER, Contract.StickerEntry.COLUMN_NAME_DATE},
							Contract.StickerEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(localID)}, null, null, null);
					
					//Getting the indexes
					int sIdentifierIndex = stickerCursor.getColumnIndexOrThrow(Contract.StickerEntry._ID);
					int sIdentifierMessageIndex = stickerCursor.getColumnIndexOrThrow(Contract.StickerEntry.COLUMN_NAME_MESSAGEINDEX);
					int sIdentifierGuid = stickerCursor.getColumnIndexOrThrow(Contract.StickerEntry.COLUMN_NAME_GUID);
					int sIdentifierSender = stickerCursor.getColumnIndexOrThrow(Contract.StickerEntry.COLUMN_NAME_SENDER);
					int sIdentifierDate = stickerCursor.getColumnIndexOrThrow(Contract.StickerEntry.COLUMN_NAME_DATE);
					
					//Adding the results to the message
					while(stickerCursor.moveToNext()) messageInfo.addSticker(new ConversationManager.StickerInfo(stickerCursor.getLong(sIdentifierIndex), stickerCursor.getString(sIdentifierGuid), localID, stickerCursor.getInt(sIdentifierMessageIndex), stickerCursor.getString(sIdentifierSender), stickerCursor.getLong(sIdentifierDate)));
					
					//Closing the sticker cursor
					stickerCursor.close();
				}
				
				{
					//Querying the database for tapbacks
					Cursor tapbackCursor = database.query(Contract.TapbackEntry.TABLE_NAME, new String[]{Contract.TapbackEntry._ID, Contract.TapbackEntry.COLUMN_NAME_MESSAGEINDEX, Contract.TapbackEntry.COLUMN_NAME_SENDER, Contract.TapbackEntry.COLUMN_NAME_CODE},
							Contract.TapbackEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(localID)}, null, null, null);
					
					//Getting the indexes
					int tIdentifierIndex = tapbackCursor.getColumnIndexOrThrow(Contract.TapbackEntry._ID);
					int tIdentifierMessageIndex = tapbackCursor.getColumnIndexOrThrow(Contract.TapbackEntry.COLUMN_NAME_MESSAGEINDEX);
					int tIdentifierSender = tapbackCursor.getColumnIndexOrThrow(Contract.TapbackEntry.COLUMN_NAME_SENDER);
					int tIdentifierCode = tapbackCursor.getColumnIndexOrThrow(Contract.TapbackEntry.COLUMN_NAME_CODE);
					
					//Adding the results to the message
					while(tapbackCursor.moveToNext()) messageInfo.addTapback(new ConversationManager.TapbackInfo(tapbackCursor.getLong(tIdentifierIndex), localID, tapbackCursor.getInt(tIdentifierMessageIndex), tapbackCursor.getString(tIdentifierSender), tapbackCursor.getInt(tIdentifierCode)));
					
					//Closing the tapback cursor
					tapbackCursor.close();
				}
				
				//Adding the conversation item
				conversationItems.add(messageInfo);
			}
			//Otherwise checking if the item is a group action
			else if(itemType == ConversationManager.GroupActionInfo.itemType) {
				//Getting the other
				String other = cursor.getString(iOther);
				
				//Getting the action type
				int subtype = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_ITEMSUBTYPE));
				
				//Creating the conversation item
				ConversationManager.GroupActionInfo conversationItem = new ConversationManager.GroupActionInfo(localID, serverID, guid, conversationInfo, subtype, sender, other, date);
				
				//Adding the conversation item
				conversationItems.add(conversationItem);
			}
			//Otherwise checking if the item is a chat rename
			else if(itemType == ConversationManager.ChatRenameActionInfo.itemType) {
				//Getting the name
				String title = cursor.getString(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_OTHER));
				
				//Creating the conversation item
				ConversationManager.ChatRenameActionInfo conversationItem = new ConversationManager.ChatRenameActionInfo(localID, serverID, guid, conversationInfo, sender, title, date);
				
				//Adding the conversation item
				conversationItems.add(conversationItem);
			}
			//Otherwise checking if the item is a chat creation message
			else if(itemType == ConversationManager.ChatCreationMessage.itemType) {
				//Adding the conversation item
				conversationItems.add(new ConversationManager.ChatCreationMessage(localID, date, conversationInfo));
			}
		}
		
		//Closing the cursor
		cursor.close();
		
		//Returning the conversation items
		return conversationItems;
	}
	
	void invalidateAttachment(long localID) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.putNull(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH);
		
		//Updating the database
		getWritableDatabase().update(Contract.AttachmentEntry.TABLE_NAME, contentValues, Contract.AttachmentEntry._ID + " = ?", new String[]{Long.toString(localID)});
	}
	
	void updateAttachmentFile(long localID, Context context, File file) {
		//Creating the content values variable
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH, ConversationManager.AttachmentInfo.getRelativePath(context, file));
		
		//Updating the data
		getWritableDatabase().update(Contract.AttachmentEntry.TABLE_NAME, contentValues, Contract.AttachmentEntry._ID + "=?", new String[]{Long.toString(localID)});
	}
	
	void updateAttachmentChecksum(long localID, byte[] checksum) {
		//Creating the content values variable
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM, Base64.encodeToString(checksum, Base64.NO_WRAP));
		
		//Updating the data
		getWritableDatabase().update(Contract.AttachmentEntry.TABLE_NAME, contentValues, Contract.AttachmentEntry._ID + "=?", new String[]{Long.toString(localID)});
	}
	
	/* static void createUpdateAttachmentFile(SQLiteDatabase writableDatabase, long localID, ConversationManager.AttachmentInfo attachmentInfo, File file) {
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
	
	ConversationManager.DraftFile addDraftReference(long conversationID, File file, String fileName, long fileSize, String fileType, File originalFile, long modificationDate, long updateTime) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Correcting the file type
		if(fileType == null) fileType = "application/octet-stream";
		
		//Adding the file
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.DraftFileEntry.COLUMN_NAME_CHAT, conversationID);
		contentValues.put(Contract.DraftFileEntry.COLUMN_NAME_FILE, ConversationManager.DraftFile.getRelativePath(MainApplication.getInstance(), file));
		contentValues.put(Contract.DraftFileEntry.COLUMN_NAME_FILENAME, fileName);
		contentValues.put(Contract.DraftFileEntry.COLUMN_NAME_FILESIZE, fileSize);
		contentValues.put(Contract.DraftFileEntry.COLUMN_NAME_FILETYPE, fileType);
		if(originalFile != null) {
			contentValues.put(Contract.DraftFileEntry.COLUMN_NAME_ORIGINALPATH, originalFile.getAbsolutePath());
			contentValues.put(Contract.DraftFileEntry.COLUMN_NAME_MODIFICATIONDATE, modificationDate);
		}
		
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
		return new ConversationManager.DraftFile(localID, file, fileName, fileSize, fileType, originalFile, modificationDate);
	}
	
	void removeDraftReference(long draftID, long updateTime) {
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
	
	private void updateConversationDraftUpdateTime(SQLiteDatabase database, long conversationID, long updateTime) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_DRAFTUPDATETIME, updateTime);
		
		database.update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " + ?", new String[]{Long.toString(conversationID)});
	}
	
	ArrayList<ConversationManager.DraftFile> getDraftReferences(long conversationID) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Querying the database
		try(Cursor cursor = database.query(Contract.DraftFileEntry.TABLE_NAME,
				new String[]{Contract.DraftFileEntry._ID, Contract.DraftFileEntry.COLUMN_NAME_FILE, Contract.DraftFileEntry.COLUMN_NAME_FILENAME, Contract.DraftFileEntry.COLUMN_NAME_FILESIZE, Contract.DraftFileEntry.COLUMN_NAME_FILETYPE, Contract.DraftFileEntry.COLUMN_NAME_ORIGINALPATH, Contract.DraftFileEntry.COLUMN_NAME_MODIFICATIONDATE},
				Contract.DraftFileEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(conversationID)},
				null, null, null)) {
			//Reading the results
			ArrayList<ConversationManager.DraftFile> draftList = new ArrayList<>();
			
			if(cursor.moveToNext()) {
				int indexIdentifier = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry._ID);
				int indexFile = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_FILE);
				int indexFileName = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_FILENAME);
				int indexFileSize = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_FILESIZE);
				int indexFileType = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_FILETYPE);
				int indexOriginalPath = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_ORIGINALPATH);
				int indexModificationDate = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_MODIFICATIONDATE);
				do {
					String originalPath;
					draftList.add(new ConversationManager.DraftFile(
							cursor.getLong(indexIdentifier),
							ConversationManager.DraftFile.getAbsolutePath(MainApplication.getInstance(), cursor.getString(indexFile)),
							cursor.getString(indexFileName),
							cursor.getLong(indexFileSize),
							cursor.getString(indexFileType),
							(originalPath = cursor.getString(indexOriginalPath)) == null ? null : new File(originalPath),
							originalPath == null ? 0 : cursor.getLong(indexModificationDate)
					));
				} while(cursor.moveToNext());
			}
			
			//Returning the list
			return draftList;
		}
	}
	
	private ConversationManager.LightConversationItem getLightItem(Context context, long chatID) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Getting the last item
		Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME,
				new String[]{Contract.MessageEntry._ID, Contract.MessageEntry.COLUMN_NAME_ITEMTYPE, Contract.MessageEntry.COLUMN_NAME_DATE},
				Contract.MessageEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(chatID)},
				null, null,
				messageSortOrder + " DESC ",
				"1");
		
		//Closing the cursor and returning if there are no results
		if(!cursor.moveToNext()) {
			cursor.close();
			return null;
		}
		
		//Getting the data
		long date = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_DATE));
		long lastItemID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID));
		int itemType = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE));
		
		//Closing the cursor
		cursor.close();
		
		switch(itemType) {
			case ConversationManager.MessageInfo.itemType: //Message
				//Retrieving the message data
				cursor = database.query(Contract.MessageEntry.TABLE_NAME,
						new String[]{Contract.MessageEntry.COLUMN_NAME_SENDER, Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT, Contract.MessageEntry.COLUMN_NAME_SENDSTYLE},
						Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(lastItemID)},
						null, null, null);
				
				//Closing the cursor and returning if there are no results
				if(!cursor.moveToNext()) {
					cursor.close();
					return null;
				}
				
				int currentIndex = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDER);
				String sender = cursor.isNull(currentIndex) ? null : cursor.getString(currentIndex);
				
				currentIndex = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT);
				String message = cursor.isNull(currentIndex) ? null : cursor.getString(currentIndex);
				
				currentIndex = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDSTYLE);
				String sendStyle = cursor.isNull(currentIndex) ? null : cursor.getString(currentIndex);
				
				//Closing the cursor
				cursor.close();
				
				//Checking if the message is valid
				if(message != null) {
					//Returning the light message info (without the attachments)
					return new ConversationManager.LightConversationItem(ConversationManager.MessageInfo.getSummary(context, sender == null, message, sendStyle, new ArrayList<>()), date, lastItemID, -1);
				}
				
				//Retrieving the attachments
				cursor = database.query(Contract.AttachmentEntry.TABLE_NAME,
						new String[]{Contract.AttachmentEntry.COLUMN_NAME_FILETYPE},
						Contract.AttachmentEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(lastItemID)},
						null, null, null);
				
				//Closing the cursor and returning if an empty item there are no results
				if(!cursor.moveToNext()) {
					cursor.close();
					return new ConversationManager.LightConversationItem(context.getResources().getString(R.string.part_unknown), date, lastItemID, -1);
				}
				
				//Getting the attachment string resources
				List<Integer> attachmentStringRes = new ArrayList<>();
				int indexType = cursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILETYPE);
				do attachmentStringRes.add(ConversationManager.getNameFromContentType(cursor.getString(indexType)));
				while(cursor.moveToNext());
				
				//Closing the cursor
				cursor.close();
				
				//Returning the light message info (without the message)
				return new ConversationManager.LightConversationItem(ConversationManager.MessageInfo.getSummary(context, sender == null, null, sendStyle, attachmentStringRes), date, lastItemID, -1);
			case ConversationManager.GroupActionInfo.itemType: //Group action
			{
				//Retrieving the action data
				cursor = database.query(Contract.MessageEntry.TABLE_NAME,
						new String[]{Contract.MessageEntry.COLUMN_NAME_SENDER, Contract.MessageEntry.COLUMN_NAME_OTHER, Contract.MessageEntry.COLUMN_NAME_ITEMSUBTYPE},
						Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(lastItemID)},
						null, null, null);
				
				//Closing the cursor and returning if there are no results
				if(!cursor.moveToNext()) {
					cursor.close();
					return null;
				}
				
				//Creating the summary
				/* int indexAgent = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDER);
				int indexOther = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_OTHER);
				String summary = ConversationManager.GroupActionInfo.getSummary(context, cursor.isNull(indexAgent) ? null : cursor.getString(indexAgent), cursor.isNull(indexOther) ? null : cursor.getString(indexOther), cursor.getInt(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_ITEMSUBTYPE))); */
				
				String agent = cursor.getString(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDER));
				
				if(agent != null) {
					UserCacheHelper.UserInfo userInfo = MainApplication.getInstance().getUserCacheHelper().getUserInfoSync(context, agent);
					if(userInfo != null) agent = userInfo.getContactName();
				}
				
				String other = cursor.getString(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_OTHER));
				if(other != null) {
					UserCacheHelper.UserInfo userInfo = MainApplication.getInstance().getUserCacheHelper().getUserInfoSync(context, other);
					if(userInfo != null) other = userInfo.getContactName();
				}
				
				String summary = ConversationManager.GroupActionInfo.getDirectSummary(context, agent, other, cursor.getInt(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_ITEMSUBTYPE)));
				
				//Closing the cursor
				cursor.close();
				
				//Returning the light message info
				return new ConversationManager.LightConversationItem(summary, date, lastItemID, -1);
			}
			case ConversationManager.ChatRenameActionInfo.itemType: //Chat rename
				//Retrieving the action data
				cursor = database.query(Contract.MessageEntry.TABLE_NAME,
						new String[]{Contract.MessageEntry.COLUMN_NAME_SENDER, Contract.MessageEntry.COLUMN_NAME_OTHER},
						Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(lastItemID)},
						null, null, null);
				
				//Closing the cursor and returning if there are no results
				if(!cursor.moveToNext()) {
					cursor.close();
					return null;
				}
				
				//Creating the summary
				String agent = cursor.getString(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDER));
				if(agent != null) {
					UserCacheHelper.UserInfo userInfo = MainApplication.getInstance().getUserCacheHelper().getUserInfoSync(context, agent);
					if(userInfo != null) agent = userInfo.getContactName();
				}
				
				String other = cursor.getString(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_OTHER));
				if(other != null) {
					UserCacheHelper.UserInfo userInfo = MainApplication.getInstance().getUserCacheHelper().getUserInfoSync(context, other);
					if(userInfo != null) other = userInfo.getContactName();
				}
				
				String summary = ConversationManager.ChatRenameActionInfo.getDirectSummary(context, agent, other);
				
				//Closing the cursor
				cursor.close();
				
				//Returning the light conversation item
				return new ConversationManager.LightConversationItem(summary, date, lastItemID, -1);
			case ConversationManager.ChatCreationMessage.itemType: //Chat creation
				//Returning the light conversation item
				return new ConversationManager.LightConversationItem(context.getString(R.string.message_conversationcreated), date, lastItemID, -1);
		}
		
		//Returning the light message info
		return new ConversationManager.LightConversationItem("", date, lastItemID, -1);

		/* //Getting the indexes
		int senderColumnIndex = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDER);

		//Getting the general message info
		String agent = cursor.isNull(senderColumnIndex) ? null : cursor.getString(senderColumnIndex);
		int viewType = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE));
		long date = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_DATE));

		//Checking if the item is a message
		if(viewType == ConversationManager.MessageInfo.ITEM_TYPE) {
			//Getting the content type
			ConversationManager.ContentType contentType = cursor.isNull(subtypeTypeIndex) ? null : ConversationManager.ContentType.fromIdentifier(cursor.getInt(subtypeTypeIndex));

			//Checking if the content type is a text message
			if(contentType == ConversationManager.ContentType.TEXT) {
				//Getting the message info
				String message = cursor.getString(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT));
				String sendEffect = cursor.getString(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDSTYLE));

				//Setting the light conversation item
				return new ConversationManager.LightConversationItem(agent, message, new DateTime(date), sendEffect);
			} else {
				//Setting the light conversation item
				return new ConversationManager.LightConversationItem(agent, contentType, new DateTime(date));
			}
		}
		//Otherwise checking if the item is an action
		else if(viewType == ConversationManager.ActionInfo.ITEM_TYPE) {
			//Getting the other
			String other = cursor.getString(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_OTHER));

			//Getting the action type
			int subtypeTypeIndex = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SUBTYPE);
			ConversationManager.ActionType actionType = cursor.isNull(subtypeTypeIndex) ? null : ConversationManager.ActionType.fromIdentifier(cursor.getInt(subtypeTypeIndex));

			//Setting the light conversation item
			return new ConversationManager.LightConversationItem(agent, other, actionType, new DateTime(date));
		}

		//Returning null
		return null; */
	}
	
	ConversationManager.ConversationInfo addRetrieveServerCreatedConversationInfo(Context context, String guid) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Returning the existing conversation if one already exists
		Cursor cursor = database.query(Contract.ConversationEntry.TABLE_NAME, null, Contract.ConversationEntry.COLUMN_NAME_GUID + " = ?", new String[]{guid}, null, null, null, "1");
		if(cursor.getCount() > 0) {
			cursor.close();
			return fetchConversationInfo(context, guid);
		}
		cursor.close();
		
		//Setting the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_GUID, guid);
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_STATE, ConversationManager.ConversationInfo.ConversationState.INCOMPLETE_SERVER.getIdentifier());
		
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
		return new ConversationManager.ConversationInfo(localID, guid, ConversationManager.ConversationInfo.ConversationState.INCOMPLETE_SERVER);
	}
	
	ConversationManager.ConversationInfo addReadyConversationInfo(Context context, Blocks.ConversationInfo structConversationInfo) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Deleting the conversation if it exists in the database
		long existingLocalID = -1;
		Cursor cursor = database.query(Contract.ConversationEntry.TABLE_NAME, new String[]{Contract.ConversationEntry._ID}, Contract.ConversationEntry.COLUMN_NAME_GUID + " = ?", new String[]{structConversationInfo.guid}, null, null, null, "1");
		if(cursor.moveToNext()) {
			existingLocalID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.ConversationEntry._ID));
			database.delete(Contract.ConversationEntry.TABLE_NAME, Contract.ConversationEntry.COLUMN_NAME_GUID + " = ?", new String[]{structConversationInfo.guid});
		}
		cursor.close();
		
		//Picking a color
		int conversationColor = ConversationManager.ConversationInfo.getDefaultConversationColor(structConversationInfo.guid);
		
		//Setting the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_GUID, structConversationInfo.guid);
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_STATE, ConversationManager.ConversationInfo.ConversationState.READY.getIdentifier());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_SERVICE, structConversationInfo.service);
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_NAME, structConversationInfo.name);
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_COLOR, conversationColor);
		
		//Inserting the conversation into the database
		long localID;
		try {
			localID = database.insertOrThrow(Contract.ConversationEntry.TABLE_NAME, null, contentValues);
		} catch(SQLiteConstraintException exception) {
			//Printing the exception's stack trace
			exception.printStackTrace();
			
			//Returning null
			return null;
		}
		
		//Creating and configuring the conversation info
		ConversationManager.ConversationInfo conversationInfo = new ConversationManager.ConversationInfo(localID, structConversationInfo.guid, ConversationManager.ConversationInfo.ConversationState.READY);
		conversationInfo.setService(structConversationInfo.service);
		conversationInfo.setConversationColor(conversationColor);
		conversationInfo.setTitle(context, structConversationInfo.name);
		conversationInfo.setConversationMembersCreateColors(structConversationInfo.members);
		
		//Adding the members
		if(existingLocalID != -1) database.delete(Contract.MemberEntry.TABLE_NAME, Contract.MemberEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(existingLocalID)}); //Deleting the existing members
		for(ConversationManager.MemberInfo member : conversationInfo.getConversationMembers()) {
			contentValues = new ContentValues();
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_MEMBER, member.getName());
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_COLOR, member.getColor());
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_CHAT, localID);
			
			database.insert(Contract.MemberEntry.TABLE_NAME, null, contentValues);
		}
		
		//Returning the conversation info
		return conversationInfo;
	}
	
	ConversationManager.ConversationInfo addRetrieveClientCreatedConversationInfo(Context context, List<String> members, String service) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Returning an existing conversation if it exists
		ConversationManager.ConversationInfo existingConversation = findConversationInfoWithMembers(context, members, service, false);
		if(existingConversation != null) return existingConversation;
		
		//Picking a conversation color
		int conversationColor = ConversationManager.ConversationInfo.getRandomColor();
		
		//Setting the content values
		ContentValues contentValues = new ContentValues();
		//contentValues.put(Contract.ConversationEntry.COLUMN_NAME_GUID, message); //Conversation not serverlinked, so no GUID can be provided
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_STATE, ConversationManager.ConversationInfo.ConversationState.INCOMPLETE_CLIENT.getIdentifier());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_COLOR, conversationColor);
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_SERVICE, service);
		
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
		
		//Creating the conversation info
		ConversationManager.ConversationInfo conversationInfo = new ConversationManager.ConversationInfo(localID, ConversationManager.ConversationInfo.ConversationState.INCOMPLETE_CLIENT);
		conversationInfo.setConversationColor(conversationColor);
		conversationInfo.setService(service);
		
		//Adding the members
		conversationInfo.setConversationMembersCreateColors(members.toArray(new String[0]));
		
		//Adding the conversation members
		for(ConversationManager.MemberInfo member : conversationInfo.getConversationMembers()) {
			//Setting the content values
			contentValues = new ContentValues();
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_CHAT, localID);
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_MEMBER, member.getName());
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_COLOR, member.getColor());
			
			//Inserting the data
			database.insert(Contract.MemberEntry.TABLE_NAME, null, contentValues);
		}
		
		//Adding the conversation created message
		ConversationManager.ConversationItem createdMessage = new ConversationManager.ChatCreationMessage(localID, System.currentTimeMillis(), conversationInfo);
		conversationInfo.trySetLastItem(createdMessage.toLightConversationItemSync(context), false);
		//conversationInfo.addConversationItems(context, Arrays.asList(createdMessage));
		
		contentValues = new ContentValues();
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_DATE, createdMessage.getDate());
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE, createdMessage.getItemType());
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_CHAT, createdMessage.getConversationInfo().getLocalID());
		
		database.insert(Contract.MessageEntry.TABLE_NAME, null, contentValues);
		
		//Returning the conversation info
		return conversationInfo;
	}
	
	ConversationManager.ConversationInfo findConversationInfoWithMembers(Context context, List<String> members, String service, boolean clientIncompleteOnly) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Getting all conversation identifiers
		Cursor conversationCursor = database.query(Contract.ConversationEntry.TABLE_NAME, new String[]{Contract.ConversationEntry._ID},
				Contract.ConversationEntry.COLUMN_NAME_SERVICE + " = ?" + (clientIncompleteOnly ? " AND " + Contract.ConversationEntry.COLUMN_NAME_STATE + " = ?" : ""),
				clientIncompleteOnly ? new String[]{service, Integer.toString(ConversationManager.ConversationInfo.ConversationState.INCOMPLETE_CLIENT.getIdentifier())} : new String[]{service},
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
			Constants.normalizeAddresses(conversationMembers);
			
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
	
	ConversationManager.ConversationInfo fetchConversationInfo(Context context, String conversationGUID) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Querying the database
		Cursor cursor = database.query(Contract.ConversationEntry.TABLE_NAME, sqlQueryConversationData, Contract.ConversationEntry.COLUMN_NAME_GUID + " = ?", new String[]{conversationGUID}, null, null, null);
		
		//Returning null if there are no results
		if(!cursor.moveToNext()) {
			cursor.close();
			return null;
		}
		
		//Getting the conversation info
		long localID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.ConversationEntry._ID));
		ConversationManager.ConversationInfo.ConversationState conversationState = ConversationManager.ConversationInfo.ConversationState.fromIdentifier(cursor.getInt(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_STATE)));
		String service = cursor.getString(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_SERVICE));
		String chatTitle = cursor.getString(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_NAME));
		int chatUnreadMessages = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_UNREADMESSAGECOUNT));
		boolean chatArchived = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_ARCHIVED)) != 0;
		boolean chatMuted = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_MUTED)) != 0;
		int chatColor = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_COLOR));
		ConversationManager.LightConversationItem lightItem = getLightItem(context, localID);
		String draftMessage = cursor.getString(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_DRAFTMESSAGE));
		long draftUpdateTime = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_DRAFTUPDATETIME));
		
		//Closing the cursor
		cursor.close();
		
		//Returning an empty conversation if it isn't complete
		//if(conversationState != ConversationManager.ConversationInfo.ConversationState.READY) return new ConversationManager.ConversationInfo(localID, conversationGUID, conversationState); //TODO check if this line actually does anything important
		
		//Getting the members and drafts
		ArrayList<ConversationManager.MemberInfo> conversationMembers = loadConversationMembers(database, localID);
		ArrayList<ConversationManager.DraftFile> draftFiles = loadDraftFiles(database, context, localID);
		
		//Creating the conversation info
		ConversationManager.ConversationInfo conversationInfo = new ConversationManager.ConversationInfo(localID, conversationGUID, conversationState, service, conversationMembers, chatTitle, chatUnreadMessages, chatColor, draftMessage, draftFiles, draftUpdateTime);
		conversationInfo.setArchived(chatArchived);
		conversationInfo.setMuted(chatMuted);
		conversationInfo.trySetLastItem(lightItem, false);
		
		//Returning the conversation info
		return conversationInfo;
	}
	
	ConversationManager.ConversationInfo fetchConversationInfo(Context context, long localID) {
		//Getting the database
		SQLiteDatabase database = getReadableDatabase();
		
		//Querying the database
		try(Cursor cursor = database.query(Contract.ConversationEntry.TABLE_NAME, new String[]{
				Contract.ConversationEntry.COLUMN_NAME_GUID,
				Contract.ConversationEntry.COLUMN_NAME_STATE,
				Contract.ConversationEntry.COLUMN_NAME_SERVICE,
				Contract.ConversationEntry.COLUMN_NAME_NAME,
				Contract.ConversationEntry.COLUMN_NAME_UNREADMESSAGECOUNT,
				Contract.ConversationEntry.COLUMN_NAME_ARCHIVED,
				Contract.ConversationEntry.COLUMN_NAME_MUTED,
				Contract.ConversationEntry.COLUMN_NAME_COLOR,
				Contract.ConversationEntry.COLUMN_NAME_DRAFTMESSAGE,
				Contract.ConversationEntry.COLUMN_NAME_DRAFTUPDATETIME}, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(localID)}, null, null, null)) {
			//Returning null if there are no results
			if(!cursor.moveToNext()) return null;
			
			//Getting the conversation info
			String conversationGUID = cursor.getString(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_GUID));
			ConversationManager.ConversationInfo.ConversationState conversationState = ConversationManager.ConversationInfo.ConversationState.fromIdentifier(cursor.getInt(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_STATE)));
			String service = cursor.getString(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_SERVICE));
			String chatTitle = cursor.getString(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_NAME));
			int chatUnreadMessages = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_UNREADMESSAGECOUNT));
			boolean chatArchived = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_ARCHIVED)) != 0;
			boolean chatMuted = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_MUTED)) != 0;
			int chatColor = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_COLOR));
			ConversationManager.LightConversationItem lightItem = getLightItem(context, localID);
			String draftMessage = cursor.getString(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_DRAFTMESSAGE));
			long draftUpdateTime = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.ConversationEntry.COLUMN_NAME_DRAFTUPDATETIME));
			
			//Closing the cursor
			cursor.close();
			
			//Returning an empty conversation if it isn't complete
			//if(conversationState != ConversationManager.ConversationInfo.ConversationState.READY) return new ConversationManager.ConversationInfo(localID, conversationGUID, conversationState); //TODO check if this line actually does anything important
			
			//Getting the members and drafts
			ArrayList<ConversationManager.MemberInfo> conversationMembers = loadConversationMembers(database, localID);
			ArrayList<ConversationManager.DraftFile> draftFiles = loadDraftFiles(database, context, localID);
			
			//Creating the conversation info
			ConversationManager.ConversationInfo conversationInfo = new ConversationManager.ConversationInfo(localID, conversationGUID, conversationState, service, conversationMembers, chatTitle, chatUnreadMessages, chatColor, draftMessage, draftFiles, draftUpdateTime);
			conversationInfo.setArchived(chatArchived);
			conversationInfo.setMuted(chatMuted);
			conversationInfo.trySetLastItem(lightItem, false);
			
			//Returning the conversation info
			return conversationInfo;
		}
	}
	
	private ArrayList<ConversationManager.MemberInfo> loadConversationMembers(SQLiteDatabase database, long chatID) {
		ArrayList<ConversationManager.MemberInfo> conversationMembers = new ArrayList<>();
		try(Cursor cursor = database.query(Contract.MemberEntry.TABLE_NAME, new String[]{Contract.MemberEntry.COLUMN_NAME_MEMBER, Contract.MemberEntry.COLUMN_NAME_COLOR}, Contract.MemberEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(chatID)}, null, null, null)) {
			int indexMember = cursor.getColumnIndexOrThrow(Contract.MemberEntry.COLUMN_NAME_MEMBER);
			int indexColor = cursor.getColumnIndexOrThrow(Contract.MemberEntry.COLUMN_NAME_COLOR);
			while(cursor.moveToNext()) conversationMembers.add(new ConversationManager.MemberInfo(cursor.getString(indexMember), cursor.getInt(indexColor)));
		}
		return conversationMembers;
	}
	
	private ArrayList<ConversationManager.DraftFile> loadDraftFiles(SQLiteDatabase database, Context context, long chatID) {
		ArrayList<ConversationManager.DraftFile> draftFiles = new ArrayList<>();
		try(Cursor cursor = database.query(Contract.DraftFileEntry.TABLE_NAME, new String[]{Contract.DraftFileEntry._ID, Contract.DraftFileEntry.COLUMN_NAME_FILE, Contract.DraftFileEntry.COLUMN_NAME_FILENAME, Contract.DraftFileEntry.COLUMN_NAME_FILESIZE, Contract.DraftFileEntry.COLUMN_NAME_FILETYPE, Contract.DraftFileEntry.COLUMN_NAME_ORIGINALPATH, Contract.DraftFileEntry.COLUMN_NAME_MODIFICATIONDATE}, Contract.DraftFileEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(chatID)}, null, null, null)) {
			int indexIdentifier = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry._ID);
			int indexFile = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_FILE);
			int indexFileName = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_FILENAME);
			int indexFileSize = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_FILESIZE);
			int indexFileType = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_FILETYPE);
			int indexOriginalPath = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_ORIGINALPATH);
			int indexModificationDate = cursor.getColumnIndexOrThrow(Contract.DraftFileEntry.COLUMN_NAME_MODIFICATIONDATE);
			while(cursor.moveToNext()) {
				String originalPath = cursor.getString(indexOriginalPath);
				draftFiles.add(new ConversationManager.DraftFile(
						cursor.getLong(indexIdentifier),
						ConversationManager.DraftFile.getAbsolutePath(context, cursor.getString(indexFile)),
						cursor.getString(indexFileName),
						cursor.getLong(indexFileSize),
						cursor.getString(indexFileType),
						originalPath == null ? null : new File(originalPath),
						cursor.getLong(indexModificationDate)));
			}
		}
		return draftFiles;
	}
	
	ConversationManager.ConversationItem addConversationItemReplaceGhost(Blocks.ConversationItem conversationItem, ConversationManager.ConversationInfo conversationInfo) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Checking if the item is a message
		if(conversationItem instanceof Blocks.MessageInfo) {
			//Getting the message info
			Blocks.MessageInfo messageStruct = (Blocks.MessageInfo) conversationItem;
			
			//Checking if the message is outgoing
			if(messageStruct.sender == null) {
				//Creating the content values
				ContentValues contentValues = new ContentValues();
				contentValues.put(Contract.MessageEntry.COLUMN_NAME_SERVERID, messageStruct.serverID);
				contentValues.put(Contract.MessageEntry.COLUMN_NAME_DATE, messageStruct.date);
				contentValues.put(Contract.MessageEntry.COLUMN_NAME_GUID, messageStruct.guid);
				contentValues.put(Contract.MessageEntry.COLUMN_NAME_STATE, messageStruct.stateCode);
				contentValues.put(Contract.MessageEntry.COLUMN_NAME_ERROR, messageStruct.errorCode);
				contentValues.put(Contract.MessageEntry.COLUMN_NAME_DATEREAD, messageStruct.dateRead);
				contentValues.put(Contract.MessageEntry.COLUMN_NAME_CHAT, conversationInfo.getLocalID());
				
				//Checking if the message is a text message
				if(messageStruct.text != null && messageStruct.attachments.isEmpty()) {
					//Finding a matching row
					try(Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry._ID, Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED},
							Contract.MessageEntry.COLUMN_NAME_STATE + " = ? AND " + Contract.MessageEntry.COLUMN_NAME_SENDER + " IS NULL AND " + Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT + " = ? AND " + Contract.MessageEntry.COLUMN_NAME_CHAT + " = ?",
							new String[]{Integer.toString(Blocks.MessageInfo.stateCodeGhost), messageStruct.text, Long.toString(conversationInfo.getLocalID())},
							null, null, messageSortOrder + " DESC", "1")) {
						//Checking if there are any results
						if(cursor.moveToFirst()) {
							//Getting the message identifier
							long messageID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID));
							
							//Updating the message
							try {
								database.update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(messageID)});
							} catch(SQLiteConstraintException exception) {
								//Printing the stack trace
								exception.printStackTrace();
								
								//Returning
								return null;
							}
							
							//Adding the associations
							List<ConversationManager.StickerInfo> stickers = addMessageStickers(messageID, messageStruct.stickers);
							List<ConversationManager.TapbackInfo> tapbacks = addMessageTapbacks(messageID, messageStruct.tapbacks);
							
							//Getting the client-relevant message information
							boolean sendStyleViewed = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED)) != 0;
							
							//Creating and returning the message
							ConversationManager.MessageInfo messageInfo = new ConversationManager.MessageInfo(messageID, messageStruct.serverID, messageStruct.guid, conversationInfo, messageStruct.sender, messageStruct.text, new ArrayList<>(), messageStruct.sendEffect, sendStyleViewed, messageStruct.date, messageStruct.stateCode, messageStruct.errorCode, messageStruct.dateRead);
							for(ConversationManager.StickerInfo sticker : stickers) messageInfo.addSticker(sticker);
							for(ConversationManager.TapbackInfo tapback : tapbacks) messageInfo.addTapback(tapback);
							return messageInfo;
						}
					}
				} else if(messageStruct.attachments.size() == 1 && messageStruct.attachments.get(0).checksum != null) {
					//Getting the checksum
					byte[] attachmentChecksum = messageStruct.attachments.get(0).checksum;
					
					//Finding a matching row
					/* Cursor cursor = writableDatabase.query(Contract.AttachmentEntry.TABLE_NAME, new String[]{Contract.AttachmentEntry.COLUMN_NAME_MESSAGE},
							Contract.MessageEntry.COLUMN_NAME_STATE + " = ? AND " + Contract.MessageEntry.COLUMN_NAME_SENDER + " = ? AND " + Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM + " = ?",
							new String[]{Integer.toString(Blocks.MessageInfo.stateCodeGhost), null, "x'" + Constants.bytesToHex(attachmentHash)}, null, null, null, "1"); */
					try(Cursor cursor = database.rawQuery("SELECT " + Contract.AttachmentEntry.TABLE_NAME + '.' + Contract.AttachmentEntry._ID + ',' + Contract.AttachmentEntry.TABLE_NAME + '.' + Contract.AttachmentEntry.COLUMN_NAME_FILEPATH + ',' + Contract.AttachmentEntry.TABLE_NAME + '.' + Contract.AttachmentEntry.COLUMN_NAME_MESSAGE + " FROM " + Contract.AttachmentEntry.TABLE_NAME +
									" JOIN " + Contract.MessageEntry.TABLE_NAME + " ON " + Contract.AttachmentEntry.COLUMN_NAME_MESSAGE + " = " + Contract.MessageEntry.TABLE_NAME + '.' + Contract.MessageEntry._ID +
									" WHERE " + Contract.MessageEntry.COLUMN_NAME_STATE + " = " + Blocks.MessageInfo.stateCodeGhost + " AND " + Contract.MessageEntry.COLUMN_NAME_SENDER + " IS NULL AND " + Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM + " = '" + Base64.encodeToString(attachmentChecksum, Base64.NO_WRAP) + "' AND " + Contract.MessageEntry.COLUMN_NAME_CHAT + " = " + conversationInfo.getLocalID() +
									" ORDER BY " + messageSortOrder + " DESC" +
									" LIMIT 1;",
							null)) {
						//Checking if there are any results
						if(cursor.moveToFirst()) {
							//Getting the identifiers
							long attachmentID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.AttachmentEntry._ID));
							String attachmentFilePath = cursor.getString(cursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH));
							long messageID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_MESSAGE));
							
							//Checking if a file could be found
							if(attachmentFilePath != null) {
								//Getting the attachment file
								File attachmentFile = ConversationManager.AttachmentInfo.getAbsolutePath(MainApplication.getInstance(), attachmentFilePath);
								
								//Updating the message
								try {
									database.update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(messageID)});
								} catch(SQLiteConstraintException exception) {
									exception.printStackTrace();
									return null;
								}
								
								//Updating the attachment's GUID
								contentValues = new ContentValues();
								contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_GUID, messageStruct.attachments.get(0).guid);
								try {
									database.update(Contract.AttachmentEntry.TABLE_NAME, contentValues, Contract.AttachmentEntry._ID + " = ?", new String[]{Long.toString(attachmentID)});
								} catch(SQLiteConstraintException exception) {
									//Printing the stack trace
									exception.printStackTrace();
									
									//Returning
									return null;
								}
								
								//Adding the associations
								List<ConversationManager.StickerInfo> stickers = addMessageStickers(messageID, messageStruct.stickers);
								List<ConversationManager.TapbackInfo> tapbacks = addMessageTapbacks(messageID, messageStruct.tapbacks);
								
								//Fetching the message information
								boolean entryFound = false;
								boolean sendStyleViewed = false;
								try(Cursor messageCursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED}, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(messageID)}, null, null, null)) {
									if(messageCursor.moveToFirst()) {
										entryFound = true;
										sendStyleViewed = messageCursor.getInt(messageCursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED)) != 0;
									}
								}
								
								//Checking if an entry was found
								if(entryFound) {
									//Creating the message
									ConversationManager.MessageInfo messageInfo = new ConversationManager.MessageInfo(messageID, messageStruct.serverID, messageStruct.guid, conversationInfo, messageStruct.sender, messageStruct.text, new ArrayList<>(), messageStruct.sendEffect, sendStyleViewed, messageStruct.date, messageStruct.stateCode, messageStruct.errorCode, messageStruct.dateRead);
									for(ConversationManager.StickerInfo sticker : stickers) messageInfo.addSticker(sticker);
									for(ConversationManager.TapbackInfo tapback : tapbacks) messageInfo.addTapback(tapback);
									
									//Creating the attachment
									Blocks.AttachmentInfo attachmentStruct = messageStruct.attachments.get(0);
									ConversationManager.AttachmentInfo attachment = ConversationManager.createAttachmentInfoFromType(attachmentID, attachmentStruct.guid, messageInfo, attachmentStruct.name, attachmentStruct.type, attachmentStruct.size, attachmentFile);
									
									//Setting the checksum
									attachment.setFileChecksum(attachmentStruct.checksum);
									
									//Adding the attachment
									messageInfo.addAttachment(attachment);
									
									//Returning the message
									return messageInfo;
								}
							}
						}
					}
				}
			}
		}
		
		//Adding the conversation item normally
		return addConversationItem(conversationItem, conversationInfo);
	}
	
	void transferConversationItemReplaceGhost(ConversationManager.ConversationItem conversationItem, ConversationManager.ConversationInfo conversationInfo) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Checking if the item is a message
		if(conversationItem instanceof ConversationManager.MessageInfo) {
			//Getting the message info
			ConversationManager.MessageInfo message = (ConversationManager.MessageInfo) conversationItem;
			
			//Checking if the message is outgoing
			if(message.getSender() == null) {
				//Creating the content values
				ContentValues contentValues = new ContentValues();
				if(message.getServerID() == -1) contentValues.putNull(Contract.MessageEntry.COLUMN_NAME_SERVERID);
				else contentValues.put(Contract.MessageEntry.COLUMN_NAME_SERVERID, message.getServerID());
				contentValues.put(Contract.MessageEntry.COLUMN_NAME_GUID, message.getGuid());
				contentValues.put(Contract.MessageEntry.COLUMN_NAME_DATE, message.getDate());
				contentValues.put(Contract.MessageEntry.COLUMN_NAME_STATE, message.getMessageState());
				contentValues.put(Contract.MessageEntry.COLUMN_NAME_ERROR, message.getErrorCode());
				contentValues.put(Contract.MessageEntry.COLUMN_NAME_DATEREAD, message.getDateRead());
				contentValues.put(Contract.MessageEntry.COLUMN_NAME_CHAT, conversationInfo.getLocalID());
				
				//Checking if the message is a text message
				if(message.getMessageText() != null && message.getAttachments().isEmpty()) {
					//Finding a matching row
					try(Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry._ID},
							Contract.MessageEntry.COLUMN_NAME_STATE + " = ? AND " + Contract.MessageEntry.COLUMN_NAME_SENDER + " IS NULL AND " + Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT + " = ? AND " + Contract.MessageEntry.COLUMN_NAME_CHAT + " = ?",
							new String[]{Integer.toString(Blocks.MessageInfo.stateCodeGhost), message.getMessageText(), Long.toString(conversationInfo.getLocalID())},
							null, null, messageSortOrder + " DESC", "1")) {
						//Checking if there are any results
						if(cursor.moveToFirst()) {
							//Getting the message identifier
							long messageID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID));
							
							try {
								//Deleting the provided message
								database.delete(Contract.MessageEntry.TABLE_NAME, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(message.getLocalID())});
								
								//Updating the message
								database.update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(messageID)});
							} catch(SQLiteConstraintException exception) {
								//Printing the stack trace
								exception.printStackTrace();
								
								//Returning
								return;
							}
							
							//Updating the associations
							transferMessageStickers(messageID, message.getStickers());
							transferMessageTapbacks(messageID, message.getTapbacks());
							
							//Returning
							return;
							
							//Getting the client-relevant message information
							/* boolean sendStyleViewed = cursor.getInt(cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED)) != 0;
							
							//Creating and returning the message
							ConversationManager.MessageInfo messageInfo = new ConversationManager.MessageInfo(messageID, message.getGuid(), conversationInfo, message.getSender(), message.getMessageText(), message.getAttachments(), message.getSendStyle(), sendStyleViewed, message.getDate(), message.getMessageState(), message.getErrorCode(), message.getDateRead());
							return messageInfo; */
						}
					}
				} else if(message.getAttachments().size() == 1 && message.getAttachments().get(0).getFileChecksum() != null) {
					//Getting the checksum
					byte[] attachmentChecksum = message.getAttachments().get(0).getFileChecksum();
					
					//Finding a matching row
					/* Cursor cursor = writableDatabase.query(Contract.AttachmentEntry.TABLE_NAME, new String[]{Contract.AttachmentEntry.COLUMN_NAME_MESSAGE},
							Contract.MessageEntry.COLUMN_NAME_STATE + " = ? AND " + Contract.MessageEntry.COLUMN_NAME_SENDER + " = ? AND " + Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM + " = ?",
							new String[]{Integer.toString(Blocks.MessageInfo.stateCodeGhost), null, "x'" + Constants.bytesToHex(attachmentHash)}, null, null, null, "1"); */
					try(Cursor cursor = database.rawQuery("SELECT " + Contract.AttachmentEntry.TABLE_NAME + '.' + Contract.AttachmentEntry._ID + ',' + Contract.AttachmentEntry.TABLE_NAME + '.' + Contract.AttachmentEntry.COLUMN_NAME_FILEPATH + ',' + Contract.AttachmentEntry.TABLE_NAME + '.' + Contract.AttachmentEntry.COLUMN_NAME_MESSAGE + " FROM " + Contract.AttachmentEntry.TABLE_NAME +
									" JOIN " + Contract.MessageEntry.TABLE_NAME + " ON " + Contract.AttachmentEntry.COLUMN_NAME_MESSAGE + " = " + Contract.MessageEntry.TABLE_NAME + '.' + Contract.MessageEntry._ID +
									" WHERE " + Contract.MessageEntry.COLUMN_NAME_STATE + " = " + Blocks.MessageInfo.stateCodeGhost + " AND " + Contract.MessageEntry.COLUMN_NAME_SENDER + " IS NULL AND " + Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM + " = '" + Base64.encodeToString(attachmentChecksum, Base64.NO_WRAP) + "' AND " + Contract.MessageEntry.COLUMN_NAME_CHAT + " = " + conversationInfo.getLocalID() +
									" ORDER BY " + messageSortOrder + " DESC" +
									" LIMIT 1;",
							null)) {
						//Checking if there are any results
						if(cursor.moveToFirst()) {
							//Getting the identifiers
							long attachmentID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.AttachmentEntry._ID));
							String attachmentFilePath = cursor.getString(cursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH));
							long messageID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.AttachmentEntry.COLUMN_NAME_MESSAGE));
							
							//Checking if a file could be found
							if(attachmentFilePath != null) {
								//Getting the attachment file
								//File attachmentFile = ConversationManager.AttachmentInfo.getAbsolutePath(MainApplication.getInstance(), attachmentFilePath);
								
								try {
									//Deleting the original message
									database.delete(Contract.MessageEntry.TABLE_NAME, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(message.getLocalID())});
									
									//Updating the message
									database.update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(messageID)});
								} catch(SQLiteConstraintException exception) {
									exception.printStackTrace();
									return;
								}
								
								//Building the content values
								contentValues = new ContentValues();
								contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_GUID, message.getAttachments().get(0).guid);
								
								try {
									//Deleting the provided attachment
									database.delete(Contract.AttachmentEntry.TABLE_NAME, Contract.AttachmentEntry._ID + " = ?", new String[]{Long.toString(message.getAttachments().get(0).getLocalID())});
									
									//Updating the attachment
									database.update(Contract.AttachmentEntry.TABLE_NAME, contentValues, Contract.AttachmentEntry._ID + " = ?", new String[]{Long.toString(attachmentID)});
								} catch(SQLiteConstraintException exception) {
									//Printing the stack trace
									exception.printStackTrace();
									
									//Returning
									return;
								}
								
								//Fetching the message information
								/* boolean entryFound = false;
								boolean sendStyleViewed = false;
								try(Cursor messageCursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED}, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(messageID)}, null, null, null)) {
									if(messageCursor.moveToFirst()) {
										entryFound = true;
										sendStyleViewed = messageCursor.getInt(messageCursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED)) != 0;
									}
								}
								
								//Checking if an entry was found
								if(entryFound) {
									//Creating the message
									ConversationManager.MessageInfo messageInfo = new ConversationManager.MessageInfo(messageID, message.getGuid(), conversationInfo, message.getSender(), message.getMessageText(), message.getAttachments(), message.getSendStyle(), sendStyleViewed, message.date, message.stateCode, message.errorCode, message.dateRead);
									
									//Updating the associations
									transferMessageStickers(messageID, message.getStickers());
									transferMessageTapbacks(messageID, message.getTapbacks());
									
									//Transferring the file checksum
									messageInfo.getAttachments().get(0).setFileChecksum(message.getAttachments().get(0).getFileChecksum());
									
									//Returning the message
									return messageInfo;
								} */
							}
						}
					}
				}
			}
		}
		
		//Doing a standard conversation item ownership transfer
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_CHAT, conversationInfo.getLocalID());
		
		getWritableDatabase().update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(conversationItem.getLocalID())});
	}
	
	private void transferMessageStickers(long messageID, List<ConversationManager.StickerInfo> stickers) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the stickers
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.StickerEntry.COLUMN_NAME_MESSAGE, messageID);
		
		//Changing the stickers' local message ID
		for(ConversationManager.StickerInfo sticker : stickers) {
			database.update(Contract.StickerEntry.TABLE_NAME, contentValues, Contract.StickerEntry._ID + " = ?", new String[]{Long.toString(sticker.getLocalID())});
			//sticker.setMessageID(messageID);
		}
	}
	
	private void transferMessageTapbacks(long messageID, List<ConversationManager.TapbackInfo> tapbacks) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the stickers
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.TapbackEntry.COLUMN_NAME_MESSAGE, messageID);
		
		//Changing the stickers' local message ID
		for(ConversationManager.TapbackInfo tapback : tapbacks) {
			database.update(Contract.StickerEntry.TABLE_NAME, contentValues, Contract.StickerEntry._ID + " = ?", new String[]{Long.toString(tapback.getLocalID())});
			//tapback.setMessageID(messageID);
		}
	}
	
	ConversationManager.ConversationItem addConversationItem(Blocks.ConversationItem conversationItem, ConversationManager.ConversationInfo conversationInfo) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the content values and adding the common data
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_SERVERID, conversationItem.serverID);
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_GUID, conversationItem.guid);
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_DATE, conversationItem.date);
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_CHAT, conversationInfo.getLocalID());
		
		//Checking if the item is a message
		if(conversationItem instanceof Blocks.MessageInfo) {
			//Casting the item
			Blocks.MessageInfo messageInfoStruct = (Blocks.MessageInfo) conversationItem;
			
			//Putting the content values
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_SENDER, messageInfoStruct.sender);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE, ConversationManager.MessageInfo.itemType);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT, messageInfoStruct.text);
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
			
			//Adding the associations
			List<ConversationManager.StickerInfo> stickers = addMessageStickers(messageLocalID, messageInfoStruct.stickers);
			List<ConversationManager.TapbackInfo> tapbacks = addMessageTapbacks(messageLocalID, messageInfoStruct.tapbacks);
			
			//Creating the message info
			ConversationManager.MessageInfo messageInfo = new ConversationManager.MessageInfo(messageLocalID, messageInfoStruct.serverID, messageInfoStruct.guid, conversationInfo, messageInfoStruct.sender, messageInfoStruct.text, new ArrayList<>(), messageInfoStruct.sendEffect, false, messageInfoStruct.date, messageInfoStruct.stateCode, messageInfoStruct.errorCode, messageInfoStruct.dateRead);
			for(ConversationManager.StickerInfo sticker : stickers) messageInfo.addSticker(sticker);
			for(ConversationManager.TapbackInfo tapback : tapbacks) messageInfo.addTapback(tapback);
			
			//Iterating over the attachments
			for(Blocks.AttachmentInfo attachmentStruct : messageInfoStruct.attachments) {
				//Creating the content values
				contentValues.clear();
				contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_GUID, attachmentStruct.guid);
				contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_MESSAGE, messageLocalID);
				contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILENAME, attachmentStruct.name);
				contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILETYPE, attachmentStruct.type);
				if(attachmentStruct.size != -1) contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILESIZE, attachmentStruct.size);
				if(attachmentStruct.checksum != null) contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM, Base64.encodeToString(attachmentStruct.checksum, Base64.NO_WRAP));
				
				//Inserting the attachment into the database
				long localID;
				try {
					localID = database.insertOrThrow(Contract.AttachmentEntry.TABLE_NAME, null, contentValues);
				} catch(SQLiteConstraintException exception) {
					//Printing the stack trace
					exception.printStackTrace();
					
					//Skipping the remainder of the iteration
					continue;
				}
				
				//Creating the attachment
				ConversationManager.AttachmentInfo attachment = ConversationManager.createAttachmentInfoFromType(localID, attachmentStruct.guid, messageInfo, attachmentStruct.name, attachmentStruct.type, attachmentStruct.size);
				
				//Adding the attachment to the message
				messageInfo.addAttachment(attachment);
			}
			
			//Returning the message info
			return messageInfo;
		}
		//Otherwise checking if the item is a group action
		else if(conversationItem instanceof Blocks.GroupActionInfo) {
			//Casting the item
			Blocks.GroupActionInfo groupActionInfoStruct = (Blocks.GroupActionInfo) conversationItem;
			
			//Putting the content values
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_SENDER, groupActionInfoStruct.agent);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE, ConversationManager.GroupActionInfo.itemType);
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
			return new ConversationManager.GroupActionInfo(localID, groupActionInfoStruct.serverID, groupActionInfoStruct.guid, conversationInfo, groupActionInfoStruct.groupActionType, groupActionInfoStruct.agent, groupActionInfoStruct.other, groupActionInfoStruct.date);
		}
		//Otherwise checking if the item is a chat rename
		else if(conversationItem instanceof Blocks.ChatRenameActionInfo) {
			//Casting the item
			Blocks.ChatRenameActionInfo chatRenameInfoStruct = (Blocks.ChatRenameActionInfo) conversationItem;
			
			//Putting the content values
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_SENDER, chatRenameInfoStruct.agent);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE, ConversationManager.ChatRenameActionInfo.itemType);
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
			return new ConversationManager.ChatRenameActionInfo(localID, chatRenameInfoStruct.serverID, chatRenameInfoStruct.guid, conversationInfo, chatRenameInfoStruct.agent, chatRenameInfoStruct.newChatName, chatRenameInfoStruct.date);
		}
		
		//Returning null
		return null;
	}
	
	void addConversationItem(ConversationManager.ConversationItem conversationItem) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the content values and adding the common data
		ContentValues contentValues = new ContentValues();
		if(conversationItem.getServerID() == -1) contentValues.putNull(Contract.MessageEntry.COLUMN_NAME_SERVERID);
		else contentValues.put(Contract.MessageEntry.COLUMN_NAME_SERVERID, conversationItem.getServerID());
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_GUID, conversationItem.getGuid());
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_DATE, conversationItem.getDate());
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_CHAT, conversationItem.getConversationInfo().getLocalID());
		
		long itemLocalID = -1;
		
		//Checking if the item is a message
		if(conversationItem instanceof ConversationManager.MessageInfo) {
			//Casting the item
			ConversationManager.MessageInfo messageInfo = (ConversationManager.MessageInfo) conversationItem;
			
			//Putting the content values
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_SENDER, messageInfo.getSender());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE, ConversationManager.MessageInfo.itemType);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT, messageInfo.getMessageText());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_STATE, messageInfo.getMessageState());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ERROR, messageInfo.getErrorCode());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_DATEREAD, messageInfo.getDateRead());
			if(messageInfo.getSendStyle() != null) contentValues.put(Contract.MessageEntry.COLUMN_NAME_SENDSTYLE, messageInfo.getSendStyle());
			
			//Inserting the conversation into the database
			try {
				itemLocalID = database.insertOrThrow(Contract.MessageEntry.TABLE_NAME, null, contentValues);
			} catch(SQLiteConstraintException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Returning
				return;
			}
			
			//Iterating over the attachments
			for(ConversationManager.AttachmentInfo attachment : messageInfo.getAttachments()) {
				//Creating the content values
				contentValues.clear();
				contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_GUID, attachment.guid);
				contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_MESSAGE, itemLocalID);
				contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILENAME, attachment.fileName);
				contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILETYPE, attachment.fileType);
				if(attachment.fileSize != -1) contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILESIZE, attachment.fileSize);
				if(attachment.file != null) contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH, ConversationManager.AttachmentInfo.getRelativePath(MainApplication.getInstance(), attachment.file));
				contentValues.put(Contract.AttachmentEntry.COLUMN_NAME_FILECHECKSUM, attachment.fileChecksum);
				
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
		else if(conversationItem instanceof ConversationManager.GroupActionInfo) {
			//Casting the item
			ConversationManager.GroupActionInfo groupActionInfoStruct = (ConversationManager.GroupActionInfo) conversationItem;
			
			//Putting the content values
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_SENDER, groupActionInfoStruct.agent);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE, ConversationManager.GroupActionInfo.itemType);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ITEMSUBTYPE, groupActionInfoStruct.getActionType());
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_OTHER, groupActionInfoStruct.other);
			
			//Inserting the action into the database
			try {
				itemLocalID = database.insertOrThrow(Contract.AttachmentEntry.TABLE_NAME, null, contentValues);
			} catch(SQLiteConstraintException exception) {
				//Printing the stack trace
				exception.printStackTrace();
			}
		}
		//Otherwise checking if the item is a chat rename
		else if(conversationItem instanceof ConversationManager.ChatRenameActionInfo) {
			//Casting the item
			ConversationManager.ChatRenameActionInfo chatRenameInfoStruct = (ConversationManager.ChatRenameActionInfo) conversationItem;
			
			//Putting the content values
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_SENDER, chatRenameInfoStruct.agent);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_ITEMTYPE, ConversationManager.ChatRenameActionInfo.itemType);
			contentValues.put(Contract.MessageEntry.COLUMN_NAME_OTHER, chatRenameInfoStruct.title);
			
			//Inserting the action into the database
			try {
				itemLocalID = database.insertOrThrow(Contract.AttachmentEntry.TABLE_NAME, null, contentValues);
			} catch(SQLiteConstraintException exception) {
				//Printing the stack trace
				exception.printStackTrace();
			}
		}
		
		//Setting the local ID
		conversationItem.setLocalID(itemLocalID);
	}
	
	void setUnreadMessageCount(long conversationID, int count) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_UNREADMESSAGECOUNT, count);
		
		//Updating the database
		getWritableDatabase().update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(conversationID)});
	}
	
	void incrementUnreadMessageCount(long conversationID) {
		getWritableDatabase().execSQL("UPDATE " + Contract.ConversationEntry.TABLE_NAME + " SET " + Contract.ConversationEntry.COLUMN_NAME_UNREADMESSAGECOUNT + " = " + Contract.ConversationEntry.COLUMN_NAME_UNREADMESSAGECOUNT + " + 1 WHERE " + Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(conversationID)});
	}
	
	private List<ConversationManager.StickerInfo> addMessageStickers(long messageID, List<Blocks.StickerModifierInfo> stickers) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the list
		List<ConversationManager.StickerInfo> list = new ArrayList<>();
		
		//Iterating over the stickers
		ContentValues contentValues;
		for(Blocks.StickerModifierInfo sticker : stickers) {
			//Creating the content values
			contentValues = new ContentValues();
			contentValues.put(Contract.StickerEntry.COLUMN_NAME_GUID, sticker.fileGuid);
			contentValues.put(Contract.StickerEntry.COLUMN_NAME_MESSAGE, messageID);
			contentValues.put(Contract.StickerEntry.COLUMN_NAME_MESSAGEINDEX, sticker.messageIndex);
			contentValues.put(Contract.StickerEntry.COLUMN_NAME_SENDER, sticker.sender);
			contentValues.put(Contract.StickerEntry.COLUMN_NAME_DATE, sticker.date);
			contentValues.put(Contract.StickerEntry.COLUMN_NAME_DATA, sticker.image);
			
			//Inserting the entry
			long rowID;
			try {
				rowID = database.insert(Contract.StickerEntry.TABLE_NAME, null, contentValues);
			} catch(SQLiteConstraintException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Skipping the remainder of the iteration
				continue;
			}
			
			//Adding the sticker to the list
			list.add(new ConversationManager.StickerInfo(rowID, sticker.fileGuid, messageID, sticker.messageIndex, sticker.sender, sticker.date));
		}
		
		//Returning the list
		return list;
	}
	
	private List<ConversationManager.TapbackInfo> addMessageTapbacks(long messageID, List<Blocks.TapbackModifierInfo> tapbacks) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the list
		List<ConversationManager.TapbackInfo> list = new ArrayList<>();
		
		//Iterating over the tapbacks
		ContentValues contentValues;
		for(Blocks.TapbackModifierInfo tapback : tapbacks) {
			//Creating the content values
			contentValues = new ContentValues();
			contentValues.put(Contract.TapbackEntry.COLUMN_NAME_MESSAGE, messageID);
			contentValues.put(Contract.TapbackEntry.COLUMN_NAME_MESSAGEINDEX, tapback.messageIndex);
			contentValues.put(Contract.TapbackEntry.COLUMN_NAME_SENDER, tapback.sender);
			contentValues.put(Contract.TapbackEntry.COLUMN_NAME_CODE, ConversationManager.TapbackInfo.convertToPrivateCode(tapback.code));
			
			//Inserting the entry
			long rowID;
			try {
				rowID = database.insert(Contract.TapbackEntry.TABLE_NAME, null, contentValues);
			} catch(SQLiteConstraintException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Skipping the remainder of the iteration
				continue;
			}
			
			//Adding the tapback to the list
			list.add(new ConversationManager.TapbackInfo(rowID, messageID, tapback.messageIndex, tapback.sender, ConversationManager.TapbackInfo.convertToPrivateCode(tapback.code)));
		}
		
		//Returning the list
		return list;
	}
	
	void updateConversationInfo(ConversationManager.ConversationInfo conversationInfo, boolean updateMembers) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		
		//Updating the conversation data
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_GUID, conversationInfo.getGuid());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_STATE, conversationInfo.getState().getIdentifier());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_SERVICE, conversationInfo.getService());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_NAME, conversationInfo.getStaticTitle());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_COLOR, conversationInfo.getConversationColor());
		
		try {
			database.update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + "=?", new String[]{Long.toString(conversationInfo.getLocalID())});
		} catch(SQLiteConstraintException exception) {
			exception.printStackTrace();
			return;
		}
		
		//Checking if members should be updated
		if(updateMembers) {
			//Looping through all members
			for(ConversationManager.MemberInfo member : conversationInfo.getConversationMembers()) {
				//Putting the data
				contentValues = new ContentValues();
				contentValues.put(Contract.MemberEntry.COLUMN_NAME_MEMBER, member.getName());
				contentValues.put(Contract.MemberEntry.COLUMN_NAME_CHAT, conversationInfo.getLocalID());
				contentValues.put(Contract.MemberEntry.COLUMN_NAME_COLOR, member.getColor());
				
				//Inserting the values into the conversation / users join table
				database.insert(Contract.MemberEntry.TABLE_NAME, null, contentValues);
			}
		}
	}
	
	void copyConversationInfo(ConversationManager.ConversationInfo sourceConversation, ConversationManager.ConversationInfo targetConversation, boolean updateMembers) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		
		//Updating the conversation data
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_GUID, sourceConversation.getGuid());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_STATE, sourceConversation.getState().getIdentifier());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_SERVICE, sourceConversation.getService());
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_NAME, sourceConversation.getStaticTitle());
		
		database.update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + "=?", new String[]{Long.toString(targetConversation.getLocalID())});
		
		//Checking if members should be updated
		if(updateMembers) {
			//Looping through all members
			for(ConversationManager.MemberInfo member : sourceConversation.getConversationMembers()) {
				//Putting the data
				contentValues = new ContentValues();
				contentValues.put(Contract.MemberEntry.COLUMN_NAME_MEMBER, member.getName());
				contentValues.put(Contract.MemberEntry.COLUMN_NAME_CHAT, targetConversation.getLocalID());
				contentValues.put(Contract.MemberEntry.COLUMN_NAME_COLOR, member.getColor());
				
				//Inserting the values into the conversation / users join table
				database.insert(Contract.MemberEntry.TABLE_NAME, null, contentValues);
			}
		}
	}
	
	void updateConversationColor(long conversationID, int newColor) {
		//Creating and setting the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_COLOR, newColor);
		
		//Updating the conversation's color in the database
		getWritableDatabase().update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(conversationID)});
	}
	
	void updateMemberColor(long conversationID, String member, int color) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the content values
		ContentValues contentValues;
		
		//Updating the user's color in the database
		contentValues = new ContentValues();
		contentValues.put(Contract.MemberEntry.COLUMN_NAME_COLOR, color);
		database.update(Contract.MemberEntry.TABLE_NAME, contentValues, Contract.MemberEntry.COLUMN_NAME_CHAT + " = ? AND " + Contract.MemberEntry.COLUMN_NAME_MEMBER + " = ?", new String[]{Long.toString(conversationID), member});
	}
	
	void updateMemberColors(long conversationID, ConversationManager.MemberInfo[] members) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Creating the content values
		ContentValues contentValues;
		
		//Iterating over the members
		for(ConversationManager.MemberInfo member : members) {
			//Updating the user's color in the database
			contentValues = new ContentValues();
			contentValues.put(Contract.MemberEntry.COLUMN_NAME_COLOR, member.getColor());
			database.update(Contract.MemberEntry.TABLE_NAME, contentValues, Contract.MemberEntry.COLUMN_NAME_CHAT + " = ? AND " + Contract.MemberEntry.COLUMN_NAME_MEMBER + " = ?", new String[]{Long.toString(conversationID), member.getName()});
		}
	}
	
	void updateConversationTitle(String title, long conversationID) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_NAME, title);
		
		//Updating the entry
		getWritableDatabase().update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(conversationID)});
	}
	
	void addConversationMember(long chatID, String memberName, int memberColor) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Inserting the content
		/* writableDatabase.execSQL("INSERT INTO " + Contract.MemberEntry.TABLE_NAME + '(' + Contract.MemberEntry.COLUMN_NAME_MEMBER + ',' + Contract.MemberEntry.COLUMN_NAME_CHAT + ',' + Contract.MemberEntry.COLUMN_NAME_COLOR + ')' +
				" SELECT " + chatID + ',' + member.getColor() + ',' + escapedMemberName +
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
	
	void removeConversationMember(long chatID, String member) {
		//Removing the content
		getWritableDatabase().delete(Contract.MemberEntry.TABLE_NAME, Contract.MemberEntry.COLUMN_NAME_CHAT + " = ? AND " + Contract.MemberEntry.COLUMN_NAME_MEMBER + " = ?", new String[]{Long.toString(chatID), member});
	}
	
	void clearAttachmentFiles() {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.putNull(Contract.AttachmentEntry.COLUMN_NAME_FILEPATH);
		
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Updating the database
		database.update(Contract.AttachmentEntry.TABLE_NAME, contentValues, Contract.AttachmentEntry.COLUMN_NAME_FILEPATH + " IS NOT NULL", null);
	}
	
	/* void removeText(long messageID) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		
		//Setting the text to null
		contentValues.putNull(Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT);
		
		//Updating the data
		getWritableDatabase().update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(messageID)});
	}
	
	void removeAttachment(long attachmentID) {
		//Deleting the attachment
		getWritableDatabase().delete(Contract.AttachmentEntry.TABLE_NAME, Contract.AttachmentEntry._ID + " = ?", new String[]{Long.toString(attachmentID)});
	}
	
	void deleteConversationItem(ConversationManager.ConversationItem conversationItem) {
		//Deleting the conversation item
		getWritableDatabase().delete(Contract.MessageEntry.TABLE_NAME, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(conversationItem.getLocalID())});
	} */
	
	void deleteConversation(ConversationManager.ConversationInfo conversationInfo) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Deleting the conversation
		database.delete(Contract.ConversationEntry.TABLE_NAME, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(conversationInfo.getLocalID())});
		
		//Deleting all related messages
		try(Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry._ID}, Contract.MessageEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(conversationInfo.getLocalID())}, null, null, null)) {
			while(cursor.moveToNext()) deleteMessage(cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID)));
		}
		
		//Deleting all related members
		database.delete(Contract.MemberEntry.TABLE_NAME, Contract.MemberEntry.COLUMN_NAME_CHAT + " = ?", new String[]{Long.toString(conversationInfo.getLocalID())});
	}
	
	void deleteMessage(long messageID) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Deleting the message
		database.delete(Contract.MessageEntry.TABLE_NAME, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(messageID)});
		
		//Deleting all related entries
		database.delete(Contract.AttachmentEntry.TABLE_NAME, Contract.AttachmentEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(messageID)});
		database.delete(Contract.StickerEntry.TABLE_NAME, Contract.StickerEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(messageID)});
		database.delete(Contract.TapbackEntry.TABLE_NAME, Contract.TapbackEntry.COLUMN_NAME_MESSAGE + " = ?", new String[]{Long.toString(messageID)});
	}
	
	void deleteEverything() {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Clearing the tables
		String tableNames[] = getTableNames(database);
		for(String table : tableNames) database.delete(table, null, null);
		
		//Shrinking the database
		//database.execSQL("VACUUM;");
		
		/* database.delete(Contract.MessageEntry.TABLE_NAME, null, null);
		database.delete(Contract.ConversationEntry.TABLE_NAME, null, null);
		database.delete(Contract.MemberEntry.TABLE_NAME, null, null);
		database.delete(Contract.AttachmentEntry.TABLE_NAME, null, null);
		database.delete(Contract.StickerEntry.TABLE_NAME, null, null);
		database.delete(Contract.TapbackEntry.TABLE_NAME, null, null); */
	}
	
	void updateConversationMuted(long conversationID, boolean value) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(DatabaseManager.Contract.ConversationEntry.COLUMN_NAME_MUTED, value);
		
		//Updating the conversation
		getWritableDatabase().update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(conversationID)});
	}
	
	void updateConversationArchived(long conversationID, boolean value) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.ConversationEntry.COLUMN_NAME_ARCHIVED, value);
		
		//Updating the conversation
		getWritableDatabase().update(Contract.ConversationEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(conversationID)});
	}
	
	void updateConversationDraftMessage(long conversationID, String value, long time) {
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
	
	void updateMessageErrorCode(long messageID, int errorCode) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_ERROR, errorCode);
		
		//Updating the database
		getWritableDatabase().update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.ConversationEntry._ID + " = ?", new String[]{Long.toString(messageID)});
	}
	
	void updateMessageState(String guid, int state, long dateRead) {
		//Creating the content values
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_STATE, state);
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_DATEREAD, dateRead);
		
		//Updating the entry
		getWritableDatabase().update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.MessageEntry.COLUMN_NAME_GUID + " = ?", new String[]{guid});
	}
	
	ConversationManager.StickerInfo addMessageSticker(Blocks.StickerModifierInfo sticker) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Fetching the local ID of the associated message
		Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry._ID}, Contract.MessageEntry.COLUMN_NAME_GUID + " = ?", new String[]{sticker.message}, null, null, null, "1");
		
		//Returning null if the cursor is empty (the associated message could not be found)
		if(!cursor.moveToNext()) {
			cursor.close();
			return null;
		}
		
		//Getting the ID
		long messageID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID));
		
		//Closing the cursor
		cursor.close();
		
		//Inserting the sticker
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.StickerEntry.COLUMN_NAME_GUID, sticker.fileGuid);
		contentValues.put(Contract.StickerEntry.COLUMN_NAME_MESSAGE, messageID);
		contentValues.put(Contract.StickerEntry.COLUMN_NAME_MESSAGEINDEX, sticker.messageIndex);
		contentValues.put(Contract.StickerEntry.COLUMN_NAME_SENDER, sticker.sender);
		contentValues.put(Contract.StickerEntry.COLUMN_NAME_DATE, sticker.date);
		contentValues.put(Contract.StickerEntry.COLUMN_NAME_DATA, sticker.image);
		
		long localID;
		try {
			localID = database.insertOrThrow(Contract.StickerEntry.TABLE_NAME, null, contentValues);
		} catch(SQLiteConstraintException exception) {
			//Printing the stack trace
			exception.printStackTrace();
			
			//Returning null
			return null;
		}
		
		//Returning the sticker info
		return new ConversationManager.StickerInfo(localID, sticker.fileGuid, messageID, sticker.messageIndex, sticker.sender, sticker.date);
	}
	
	byte[] getStickerBlob(long identifier) {
		try(Cursor cursor = getReadableDatabase().query(Contract.StickerEntry.TABLE_NAME, new String[]{Contract.StickerEntry.COLUMN_NAME_DATA}, Contract.StickerEntry._ID + " = ?", new String[]{Long.toString(identifier)}, null, null, null, "1")) {
			if(cursor.moveToNext()) return cursor.getBlob(0);
			return null;
		}
	}
	
	ConversationManager.TapbackInfo addMessageTapback(Blocks.TapbackModifierInfo tapback) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Fetching the local ID of the associated message
		Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry._ID}, Contract.MessageEntry.COLUMN_NAME_GUID + " = ?", new String[]{tapback.message}, null, null, null, "1");
		
		//Returning null if the cursor is empty (the associated message could not be found)
		if(!cursor.moveToNext()) {
			cursor.close();
			return null;
		}
		
		//Getting the ID
		long messageID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID));
		
		//Closing the cursor
		cursor.close();
		
		//Creating the content values with the code
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.TapbackEntry.COLUMN_NAME_CODE, ConversationManager.TapbackInfo.convertToPrivateCode(tapback.code));
		
		//Updating the matching entry
		int affectedRows = database.updateWithOnConflict(Contract.TapbackEntry.TABLE_NAME, contentValues,
				Contract.TapbackEntry.COLUMN_NAME_MESSAGE + " = ? AND " + Contract.TapbackEntry.COLUMN_NAME_MESSAGEINDEX + " = ? AND " + Contract.TapbackEntry.COLUMN_NAME_SENDER + " = ?", new String[]{Long.toString(messageID), Integer.toString(tapback.messageIndex), tapback.sender}, SQLiteDatabase.CONFLICT_IGNORE);
		
		//Checking if the entry didn't already exist
		if(affectedRows == 0) {
			//Adding the remaining content values
			contentValues.put(Contract.TapbackEntry.COLUMN_NAME_MESSAGE, messageID);
			contentValues.put(Contract.TapbackEntry.COLUMN_NAME_MESSAGEINDEX, tapback.messageIndex);
			contentValues.put(Contract.TapbackEntry.COLUMN_NAME_SENDER, tapback.sender);
			
			//Inserting the modifier
			long localID;
			try {
				localID = database.insertOrThrow(Contract.TapbackEntry.TABLE_NAME, null, contentValues);
			} catch(SQLiteConstraintException exception) {
				//Printing the stack trace
				exception.printStackTrace();
				
				//Returning null
				return null;
			}
			
			//Returning the tapback info
			return new ConversationManager.TapbackInfo(localID, messageID, tapback.messageIndex, tapback.sender, ConversationManager.TapbackInfo.convertToPrivateCode(tapback.code));
		} else {
			//Getting the affected row ID
			cursor = database.query(Contract.TapbackEntry.TABLE_NAME, new String[]{Contract.TapbackEntry._ID},
					Contract.TapbackEntry.COLUMN_NAME_MESSAGE + " = ? AND " + Contract.TapbackEntry.COLUMN_NAME_SENDER + " = ?", new String[]{Long.toString(messageID), tapback.sender},
					null, null, null);
			
			//Returning null if the cursor is empty (the associated message could not be found)
			if(!cursor.moveToNext()) {
				cursor.close();
				return null;
			}
			
			//Getting the ID
			long modifierID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.TapbackEntry._ID));
			
			//Closing the cursor
			cursor.close();
			
			//Returning the tapback info
			return new ConversationManager.TapbackInfo(modifierID, messageID, tapback.messageIndex, tapback.sender, ConversationManager.TapbackInfo.convertToPrivateCode(tapback.code));
		}
	}
	
	void removeMessageTapback(Blocks.TapbackModifierInfo tapback) {
		//Getting the database
		SQLiteDatabase database = getWritableDatabase();
		
		//Fetching the local ID of the associated message
		Cursor cursor = database.query(Contract.MessageEntry.TABLE_NAME, new String[]{Contract.MessageEntry._ID}, Contract.MessageEntry.COLUMN_NAME_GUID + " = ?", new String[]{tapback.message}, null, null, null, "1");
		
		//Returning if the cursor is empty (the associated message could not be found)
		if(!cursor.moveToNext()) {
			cursor.close();
			return;
		}
		
		//Getting the ID
		long messageID = cursor.getLong(cursor.getColumnIndexOrThrow(Contract.MessageEntry._ID));
		
		//Closing the cursor
		cursor.close();
		
		//Deleting the tapback
		database.delete(Contract.TapbackEntry.TABLE_NAME, Contract.TapbackEntry.COLUMN_NAME_MESSAGE + " = ? AND " + Contract.TapbackEntry.COLUMN_NAME_MESSAGEINDEX + " = ? AND " + Contract.TapbackEntry.COLUMN_NAME_SENDER + " = ?", new String[]{Long.toString(messageID), Integer.toString(tapback.messageIndex), tapback.sender});
	}
	
	void markSendStyleViewed(long messageID) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(Contract.MessageEntry.COLUMN_NAME_SENDSTYLEVIEWED, 1);
		getWritableDatabase().update(Contract.MessageEntry.TABLE_NAME, contentValues, Contract.MessageEntry._ID + " = ?", new String[]{Long.toString(messageID)});
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
}