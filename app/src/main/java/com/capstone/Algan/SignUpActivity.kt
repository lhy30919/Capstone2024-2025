package com.capstone.Algan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.capstone.Algan.utils.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var companyNameEditText: EditText
    private lateinit var generatedCompanyCodeTextView: TextView
    private lateinit var companyCodeEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // FirebaseAuth 및 Realtime Database 초기화
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val emailEditText: EditText = findViewById(R.id.emailEditText)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val usernameField: EditText = findViewById(R.id.usernameEditText)
        val phoneField: EditText = findViewById(R.id.phoneEditText)
        val roleGroup: RadioGroup = findViewById(R.id.roleGroup)

        companyNameEditText = findViewById(R.id.companyNameEditText)
        companyCodeEditText = findViewById(R.id.companyCodeEditText)
        generatedCompanyCodeTextView = findViewById(R.id.generatedCompanyCodeTextView)

        val signupButton: Button = findViewById(R.id.signupButton)

        // 역할 선택에 따라 입력 필드 가시성 조정
        roleGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.employerRadioButton -> {
                    companyNameEditText.visibility = EditText.VISIBLE
                    companyCodeEditText.visibility = EditText.GONE
                    generatedCompanyCodeTextView.visibility = TextView.VISIBLE
                }

                R.id.employeeRadioButton -> {
                    companyNameEditText.visibility = EditText.GONE
                    companyCodeEditText.visibility = EditText.VISIBLE
                    generatedCompanyCodeTextView.visibility = TextView.GONE
                }
            }
        }

        // 회사 이름 입력 필드에 텍스트 변경 리스너 추가
        companyNameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length >= 1) {
                    val companyCode = generateCompanyCode(s.toString())
                    generatedCompanyCodeTextView.text = "회사 코드: $companyCode"
                } else {
                    generatedCompanyCodeTextView.text = ""
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 뒤로가기 버튼 클릭 리스너 추가
        val backButton: ImageButton = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        signupButton.setOnClickListener {
            val username = usernameField.text.toString()
            val password = passwordEditText.text.toString()
            val email = emailEditText.text.toString()
            val phone = phoneField.text.toString()
            val companyName = companyNameEditText.text.toString()
            val selectedRoleId = roleGroup.checkedRadioButtonId
            val role = findViewById<RadioButton>(selectedRoleId)?.text.toString()
            val companyCode = companyCodeEditText.text.toString()

            // 필드 체크
            if (username.isEmpty() || password.isEmpty() || email.isEmpty() || phone.isEmpty() || role.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 역할에 따라 회원가입 처리
            if (role == "사업주") {
                if (companyName.isEmpty()) {
                    Toast.makeText(this, "회사 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val companyCodeWithSuffix =
                    generatedCompanyCodeTextView.text.toString().replace("회사 코드: ", "")
                signUpAsBusinessOwner(
                    username,
                    password,
                    email,
                    phone,
                    companyName,
                    companyCodeWithSuffix
                )
            } else if (role == "근로자") {
                if (companyCode.isEmpty()) {
                    Toast.makeText(this, "회사 코드를 입력하세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                signUpAsEmployee(username, password, email, phone, companyCode)
            }
        }
    }

    private fun generateCompanyCode(companyName: String): String {
        val companyCode = if (companyName.length < 3) { // 회사 이름이 3자리 미만일 때
            val nameSuffix = companyName.takeLast(1) // 회사 이름 마지막 1자리 가져오고
            val randomLetters =
                ('A'..'Z').random().toString() + ('A'..'Z').random().toString() //랜덤한 두자리 알파벳 추가
            val randomSuffix = (10000..99999).random().toString()
            "$nameSuffix$randomLetters$randomSuffix"
        } else {
            val nameSuffix = companyName.takeLast(3)
            val randomSuffix = (10000..99999).random().toString()
            "$nameSuffix$randomSuffix"
        }
        return companyCode
    }


    private fun signUpAsBusinessOwner(
        username: String,
        password: String,
        email: String,
        phone: String,
        companyName: String,
        companyCode: String
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser!!.uid

                    // 사업주 데이터 객체 생성
                    val businessOwner = BusinessOwner(
                        uid = userId, username = username, phone = phone,
                        email = email, companyName = companyName, companyCode = companyCode
                    )

                    // 공통 User 데이터 객체 생성
                    val user = User(
                        uid = userId, username = username, role = "사업주",
                        phone = phone, email = email, companyCode = companyCode
                    )

                    val databaseRef = database.reference
                    val userRef = databaseRef.child("users").child(userId)  // users/{uid} 저장
                    val companyRef = databaseRef.child("companies").child(companyCode)
                        .child("owner")  // companies/{companyCode}/owner 저장

                    // Firebase에 동시에 저장
                    val updates = hashMapOf<String, Any>(
                        "users/$userId" to user,
                        "companies/$companyCode/owner" to businessOwner
                    )

                    databaseRef.updateChildren(updates)
                        .addOnSuccessListener {
                            Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show()

                            // 사용자 정보를 로컬에 저장
                            saveUserData(username, email, phone, companyName, companyCode)

                            // 로그인 화면으로 이동
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "데이터 저장 실패: ${e.message}", Toast.LENGTH_SHORT)
                                .show()
                        }
                } else {
                    Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }


    private fun signUpAsEmployee(
        username: String,
        password: String,
        email: String,
        phone: String,
        companyCode: String
    ) {
        UserData.isCompanyCodeValid(this, companyCode) { isValid ->
            if (isValid) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser!!.uid

                            // 근로자 데이터 객체 생성
                            val employee = Employee(
                                uid = userId, username = username, phone = phone,
                                email = email, companyCode = companyCode,
                                salary = null, workingHours = null
                            )

                            // 공통 User 데이터 객체 생성
                            val user = User(
                                uid = userId, username = username, role = "근로자",
                                phone = phone, email = email, companyCode = companyCode
                            )

                            val databaseRef = database.reference
                            val userRef =
                                databaseRef.child("users").child(userId)  // users/{uid} 저장
                            val companyRef =
                                databaseRef.child("companies").child(companyCode).child("employees")
                                    .child(userId)  // companies/{companyCode}/employees/{uid} 저장

                            // Firebase에 동시에 저장
                            val updates = hashMapOf<String, Any>(
                                "users/$userId" to user,
                                "companies/$companyCode/employees/$userId" to employee
                            )

                            databaseRef.updateChildren(updates)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "근로자 회원가입 성공!", Toast.LENGTH_SHORT).show()

                                    // 사용자 정보를 로컬에 저장
                                    saveUserData(username, email, phone, null, companyCode)

                                    // 로그인 화면으로 이동
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "데이터 저장 실패: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            Toast.makeText(
                                this,
                                "회원가입 실패: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(this, "유효하지 않은 회사 코드입니다.", Toast.LENGTH_SHORT).show()
            }
        }

    }


    private fun saveUserData(
        username: String,
        email: String,
        phone: String?,
        companyName: String?,
        companyCode: String
    ) {
        val sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putString("username", username)
        editor.putString("email", email)
        editor.putString("phone", phone)
        editor.putString("companyName", companyName)
        editor.putString("companyCode", companyCode)

        val success = editor.commit()
        if (success) {
            Log.d("SignUpActivity", "데이터 저장 성공")
        } else {
            Log.e("SignUpActivity", "데이터 저장 실패")
        }
    }
}
