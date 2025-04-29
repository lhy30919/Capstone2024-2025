package com.capstone.Algan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.capstone.Algan.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        supportActionBar?.hide()

        val signupButton = findViewById<Button>(R.id.signup_button)
        signupButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
        val pwdfindButton = findViewById<Button>(R.id.passwordfind_button)


        pwdfindButton.setOnClickListener {
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
                            Toast.makeText(this, "비밀번호 재설정 이메일이 전송되었습니다.", Toast.LENGTH_SHORT)
                                .show()
                            dialog.dismiss() // 다이얼로그 닫기
                        } else {
                            Toast.makeText(this, "이메일 전송에 실패했습니다. 이메일을 확인해주세요.", Toast.LENGTH_SHORT)
                                .show()
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
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this@LoginActivity, "이메일과 비밀번호를 모두 입력하세요.", Toast.LENGTH_SHORT).show()
            } else {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()

                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                fetchAndSaveUserInfo(userId, email)
                            }
                        } else {
                            Toast.makeText(this, "로그인 실패. 다시 시도하세요.", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }
    }

    private fun fetchAndSaveUserInfo(userId: String, email: String) {
        val database = Firebase.database
        val companiesRef = database.getReference("companies")

        companiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (companySnapshot in snapshot.children) {
                    val companyCode = companySnapshot.key

                    if (companyCode != null) {
                        val employeeSnapshot = companySnapshot.child("employees").child(userId)
                        if (employeeSnapshot.exists()) {
                            val username =
                                employeeSnapshot.child("username").getValue(String::class.java)
                                    ?: "이름 없음"
                            saveUserInfoToPreferences(userId, username, email, companyCode)

                            // ✅ FCM 토큰 저장 (근로자)
                            saveFcmTokenToDatabase(userId, companyCode, isOwner = false)

                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                            return
                        }

                        val ownerSnapshot = companySnapshot.child("owner")
                        val ownerEmail = ownerSnapshot.child("email").getValue(String::class.java)

                        if (ownerEmail == email) {
                            val username =
                                ownerSnapshot.child("username").getValue(String::class.java)
                                    ?: "이름 없음"
                            saveUserInfoToPreferences(userId, username, email, companyCode)

                            // ✅ FCM 토큰 저장 (사업주)
                            saveFcmTokenToDatabase(userId, companyCode, isOwner = true)

                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                            return
                        }
                    }
                }
                Toast.makeText(this@LoginActivity, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LoginActivity", "사용자 정보 불러오기 실패: ${error.message}")
            }
        })
    }

    private fun saveUserInfoToPreferences(
        userId: String,
        username: String,
        email: String,
        companyCode: String
    ) {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("userId", userId)
            putString("username", username)
            putString("userEmail", email)
            putString("companyCode", companyCode)
            apply()
        }

        Log.d(
            "LoginActivity",
            "사용자 정보 저장 완료: ID=$userId, Name=$username, Email=$email, CompanyCode=$companyCode"
        )
    }

    private fun saveFcmTokenToDatabase(userId: String, companyCode: String, isOwner: Boolean) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
            if (tokenTask.isSuccessful) {
                val fcmToken = tokenTask.result
                val rolePath = if (isOwner) "owner" else "employees/$userId"

                val tokenRef = Firebase.database
                    .getReference("companies")
                    .child(companyCode)
                    .child(rolePath)
                    .child("fcmToken")

                tokenRef.setValue(fcmToken)
                    .addOnSuccessListener {
                        Log.d("LoginActivity", "FCM 토큰 저장 성공: $fcmToken")
                    }
                    .addOnFailureListener { e ->
                        Log.e("LoginActivity", "FCM 토큰 저장 실패: ${e.message}")
                    }
            } else {
                Log.w("LoginActivity", "FCM 토큰 가져오기 실패", tokenTask.exception)
            }
        }
    }
}
