package com.naver.test

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(
    context, "itemdb", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        val itemSQL = "create table item " +
                "(itemid integer primary key," +
                "itemname," +
                "price integer," +
                "description," +
                "pictureurl," +
                "updatedate)"
        db.execSQL(itemSQL)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("drop table item")
        onCreate(db)
    }
}