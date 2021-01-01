package me.tagavari.airmessage.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;

import me.tagavari.airmessage.enums.ProxyType;
import me.tagavari.airmessage.util.DirectConnectionParams;
import me.tagavari.airmessage.util.Triplet;

public class SharedPreferencesManager {
	private static final int sharedPreferencesSchemaVersion = 1;
	
	private static final String sharedPreferencesInstallationFile = "installation";
	private static final String sharedPreferencesDefaultKeySchemaVersion = "schema_version";
	private static final String sharedPreferencesDefaultKeyInstallationID = "installation_id";
	
	private static final String sharedPreferencesConnectivityFile = "connectivity";
	private static final String sharedPreferencesConnectivityKeyProxyType = "account_type"; //The proxy type to use (direct connection or AM Connect)
	private static final String sharedPreferencesConnectivityKeyConnectServerConfirmed = "connect_server_confirmed"; //TRUE if this account has confirmed its connection with the server
	private static final String sharedPreferencesConnectivityKeyLastSyncInstallationID = "last_sync_installation_id"; //The installation ID recorded when messages were last synced (or cleared), used for tracking when the user should be prompted to re-sync their messages
	private static final String sharedPreferencesConnectivityKeyLastConnectionTime = "last_connection_time"; //The last time this client established a connection with the server
	private static final String sharedPreferencesConnectivityKeyLastConnectionInstallationID = "last_connection_installation_id"; //The installation ID of the server from the last conversation, used for tracking server changes immediately when connecting
	private static final String sharedPreferencesConnectivityKeyTextMessageConversationsInstalled = "text_message_conversations_installed"; //Whether text message conversations are currently imported into the app's database
	
	private static final String sharedPreferencesSecureFile = "secure";
	private static final String sharedPreferencesSecureKeyAddress = "hostname";
	private static final String sharedPreferencesSecureKeyAddressFallback = "hostname_fallback";
	private static final String sharedPreferencesSecureKeyPassword = "password";
	
	/**
	 * Gets the shared preferences instance for installation-related information
	 */
	private static SharedPreferences getInstallationSharedPrefs(Context context) {
		return context.getSharedPreferences(sharedPreferencesInstallationFile, Context.MODE_PRIVATE);
	}
	
	/**
	 * Gets the shared preferences instance for connectivity-related information
	 */
	private static SharedPreferences getConnectivitySharedPrefs(Context context) {
		return context.getSharedPreferences(sharedPreferencesConnectivityFile, Context.MODE_PRIVATE);
	}
	
	/**
	 * Gets the shared preferences instance for sensitive information
	 */
	private static SharedPreferences getSecureSharedPrefs(Context context) throws GeneralSecurityException, IOException {
		return EncryptedSharedPreferences.create(
				sharedPreferencesSecureFile,
				MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
				context,
				EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
				EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
		);
	}
	
	/**
	 * Upgrade shared preferences to the latest version required by the app
	 *
	 * This function does nothing if shared preferences are already up-to-date
	 */
	@SuppressLint("ApplySharedPref")
	public static void upgradeSharedPreferences(Context context) {
		//Reading the current schema version
		SharedPreferences installationSP = getInstallationSharedPrefs(context);
		int schemaVer = installationSP.getInt(sharedPreferencesDefaultKeySchemaVersion, -1);
		
		//Ignoring if we're up-to-date
		if(schemaVer == sharedPreferencesSchemaVersion) return;
		
		if(schemaVer == -1) {
			try {
				upgradeSharedPreferencesLegacy(context);
			} catch(GeneralSecurityException | IOException exception) {
				//Just let the app crash; nothing we can do about this
				throw new RuntimeException(exception);
			}
		}
	}
	
