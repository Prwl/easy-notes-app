package com.prawalsharma.inkleaf.di

import android.content.Context
import androidx.room.Room
import com.prawalsharma.inkleaf.data.db.FolderDao
import com.prawalsharma.inkleaf.data.db.NoteDao
import com.prawalsharma.inkleaf.data.db.NoteDatabase
import com.prawalsharma.inkleaf.data.prefs.UserPreferences
import com.prawalsharma.inkleaf.data.repository.FolderRepository
import com.prawalsharma.inkleaf.data.repository.NoteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNoteDatabase(@ApplicationContext context: Context): NoteDatabase {
        // Use getInstance so the Glance widget shares the same instance
        return NoteDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideNoteDao(database: NoteDatabase): NoteDao = database.noteDao()

    @Provides
    @Singleton
    fun provideFolderDao(database: NoteDatabase): FolderDao = database.folderDao()

    @Provides
    @Singleton
    fun provideNoteRepository(noteDao: NoteDao): NoteRepository = NoteRepository(noteDao)

    @Provides
    @Singleton
    fun provideFolderRepository(folderDao: FolderDao): FolderRepository = FolderRepository(folderDao)

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences =
        UserPreferences(context)
}
