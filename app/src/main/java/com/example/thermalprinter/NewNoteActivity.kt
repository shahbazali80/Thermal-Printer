package com.example.thermalprinter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.text.*
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import com.example.thermalprinter.models.CustomTypeFaceSpan
import com.example.thermalprinter.models.FaceModel
import com.example.thermalprinter.models.FontModel
import com.example.thermalprinter.models.NoteModel
import com.example.thermalprinter.viewmodel.NoteViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_new_note.*
import kotlinx.android.synthetic.main.custom_font_format_layout.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log


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
    var fontlist: MutableList<FontModel>? = null
    var newLineList: MutableList<Int>? = null
    var isBold = false
    var isItalic = false
    var isUnderLine = false
    var isPrinterConnect = false
    var isBtnAddUpdate = false
    var isFocus = false

    var noteType : String ? = null
    var noteTitle : String ? = null
    var noteDescription : String ? = null
    var noteDate : String ? = null
    var toolbarTitle : String ? = null
    var currentDateAndTime: String ?  = null
    var connectedPrinterName: String ?  = null

    lateinit var spannableString : SpannableString

    lateinit var viewModal: NoteViewModel
    var noteID = -1;

    var faceBold = 0;
    var faceItalic = 0;
    var faceUnderline = 0;

    var handler: Handler = Handler()
    var runnable: Runnable? = null
    var delay = 1000

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
        fontlist = ArrayList()
        BTDeviceList = ArrayList()
        newLineList = ArrayList()

        list!!.clear()

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

            val item: MenuItem = bottomNavigationView.menu.findItem(R.id.doneNote)
            item.title = getString(R.string.update_text)
            //item.icon = ContextCompat.getDrawable(activity, R.drawable.ic_barcode)

        } else
            toolbarTitle="New Note 001"

        viewModal = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(NoteViewModel::class.java)

        if(!mBluetoothAdapter.isEnabled)
            findBT()

        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        fontFamilyDialog!!.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        sizeDialog!!.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        formatDialog!!.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        et_note.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {

                var start=et_note.selectionStart
                val letter=et_note!!.text.toString().substring(start-1,start)
                if(letter.equals(" "))
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

    private val mOnNavigationItemSelectedListener: BottomNavigationView.OnNavigationItemSelectedListener =
        object : BottomNavigationView.OnNavigationItemSelectedListener {
            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                run {
                    when (item.itemId) {
                        R.id.font -> fontFamilyDialog!!.show()
                        R.id.size -> sizeDialog!!.show()
                        R.id.format -> formatDialog!!.show()
                        R.id.doneNote -> {
                            isBtnAddUpdate=true
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
                if(isPrinterConnect==true) {
                    try {
                        sendData()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(applicationContext, "Error Occur During Sending Data\n$e", Toast.LENGTH_SHORT).show()
                    }
                } else
                    Toast.makeText(this, "No Printer Device Connected", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_findPrinter -> {
                if(isPrinterConnect==false)
                    findBT()
                else
                    Toast.makeText(this, "${connectedPrinterName} Already Connected", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_printReview -> {
                showPrintView()
                true
            }
            R.id.menu_delete -> {
                deleteNote()
                true
            }
            R.id.menu_setting -> {
                var str = et_note.text.toString()
                val words = str.split("\n" , " ") as ArrayList
                for(item in words){
                    val n = item.length
                    if(item.toString()=="")
                        android.util.Log.d("TAG","new line")
                    else
                        android.util.Log.d("TAG",item.toString())
                }
                true
            }
            R.id.menu_about -> {
                Toast.makeText(this, et_note.selectionStart.toString(), Toast.LENGTH_SHORT).show()
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

    private fun showPrintView() {
        val dialogBuilder = android.app.AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.print_preview_dialog, null)
        dialogBuilder.setView(dialogView)

        val tv_printview = dialogView.findViewById(R.id.tv_printview) as TextView

        dialogBuilder.setTitle("Print Preview")
        //dialogBuilder.setMessage("Enter data below")

        //set text their apploed font style
        var str = et_note.text.toString()
        str.replace("\\s".toRegex(), " ")
        val words = str.split(" " , "\n") as ArrayList

        for (item in words) {
            if(item.toString()!=""){
                if(item[0].toString().equals("*")){
                    val ww=item.replace("*","")
                    tv_printview.append(Html.fromHtml(
                        "<b>$ww</b>"
                    ))
                    tv_printview.append(" ")
                } else if(item[0].toString().equals("_")){
                    val ww=item.replace("_","")
                    tv_printview.append(Html.fromHtml(
                        "<u>$ww</u>"
                    ))
                    tv_printview.append(" ")
                } else if(item[0].toString()=="^") {
                    val ww=item.replace("^","")+"\n"
                    tv_printview.append(ww)
                } else {
                    tv_printview.append(item+" ")
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

        et_toolbar.setText(toolbarTitle)

        dialogBuilder.setPositiveButton("Update", DialogInterface.OnClickListener { _, _ ->
            if(et_toolbar.text.toString().isEmpty() ||  et_toolbar.text.toString().trim().isEmpty()){
                et_toolbar.setError("Write Note Title")
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
        dialogBuilder.setNegativeButton(Html.fromHtml("<font color='#FF7F27'>Close</font>"), DialogInterface.OnClickListener { dialog, which ->
            //pass
        })
        val b = dialogBuilder.create()
        b.show()
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
            var start=et_note.selectionStart
            var end=et_note.selectionEnd

            val action_letter=et_note!!.text.toString().substring(start-1,start)
            val selected_letter=et_note!!.text.toString().substring(start,end)
            if(action_letter=="*") {
                et_note.setText(et_note.text.toString().replace("*$selected_letter",selected_letter))
                Toast.makeText(this, "Bold is removed", Toast.LENGTH_SHORT).show()
            } else {
                et_note.text.insert(start, "*")
                Toast.makeText(this, "Bold is selected", Toast.LENGTH_SHORT).show()
            }

            rb_bold.isChecked = false;

        }

        rb_italic.setOnClickListener {
            val start = et_note!!.selectionStart
            val end = et_note!!.selectionEnd

            try {
                spannableString = SpannableString(et_note!!.text.toString())
                if(list!!.size>0) {
                    for (model in list!!) {
                        if (model.faceBold == 1 && model.start == start && model.end == end) {
                            list!!.add(FaceModel(start, end, 1, 1, 0))
                            val boldSpan = StyleSpan(Typeface.BOLD_ITALIC)
                            spannableString.setSpan(
                                boldSpan,
                               start,
                               end,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        } else {
                            list!!.add(FaceModel(start, end, 0, 1, 0))
                            val boldSpan = StyleSpan(Typeface.ITALIC)
                            spannableString.setSpan(
                                boldSpan,
                               start,
                               end,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        et_note!!.setText(spannableString)
                    }
                } else {
                    list!!.add(FaceModel(start, end, 0, 1, 0))
                    val boldSpan = StyleSpan(Typeface.ITALIC)
                    spannableString.setSpan(
                        boldSpan,
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    et_note!!.setText(spannableString)
                }
            }catch (e: Exception){
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        rb_underLine.setOnClickListener {
            val start = et_note.selectionStart
            val end = et_note.selectionEnd

            val action_letter=et_note!!.text.toString().substring(start-1,start)
            val selected_letter=et_note!!.text.toString().substring(start,end)
            if(action_letter=="_") {
                et_note.setText(et_note.text.toString().replace("_$selected_letter",selected_letter))
                Toast.makeText(this, "Underline is removed", Toast.LENGTH_SHORT).show()
            } else {
                et_note.text.insert(start, "_")
                Toast.makeText(this, "Underline is selected", Toast.LENGTH_SHORT).show()
            }

            rb_underLine.isChecked = false
        }

        rg_fontJustify.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { radioGroup, i ->
            val start = et_note!!.selectionStart
            val end = et_note!!.selectionEnd

            when (i) {
                R.id.rb_left -> {
                    //et_note!!.gravity = Gravity.LEFT
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
        val font_alegreya_sans_regular = view.findViewById<TextView>(R.id.font_alegreya_sans_regular)
        val font_abeezee_italic = view.findViewById<TextView>(R.id.font_abeezee_italic)
        val font_calligraffitti = view.findViewById<TextView>(R.id.font_calligraffitti)
        val font_cookie_regular = view.findViewById<TextView>(R.id.font_cookie_regular)
        val font_simonetta_italic = view.findViewById<TextView>(R.id.font_simonetta_italic)
        val closeDialog = view.findViewById<ImageView>(R.id.fontClose)

        closeDialog.setOnClickListener { fontFamilyDialog!!.cancel() }

        font_abril_fatface.setOnClickListener {

            val typeface = ResourcesCompat.getFont(applicationContext, R.font.abril_fatface)
            val robotoRegularSpan: TypefaceSpan = CustomTypeFaceSpan("",typeface)

            val start = et_note!!.selectionStart
            val end = et_note!!.selectionEnd
            list!!.add(FaceModel(start, end, 0,0,0))
            spannableString = SpannableString(et_note!!.text.toString())

            for (model in list!!) {
                spannableString.setSpan(robotoRegularSpan, model.start, model.end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            et_note!!.setText(spannableString)

            //et_note!!.typeface = typeface
            font_abril_fatface.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_done_24, 0)
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
            val robotoRegularSpan: TypefaceSpan = CustomTypeFaceSpan("",typeface)

            val start = et_note!!.selectionStart
            val end = et_note!!.selectionEnd
            list!!.add(FaceModel(start, end, 0,0,0))
            spannableString = SpannableString(et_note!!.text.toString())

            for (model in list!!) {
                spannableString.setSpan(robotoRegularSpan, model.start, model.end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            et_note!!.setText(spannableString)

            font_abril_fatface.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_alegreya_sans_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_abeezee_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_done_24, 0)
            font_calligraffitti.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_cookie_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_simonetta_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
        font_calligraffitti.setOnClickListener {

            val typeface = ResourcesCompat.getFont(applicationContext, R.font.calligraffitti)
            val robotoRegularSpan: TypefaceSpan = CustomTypeFaceSpan("",typeface)

            val start = et_note!!.selectionStart
            val end = et_note!!.selectionEnd
            list!!.add(FaceModel(start, end, 0,0,0))
            spannableString = SpannableString(et_note!!.text.toString())

            for (model in list!!) {
                spannableString.setSpan(robotoRegularSpan, model.start, model.end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            et_note!!.setText(spannableString)

            font_abril_fatface.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_alegreya_sans_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_abeezee_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_calligraffitti.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_done_24, 0)
            font_cookie_regular.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            font_simonetta_italic.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
        font_cookie_regular.setOnClickListener {

            val typeface = ResourcesCompat.getFont(applicationContext, R.font.cookie_regular)
            val robotoRegularSpan: TypefaceSpan = CustomTypeFaceSpan("",typeface)

            val start = et_note!!.selectionStart
            val end = et_note!!.selectionEnd
            list!!.add(FaceModel(start, end, 0,0,0))
            spannableString = SpannableString(et_note!!.text.toString())

            for (model in list!!) {
                spannableString.setSpan(robotoRegularSpan, model.start, model.end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            et_note!!.setText(spannableString)

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
            val robotoRegularSpan: TypefaceSpan = CustomTypeFaceSpan("",typeface)

            val start = et_note!!.selectionStart
            val end = et_note!!.selectionEnd
            list!!.add(FaceModel(start, end, 0,0,0))
            spannableString = SpannableString(et_note!!.text.toString())

            for (model in list!!) {
                spannableString.setSpan(robotoRegularSpan, model.start, model.end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            et_note!!.setText(spannableString)
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

        switch_btOn.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                if(isPrinterConnect==false)
                    findBT()
                else
                    Toast.makeText(this, "${connectedPrinterName} Already Connected", Toast.LENGTH_SHORT).show()
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
            mmOutputStream = mmSocket.getOutputStream()
            mmInputStream = mmSocket.getInputStream()
            beginListenForData()
            Toast.makeText(applicationContext, "$connectedPrinterName Device connected", Toast.LENGTH_LONG).show()
            isPrinterConnect=true

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                applicationContext,
                "Please again try to connect BT Device\n$e",
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

            val format = byteArrayOf(27, 33, 0)
            var titleMsg = "\n$toolbarTitle   $currentDateAndTime\n"
            mmOutputStream!!.write(format)
            mmOutputStream!!.write(titleMsg.toByteArray())

            //val format = byteArrayOf(27, 33, 0)
            //third parameter of format increase the size of text
            //val center = byteArrayOf(0x1b, 'a'.toByte(), 0x01)      // center alignment
            //val left = byteArrayOf(0x1b, 'a'.toByte(), 0x00)        // left alignment
            //val right = byteArrayOf(0x1b, 'a'.toByte(), 0x02)       // right alignment
            //format[2] = (0x8 or format.get(2).toInt()).toByte()     // text bold
            //format[2] = (0x80 or format.get(2).toInt()).toByte()    // Underline

            var str = et_note.text.toString()
            str.replace("\\s".toRegex(), " ")
            val words = str.split(" " , "\n") as ArrayList

            for (item in words) {
                if(item.toString()!=""){
                    if(item[0].toString().equals("*")){
                        val ww=item.replace("*","")+" "
                        format[2] = (0x8 or format.get(2).toInt()).toByte()
                        mmOutputStream!!.write(format)
                        mmOutputStream!!.write(ww.toByteArray())
                    } else if(item[0].toString().equals("_")){
                        val ww=item.replace("_","")
                        val format = byteArrayOf(27, 33, 0)
                        format[2] = (0x80 or format.get(2).toInt()).toByte()
                        mmOutputStream!!.write(format)
                        mmOutputStream!!.write(ww.toByteArray())
                        val ww1=" "
                        val format1 = byteArrayOf(27, 33, 0)
                        mmOutputStream!!.write(format1)
                        mmOutputStream!!.write(ww1.toByteArray())
                    } else if(item[0].toString()=="^") {
                        val ww=item.replace("^","")+"\n"
                        val format = byteArrayOf(27, 33, 0)
                        mmOutputStream!!.write(format)
                        mmOutputStream!!.write(ww.toByteArray())
                    } else {
                        val ww="$item "
                        val format = byteArrayOf(27, 33, 0)
                        mmOutputStream!!.write(format)
                        mmOutputStream!!.write(ww.toByteArray())
                    }
                }
            }

            /*var str = et_note.text.toString()
            var delimiter = " "
            val words = str.split(delimiter) as ArrayList

            for (word in words){
                if(word.substring(0,1)=="*"){
                    val ww=word.replace("*","")+" "
                    format[2] = (0x8 or format.get(2).toInt()).toByte()
                    mmOutputStream!!.write(format)
                    mmOutputStream!!.write(ww.toByteArray())
                }else if(word.substring(0,1)=="_"){
                    val ww=word.replace("_","")+" "
                    val format = byteArrayOf(27, 33, 0)
                    format[2] = (0x80 or format.get(2).toInt()).toByte()
                    mmOutputStream!!.write(format)
                    mmOutputStream!!.write(ww.toByteArray())
                } else {
                    val ww="$word "
                    val format = byteArrayOf(27, 33, 0)
                    mmOutputStream!!.write(format)
                    mmOutputStream!!.write(ww.toByteArray())
                }
            }*/

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