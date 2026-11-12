package com.example.screentextapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream

/**
 * FloatingOverlayService — Foreground Service yang:
 *
 *  1. Menampilkan tombol FAB melayang (draggable) di atas semua aplikasi
 *  2. Saat FAB diklik → meminta izin MediaProjection via CapturePermissionActivity
 *  3. Setelah izin diberikan → menangkap screenshot layar secara penuh
 *  4. Membuka CropSelectionActivity untuk memilih area teks
 *  5. Setelah OCR selesai → menampilkan popup hasil dengan tombol Salin & Bagikan
 *
 * ACTION yang didukung:
 *  - ACTION_START       : mulai service & tampilkan floating button
 *  - ACTION_STOP        : hentikan service & bersihkan view
 *  - ACTION_DO_CAPTURE  : terima hasil MediaProjection → tangkap layar
 *  - ACTION_SHOW_BUTTON : tampilkan floating button kembali (setelah cancel/selesai)
 *  - ACTION_SHOW_RESULT : ambil teks dari FloatingResultHolder → tampilkan popup
 */
class FloatingOverlayService : Service() {

    companion object {
        const val ACTION_START       = "action_start"
        const val ACTION_STOP        = "action_stop"
        const val ACTION_DO_CAPTURE  = "action_do_capture"
        const val ACTION_SHOW_BUTTON = "action_show_button"
        const val ACTION_SHOW_RESULT = "action_show_result"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA        = "extra_data"

        private const val CHANNEL_ID = "screentext_overlay_channel"
        private const val NOTIF_ID   = 2001
    }

    private lateinit var windowManager: WindowManager

    // View yang sedang ditampilkan sebagai overlay
    private var floatingButtonView: View? = null
    private var floatingResultView: View? = null

    // State MediaProjection untuk capture layar
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay:  VirtualDisplay?  = null
    private var imageReader:     ImageReader?     = null

    private val screenWidth  by lazy { resources.displayMetrics.widthPixels }
    private val screenHeight by lazy { resources.displayMetrics.heightPixels }
    private val screenDensity by lazy { resources.displayMetrics.densityDpi }

    private val mainHandler = Handler(Looper.getMainLooper())

    // =====================================================================
    //  Lifecycle Service
    // =====================================================================

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {

            ACTION_START -> {
                startForeground(NOTIF_ID, buildNotification("Ketuk ikon untuk scan teks layar"))
                showFloatingButton()
            }

            ACTION_DO_CAPTURE -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, android.app.Activity.RESULT_CANCELED)
                @Suppress("DEPRECATION")
                val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                else
                    intent.getParcelableExtra(EXTRA_DATA)

                if (resultCode == android.app.Activity.RESULT_OK && data != null) {
                    // Android 14+ — update tipe foreground service ke mediaProjection
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(
                            NOTIF_ID,
                            buildNotification("Mengambil screenshot..."),
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIF_ID,
                            buildNotification("Mengambil screenshot..."),
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                        )
                    }

                    val mpManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    mediaProjection = mpManager.getMediaProjection(resultCode, data)

