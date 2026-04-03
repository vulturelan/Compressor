package id.zelory.compressor.sample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import id.zelory.compressor.sample.databinding.ActivityEntryBinding
import kotlin.jvm.java

class EntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.legacyButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.photoPickerButton.setOnClickListener {
            startActivity(Intent(this, PhotoPickerActivity::class.java))
        }
    }
}
