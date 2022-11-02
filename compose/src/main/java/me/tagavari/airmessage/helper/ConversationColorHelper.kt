package me.tagavari.airmessage.helper

import me.tagavari.airmessage.messaging.ConversationInfo
import me.tagavari.airmessage.messaging.MemberInfo
import kotlin.math.abs
import kotlin.random.Random

object ConversationColorHelper {
	@JvmField
	val standardUserColors: Array<Int> = arrayOf(
			0xFFFF1744.toInt(), //Red
			0xFFF50057.toInt(), //Pink
			0xFFB317CF.toInt(), //Purple
			0xFF703BE3.toInt(), //Dark purple
			0xFF3D5AFE.toInt(), //Indigo
			0xFF2979FF.toInt(), //Blue
			0xFF00B0FF.toInt(), //Light blue
			0xFF00B8D4.toInt(), //Cyan
			0xFF00BFA5.toInt(), //Teal
			0xFF00C853.toInt(), //Green
			0xFF5DD016.toInt(), //Light green
			0xFF99CC00.toInt(), //Lime green
			0xFFF2CC0D.toInt(), //Yellow
			0xFFFFC400.toInt(), //Amber
			0xFFFF9100.toInt(), //Orange
			0xFFFF3D00.toInt(), //Deep orange
			//0xFF795548.toInt(), //Brown
			//0xFF607D8B.toInt(), //Blue grey
	)
	const val backupUserColor = 0xFF607D8B.toInt() //Blue grey
	private fun getConversationRandom(conversationInfo: ConversationInfo): Random {
		return when {
			conversationInfo.guid != null -> Random(conversationInfo.guid!!.hashCode().toLong())
			conversationInfo.externalID != -1L -> Random(conversationInfo.externalID)
			else -> Random.Default
		}
	}
	
	@JvmStatic
	fun getDefaultConversationColor(conversationGUID: String): Int {
		return standardUserColors[abs(conversationGUID.hashCode()) % standardUserColors.size]
	}
	
	@JvmStatic
	fun getDefaultConversationColor(conversationExternalID: Long): Int {
		return standardUserColors[(abs(conversationExternalID) % standardUserColors.size).toInt()]
	}
	
	@JvmStatic
	fun getDefaultConversationColor(): Int {
		return standardUserColors[Random.nextInt(standardUserColors.size)]
	}
	
	@JvmStatic
	fun getNextUserColor(conversation: ConversationInfo): Int {
		val availableColors = conversation.members
				.map { it.color }
				//Only allowing standard colors to be counted
				.filter { memberColor -> standardUserColors.any { standardColor -> standardColor == memberColor } }
				//Group and count color usages
				.groupingBy { it }
				.eachCount()
				.entries
				//Filter to only keep results that have the lowest usage count
				.run {
					val leastUsedCount = fold(Int.MAX_VALUE) {acc, entry -> if(entry.value < acc) entry.value else acc}
					filter { entry -> entry.value == leastUsedCount }
				}
				//Map each entry to its color code value
				.map { it.key }
				//Default to standard user colors if there are no values
				.let { if(it.isEmpty()) standardUserColors.toList() else it }
		
		//Picking a least used color
		return availableColors[getConversationRandom(conversation).nextInt(availableColors.size)]
	}
	
	@JvmStatic
	fun getColoredMembers(members: List<String>, conversationColor: Int): MutableList<MemberInfo> {
		return getColoredMembers(members, conversationColor, Random.Default)
	}
	
	@JvmStatic
	fun getColoredMembers(members: List<String>, conversationColor: Int, conversationGUID: String): MutableList<MemberInfo> {
		return getColoredMembers(members, conversationColor, Random(conversationGUID.hashCode().toLong()))
	}
	
	@JvmStatic
	fun getColoredMembers(members: List<String>, conversationColor: Int, conversationExternalID: Long): MutableList<MemberInfo> {
		return getColoredMembers(members, conversationColor, Random(conversationExternalID))
	}
	
	@JvmStatic
	fun getColoredMembers(members: List<String>, conversationColor: Int, random: Random): MutableList<MemberInfo> {
		//Inheriting the conversation color if there is only one member
		return if(members.size == 1) {
			mutableListOf(MemberInfo(members[0], conversationColor))
		} else {
			var palette: MutableList<Int> = mutableListOf()
			
			//Sort the members alphabetically
			members.sorted()
					.map { address ->
						//Restock the palette if it is empty
						if(palette.isEmpty()) palette = standardUserColors.toMutableList()
						
						//Pick a random color from the palette
						val color = palette.random(random)
						palette.remove(color)
						
						MemberInfo(address, color)
					}
					.toMutableList()
		}
	}
}