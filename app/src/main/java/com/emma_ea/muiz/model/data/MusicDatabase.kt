package com.emma_ea.muiz.model.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.emma_ea.muiz.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(entities = [Song::class], version = 1, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {

    abstract fun musicDAO() : MusicDao

    companion object {
        private var database: MusicDatabase? = null
        fun getDatabase(context: Context, scope: CoroutineScope): MusicDatabase {
            database?:kotlin.run {
                database = Room.databaseBuilder(context, MusicDatabase::class.java, "music_database")
                    .addCallback(roomCallback(scope))
                    .build()
            }
            return database!!
        }

        private fun roomCallback(scope: CoroutineScope) = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                database?.let {
                    scope.launch {
                        it.musicDAO().insert(
                            Song(
                                1,
                                1001,
                                "Guitar Solo",
                                "Guitarist",
                                "Greatest Hits",
                                "22",
                                "content://1011010",
                                "2022"
                            )
                        )
                    }
                }
            }
        }
    }
}