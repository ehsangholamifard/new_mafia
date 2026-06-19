package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.MafiaDao
import com.example.data.model.PlayerEntity
import com.example.data.model.RoleEntity
import com.example.data.model.GameLogEntity
import com.example.data.model.GameHistoryEntity

@Database(
    entities = [PlayerEntity::class, RoleEntity::class, GameLogEntity::class, GameHistoryEntity::class],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mafiaDao(): MafiaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE players ADD COLUMN isVoteRevoked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE roles ADD COLUMN iconName TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE IF NOT EXISTS `game_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `winnerTeam` TEXT NOT NULL, `reason` TEXT NOT NULL, `playersJson` TEXT NOT NULL, `logsJson` TEXT NOT NULL)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE players ADD COLUMN isKilledToday INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE game_history ADD COLUMN moderatorName TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE players ADD COLUMN isSlaughtered INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN isHealedThisNight INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE players ADD COLUMN isShotThisNight INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE players ADD COLUMN doctorSelfSavesCount INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE players ADD COLUMN isBlockedThisNight INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE players ADD COLUMN isRevealedMafia INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE players ADD COLUMN willDieNextNight INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE players ADD COLUMN isRevivedThisNight INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE players ADD COLUMN hasBlankGunThisRound INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE players ADD COLUMN hasLiveGunThisRound INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE players ADD COLUMN usedLiveGun INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE roles ADD COLUMN abilitiesJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE players ADD COLUMN isInsuredThisNight INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // ignore if already exists
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mafia_god_database"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, 
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                        MIGRATION_9_10, MIGRATION_10_11
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