	/**
	 * Upgrade shared preferences from AirMessage 0.X or 3.0.X to 3.1 schema 1
	 *
	 * This function does nothing if shared preferences are already up-to-date
	 */
	@SuppressLint("ApplySharedPref")
	public static void upgradeSharedPreferencesLegacy(Context context) throws GeneralSecurityException, IOException {
		//Editing the shared preferences
		SharedPreferences preferencesSP = PreferenceManager.getDefaultSharedPreferences(context); //This was previously used as the default shared preferences, now we use 'installation'
		SharedPreferences.Editor preferencesEditor = preferencesSP.edit();
		
		SharedPreferences installationSP = getInstallationSharedPrefs(context); //The new default shared preferences
		SharedPreferences.Editor installationEditor = installationSP.edit();
		
		SharedPreferences connectivitySP = getConnectivitySharedPrefs(context); //Shared preferences for connectivity details
		SharedPreferences.Editor connectivityEditor = connectivitySP.edit();
		
		SharedPreferences secureSP = getSecureSharedPrefs(context); //Shared preferences for connectivity details
		SharedPreferences.Editor secureEditor = secureSP.edit();
		
		//Checking if we're upgrading from 3.0.X (only used in 3.0.X schema)
		if(preferencesSP.contains("default_schemaversion")) {
			//Removing the last hostname from connectivity
			connectivityEditor.remove("last_connection_hostname");
			
			//Removing the schema version from preferences
			preferencesEditor.remove("default_schemaversion");
			
			//Migrating the installation ID from preferences to installation
			if(preferencesSP.contains("default_installationID")) {
				String installationID = preferencesSP.getString("default_installationID", null);
				preferencesEditor.remove("default_installationID");
				installationEditor.putString("installation_id", installationID);
			}
			
			//Migrating server details from connectivity to secure
			{
				String hostname = connectivitySP.getString("hostname", null);
				if(hostname != null) {
					connectivityEditor.remove("hostname");
					secureEditor.putString("hostname", hostname);
				}
				
				String hostnameFallback = connectivitySP.getString("hostname_fallback", null);
				if(hostnameFallback != null) {
					connectivityEditor.remove("hostname_fallback");
					secureEditor.putString("hostname_fallback", hostnameFallback);
				}
				
				String password = connectivitySP.getString("password", null);
				if(password != null) {
					connectivityEditor.remove("password");
					secureEditor.putString("password", password);
				}
			}
		} else { //We're upgrading from pre-3.0
			//Checking if we have anything to migrate (if the user has already set up a connection)
			if(connectivitySP.contains("hostname")) {
				//Migrating the fallback address from 'preferences' shared preferences to 'secure' shared preferences
				String fallbackKey = "pref_key_server_fallbackaddress";
				if(preferencesSP.contains(fallbackKey)) {
					String fallback = preferencesSP.getString(fallbackKey, null);
					preferencesEditor.remove(fallbackKey);
					secureEditor.putString("hostname_fallback", fallback);
				}
				
				//Migrating server hostname and password
				{
					String hostname = connectivitySP.getString("hostname", null);
					if(hostname != null) {
						connectivityEditor.remove("hostname");
						secureEditor.putString("hostname", hostname);
					}
					
					String password = connectivitySP.getString("password", null);
					if(password != null) {
						connectivityEditor.remove("password");
						secureEditor.putString("password", password);
					}
				}
				
				//Setting a notrigger installation ID to prevent the sync prompt from showing up after updating
				if(connectivitySP.contains("last_connection_hostname")) {
					connectivityEditor.remove("last_connection_hostname");
					connectivityEditor.putString("last_sync_installation_id", "notrigger");
				}
				
				//Updating the connection status
				connectivityEditor.putInt("account_type", 0); //Direct
				connectivityEditor.putBoolean("connect_server_confirmed", true);
			}
			
			//Updating the schema version
			preferencesEditor.remove("default_schemaversion");
		}
		
		//Updating the schema version
		installationEditor.putInt("schema_version", 1);
		
		//Saving changes
		preferencesEditor.commit();
		installationEditor.commit();
		connectivityEditor.commit();
		secureEditor.commit();
	}
	
	/**
	 * Fetches the {@link ProxyType} to use
	 */
	@ProxyType
	public static int getProxyType(Context context) {
		return getConnectivitySharedPrefs(context).getInt(sharedPreferencesConnectivityKeyProxyType, ProxyType.direct);
	}
	
	/**
	 * Sets {@link ProxyType} to use
	 */
	public static void setProxyType(Context context, @ProxyType int proxyType) {
		getConnectivitySharedPrefs(context).edit().putInt(sharedPreferencesConnectivityKeyProxyType, proxyType).apply();
	}
	
	/**
	 * Fetches the address used for direct connections
	 */
	public static String getDirectConnectionAddress(Context context) throws GeneralSecurityException, IOException {
		return getSecureSharedPrefs(context).getString(sharedPreferencesSecureKeyAddress, null);
	}
	
	/**
	 * Fetches the fallback address used for direct connections
	 */
	public static String getDirectConnectionFallbackAddress(Context context) throws GeneralSecurityException, IOException {
		return getSecureSharedPrefs(context).getString(sharedPreferencesSecureKeyAddressFallback, null);
	}
	
	/**
	 * Fetches the password used for direct connections
	 */
	public static String getDirectConnectionPassword(Context context) throws GeneralSecurityException, IOException {
		return getSecureSharedPrefs(context).getString(sharedPreferencesSecureKeyPassword, null);
	}
	
