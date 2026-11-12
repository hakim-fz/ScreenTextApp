package com.example.screentextapp

/**
 * Singleton sederhana untuk meneruskan teks hasil OCR
 * dari CropSelectionActivity ke FloatingOverlayService.
 *
 * (Alternatif: LocalBroadcastManager; singleton lebih ringan untuk kasus ini)
 */
object FloatingResultHolder {
    var pendingText: String? = null
}
