package com.capstone.Algan

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.capstone.Algan.utils.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class MyPageActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var companyNameTextView: TextView
    private lateinit var companyCodeTextView: TextView
    private lateinit var userRoleTextView: TextView
    private lateinit var logoutButton: Button
    private lateinit var editButton: Button

    private lateinit var usernameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var profileImageView: ImageView
    private lateinit var profileImageView2: ImageView

    private val GALLERY_REQUEST_CODE = 100
    private val CAMERA_REQUEST_CODE = 101

    private var isEditing = false
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_page)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        userRoleTextView = findViewById(R.id.userrollTextView)
        usernameTextView = findViewById(R.id.usernameTextView)
        emailTextView = findViewById(R.id.emailTextView)
        phoneTextView = findViewById(R.id.phoneTextView)
        companyNameTextView = findViewById(R.id.companyNameTextView)
        companyCodeTextView = findViewById(R.id.companyCodeTextView)
        logoutButton = findViewById(R.id.logoutButton)
        editButton = findViewById(R.id.editButton)
        usernameEditText = findViewById(R.id.usernameEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        profileImageView = findViewById(R.id.profileImageView)
        profileImageView2 = findViewById(R.id.profileImageView2)

        profileImageView2.setOnClickListener {
            if (isEditing) showImagePickerDialog()
        }

        loadUserData()

        logoutButton.setOnClickListener { logout() }

        findViewById<ImageButton>(R.id.back_button).setOnClickListener { finish() }

        editButton.setOnClickListener { toggleEditMode() }
    }

    private fun loadUserData() {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", null) ?: return

        UserData.loadUserCompanyCode(this, userId) { companyCode ->
            if (companyCode == null) return@loadUserCompanyCode

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
            }
            UserData.getCompanyName(companyCode) { name ->
                companyNameTextView.text = "회사 이름: ${name ?: "알 수 없음"}"
            }
            companyCodeTextView.text = "회사 코드: $companyCode"

            // 고정 경로에서 프로필 이미지 다운로드 URL 받아오기
            val imageRef = storage.reference.child("profile_images/$companyCode/$userId.jpg")
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                Log.d("MyPageActivity", "프로필 이미지 다운로드 URL 성공: $uri")
                Glide.with(this).load(uri).into(profileImageView)
                Glide.with(this).load(uri).into(profileImageView2)
            }.addOnFailureListener { e ->
                Log.e("MyPageActivity", "프로필 이미지 다운로드 URL 실패", e)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    imageUri = data.data
                    profileImageView.setImageURI(imageUri)
                    profileImageView2.setImageURI(imageUri)
                }
                CAMERA_REQUEST_CODE -> {
                    val bitmap = data.extras?.get("data") as? Bitmap
                    bitmap?.let {
                        val uri = getImageUriFromBitmap(this, it)
                        imageUri = uri
                        profileImageView.setImageBitmap(it)
                        profileImageView2.setImageBitmap(it)
                    }
                }
            }
        }
    }

    private fun toggleEditMode() {
        if (isEditing) saveUserData() else enableEditing(true)
        isEditing = !isEditing
    }

    private fun enableEditing(enable: Boolean) {
        profileImageView.visibility = if (enable) View.GONE else View.VISIBLE
        profileImageView2.visibility = if (enable) View.VISIBLE else View.GONE
        usernameTextView.visibility = if (enable) View.GONE else View.VISIBLE
        phoneTextView.visibility = if (enable) View.GONE else View.VISIBLE
        usernameEditText.visibility = if (enable) View.VISIBLE else View.GONE
        phoneEditText.visibility = if (enable) View.VISIBLE else View.GONE
        editButton.text = if (enable) "저장하기" else "수정하기"

        if (enable) {
            usernameEditText.setText(usernameTextView.text.toString().replace("이름: ", ""))
            phoneEditText.setText(phoneTextView.text.toString().replace("전화번호: ", ""))
        }
    }

    private fun saveUserData() {
        val username = usernameEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", null) ?: return
        val companyCode = companyCodeTextView.text.toString().replace("회사 코드: ", "")
        val role = userRoleTextView.text.toString()

        val data = mapOf("username" to username, "phone" to phone)

        val uploadProfileImage = {
            imageUri?.let {
                val ref = storage.reference.child("profile_images/$companyCode/$userId.jpg")
                val progressDialog = ProgressDialog(this).apply {
                    setMessage("이미지 업로드 중...")
                    setCancelable(false)
                    show()
                }
                ref.putFile(it)
                    .addOnSuccessListener {
                        Log.d("MyPageActivity", "프로필 이미지 업로드 성공")
                        Toast.makeText(this, "이미지 업로드 성공", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("MyPageActivity", "프로필 이미지 업로드 실패", e)
                        Toast.makeText(this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                    }
                    .addOnCompleteListener {
                        progressDialog.dismiss()
                    }
            }
        }

        UserData.saveUserData(companyCode, userId, data, role) { success ->
            if (success) {
                usernameTextView.text = "이름: $username"
                phoneTextView.text = "전화번호: $phone"
                uploadProfileImage()
                enableEditing(false)
                isEditing = false
                Toast.makeText(this, "정보가 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getImageUriFromBitmap(context: Context, bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "temp", null)
        return Uri.parse(path)
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("갤러리에서 선택", "카메라로 사진 찍기")
        android.app.AlertDialog.Builder(this)
            .setTitle("사진 선택 방법")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openCamera()
                }
            }.show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
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
