package me.tagavari.airmessage.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import me.tagavari.airmessage.enums.ProxyType
import me.tagavari.airmessage.helper.StringHelper
import me.tagavari.airmessage.util.ConnectionParams
import me.tagavari.airmessage.util.DirectConnectionDetails
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.*

object SharedPreferencesManager {
	private const val sharedPreferencesSchemaVersion = 1
	
	private const val sharedPreferencesInstallationFile = "installation"
	private const val sharedPreferencesDefaultKeySchemaVersion = "schema_version"
	private const val sharedPreferencesDefaultKeyInstallationID = "installation_id"

	private const val sharedPreferencesDefaultServerSupportsFaceTime = "server_supports_facetime" //A cached value of whether the server supports FaceTime
	
	private const val sharedPreferencesConnectivityFile = "connectivity"
	private const val sharedPreferencesConnectivityKeyProxyType = "account_type" //The proxy type to use (direct connection or AM Connect)
	private const val sharedPreferencesConnectivityKeyConnectServerConfirmed = "connect_server_confirmed" //TRUE if this account has confirmed its connection with the server
	
	private const val sharedPreferencesConnectivityKeyLastSyncInstallationID = "last_sync_installation_id" //The installation ID recorded when messages were last synced (or cleared), used for tracking when the user should be prompted to re-sync their messages
	private const val sharedPreferencesConnectivityKeyLastConnectionTime = "last_connection_time" //The last time this client established a connection with the server
	
	private const val sharedPreferencesConnectivityKeyLastServerMessageID = "last_server_message_id" //The last message ID received from the server
	private const val sharedPreferencesConnectivityKeyLastConnectionInstallationID = "last_connection_installation_id" //The installation ID of the server from the last conversation, used for tracking server changes immediately when connecting
	private const val sharedPreferencesConnectivityKeyTextMessageConversationsInstalled = "text_message_conversations_installed" //Whether text message conversations are currently imported into the app's database

	private const val sharedPreferencesSecureFile = "secure"
	private const val sharedPreferencesSecureKeyAddress = "hostname"
	private const val sharedPreferencesSecureKeyAddressFallback = "hostname_fallback"
	private const val sharedPreferencesSecureKeyPassword = "password"
	
	/**
	 * Gets the shared preferences instance for installation-related information
	 */
	private fun getInstallationSharedPrefs(context: Context): SharedPreferences {
		return context.getSharedPreferences(sharedPreferencesInstallationFile, Context.MODE_PRIVATE)
	}
	
	/**
	 * Gets the shared preferences instance for connectivity-related information
	 */
	private fun getConnectivitySharedPrefs(context: Context): SharedPreferences {
		return context.getSharedPreferences(sharedPreferencesConnectivityFile, Context.MODE_PRIVATE)
	}
	
	/**
	 * Gets the shared preferences instance for sensitive information
	 */
	@Throws(GeneralSecurityException::class, IOException::class)
	private fun getSecureSharedPrefs(context: Context): SharedPreferences {
		return EncryptedSharedPreferences.create(
			sharedPreferencesSecureFile,
			MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
			context,
			EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
			EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
		)
	}
	
	/**
	 * Upgrade shared preferences to the latest version required by the app
	 *
	 * This function does nothing if shared preferences are already up-to-date
	 */
	@JvmStatic
	@SuppressLint("ApplySharedPref")
	fun upgradeSharedPreferences(context: Context) {
		//Reading the current schema version
		val installationSP = getInstallationSharedPrefs(context)
		val schemaVer = installationSP.getInt(sharedPreferencesDefaultKeySchemaVersion, -1)
		
		//Ignoring if we're up-to-date
		if(schemaVer == sharedPreferencesSchemaVersion) return
		
		if(schemaVer == -1) {
			upgradeSharedPreferencesLegacy(context)
		}
	}
	
