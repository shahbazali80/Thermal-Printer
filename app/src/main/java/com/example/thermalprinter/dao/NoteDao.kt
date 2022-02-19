package com.example.thermalprinter.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.thermalprinter.models.FaceModel
import com.example.thermalprinter.models.FontModel
import com.example.thermalprinter.models.NoteModel

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNote(noteModel: NoteModel?)

    @Delete
    fun deleteNote(noteModel: NoteModel?)

    @Update
    fun updateNote(model: NoteModel?)

    @Query(" SELECT * FROM tbl_notes")
    fun allNotes(): LiveData<List<NoteModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFont(faceModel: FaceModel?)
}