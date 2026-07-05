package com.notikeep.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.notikeep.data.local.dao.AppRuleDao
import com.notikeep.data.local.dao.NotificationDao
import com.notikeep.data.local.entity.AppRuleEntity
import com.notikeep.data.local.entity.NotificationEntity
import com.notikeep.data.local.entity.NotificationFts

@Database(
    entities = [NotificationEntity::class, NotificationFts::class, AppRuleEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(NotikeepConverters::class)
abstract class NotikeepDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun appRuleDao(): AppRuleDao

    companion object {
        const val NAME = "notikeep.db"
    }
}
