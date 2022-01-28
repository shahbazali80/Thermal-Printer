package com.example.thermalprinter.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class OpenHelper extends SQLiteOpenHelper {

    public static final String db_name="db_thermal_printer.db";
    public static final String tbl_facetype="tbl_facetype";
    String CREATE_FACETYPE_TABLE=" CREATE TABLE "+tbl_facetype+"(face_id INTEGER PRIMARY KEY AUTOINCREMENT, start_index INTEGER, end_index INTEGER, face_type INTEGER)";

    public OpenHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_FACETYPE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS "+tbl_facetype);
        onCreate(db);
    }

    public boolean storeFacetypeInfo(int start_index, int end_index, int face_type){
        SQLiteDatabase db=this.getReadableDatabase();
        ContentValues values=new ContentValues();
        values.put("start_index",start_index);
        values.put("end_index",end_index);
        values.put("face_type",face_type);

        long res=db.insert(tbl_facetype,null,values);
        if(res==-1)
            return false;
        else
            return true;
    }

    public Integer deleteFacetypeInfo(){
        SQLiteDatabase database=this.getWritableDatabase();
        return database.delete(tbl_facetype, null,null);
    }

    public Integer deleteEachFacetypeInfo(String start_index, String end_index, String face_type){
        SQLiteDatabase database=this.getWritableDatabase();
        return database.delete(tbl_facetype, "start_index=? and end_index=? and face_type=?",new String[]{start_index, end_index, face_type});
    }
}