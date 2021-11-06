package me.tagavari.airmessage.activity

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import me.tagavari.airmessage.R
import me.tagavari.airmessage.util.ServerUpdateData

class ServerUpdate: AppCompatActivity() {
    //Data
    private lateinit var updateData: ServerUpdateData

    //Views
    private lateinit var labelVersion: TextView
    private lateinit var labelReleaseNotes: TextView
    private lateinit var labelNotice: TextView
    private lateinit var buttonInstall: Button
    private lateinit var viewGroupProgress: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Setting the content view
        setContentView(R.layout.activity_newmessage)

        //Getting the update data
        updateData = intent.getParcelableExtra(PARAM_DATA)!!

        //Getting the views
        labelVersion = findViewById(R.id.label_version)
        labelReleaseNotes = findViewById(R.id.label_releasenotes)
        labelNotice = findViewById(R.id.label_notice)
        buttonInstall = findViewById(R.id.button_install)
        viewGroupProgress = findViewById(R.id.group_progress)
    }

    companion object {
        const val PARAM_DATA = "data"
    }
}