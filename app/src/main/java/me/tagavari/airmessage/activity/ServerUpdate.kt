package me.tagavari.airmessage.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.text.method.LinkMovementMethod
import android.transition.TransitionManager
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.R
import me.tagavari.airmessage.composite.AppCompatCompositeActivity
import me.tagavari.airmessage.compositeplugin.PluginConnectionService
import me.tagavari.airmessage.compositeplugin.PluginRXDisposable
import me.tagavari.airmessage.connection.exception.AMRemoteUpdateException
import me.tagavari.airmessage.redux.ReduxEmitterNetwork
import me.tagavari.airmessage.redux.ReduxEventConnection
import me.tagavari.airmessage.redux.ReduxEventRemoteUpdate
import me.tagavari.airmessage.util.ServerUpdateData
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

class ServerUpdate : AppCompatCompositeActivity() {
    //Parameters
    private lateinit var updateData: ServerUpdateData
    private lateinit var serverVersion: String
    private lateinit var serverName: String

    //State
    private val viewModel: ActivityViewModel by viewModels()

    //Views
    private lateinit var viewGroupLayout: ViewGroup
    private lateinit var labelVersion: TextView
    private lateinit var labelReleaseNotes: TextView
    private lateinit var labelNotice: TextView
    private lateinit var buttonInstall: Button
    private lateinit var viewGroupProgress: ViewGroup

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
        viewGroupLayout = findViewById(R.id.layout)
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

        //Sync the UI with loading updates
        viewModel.isLoading.observe(this, this::updateUILoading)

        //Set the install button click listener
        buttonInstall.setOnClickListener(this::installUpdate)

        //Subscribe to connection updates
        pluginRXCD.activity().addAll(
            ReduxEmitterNetwork.connectionStateSubject.subscribe(this::updateStateConnection),
            ReduxEmitterNetwork.remoteUpdateProgressSubject.subscribe(this::updateStateUpdateProgress)
        )

        //Subscribe to timeout updates
        viewModel.updateRequestTimeoutCallback = this::handleUpdateRequestTimeout
    }

    override fun onDestroy() {
        super.onDestroy()

        //Unsubscribe from timeout updates
        viewModel.updateRequestTimeoutCallback = null
    }

    private fun updateStateConnection(event: ReduxEventConnection) {
        //If we're no longer connected, finish the activity
        if(event !is ReduxEventConnection.Connected) {
            finish()
        }
    }

    private fun updateStateUpdateProgress(event: ReduxEventRemoteUpdate) {
        //Ignore if we're not waiting for an update
        if(!viewModel.isLoading.value!!) return

        if(event is ReduxEventRemoteUpdate.Initiate) {
            //Cancel the timeout
            viewModel.cancelTimeout()
        } else if(event is ReduxEventRemoteUpdate.Error) {
            //Cancel the timeout
            viewModel.cancelTimeout()

            //Set the state to not loading
            viewModel.isLoading.value = false

            //Notify the user with a snackbar
            Snackbar.make(findViewById(R.id.root), R.string.message_serverupdate_failed, Snackbar.LENGTH_INDEFINITE)
                .apply {
                    setAction(R.string.action_details) {
                        //Show a basic error dialog
                        MaterialAlertDialogBuilder(this@ServerUpdate).apply {
                            setTitle(R.string.message_serverupdate_failed)
                            setMessage(
                                when(event.exception.errorCode) {
                                    AMRemoteUpdateException.errorCodeMismatch -> R.string.error_updatemismatch
                                    AMRemoteUpdateException.errorCodeDownload -> R.string.error_updatedownload
                                    AMRemoteUpdateException.errorCodeBadPackage -> R.string.error_updatebadpackage
                                    AMRemoteUpdateException.errorCodeInternal -> R.string.error_internal
                                    AMRemoteUpdateException.errorCodeUnknown -> R.string.error_unknown
                                    else -> R.string.error_unknown
                                }
                            )

                            setPositiveButton(R.string.action_dismiss) { dialog, _ ->
                                dialog.dismiss()
                            }

                            //If we have error details, add a button to let the user view them
                            event.exception.errorDetails?.let { errorDetails ->
                                setNeutralButton(R.string.action_details) { dialog, _ ->
                                    //Dismiss the dialog and show an error dialog
                                    dialog.dismiss()

                                    MaterialAlertDialogBuilder(this@ServerUpdate).apply {
                                        setTitle(R.string.message_messageerror_details_title)
                                        //Use a custom view with monospace font
                                        setView(
                                            layoutInflater.inflate(R.layout.dialog_simplescroll, null).apply {
                                                findViewById<TextView>(R.id.text).apply {
                                                    typeface = Typeface.MONOSPACE
                                                    text = errorDetails
                                                }
                                            }
                                        )

                                        //Copy to clipboard
                                        setNeutralButton(R.string.action_copy) { dialog, _ ->
                                            val clipboard = MainApplication.getInstance().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Error details", errorDetails))

                                            Toast.makeText(MainApplication.getInstance(), R.string.message_textcopied, Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                        }
                                        setPositiveButton(R.string.action_dismiss) { dialog, _ ->
                                            dialog.dismiss()
                                        }
                                    }.create().show()
                                }
                            }
                        }.create().show()
                    }
                }
                .show()
        }
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

    private fun installUpdate(view: View? = null) {
        //Install the update
        pluginCS.connectionManager?.installSoftwareUpdate(updateData.id) ?: run {
            //Notify the user with a snackbar
            Snackbar.make(findViewById(R.id.root), R.string.error_noconnection, Snackbar.LENGTH_INDEFINITE).show()
            return
        }

        //Schedule a timeout
        viewModel.startTimeout()

        //Set the state to loading
        viewModel.isLoading.value = true
    }

    private fun handleUpdateRequestTimeout() {
        //Notify the user with a snackbar
        Snackbar.make(findViewById(R.id.root), R.string.error_timedout, Snackbar.LENGTH_INDEFINITE).show()
    }

    private fun updateUILoading(isLoading: Boolean) {
        //Show or hide the views
        TransitionManager.beginDelayedTransition(viewGroupLayout)
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

    class ActivityViewModel : ViewModel() {
        private val timeoutHandler = Handler(Looper.getMainLooper())
        private val handleUpdateRequestTimeout = Runnable {
            //Set the state to not loading
            isLoading.value = false

            //Notify the activity
            updateRequestTimeoutCallback?.invoke()
        }

        val isLoading = MutableLiveData(false)
        var updateRequestTimeoutCallback: (() -> Unit)? = null

        fun startTimeout() {
            timeoutHandler.postDelayed(handleUpdateRequestTimeout, requestTimeout)
        }

        fun cancelTimeout() {
            timeoutHandler.removeCallbacks(handleUpdateRequestTimeout)
        }
    }

    companion object {
        //Parameters
        const val PARAM_UPDATE = "update"
        const val PARAM_SERVERVERSION = "server_version"
        const val PARAM_SERVERNAME = "server_name"

        private const val requestTimeout = 10 * 1000L
    }
}