                    // Sembunyikan button dulu, tunggu layar bersih
                    hideFloatingButton()
                    mainHandler.postDelayed({ captureScreen() }, 400)
                }
            }

            ACTION_SHOW_BUTTON -> {
                showFloatingButton()
            }

            ACTION_SHOW_RESULT -> {
                val teks = FloatingResultHolder.pendingText ?: return START_STICKY
                FloatingResultHolder.pendingText = null
                showFloatingResult(teks)
            }

            ACTION_STOP -> {
                cleanup()
                stopSelf()
            }
        }
        return START_STICKY
    }

    // =====================================================================
    //  Floating Button
    // =====================================================================

    private fun showFloatingButton() {
        if (floatingButtonView != null) return

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.layout_floating_button, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 350
        }

        // Variabel untuk drag
        var initX = 0; var initY = 0
        var touchX = 0f; var touchY = 0f
        var isClick = true

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x; initY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    isClick = true
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (Math.abs(event.rawX - touchX) > 8 || Math.abs(event.rawY - touchY) > 8) {
                        isClick = false
                    }
                    params.x = initX + (touchX - event.rawX).toInt()
                    params.y = initY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) mulaiCaptureFlow()
                    false
                }
                else -> false
            }
        }

        floatingButtonView = view
        windowManager.addView(view, params)
    }

    private fun hideFloatingButton() {
        floatingButtonView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            floatingButtonView = null
        }
    }

    /** Mulai alur capture → buka CapturePermissionActivity */
    private fun mulaiCaptureFlow() {
        val intent = Intent(this, CapturePermissionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    // =====================================================================
    //  Screen Capture via MediaProjection
    // =====================================================================

    private fun captureScreen() {
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenTextCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        // Tunggu frame pertama tersedia
        mainHandler.postDelayed({
            val image = imageReader?.acquireLatestImage()
            if (image != null) {
                val planes    = image.planes
                val buffer    = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride   = planes[0].rowStride
                val rowPadding  = rowStride - pixelStride * screenWidth

                val bitmap = Bitmap.createBitmap(
                    screenWidth + rowPadding / pixelStride,
                    screenHeight,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()

                // Simpan ke cache
                val tempFile = File(cacheDir, "screenshot_temp.png")
                try {
                    FileOutputStream(tempFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    bitmap.recycle()
                }

                // Hentikan capture resources
                virtualDisplay?.release(); virtualDisplay = null
                mediaProjection?.stop();   mediaProjection = null
                imageReader?.close();      imageReader = null

                // Buka CropSelectionActivity
                val intent = Intent(this, CropSelectionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(CropSelectionActivity.EXTRA_SCREENSHOT_PATH, tempFile.absolutePath)
                }
                startActivity(intent)

            } else {
                // Frame tidak tersedia — tampilkan kembali tombol
                mainHandler.post {
                    Toast.makeText(this, "Gagal mengambil screenshot, coba lagi", Toast.LENGTH_SHORT).show()
                    showFloatingButton()
                }
            }
        }, 300)
    }

    // =====================================================================
    //  Floating Result Popup
    // =====================================================================

    fun showFloatingResult(teks: String) {
        // Hapus popup lama jika ada
        dismissResult()

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.layout_floating_result, null)

        val params = WindowManager.LayoutParams(
            (screenWidth * 0.92).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 80
        }

        view.apply {
            findViewById<TextView>(R.id.tvFloatingResult)?.text = teks
            findViewById<TextView>(R.id.tvFloatingCharCount)?.text = "${teks.length} karakter"

            // Tombol Salin
            findViewById<View>(R.id.btnFloatingSalin)?.setOnClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Hasil Scan", teks))
                Toast.makeText(this@FloatingOverlayService, "✓ Disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                dismissResult()
                showFloatingButton()
            }

            // Tombol Bagikan
            findViewById<View>(R.id.btnFloatingBagikan)?.setOnClickListener {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, teks)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(Intent.createChooser(shareIntent, "Bagikan via").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }

            // Tombol Tutup
            findViewById<View>(R.id.btnFloatingTutup)?.setOnClickListener {
                dismissResult()
                showFloatingButton()
            }
        }

        floatingResultView = view
        windowManager.addView(view, params)
    }

    private fun dismissResult() {
        floatingResultView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            floatingResultView = null
        }
    }

    // =====================================================================
    //  Helpers
    // =====================================================================

    @Suppress("DEPRECATION")
    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

    private fun cleanup() {
        hideFloatingButton()
        dismissResult()
        virtualDisplay?.release()
        mediaProjection?.stop()
        imageReader?.close()
        // Hapus file temp
        File(cacheDir, "screenshot_temp.png").delete()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    // =====================================================================
    //  Notification
    // =====================================================================

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ScreenText Layanan Aktif",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Overlay scan teks dari layar"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val stopPendingIntent = PendingIntent.getService(
            this, 0,
            Intent(this, FloatingOverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ScreenText — Scan Layar Aktif")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Hentikan",
                stopPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
