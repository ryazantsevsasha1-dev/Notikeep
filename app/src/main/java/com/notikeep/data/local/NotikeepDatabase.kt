package com.notikeep.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.notikeep.data.local.dao.AppRuleDao
import com.notikeep.data.local.dao.NotificationDao
import com.notikeep.data.local.entity.AppRuleEntity
import com.notikeep.data.local.entity.NotificationEntity

@Database(
    entities = [NotificationEntity::class, AppRuleEntity::class],
    version = 6,
    exportSchema = true,
)
@TypeConverters(NotikeepConverters::class)
abstract class NotikeepDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun appRuleDao(): AppRuleDao

    companion object {
        const val NAME = "notikeep.db"

        /** v2: unread tracking + favorites on captured notifications. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notifications ADD COLUMN isRead INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notifications ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v3: unique dedup index so backfilling active notifications is idempotent.
         * Existing duplicates are collapsed first (keeping the oldest row) or the
         * index creation would fail.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    DELETE FROM notifications WHERE id NOT IN (
                        SELECT MIN(id) FROM notifications
                        GROUP BY packageName, postedAt, title, text
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_notifications_packageName_postedAt_title_text` " +
                        "ON `notifications` (`packageName`, `postedAt`, `title`, `text`)",
                )
            }
        }

        /** v4: remove FTS as search moved to LIKE (better case/partial matching). */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS notifications_fts")
            }
        }

        /** v5: store the OS notification key so the BY_KEY dedup strategy can collapse updates. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notifications ADD COLUMN sbnKey TEXT")
            }
        }

        /**
         * v6: indexes that keep per-capture dedup lookups cheap as the table grows.
         * Names must match what Room derives from @Index or schema validation fails.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_notifications_packageName_title` " +
                        "ON `notifications` (`packageName`, `title`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_notifications_sbnKey` " +
                        "ON `notifications` (`sbnKey`)",
                )
            }
        }
    }
}
