package com.example.screentextapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.screentextapp.databinding.ActivityProfilBinding
import com.google.android.material.snackbar.Snackbar

class ProfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilBinding

    // SLIDE 12: companion object — konstanta kunci putExtra/getExtra agar tidak salah ketik
    companion object {
        const val EXTRA_USERNAME = "extra_username"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // SLIDE 13: getStringExtra ambil data dari Intent — siapkan nilai cadangan dengan ?:
        val username = intent.getStringExtra(EXTRA_USERNAME) ?: "user"

        // SLIDE 13: tampilkan data segera di onCreate agar layar langsung terisi
        binding.tvNamaUser.text = username.replaceFirstChar { it.uppercase() }
        binding.tvUsername.text = username

        // SLIDE 5: setOnClickListener toolbar navigation (back)
        binding.toolbar.setNavigationOnClickListener {
            // SLIDE 14: finish() — tutup Activity ini dan kembali ke layar sebelumnya
            finish()
        }

        // SLIDE 9: setOnLongClickListener avatar — tekan lama untuk info
        binding.viewAvatar.setOnLongClickListener {
            // SLIDE 8: tvStatus sebagai pesan menetap
            Snackbar.make(binding.root, "Foto profil belum bisa diubah di versi ini", Snackbar.LENGTH_LONG).show()
            true
        }

        // SLIDE 5: onClick tombol Edit (jika ada aksi di masa depan)
        binding.btnEdit.setOnClickListener {
            // SLIDE 8: Toast umpan balik
            Toast.makeText(this, "Fitur edit profil segera hadir!", Toast.LENGTH_SHORT).show()
        }

        // SLIDE 5: onClick tombol Keluar
        binding.btnLogout.setOnClickListener {
            // SLIDE 8: konfirmasi sebelum keluar
            Toast.makeText(this, "Sampai jumpa, ${username.replaceFirstChar { it.uppercase() }}!", Toast.LENGTH_SHORT).show()
            // SLIDE 11: Intent eksplisit kembali ke Login
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            // SLIDE 14: finish() tutup Activity ini
            finish()
        }
    }
}
