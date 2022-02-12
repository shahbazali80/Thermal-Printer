package com.example.thermalprinter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import com.example.thermalprinter.models.FaceModel
import com.example.thermalprinter.models.NoteModel
import com.example.thermalprinter.viewmodel.NoteViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_new_note.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


class NewNoteActivity : AppCompatActivity() {

    var sizeDialog: BottomSheetDialog? = null
    var formatDialog: BottomSheetDialog? = null
    var fontFamilyDialog: BottomSheetDialog? = null
    var seekbarValue = 0
    lateinit var mBluetoothAdapter: BluetoothAdapter
    lateinit var mmSocket: BluetoothSocket
    lateinit var mmDevice: BluetoothDevice
    var mmOutputStream: OutputStream? = null
    var mmInputStream: InputStream? = null
    var workerThread: Thread? = null
    lateinit var readBuffer: ByteArray
    var readBufferPosition = 0

    @Volatile
    var stopWorker = false
    lateinit var BTDeviceList: MutableList<String>
    var list: MutableList<FaceModel>? = null
    var isBold = false
    var isItalic = false
    var isUnderLine = false
    var isFindPrinter = false

    var noteType : String ? = null
    var noteTitle : String ? = null
    var noteDescription : String ? = null
    var noteDate : String ? = null
    var toolbarTitle : String ? = null
    var currentDateAndTime: String ?  = null

