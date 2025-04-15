package com.capstone.Algan

import com.capstone.Algan.utils.UserData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        // ActionBar 숨기기
        supportActionBar?.hide()

        val signupButton = findViewById<Button>(R.id.signup_button)
        signupButton.setOnClickListener {
            // SignUpActivity로 이동
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
        val savebtn = findViewById<Button>(R.id.savebtn)

        // 비밀번호 찾기 버튼 클릭 리스너
        savebtn.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_password_find, null)
            val emailEditText = dialogView.findViewById<EditText>(R.id.Email)

            val dialog = android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            val btnPassFind = dialogView.findViewById<Button>(R.id.btn_passfind)
            btnPassFind.setOnClickListener {
                val email = emailEditText.text.toString().trim()

                if (email.isEmpty()) {
                    Toast.makeText(this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "비밀번호 재설정 이메일이 전송되었습니다.", Toast.LENGTH_SHORT).show()
                            dialog.dismiss() // 다이얼로그 닫기
                        } else {
                            Toast.makeText(this, "이메일 전송에 실패했습니다. 이메일을 확인해주세요.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }

            dialog.show()
        }


        val loginButton = findViewById<Button>(R.id.login_button)
        loginButton.setOnClickListener {
            val emailField = findViewById<EditText>(R.id.Email)
            val passwordField = findViewById<EditText>(R.id.password)

            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // 로그인 성공
                        Toast.makeText(this, "로그인 성공!", Toast.LENGTH_LONG).show()

                        // 현재 로그인한 사용자 ID 가져오기
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            UserData.fetchAndSaveUserInfo(
                                context = this,
                                userId = userId,
                                email = email,
                                onSuccess = {
                                    val intent = Intent(this, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                },
                                onFailure = {
                                    Toast.makeText(this, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_LONG)
                                        .show()
                                }
                            )
                        }
                    }
                }
        }
    }
}