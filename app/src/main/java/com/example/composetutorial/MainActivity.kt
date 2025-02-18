package com.example.composetutorial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.composetutorial.ui.theme.ComposeTutorialTheme
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.rememberAsyncImagePainter
import com.example.composetutorial.Data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.viewModels
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {
    private lateinit var sensorManager: SensorManager
    private var temperatureSensor: Sensor? = null
    private val temperatureViewModel: TemperatureViewModel by viewModels()
    private var temperatureThreshold = 30f
    private var notificationSent = false

    private val temperatureListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val temperature = event.values[0]
            temperatureViewModel.updateTemperature(temperature)
            checkTemperatureThreshold(temperature)
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        if (temperatureSensor == null) {
            simulateTemperature()
        }
        val userPreferences = UserPreferences(applicationContext)
        setContent {
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = true
            ComposeTutorialTheme {
                Surface(modifier = Modifier.systemBarsPadding()) {
                    MainScreen(userPreferences = userPreferences)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        temperatureSensor?.let {
            sensorManager.registerListener(temperatureListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(temperatureListener)
    }

    private fun checkTemperatureThreshold(currentTemp: Float) {
        if (currentTemp >= temperatureThreshold && !notificationSent) {
            showTemperatureAlertNotification()
            notificationSent = true
        } else if (currentTemp < temperatureThreshold) {
            notificationSent = false
        }
    }

    private fun simulateTemperature() {
        lifecycleScope.launch {
            var currentTemp = 20f
            while (true) {
                delay(5000)
                currentTemp += (Math.random() * 4 - 2).toFloat()
                temperatureViewModel.updateTemperature(currentTemp)
                checkTemperatureThreshold(currentTemp)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "temperature_channel",
                "Temperature Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when temperature reaches a certain level"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showTemperatureAlertNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "temperature_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Temperature Alert")
            .setContentText("Temperature has reached the threshold!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userPreferences: UserPreferences,
    temperatureViewModel: TemperatureViewModel = viewModel()
) {
    val navController = rememberNavController()
    val userName by userPreferences.usernameFlow.collectAsState(initial = "Lexi")
    val profileImageUri by userPreferences.profileImageUriFlow.collectAsState(initial = "")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Compose Tutorial",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                actions = {
                    SettingsButton(navController = navController)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                val updatedMessages = SampleData.conversationSample.map { message ->
                    if (message.author == "Lexi") message.copy(author = userName) else message
                }
                val currentTemp by temperatureViewModel.currentTemperature.collectAsState()
                Column {
                    Text(
                        text = "Current Temperature: ${"%.1f".format(currentTemp)}°C",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Conversation(
                        messages = updatedMessages,
                        profileImageUri = profileImageUri,
                        userName = userName
                    )
                }
            }
            composable("settings") {
                val scope = rememberCoroutineScope()
                SettingsScreen(
                    userName = userName,
                    onUserNameChange = { newName ->
                        scope.launch { userPreferences.setUsername(newName) }
                    },
                    profileImageUri = profileImageUri,
                    onProfileImageChange = { newUri ->
                        scope.launch { userPreferences.setProfileImageUri(newUri) }
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    userName: String,
    onUserNameChange: (String) -> Unit,
    profileImageUri: String,
    onProfileImageChange: (String) -> Unit
) {
    var currentName by remember { mutableStateOf(userName) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                val inputStream = context.contentResolver.openInputStream(it)
                val outputFile = File(context.filesDir, "profile_picture.jpg")
                inputStream?.use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                onProfileImageChange(outputFile.absolutePath)
            }
        }
    }

    LaunchedEffect(userName) {
        currentName = userName
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
            ) {
                if (profileImageUri.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(File(profileImageUri)),
                        contentDescription = "Profile picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Add profile picture",
                        modifier = Modifier.size(100.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            UserNameTextField(
                userName = currentName,
                onUserNameChange = { newName ->
                    currentName = newName
                    onUserNameChange(newName)
                }
            )
        }
    }
}

data class Message(val author: String, val body: String)

@Composable
fun MessageCard(msg: Message, profileImageUri: String, userName: String) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        val imagePainter = if (msg.author == userName && profileImageUri.isNotEmpty()) {
            rememberAsyncImagePainter(File(profileImageUri))
        } else {
            painterResource(R.drawable.banana_sword)
        }

        Image(
            painter = imagePainter,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))

        // We keep track if the message is expanded or not in this
        // variable
        var isExpanded by remember { mutableStateOf(false) }
        // surfaceColor will be updated gradually from one color to the other
        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            label = "chat color change when click",
        )

        // We toggle the isExpanded variable when we click on this Column
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Text(
                text = msg.author,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
                // surfaceColor color will be changing gradually from primary to surface
                color = surfaceColor,
                // animateContentSize will change the Surface size gradually
                modifier = Modifier.animateContentSize().padding(1.dp)
            ) {
                Text(
                    text = msg.body,
                    modifier = Modifier.padding(all = 4.dp),
                    // If the message is expanded, we display all its content
                    // otherwise we only display the first line
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun Conversation(messages: List<Message>, profileImageUri: String, userName: String) {
    LazyColumn {
        items(messages) { message ->
            MessageCard(
                msg = message,
                profileImageUri = profileImageUri,
                userName = userName
            )
        }
    }
}

@Composable
fun SettingsButton(navController: NavHostController) {
    IconButton(onClick = {
        val currentRoute = navController.currentDestination?.route
        if (currentRoute == "settings") {
            // Navigate back to home
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        } else {
            // Navigate to settings
            navController.navigate("settings")
        }
    }) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings"
        )
    }
}

@Composable
fun UserNameTextField(userName: String, onUserNameChange: (String) -> Unit) {
    OutlinedTextField(
        value = userName,
        onValueChange = onUserNameChange,
        label = { Text(stringResource(R.string.userName)) }
    )
}


