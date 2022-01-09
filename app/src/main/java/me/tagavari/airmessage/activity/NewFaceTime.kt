package me.tagavari.airmessage.activity

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import android.widget.TextView.OnEditorActionListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.PublishSubject
import me.tagavari.airmessage.MainApplication
import me.tagavari.airmessage.R
import me.tagavari.airmessage.component.ContactChip
import me.tagavari.airmessage.component.ContactListReactiveUpdate
import me.tagavari.airmessage.component.ContactsRecyclerAdapter
import me.tagavari.airmessage.composite.AppCompatCompositeActivity
import me.tagavari.airmessage.connection.ConnectionManager
import me.tagavari.airmessage.connection.exception.AMRemoteUpdateException
import me.tagavari.airmessage.connection.exception.AMRequestException
import me.tagavari.airmessage.constants.ColorConstants
import me.tagavari.airmessage.enums.FaceTimeInitiateCode
import me.tagavari.airmessage.helper.AddressHelper.normalizeAddress
import me.tagavari.airmessage.helper.AddressHelper.validateAddress
import me.tagavari.airmessage.helper.ConnectionServiceLink
import me.tagavari.airmessage.helper.PlatformHelper.updateChromeOSStatusBar
import me.tagavari.airmessage.helper.ThemeHelper
import me.tagavari.airmessage.helper.ThemeHelper.shouldUseAMOLED
import me.tagavari.airmessage.helper.WindowHelper.enforceContentWidthView
import me.tagavari.airmessage.helper.bindUntilDestroy
import me.tagavari.airmessage.task.ContactsTask
import me.tagavari.airmessage.task.ContactsTask.ContactAddressPart
import me.tagavari.airmessage.util.ContactInfo

class NewFaceTime : AppCompatCompositeActivity() {
    //State
    private val viewModel: ActivityViewModel by viewModels()
    private val csLink = ConnectionServiceLink(this)
    
    //Views
    private lateinit var recipientListGroup: ViewGroup
    private lateinit var recipientInput: EditText
    private lateinit var recipientInputToggle: ImageButton
    private lateinit var contactListView: RecyclerView
    private lateinit var contactListAdapter: ContactsRecyclerAdapter
    
    private lateinit var buttonConfirm: Button
    private lateinit var groupMessagePermission: ViewGroup
    private lateinit var groupMessageError: ViewGroup
    
