package com.example.a224_lablearnandroid

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Step 1: Hardware Tracker
class SensorTracker(private val context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    // Utilizing Accelerometer as demonstration of continuous value
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    var onSensorUpdate: ((FloatArray) -> Unit)? = null
    var onLocationUpdate: ((android.location.Location) -> Unit)? = null

    fun startSensorTracking() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopSensorTracking() {
        sensorManager.unregisterListener(this)
    }

    @SuppressLint("MissingPermission")
    fun startLocationTracking() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    fun stopLocationTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { onLocationUpdate?.invoke(it) }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            onSensorUpdate?.invoke(event.values.clone()) // clone to avoid reference mutation
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

// Step 2: ViewModel
class SensorViewModel(application: Application) : AndroidViewModel(application) {
    private val tracker = SensorTracker(application)

    private val _accelerometerData = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val accelerometerData: StateFlow<FloatArray> = _accelerometerData

    private val _locationData = MutableStateFlow<android.location.Location?>(null)
    val locationData: StateFlow<android.location.Location?> = _locationData

    init {
        tracker.onSensorUpdate = { _accelerometerData.value = it }
        tracker.onLocationUpdate = { _locationData.value = it }
    }

    fun startSensorTracking() {
        tracker.startSensorTracking()
    }

    fun startLocationTracking() {
        tracker.startLocationTracking()
    }

    fun stopLocationTracking() {
        tracker.stopLocationTracking()
    }

    override fun onCleared() {
        super.onCleared()
        tracker.stopSensorTracking()
        tracker.stopLocationTracking()
    }
}

// Step 3: Compose UI & Activity
class SensorLocationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SensorScreen()
                }
            }
        }
    }
}

@Composable
fun SensorScreen(viewModel: SensorViewModel = viewModel()) {
    val accValue by viewModel.accelerometerData.collectAsState()
    val locationValue by viewModel.locationData.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.startLocationTracking()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startSensorTracking()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Sensor & Location MVVM", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(text = "Accelerometer:", style = MaterialTheme.typography.titleLarge)
        Text(text = "X: ${String.format("%.2f", accValue[0])}", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Y: ${String.format("%.2f", accValue[1])}", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Z: ${String.format("%.2f", accValue[2])}", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(text = "GPS Location:", style = MaterialTheme.typography.titleLarge)
        if (locationValue != null) {
            Text(text = "Lat: ${locationValue!!.latitude}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Lng: ${locationValue!!.longitude}", style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(text = "No Location Data / Stopped", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            androidx.compose.material3.Button(onClick = {
                val hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (hasFineLocation) {
                    viewModel.startLocationTracking()
                } else {
                    permissionLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }
            }) {
                Text("Start Location")
            }
            
            androidx.compose.material3.Button(onClick = {
                viewModel.stopLocationTracking()
            }) {
                Text("Stop Location")
            }
        }
    }
}