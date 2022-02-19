package com.example.thermalprinter.repository

import androidx.lifecycle.LiveData
import com.example.thermalprinter.dao.NoteDao
import com.example.thermalprinter.models.FaceModel
import com.example.thermalprinter.models.FontModel
import com.example.thermalprinter.models.NoteModel

class NoteRepository (private val noteDao: NoteDao) {

    val allNotes: LiveData<List<NoteModel>> = noteDao.allNotes()

    suspend fun insert(noteModel: NoteModel) {
        noteDao.insertNote(noteModel)
    }

    suspend fun delete(noteModel: NoteModel){
        noteDao.deleteNote(noteModel)
    }

    suspend fun update(noteModel: NoteModel){
        noteDao.updateNote(noteModel)
    }

    suspend fun insertFont(faceModel: FaceModel) {
        noteDao.insertFont(faceModel)
    }
}