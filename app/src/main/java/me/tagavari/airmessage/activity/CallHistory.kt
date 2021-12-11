package me.tagavari.airmessage.activity

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import me.tagavari.airmessage.R
import me.tagavari.airmessage.composite.AppCompatCompositeActivity
import me.tagavari.airmessage.compositeplugin.PluginConnectionService
import me.tagavari.airmessage.compositeplugin.PluginRXDisposable
import me.tagavari.airmessage.connection.exception.AMRequestException
import me.tagavari.airmessage.enums.FaceTimeLinkErrorCode

class CallHistory : AppCompatCompositeActivity() {
    //State
    private val viewModel: ActivityViewModel by viewModels()

    //Callbacks
    private val requestMessagingPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if(result.values.all { it }) {
                viewModel.isWelcome.value = false
            }
        }

    //Views
    private lateinit var viewGroupWelcome: ViewGroup
    private lateinit var viewGroupMain: ViewGroup
    private lateinit var viewCameraPreview: PreviewView

    private lateinit var buttonCreateLink: Button
    private lateinit var buttonCreateCall: Button

    //Plugins
    private val pluginCS: PluginConnectionService
    private val pluginRXCD: PluginRXDisposable

    init {
        addPlugin(PluginConnectionService().also { pluginCS = it })
        addPlugin(PluginRXDisposable().also { pluginRXCD = it })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Render edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        //Set the content view
        setContentView(R.layout.activity_callhistory)

        //Get the views
        viewGroupWelcome = findViewById(R.id.group_welcome)
        viewGroupMain = findViewById(R.id.group_main)
        viewCameraPreview = findViewById(R.id.camera_preview)
        buttonCreateLink = findViewById(R.id.button_link)
        buttonCreateCall = findViewById(R.id.button_facetime)

        //Configure the toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        //Keep the content within the window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.content)) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
                topMargin = insets.top
            }

            WindowInsetsCompat.CONSUMED
        }

        //Set the button listeners
        findViewById<Button>(R.id.button_continue).setOnClickListener(this::onClickContinue)
        buttonCreateLink.setOnClickListener(this::onClickCreateLink)
        buttonCreateCall.setOnClickListener(this::onClickNewFaceTime)

        //Sync the UI with the welcome state
        viewModel.isWelcome.observe(this, this::updateUIWelcomeState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return false
    }

    private fun updateUIWelcomeState(isWelcome: Boolean) {
        if(isWelcome) {
            //The title is part of the welcome UI
            supportActionBar!!.title = null
            viewGroupWelcome.visibility = View.VISIBLE
            viewGroupMain.visibility = View.GONE
        } else {
            supportActionBar!!.setTitle(R.string.title_facetime)
            viewGroupWelcome.visibility = View.GONE
            viewGroupMain.visibility = View.VISIBLE

            //Start the camera preview
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                //Used to bind the lifecycle of cameras to the lifecycle owner
                val cameraProvider = cameraProviderFuture.get()

                //Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewCameraPreview.surfaceProvider)
                    }

                try {
                    //Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    //Bind use cases to camera
                    cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview)
                } catch(exception: Exception) {
                    exception.printStackTrace()
                }

            }, ContextCompat.getMainExecutor(this))

        }
    }

    private fun onClickContinue(view: View? = null) {
        //Request permissions
        requestMessagingPermissionsLauncher.launch(requiredPermissions)
    }

    private fun onClickCreateLink(view: View? = null) {
        //Disable the button
        buttonCreateLink.isEnabled = false

        //Get a link
        pluginCS.connectionManager?.let { connectionManager ->
            pluginRXCD.activity().add(
                connectionManager.requestFaceTimeLink()
                    .doOnTerminate {
                        //Re-enable the button
                        buttonCreateLink.isEnabled = true
                    }
                    .subscribe(
                        { link ->
                            //Share the link
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, link)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            startActivity(shareIntent)
                        },
                        { error ->
                            //Show a dialog
                            MaterialAlertDialogBuilder(this).apply {
                                setTitle(R.string.message_facetimelink_error)

                                if(error is AMRequestException) {
                                    setMessage(when(error.errorCode) {
                                        FaceTimeLinkErrorCode.network -> R.string.error_noconnection
                                        FaceTimeLinkErrorCode.external -> R.string.error_external
                                        else -> R.string.error_unknown
                                    })
                                } else {
                                    setMessage(R.string.error_unknown)
                                }

                                setPositiveButton(R.string.action_dismiss) { dialog, _ -> dialog.dismiss() }
                            }.create().show()
                        }
                    )
            )
        }
    }

    private fun onClickNewFaceTime(view: View? = null) {
        startActivity(Intent(this, NewFaceTime::class.java))
    }

    class ActivityViewModel(application: Application) : AndroidViewModel(application) {
        val isWelcome = MutableLiveData(requiredPermissions.any {
            ContextCompat.checkSelfPermission(getApplication(), it) != PackageManager.PERMISSION_GRANTED
        })
    }

    companion object {
        private val requiredPermissions = arrayOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA
        )
    }
}