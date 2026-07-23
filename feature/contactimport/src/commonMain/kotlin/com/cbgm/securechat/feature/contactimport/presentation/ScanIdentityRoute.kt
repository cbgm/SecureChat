package com.cbgm.securechat.feature.contactimport.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.feature.contactimport.platform.rememberQrScannerPermissionRequest
import com.cbgm.securechat.feature.contactimport.presentation.screen.ScanIdentityScreen

@Composable
fun ScanIdentityRoute(
    onQrCodeScanned: (String) -> Unit,
    onBack: () -> Unit,
) {
    var cameraPermissionGranted by remember { mutableStateOf(false) }

    var cameraPermissionDenied by remember { mutableStateOf(false) }

    val requestCameraPermission =
        rememberQrScannerPermissionRequest(
            onPermissionGranted = {
                cameraPermissionGranted = true

                cameraPermissionDenied = false
            },
            onPermissionDenied = {
                cameraPermissionDenied = true
            },
        )

    LaunchedEffect(Unit) {
        requestCameraPermission()
    }

    when {
        cameraPermissionGranted -> {
            ScanIdentityScreen(
                onQrCodeScanned = onQrCodeScanned,
                onBack = onBack,
            )
        }

        cameraPermissionDenied -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "Camera permission is required to scan a QR code.")

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = requestCameraPermission,
                ) {
                    Text("Grant camera permission")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onBack,
                ) {
                    Text("Back")
                }
            }
        }
    }
}
