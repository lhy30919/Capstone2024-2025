package com.capstone.Algan

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MyPageActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var companyNameTextView: TextView
    private lateinit var companyCodeTextView: TextView
    private lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_page)

        // FirebaseAuth 및 Realtime Database 초기화
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        usernameTextView = findViewById(R.id.usernameTextView)
        emailTextView = findViewById(R.id.emailTextView)
        phoneTextView = findViewById(R.id.phoneTextView)
        companyNameTextView = findViewById(R.id.companyNameTextView)
        companyCodeTextView = findViewById(R.id.companyCodeTextView)
        logoutButton = findViewById(R.id.logoutButton)

        // 사용자 정보 가져오기
        loadUserData()

        // 로그아웃 버튼 클릭 리스너
        logoutButton.setOnClickListener {
            logout()
        }
    }

    /**
     * SharedPreferences와 Firebase에서 사용자 정보를 불러오는 함수
     */
    private fun loadUserData() {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)

        val userId = sharedPreferences.getString("userId", null) // 로그인한 사용자의 UID
        if (userId == null) {
            Toast.makeText(this, "로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseDatabase.getInstance()
        val companiesRef = database.getReference("companies")

        companiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (companySnapshot in snapshot.children) {
                    val ownerSnapshot = companySnapshot.child("owner")

                    // 🔹 [1] 사업주(owner) 확인
                    val storedOwnerUid = ownerSnapshot.child("uid").getValue(String::class.java)
                    if (storedOwnerUid == userId) {
                        loadUserInfo(ownerSnapshot, "사업주")
                        return
                    }

                    // 🔹 [2] 근로자(employee) 확인
                    val employeesSnapshot = companySnapshot.child("employees")
                    for (employeeSnapshot in employeesSnapshot.children) {
                        val storedEmployeeUid = employeeSnapshot.child("uid").getValue(String::class.java)


                        if (storedEmployeeUid == userId) {
                            loadUserInfo(employeeSnapshot, "근로자")
                            return
                        }
                    }
                }

                // 🔹 [3] 사용자 정보를 찾을 수 없을 경우
                Log.e("MyPageActivity", "Firebase에서 사용자 정보를 찾을 수 없음")
                Toast.makeText(this@MyPageActivity, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MyPageActivity", "Firebase 데이터 불러오기 실패: ${error.message}")
                Toast.makeText(this@MyPageActivity, "데이터를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun loadUserInfo(userSnapshot: DataSnapshot, role: String) {
        val username = userSnapshot.child("username").getValue(String::class.java) ?: "알 수 없음"
        val email = userSnapshot.child("email").getValue(String::class.java) ?: "알 수 없음"
        val phone = userSnapshot.child("phone").getValue(String::class.java) ?: "알 수 없음"
        val companyName = userSnapshot.child("companyName").getValue(String::class.java) ?: "알 수 없음"
        val companyCode = userSnapshot.child("companyCode").getValue(String::class.java) ?: "알 수 없음"

        // 🔹 UI 업데이트
        usernameTextView.text = "이름: $username"
        emailTextView.text = "이메일: $email"
        phoneTextView.text = "전화번호: $phone"
        companyNameTextView.text = "회사 이름: $companyName"
        companyCodeTextView.text = "회사 코드: $companyCode"

        Log.d("MyPageActivity", "사용자($role) 데이터 불러오기 성공!")
    }


    /**
     * 로그아웃 처리 함수
     */
    private fun logout() {
        auth.signOut()

        // SharedPreferences 초기화
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

        // 로그인 화면으로 이동
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // 현재 Activity 종료
    }
}
