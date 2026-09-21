package com.expensevault.feature.transactions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReceiptPhotoHelper(private val context: Context) {

    fun createTempImageUri(): Uri {
        val tempFile = File.createTempFile(
            "temp_receipt_",
            ".jpg",
            context.cacheDir
        ).apply {
            createNewFile()
            deleteOnExit()
        }
        
        // This relies on FileProvider being declared in AndroidManifest with this authority
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    fun processCapturedPhoto(tempUri: Uri): PhotoResult? {
        try {
            val receiptsDir = File(context.filesDir, "receipts").apply {
                if (!exists()) mkdirs()
            }
            
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "receipt_$timeStamp.jpg"
            val thumbName = "receipt_thumb_$timeStamp.jpg"
            
            val fullFile = File(receiptsDir, fileName)
            val thumbFile = File(receiptsDir, thumbName)
            
            val inputStream = context.contentResolver.openInputStream(tempUri) ?: return null
            
            val outputStream = FileOutputStream(fullFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            
            // Generate thumbnail
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(fullFile.absolutePath, options)
            
            val targetSize = 200
            var scale = 1
            while (options.outWidth / scale / 2 >= targetSize && options.outHeight / scale / 2 >= targetSize) {
                scale *= 2
            }
            
            val thumbOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            val thumbnailBitmap = BitmapFactory.decodeFile(fullFile.absolutePath, thumbOptions)
            
            FileOutputStream(thumbFile).use { out ->
                thumbnailBitmap?.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            
            return PhotoResult(
                fullUri = Uri.fromFile(fullFile),
                thumbUri = Uri.fromFile(thumbFile)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}

data class PhotoResult(
    val fullUri: Uri,
    val thumbUri: Uri
)
