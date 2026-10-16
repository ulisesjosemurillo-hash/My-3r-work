package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RecentDocumentEntity::class,
        Document::class,
        ReadingState::class,
        com.example.data.local.learning.SpacedRepetitionEntity::class,
        com.example.data.local.learning.StudySessionEntity::class,
        com.example.data.local.learning.CornellNoteEntity::class,
        Highlight::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recentDocumentDao(): RecentDocumentDao
    abstract fun documentDao(): DocumentDao
    abstract fun readingStateDao(): ReadingStateDao
    abstract fun studyDao(): com.example.data.local.learning.StudyDao
    abstract fun highlightDao(): HighlightDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recent_documents ADD COLUMN audioCommentsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE recent_documents ADD COLUMN questionsHistoryJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recent_documents ADD COLUMN author TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE recent_documents ADD COLUMN materia TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE recent_documents ADD COLUMN year TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE recent_documents ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE recent_documents ADD COLUMN collection TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE recent_documents ADD COLUMN tagsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE recent_documents ADD COLUMN generalNotes TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recent_documents ADD COLUMN positionHistoryJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS spaced_repetition_cards (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "documentId TEXT NOT NULL, " +
                    "concept TEXT NOT NULL, " +
                    "question TEXT NOT NULL, " +
                    "answer TEXT NOT NULL, " +
                    "sourcePage INTEGER NOT NULL, " +
                    "intervalDays INTEGER NOT NULL DEFAULT 1, " +
                    "easeFactor REAL NOT NULL DEFAULT 2.5, " +
                    "reviewsCount INTEGER NOT NULL DEFAULT 0, " +
                    "correctCount INTEGER NOT NULL DEFAULT 0, " +
                    "failCount INTEGER NOT NULL DEFAULT 0, " +
                    "nextReviewTimestamp INTEGER NOT NULL, " +
                    "lastReviewTimestamp INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS study_sessions (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "documentId TEXT NOT NULL, " +
                    "documentTitle TEXT NOT NULL, " +
                    "mode TEXT NOT NULL, " +
                    "durationMinutes INTEGER NOT NULL, " +
                    "questionsAnswered INTEGER NOT NULL, " +
                    "questionsCorrect INTEGER NOT NULL, " +
                    "selfReflection TEXT NOT NULL, " +
                    "timestamp INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS cornell_notes (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "documentId TEXT NOT NULL, " +
                    "pageNumber INTEGER NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "cuesJson TEXT NOT NULL, " +
                    "notes TEXT NOT NULL, " +
                    "summary TEXT NOT NULL, " +
                    "timestamp INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS highlights (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "documentId TEXT NOT NULL, " +
                    "pageNumber INTEGER NOT NULL, " +
                    "fragmentId INTEGER NOT NULL, " +
                    "selectedText TEXT NOT NULL, " +
                    "startOffset INTEGER NOT NULL DEFAULT 0, " +
                    "endOffset INTEGER NOT NULL DEFAULT 0, " +
                    "color TEXT NOT NULL DEFAULT '#FFF59D', " +
                    "timestamp INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_highlights_documentId ON highlights(documentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_highlights_documentId_pageNumber ON highlights(documentId, pageNumber)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_highlights_documentId_fragmentId ON highlights(documentId, fragmentId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "juristech_reader_db"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            try {
                                db.execSQL("UPDATE recent_documents SET pagesJson = 'file:pages_' || id || '.json' WHERE length(pagesJson) > 500")
                            } catch (_: Throwable) {}
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