	/**
	 * Fetches the address, fallback address, and password used for direct connections
	 */
	public static DirectConnectionParams getDirectConnectionDetails(Context context) throws GeneralSecurityException, IOException {
		SharedPreferences prefs = getSecureSharedPrefs(context);
		return new DirectConnectionParams(
				prefs.getString(sharedPreferencesSecureKeyAddress, null),
				prefs.getString(sharedPreferencesSecureKeyAddressFallback, null),
				prefs.getString(sharedPreferencesSecureKeyPassword, null)
		);
	}
	
	/**
	 * Sets the address, fallback address, and password used for direct connections
	 */
	public static void setDirectConnectionDetails(Context context, DirectConnectionParams params) throws GeneralSecurityException, IOException {
		getSecureSharedPrefs(context).edit()
				.putString(sharedPreferencesSecureKeyAddress, params.getAddress())
				.putString(sharedPreferencesSecureKeyAddressFallback, params.getFallbackAddress())
				.putString(sharedPreferencesSecureKeyPassword, params.getPassword())
				.apply();
	}
	
	/**
	 * Fetches or generates an installation ID for this client
	 */
	@NonNull
	public static String getInstallationID(Context context) {
		SharedPreferences defaultSP = getInstallationSharedPrefs(context);
		
		//Reading and returning the UUID
		String installationID = defaultSP.getString(sharedPreferencesDefaultKeyInstallationID, null);
		if(installationID != null) return installationID;
		
		//Generating, saving and returning a new UUID if an existing one doesn't exist
		installationID = UUID.randomUUID().toString();
		defaultSP.edit()
				.putString(sharedPreferencesDefaultKeyInstallationID, installationID)
				.apply();
		return installationID;
	}
	
	/**
	 * Fetches the time this client last connected to the server, or -1 if unavailable
	 */
	public static long getLastConnectionTime(Context context) {
		return getConnectivitySharedPrefs(context).getLong(sharedPreferencesConnectivityKeyLastConnectionTime, -1);
	}
	
	/**
	 * Sets the time this client last connected to the server
	 */
	public static void setLastConnectionTime(Context context, long time) {
		getConnectivitySharedPrefs(context).edit().putLong(sharedPreferencesConnectivityKeyLastConnectionTime, time).apply();
	}
	
	/**
	 * Gets the server's installation ID from the last time this client connected, or NULL if unavailable
	 */
	@Nullable
	public static String getLastConnectionInstallationID(Context context) {
		return getConnectivitySharedPrefs(context).getString(sharedPreferencesConnectivityKeyLastConnectionInstallationID, null);
	}
	
	/**
	 * Sets the server's installation ID from the last time this client connected
	 */
	public static void setLastConnectionInstallationID(Context context, String installationID) {
		getConnectivitySharedPrefs(context).edit().putString(sharedPreferencesConnectivityKeyLastConnectionInstallationID, installationID).apply();
	}
	
	/**
	 * Gets the server's installation ID from the last time this client synced, or NULL if unavailable
	 */
	@Nullable
	public static String getLastSyncInstallationID(Context context) {
		return getConnectivitySharedPrefs(context).getString(sharedPreferencesConnectivityKeyLastSyncInstallationID, null);
	}
	
	/**
	 * Sets the server's installation ID from the last time this client synced
	 */
	public static void setLastSyncInstallationID(Context context, String installationID) {
		getConnectivitySharedPrefs(context).edit().putString(sharedPreferencesConnectivityKeyLastSyncInstallationID, installationID).apply();
	}
	
	/**
	 * Sets whether the connection to the server is configured and can be used to connect
	 */
	public static void setConnectionConfigured(Context context, boolean connectionConfirmed) {
		getConnectivitySharedPrefs(context).edit().putBoolean(sharedPreferencesConnectivityKeyConnectServerConfirmed, connectionConfirmed).apply();
	}
	
	/**
	 * Fetches whether the connection to the server is configured and can be used to connect
	 */
	public static boolean isConnectionConfigured(Context context) {
		return getConnectivitySharedPrefs(context).getBoolean(sharedPreferencesConnectivityKeyConnectServerConfirmed, false);
	}
	
	/**
	 * Fetches whether text message conversations are currently installed
	 */
	public static boolean getTextMessageConversationsInstalled(Context context) {
		return getConnectivitySharedPrefs(context).getBoolean(sharedPreferencesConnectivityKeyTextMessageConversationsInstalled, false);
	}
	
	/**
	 * Sets whether text message conversations are currently installed
	 */
	public static void setTextMessageConversationsInstalled(Context context, boolean value) {
		getConnectivitySharedPrefs(context).edit().putBoolean(sharedPreferencesConnectivityKeyTextMessageConversationsInstalled, value).apply();
	}
}