    //Listeners
    private val recipientInputTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            //Filtering the list
            contactListAdapter.filterList(s.toString())
        }
        override fun afterTextChanged(s: Editable) = Unit
    }
    private val recipientInputOnKeyListener = View.OnKeyListener { _, keyCode, event ->
        //Return if the event is not a key down
        if(event.action != KeyEvent.ACTION_DOWN) return@OnKeyListener false
        
        //Check if the key is the delete key
        if(keyCode == KeyEvent.KEYCODE_DEL) {
            //Check if the cursor is at the start and there are chips
            if(recipientInput.selectionStart == 0 && recipientInput.selectionEnd == 0 && viewModel.userChips.isNotEmpty()) {
                //Remove the last chip
                removeChip(viewModel.userChips[viewModel.userChips.size - 1])
                
                //Returning true
                return@OnKeyListener true
            }
        }
    
        return@OnKeyListener false
    }
    private val recipientInputOnActionListener = OnEditorActionListener { _, actionId, _ ->
        //Check if the action is the "done" button
        if(actionId == EditorInfo.IME_ACTION_DONE) {
            //Get the string
            val cleanString = recipientInput.text.toString().trim()
            
            //Check if the string passes validation
            if(validateAddress(cleanString)) {
                //Add a chip
                addChip(ContactChip(
                    this@NewFaceTime,
                    cleanString,
                    normalizeAddress(cleanString),
                    { removeChip(it) }
                ))
                
                //Clearing the text input
                recipientInput.setText("")
            }
            
            return@OnEditorActionListener true
        }
        
        //Returning false
        return@OnEditorActionListener false
    }
    
    private val contactStateObserver = Observer { state: Int ->
        when(state) {
            NewMessage.ActivityViewModel.contactStateReady -> {
                contactListView.visibility = View.VISIBLE
                buttonConfirm.visibility = View.VISIBLE
                groupMessagePermission.visibility = View.GONE
                groupMessageError.visibility = View.GONE
            }
            NewMessage.ActivityViewModel.contactStateNoAccess -> {
                contactListView.visibility = View.GONE
                buttonConfirm.visibility = View.GONE
                groupMessagePermission.visibility = View.VISIBLE
                groupMessageError.visibility = View.GONE
            }
            NewMessage.ActivityViewModel.contactStateFailed -> {
                contactListView.visibility = View.GONE
                buttonConfirm.visibility = View.GONE
                groupMessagePermission.visibility = View.GONE
                groupMessageError.visibility = View.VISIBLE
            }
        }
    }
    
    //Callbacks
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if(isGranted) {
            //Load the contacts
            viewModel.loadContacts()
    
            //Start the update listener
            MainApplication.getInstance().registerContactsListener()
        } else {
            //Show a snackbar
            Snackbar.make(findViewById(android.R.id.content), R.string.message_permissionrejected, Snackbar.LENGTH_LONG)
                .setAction(R.string.screen_settings) {
                    //Open the application settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
                .show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Set the layout
        setContentView(R.layout.activity_newfacetime)

        //Configure the toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        //Get the views
        recipientListGroup = findViewById(R.id.group_recipientlist)
        recipientInput = findViewById(R.id.input_recipients)
        recipientInputToggle = findViewById(R.id.button_recipients)
        contactListView = findViewById(R.id.list_contacts)
    
        buttonConfirm = findViewById(R.id.fab_confirm)
        groupMessagePermission = findViewById(R.id.group_permission)
        groupMessageError = findViewById(R.id.group_error)

        //Enforce the maximum content width
        enforceContentWidthView(resources, contactListView)

        //Configure the AMOLED theme
        if(shouldUseAMOLED(this)) setDarkAMOLED()

        //Set the status bar color
        updateChromeOSStatusBar(this)
    
        //Add the input listeners
        recipientInput.addTextChangedListener(recipientInputTextWatcher)
        recipientInput.setOnKeyListener(recipientInputOnKeyListener)
        recipientInput.setOnEditorActionListener(recipientInputOnActionListener)
        
        //Focus the input field
        recipientInput.requestFocus()
        
        //Register the observers
        viewModel.contactState.observe(this, contactStateObserver)
        viewModel.loadingState.observe(this) { setActivityStateLoading(it, true) }
        viewModel.completionLaunchIntent.observe(this) { intent: Intent ->
            startActivity(intent)
            finish()
        }
        viewModel.errorDetails.observe(this) { error ->
            error?.let { (message, details) ->
                showErrorDialog(message, details)
            }
        }
        viewModel.contactListSubject.subscribe {
            it.updateAdapter(contactListAdapter)
        }.bindUntilDestroy(this)
        
        //Restore the input bar
        restoreInputBar()
        
        //Configure the list
        contactListAdapter = ContactsRecyclerAdapter(this, viewModel.contactList) { address: String ->
            //Add the chip
            addChip(ContactChip(this, address, normalizeAddress(address), this::removeChip))
        
            //Clear the text
            recipientInput.setText("")
        }
        contactListView.itemAnimator = null
        contactListView.adapter = contactListAdapter
        
        //Configure the confirm button
        buttonConfirm.isEnabled = viewModel.userChips.isNotEmpty() && !(viewModel.loadingState.value ?: false)
        
        //Request edge-to-edge rendering
        WindowCompat.setDecorFitsSystemWindows(window, false)
    
        var contactListViewBottomInset = 0
        
        //Update the list insets
        ViewCompat.setOnApplyWindowInsetsListener(contactListView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            contactListViewBottomInset = insets.bottom
            view.updatePadding(bottom = insets.bottom + buttonConfirm.paddingBottom + buttonConfirm.height)
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = -insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }
        buttonConfirm.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                buttonConfirm.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val paddingBottom = contactListViewBottomInset + buttonConfirm.marginBottom + buttonConfirm.height + buttonConfirm.marginTop
                contactListView.updatePadding(bottom = paddingBottom)
            }
        })
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) {
            finish()
            return true
        }
        
        return false
    }
    
    private fun restoreInputBar() {
        //Restoring the input type
        if(viewModel.recipientInputAlphabetical) {
            recipientInput.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            recipientInputToggle.setImageResource(R.drawable.dialpad)
        } else {
            recipientInput.inputType = InputType.TYPE_CLASS_PHONE
            recipientInputToggle.setImageResource(R.drawable.keyboard_outlined)
        }
        
        //Restoring the chips
        if(viewModel.userChips.isEmpty()) {
            //Setting the hint
            recipientInput.setHint(R.string.imperative_userinput)
        } else {
            //Removing the hint
            recipientInput.hint = ""
            
            //Adding the views
            for((chipIndex, chip) in viewModel.userChips.withIndex()) {
                (chip.view.parent as ViewGroup).removeView(chip.view)
                recipientListGroup.addView(chip.view, chipIndex)
            }
        }
    }
    
    /**
     * Adds a chip to the chip list
     */
    private fun addChip(chip: ContactChip) {
        //Make sure we aren't adding the same chip twice
        if(viewModel.userChips.any { it.address == chip.address }) return
        
        //remove the hint from the recipient input if this is the first chip
        if(viewModel.userChips.isEmpty()) {
            recipientInput.hint = ""
        }
        
        //Add the chip to the list
        viewModel.userChips.add(chip)
        
        //Add the view
        recipientListGroup.addView(chip.view, viewModel.userChips.size - 1)
        
        //Enable the confirm button
        buttonConfirm.isEnabled = true
    }
    
    /**
     * Removes a chip from the chip list
     */
    private fun removeChip(chip: ContactChip) {
        //Ignore if we're loading
        if(viewModel.loadingState.value == true) return
        
        //Remove the chip from the list
        viewModel.userChips.remove(chip)
        
        //Remove the view
        recipientListGroup.removeView(chip.view)
        
        //Check if there are no more chips
        if(viewModel.userChips.isEmpty()) {
            //Restore the input hint
            recipientInput.setHint(R.string.imperative_userinput)
            
            //Disable the confirm button
            buttonConfirm.isEnabled = false
        }
    }
    
    /**
     * Updates the view to reflect the activity's loading state
     * @param loading Whether the activity is loading
     * @param animate Whether the change should be animated
     */
    private fun setActivityStateLoading(loading: Boolean, animate: Boolean) {
        //Disable the confirm button
        buttonConfirm.isEnabled = !loading && viewModel.userChips.isNotEmpty()
        
        //Disable the inputs
        recipientInput.isEnabled = !loading
        recipientInputToggle.isEnabled = !loading
        
        //Disable the list
        contactListView.isEnabled = !loading
        val scrim = findViewById<View>(R.id.scrim_content)
        val progressBar = findViewById<LinearProgressIndicator>(R.id.progressbar_content)
        if(animate) {
            if(loading) {
                scrim.animate().apply {
                    alpha(1F)
                    withStartAction {
                        scrim.visibility = View.VISIBLE
                    }
                }.start()
                progressBar.animate().apply {
                    alpha(1F)
                    withStartAction {
                        progressBar.visibility = View.VISIBLE
                    }
                    setStartDelay(1500)
                }.start()
            } else {
                scrim.animate().apply {
                    alpha(0F)
                    withEndAction {
                        scrim.visibility = View.GONE
                    }
                }.start()
                progressBar.animate().apply {
                    alpha(0F)
                    withEndAction {
                        progressBar.visibility = View.GONE
                    }
                }.start()
            }
        } else {
            if(loading) {
                scrim.visibility = View.VISIBLE
                scrim.alpha = 1F
                progressBar.visibility = View.VISIBLE
            } else {
                scrim.visibility = View.GONE
                scrim.alpha = 0F
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun setDarkAMOLED() {
        ThemeHelper.setActivityAMOLEDBase(this)
        findViewById<View>(R.id.appbar).setBackgroundColor(ColorConstants.colorAMOLED)
    }
    
    private fun showErrorDialog(message: String, details: String? = null) {
        //Show a basic error dialog
        MaterialAlertDialogBuilder(this).apply {
            setTitle(R.string.message_facetime_error_create)
            setMessage(message)
        
            setPositiveButton(R.string.action_dismiss) { dialog, _ ->
                dialog.dismiss()
            }
        
            //If we have error details, add a button to let the user view them
            details?.let { errorDetails ->
                setNeutralButton(R.string.action_details) { dialog, _ ->
                    //Dismiss the dialog and show an error dialog
                    dialog.dismiss()
                
                    MaterialAlertDialogBuilder(this@NewFaceTime).apply {
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
        
            setOnDismissListener {
                //Clear the error details when we dismiss the dialog
                viewModel.errorDetails.value = null
            }
        }.create().show()
    }
    
    fun onClickRequestContacts(view: View? = null) {
        requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
    }
    
    fun onClickRetryLoad(view: View? = null) {
        if(viewModel.contactState.value == NewMessage.ActivityViewModel.contactStateFailed) viewModel.loadContacts()
    }
    
    fun onToggleInputType(view: View) {
        view as ImageButton
        
        //Save the selection
        val selectionStart = recipientInput.selectionStart
        val selectionEnd = recipientInput.selectionEnd
    
        //Check if the input is alphabetical
        if(viewModel.recipientInputAlphabetical) {
            //Set the input type
            recipientInput.inputType = InputType.TYPE_CLASS_PHONE
        
            //Set the toggle input type icon
            view.setImageResource(R.drawable.keyboard_outlined)
        
            //Set the alphabetical input variable
            viewModel.recipientInputAlphabetical = false
        } else {
            //Set the input type
            recipientInput.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        
            //Set the toggle input type icon
            view.setImageResource(R.drawable.dialpad)
        
            //Set the alphabetical input variable
            viewModel.recipientInputAlphabetical = true
        }
        
        //Restore the selection
        recipientInput.setSelection(selectionStart, selectionEnd)
    }
    
    fun onClickConfirm(view: View? = null) {
        csLink.connectionManager?.also { connectionManager ->
            //Create the call
            viewModel.confirmParticipants(connectionManager)
        } ?: run {
            //Let the user know an error occurred
            viewModel.errorDetails.value = Pair(resources.getString(R.string.error_noconnection), null)
        }
    }
    
    class ActivityViewModel(application: Application) : AndroidViewModel(application) {
        val userChips = mutableListOf<ContactChip>()
        val contactList = mutableListOf<ContactInfo>()

        val contactState = MutableLiveData<Int>() //The current state of the activity
        val loadingState = MutableLiveData<Boolean>() //Whether the activity is loading due to a participant confirmation
        val completionLaunchIntent = MutableLiveData<Intent>() //An intent to launch to complete this activity
        val errorDetails = MutableLiveData<Pair<String, String?>>() //An error message and error detail to show when a call couldn't be created
        val contactListSubject: PublishSubject<ContactListReactiveUpdate> = PublishSubject.create() //A subject to emit updates to the contacts list
        var recipientInputAlphabetical = true //Whether the recipient input field is in alphabetical or numeric mode

        private val compositeDisposable = CompositeDisposable()

        override fun onCleared() {
            super.onCleared()

            compositeDisposable.dispose()
        }

        init {
            //Loading contacts
            loadContacts()
        }

        fun loadContacts() {
            //Aborting if contacts cannot be used
            if(!MainApplication.canUseContacts(getApplication())) {
                contactState.value = NewMessage.ActivityViewModel.contactStateNoAccess
                return
            }

            //Updating the state
            contactState.value = NewMessage.ActivityViewModel.contactStateReady

            //Loading the contacts
            ContactsTask.loadContacts(getApplication())
                .map { contactPart: ContactAddressPart ->
                    //Trying to match a contact in the list
                    val matchingContactIndex = contactList.indexOfFirst { it.identifier == contactPart.id }
                    if(matchingContactIndex == -1) {
                        //Add a new contact
                        contactList.add(ContactInfo(contactPart.id, contactPart.name, mutableListOf(contactPart.address)))
                        return@map ContactListReactiveUpdate.Addition(contactList.size - 1)
                    } else {
                        //Updating the contact
                        contactList[matchingContactIndex].addAddress(contactPart.address)
                        return@map ContactListReactiveUpdate.Change(matchingContactIndex)
                    }
                }.subscribe(contactListSubject)
        }
        
        fun confirmParticipants(connectionManager: ConnectionManager) {
            //Disable the UI
            loadingState.value = true
            
            val addresses = userChips.map { it.address }
            compositeDisposable.add(
                connectionManager.initiateFaceTimeCall(addresses)
                    .subscribe(
                        {
                            //Start the call
                            completionLaunchIntent.value = Intent(getApplication(), FaceTimeCall::class.java).apply {
                                putExtra(FaceTimeCall.PARAM_TYPE, FaceTimeCall.Type.outgoing)
                                putStringArrayListExtra(FaceTimeCall.PARAM_PARTICIPANTS, ArrayList(addresses))
                            }
                        },
                        { error ->
                            //Re-enable the UI
                            loadingState.value = false
                    
                            //Set the error details
                            errorDetails.value = if(error is AMRequestException) {
                                val messageID = when(error.errorCode) {
                                    FaceTimeInitiateCode.network -> R.string.error_noconnection
                                    FaceTimeInitiateCode.badMembers -> R.string.error_badmembers
                                    FaceTimeInitiateCode.external -> R.string.error_external
                                    else -> R.string.error_external
                                }
                        
                                Pair(getApplication<Application>().getString(messageID), error.errorDetails)
                            } else {
                                Pair(getApplication<Application>().getString(R.string.error_internal), null)
                            }
                        }
                    )
            )
        }
    }
}