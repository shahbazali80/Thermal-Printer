package com.example.thermalprinter.repository

import androidx.lifecycle.LiveData
import com.example.thermalprinter.database.NoteDao
import com.example.thermalprinter.models.NoteModel

class NoteRepository (private val noteDao: NoteDao) {

    val allNotes: LiveData<List<NoteModel>> = noteDao.allNotes()

    fun insert(noteModel: NoteModel) {
        noteDao.insertNote(noteModel)
    }

    fun delete(noteModel: NoteModel){
        noteDao.deleteNote(noteModel)
    }

    fun update(noteModel: NoteModel){
        noteDao.updateNote(noteModel)
    }
}