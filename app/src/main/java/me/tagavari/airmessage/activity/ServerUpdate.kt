package me.tagavari.airmessage.activity

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.rxjava3.disposables.Disposable
import me.tagavari.airmessage.R
import me.tagavari.airmessage.composite.AppCompatCompositeActivity
import me.tagavari.airmessage.compositeplugin.PluginConnectionService
import me.tagavari.airmessage.compositeplugin.PluginRXDisposable
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEmitterNetwork.connectionStateSubject
import me.tagavari.airmessage.redux.ReduxEventConnection
import me.tagavari.airmessage.util.ServerUpdateData
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

class ServerUpdate: AppCompatCompositeActivity() {
    //Data
    private lateinit var updateData: ServerUpdateData
    private lateinit var serverVersion: String
    private lateinit var serverName: String

    //Views
    private lateinit var labelVersion: TextView
    private lateinit var labelReleaseNotes: TextView
    private lateinit var labelNotice: TextView
    private lateinit var buttonInstall: Button
    private lateinit var viewGroupProgress: ViewGroup

    private lateinit var subscriptionDisposable: Disposable

    //Plugins
    private val pluginCS: PluginConnectionService
    private val pluginRXCD: PluginRXDisposable

    init {
        addPlugin(PluginConnectionService().also { pluginCS = it })
        addPlugin(PluginRXDisposable().also { pluginRXCD = it })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Setting the content view
        setContentView(R.layout.activity_serverupdate)

        //Getting the parameter data
        updateData = intent.getParcelableExtra(PARAM_UPDATE)!!
        serverVersion = intent.getStringExtra(PARAM_SERVERVERSION) ?: resources.getString(R.string.part_unknown)
        serverName = intent.getStringExtra(PARAM_SERVERNAME) ?: resources.getString(R.string.part_unknown)

        //Getting the views
        labelVersion = findViewById(R.id.label_version)
        labelReleaseNotes = findViewById(R.id.label_releasenotes)
        labelNotice = findViewById(R.id.label_notice)
        buttonInstall = findViewById(R.id.button_install)
        viewGroupProgress = findViewById(R.id.group_progress)

        //Configuring the toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        //Applying the update data to the UI
        applyUpdateData()

        //Default to not loading
        updateUILoading(false)

        //Set the install button click listener
        buttonInstall.setOnClickListener {
            //Update the UI
            updateUILoading(true)

            //Install the update
            pluginCS.connectionManager?.connect()
        }

        //Subscribe to connection updates
        pluginRXCD.activity().add(
            connectionStateSubject.subscribe { event: ReduxEventConnection ->
                //If we disconnected, finish the activity
                if(event is ReduxEventConnection.Disconnected) {
                    finish()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        subscriptionDisposable.dispose()
    }

    private fun applyUpdateData() {
        //Set the update notice string
        labelVersion.text = resources.getString(R.string.message_serverupdate_version, updateData.version, serverVersion)

        //Parse and set the update notes
        val parser = Parser.builder().build()
        val renderer = HtmlRenderer.builder().build()
        val updateNotesHTML = renderer.render(parser.parse(updateData.notes))
        labelReleaseNotes.text = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(updateNotesHTML, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(updateNotesHTML)
        }
        labelReleaseNotes.movementMethod = LinkMovementMethod.getInstance()

        //Set the notice
        labelNotice.text = resources.getString(R.string.message_serverupdate_remotenotice, serverName)
    }

    private fun updateUILoading(isLoading: Boolean) {
        buttonInstall.visibility = if(isLoading) View.GONE else View.VISIBLE
        viewGroupProgress.visibility = if(isLoading) View.VISIBLE else View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return false
    }

    companion object {
        const val PARAM_UPDATE = "update"
        const val PARAM_SERVERVERSION = "server_version"
        const val PARAM_SERVERNAME = "server_name"
    }
}