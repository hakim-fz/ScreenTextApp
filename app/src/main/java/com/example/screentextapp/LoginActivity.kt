package com.example.screentextapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.screentextapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // SLIDE 5: setOnClickListener — listener dipasang di onCreate, bukan dijalankan saat itu
        binding.btnLogin.setOnClickListener {
            // SLIDE 7: baca input EditText DI DALAM listener, bukan di onCreate
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            when {
                username.isEmpty() -> {
                    binding.etUsername.error = "Username tidak boleh kosong"
                    // SLIDE 8: Toast sebagai umpan balik cepat
                    Toast.makeText(this, "Isi username terlebih dahulu!", Toast.LENGTH_SHORT).show()
                }
                password.isEmpty() -> {
                    binding.etPassword.error = "Password tidak boleh kosong"
                    Toast.makeText(this, "Isi password terlebih dahulu!", Toast.LENGTH_SHORT).show()
                }
                username == "user" && password == "1234" -> {
                    Toast.makeText(this, "Selamat datang, $username!", Toast.LENGTH_SHORT).show()
                    // SLIDE 11: Intent eksplisit — tujuan disebut jelas
                    val intent = Intent(this, HomeActivity::class.java)
                    // SLIDE 12: putExtra — kirim data ke Activity tujuan
                    intent.putExtra(HomeActivity.EXTRA_USERNAME, username)
                    startActivity(intent)
                    finish()
                }
                else -> {
                    // SLIDE 8: Toast umpan balik untuk input salah
                    Toast.makeText(this, "Username atau password salah!", Toast.LENGTH_SHORT).show()
                    // SLIDE 7: kosongkan field password setelah gagal
                    binding.etPassword.text?.clear()
                }
            }
        }

        // SLIDE 9: setOnLongClickListener — tekan lama untuk hint
        binding.btnLogin.setOnLongClickListener {
            Toast.makeText(this, "Hint: user / 1234", Toast.LENGTH_LONG).show()
            true // kembalikan true bila event sudah "dihabiskan" di sini
        }
    }
}
