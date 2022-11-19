package me.tagavari.airmessage.data

import com.google.mlkit.nl.smartreply.TextMessage
import me.tagavari.airmessage.common.data.DatabaseManager
import me.tagavari.airmessage.common.enums.ConversationItemType
import me.tagavari.airmessage.common.helper.SmartReplyHelper

fun DatabaseManager.loadConversationForMLKit(conversationID: Long): MutableList<TextMessage> {
	//Getting the database
	val database = readableDatabase
	
	//Creating the message list
	val messageList = mutableListOf<TextMessage>()
	
	//Querying the database
	val cursor = database.query(DatabaseManager.Contract.MessageEntry.TABLE_NAME, arrayOf(DatabaseManager.Contract.MessageEntry.COLUMN_NAME_SENDER, DatabaseManager.Contract.MessageEntry.COLUMN_NAME_DATE, DatabaseManager.Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT),
			DatabaseManager.Contract.MessageEntry.COLUMN_NAME_ITEMTYPE + " = " + ConversationItemType.message + " AND " + DatabaseManager.Contract.MessageEntry.COLUMN_NAME_CHAT + " = ? AND " + DatabaseManager.Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT + " IS NOT NULL", arrayOf(conversationID.toString()),
			null, null, DatabaseManager.Contract.MessageEntry.COLUMN_NAME_DATE + " DESC", SmartReplyHelper.smartReplyHistoryLength.toString())
	//Cursor cursor = database.rawQuery(SQL_FETCH_CONVERSATION_MESSAGES, new String[]{Long.toString(conversationInfo.getLocalID())});
	
	//Getting the indexes
	val iSender = cursor.getColumnIndexOrThrow(DatabaseManager.Contract.MessageEntry.COLUMN_NAME_SENDER)
	val iDate = cursor.getColumnIndexOrThrow(DatabaseManager.Contract.MessageEntry.COLUMN_NAME_DATE)
	val iMessageText = cursor.getColumnIndexOrThrow(DatabaseManager.Contract.MessageEntry.COLUMN_NAME_MESSAGETEXT)
	//int iOther = cursor.getColumnIndexOrThrow(Contract.MessageEntry.COLUMN_NAME_OTHER);
	
	//Looping while there are items (in reverse order, because Firebase wants newer messages at the start of the list)
	cursor.moveToLast()
	while(!cursor.isBeforeFirst) {
		
		//Getting the message info
		val sender = if(cursor.isNull(iSender)) null else cursor.getString(iSender)
		val date = cursor.getLong(iDate)
		val message = cursor.getString(iMessageText)
		
		//Adding the message to the list
		messageList.add(if(sender == null) TextMessage.createForLocalUser(message, date) else TextMessage.createForRemoteUser(message, date, sender))
		cursor.moveToPrevious()
	}
	
	//Closing the cursor
	cursor.close()
	
	//Returning the conversation items
	return messageList
}