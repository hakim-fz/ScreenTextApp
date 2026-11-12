package com.example.screentextapp

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * CapturePermissionActivity — Aktifitas transparan yang bertugas meminta
 * izin MediaProjection kepada pengguna (dialog sistem "Izinkan menangkap layar?").
 *
 * Setelah izin diberikan, activity langsung meneruskan hasil ke FloatingOverlayService
 * dan menutup diri (tidak terlihat di recent apps).
 *
 * Ini adalah pola yang diperlukan karena MediaProjection HARUS diminta dari Activity,
 * bukan dari Service.
 */
class CapturePermissionActivity : AppCompatActivity() {

    private val capturePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // Teruskan ke FloatingOverlayService untuk memulai capture
            val serviceIntent = Intent(this, FloatingOverlayService::class.java).apply {
                action = FloatingOverlayService.ACTION_DO_CAPTURE
                putExtra(FloatingOverlayService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(FloatingOverlayService.EXTRA_DATA, result.data)
            }
            // Harus startForegroundService agar service bisa segera panggil startForeground()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            Toast.makeText(this, "Izin tidak diberikan — tidak dapat scan layar", Toast.LENGTH_SHORT).show()
            // Tampilkan kembali floating button
            startService(Intent(this, FloatingOverlayService::class.java).apply {
                action = FloatingOverlayService.ACTION_SHOW_BUTTON
            })
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Tidak ada layout — activity ini sepenuhnya transparan

        val mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // Minta izin capture layar (menampilkan dialog sistem)
        capturePermissionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
}
