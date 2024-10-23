package com.example.ruleengineast.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [RuleEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rules_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
