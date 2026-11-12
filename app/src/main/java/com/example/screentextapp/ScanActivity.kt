package com.example.screentextapp

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.screentextapp.databinding.ActivityScanBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yalantis.ucrop.UCrop
import coil.load
import java.io.File

/**
 * ScanActivity — fitur OCR + Crop gambar
 *
 * Alur kerja:
 *  1. User pilih gambar dari Galeri ATAU ambil foto langsung
 *  2. (Opsional) Crop gambar via UCrop sebelum scan
 *  3. Tekan SCAN TEKS → ML Kit memproses gambar offline
 *  4. Hasil teks tampil di area EditText (editable, bisa disalin/dibagikan)
 */
class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding

    // URI gambar yang sedang dipilih / hasil crop
    private var selectedImageUri: Uri? = null

    // URI foto kamera (disimpan di MediaStore)
    private var cameraImageUri: Uri? = null

    // =====================================================================
    //  Activity Result Launchers
    // =====================================================================

    /** Pilih gambar dari galeri */
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                tampilkanPratinjau(it)
                sembunyikanHasil()
                tampilkanTombolCrop(true)
            }
        }

    /** Ambil foto dari kamera */
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { berhasil ->
            if (berhasil) {
                cameraImageUri?.let { uri ->
                    selectedImageUri = uri
                    tampilkanPratinjau(uri)
                    sembunyikanHasil()
                    tampilkanTombolCrop(true)
                }
            } else {
                Toast.makeText(this, "Pengambilan foto dibatalkan", Toast.LENGTH_SHORT).show()
            }
        }

    /** Hasil dari UCrop */
    private val uCropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val resultUri = UCrop.getOutput(result.data!!)
                if (resultUri != null) {
                    selectedImageUri = resultUri
                    tampilkanPratinjau(resultUri)
                    sembunyikanHasil()
                    Toast.makeText(this, "✓ Gambar berhasil di-crop", Toast.LENGTH_SHORT).show()
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val error = result.data?.let { UCrop.getError(it) }
                Toast.makeText(this, "Crop gagal: ${error?.message}", Toast.LENGTH_SHORT).show()
            }
        }

    /** Minta izin kamera */
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) bukaKamera()
            else Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_LONG).show()
        }

    /** Minta izin baca storage (Android ≤ 12) */
    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) pickImageLauncher.launch("image/*")
            else Toast.makeText(this, "Izin penyimpanan diperlukan", Toast.LENGTH_LONG).show()
        }

    // =====================================================================
    //  Lifecycle
    // =====================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnPilihGaleri.setOnClickListener { cekIzinDanBukaGaleri() }
        binding.btnAmbilFoto.setOnClickListener { cekIzinKamera() }

        // Tombol Crop — hanya aktif setelah gambar dipilih
        binding.btnCrop.setOnClickListener {
            val uri = selectedImageUri
            if (uri == null) {
                Toast.makeText(this, "Pilih gambar terlebih dahulu!", Toast.LENGTH_SHORT).show()
            } else {
                launchCrop(uri)
            }
        }

        binding.btnScan.setOnClickListener {
            val uri = selectedImageUri
            if (uri == null) Toast.makeText(this, "Pilih gambar terlebih dahulu!", Toast.LENGTH_SHORT).show()
            else prosesOCR(uri)
        }

        binding.btnSalin.setOnClickListener {
            val teks = binding.etHasilTeks.text.toString()
            if (teks.isBlank()) { Toast.makeText(this, "Tidak ada teks", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Hasil Scan Teks", teks))
            Toast.makeText(this, "✓ Teks disalin ke clipboard!", Toast.LENGTH_SHORT).show()
        }

        binding.btnBagikan.setOnClickListener {
            val teks = binding.etHasilTeks.text.toString()
            if (teks.isBlank()) { Toast.makeText(this, "Tidak ada teks", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, teks) },
                "Bagikan teks via"
            ))
        }

        binding.btnHapus.setOnClickListener {
            binding.etHasilTeks.setText("")
            binding.tvJumlahKarakter.text = "0 karakter"
            sembunyikanHasil()
            Toast.makeText(this, "Hasil dihapus", Toast.LENGTH_SHORT).show()
        }
    }

    // =====================================================================
    //  Crop — UCrop
    // =====================================================================

    /**
     * Buka UCrop untuk crop gambar sebelum scan.
     * Mendukung semua format gambar (JPG, PNG, WEBP, dll).
     */
    private fun launchCrop(sourceUri: Uri) {
        // File tujuan hasil crop (di cache app)
        val destFile = File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        val destUri = Uri.fromFile(destFile)

        val options = UCrop.Options().apply {
            setToolbarTitle("Crop Gambar")
            setStatusBarColor(android.graphics.Color.parseColor("#1565C0"))
            setToolbarColor(android.graphics.Color.parseColor("#1976D2"))
            setToolbarWidgetColor(android.graphics.Color.WHITE)
            setActiveControlsWidgetColor(android.graphics.Color.parseColor("#1976D2"))
            setCompressionQuality(100)
            // Free crop — tidak terikat aspek rasio
            setFreeStyleCropEnabled(true)
        }

        val intent = UCrop.of(sourceUri, destUri)
            .withOptions(options)
            .getIntent(this)

        uCropLauncher.launch(intent)
    }

    // =====================================================================
    //  OCR — Google ML Kit Text Recognition
    // =====================================================================

    private fun prosesOCR(imageUri: Uri) {
        setLoadingState(true)
        try {
            val inputImage = InputImage.fromFilePath(this, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    setLoadingState(false)
                    val hasilTeks = visionText.text
                    if (hasilTeks.isBlank()) {
                        Toast.makeText(this, "⚠ Tidak ada teks terdeteksi di gambar ini", Toast.LENGTH_LONG).show()
                        sembunyikanHasil()
                    } else {
                        tampilkanHasil(hasilTeks)
                    }
                }
                .addOnFailureListener { e ->
                    setLoadingState(false)
                    Toast.makeText(this, "Gagal memproses: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            setLoadingState(false)
            Toast.makeText(this, "Error membaca gambar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // =====================================================================
    //  UI Helpers
    // =====================================================================

    private fun tampilkanPratinjau(uri: Uri) {
        binding.ivPreview.load(uri) {
            crossfade(true)
            error(android.R.drawable.ic_menu_gallery)
        }
    }

    private fun tampilkanTombolCrop(show: Boolean) {
        binding.btnCrop.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun tampilkanHasil(teks: String) {
        binding.etHasilTeks.setText(teks)
        binding.tvJumlahKarakter.text = "${teks.length} karakter"
        binding.cardHasil.visibility = View.VISIBLE
        binding.layoutEmpty.visibility = View.GONE
        binding.etHasilTeks.requestFocus()
        Toast.makeText(this, "✓ Teks berhasil diekstrak!", Toast.LENGTH_SHORT).show()
    }

    private fun sembunyikanHasil() {
        binding.cardHasil.visibility = View.GONE
        binding.layoutEmpty.visibility = View.VISIBLE
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.layoutLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnScan.isEnabled = !isLoading
        binding.btnScan.text = if (isLoading) "Memproses..." else "🔍  SCAN TEKS"
    }

    // =====================================================================
    //  Izin & Pembukaan Galeri / Kamera
    // =====================================================================

    private fun cekIzinDanBukaGaleri() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED)
                    pickImageLauncher.launch("image/*")
                else
                    requestStoragePermission.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> pickImageLauncher.launch("image/*")
            else -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                    pickImageLauncher.launch("image/*")
                else
                    requestStoragePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun cekIzinKamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            bukaKamera()
        else
            requestCameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun bukaKamera() {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "scan_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(this, "Tidak dapat membuat file foto", Toast.LENGTH_SHORT).show()
        }
    }
}
