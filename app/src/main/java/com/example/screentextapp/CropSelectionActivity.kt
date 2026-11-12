package com.example.screentextapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream

/**
 * CropSelectionActivity — Layar fullscreen di atas screenshot
 * untuk memilih area yang ingin di-scan teksnya.
 *
 * Alur:
 *  1. Menerima path screenshot dari FloatingOverlayService
 *  2. Menampilkan screenshot sebagai background penuh
 *  3. User menyeret untuk memilih area crop (via CropView)
 *  4. Tekan "Scan Teks Area Ini" → crop bitmap → jalankan OCR
 *  5. Simpan hasil ke FloatingResultHolder → FloatingOverlayService
 *     menampilkan popup hasil
 */
class CropSelectionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SCREENSHOT_PATH = "extra_screenshot_path"
    }

    private lateinit var screenshotBitmap: Bitmap
    private lateinit var ivScreenshot: ImageView
    private lateinit var cropView: CropView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen — tidak ada status/navigation bar gap
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        supportActionBar?.hide()

        setContentView(R.layout.activity_crop_selection)

        val path = intent.getStringExtra(EXTRA_SCREENSHOT_PATH)
        if (path == null) { finish(); return }

        screenshotBitmap = BitmapFactory.decodeFile(path) ?: run {
            Toast.makeText(this, "Tidak dapat membaca screenshot", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        ivScreenshot = findViewById(R.id.ivScreenshot)
        cropView     = findViewById(R.id.cropView)

        // Tampilkan screenshot fullscreen
        ivScreenshot.setImageBitmap(screenshotBitmap)

        // Tombol Batal
        findViewById<android.view.View>(R.id.btnCancelCrop).setOnClickListener {
            finish()
            // Tampilkan kembali floating button
            startService(android.content.Intent(this, FloatingOverlayService::class.java).apply {
                action = FloatingOverlayService.ACTION_SHOW_BUTTON
            })
        }

        // Tombol konfirmasi crop
        findViewById<android.view.View>(R.id.btnConfirmCrop).setOnClickListener {
            if (!cropView.hasValidSelection()) {
                Toast.makeText(this, "Seret untuk memilih area teks terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prosesOCR()
        }
    }

    // =====================================================================
    //  Crop + OCR
    // =====================================================================

    private fun prosesOCR() {
        val cropRectView = cropView.getCropRect()

        // Peta dari koordinat View ke koordinat Bitmap
        val imageMatrix = ivScreenshot.imageMatrix
        val inverseMatrix = Matrix()
        imageMatrix.invert(inverseMatrix)

        val mappedRect = RectF(cropRectView)
        inverseMatrix.mapRect(mappedRect)

        // Pastikan crop rect dalam batas bitmap
        val left   = mappedRect.left .coerceIn(0f, screenshotBitmap.width .toFloat())
        val top    = mappedRect.top  .coerceIn(0f, screenshotBitmap.height.toFloat())
        val right  = mappedRect.right .coerceIn(0f, screenshotBitmap.width .toFloat())
        val bottom = mappedRect.bottom.coerceIn(0f, screenshotBitmap.height.toFloat())

        val w = (right - left).toInt()
        val h = (bottom - top).toInt()

        if (w < 10 || h < 10) {
            Toast.makeText(this, "Area crop terlalu kecil, coba seret lebih besar", Toast.LENGTH_SHORT).show()
            return
        }

        val croppedBitmap = Bitmap.createBitmap(screenshotBitmap, left.toInt(), top.toInt(), w, h)

        // Jalankan OCR (offline, ML Kit)
        val inputImage = InputImage.fromBitmap(croppedBitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                croppedBitmap.recycle()
                val extractedText = visionText.text.trim()

                // Simpan ke holder → service akan mengambilnya
                FloatingResultHolder.pendingText =
                    if (extractedText.isBlank()) "⚠ Tidak ada teks yang terdeteksi di area ini"
                    else extractedText

                // Tutup activity ini dan suruh service tampilkan popup
                finish()
                startService(android.content.Intent(this, FloatingOverlayService::class.java).apply {
                    action = FloatingOverlayService.ACTION_SHOW_RESULT
                })
            }
            .addOnFailureListener { e ->
                croppedBitmap.recycle()
                Toast.makeText(this, "OCR gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::screenshotBitmap.isInitialized && !screenshotBitmap.isRecycled) {
            screenshotBitmap.recycle()
        }
    }
}
