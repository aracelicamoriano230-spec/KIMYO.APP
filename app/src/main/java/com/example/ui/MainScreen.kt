package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ChatItemEntity
import com.example.data.UserEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val showSplash by viewModel.showSplash.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        if (showSplash) {
            SplashScreenView()
        } else {
            Crossfade(targetState = currentUser, label = "ScreenTransition") { user ->
                if (user == null) {
                    LoginView(viewModel = viewModel)
                } else {
                    AppHubView(viewModel = viewModel, user = user)
                }
            }
        }
    }
}

// --- Dynamic Splash Presentation View ---
@Composable
fun SplashScreenView() {
    var alphaVal by remember { mutableStateOf(0f) }
    var textAlphaVal by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(1200, easing = FastOutSlowInEasing)
        ) { value, _ -> alphaVal = value }
        
        delay(300)
        
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(900, easing = LinearOutSlowInEasing)
        ) { value, _ -> textAlphaVal = value }

        delay(800)

        // Slowly fading away before transitioning
        animate(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = tween(500, easing = FastOutLinearInEasing)
        ) { value, _ ->
            alphaVal = value
            textAlphaVal = value
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B13))
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_anonymous_mask),
                contentDescription = "Anonymous Mask Logo",
                modifier = Modifier
                    .size(160.dp)
                    .alpha(alphaVal)
                    .padding(8.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "HACKERS COMPANYS",
                color = HackerGreen,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier
                    .alpha(textAlphaVal)
                    .testTag("splash_brand_text")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "KimYo 5V - Advanced Cybernetics Node",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.alpha(textAlphaVal)
            )
        }
    }
}

