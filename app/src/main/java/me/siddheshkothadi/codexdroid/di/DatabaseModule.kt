package me.siddheshkothadi.codexdroid.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.siddheshkothadi.codexdroid.data.local.CodexDroidDatabase
import me.siddheshkothadi.codexdroid.data.local.ThreadDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_2_3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE threads ADD COLUMN threadJson TEXT")
            }
        }

    private val MIGRATION_3_4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rebuild `threads` with a composite primary key (connectionId, id).
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `threads_new` (
                      `id` TEXT NOT NULL,
                      `preview` TEXT NOT NULL,
                      `modelProvider` TEXT NOT NULL,
                      `createdAt` INTEGER NOT NULL,
                      `updatedAt` INTEGER NOT NULL,
                      `path` TEXT NOT NULL,
                      `cwd` TEXT NOT NULL,
                      `connectionId` TEXT NOT NULL,
                      `threadJson` TEXT,
                      PRIMARY KEY(`connectionId`, `id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `threads_new` (`id`,`preview`,`modelProvider`,`createdAt`,`updatedAt`,`path`,`cwd`,`connectionId`,`threadJson`)
                    SELECT `id`,`preview`,`modelProvider`,`createdAt`,`updatedAt`,`path`,`cwd`,`connectionId`,`threadJson`
                    FROM `threads`
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE `threads`")
                db.execSQL("ALTER TABLE `threads_new` RENAME TO `threads`")
            }
        }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CodexDroidDatabase {
        return Room.databaseBuilder(
            context,
            CodexDroidDatabase::class.java,
            "codexdroid_database"
        )
        .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideThreadDao(database: CodexDroidDatabase): ThreadDao {
        return database.threadDao()
    }
}
