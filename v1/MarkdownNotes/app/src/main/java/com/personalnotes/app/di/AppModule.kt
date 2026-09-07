package com.personalnotes.app.di

import android.content.Context
import androidx.room.Room
import com.personalnotes.app.data.local.*
import com.personalnotes.app.data.remote.DriveService
import com.personalnotes.app.domain.repository.FolderRepository
import com.personalnotes.app.domain.repository.NoteRepository
import com.personalnotes.app.domain.repository.TagRepository
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
    fun provideDatabase(@ApplicationContext context: Context): NotesDatabase =
        Room.databaseBuilder(context, NotesDatabase::class.java, NotesDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideNoteDao(db: NotesDatabase): NoteDao = db.noteDao()
    @Provides fun provideFolderDao(db: NotesDatabase): FolderDao = db.folderDao()
    @Provides fun provideTagDao(db: NotesDatabase): TagDao = db.tagDao()

    @Provides
    @Singleton
    fun provideNoteRepository(noteDao: NoteDao): NoteRepository =
        NoteRepositoryImpl(noteDao)

    @Provides
    @Singleton
    fun provideFolderRepository(folderDao: FolderDao): FolderRepository =
        FolderRepositoryImpl(folderDao)

    @Provides
    @Singleton
    fun provideTagRepository(tagDao: TagDao): TagRepository =
        TagRepositoryImpl(tagDao)

    @Provides
    @Singleton
    fun provideDriveService(@ApplicationContext context: Context): DriveService =
        DriveService(context)
}
