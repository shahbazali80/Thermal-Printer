package com.example.thermalprinter.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.thermalprinter.R
import com.example.thermalprinter.adapter.NoteAdapter
import com.example.thermalprinter.viewmodel.NoteViewModel
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    lateinit var viewModal: NoteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = NoteAdapter(this)

        recyclerView.adapter = adapter

        viewModal = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[NoteViewModel::class.java]

        viewModal.allNotes.observe(this, Observer { list ->
            list?.let {
                if(it.isNullOrEmpty()){
                    linearLayout.visibility = View.GONE
                    tv_welcmNote.visibility = View.VISIBLE
                    tv_welcmNote.text = "Bluetooth Notes Printer helps to take prints via thermal, lets get started with your first note " +
                            "by clicking on 'New Note' button bottom right corner"
                } else {
                    tv_welcmNote.visibility = View.GONE
                    tv_welcm.text = "Notes"
                    adapter.setList(it)
                }
            }
        })

        addFabBtn.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, NewNoteActivity::class.java)
            intent.putExtra("noteType", "New")
            startActivity(intent)
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }
}