	/**
	 * Upgrade shared preferences from AirMessage 0.X or 3.0.X to 3.1 schema 1
	 *
	 * This function does nothing if shared preferences are already up-to-date
	 */
	@SuppressLint("ApplySharedPref")
	@Throws(GeneralSecurityException::class, IOException::class)
	fun upgradeSharedPreferencesLegacy(context: Context) {
		//Editing the shared preferences
		val preferencesSP = PreferenceManager.getDefaultSharedPreferences(context) //This was previously used as the default shared preferences, now we use 'installation'
		val preferencesEditor = preferencesSP.edit()
		
		val installationSP = getInstallationSharedPrefs(context) //The new default shared preferences
		val installationEditor = installationSP.edit()
		
		val connectivitySP = getConnectivitySharedPrefs(context) //Shared preferences for connectivity details
		val connectivityEditor = connectivitySP.edit()
		
		val secureSP = getSecureSharedPrefs(context) //Shared preferences for connectivity details
		val secureEditor = secureSP.edit()
		
		//Checking if we're upgrading from 3.0.X (only used in 3.0.X schema)
		if(preferencesSP.contains("default_schemaversion")) {
			//Removing the last hostname from connectivity
			connectivityEditor.remove("last_connection_hostname")
			
			//Removing the schema version from preferences
			preferencesEditor.remove("default_schemaversion")
			
			//Migrating the installation ID from preferences to installation
			if(preferencesSP.contains("default_installationID")) {
				val installationID = preferencesSP.getString("default_installationID", null)
				preferencesEditor.remove("default_installationID")
				installationEditor.putString("installation_id", installationID)
			}
			
			//Migrating server details from connectivity to secure
			connectivitySP.getString("hostname", null)?.let {
				connectivityEditor.remove("hostname")
				secureEditor.putString("hostname", it)
			}
			
			connectivitySP.getString("hostname_fallback", null)?.let {
				connectivityEditor.remove("hostname_fallback")
				secureEditor.putString("hostname_fallback", it)
			}
			
			connectivitySP.getString("password", null)?.let {
				connectivityEditor.remove("password")
				secureEditor.putString("password", it)
			}
		} else { //We're upgrading from pre-3.0
			//Checking if we have anything to migrate (if the user has already set up a connection)
			if(connectivitySP.contains("hostname")) {
				//Migrating the fallback address from 'preferences' shared preferences to 'secure' shared preferences
				val fallbackKey = "pref_key_server_fallbackaddress"
				if(preferencesSP.contains(fallbackKey)) {
					val fallback = preferencesSP.getString(fallbackKey, null)
					preferencesEditor.remove(fallbackKey)
					secureEditor.putString("hostname_fallback", fallback)
				}
				
				//Migrating server hostname and password
				connectivitySP.getString("hostname", null)?.let {
					connectivityEditor.remove("hostname")
					secureEditor.putString("hostname", it)
				}
				
				connectivitySP.getString("password", null)?.let {
					connectivityEditor.remove("password")
					secureEditor.putString("password", it)
				}
				
				//Setting a notrigger installation ID to prevent the sync prompt from showing up after updating
				if(connectivitySP.contains("last_connection_hostname")) {
					connectivityEditor.remove("last_connection_hostname")
					connectivityEditor.putString("last_sync_installation_id", "notrigger")
				}
				
				//Updating the connection status
				connectivityEditor.putInt("account_type", 0) //Direct
				connectivityEditor.putBoolean("connect_server_confirmed", true)
			}
			
			//Updating the schema version
			preferencesEditor.remove("default_schemaversion")
		}
		
		//Updating the schema version
		installationEditor.putInt("schema_version", 1)
		
		//Saving changes
		preferencesEditor.commit()
		installationEditor.commit()
		connectivityEditor.commit()
		secureEditor.commit()
	}
	
	/**
	 * Fetches the [ProxyType] to use
	 */
	@JvmStatic
	@ProxyType
	fun getProxyType(context: Context): Int {
		return getConnectivitySharedPrefs(context).getInt(sharedPreferencesConnectivityKeyProxyType, ProxyType.direct)
	}
	
	/**
	 * Sets the [ProxyType] to use
	 */
	@JvmStatic
	fun setProxyType(context: Context, @ProxyType proxyType: Int) {
		getConnectivitySharedPrefs(context).edit().putInt(sharedPreferencesConnectivityKeyProxyType, proxyType).apply()
	}
	
