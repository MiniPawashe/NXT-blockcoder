package com.example.nxt.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SavedProgramEntity::class], version = 1, exportSchema = false)
abstract class NxtAppDatabase : RoomDatabase() {

    abstract fun programDao(): ProgramDao

    companion object {
        @Volatile
        private var INSTANCE: NxtAppDatabase? = null

        fun getDatabase(context: Context): NxtAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NxtAppDatabase::class.java,
                    "nxt_programs_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
