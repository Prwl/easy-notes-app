package com.prawalsharma.inkleaf.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.prawalsharma.inkleaf.data.model.Folder
import com.prawalsharma.inkleaf.data.model.Note

@Database(
    entities = [Note::class, Folder::class],
    version = 4,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao

    companion object {
        const val DATABASE_NAME = "easy_notes_db"

        @Volatile private var INSTANCE: NoteDatabase? = null

        /** Used by the Glance widget (outside Hilt scope). */
        fun getInstance(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS folders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        color INTEGER NOT NULL DEFAULT -26219,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes ADD COLUMN imageUris TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE notes ADD COLUMN drawingPath TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes ADD COLUMN isHidden INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
