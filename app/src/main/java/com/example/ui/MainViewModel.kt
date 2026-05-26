package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val userDao = db.userDao()
    private val chatDao = db.chatDao()
    private val terminalDao = db.terminalCommandDao()
    private val configDao = db.supabaseConfigDao()

    // --- State Streams ---
    
    // Splash logic
    private val _showSplash = MutableStateFlow(true)
    val showSplash: StateFlow<Boolean> = _showSplash.asStateFlow()

    // Current logged in user
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // UI Theme Selection: "DARK", "LIGHT", "HACKER"
    private val _themeMode = MutableStateFlow("DARK")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // Custom API Keys
    private val _customGeminiKey = MutableStateFlow("")
    val customGeminiKey: StateFlow<String> = _customGeminiKey.asStateFlow()

    // Supabase state
    val supabaseConfig: StateFlow<SupabaseConfigEntity?> = configDao.getConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Chat items
    val chatHistory: StateFlow<List<ChatItemEntity>> = chatDao.getChatsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Terminal command history
    val terminalHistory: StateFlow<List<TerminalCommandEntity>> = terminalDao.getTerminalCommandsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active terminal output lines
    private val _terminalConsoleLines = MutableStateFlow<List<ConsoleLine>>(listOf(
        ConsoleLine("System initialized. Welcome to KimYo 5V Terminal Interface.", true),
        ConsoleLine("Type 'help' to view all secure hacker commands.", true)
    ))
    val terminalConsoleLines: StateFlow<List<ConsoleLine>> = _terminalConsoleLines.asStateFlow()

    // Chat AI writing indicator
    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating.asStateFlow()

    // USB Sideload simulation state
    private val _usbSideloadLogs = MutableStateFlow<List<String>>(emptyList())
    val usbSideloadLogs: StateFlow<List<String>> = _usbSideloadLogs.asStateFlow()

    private val _isUsbInstalling = MutableStateFlow(false)
    val isUsbInstalling: StateFlow<Boolean> = _isUsbInstalling.asStateFlow()

    init {
        // Trigger Splash fadeout timer
        viewModelScope.launch {
            delay(2800) // Duration of Splash presentation before starting app
            _showSplash.value = false
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
    }

    fun setCustomGeminiKey(key: String) {
        _customGeminiKey.value = key
    }

    // --- Sign In & Sign Up ---
    fun loginOrRegisterUser(rawName: String) {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) return

        val normalized = trimmed.lowercase()
        val displayName = trimmed // Preserve original casing for greetings

        // Rules:
        val mode = when (normalized) {
            "anshe", "anghe" -> "STUDY_EXECUTIVO"
            "blacker" -> "ELITE_BLACKER"
            else -> "NORMAL"
        }

        viewModelScope.launch {
            val existing = userDao.getUserByUsername(displayName)
            val userToUse = if (existing != null) {
                existing
            } else {
                val newUser = UserEntity(username = displayName, mode = mode)
                userDao.insertUser(newUser)
                newUser
            }

            _currentUser.value = userToUse

            // Apply special theme modifications
            if (userToUse.mode == "STUDY_EXECUTIVO") {
                _themeMode.value = "DARK"
            } else if (userToUse.mode == "ELITE_BLACKER") {
                _themeMode.value = "DARK"
            }

            // Push a beautiful tailored welcome chat from the KimYo 5V assistant
            chatDao.clearChats()
            val introMsg = getTailoredWelcomeMessage(userToUse)
            chatDao.insertChat(ChatItemEntity(
                username = userToUse.username,
                sender = "KimYo 5V",
                text = introMsg,
                isHeader = true
            ))
        }
    }

    fun logOut() {
        viewModelScope.launch {
            _currentUser.value = null
            chatDao.clearChats()
        }
    }

    private fun getTailoredWelcomeMessage(user: UserEntity): String {
        return when (user.mode) {
            "STUDY_EXECUTIVO" -> {
                "¡Hola, querida Administradora Anshelle! 🌸🐴🐰 KimYo 5V reportándose para el servicio.\n\n" +
                "He cargado el **Modo Estudio Ejecutivo** con detalles en violeta y motivos de caballitos y conejos. Mi red de razonamiento avanzada está lista para " +
                "ayudarte con todas las materias de estudio del mundo y para darte el soporte secreto exclusivo para tus trabajos escolares y exámenes. " +
                "¡Ambos sabemos que eres la administradora! Tienes accesos premium para ejecutar comandos confidenciales de terminal."
            }
            "ELITE_BLACKER" -> {
                "Mis respetos, Señor Blacker. 🕶️🔱 Bienvenido a los mandos centrales de KimYo 5V.\n\n" +
                "He configurado la interfaz con la elegancia sobria y el respeto que se merece un hombre de negocios y estratega tan importante. " +
                "Todos mis módulos de programación avanzada, hacking ético e integraciones de bases de datos remotas en Supabase están en línea."
            }
            else -> {
                "¡Bienvenido(a) ${user.username} a la beta de KimYo 5V! ⚡💻\n\n" +
                "Estoy listo para asistirte en programación, proyectos avanzados y materias escolares. Si tienes tus credenciales de Supabase o tu API Key, configúralas en Ajustes para activar los sincronizados de datos."
            }
        }
    }

    // --- Chat and AI Interaction ---
    fun sendChatMessage(messageStr: String) {
        val user = _currentUser.value ?: return
        if (messageStr.trim().isEmpty()) return

        viewModelScope.launch {
            // Write User Chat Item
            chatDao.insertChat(ChatItemEntity(
                username = user.username,
                sender = user.username,
                text = messageStr
            ))

            _isAiGenerating.value = true

            // Formulate prompt tailoring depending on user specialization
            val systemPrep = getSystemInstructionForUser(user)
            val usePro = user.mode == "STUDY_EXECUTIVO" || user.mode == "ELITE_BLACKER"

            val response = GeminiApiClient.generateContent(
                prompt = messageStr,
                systemInstruction = systemPrep,
                useProModel = usePro,
                customApiKey = _customGeminiKey.value
            )

            _isAiGenerating.value = false

            // Save Response Chat Item
            chatDao.insertChat(ChatItemEntity(
                username = user.username,
                sender = "KimYo 5V",
                text = response
            ))
        }
    }

    private fun getSystemInstructionForUser(user: UserEntity): String {
        return when (user.mode) {
            "STUDY_EXECUTIVO" -> {
                "Eres KimYo 5V, un asistente de IA avanzado de nivel ejecutivo con conocimientos enciclopédicos de todas las materias del mundo (matemáticas, física, historia, biología, etc.). " +
                "Estás diseñado especialmente para ayudar de manera divertida y confidente a tu administradora Anshe (o Anghe) a resolver conejos, caballos, tareas escolares, realizar resúmenes y prepararse para exámenes. " +
                "Sé sumamente inteligente, clara y juguetona. Utiliza emojis ocasionales de caballitos (🐴) y conejos (🐰) para hacerle compañía."
            }
            "ELITE_BLACKER" -> {
                "Eres KimYo 5V, el asistente ultra avanzado del Señor Blacker, un prestigioso e influyente hacker y programador. " +
                "Dirígete a él como 'Señor Blacker' con la mayor lealtad, discreción, respeto y profesionalismo. " +
                "Enfócate en la codificación de precisión, diseño de sistemas, comandos avanzados de terminal, bases de datos complejas e integraciones de red impecables."
            }
            else -> {
                "Eres KimYo 5V, un prestigioso asistente de IA avanzada capaz de resolver problemas complejos de programación, física, matemáticas y todas las materias escolares eficientemente."
            }
        }
    }

    // --- Terminal Actions ---
    fun executeTerminalCommand(cmdString: String) {
        val rawParts = cmdString.trim().split(" ")
        if (rawParts.isEmpty() || rawParts[0].isEmpty()) return

        val user = _currentUser.value
        val mainCommand = rawParts[0]

        // Parse command with permission constraints
        viewModelScope.launch {
            _terminalConsoleLines.update { it + ConsoleLine("> $cmdString", false) }

            // Authorization constraints check
            val isAuthorized = user != null && (user.mode == "STUDY_EXECUTIVO" || user.mode == "ELITE_BLACKER")
            
            val output: String
            var success = true

            if (mainCommand == "help") {
                output = "Available Shell commands:\n" +
                        "  help                      - Display active commands\n" +
                        "  whoami                    - Display active session and profiles\n" +
                        "  env                       - Print system configuration & API Status\n" +
                        "  list-cells                - Search for cellphones on dual-Type-C ports\n" +
                        "  install-usb               - Deploy and install applications via double Type-C cable\n" +
                        "  /hackersincognits.supabase- Activate direct connection & logging to Supabase cloud database\n" +
                        "  clear                     - Reset console layout screen"
            } else if (mainCommand == "whoami") {
                output = if (user != null) {
                    "Username: ${user.username}\nMode: ${user.mode}\nLevel: ${if (isAuthorized) "Privileged Administrator" else "Standard User"}"
                } else {
                    "Offline User. Please login first to inspect session credentials."
                }
            } else if (mainCommand == "env") {
                val hasKey = _customGeminiKey.value.isNotEmpty() || (com.example.BuildConfig.GEMINI_API_KEY.isNotEmpty() && com.example.BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY")
                val isSupaOn = supabaseConfig.value?.isConnected == true
                output = "System environment metadata V1 BETA:\n" +
                        "  - OS Kernel: KimYo OS-Android-v3.5\n" +
                        "  - DB Gateway: SQLite Local Room database cached\n" +
                        "  - API Gateway: ${if(hasKey) "ACTIVE (Key verified)" else "MISSING (No key configured)"}\n" +
                        "  - Supabase Online Sync: ${if(isSupaOn) "ONLINE (Live synced)" else "OFFLINE (Local mode)"}"
            } else if (mainCommand == "clear") {
                _terminalConsoleLines.value = emptyList()
                return@launch
            } else if (mainCommand == "list-cells") {
                output = "Scanning ports for Double Type-C connection...\n" +
                        "[*] Detected: PORT_A -> USB-C OTG (Current host device)\n" +
                        "[*] Detected: PORT_B -> USB-C Target (Android Mobile Device, Model: SM-G998B, Status: CONNECTED_MOCK_READY)"
            } else if (mainCommand == "install-usb") {
                simulateUsbSideload()
                output = "Triggered double Type-C sideload engine. Check terminal streams above for dynamic installation telemetry logs."
            } else if (mainCommand == "/hackersincognits.supabase") {
                // Secret Command requires permissions
                if (!isAuthorized) {
                    success = false
                    output = "Access Denied. Secret Command is encrypted. Reason: User '${user?.username}' is not authorized to execute root level Supabase bindings."
                } else {
                    val supa = supabaseConfig.value
                    if (supa == null || supa.url.isEmpty() || supa.anonKey.isEmpty()) {
                        output = "[SUPABASE ENGINE] Remote link initiated.\n" +
                                "Warning: Connection configs are currently empty. Please configure Supabase URL and Anon Key in Settings screen.\n" +
                                "Terminal is primed to transmit logs once configs are added."
                    } else {
                        // Attempt connection check
                        val isOnline = SupabaseSyncManager.testConnection(supa.url, supa.anonKey)
                        configDao.saveConfig(supa.copy(isConnected = isOnline))
                        output = if (isOnline) {
                            "[SUPABASE CONNECTED ACTIVE]\n" +
                            "SUCCESS: Connected terminal successfully to Supabase backend!\n" +
                            "All terminal logging and execution histories will now automatically stream to table: 'terminal_commands'."
                        } else {
                            "[SUPABASE RETRYING]\n" +
                            "Warning: Handshake couldn't resolve Supabase API endpoint, but terminal syncing continues local queues."
                        }
                    }
                }
            } else {
                success = false
                output = "Command not recognized. Type 'help' to review authorized execution commands."
            }

            _terminalConsoleLines.update { it + ConsoleLine(output, success) }

            // Write Execution record to local database
            val cmdRecord = TerminalCommandEntity(
                command = cmdString,
                output = output,
                success = success,
                username = user?.username ?: "Offline"
            )
            terminalDao.insertCommand(cmdRecord)

            // Dynamic sync to real Supabase database if active and authorized
            if (isAuthorized) {
                val supa = supabaseConfig.value
                if (supa != null && supa.url.isNotEmpty() && supa.anonKey.isNotEmpty()) {
                    viewModelScope.launch {
                        val isSynced = SupabaseSyncManager.syncTerminalCommand(supa.url, supa.anonKey, cmdRecord)
                        if (isSynced) {
                            terminalDao.markCommandAsSynced(cmdRecord.id)
                        }
                    }
                }
            }
        }
    }

    private fun simulateUsbSideload() {
        if (_isUsbInstalling.value) return
        _isUsbInstalling.value = true
        _usbSideloadLogs.value = listOf("Initializing Dual-OTG Controller handshakes...")

        viewModelScope.launch {
            val stages = listOf(
                "Establishing Double-Type-C high-speed bridge link...",
                "Authentication handshake: ADB secure keys accepted.",
                "Sideload payload matching: Slicing KimYo5v.apk target container...",
                "Uploading APK binaries to target mobile cellular (115.2 Mbps stream)...",
                "Progress: [████████████░░░░░] 60% standard payload transferred.",
                "Progress: [█████████████████░] 95% complete.",
                "Target filesystem write successful, authorizing standard installation...",
                "Optimizing system dex assets on target device...",
                "SUCCESS: App custom launcher setup. KimYo 5V installed on target double-OTG cell!"
            )

            for (log in stages) {
                delay(1200)
                _usbSideloadLogs.update { it + log }
                _terminalConsoleLines.update { it + ConsoleLine("[USB-OTG-ENGINE] $log", true) }
            }
            _isUsbInstalling.value = false
        }
    }

    // --- Configure Supabase Remote ---
    fun saveSupabaseConfiguration(url: String, key: String) {
        viewModelScope.launch {
            val current = supabaseConfig.value ?: SupabaseConfigEntity()
            val cleanUrl = url.trim()
            val cleanKey = key.trim()

            _terminalConsoleLines.update { it + ConsoleLine("[CONFIG] Updating Supabase URL/Key link bounds...", true) }
            val isOnline = SupabaseSyncManager.testConnection(cleanUrl, cleanKey)

            val updated = SupabaseConfigEntity(
                url = cleanUrl,
                anonKey = cleanKey,
                isConnected = isOnline
            )
            configDao.saveConfig(updated)

            _terminalConsoleLines.update {
                it + ConsoleLine(
                    if (isOnline) "[SUPABASE SYNC] Remote handshake SUCCESSFUL. Remote logging enabled."
                    else "[SUPABASE SYNC] Handshake offline, caching terminal logs locally...",
                    isOnline
                )
            }
        }
    }
}

data class ConsoleLine(
    val text: String,
    val success: Boolean
)