	/**
	 * Fetches the address used for direct connections
	 */
	@JvmStatic
	@Throws(GeneralSecurityException::class, IOException::class)
	fun getDirectConnectionAddress(context: Context): String? {
		return getSecureSharedPrefs(context).getString(sharedPreferencesSecureKeyAddress, null)
	}
	
	/**
	 * Fetches the fallback address used for direct connections
	 */
	@Throws(GeneralSecurityException::class, IOException::class)
	fun getDirectConnectionFallbackAddress(context: Context): String? {
		return getSecureSharedPrefs(context).getString(sharedPreferencesSecureKeyAddressFallback, null)
	}
	
	/**
	 * Fetches the password used for direct connections
	 */
	@JvmStatic
	@Throws(GeneralSecurityException::class, IOException::class)
	fun getDirectConnectionPassword(context: Context): String? {
		return getSecureSharedPrefs(context).getString(sharedPreferencesSecureKeyPassword, null)
	}
	
	/**
	 * Sets the password used for direct connections
	 */
	@JvmStatic
	@Throws(GeneralSecurityException::class, IOException::class)
	fun setDirectConnectionPassword(context: Context, password: String?) {
		getSecureSharedPrefs(context).edit().putString(sharedPreferencesSecureKeyPassword, password).apply()
	}
	
	/**
	 * Fetches the address, fallback address, and password used for direct connections
	 */
	@JvmStatic
	@Throws(GeneralSecurityException::class, IOException::class)
	fun getDirectConnectionDetails(context: Context): DirectConnectionDetails {
		val prefs = getSecureSharedPrefs(context)
		return DirectConnectionDetails(
			StringHelper.nullifyEmptyString(prefs.getString(sharedPreferencesSecureKeyAddress, null)),
			StringHelper.nullifyEmptyString(prefs.getString(sharedPreferencesSecureKeyAddressFallback, null)),
			StringHelper.nullifyEmptyString(prefs.getString(sharedPreferencesSecureKeyPassword, null))
		)
	}
	
	/**
	 * Sets the address, fallback address, and password used for direct connections
	 */
	@JvmStatic
	@Throws(GeneralSecurityException::class, IOException::class)
	fun setDirectConnectionDetails(context: Context, params: DirectConnectionDetails) {
		getSecureSharedPrefs(context).edit()
			.putString(sharedPreferencesSecureKeyAddress, params.address)
			.putString(sharedPreferencesSecureKeyAddressFallback, params.fallbackAddress)
			.putString(sharedPreferencesSecureKeyPassword, params.password)
			.apply()
	}
	
	/**
	 * Sets the address, fallback address, and password used for direct connections
	 */
	@JvmStatic
	@Throws(GeneralSecurityException::class, IOException::class)
	fun setDirectConnectionDetails(context: Context, params: ConnectionParams.Direct) {
		getSecureSharedPrefs(context).edit()
			.putString(sharedPreferencesSecureKeyAddress, params.address)
			.putString(sharedPreferencesSecureKeyAddressFallback, params.fallbackAddress)
			.putString(sharedPreferencesSecureKeyPassword, params.password)
			.apply()
	}
	
	/**
	 * Fetches or generates an installation ID for this client
	 */
	@JvmStatic
	fun getInstallationID(context: Context): String {
		val defaultSP = getInstallationSharedPrefs(context)
		
		//Reading and returning the UUID
		var installationID = defaultSP.getString(sharedPreferencesDefaultKeyInstallationID, null)
		if(installationID != null) return installationID
		
		//Generating, saving, and returning a new UUID if an existing one doesn't exist
		installationID = UUID.randomUUID().toString()
		defaultSP.edit()
			.putString(sharedPreferencesDefaultKeyInstallationID, installationID)
			.apply()
		return installationID
	}

	@JvmStatic
	fun getServerSupportsFaceTime(context: Context): Boolean {
		return getInstallationSharedPrefs(context).getBoolean(sharedPreferencesDefaultServerSupportsFaceTime, false)
	}

	@JvmStatic
	fun setServerSupportsFaceTime(context: Context, supportsFaceTime: Boolean) {
		getInstallationSharedPrefs(context).edit().putBoolean(sharedPreferencesDefaultServerSupportsFaceTime, supportsFaceTime).apply()
	}
	
