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
import com.notikeep.data.local.entity.NotificationFts

@Database(
    entities = [NotificationEntity::class, NotificationFts::class, AppRuleEntity::class],
    version = 2,
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
    }
}
