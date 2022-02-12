package com.example.thermalprinter.models

import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "tbl_notes")
class NoteModel(
    @ColumnInfo(name = "note_title") val note_title: String,
    @ColumnInfo(name = "note_desc") val note_desc: String,
    @ColumnInfo(name = "timestamp") val timeStamp: String){
    @PrimaryKey(autoGenerate = true)
    var id = 0
}