    lateinit var viewModal: NoteViewModel
    var noteID = -1;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_note)

        setSupportActionBar(newNoteToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        fontFamilyDialog = BottomSheetDialog(this)
        sizeDialog = BottomSheetDialog(this)
        formatDialog = BottomSheetDialog(this)
        openFontDialog()
        openFontSizeDialog()
        openFontFormatDialog()
        list = ArrayList()
        BTDeviceList = ArrayList()

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        var sdf = SimpleDateFormat("dd/MMM/yyyy")
        currentDateAndTime = sdf.format(Date())

        noteType = intent.getStringExtra("noteType")
        if (noteType.equals("Edit")) {
            noteTitle = intent.getStringExtra("noteTitle")
            noteDescription = intent.getStringExtra("noteDescription")
            noteDate = intent.getStringExtra("noteDate")
            noteID = intent.getIntExtra("noteId", -1)
            supportActionBar!!.setTitle(noteTitle)
            et_note.setText(noteDescription)
            toolbarTitle=noteTitle
        } else
            toolbarTitle="New Note 001"

        viewModal = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(NoteViewModel::class.java)

        newNoteToolbar.setOnClickListener(View.OnClickListener { openEditToolbarTile() })

        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        fontFamilyDialog!!.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        sizeDialog!!.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        formatDialog!!.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }

    private fun openEditToolbarTile() {
        val dialogBuilder = android.app.AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.edit_toolbar_dialog, null)
        dialogBuilder.setView(dialogView)

        val et_toolbar = dialogView.findViewById(R.id.et_toolbar_edit) as TextView

        dialogBuilder.setTitle("Update Title")
        //dialogBuilder.setMessage("Enter data below")
        dialogBuilder.setPositiveButton("Update", DialogInterface.OnClickListener { _, _ ->
            toolbarTitle = et_toolbar.text.toString()
            supportActionBar!!.setTitle(toolbarTitle)
        })
        dialogBuilder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
            //pass
        })
        val b = dialogBuilder.create()
        b.show()
    }

    private val mOnNavigationItemSelectedListener: BottomNavigationView.OnNavigationItemSelectedListener =
        object : BottomNavigationView.OnNavigationItemSelectedListener {
            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                run {
                    when (item.itemId) {
                        R.id.font -> fontFamilyDialog!!.show()
                        R.id.size -> sizeDialog!!.show()
                        R.id.format -> formatDialog!!.show()
                        R.id.doneNote -> {
                            addUpdateNote()
                        }
                    }
                    return false
                }
            }
        }

    private fun addUpdateNote() {
        if(et_note.text.toString().isEmpty() ||  et_note.text.toString().trim().isEmpty()){
            et_note.setError("Write Something here")
            et_note.requestFocus()
            et_note.performClick()
        }else {
            val noteDescription = et_note.text.toString()
            if (noteType.equals("Edit")) {
                if (noteDescription.isNotEmpty()) {
                    val updatedNote = NoteModel(
                        toolbarTitle.toString(),
                        noteDescription,
                        currentDateAndTime.toString()
                    )
                    updatedNote.id = noteID
                    viewModal.updateNote(updatedNote)
                    Toast.makeText(
                        this@NewNoteActivity,
                        "Successfully Updated Note",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(this@NewNoteActivity, MainActivity::class.java))
                    finish()
                }
            } else {
                if (noteDescription.isNotEmpty()) {
                    viewModal.addNote(
                        NoteModel(
                            toolbarTitle.toString(),
                            noteDescription,
                            currentDateAndTime.toString()
                        )
                    )
                    Toast.makeText(
                        this@NewNoteActivity,
                        "Successfully Inserted Note",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(this@NewNoteActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        //return super.onCreateOptionsMenu(menu);
        menuInflater.inflate(R.menu.new_note_toolbar_menu, menu)
        if(noteType.equals("New")) {
            menu.findItem(R.id.menu_delete).isVisible=false
        }

        return true
    }

    @SuppressLint("MissingPermission")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_printerOut -> {
                try {
                    sendData()
                    //boldText();
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(applicationContext, "Error Occur During Sending Data\n$e", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.menu_findPrinter -> {
                findBT()
                true
            }
            R.id.menu_delete -> {
               deleteNote()
                true
            }
            R.id.menu_setting -> {
                boldText()
                Toast.makeText(applicationContext, "Setting", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_about -> {

                true
            }
            else -> //boldText();
            {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun deleteNote() {
        val dialogBuilder = android.app.AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.delete_dialog, null)
        dialogBuilder.setView(dialogView)

        val del_note = dialogView.findViewById(R.id.del_note) as TextView

        del_note.setText("Are you sure you want to delete it?")

        val updatedNote = NoteModel("Title Note 001", et_note.text.toString(), noteDate.toString())
        updatedNote.id = noteID

        dialogBuilder.setTitle("Delete Note")
        //dialogBuilder.setMessage("Enter data below")
        dialogBuilder.setPositiveButton("Delete", DialogInterface.OnClickListener { _, _ ->
            viewModal.deleteNote(updatedNote)
            Toast.makeText(this, "Note Deleted", Toast.LENGTH_LONG).show()
            startActivity(Intent(this@NewNoteActivity,MainActivity::class.java))
            finish()
        })
        dialogBuilder.setNegativeButton("Close", DialogInterface.OnClickListener { dialog, which ->
            //pass
        })
        val b = dialogBuilder.create()
        b.show()
    }

    private fun boldText() {
        val start = et_note!!.selectionStart
        val end = et_note!!.selectionEnd

        list!!.add(FaceModel(start, end, 1,0,0))
        val spannableString = SpannableString(et_note!!.text.toString())
        val buffer = StringBuffer()
        //Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        for (model in list!!) {
            val boldSpan = StyleSpan(Typeface.BOLD)
            spannableString.setSpan(
                boldSpan,
                model.start,
                model.end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        et_note!!.setText(spannableString)
    }

    private fun openFontFormatDialog() {
        val view = layoutInflater.inflate(R.layout.custom_font_format_layout, null, false)
        val closeFormatDailog = view.findViewById<ImageView>(R.id.fontFormatClose)
        val rb_bold = view.findViewById<RadioButton>(R.id.rb_bold)
        val rb_italic = view.findViewById<RadioButton>(R.id.rb_italic)
        val rb_underLine = view.findViewById<RadioButton>(R.id.rb_underLine)
        val rg_fontJustify = view.findViewById<RadioGroup>(R.id.rg_fontJustify)
        closeFormatDailog.setOnClickListener { formatDialog!!.cancel() }

        rb_bold.setOnClickListener {
            val start = et_note!!.selectionStart
            val end = et_note!!.selectionEnd
            //start+end.getTypeface().getStyle()==Typeface.BOLD

            list!!.add(FaceModel(start, end, 1,0,0))
            val spannableString = SpannableString(et_note!!.text.toString())
            for (model in list!!) {
                if(model.faceItalic==1 && model.faceBold==1){
                    val boldSpan = StyleSpan(Typeface.ITALIC)
                    spannableString.setSpan(
                        boldSpan,
                        model.start,
                        model.end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else if(model.faceItalic==1) {
                    val boldSpan = StyleSpan(Typeface.BOLD_ITALIC)
                    spannableString.setSpan(
                        boldSpan,
                        model.start,
                        model.end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else {
                    val boldSpan = StyleSpan(Typeface.BOLD)
                    spannableString.setSpan(
                        boldSpan,
                        model.start,
                        model.end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

            }
            et_note!!.setText(spannableString)
        }

        rb_italic.setOnClickListener {
            val start = et_note!!.selectionStart
            val end = et_note!!.selectionEnd

            list!!.add(FaceModel(start, end, 0,1,0))
            val spannableString = SpannableString(et_note!!.text.toString())
            val buffer = StringBuffer()
            //Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            for (model in list!!) {

                if(model.faceBold==1){
                    val boldSpan = StyleSpan(Typeface.BOLD_ITALIC)
                    spannableString.setSpan(
                        boldSpan,
                        model.start,
                        model.end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else {
                    val boldSpan = StyleSpan(Typeface.ITALIC)
                    spannableString.setSpan(
                        boldSpan,
                        model.start,
                        model.end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            et_note!!.setText(spannableString)
        }

        rb_underLine.setOnClickListener {
            val start = et_note!!.selectionStart
            val end = et_note!!.selectionEnd

            val strikethroughSpan = StrikethroughSpan()

            list!!.add(FaceModel(start, end, 0,0,1))
            val spannableString = SpannableString(et_note!!.text.toString())
            for (model in list!!) {
                val underlineSpan = UnderlineSpan()
                spannableString.setSpan(
                    underlineSpan,
                    model.start,
                    model.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            et_note!!.setText(spannableString)
        }

        rg_fontJustify.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { radioGroup, i ->
            when (i) {
                R.id.rb_left -> {
                    et_note!!.gravity = Gravity.LEFT
                    return@OnCheckedChangeListener
                }
                R.id.rb_center -> {
                    et_note!!.gravity = Gravity.CENTER
                    return@OnCheckedChangeListener
                }
                R.id.rb_right -> {
                    et_note!!.gravity = Gravity.RIGHT
                    return@OnCheckedChangeListener
                }
            }
        })
        formatDialog!!.setContentView(view)
    }

    private fun openFontSizeDialog() {
        val view = layoutInflater.inflate(R.layout.custom_font_size_dialog_layout, null, false)
        val close = view.findViewById<ImageView>(R.id.fontSizeClose)
        val fontSizeShow = view.findViewById<TextView>(R.id.fontSizeTvShow)
        val seekBar = view.findViewById<SeekBar>(R.id.fontSizeSeekbar)
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                seekbarValue = i
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                et_note!!.textSize = seekbarValue.toFloat()
                fontSizeShow.text = seekbarValue.toString() + ""
            }
        })
        close.setOnClickListener { sizeDialog!!.cancel() }
        sizeDialog!!.setContentView(view)
    }

    private fun openFontDialog() {
        val view = layoutInflater.inflate(R.layout.custom_font_layout, null, false)
        val font_abril_fatface = view.findViewById<TextView>(R.id.font_abril_fatface)
        val font_alegreya_sans_regular =
            view.findViewById<TextView>(R.id.font_alegreya_sans_regular)
        val font_abeezee_italic = view.findViewById<TextView>(R.id.font_abeezee_italic)
        val font_calligraffitti = view.findViewById<TextView>(R.id.font_calligraffitti)
        val font_cookie_regular = view.findViewById<TextView>(R.id.font_cookie_regular)
        val font_simonetta_italic = view.findViewById<TextView>(R.id.font_simonetta_italic)
        val closeDialog = view.findViewById<ImageView>(R.id.fontClose)
        closeDialog.setOnClickListener { fontFamilyDialog!!.cancel() }
        font_abril_fatface.setOnClickListener {
            val typeface = ResourcesCompat.getFont(applicationContext, R.font.abril_fatface)
            et_note!!.typeface = typeface
            font_abril_fatface.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_baseline_done_24,
                0
            )
            font_alegreya_sans_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_abeezee_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_calligraffitti.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_cookie_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_simonetta_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
        font_alegreya_sans_regular.setOnClickListener {
            val typeface = ResourcesCompat.getFont(applicationContext, R.font.alegreya_sans_regular)
            et_note!!.typeface = typeface
            font_abril_fatface.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_alegreya_sans_regular.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_baseline_done_24,
                0
            )
            font_abeezee_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_calligraffitti.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_cookie_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_simonetta_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
        font_abeezee_italic.setOnClickListener {
            val typeface = ResourcesCompat.getFont(applicationContext, R.font.abeezee_italic)
            et_note!!.typeface = typeface
            font_abril_fatface.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_alegreya_sans_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_abeezee_italic.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_baseline_done_24,
                0
            )
            font_calligraffitti.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_cookie_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_simonetta_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
        font_calligraffitti.setOnClickListener {
            val typeface = ResourcesCompat.getFont(applicationContext, R.font.calligraffitti)
            et_note!!.typeface = typeface
            font_abril_fatface.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_alegreya_sans_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_abeezee_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_calligraffitti.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_baseline_done_24,
                0
            )
            font_cookie_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_simonetta_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
        font_cookie_regular.setOnClickListener {
            val typeface = ResourcesCompat.getFont(applicationContext, R.font.cookie_regular)
            et_note!!.typeface = typeface
            font_abril_fatface.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_alegreya_sans_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_abeezee_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_calligraffitti.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_cookie_regular.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_baseline_done_24,
                0
            )
            font_simonetta_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
        font_simonetta_italic.setOnClickListener {
            val typeface = ResourcesCompat.getFont(applicationContext, R.font.simonetta_italic)
            et_note!!.typeface = typeface
            font_abril_fatface.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_alegreya_sans_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_abeezee_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_calligraffitti.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_cookie_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_simonetta_italic.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_baseline_done_24,
                0
            )
        }
        fontFamilyDialog!!.setContentView(view)
    }

    @SuppressLint("MissingPermission")
    fun findBT() {
        try {
            if (mBluetoothAdapter == null) {
                Toast.makeText(
                    applicationContext,
                    "No bluetooth adapter available",
                    Toast.LENGTH_LONG
                ).show()
            }
            if (!mBluetoothAdapter.isEnabled()) {
                val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBluetooth, 0)
            }
            val pairedDevices = mBluetoothAdapter.getBondedDevices()
            BTDeviceList!!.clear()
            if (pairedDevices.size > 0) {
                for (device in pairedDevices) {
                    BTDeviceList!!.add(device.name)
                }
            } else
                Toast.makeText(this, "No Paired Device found. Go to Setting", Toast.LENGTH_LONG).show()

            if(mBluetoothAdapter.isEnabled)
                openBTDeviceDialog()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                applicationContext,
                "Error Occur During Find BT Device\n$e",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun openBTDeviceDialog() {

        val alert = AlertDialog.Builder(this@NewNoteActivity)
        //val view = layoutInflater.inflate(R.layout.custom_bt_devices_layout, null)
        val view = layoutInflater.inflate(R.layout.bt_device_layout, null)
        val tv_mybtName = view.findViewById<TextView>(R.id.tv_device_name)
        val tv_openSetting = view.findViewById<TextView>(R.id.tv_openSetting)
        val switch_btOn = view.findViewById<Switch>(R.id.switch_bt)
        val list_device = view.findViewById<ListView>(R.id.lv_device)
        val closeBtDialog = view.findViewById<ImageView>(R.id.img_close)
        alert.setView(view)
        val alertDialog = alert.create()
        alertDialog.setCanceledOnTouchOutside(false)
        tv_mybtName.text = mBluetoothAdapter!!.name
        switch_btOn.isChecked = true
        switch_btOn.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                findBT()
            } else {
                mBluetoothAdapter!!.disable()
                tv_mybtName.text = "BT OFF"
                BTDeviceList!!.clear()
                alertDialog.dismiss()
            }
        }

        tv_openSetting.setOnClickListener {
            alertDialog.dismiss()
            startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, android.R.id.text1, BTDeviceList)
        list_device.adapter = adapter

        closeBtDialog.setOnClickListener { alertDialog.dismiss() }

        list_device.onItemClickListener = OnItemClickListener { adapterView, view, position, l ->
            val pairedDevices = mBluetoothAdapter!!.bondedDevices
            if (pairedDevices.size > 0) {
                for (device in pairedDevices) {
                    if (adapter.getItem(position)!!.trim { it <= ' ' } == device.name) {
                        try {
                            mmDevice = device
                            openBT()
                            alertDialog.dismiss()
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Toast.makeText(this@NewNoteActivity, e.toString(), Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }

        alertDialog.show()
    }

    @SuppressLint("MissingPermission")
    @Throws(IOException::class)
    fun openBT() {
        try {

            // Standard SerialPortService ID
            //UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            mmSocket = mmDevice!!.createRfcommSocketToServiceRecord(uuid)
            mmSocket.connect()
            mmOutputStream = mmSocket.getOutputStream()
            mmInputStream = mmSocket.getInputStream()
            beginListenForData()
            Toast.makeText(applicationContext, "Your Device connected", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                applicationContext,
                "This Device already connected\n Error: \n$e",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun beginListenForData() {
        try {
            val handler = Handler()

            // this is the ASCII code for a newline character
            val delimiter: Byte = 10
            stopWorker = false
            readBufferPosition = 0
            readBuffer = ByteArray(1024)
            workerThread = Thread {
                while (!Thread.currentThread().isInterrupted && !stopWorker) {
                    try {
                        val bytesAvailable = mmInputStream!!.available()
                        if (bytesAvailable > 0) {
                            val packetBytes = ByteArray(bytesAvailable)
                            Toast.makeText(
                                applicationContext,
                                packetBytes.toString() + "",
                                Toast.LENGTH_SHORT
                            ).show()
                            mmInputStream!!.read(packetBytes)
                            for (i in 0 until bytesAvailable) {
                                val b = packetBytes[i]
                                if (b == delimiter) {
                                    val encodedBytes = ByteArray(readBufferPosition)
                                    System.arraycopy(
                                        readBuffer, 0,
                                        encodedBytes, 0,
                                        encodedBytes.size
                                    )

                                    // specify US-ASCII encoding
                                    val data = String(encodedBytes, Charsets.US_ASCII)

                                    readBufferPosition = 0

                                    // tell the user data were sent to bluetooth printer device
                                    handler.post {
                                        Toast.makeText(
                                            applicationContext,
                                            data.toString(),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else {
                                    readBuffer[readBufferPosition++] = b
                                }
                            }
                        }
                    } catch (ex: IOException) {
                        stopWorker = true
                    }
                }
            }
            workerThread!!.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun sendData() {
        try {
            var msg = toolbarTitle+"   "+currentDateAndTime+"\n"+et_note!!.text.toString()
            msg += "\n"
            mmOutputStream!!.write(msg.toByteArray())
            Toast.makeText(applicationContext, "Data Sent to Printer", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                applicationContext,
                "No Device connected and error is: $e",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}