// --- Login & Registration View ---
@Composable
fun LoginView(viewModel: MainViewModel) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    val isReady = if (isRegisterMode) {
        usernameInput.trim().isNotEmpty() && passwordInput.isNotEmpty() && confirmPasswordInput.isNotEmpty()
    } else {
        usernameInput.trim().isNotEmpty() && passwordInput.isNotEmpty()
    }

    var showAnonymousEye by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            showAnonymousEye = true
            delay(1500)
            showAnonymousEye = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF090D1A), Color(0xFF14192B))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Animated hacker face graphic container
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, HackerGreen.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = showAnonymousEye, label = "HackerFaceTransition") { eyeState ->
                    if (eyeState) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_anonymous_mask),
                            contentDescription = "Anonymous Mask Logo blinking",
                            modifier = Modifier.size(65.dp),
                            colorFilter = ColorFilter.tint(HackerGreen)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_anonymous_mask),
                            contentDescription = "Anonymous Mask Logo standard",
                            modifier = Modifier.size(65.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "KIMYO 5V ENGINE",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Secure Autonomous Network & Study Gateway",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Tabs for toggling Register / Login
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { 
                        isRegisterMode = false 
                        errorMessage = ""
                        successMessage = ""
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isRegisterMode) HackerGreen else Color.Transparent,
                        contentColor = if (!isRegisterMode) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Button(
                    onClick = { 
                        isRegisterMode = true 
                        errorMessage = ""
                        successMessage = ""
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRegisterMode) HackerGreen else Color.Transparent,
                        contentColor = if (isRegisterMode) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("REGISTRARSE", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Input fields with hacker accent
            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { Text("Nombre de Registro / Username", color = Color.White.copy(alpha = 0.6f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HackerGreen,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = HackerGreen,
                    cursorColor = HackerGreen,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("name_login_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Contraseña", color = Color.White.copy(alpha = 0.6f)) },
                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HackerGreen,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = HackerGreen,
                    cursorColor = HackerGreen,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (isRegisterMode) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPasswordInput,
                    onValueChange = { confirmPasswordInput = it },
                    label = { Text("Confirmar Contraseña", color = Color.White.copy(alpha = 0.6f)) },
                    visualTransformation = if (confirmPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle confirm password visibility",
                                tint = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HackerGreen,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = HackerGreen,
                        cursorColor = HackerGreen,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error Status and success feedback cards
            if (errorMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF421E1E)),
                    border = BorderStroke(1.dp, Color(0xFFE57373)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFFCDD2),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (successMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3821)),
                    border = BorderStroke(1.dp, HackerGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = successMessage,
                        color = HackerGreen,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Credential hints for system operators
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚡ Perfiles Predeterminados de Administración:",
                        color = HackerGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• anghe / anshe - Administradora (Clave: potato) violeta, caballos & conejos.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• blacker - Creador de élite / Dueño (Clave: benja30100) accesos raíz.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val infiniteTransition = rememberInfiniteTransition(label = "ButtonPulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.04f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "PulseScale"
            )

            Button(
                onClick = {
                    errorMessage = ""
                    successMessage = ""
                    if (isRegisterMode) {
                        if (passwordInput != confirmPasswordInput) {
                            errorMessage = "Las contraseñas ingresadas no coinciden"
                            return@Button
                        }
                        viewModel.registerNewUser(usernameInput, passwordInput) { ok, msg ->
                            if (ok) {
                                successMessage = msg
                                usernameInput = ""
                                passwordInput = ""
                                confirmPasswordInput = ""
                                isRegisterMode = false
                            } else {
                                errorMessage = msg
                            }
                        }
                    } else {
                        viewModel.authenticateUser(usernameInput, passwordInput) { ok, msg ->
                            if (!ok) {
                                errorMessage = msg
                            }
                        }
                    }
                },
                enabled = isReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .graphicsLayer {
                        if (isReady) {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                    }
                    .testTag("login_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HackerGreen,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.12f),
                    disabledContentColor = Color.White.copy(alpha = 0.38f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isRegisterMode) "CREAR CUENTA SECURA" else "AUTENTICAR AL SISTEMA",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// --- Main Application Viewport Hub ---
@Composable
fun AppHubView(viewModel: MainViewModel, user: UserEntity) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    // Determine Theme scheme based on user type & settings override
    val rawPalette = when {
        themeMode == "LIGHT" -> LightColorScheme
        user.mode == "STUDY_EXECUTIVO" -> darkColorScheme(
            primary = AnsheViolet,
            secondary = PurpleGrey80,
            tertiary = HackerGreen,
            background = AnsheBg,
            surface = Color(0xFF1B162C),
            onBackground = Color.White,
            onSurface = Color.White
        )
        user.mode == "ELITE_BLACKER" -> darkColorScheme(
            primary = BlackerGold,
            secondary = PurpleGrey80,
            tertiary = HackerGreen,
            background = BlackerBg,
            surface = Color(0xFF121212),
            onBackground = Color.White,
            onSurface = Color.White
        )
        themeMode == "HACKER" -> darkColorScheme(
            primary = HackerGreen,
            secondary = HackerGreen,
            tertiary = HackerGreen,
            background = TerminalBg,
            surface = Color(0xFF0F151B),
            onBackground = HackerGreen,
            onSurface = HackerGreen
        )
        else -> darkColorScheme( // Standard Dark Mode
            primary = AccentBlue,
            secondary = PurpleGrey80,
            tertiary = Pink80,
            background = DarkBackground,
            surface = Color(0xFF151B2E),
            onBackground = Color.White,
            onSurface = Color.White
        )
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: AI Chat, 1: Shell Terminal, 2: System Settings

    MaterialTheme(colorScheme = rawPalette) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("KimYo 5V AI", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        icon = {
                            Icon(
                                imageVector = if (user.mode == "STUDY_EXECUTIVO") Icons.Filled.Pets else Icons.Filled.SmartToy,
                                contentDescription = "Active chat node"
                            )
                        },
                        modifier = Modifier.testTag("tab_ai_chat")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("Terminal", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Terminal,
                                contentDescription = "Hacker CLI execution"
                            )
                        },
                        modifier = Modifier.testTag("tab_terminal")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        label = { Text("Configuración", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "System tuning options"
                            )
                        },
                        modifier = Modifier.testTag("tab_settings")
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                    },
                    label = "TabTransition"
                ) { targetValue ->
                    when (targetValue) {
                        0 -> ChatViewTab(viewModel = viewModel, user = user)
                        1 -> TerminalViewTab(viewModel = viewModel, user = user)
                        2 -> SettingsViewTab(viewModel = viewModel, user = user)
                    }
                }
            }
        }
    }
}

// --- Tab 1: Customized KimYo AI Chat View ---
@Composable
fun ChatViewTab(viewModel: MainViewModel, user: UserEntity) {
    val chats by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isAiGenerating.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var rawTextQuery by remember { mutableStateOf("") }

    LaunchedEffect(chats.size) {
        if (chats.isNotEmpty()) {
            listState.animateScrollToItem(chats.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Dynamic decorative header with custom icons depending on mode
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(14.dp)
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokeWidth
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_anonymous_mask),
                            contentDescription = "KimYo Face",
                            modifier = Modifier.size(28.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "KimYo 5V",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(HackerGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Advanced AI (BETA V1)",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Executive horses and rabbit decorative vectors for Anshe / Anghe
                if (user.mode == "STUDY_EXECUTIVO") {
                    Row(
                        modifier = Modifier
                            .background(AnsheViolet.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🐰", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🐴", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Estudio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = AnsheViolet,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else if (user.mode == "ELITE_BLACKER") {
                    Row(
                        modifier = Modifier
                            .background(BlackerGold.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔱", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Señor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = BlackerGold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Main chats stream list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chats) { item ->
                ChatBubble(item = item, currentUsername = user.username)
            }

            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "KimYo 5V está procesando...",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Text query entry box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(12.dp)
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = strokeWidth
                    )
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = rawTextQuery,
                    onValueChange = { rawTextQuery = it },
                    placeholder = {
                        Text(
                            text = if (user.mode == "STUDY_EXECUTIVO") "Haz tu consulta escolar, Anshe... 🐰" else "Pregúntale a KimYo 5V...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.15f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (rawTextQuery.trim().isNotEmpty()) {
                            viewModel.sendChatMessage(rawTextQuery)
                            rawTextQuery = ""
                        }
                    })
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (rawTextQuery.trim().isNotEmpty()) {
                            viewModel.sendChatMessage(rawTextQuery)
                            rawTextQuery = ""
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = if (user.mode == "ELITE_BLACKER") Color.Black else Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_chat_fab")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Transmit signal icon",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(item: ChatItemEntity, currentUsername: String) {
    val isSelf = item.sender == currentUsername
    val alignment = if (isSelf) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
        ) {
            Text(
                text = item.sender,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isSelf) MaterialTheme.colorScheme.primary else HackerGreen,
                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .background(
                    color = if (isSelf) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isSelf) 16.dp else 0.dp,
                        bottomEnd = if (isSelf) 0.dp else 16.dp
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isSelf) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isSelf) 16.dp else 0.dp,
                        bottomEnd = if (isSelf) 0.dp else 16.dp
                    )
                )
                .padding(14.dp)
                .widthIn(max = 290.dp)
        ) {
            Column {
                if (item.isHeader && currentUsername.lowercase() in listOf("anshe", "anghe")) {
                    // Rabbit Horse Animation inside the Anshe's chat intro
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .background(AnsheViolet.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🐰 ✨ 🐴 ESTUDIANTE VIP 🐴 ✨ 🐰", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AnsheViolet)
                    }
                }

                Text(
                    text = item.text,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// --- Tab 2: Hacking Terminal section ---
@Composable
fun TerminalViewTab(viewModel: MainViewModel, user: UserEntity) {
    val logs by viewModel.terminalConsoleLines.collectAsStateWithLifecycle()
    val usbLogs by viewModel.usbSideloadLogs.collectAsStateWithLifecycle()
    val isInstalling by viewModel.isUsbInstalling.collectAsStateWithLifecycle()
    val isSupaOn by viewModel.supabaseConfig.collectAsStateWithLifecycle()

    var inputCmd by remember { mutableStateOf("") }
    val isAuthorized = user.mode == "STUDY_EXECUTIVO" || user.mode == "ELITE_BLACKER"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .padding(14.dp)
    ) {
        // Console visual header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF101622), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(HackerGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "root@kimyo_5v:~",
                    color = HackerGreen,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isSupaOn?.isConnected == true) "SUPABASE-ON ☁️" else "OFFLINE 🏠",
                    color = if (isSupaOn?.isConnected == true) HackerGreen else Color.White.copy(alpha = 0.4f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Console screen outputs list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF070A0F), RoundedCornerShape(8.dp))
                .border(1.dp, HackerGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            val terminalScroll = rememberScrollState()
            LaunchedEffect(logs.size, usbLogs.size) {
                terminalScroll.animateScrollTo(terminalScroll.maxValue)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(terminalScroll)
            ) {
                logs.forEach { line ->
                    Text(
                        text = line.text,
                        color = if (line.success) HackerGreen else Color(0xFFFF5252),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                if (isInstalling) {
                    Text(
                        text = ">>> Instaliando APK por puerto dual Tipo-C...",
                        color = Color.Yellow,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Prompt Execution entry with command suggestions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "root# ",
                color = HackerGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            OutlinedTextField(
                value = inputCmd,
                onValueChange = { inputCmd = it },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HackerGreen,
                    unfocusedBorderColor = HackerGreen.copy(alpha = 0.3f),
                    focusedTextColor = HackerGreen,
                    unfocusedTextColor = HackerGreen,
                    cursorColor = HackerGreen
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_input_field"),
                shape = RoundedCornerShape(8.dp),
                placeholder = {
                    Text(
                        "Ingresa un comando...",
                        color = HackerGreen.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    if (inputCmd.trim().isNotEmpty()) {
                        viewModel.executeTerminalCommand(inputCmd)
                        inputCmd = ""
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (inputCmd.trim().isNotEmpty()) {
                        viewModel.executeTerminalCommand(inputCmd)
                        inputCmd = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = HackerGreenDark,
                    contentColor = HackerGreen
                ),
                border = BorderStroke(1.dp, HackerGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("terminal_execute_button")
            ) {
                Text("RUN", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Easy hotkeys shortcuts panel for privileged users
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { inputCmd = "install-usb" },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151C2C)),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(2.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("USB-Dual C", fontSize = 10.sp, color = HackerGreen, fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = {
                    if (isAuthorized) {
                        inputCmd = "/hackersincognits.supabase"
                    } else {
                        viewModel.executeTerminalCommand("/hackersincognits.supabase")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151C2C)),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(2.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("/hackersincognits", fontSize = 10.sp, color = if (isAuthorized) HackerGreen else Color.Gray, fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = { inputCmd = "list-cells" },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151C2C)),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(2.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Scan OTG", fontSize = 10.sp, color = HackerGreen, fontFamily = FontFamily.Monospace)
            }
        }

        if (user.mode == "ELITE_BLACKER") {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { inputCmd = "/admin-users" },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2236)),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("🔑 Admin Users", fontSize = 9.sp, color = Color(0xFFFFB74D), fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = { inputCmd = "/admin-logs" },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2236)),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("📜 Audit Logs", fontSize = 9.sp, color = Color(0xFFFFB74D), fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = { inputCmd = "/approve-user " },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2236)),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("✅ Approve User", fontSize = 9.sp, color = Color(0xFFFFB74D), fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { inputCmd = "/set-rank  usuario" },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E1C1C)),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("👤 Rango Usuario", fontSize = 8.sp, color = Color(0xFFE57373), fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = { inputCmd = "/set-rank  administrador" },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C2C1E)),
                    modifier = Modifier.weight(1.1f),
                    contentPadding = PaddingValues(2.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("⭐ Rango Admin", fontSize = 8.sp, color = Color(0xFF81C784), fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = { inputCmd = "/set-rank  omega" },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C1C2C)),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("👑 Rango Omega", fontSize = 8.sp, color = Color(0xFFBA68C8), fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

// --- Tab 3: System Settings and Supabase bound View ---
@Composable
fun SettingsViewTab(viewModel: MainViewModel, user: UserEntity) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val customKey by viewModel.customGeminiKey.collectAsStateWithLifecycle()
    val supabaseConfig by viewModel.supabaseConfig.collectAsStateWithLifecycle()

    var customSupaUrl by remember { mutableStateOf(supabaseConfig?.url ?: "") }
    var customSupaKey by remember { mutableStateOf(supabaseConfig?.anonKey ?: "") }
    var customGeminiKeyInput by remember { mutableStateOf(customKey) }

    LaunchedEffect(supabaseConfig) {
        if (supabaseConfig != null) {
            customSupaUrl = supabaseConfig!!.url
            customSupaKey = supabaseConfig!!.anonKey
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Text(
            text = "CONFIGURACIÓN",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Custom Profile Badge Layout
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (user.mode == "STUDY_EXECUTIVO") Icons.Filled.Star
                        else if (user.mode == "ELITE_BLACKER") Icons.Filled.MilitaryTech
                        else Icons.Filled.Person,
                        contentDescription = "Profile Level Avatar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = user.username.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Rol: ${user.mode}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Section: Supabase Configuration URL and credentials
        Text(
            text = "NODO DE BACKEND SUPABASE (REAL)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Conecta la base de datos de Supabase. Todo se guardará en tu servidor real en tiempo real.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                val isOmega = user.mode == "ELITE_BLACKER"

                OutlinedTextField(
                    value = customSupaUrl,
                    onValueChange = { if (isOmega) customSupaUrl = it },
                    label = { Text("Supabase URL", fontSize = 12.sp) },
                    singleLine = true,
                    enabled = isOmega,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .testTag("settings_supa_url_input")
                )

                OutlinedTextField(
                    value = customSupaKey,
                    onValueChange = { if (isOmega) customSupaKey = it },
                    label = { Text("Supabase Service-Role / Anon Key", fontSize = 12.sp) },
                    singleLine = true,
                    enabled = isOmega,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_supa_key_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (!isOmega) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF381E1E)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = "Locked settings", tint = Color(0xFFE57373), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "ACCESO RESTRINGIDO: Sólo el rango Omega puede modificar el servidor Supabase.",
                                color = Color(0xFFFFCDD2),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.saveSupabaseConfiguration(customSupaUrl, customSupaKey) },
                    enabled = isOmega,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOmega) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("GUARDAR Y CONECTAR BACKEND", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }

        // Section: Gemini API Credentials customization
        Text(
            text = "CLAVE DE AGENTE DE IA (GEMINI API KEY)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Asigna una API Key personalizada de Gemini para tus llamadas, o usa la clave predeterminada inyectada por AI Studio.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                OutlinedTextField(
                    value = customGeminiKeyInput,
                    onValueChange = {
                        customGeminiKeyInput = it
                        viewModel.setCustomGeminiKey(it)
                    },
                    label = { Text("Personal Gemini API Key", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_gemini_key_input")
                )
            }
        }

        // Section: Custom Theme overrides
        Text(
            text = "APARIENCIA Y MODO",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Modo de Tema Activo:", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Text(themeMode, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.setThemeMode("DARK") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14192B)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("DARK", fontSize = 10.sp, color = Color.White)
                    }

                    Button(
                        onClick = { viewModel.setThemeMode("LIGHT") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.Gray),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("LIGHT", fontSize = 10.sp, color = Color.Black)
                    }

                    Button(
                        onClick = { viewModel.setThemeMode("HACKER") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = HackerGreenDark),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("HACKER", fontSize = 10.sp, color = HackerGreen)
                    }
                }
            }
        }

        // Log out button
        Button(
            onClick = { viewModel.logOut() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("logout_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(imageVector = Icons.Filled.ExitToApp, contentDescription = "Log out icon")
            Spacer(modifier = Modifier.width(8.dp))
            Text("SALIR DE LA SESIÓN DE RED", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
