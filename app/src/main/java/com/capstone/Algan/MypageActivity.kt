package com.capstone.Algan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.capstone.Algan.utils.UserData
import com.google.firebase.auth.FirebaseAuth

class MyPageActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var companyNameTextView: TextView
    private lateinit var companyCodeTextView: TextView
    private lateinit var userRoleTextView: TextView
    private lateinit var timeSalaryTextView: TextView
    private lateinit var logoutButton: Button
    private lateinit var editButton: Button

    private lateinit var usernameEditText: EditText
    private lateinit var phoneEditText: EditText

    private var isEditing = false // 수정 모드 여부 체크

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_page)

        // Firebase 초기화
        auth = FirebaseAuth.getInstance()

        // UI 요소 초기화
        userRoleTextView = findViewById(R.id.userrollTextView)
        usernameTextView = findViewById(R.id.usernameTextView)
        emailTextView = findViewById(R.id.emailTextView)
        phoneTextView = findViewById(R.id.phoneTextView)
        companyNameTextView = findViewById(R.id.companyNameTextView)
        companyCodeTextView = findViewById(R.id.companyCodeTextView)
        timeSalaryTextView = findViewById(R.id.timeSalaryTextView)
        logoutButton = findViewById(R.id.logoutButton)
        editButton = findViewById(R.id.editButton)

        usernameEditText = findViewById(R.id.usernameEditText)
        phoneEditText = findViewById(R.id.phoneEditText)

        loadUserData()

        logoutButton.setOnClickListener {
            logout()
        }

        // 뒤로가기 버튼 리스너 추가
        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        editButton.setOnClickListener {
            toggleEditMode()
        }
    }

    private fun loadUserData() {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", null)

        if (userId == null) {
            Toast.makeText(this, "로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // `UserData`를 활용하여 사용자 데이터 및 회사 정보 로드
        UserData.loadUserCompanyCode(this, userId) { companyCode ->
            if (companyCode == null) {
                Toast.makeText(this, "회사 코드를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                return@loadUserCompanyCode
            }

            // 각 필드를 개별적으로 로드
            UserData.getUserName(companyCode, userId) { username ->
                usernameTextView.text = "이름: ${username ?: "알 수 없음"}"
            }
            UserData.getUserEmail(companyCode, userId) { email ->
                emailTextView.text = "이메일: ${email ?: "알 수 없음"}"
            }

            UserData.getUserPhone(companyCode, userId) { phone ->
                phoneTextView.text = "전화번호: ${phone ?: "알 수 없음"}"
            }

            UserData.getUserRole(companyCode, userId) { role ->
                userRoleTextView.text = role ?: "알 수 없음"

                // 근로자일 경우 시급 표시
                if (role == "근로자") {
                    UserData.getUserSalary(companyCode, userId) { salary ->
                        timeSalaryTextView.text = "시급: ${salary ?: "알 수 없음"}"
                        timeSalaryTextView.visibility = View.VISIBLE
                    }
                } else {
                    timeSalaryTextView.visibility = View.GONE
                }
            }

            UserData.getCompanyName(companyCode) { companyName ->
                companyNameTextView.text = "회사 이름: ${companyName ?: "알 수 없음"}"
            }

            companyCodeTextView.text = "회사 코드: $companyCode"
        }
    }

    private fun toggleEditMode() {
        if (isEditing) {
            saveUserData()
        } else {
            enableEditing(true)
        }
        isEditing = !isEditing
    }

    private fun enableEditing(enable: Boolean) {
        if (enable) {
            usernameTextView.visibility = View.GONE
            phoneTextView.visibility = View.GONE

            usernameEditText.visibility = View.VISIBLE
            phoneEditText.visibility = View.VISIBLE

            usernameEditText.setText(usernameTextView.text.toString().replace("이름: ", ""))
            phoneEditText.setText(phoneTextView.text.toString().replace("전화번호: ", ""))

            editButton.text = "저장하기"
        } else {
            usernameTextView.visibility = View.VISIBLE
            emailTextView.visibility = View.VISIBLE
            phoneTextView.visibility = View.VISIBLE

            usernameEditText.visibility = View.GONE
            phoneEditText.visibility = View.GONE

            editButton.text = "수정하기"
        }
    }

    private fun saveUserData() {
        val newUsername = usernameEditText.text.toString().trim()
        val newPhone = phoneEditText.text.toString().trim()

        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", null) ?: return
        val companyCode = companyCodeTextView.text.toString().replace("회사 코드: ", "")
        val role = userRoleTextView.text.toString()

        val updatedData = mapOf(
            "username" to newUsername,
            "phone" to newPhone
        )

        // `UserData`를 활용하여 데이터 저장
        UserData.saveUserData(companyCode, userId, updatedData, role) { success ->
            if (success) {
                usernameTextView.text = "이름: $newUsername"
                phoneTextView.text = "전화번호: $newPhone"

                enableEditing(false)
                isEditing = false
                Toast.makeText(this, "정보가 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logout() {
        auth.signOut()
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}