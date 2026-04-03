package id.zelory.compressor.sample

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import id.zelory.compressor.Compressor
import id.zelory.compressor.calculateInSampleSize
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import id.zelory.compressor.determineImageRotation
import id.zelory.compressor.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.DecimalFormat
import java.util.Random
import kotlin.math.log10
import kotlin.math.pow

/**
 * Created on : January 25, 2020
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
class PhotoPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var actualImageUri: Uri? = null
    private var compressedImage: File? = null

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            Log.d(TAG, "uri: $uri")
            if (uri != null) {
                actualImageUri = uri
                lifecycleScope.launch {
                    try {
                        val bitmap = withContext(Dispatchers.IO) {
                            val options = BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                            }
                            // Estimate memory usage directly from Uri without creating a temp file
                            contentResolver.openInputStream(uri)?.use { inputStream ->
                                BitmapFactory.decodeStream(inputStream, null, options)
                            }
                            
                            val estimatedMemory = options.outWidth.toLong() * options.outHeight.toLong() * 4
                            val safeThreshold = 20 * 1024 * 1024 // 20MB threshold

                            if (estimatedMemory > safeThreshold) {
                                Log.d(TAG, "Image is large (${getReadableFileSize(estimatedMemory)}), using sampled decoding from Uri")
                                decodeSampledBitmapFromUri(this@PhotoPickerActivity, uri, 1000, 1000)
                            } else {
                                Log.d(TAG, "Image is safe (${getReadableFileSize(estimatedMemory)}), loading full bitmap from Uri")
                                loadBitmap(this@PhotoPickerActivity, uri)
                            }
                        }

                        binding.actualImageView.setImageBitmap(bitmap)
                        val size = getFileSize(uri)
                        binding.actualSizeTextView.text =
                            String.format("Size : %s", getReadableFileSize(size))
                        clearImage()
                    } catch (e: Exception) {
                        showError("Failed to read picture data!")
                        e.printStackTrace()
                    }
                }
            } else {
                showError("No media selected")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.actualImageView.setBackgroundColor(getRandomColor())
        clearImage()
        setupClickListener()
    }

    private fun setupClickListener() {
        binding.chooseImageButton.setOnClickListener { chooseImage() }
        binding.compressImageButton.setOnClickListener { compressImage() }
        binding.customCompressImageButton.setOnClickListener { customCompressImage() }
    }

    private fun chooseImage() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun compressImage() {
        actualImageUri?.let { uri ->
            lifecycleScope.launch {
                // Default compression using Uri
                compressedImage = Compressor.compress(this@PhotoPickerActivity, uri)
                setCompressedImage()
            }
        } ?: showError("Please choose an image!")
    }

    private fun customCompressImage() {
        actualImageUri?.let { uri ->
            lifecycleScope.launch {
                // Full custom using Uri
                compressedImage = Compressor.compress(this@PhotoPickerActivity, uri) {
                    resolution(1280, 720)
                    quality(80)
                    format(Bitmap.CompressFormat.WEBP)
                    size(2_097_152) // 2 MB
                }
                setCompressedImage()
            }
        } ?: showError("Please choose an image!")
    }

    private fun setCompressedImage() {
        compressedImage?.let {
            binding.compressedImageView.setImageBitmap(BitmapFactory.decodeFile(it.absolutePath))
            binding.compressedSizeTextView.text =
                String.format("Size : %s", getReadableFileSize(it.length()))
            Toast.makeText(this, "Compressed image save in " + it.path, Toast.LENGTH_LONG).show()
            Log.d("Compressor", "Compressed image save in " + it.path)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun clearImage() {
        binding.actualImageView.setBackgroundColor(getRandomColor())
        binding.compressedImageView.setImageDrawable(null)
        binding.compressedImageView.setBackgroundColor(getRandomColor())
        binding.compressedSizeTextView.text = "Size : -"
    }

    private fun getFileSize(uri: Uri): Long {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                cursor.getLong(sizeIndex)
            } else {
                0L
            }
        } ?: 0L
    }

    private fun showError(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }

    private fun getRandomColor() = Random().run {
        Color.argb(100, nextInt(256), nextInt(256), nextInt(256))
    }

    private fun getReadableFileSize(size: Long): String {
        if (size <= 0) {
            return "0"
        }
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    fun loadBitmap(context: Context, uri: Uri): Bitmap {
        val options = BitmapFactory.Options()
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IOException("Failed to decode bitmap from uri")

        return context.contentResolver.openInputStream(uri)?.use {
            determineImageRotation(it, bitmap)
        } ?: bitmap
    }

    fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, this)
            }
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
            inJustDecodeBounds = false

            // Adjust density for high quality sampling
            val outRatio = outWidth.toFloat() / outHeight.toFloat()
            val reqRatio = reqWidth.toFloat() / reqHeight.toFloat()
            if (outRatio > reqRatio) {
                inDensity = outHeight
                inTargetDensity = reqHeight * inSampleSize
            } else if (outRatio <= reqRatio) {
                inDensity = outWidth
                inTargetDensity = reqWidth * inSampleSize
            }

            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, this)
            } ?: throw IOException("Failed to open input stream from uri")
        }
    }

    companion object {
        private const val TAG = "PhotoPickerActivity"
    }
}
