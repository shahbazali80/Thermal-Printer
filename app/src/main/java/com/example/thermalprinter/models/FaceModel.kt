package com.example.thermalprinter.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tbl_font")
class FaceModel(
    @ColumnInfo(name = "start_index")var start: Int,
    @ColumnInfo(name = "end_index")var end: Int,
    @ColumnInfo(name = "faceBold")var faceBold: Int,
    @ColumnInfo(name = "faceItalic")var faceItalic: Int,
    @ColumnInfo(name = "faceUnderline")var faceUnderline: Int){
    @PrimaryKey(autoGenerate = true)
    var font_id = 0
}