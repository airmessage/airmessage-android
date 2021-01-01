package me.tagavari.airmessage.util;

/**
 * Represents a conversation that can be sent to
 */
public abstract class ConversationTarget {
	public static class AppleLinked extends ConversationTarget {
		private final String guid;
		
		public AppleLinked(String guid) {
			this.guid = guid;
		}
		
		public String getGuid() {
			return guid;
		}
	}
	
	public static class AppleUnlinked extends ConversationTarget {
		private final String[] members;
		private final String service;
		
		public AppleUnlinked(String[] members, String service) {
			this.members = members;
			this.service = service;
		}
		
		public String[] getMembers() {
			return members;
		}
		
		public String getService() {
			return service;
		}
	}
	
	public static class SystemSMS extends ConversationTarget {
		private final long externalID;
		
		public SystemSMS(long externalID) {
			this.externalID = externalID;
		}
		
		public long getExternalID() {
			return externalID;
		}
	}
}