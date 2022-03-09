package com.example.thermalprinter.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.util.Log
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.thermalprinter.R
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
    private lateinit var BTDeviceList: MutableList<String>
    var newLineList: MutableList<Int>? = null
    var isPrinterConnect = false
    var isBtnAddUpdate = false

    var noteType : String ? = null
    var noteTitle : String ? = null
    var noteDescription : String ? = null
    private var noteDate : String ? = null
    private var toolbarTitle : String ? = null
    var currentDateAndTime: String ?  = null
    var connectedPrinterName: String ?  = null

    private lateinit var viewModal: NoteViewModel
    var noteID = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_note)

        setSupportActionBar(newNoteToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        BTDeviceList = ArrayList()
        newLineList = ArrayList()

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        var sdf = SimpleDateFormat("dd/MMM/yyyy")
        currentDateAndTime = sdf.format(Date())

        noteType = intent.getStringExtra("noteType")
        if (noteType.equals("Edit")) {
            noteTitle = intent.getStringExtra("noteTitle")
            noteDescription = intent.getStringExtra("noteDescription")
            noteDate = intent.getStringExtra("noteDate")
            noteID = intent.getIntExtra("noteId", -1)
            supportActionBar!!.title = noteTitle
            et_note.setText(noteDescription)
            toolbarTitle=noteTitle

            val item: MenuItem = bottomNavigationView.menu.findItem(R.id.doneNote)
            item.title = getString(R.string.update_text)

        } else {
            loadAndSavedTitle()
        }

        viewModal = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[NoteViewModel::class.java]

        if(!mBluetoothAdapter.isEnabled)
            findBT()

        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        et_note.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                var start=et_note.selectionStart
                val letter=et_note!!.text.toString().substring(start-1,start)
                if(letter == " ")
                    et_note.append("^")
                else {
                    et_note.append(" ")
                    et_note.append("^")
                }
                true
            }
            false
        })
    }

    private fun loadAndSavedTitle() {
        val sharedPreferences : SharedPreferences = getSharedPreferences("myPre", Context.MODE_PRIVATE)
        val savedTitle : String? = sharedPreferences.getString("titleCount", null)

        var titleNewInt : Int
        if(savedTitle!=null) {
            titleNewInt = savedTitle!!.toInt() + 1
            toolbarTitle = "New Note $titleNewInt"
            savedTitleToPref(titleNewInt)
            supportActionBar!!.title = "New Note $titleNewInt"
        } else {
            toolbarTitle = "New Note 1"
            savedTitleToPref(1)
        }

    }

    private fun savedTitleToPref(titleNewInt: Int) {
        val sharedPreferences : SharedPreferences = getSharedPreferences("myPre", Context.MODE_PRIVATE)
        val editor : SharedPreferences.Editor = sharedPreferences.edit()
        editor.apply {
            putString("titleCount", titleNewInt.toString())
                .apply()
        }
    }

    private val mOnNavigationItemSelectedListener: BottomNavigationView.OnNavigationItemSelectedListener =
        object : BottomNavigationView.OnNavigationItemSelectedListener {
            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                run {
                    when (item.itemId) {
                        R.id.size -> openSizeDialog()
                        R.id.format -> openFontDialog()
                        R.id.printReview -> {
                            when {
                                et_note!!.text.isNotEmpty() -> showPrintView()
                                else -> Toast.makeText(this@NewNoteActivity, "Write something before Print Review", Toast.LENGTH_SHORT).show()
                            }
                        }
                        R.id.doneNote -> {
                            isBtnAddUpdate=true
                            addUpdateNote()
                        }
                    }
                    return false
                }
            }
        }

    private fun openFontStyleDialog(firstWord : String) {
        val view: View = layoutInflater.inflate(R.layout.font_format_layout, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(view)

        dialog

        val closeFormatSheet = view.findViewById<ImageView>(R.id.fontFormatDialogClose)
        val rbBold = view.findViewById<RadioButton>(R.id.rb_bold)
        val rbUnderLine = view.findViewById<RadioButton>(R.id.rb_underLine)

        closeFormatSheet.setOnClickListener { dialog.cancel() }

        when (firstWord) {
            "*" -> rbBold.isChecked = true
            "#" -> rbUnderLine.isChecked = true
        }

        rbBold.setOnClickListener {
            var start=et_note.selectionStart
            var end=et_note.selectionEnd

            var bIndex = start-1

            if(et_note!!.hasSelection()) {
                if (bIndex != -1) {

                    val firstBoldWord = et_note!!.text.toString().substring(bIndex, start)
                    val word = et_note!!.text.toString().substring(start, end)
                    when (firstBoldWord) {
                        "*" -> {
                            et_note.setText(et_note.text.toString().replace("*$word*", word))
                            et_note!!.setSelection(start - 1)
                            rbBold.isChecked = false;
                        }
                        "•" -> {
                            et_note.setText(et_note.text.toString().replace("•$word•", word))
                            et_note.text.insert(start - 1, "*")
                            et_note.text.insert(end, "*")
                        }
                        "-" -> {
                            et_note.setText(et_note.text.toString().replace("-$word-", word))
                            et_note.text.insert(start - 1, "*")
                            et_note.text.insert(end, "*")
                        }
                        "#" -> {
                            et_note.setText(et_note.text.toString().replace("#$word#", word))
                            et_note.text.insert(start - 1, "*")
                            et_note.text.insert(end, "*")
                        }
                        else -> {
                            et_note.text.insert(start, "*")
                            et_note.text.insert(end + 1, "*")
                        }
                    }
                } else {
                    et_note.text.insert(start, "*")
                    et_note.text.insert(end + 1, "*")
                }
            } else {
                Toast.makeText(this, "Please Select some Text", Toast.LENGTH_SHORT).show()
                rbBold.isChecked = false
            }
        }

        rbUnderLine.setOnClickListener {
            val start = et_note.selectionStart
            val end = et_note.selectionEnd

            var bIndex = start-1

            if(et_note!!.hasSelection()) {
                if (bIndex != -1) {
                    val firstBoldWord = et_note!!.text.toString().substring(bIndex, start)
                    val word = et_note!!.text.toString().substring(start, end)

                    when (firstBoldWord) {
                        "#" -> {
                            et_note.setText(et_note.text.toString().replace("#$word#", word))
                            et_note!!.setSelection(start - 1)
                            rbUnderLine.isChecked = false
                        }
                        "•" -> {
                            et_note.setText(et_note.text.toString().replace("•$word•", word))
                            et_note.text.insert(start - 1, "#")
                            et_note.text.insert(end, "#")
                        }
                        "-" -> {
                            et_note.setText(et_note.text.toString().replace("-$word-", word))
                            et_note.text.insert(start - 1, "#")
                            et_note.text.insert(end, "#")
                        }
                        "*" -> {
                            et_note.setText(et_note.text.toString().replace("*$word*", word))
                            et_note.text.insert(start - 1, "#")
                            et_note.text.insert(end, "#")
                        }
                        else -> {
                            et_note.text.insert(start, "#")
                            et_note.text.insert(end + 1, "#")
                        }
                    }
                } else {
                    et_note.text.insert(start, "#")
                    et_note.text.insert(end + 1, "#")
                }
            } else {
                Toast.makeText(this, "Please Select some Text", Toast.LENGTH_SHORT).show()
                rbUnderLine.isChecked = false
            }
        }

        dialog.show()
    }

    private fun openFontSizeDialog(firstWord: String) {
        val view: View = layoutInflater.inflate(R.layout.font_size_layout, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(view)

        val close = view.findViewById<ImageView>(R.id.img_close_font_size_dialog)
        val cbSmall = view.findViewById<CheckBox>(R.id.cb_small_text_size)
        val cbNormal = view.findViewById<CheckBox>(R.id.cb_normal_text_size)
        val cbLarge = view.findViewById<CheckBox>(R.id.cb_large_text_size)

        when (firstWord) {
            "-" -> cbLarge.isChecked = true
            "•" -> cbSmall.isChecked = true
            else -> cbNormal.isChecked = true
        }

        cbSmall.setOnClickListener {
            if(et_note!!.hasSelection()) {
                cbNormal.isChecked = false
                cbLarge.isChecked = false
                applySizeOnText("•")
            } else {
                cbNormal.isChecked = false
                cbLarge.isChecked = false
                cbSmall.isChecked = false
                Toast.makeText(this, "Please Write some Text", Toast.LENGTH_SHORT).show()
            }
        }

        cbNormal.setOnClickListener {
            if(et_note!!.hasSelection()) {
                cbLarge.isChecked = false
                cbSmall.isChecked = false
                removeFont()
            } else {
                cbLarge.isChecked = false
                cbSmall.isChecked = false
                cbNormal.isChecked = false
                Toast.makeText(this, "Please Write some Text", Toast.LENGTH_SHORT).show()
            }
        }

        cbLarge.setOnClickListener {
            if(et_note!!.hasSelection()) {
                cbNormal.isChecked = false
                cbSmall.isChecked = false
                applySizeOnText("-")
            } else {
                cbNormal.isChecked = false
                cbSmall.isChecked = false
                cbLarge.isChecked = false
                Toast.makeText(this, "Please Write some Text", Toast.LENGTH_SHORT).show()
            }
        }
        close.setOnClickListener { dialog.cancel() }

        dialog.show()
    }

    private fun openSizeDialog() {
        when { et_note!!.hasSelection() -> {
            var start = et_note.selectionStart
            var bIndex = start-1
            if(bIndex!=-1) {
                if (et_note!!.hasSelection()) {
                    val firstBoldWord = et_note!!.text.toString().substring(bIndex, start)
                    openFontSizeDialog(firstBoldWord)
                }
            } else {
                openFontStyleDialog("")
            }
        }
            et_note!!.text.isEmpty() -> Toast.makeText(this@NewNoteActivity, "Please Write some Text", Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(this@NewNoteActivity, "Please Select Text First", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFontDialog() {
        when { et_note!!.hasSelection() -> {
            var start = et_note.selectionStart
            var bIndex = start-1
            if(bIndex!=-1) {
                if (et_note!!.hasSelection()) {
                    val firstBoldWord = et_note!!.text.toString().substring(bIndex, start)
                    openFontStyleDialog(firstBoldWord)
                }
            } else {
                openFontStyleDialog("")
            }
        }
            et_note!!.text.isEmpty() -> Toast.makeText(this@NewNoteActivity, "Please Write some Text", Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(this@NewNoteActivity, "Please Select Text First", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addUpdateNote() {
        if(et_note.text.toString().isEmpty() ||  et_note.text.toString().trim().isEmpty()){
            et_note.error = "Write Something here"
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
                    if(isBtnAddUpdate)
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
                    if (isBtnAddUpdate)
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
                printOutCalled()
                true
            }
            R.id.menu_findPrinter -> {
                findPrintCalled()
                true
            }
            R.id.menu_delete -> {
                deleteNote()
                true
            }
            R.id.menu_setting -> {
                Toast.makeText(this, "Setting is clicked", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_about -> {
                Toast.makeText(this, "About is clicked", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_editToolbar -> {
                openEditToolbarTile()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun findPrintCalled() {
        if(!isPrinterConnect)
            findBT()
        else
            Toast.makeText(
                this,
                "$connectedPrinterName Already Connected",
                Toast.LENGTH_SHORT
            ).show()
    }

    private fun printOutCalled() {
        if(isPrinterConnect) {
            try {
                sendData()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(applicationContext, "Error Occur During Sending Data\n$e", Toast.LENGTH_SHORT).show()
            }
        } else
            Toast.makeText(this, "No Printer Device Connected", Toast.LENGTH_SHORT).show()
    }

    private fun showPrintView() {
        val dialogBuilder = android.app.AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.print_preview_dialog, null)
        dialogBuilder.setView(dialogView)

        val tv_printview = dialogView.findViewById(R.id.tv_printview) as TextView

        dialogBuilder.setTitle("Print Preview")
        //dialogBuilder.setMessage("Enter data below")

        var str = et_note.text.toString()
        str.replace("\\s".toRegex(), " ")
        val words = str.split(" " , "\n") as ArrayList

        for (item in words) {
            if(item!=""){
                if(item[0].toString() == "*" && item[item.length-1].toString() == "*"){
                    val ww=item.replace("*","")
                    tv_printview.append(Html.fromHtml(
                        "<b>$ww</b>"
                    ))
                    tv_printview.append(" ")
                } else if(item[0].toString() == "#" && item[item.length-1].toString() == "#"){
                    val ww=item.replace("#","")
                    tv_printview.append(Html.fromHtml(
                        "<u>$ww</u>"
                    ))
                    tv_printview.append(" ")
                } else if(item[0].toString()=="^") {
                    val ww=item.replace("^","")+"\n"
                    tv_printview.append(ww)
                } else if(item[0].toString() == "-" && item[item.length-1].toString() == "-"){
                    val ww=item.replace("-","")
                    tv_printview.append(Html.fromHtml("<big>$ww</big>"))
                    tv_printview.append(" ")
                } else if(item[0].toString() == "•" && item[item.length-1].toString() == "•"){
                    val ww=item.replace("•","")
                    tv_printview.append(Html.fromHtml("<small>$ww</small>"))
                    tv_printview.append(" ")
                } else {
                    tv_printview.append("$item ")
                }
            }
        }

        dialogBuilder.setNegativeButton(Html.fromHtml("<font color='#FF7F27'>Close</font>"), DialogInterface.OnClickListener { _, _ ->

        })
        val b = dialogBuilder.create()
        b.show()
    }

    private fun openEditToolbarTile() {
        val dialogBuilder = android.app.AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.edit_toolbar_dialog, null)
        dialogBuilder.setView(dialogView)

        val et_toolbar = dialogView.findViewById(R.id.et_toolbar_edit) as TextView

        dialogBuilder.setTitle("Update Note Title")
        //dialogBuilder.setMessage("Enter data below")

        et_toolbar.text = toolbarTitle

        dialogBuilder.setPositiveButton(Html.fromHtml("<font color='#FF7F27'>Update</font>"), DialogInterface.OnClickListener { _, _ ->
            if(et_toolbar.text.toString().isEmpty() ||  et_toolbar.text.toString().trim().isEmpty()){
                et_toolbar.error = "Write Note Title"
                et_toolbar.requestFocus()
                et_toolbar.performClick()
            }else {
                toolbarTitle = et_toolbar.text.toString()
                supportActionBar!!.setTitle(toolbarTitle)
            }
        })
        dialogBuilder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
            //pass
        })
        val b = dialogBuilder.create()
        b.show()
    }

    private fun deleteNote() {
        val dialogBuilder = android.app.AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.delete_dialog, null)
        dialogBuilder.setView(dialogView)

        val del_note = dialogView.findViewById(R.id.del_note) as TextView

        del_note.text = "Are you sure you want to delete it?"

        val updatedNote = NoteModel("Title Note 001", et_note.text.toString(), noteDate.toString())
        updatedNote.id = noteID

        dialogBuilder.setTitle("Delete Note")
        //dialogBuilder.setMessage("Enter data below")
        dialogBuilder.setPositiveButton(Html.fromHtml("<font color='#FF7F27'>Delete</font>"), DialogInterface.OnClickListener { _, _ ->
            viewModal.deleteNote(updatedNote)
            Toast.makeText(this, "Note Deleted", Toast.LENGTH_LONG).show()
            startActivity(Intent(this@NewNoteActivity, MainActivity::class.java))
            finish()
        })
        dialogBuilder.setNegativeButton("Close", DialogInterface.OnClickListener { dialog, which ->
            //pass
        })
        val b = dialogBuilder.create()
        b.show()
    }

    private fun removeFont() {
        var start=et_note.selectionStart
        var end=et_note.selectionEnd

        var bIndex = start-1
        if(bIndex!=-1) {
            val firstLetter = et_note!!.text.toString().substring(bIndex, start)
            val word = et_note!!.text.toString().substring(start, end)
            when (firstLetter) {
                "•" -> {
                    et_note.setText(et_note.text.toString().replace("•$word•", word))
                    et_note!!.setSelection(start - 1)
                }
                "-" -> {
                    et_note.setText(et_note.text.toString().replace("-$word-", word))
                    et_note!!.setSelection(start - 1)
                }
                else -> {
                    Toast.makeText(this, "Normal Already Applied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applySizeOnText(symbol: String) {
        var start=et_note.selectionStart
        var end=et_note.selectionEnd

        var bIndex = start-1

        if(bIndex!=-1) {

            val firstLetter = et_note!!.text.toString().substring(bIndex, start)
            val word = et_note!!.text.toString().substring(start, end)

            when (firstLetter) {
                symbol -> {
                    et_note.setText(et_note.text.toString().replace("$symbol$word$symbol", word))
                    et_note!!.setSelection(start-1)
                }
                "-" -> {
                    et_note.setText(et_note.text.toString().replace("-$word-", word))
                    et_note.text.insert(start-1, symbol)
                    et_note.text.insert(end, symbol)
                }
                "*" -> {
                    et_note.setText(et_note.text.toString().replace("*$word*", word))
                    et_note.text.insert(start-1, symbol)
                    et_note.text.insert(end, symbol)
                }
                "#" -> {
                    et_note.setText(et_note.text.toString().replace("#$word#", word))
                    et_note.text.insert(start-1, symbol)
                    et_note.text.insert(end, symbol)
                }
                else -> {
                    et_note.text.insert(start, symbol)
                    et_note.text.insert(end+1, symbol)
                }
            }
        } else {
            et_note.text.insert(start, symbol)
            et_note.text.insert(end+1, symbol)
        }
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
            if (!mBluetoothAdapter.isEnabled) {
                val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBluetooth, 0)
            }

            val pairedDevices = mBluetoothAdapter.bondedDevices
            BTDeviceList!!.clear()
            if (pairedDevices.size > 0) {

                for (device in pairedDevices) {
                    BTDeviceList!!.add(device.name)
                }
            }

            if(mBluetoothAdapter.isEnabled)
                openBTDeviceDialog()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                applicationContext,
                "Error Occur During Find BT Device\n$e",
                Toast.LENGTH_SHORT
            ).show()
            Log.d("TAG",e.toString())
        }
    }

    @SuppressLint("MissingPermission")
    private fun openBTDeviceDialog() {

        val alert = AlertDialog.Builder(this@NewNoteActivity)
        val view = layoutInflater.inflate(R.layout.bt_device_layout, null)
        val tv_mybtName = view.findViewById<TextView>(R.id.tv_device_name)
        val tv_openSetting = view.findViewById<TextView>(R.id.tv_openSetting)
        val otherBTDeviceTitle = view.findViewById<TextView>(R.id.otherBTDeviceTitle)
        val switch_btOn = view.findViewById<Switch>(R.id.switch_bt)
        val list_device = view.findViewById<ListView>(R.id.lv_device)
        val closeBtDialog = view.findViewById<ImageView>(R.id.img_close)
        alert.setView(view)
        val alertDialog = alert.create()
        alertDialog.setCanceledOnTouchOutside(false)

        tv_mybtName.text = mBluetoothAdapter!!.name
        switch_btOn.isChecked = true

        switch_btOn.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if(!isPrinterConnect)
                    findBT()
                else
                    Toast.makeText(this, "$connectedPrinterName Already Connected", Toast.LENGTH_SHORT).show()
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

        if(adapter.isEmpty){
            otherBTDeviceTitle.setText("No Paired Device Found")
            otherBTDeviceTitle.gravity = Gravity.CENTER_HORIZONTAL
            Toast.makeText(this, "Go to Setting for Bluetooth Device", Toast.LENGTH_LONG).show()
        }

        closeBtDialog.setOnClickListener { alertDialog.dismiss() }

        list_device.onItemClickListener = OnItemClickListener { adapterView, view, position, l ->
            val pairedDevices = mBluetoothAdapter!!.bondedDevices
            if (pairedDevices.size > 0) {
                for (device in pairedDevices) {
                    if (adapter.getItem(position)!!.trim { it <= ' ' } == device.name) {
                        try {
                            mmDevice = device
                            connectedPrinterName = device.name
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
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            mmSocket = mmDevice!!.createRfcommSocketToServiceRecord(uuid)
            mmSocket.connect()
            mmOutputStream = mmSocket.outputStream
            mmInputStream = mmSocket.inputStream
            beginListenForData()
            Toast.makeText(applicationContext, "$connectedPrinterName Device connected", Toast.LENGTH_LONG).show()
            isPrinterConnect=true

        } catch (e: Exception) {
            e.printStackTrace()

            var str = e.toString()
            val errors = str.split("," , ":") as ArrayList
            val errorMsg=errors[1].toString()+","+errors[2].toString()

            Toast.makeText(
                applicationContext,
                "Failed! try again\n$errorMsg",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun beginListenForData() {
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

            val format = byteArrayOf(27, 33, 0)
            var titleMsg = "\n$toolbarTitle   $currentDateAndTime\n"
            mmOutputStream!!.write(format)
            mmOutputStream!!.write(titleMsg.toByteArray())

            // Fonts that can be implement in Bluetooth Printer
            //val format = byteArrayOf(27, 33, 0)                     // default format
            //val center = byteArrayOf(0x1b, 'a'.toByte(), 0x01)      // center alignment
            //val left = byteArrayOf(0x1b, 'a'.toByte(), 0x00)        // left alignment
            //val right = byteArrayOf(0x1b, 'a'.toByte(), 0x02)       // right alignment
            //format[2] = (0x8 or format.get(2).toInt()).toByte()     // text bold
            //format[2] = (0x80 or format.get(2).toInt()).toByte()    // Underline
            //format[2] = (0x0 or format.get(2).toInt()).toByte()     // Smaller
            //format[2] = (0x1 or format.get(2).toInt()).toByte()     // Medium
            //format[2] = (0x2 or format.get(2).toInt()).toByte()     // Larger

            var str = et_note.text.toString()
            str.replace("\\s".toRegex(), " ")
            val words = str.split(" " , "\n") as ArrayList

            for (item in words) {
                if(item.toString()!=""){
                    when {
                        item[0].toString() == "*" -> {
                            val ww=item.replace("*","")+" "
                            format[2] = (0x8 or format.get(2).toInt()).toByte()
                            mmOutputStream!!.write(format)
                            mmOutputStream!!.write(ww.toByteArray())
                        }
                        item[0].toString() == "#" -> {
                            val ww=item.replace("#","")
                            val format = byteArrayOf(27, 33, 0)
                            format[2] = (0x80 or format.get(2).toInt()).toByte()
                            mmOutputStream!!.write(format)
                            mmOutputStream!!.write(ww.toByteArray())
                            val ww1=" "
                            val format1 = byteArrayOf(27, 33, 0)
                            mmOutputStream!!.write(format1)
                            mmOutputStream!!.write(ww1.toByteArray())
                        }
                        item[0].toString()=="^" -> {
                            val ww=item.replace("^","")+"\n"
                            val format = byteArrayOf(27, 33, 0)
                            mmOutputStream!!.write(format)
                            mmOutputStream!!.write(ww.toByteArray())
                        }
                        item[0].toString()=="•" -> {
                            val ww=item.replace("•","")
                            val format = byteArrayOf(27, 33, 0)
                            format[2] = (0x0 or format.get(2).toInt()).toByte()
                            mmOutputStream!!.write(format)
                            mmOutputStream!!.write(ww.toByteArray())
                            Log.d("formatCall", "small")
                            val ww1=" "
                            val format1 = byteArrayOf(27, 33, 0)
                            mmOutputStream!!.write(format1)
                            mmOutputStream!!.write(ww1.toByteArray())
                        }
                        item[0].toString()=="-" -> {
                            val ww=item.replace("-","")
                            val format = byteArrayOf(27, 33, 0)
                            format[2] = (0x2 or format.get(2).toInt()).toByte()
                            mmOutputStream!!.write(format)
                            mmOutputStream!!.write(ww.toByteArray())
                            Log.d("formatCall", "large")
                            val ww1=" "
                            val format1 = byteArrayOf(27, 33, 0)
                            mmOutputStream!!.write(format1)
                            mmOutputStream!!.write(ww1.toByteArray())
                        }
                        else -> {
                            val ww="$item "
                            val format = byteArrayOf(27, 33, 0)
                            mmOutputStream!!.write(format)
                            mmOutputStream!!.write(ww.toByteArray())
                        }
                    }
                }
            }

            var newLine="\n"
            mmOutputStream!!.write(newLine.toByteArray())

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                applicationContext,
                "Error: $e",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        addUpdateNote()
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressed()
        addUpdateNote()
        return false
    }
}