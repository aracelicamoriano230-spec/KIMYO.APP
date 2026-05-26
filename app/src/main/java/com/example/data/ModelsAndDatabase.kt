package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- entities ---

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val mode: String, // "STUDY_EXECUTIVO", "ELITE_BLACKER", "NORMAL"
    val signupTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "chats")
data class ChatItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val sender: String, // "user", "KimYo"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isHeader: Boolean = false // for rabbit/horse custom elements in chat
)

@Entity(tableName = "terminal_commands")
data class TerminalCommandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val command: String,
    val output: String,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val username: String,
    val isSynced: Boolean = false
)

@Entity(tableName = "supabase_config")
data class SupabaseConfigEntity(
    @PrimaryKey val id: Int = 1,
    val url: String = "",
    val anonKey: String = "",
    val isConnected: Boolean = false
)

// --- dao interfaces ---

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>
    
    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY timestamp ASC")
    fun getChatsFlow(): Flow<List<ChatItemEntity>>

    @Insert
    suspend fun insertChat(chat: ChatItemEntity)

    @Query("DELETE FROM chats")
    suspend fun clearChats()
}

@Dao
interface TerminalCommandDao {
    @Query("SELECT * FROM terminal_commands ORDER BY timestamp DESC")
    fun getTerminalCommandsFlow(): Flow<List<TerminalCommandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(cmd: TerminalCommandEntity)

    @Query("UPDATE terminal_commands SET isSynced = 1 WHERE id = :id")
    suspend fun markCommandAsSynced(id: Long)

    @Query("DELETE FROM terminal_commands")
    suspend fun clearHistory()
}

@Dao
interface SupabaseConfigDao {
    @Query("SELECT * FROM supabase_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): SupabaseConfigEntity?

    @Query("SELECT * FROM supabase_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<SupabaseConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: SupabaseConfigEntity)
}

// --- database ---

@Database(
    entities = [UserEntity::class, ChatItemEntity::class, TerminalCommandEntity::class, SupabaseConfigEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun terminalCommandDao(): TerminalCommandDao
    abstract fun supabaseConfigDao(): SupabaseConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kimyo_5v_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
