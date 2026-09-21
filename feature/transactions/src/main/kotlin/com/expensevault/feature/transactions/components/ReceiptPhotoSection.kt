package com.expensevault.feature.transactions.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.expensevault.feature.transactions.ReceiptPhotoHelper

@Composable
fun ReceiptPhotoSection(
    photoUri: Uri?,
    onPhotoCaptured: (Uri) -> Unit,
    onPhotoRemoved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val helper = remember { ReceiptPhotoHelper(context) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                tempUri?.let { uri ->
                    val result = helper.processCapturedPhoto(uri)
                    result?.let {
                        onPhotoCaptured(it.thumbUri)
                    }
                }
            }
        }
    )

    Column(modifier = modifier.fillMaxWidth()) {
        if (photoUri == null) {
            OutlinedButton(
                onClick = {
                    val uri = helper.createTempImageUri()
                    tempUri = uri
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Take Photo",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Receipt Photo")
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = coil3.request.ImageRequest.Builder(context)
                                .data(photoUri)
                                .size(coil3.size.Size(180, 180))
                                .build()
                        ),
                        contentDescription = "Receipt thumbnail",
                        modifier = Modifier
                            .size(60.dp)
                            .padding(4.dp),
                        contentScale = ContentScale.Crop
                    )
                    
                    TextButton(onClick = onPhotoRemoved) {
                        Icon(Icons.Default.Clear, contentDescription = "Remove")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove")
                    }
                }
            }
        }
    }
}
