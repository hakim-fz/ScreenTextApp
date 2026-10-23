package com.example.screentextapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.screentextapp.databinding.ActivityHomeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        val username = intent.getStringExtra("USERNAME") ?: "user"

        // Menu toolbar: Profil & Logout
        binding.toolbar.inflateMenu(R.menu.menu_home)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_profil -> {
                    val intent = Intent(this, ProfilActivity::class.java)
                    intent.putExtra("USERNAME", username)
                    startActivity(intent)
                    true
                }
                R.id.action_logout -> {
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        // Tombol MULAI
        binding.btnMulai.setOnClickListener {
            isServiceRunning = !isServiceRunning
            if (isServiceRunning) {
                binding.btnMulai.text = "BERHENTI"
                binding.btnMulai.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#F44336")
                )
                binding.tvStatus.text = "● Aktif - Berjalan di Latar Belakang"
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#388E3C"))
                Toast.makeText(this, "Layanan ScreenText diaktifkan!", Toast.LENGTH_SHORT).show()
            } else {
                binding.btnMulai.text = "MULAI"
                binding.btnMulai.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#1976D2")
                )
                binding.tvStatus.text = "● Tidak Aktif"
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#F44336"))
                Toast.makeText(this, "Layanan ScreenText dimatikan.", Toast.LENGTH_SHORT).show()
            }
        }

        // FAB (+) → Pop-up menu Crop Screenshot
        binding.fabMenu.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Menu ScreenText")
                .setItems(arrayOf("✂  Crop Screenshot")) { _, which ->
                    when (which) {
                        0 -> {
                            Toast.makeText(
                                this,
                                "Fitur Crop Screenshot memerlukan izin Overlay & Aksesibilitas.",
                                Toast.LENGTH_LONG
                            ).show()
                            // TODO: Launch overlay crop tool
                        }
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }
}
