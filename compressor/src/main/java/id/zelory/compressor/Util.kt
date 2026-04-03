package id.zelory.compressor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream

/**
 * Created on : January 24, 2020
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */

private fun getCacheDir(context: Context): File {
    return File(context.cacheDir, "compressor").apply { mkdirs() }
}

fun File.compressFormat() = when (extension.lowercase()) {
    "png" -> Bitmap.CompressFormat.PNG
    "webp" -> Bitmap.CompressFormat.WEBP
    else -> Bitmap.CompressFormat.JPEG
}

fun Bitmap.CompressFormat.extension() = when (this) {
    Bitmap.CompressFormat.PNG -> "png"
    Bitmap.CompressFormat.WEBP -> "webp"
    else -> "jpg"
}

fun loadBitmap(imageFile: File): Bitmap = BitmapFactory.decodeFile(imageFile.absolutePath).run {
    determineImageRotation(imageFile, this)
}

fun decodeSampledBitmapFromFile(imageFile: File, reqWidth: Int, reqHeight: Int): Bitmap {
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeFile(imageFile.absolutePath, this)
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
        inJustDecodeBounds = false
        val outRatio = outWidth.toFloat() / outHeight.toFloat()
        val reqRatio = reqWidth.toFloat() / reqHeight.toFloat()
        if (outRatio > reqRatio) {
            inDensity = outHeight
            inTargetDensity = reqHeight * inSampleSize
        } else if (outRatio <= reqRatio) {
            inDensity = outWidth
            inTargetDensity = reqWidth * inSampleSize
        }
        BitmapFactory.decodeFile(imageFile.absolutePath, this)
    }
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    // Raw height and width of image
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {

        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

fun determineImageRotation(imageFile: File, bitmap: Bitmap): Bitmap {
    val exif = ExifInterface(imageFile.absolutePath)
    return rotateBitmap(bitmap, exif)
}

fun determineImageRotation(inputStream: InputStream, bitmap: Bitmap): Bitmap {
    val exif = ExifInterface(inputStream)
    return rotateBitmap(bitmap, exif)
}

private fun rotateBitmap(bitmap: Bitmap, exif: ExifInterface): Bitmap {
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
    val matrix = Matrix()
    when (orientation) {
        6 -> matrix.postRotate(90f)
        3 -> matrix.postRotate(180f)
        8 -> matrix.postRotate(270f)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

internal fun copyToCache(context: Context, imageFile: File): File {
    val targetFile = File(getCacheDir(context), imageFile.name)
    val result = imageFile.copyTo(targetFile, true)
    return result
}

internal fun copyToCache(context: Context, uri: Uri): File {
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri)) ?: ""
    val targetFile = File(getCacheDir(context), "${uri.lastPathSegment}.$extension")
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        targetFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
    return targetFile
}

fun overWrite(imageFile: File, bitmap: Bitmap, format: Bitmap.CompressFormat = imageFile.compressFormat(), quality: Int = 100): File {
    val result = if (format == imageFile.compressFormat()) {
        imageFile
    } else {
        File("${imageFile.absolutePath.substringBeforeLast(".")}.${format.extension()}")
    }
    imageFile.delete()
    saveBitmap(bitmap, result, format, quality)
    return result
}

fun saveBitmap(bitmap: Bitmap, destination: File, format: Bitmap.CompressFormat = destination.compressFormat(), quality: Int = 100) {
    destination.parentFile?.mkdirs()
    destination.outputStream().use { outputStream ->
        bitmap.compress(format, quality, outputStream)
        outputStream.flush()
    }
}