	/**
	 * Fetches the time this client last connected to the server, or -1 if unavailable
	 */
	@JvmStatic
	fun getLastConnectionTime(context: Context): Long {
		return getConnectivitySharedPrefs(context).getLong(sharedPreferencesConnectivityKeyLastConnectionTime, -1)
	}
	
	/**
	 * Sets the time this client last connected to the server
	 */
	@JvmStatic
	fun setLastConnectionTime(context: Context, time: Long) {
		getConnectivitySharedPrefs(context).edit().putLong(sharedPreferencesConnectivityKeyLastConnectionTime, time).apply()
	}
	
	/**
	 * Fetches the last server message ID, or -1 if unavailable
	 */
	@JvmStatic
	fun getLastServerMessageID(context: Context): Long {
		return getConnectivitySharedPrefs(context).getLong(sharedPreferencesConnectivityKeyLastServerMessageID, -1)
	}
	
	/**
	 * Sets the last server message ID
	 */
	@JvmStatic
	fun setLastServerMessageID(context: Context, lastMessageID: Long) {
		getConnectivitySharedPrefs(context).edit().putLong(sharedPreferencesConnectivityKeyLastServerMessageID, lastMessageID).apply()
	}
	
	/**
	 * Removes the last server message ID
	 */
	@JvmStatic
	fun removeLastServerMessageID(context: Context) {
		getConnectivitySharedPrefs(context).edit().remove(sharedPreferencesConnectivityKeyLastServerMessageID).apply()
	}
	
	/**
	 * Gets the server's installation ID from the last time this client connected, or NULL if unavailable
	 */
	@JvmStatic
	fun getLastConnectionInstallationID(context: Context): String? {
		return getConnectivitySharedPrefs(context).getString(sharedPreferencesConnectivityKeyLastConnectionInstallationID, null)
	}
	
	/**
	 * Sets the server's installation ID from the last time this client connected
	 */
	@JvmStatic
	fun setLastConnectionInstallationID(context: Context, installationID: String?) {
		getConnectivitySharedPrefs(context).edit().putString(sharedPreferencesConnectivityKeyLastConnectionInstallationID, installationID).apply()
	}
	
	/**
	 * Gets the server's installation ID from the last time this client synced, or NULL if unavailable
	 */
	@JvmStatic
	fun getLastSyncInstallationID(context: Context): String? {
		return getConnectivitySharedPrefs(context).getString(sharedPreferencesConnectivityKeyLastSyncInstallationID, null)
	}
	
	/**
	 * Sets the server's installation ID from the last time this client synced
	 */
	@JvmStatic
	fun setLastSyncInstallationID(context: Context, installationID: String?) {
		getConnectivitySharedPrefs(context).edit().putString(sharedPreferencesConnectivityKeyLastSyncInstallationID, installationID).apply()
	}
	
	/**
	 * Sets whether the connection to the server is configured and can be used to connect
	 */
	@JvmStatic
	fun setConnectionConfigured(context: Context, connectionConfirmed: Boolean) {
		getConnectivitySharedPrefs(context).edit().putBoolean(sharedPreferencesConnectivityKeyConnectServerConfirmed, connectionConfirmed).apply()
	}
	
	/**
	 * Fetches whether the connection to the server is configured and can be used to connect
	 */
	@JvmStatic
	fun isConnectionConfigured(context: Context): Boolean {
		return getConnectivitySharedPrefs(context).getBoolean(sharedPreferencesConnectivityKeyConnectServerConfirmed, false)
	}
	
	/**
	 * Fetches whether text message conversations are currently installed
	 */
	@JvmStatic
	fun getTextMessageConversationsInstalled(context: Context): Boolean {
		return getConnectivitySharedPrefs(context).getBoolean(sharedPreferencesConnectivityKeyTextMessageConversationsInstalled, false)
	}
	
	/**
	 * Sets whether text message conversations are currently installed
	 */
	@JvmStatic
	fun setTextMessageConversationsInstalled(context: Context, value: Boolean) {
		getConnectivitySharedPrefs(context).edit().putBoolean(sharedPreferencesConnectivityKeyTextMessageConversationsInstalled, value).apply()
	}
}