package me.tagavari.airmessage.helper;

import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import me.tagavari.airmessage.messaging.ConversationInfo;
import me.tagavari.airmessage.messaging.MemberInfo;

public class ConversationColorHelper {
	public static final Integer[] standardUserColors = {
			0xFFFF1744, //Red
			0xFFF50057, //Pink
			0xFFB317CF, //Purple
			0xFF703BE3, //Dark purple
			0xFF3D5AFE, //Indigo
			0xFF2979FF, //Blue
			0xFF00B0FF, //Light blue
			0xFF00B8D4, //Cyan
			0xFF00BFA5, //Teal
			0xFF00C853, //Green
			0xFF5DD016, //Light green
			0xFF99CC00, //Lime green
			0xFFF2CC0D, //Yellow
			0xFFFFC400, //Amber
			0xFFFF9100, //Orange
			0xFFFF3D00, //Deep orange
			//0xFF795548, //Brown
			//0xFF607D8B, //Blue grey
	};
	public static final int backupUserColor = 0xFF607D8B; //Blue grey
	
	private static Random getConversationRandom(ConversationInfo conversationInfo) {
		if(conversationInfo.getGUID() != null) return new Random(conversationInfo.getGUID().hashCode());
		else if(conversationInfo.getExternalID() != -1) return new Random(conversationInfo.getExternalID());
		else return new Random();
	}
	
	public static int getDefaultConversationColor(String conversationGUID) {
		return standardUserColors[Math.abs(conversationGUID.hashCode()) % standardUserColors.length];
	}
	
	public static int getDefaultConversationColor(long conversationExternalID) {
		return standardUserColors[(int) (Math.abs(conversationExternalID) % standardUserColors.length)];
	}
	
	public static int getDefaultConversationColor() {
		return standardUserColors[new Random().nextInt(standardUserColors.length)];
	}
	
	public static int getNextUserColor(ConversationInfo conversation) {
		//Creating a list of the user colors
		SparseIntArray colorUses = new SparseIntArray();
		
		//Adding all of the standard colors
		for(int color : standardUserColors) colorUses.put(color, 0);
		
		//Counting the colors
		for(MemberInfo member : conversation.getMembers()) {
			//Only allowing standard colors to be counted
			if(Arrays.stream(standardUserColors).noneMatch(standardColor -> standardColor == member.getColor())) continue;
			
			//Increasing the usage count
			colorUses.put(member.getColor(), colorUses.get(member.getColor(), 0) + 1);
		}
		
		//Finding the smallest use value
		int leastUses = conversation.getMembers().size();
		for(int i = 0; i < colorUses.size(); i++) {
			int uses = colorUses.valueAt(i);
			if(uses >= leastUses) continue;
			leastUses = uses;
			if(leastUses == 0) break;
		}
		
		//Finding all values with the least amount of uses
		ArrayList<Integer> leastUsedColors = new ArrayList<>();
		for(int i = 0; i < colorUses.size(); i++) {
			int uses = colorUses.valueAt(i);
			if(uses != leastUses) continue;
			leastUsedColors.add(colorUses.keyAt(i));
		}
		
		//Picking a least used color
		return leastUsedColors.get(getConversationRandom(conversation).nextInt(leastUsedColors.size()));
	}
	
	public static List<MemberInfo> getColoredMembers(String[] membersArray, int conversationColor) {
		return getColoredMembers(membersArray, conversationColor, new Random());
	}
	
	public static List<MemberInfo> getColoredMembers(String[] membersArray, int conversationColor, String conversationGUID) {
		return getColoredMembers(membersArray, conversationColor, new Random(conversationGUID.hashCode()));
	}
	
	public static List<MemberInfo> getColoredMembers(String[] membersArray, int conversationColor, long conversationExternalID) {
		return getColoredMembers(membersArray, conversationColor, new Random(conversationExternalID));
	}
	
	public static List<MemberInfo> getColoredMembers(String[] membersArray, int conversationColor, Random random) {
		//Inheriting the conversation color if there is only one member
		if(membersArray.length == 1) {
			return new ArrayList<>(Collections.singletonList(new MemberInfo(membersArray[0], conversationColor)));
		} else {
			//Sorting the members
			String[] members = membersArray.clone();
			Arrays.sort(members);
			
			//Getting color values
			int[] colorValues = ConversationColorHelper.getMassUserColors(random, membersArray.length);
			
			//Assigning the color values to the members
			return IntStream.range(0, members.length).mapToObj(i -> new MemberInfo(members[i], colorValues[i])).collect(Collectors.toCollection(ArrayList::new));
		}
	}
	
	private static int[] getMassUserColors(Random random, int userCount) {
		//Creating the color array
		int[] array = new int[userCount];
		
		//Adding the colors
		ArrayList<Integer> colors = new ArrayList<>();
		for(int i = 0; i < userCount; i++) {
			//Getting the colors if there are no more
			if(colors.isEmpty()) colors.addAll(Arrays.asList(standardUserColors));
			
			//Picking a color
			Integer color = colors.get(random.nextInt(colors.size()));
			
			//Setting the color
			array[i] = color;
			
			//Removing the color from use
			colors.remove(color);
		}
		
		//Returning the color array
		return array;
	}
}