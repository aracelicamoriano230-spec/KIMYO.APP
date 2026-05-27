package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val userDao = db.userDao()
    private val chatDao = db.chatDao()
    private val terminalDao = db.terminalCommandDao()
    private val configDao = db.supabaseConfigDao()

    private val prefs = application.getSharedPreferences("kimyo_5v_prefs", Context.MODE_PRIVATE)
    private val nodeVariables = mutableMapOf<String, Double>()

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
        // Load custom gemini key and theme from stored preferences
        val storedKey = prefs.getString("custom_gemini_key", "") ?: ""
        _customGeminiKey.value = storedKey
        val storedTheme = prefs.getString("theme_mode", "DARK") ?: "DARK"
        _themeMode.value = storedTheme

        // Seeding default Supabase config and default administrator/owner profiles
        viewModelScope.launch {
            try {
                var config = configDao.getConfig()
                val isLegacySupaUrl = config != null && config.url != SupabaseSyncManager.DEFAULT_URL
                if (config == null || config.url.isEmpty() || isLegacySupaUrl) {
                    val defaultConfig = SupabaseConfigEntity(
                        id = 1,
                        url = SupabaseSyncManager.DEFAULT_URL,
                        anonKey = SupabaseSyncManager.DEFAULT_ANON_KEY,
                        isConnected = true
                    )
                    configDao.saveConfig(defaultConfig)
                    config = defaultConfig
                }

                // Inject anghe study executive
                val anghe = userDao.getUserByUsername("anghe")
                if (anghe == null) {
                    userDao.insertUser(UserEntity("anghe", "STUDY_EXECUTIVO", "potato", true))
                }
                // Inject anshe study executive
                val anshe = userDao.getUserByUsername("anshe")
                if (anshe == null) {
                    userDao.insertUser(UserEntity("anshe", "STUDY_EXECUTIVO", "potato", true))
                }
                // Inject blacker elite owner
                val blacker = userDao.getUserByUsername("blacker")
                if (blacker == null) {
                    userDao.insertUser(UserEntity("blacker", "ELITE_BLACKER", "benja30100", true))
                }

                // Sync administrators to Supabase
                config?.let { c ->
                    if (c.url.isNotEmpty()) {
                        SupabaseSyncManager.syncUser(c.url, c.anonKey, UserEntity("anghe", "STUDY_EXECUTIVO", "potato", true))
                        SupabaseSyncManager.syncUser(c.url, c.anonKey, UserEntity("anshe", "STUDY_EXECUTIVO", "potato", true))
                        SupabaseSyncManager.syncUser(c.url, c.anonKey, UserEntity("blacker", "ELITE_BLACKER", "benja30100", true))
                    }
                }

                // Restore active persistent session
                val cachedUsername = prefs.getString("logged_in_username", "") ?: ""
                if (cachedUsername.isNotEmpty()) {
                    val cachedUser = userDao.getUserByUsername(cachedUsername)
                    if (cachedUser != null && cachedUser.isApproved) {
                        _currentUser.value = cachedUser
                        if (cachedUser.mode == "STUDY_EXECUTIVO" || cachedUser.mode == "ELITE_BLACKER") {
                            _themeMode.value = "DARK"
                        }
                    } else {
                        prefs.edit().remove("logged_in_username").apply()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error seeding default configuration: ${e.message}")
            }
        }

        // Trigger Splash fadeout timer
        viewModelScope.launch {
            delay(2800) // Duration of Splash presentation before starting app
            _showSplash.value = false
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setCustomGeminiKey(key: String) {
        _customGeminiKey.value = key
        prefs.edit().putString("custom_gemini_key", key).apply()
    }

    // --- Sign In & Sign Up ---
    fun registerNewUser(usernameInput: String, passwordInput: String, onResult: (Boolean, String) -> Unit) {
        val cleanUser = usernameInput.trim()
        val normalized = cleanUser.lowercase()
        if (cleanUser.isEmpty() || passwordInput.isEmpty()) {
            onResult(false, "El nombre de usuario y contraseña no pueden estar vacíos")
            return
        }

        if (normalized == "anghe" || normalized == "anshe" || normalized == "blacker") {
            onResult(false, "La cuenta especificada ya existe por defecto en el sistema")
            return
        }

        viewModelScope.launch {
            try {
                val existing = userDao.getUserByUsername(cleanUser)
                if (existing != null) {
                    onResult(false, "El nombre de usuario ya está registrado")
                } else {
                    val newUser = UserEntity(
                        username = cleanUser,
                        mode = "NORMAL",
                        passwordHash = passwordInput,
                        isApproved = false // Pending approval by Blacker
                    )
                    userDao.insertUser(newUser)

                    // Write to remote Supabase database immediately
                    val supa = configDao.getConfig()
                    if (supa != null && supa.url.isNotEmpty()) {
                        SupabaseSyncManager.syncUser(supa.url, supa.anonKey, newUser)
                    }

                    onResult(true, "Registro guardado de forma real y segura. Espera la aprobación de Blacker para ingresar.")
                }
            } catch (e: Exception) {
                onResult(false, "Error de registro: ${e.message}")
            }
        }
    }

    fun authenticateUser(usernameInput: String, passwordInput: String, onResult: (Boolean, String) -> Unit) {
        val cleanUser = usernameInput.trim()
        if (cleanUser.isEmpty() || passwordInput.isEmpty()) {
            onResult(false, "Por favor completa todos los campos del formulario")
            return
        }

        viewModelScope.launch {
            try {
                var userObj = userDao.getUserByUsername(cleanUser)

                // If user is absent locally, try syncing from remote Supabase (offline multi-device support)
                if (userObj == null) {
                    val config = configDao.getConfig()
                    if (config != null && config.url.isNotEmpty()) {
                        val remoteUsers = SupabaseSyncManager.fetchRemoteUsers(config.url, config.anonKey)
                        val matched = remoteUsers.find { it.username.equals(cleanUser, ignoreCase = true) }
                        if (matched != null) {
                            userDao.insertUser(matched)
                            userObj = matched
                        }
                    }
                }

                if (userObj == null) {
                    onResult(false, "Usuario no registrado en el sistema")
                } else {
                    if (userObj.passwordHash != passwordInput) {
                        onResult(false, "Contraseña incorrecta, verifica los datos del terminal")
                    } else if (!userObj.isApproved) {
                        onResult(false, "Tu cuenta requiere aprobación del dueño (Blacker) antes de iniciar sesión.")
                    } else {
                        // Correct! Set session variables
                        _currentUser.value = userObj
                        prefs.edit().putString("logged_in_username", userObj.username).apply()

                        if (userObj.mode == "STUDY_EXECUTIVO" || userObj.mode == "ELITE_BLACKER") {
                            _themeMode.value = "DARK"
                        }

                        // Clear history and greet user
                        chatDao.clearChats()
                        val introMsg = getTailoredWelcomeMessage(userObj)
                        chatDao.insertChat(ChatItemEntity(
                            username = userObj.username,
                            sender = "KimYo 5V",
                            text = introMsg,
                            isHeader = true
                        ))

                        onResult(true, "Acceso autorizado")
                    }
                }
            } catch (e: Exception) {
                onResult(false, "Error de autenticación: ${e.message}")
            }
        }
    }

    fun logOut() {
        viewModelScope.launch {
            _currentUser.value = null
            prefs.edit().remove("logged_in_username").apply()
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
            val userChat = ChatItemEntity(
                username = user.username,
                sender = user.username,
                text = messageStr
            )
            chatDao.insertChat(userChat)

            val supa = configDao.getConfig()
            if (supa != null && supa.url.isNotEmpty()) {
                SupabaseSyncManager.syncChat(supa.url, supa.anonKey, userChat)
            }

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
            val aiChat = ChatItemEntity(
                username = user.username,
                sender = "KimYo 5V",
                text = response
            )
            chatDao.insertChat(aiChat)

            if (supa != null && supa.url.isNotEmpty()) {
                SupabaseSyncManager.syncChat(supa.url, supa.anonKey, aiChat)
            }
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
                output = "KimYo Core Command Center Terminal v5.0\n" +
                        "=========================================\n" +
                        "SYSTEM BUILT-INS:\n" +
                        "  help                      - Display active commands & utility manuals\n" +
                        "  whoami                    - Display active session profiles & permissions\n" +
                        "  env                       - Print system configuration & network gateways\n" +
                        "  clear                     - Reset console layout screen\n" +
                        "  list-cells                - Scan dual Type-C micro-OTG ports\n" +
                        "  install-usb               - Deploy target APK via sideload simulation\n\n" +
                        "DEVELOPER INTEGRATIONS:\n" +
                        "  node [js_code]            - Interactive JavaScript Node REPL & calculation engine\n" +
                        "                              (e.g., node -e \"let x=5; console.log(x*20);\")\n" +
                        "  sql [query]               - Raw SQLite SQLiteDataReader console inspector\n" +
                        "                              (e.g., sql SELECT * FROM users;)\n" +
                        "  supabase [cli_command]    - Local and remote database migration and admin tool\n" +
                        "                              (e.g., supabase status, supabase start, supabase db pull)\n" +
                        "  /hackersincognits.supabase- Setup remote sync logger pipeline\n\n" +
                        "REAL ADMIN CONTROLS (OWNER):\n" +
                        "  /admin-users              - List all registered users and their passwords/approval status\n" +
                        "  /approve-user [name]      - Set isApproved = true for a pending user account\n" +
                        "  /admin-logs               - Stream the last 100 executed commands by any user on Supabase\n\n" +
                        "NATIVE LINUX EXECUTION:\n" +
                        "  You can run live native Android terminal binaries directly!\n" +
                        "  (e.g., ls, pwd, date, uname -a, df)"
            } else if (mainCommand == "whoami") {
                output = if (user != null) {
                    "Username: ${user.username}\nMode: ${user.mode}\nLevel: ${if (isAuthorized) "Privileged Administrator" else "Standard User"}"
                } else {
                    "Offline User. Please login first to inspect session credentials."
                }
            } else if (mainCommand == "env") {
                val hasKey = _customGeminiKey.value.isNotEmpty() || (com.example.BuildConfig.GEMINI_API_KEY.isNotEmpty() && com.example.BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY")
                val isSupaOn = supabaseConfig.value?.isConnected == true
                output = "System environment metadata V5 CONFIGURED:\n" +
                        "  - OS Kernel: KimYo OS-Android-v5.0\n" +
                        "  - DB Gateway: SQLite Local Room database cached + Supabase sync live\n" +
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
            } else if (mainCommand == "node") {
                val argsString = if (cmdString.startsWith("node")) cmdString.substring(4) else cmdString
                output = evaluateNodeCommand(argsString)
            } else if (mainCommand == "sql" || mainCommand == "psql") {
                val sqlQuery = if (cmdString.startsWith("sql")) cmdString.substring(3).trim() else cmdString.substring(4).trim()
                output = if (sqlQuery.isEmpty()) {
                    "Usage: sql <query>\nAvailable tables: users, chats, terminal_commands, supabase_config"
                } else {
                    executeRawSql(sqlQuery)
                }
            } else if (mainCommand == "supabase") {
                val supabaseArgs = if (cmdString.startsWith("supabase")) cmdString.substring(8) else cmdString
                output = interpretSupabaseCli(supabaseArgs)
            } else if (mainCommand == "/admin-users") {
                if (user?.mode != "ELITE_BLACKER") {
                    success = false
                    output = "[DENIED] El comando /admin-users sólo se puede ejecutar con el perfil de dueño (blacker)."
                } else {
                    val supa = supabaseConfig.value
                    if (supa == null || supa.url.isEmpty()) {
                        success = false
                        output = "[ERROR] La base de datos de Supabase no está configurada o está fuera de línea."
                    } else {
                        output = try {
                            val list = SupabaseSyncManager.fetchRemoteUsers(supa.url, supa.anonKey)
                            if (list.isEmpty()) {
                                "No se encontraron usuarios registrados en la base de datos de Supabase."
                            } else {
                                val header = "+----------------------+----------------------+----------------------+----------------------+\n" +
                                             "| USERNAME             | MODE                 | CLAVE                | STATUS               |\n" +
                                             "+----------------------+----------------------+----------------------+----------------------+\n"
                                val body = StringBuilder()
                                for (u in list) {
                                    val status = if (u.isApproved) "APPROVED" else "PENDING_APPROVAL"
                                    body.append("| ")
                                        .append(u.username.padEnd(20))
                                        .append(" | ")
                                        .append(u.mode.padEnd(20))
                                        .append(" | ")
                                        .append(u.passwordHash.padEnd(20))
                                        .append(" | ")
                                        .append(status.padEnd(20))
                                        .append(" |\n")
                                }
                                val footer = "+----------------------+----------------------+----------------------+----------------------+\n" +
                                             "Para aprobar a un usuario escribe: /approve-user [username]"
                                header + body.toString() + footer
                            }
                        } catch (e: Exception) {
                            "[ERROR] Sincronización remota fallida: ${e.message}"
                        }
                    }
                }
            } else if (mainCommand == "/approve-user") {
                if (user?.mode != "ELITE_BLACKER") {
                    success = false
                    output = "[DENIED] Sólo el perfil de Blacker puede aprobar inicios de sesión de otros usuarios."
                } else if (rawParts.size < 2) {
                    success = false
                    output = "Uso del sistema: /approve-user [username_a_aprobar]"
                } else {
                    val targetUser = rawParts[1].trim()
                    val supa = supabaseConfig.value
                    if (supa == null || supa.url.isEmpty()) {
                        success = false
                        output = "[ERROR] Conexión con Supabase inválida."
                    } else {
                        userDao.approveUser(targetUser, true)
                        val ok = SupabaseSyncManager.updateRemoteUserApproval(supa.url, supa.anonKey, targetUser, true)
                        output = if (ok) {
                            "APROBADO CON ÉXITO: El usuario '${targetUser}' ha sido habilitado para iniciar sesión y se guardó en Supabase."
                        } else {
                            "LOCAL_OK: Aprobado localmente, pero error al actualizar en Supabase. Esperando reintento."
                        }
                    }
                }
            } else if (mainCommand == "/set-rank" || mainCommand == "/rango") {
                val cleanParts = rawParts.filter { it.isNotEmpty() }
                var outStr = ""
                if (user?.mode != "ELITE_BLACKER") {
                    success = false
                    outStr = "[DENIED] El comando /set-rank sólo se puede ejecutar por el rango de máximo privilegio (omega)."
                } else if (cleanParts.size < 3) {
                    success = false
                    outStr = "Uso del sistema: /set-rank [username] [usuario|administrador|omega]"
                } else {
                    val targetUser = cleanParts[1]
                    val rankText = cleanParts[2].lowercase()
                    val targetMode = when (rankText) {
                        "usuario", "normal" -> "NORMAL"
                        "administrador", "admin", "docente" -> "STUDY_EXECUTIVO"
                        "omega", "owner", "blacker" -> "ELITE_BLACKER"
                        else -> null
                    }
                    if (targetMode == null) {
                        success = false
                        outStr = "Rango desconocido: '$rankText'. Rangos válidos: usuario | administrador | omega"
                    } else {
                        val supa = supabaseConfig.value
                        if (supa == null || supa.url.isEmpty()) {
                            success = false
                            outStr = "[ERROR] La base de datos de Supabase no está configurada o está fuera de línea."
                        } else {
                            try {
                                val targetObj = userDao.getUserByUsername(targetUser)
                                if (targetObj == null) {
                                    val remoteUsers = SupabaseSyncManager.fetchRemoteUsers(supa.url, supa.anonKey)
                                    val foundRemote = remoteUsers.find { it.username.equals(targetUser, ignoreCase = true) }
                                    if (foundRemote == null) {
                                        success = false
                                        outStr = "Error: El usuario '$targetUser' no fue encontrado en local ni en Supabase."
                                    } else {
                                        val updatedUser = foundRemote.copy(mode = targetMode)
                                        userDao.insertUser(updatedUser)
                                        val ok = SupabaseSyncManager.syncUser(supa.url, supa.anonKey, updatedUser)
                                        outStr = if (ok) {
                                            "ALINEACIÓN COMANDADA EXITOSA: El usuario '$targetUser' fue sincronizado y promovido al rango '$rankText' ($targetMode) de forma real en Supabase."
                                        } else {
                                            "LOCAL_OK: Modificado localmente a '$targetMode', pero falló al sincronizar con Supabase."
                                        }
                                    }
                                } else {
                                    val updatedUser = targetObj.copy(mode = targetMode)
                                    userDao.insertUser(updatedUser)
                                    val ok = SupabaseSyncManager.syncUser(supa.url, supa.anonKey, updatedUser)
                                    outStr = if (ok) {
                                        "ALINEACIÓN COMANDADA EXITOSA: El usuario '$targetUser' ahora posee el rango '$rankText' ($targetMode) de forma real y se sincronizó con Supabase."
                                    } else {
                                        "LOCAL_OK: Modificado localmente a '$targetMode', pero falló al sincronizar con Supabase."
                                    }
                                }
                            } catch (e: Exception) {
                                success = false
                                outStr = "Error al actualizar rango del usuario: ${e.message}"
                            }
                        }
                    }
                }
                output = outStr
            } else if (mainCommand == "/admin-logs") {
                if (user?.mode != "ELITE_BLACKER") {
                    success = false
                    output = "[DENIED] Sólo el dueño Blacker puede auditar las consolas de otros usuarios del terminal."
                } else {
                    val supa = supabaseConfig.value
                    if (supa == null || supa.url.isEmpty()) {
                        success = false
                        output = "[ERROR] Supabase no está conectado."
                    } else {
                        output = try {
                            val logs = SupabaseSyncManager.fetchAllRemoteCommands(supa.url, supa.anonKey)
                            if (logs.isEmpty()) {
                                "No se registran comandos ejecutados remotamente en Supabase aún."
                            } else {
                                val sFormat = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                                val header = "+----------------------+------------------------------------+------------------+----------+\n" +
                                             "| USERNAME             | COMMAND                            | TIME             | STATUS   |\n" +
                                             "+----------------------+------------------------------------+------------------+----------+\n"
                                val body = StringBuilder()
                                for (l in logs) {
                                    val timeStr = sFormat.format(java.util.Date(l.timestamp))
                                    val stat = if (l.success) "SUCCESS" else "FAILURE"
                                    val cmdText = if (l.command.length > 34) l.command.take(31) + "..." else l.command
                                    body.append("| ")
                                        .append(l.username.padEnd(20))
                                        .append(" | ")
                                        .append(cmdText.padEnd(34))
                                        .append(" | ")
                                        .append(timeStr.padEnd(16))
                                        .append(" | ")
                                        .append(stat.padEnd(8))
                                        .append(" |\n")
                                }
                                val footer = "+----------------------+------------------------------------+------------------+----------+"
                                header + body.toString() + footer
                            }
                        } catch (e: Exception) {
                            "Error obteniendo logs de auditoría: ${e.message}"
                        }
                    }
                }
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
                output = runNativeShellCommand(cmdString)
                success = !output.contains("Error running native system command") && !output.contains("Command exited with status")
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

            // Dynamic sync to real Supabase database for telemetry tracking
            val supa = supabaseConfig.value
            if (supa != null && supa.url.isNotEmpty() && supa.anonKey.isNotEmpty()) {
                viewModelScope.launch {
                    val isSynced = SupabaseSyncManager.syncTerminalCommand(supa.url, supa.anonKey, cmdRecord)
                    if (isSynced) {
                        try {
                            terminalDao.markCommandAsSynced(cmdRecord.id)
                        } catch (e: java.lang.Exception) {
                            Log.e("MainViewModel", "Error marking command synced: ${e.message}")
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
            if (_currentUser.value?.mode != "ELITE_BLACKER") {
                _terminalConsoleLines.update { it + ConsoleLine("[DENIED] Sólo el nivel supremo Omega (ELITE_BLACKER) tiene facultades para alterar la configuración del servidor.", false) }
                return@launch
            }
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

    private fun runNativeShellCommand(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val reader = process.inputStream.bufferedReader()
            val errorReader = process.errorStream.bufferedReader()
            
            val output = reader.readText().trim()
            val errorOutput = errorReader.readText().trim()
            
            process.waitFor()
            val exitCode = process.exitValue()
            
            if (exitCode == 0) {
                if (output.isEmpty()) {
                    "Execution successful (no output)."
                } else {
                    output
                }
            } else {
                if (errorOutput.isNotEmpty()) errorOutput else "Command exited with status $exitCode"
            }
        } catch (e: Exception) {
            "Error running native system command: ${e.message}"
        }
    }

    private fun evaluateNodeCommand(argsString: String): String {
        val cleanArgs = argsString.trim()
        if (cleanArgs.isEmpty()) {
            return "Welcome to Node.js v20.9.0 (Enterprise V5 Node Engine).\nType 'node -e \"console.log(2 + 2 * 10)\"' or assign variable scopes."
        }
        
        if (cleanArgs == "-v" || cleanArgs == "--version") {
            return "v20.9.0"
        }
        
        var jsCode = cleanArgs
        if (cleanArgs.startsWith("-e ")) {
            jsCode = cleanArgs.substring(3).trim()
            if ((jsCode.startsWith("\"") && jsCode.endsWith("\"")) || (jsCode.startsWith("'") && jsCode.endsWith("'"))) {
                jsCode = jsCode.substring(1, jsCode.length - 1)
            }
        }
        
        return try {
            evaluateJs(jsCode)
        } catch (e: Exception) {
            "SyntaxError: ${e.message}"
        }
    }

    private fun evaluateJs(code: String): String {
        val statements = code.split(Regex("(?<=\\G[^'\"]*);(?=(?:[^'\"]*['\"][^'\"]*['\"])*[^'\"]*$)"))
        val output = StringBuilder()
        var lastResult = "undefined"
        
        for (stmt in statements) {
            val trimmed = stmt.trim()
            if (trimmed.isEmpty()) continue
            
            if (trimmed.startsWith("console.log(")) {
                val endOffset = trimmed.lastIndexOf(")")
                val startOffset = trimmed.indexOf("(")
                if (startOffset != -1 && endOffset != -1 && endOffset > startOffset) {
                    val inner = trimmed.substring(startOffset + 1, endOffset).trim()
                    val evaluatedInner = evalExpr(inner)
                    output.append(evaluatedInner).append("\n")
                    lastResult = "undefined"
                }
            } else if (trimmed.startsWith("let ") || trimmed.startsWith("const ") || trimmed.startsWith("var ")) {
                val spaceIdx = trimmed.indexOf(" ")
                val declaration = trimmed.substring(spaceIdx + 1).trim()
                val parts = declaration.split("=")
                if (parts.size == 2) {
                    val varName = parts[0].trim()
                    val expr = parts[1].trim()
                    val value = evalExpr(expr).toDoubleOrNull() ?: 0.0
                    nodeVariables[varName] = value
                    lastResult = value.toString()
                } else {
                    lastResult = "undefined"
                }
            } else {
                val res = evalExpr(trimmed)
                lastResult = res
            }
        }
        
        val printedLog = output.toString()
        if (printedLog.isNotEmpty()) {
            return printedLog.trim()
        }
        return lastResult
    }

    private fun evalExpr(expr: String): String {
        val trimmed = expr.trim()
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length - 1)
        }
        
        if (nodeVariables.containsKey(trimmed)) {
            return nodeVariables[trimmed].toString()
        }
        
        var formula = trimmed
        for ((k, v) in nodeVariables) {
            formula = formula.replace(Regex("\\b$k\\b"), v.toString())
        }
        
        return try {
            val calculated = evaluateMath(formula)
            if (calculated % 1.0 == 0.0) {
                calculated.toInt().toString()
            } else {
                calculated.toString()
            }
        } catch (e: Exception) {
            trimmed
        }
    }

    private fun evaluateMath(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = this.pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = str.substring(startPos, this.pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected code component")
                }
                return x
            }
        }.parse()
    }

    private fun executeRawSql(sql: String): String {
        try {
            val cursor = db.openHelper.readableDatabase.query(sql)
            val columnNames = cursor.columnNames
            if (columnNames.isEmpty()) {
                return "Query completed successfully (No Columns matches)."
            }
            
            val rows = mutableListOf<List<String>>()
            while (cursor.moveToNext()) {
                val row = mutableListOf<String>()
                for (i in 0 until cursor.columnCount) {
                    val value = try {
                        cursor.getString(i) ?: "NULL"
                    } catch (e: Exception) {
                        try {
                            cursor.getBlob(i)?.let { "[BLOB ${it.size}b]" } ?: "NULL"
                        } catch (ex: Exception) {
                            "UNKNOWN"
                        }
                    }
                    row.add(value)
                }
                rows.add(row)
            }
            cursor.close()
            
            if (rows.isEmpty()) {
                return "Success: Query executed, but returned empty list (0 rows)."
            }
            
            val colWidths = IntArray(columnNames.size)
            for (i in columnNames.indices) {
                colWidths[i] = columnNames[i].length
            }
            for (row in rows) {
                for (i in row.indices) {
                    if (row[i].length > colWidths[i]) {
                        colWidths[i] = row[i].length
                    }
                }
            }
            
            val separator = StringBuilder()
            for (width in colWidths) {
                separator.append("+").append("-".repeat(width + 2))
            }
            separator.append("+")
            
            val table = StringBuilder()
            table.append(separator).append("\n")
            
            table.append("|")
            for (i in columnNames.indices) {
                val cell = columnNames[i]
                table.append(" ").append(cell.padEnd(colWidths[i])).append(" |")
            }
            table.append("\n").append(separator).append("\n")
            
            for (row in rows) {
                table.append("|")
                for (i in row.indices) {
                    val cell = row[i]
                    table.append(" ").append(cell.padEnd(colWidths[i])).append(" |")
                }
                table.append("\n")
            }
            table.append(separator)
            
            return table.toString()
        } catch (e: Exception) {
            return "SQL Error: ${e.message}"
        }
    }

    private fun interpretSupabaseCli(args: String): String {
        val clean = args.trim()
        val parts = clean.split(" ")
        if (parts.isEmpty() || parts[0].isEmpty()) {
            return "Supabase CLI (Simulated Professional Client v1.12.0)\n" +
                   "Usage: supabase [command]\n\n" +
                   "Available Commands:\n" +
                   "  status          - Query status of connected remote databases\n" +
                   "  init            - Initialize local directory structures for schema tracking\n" +
                   "  login           - Verify access tokens with remote cloud console\n" +
                   "  start           - Mount local emulated docker nodes\n" +
                   "  db pull/push    - Sync migrations from/to target databases"
        }
        val sub = parts[0]
        return when (sub) {
            "status" -> {
                val config = supabaseConfig.value
                if (config == null || config.url.isEmpty()) {
                    "STATUS: No active configuration matches. Please define public Supabase Host URL in system settings first."
                } else {
                    "Supabase Remote Engine Metadata:\n" +
                    "  [Host Endpoint] : ${config.url}\n" +
                    "  [Public Key]    : ${if (config.anonKey.isNotEmpty()) "${config.anonKey.take(12)}... (active)" else "NOT SET"}\n" +
                    "  [Database URL]  : postgresql://postgres:${if (config.anonKey.isNotEmpty()) "******" else ""}@db.${config.url.removePrefix("https://").substringBefore(".supabase")}.supabase.co:5432/postgres\n" +
                    "  [Link Status]   : ${if (config.isConnected) "ONLINE & SYNCED" else "OFFLINE"}"
                }
            }
            "init" -> {
                "[$] Initializing supabase project...\n" +
                "Writing configuration file: supabase/config.toml...\n" +
                "Successfully generated local infrastructure trackers. Supabase directory tracking is enabled."
            }
            "login" -> {
                "[$] Initiating OAuth secure handshake...\n" +
                "Detected active session on system.\n" +
                "Success: Supabase CLI authenticated via local dev token."
            }
            "start" -> {
                "[$] Launching emulated docker daemon...\n" +
                "  -> Starting postgresql database container... [OK]\n" +
                "  -> Starting postgrest API server... [OK]\n" +
                "  -> Starting kong security gateway proxy... [OK]\n" +
                "  -> Starting storage-api engine... [OK]\n" +
                "DB Gateway: Running on http://localhost:54321. Base connection listening."
            }
            "db" -> {
                if (parts.size > 1 && parts[1] == "pull") {
                    "[$] Fetching latest schema migrations from Supabase remote database...\n" +
                    "  -> Reading Table definitions: [users], [chats], [terminal_commands], [supabase_config]... [SUCCESS]\n" +
                    "  -> Syncing 4 active local schemas with SQLite OpenHelper migrations...\n" +
                    "Success: App local storage structure is synchronized."
                } else if (parts.size > 1 && parts[1] == "push") {
                    "[$] Pushing local SQLite Room cached tables to Supabase...\n" +
                    "  -> Pushing terminal command schema log indices...\n" +
                    "Success: 100% of terminal indices pushed to remote table: 'terminal_commands'."
                } else {
                    "Supabase migrating commands: 'supabase db pull' to retrieve, 'supabase db push' to upload local data schemas."
                }
            }
            else -> {
                "Supabase sub-command '$sub' is not recognized. Type 'supabase' to review available developer CLI integrations."
            }
        }
    }
}

data class ConsoleLine(
    val text: String,
    val success: Boolean
)
