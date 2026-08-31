package com.example

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.components.PermissionScreen
import com.example.ui.screens.MainGalleryScaffold
import com.example.ui.theme.GalleryProTheme
import com.example.ui.viewmodel.GalleryViewModel
import com.example.ui.viewmodel.GalleryViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: GalleryViewModel by viewModels {
        GalleryViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val pendingIntentSender by viewModel.pendingDeleteIntentSender.collectAsState()

            var hasPermission by remember {
                mutableStateOf(checkMediaPermissions())
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        val current = checkMediaPermissions()
                        if (current != hasPermission) {
                            hasPermission = current
                            if (current) {
                                viewModel.loadMedia()
                            }
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            // Permissions request launcher
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val isGranted = permissions.values.any { it } || checkMediaPermissions()
                hasPermission = isGranted
                if (isGranted) {
                    viewModel.loadMedia()
                }
            }

            // Scoped storage deletion confirmation launcher for Android 11+
            val deleteIntentSenderLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                viewModel.onScopedStorageDeleteResult(result.resultCode == Activity.RESULT_OK)
            }

            // Handle system delete request intent sender
            LaunchedEffect(pendingIntentSender) {
                pendingIntentSender?.let { sender ->
                    val intentSenderRequest = IntentSenderRequest.Builder(sender).build()
                    deleteIntentSenderLauncher.launch(intentSenderRequest)
                    viewModel.clearPendingDeleteIntentSender()
                }
            }

            GalleryProTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (hasPermission) {
                        MainGalleryScaffold(viewModel = viewModel)
                    } else {
                        PermissionScreen(
                            onRequestPermission = {
                                permissionLauncher.launch(getRequiredPermissions())
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkMediaPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+: Full access or Selected Photos access
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13: Images or Videos
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12 and below
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.READ_MEDIA_VIDEO)
            list.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            list.add(Manifest.permission.ACCESS_MEDIA_LOCATION)
        }

        return list.toTypedArray()
    }
}

