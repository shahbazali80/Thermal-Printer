package com.example.thermalprinter.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thermalprinter.NewNoteActivity
import com.example.thermalprinter.R
import com.example.thermalprinter.models.NoteModel

class NoteAdapter(val context: Context) : RecyclerView.Adapter<NoteAdapter.ViewHolder>() {

    private val allNotes = ArrayList<NoteModel>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val noteTV = itemView.findViewById<TextView>(R.id.idTVNote)
        val dateTV = itemView.findViewById<TextView>(R.id.idTVDate)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.note_layout, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val id= allNotes.get(position).id
        val title= allNotes.get(position).note_title
        val desc= allNotes.get(position).note_desc
        val noteDate= allNotes.get(position).timeStamp

        holder.noteTV.setText(title)
        holder.dateTV.setText(allNotes.get(position).timeStamp)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, NewNoteActivity::class.java)
            intent.putExtra("noteType", "Edit")
            intent.putExtra("noteTitle", title)
            intent.putExtra("noteDescription", desc)
            intent.putExtra("noteDate", noteDate)
            intent.putExtra("noteId", id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return allNotes.size
    }

    fun setList(newList: List<NoteModel>) {
        allNotes.addAll(newList)
        notifyDataSetChanged()
    }
}

interface NoteClickDeleteInterface {
    fun onDeleteIconClick(note: NoteModel)
}