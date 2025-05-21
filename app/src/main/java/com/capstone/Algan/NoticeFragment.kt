package com.capstone.Algan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.Algan.adapters.ChatAdapter
import com.capstone.Algan.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class NoticeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: Button
    private lateinit var buttonAttach: Button
    private lateinit var imageViewPreview: ImageView
    private lateinit var chatAdapter: ChatAdapter
    private var selectedImageUri: Uri? = null
    private val storage = FirebaseStorage.getInstance()

    private val messageList = mutableListOf<Message>()

    private lateinit var userRole: String
    private lateinit var companyCode: String
    private lateinit var sharedPreferences: SharedPreferences
    // 요청 코드 상수 정의
    private val GALLERY_REQUEST_CODE = 100
    private val CAMERA_REQUEST_CODE = 101

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
            } else {
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            }

            if (isGranted) {
                showImagePickerDialog()
            } else {
                Toast.makeText(requireContext(), "권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notice, container, false)

        // UI 초기화
        recyclerView = view.findViewById(R.id.recyclerView_messages)
        editTextMessage = view.findViewById(R.id.editText_message)
        buttonSend = view.findViewById(R.id.button_send)
        buttonAttach = view.findViewById(R.id.button_select_image)
        imageViewPreview = view.findViewById(R.id.imageView_preview)

        // SharedPreferences 초기화
        sharedPreferences =
            requireContext().getSharedPreferences("UserPreferences", Activity.MODE_PRIVATE)

        // 현재 로그인한 사용자 이름 가져오기 (FirebaseAuth 대신 SharedPreferences 활용)
        val currentUser = getSavedUserName()

        // RecyclerView 설정
        chatAdapter = ChatAdapter(messageList, currentUser, forceLeftGravity = true)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = chatAdapter

        // Firebase에서 회사 코드 및 역할 가져오기
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val employeeDatabase = FirebaseDatabase.getInstance().reference.child("companies")

        employeeDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var companyFound = false
                for (companySnapshot in snapshot.children) {
                    val ownerUid = companySnapshot.child("owner/uid").getValue(String::class.java)
                    val employeesSnapshot = companySnapshot.child("employees")

                    if (userId == ownerUid) {
                        // 사업주인 경우
                        companyCode = companySnapshot.key.toString()
                        userRole = "사업주" // 역할 설정
                        loadMessages() // 메시지 로드
                        companyFound = true
                        break
                    } else if (employeesSnapshot.hasChild(userId!!)) {
                        // 근로자인 경우
                        companyCode = companySnapshot.key.toString()
                        userRole = "근로자" // 역할 설정
                        loadMessages() // 메시지 로드

                        // 근로자는 채팅 안보이게
                        view?.findViewById<LinearLayout>(R.id.LinChat)?.visibility = View.GONE
                        editTextMessage.isClickable = false
                        editTextMessage.isFocusable = false

                        companyFound = true
                        break
                    }
                }
                if (!companyFound) {
                    Toast.makeText(requireContext(), "사용자가 속한 회사가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error retrieving user role: ${error.message}")
            }
        })

        buttonSend.setOnClickListener {
            if (!::userRole.isInitialized) {
                Toast.makeText(
                    requireContext(),
                    "사용자 정보를 불러오는 중입니다. 잠시 후 다시 시도해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val messageText = editTextMessage.text.toString()
            val currentUserName = getSavedUserName() // SharedPreferences에서 사용자 이름 가져오기
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""


            getProfileImageUrl(companyCode, currentUserId) { profileImageUrl ->

                val message = Message(
                    content = messageText,
                    timestamp = getCurrentTimestamp(),
                    username = currentUserName,
                    profileImageUrl = profileImageUrl,
                    imageUri = selectedImageUri?.toString(),
                    companyCode = companyCode,
                    userId = currentUserId  // 이 부분 추가
                )


                saveMessageToFirebase(message)
                messageList.add(message)
                chatAdapter.notifyItemInserted(messageList.size - 1)

                editTextMessage.text.clear()
                selectedImageUri = null
                imageViewPreview.setImageDrawable(null)
                imageViewPreview.visibility = View.GONE
                recyclerView.scrollToPosition(messageList.size - 1)
            }
        }


        // 사진 첨부 버튼 클릭 처리
        buttonAttach.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permission = Manifest.permission.READ_MEDIA_IMAGES
                if (requireContext().checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    showImagePickerDialog()
                } else {
                    requestPermissionLauncher.launch(arrayOf(permission))
                }
            } else {
                val permission = Manifest.permission.READ_EXTERNAL_STORAGE
                if (requireContext().checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    showImagePickerDialog()
                } else {
                    requestPermissionLauncher.launch(arrayOf(permission))
                }
            }
        }

        return view
    }

    // SharedPreferences에서 사용자 이름 가져오는 함수
    private fun getSavedUserName(): String {
        return sharedPreferences.getString("username", "Unknown User") ?: "Unknown User"
    }

    // Firebase에 메시지 저장
    private fun saveMessageToFirebase(message: Message) {
        val messageId = FirebaseDatabase.getInstance().reference.child("companies").child(companyCode)
            .child("messages").push().key
        if (messageId != null) {
            FirebaseDatabase.getInstance().reference.child("companies").child(companyCode)
                .child("messages").child(messageId).setValue(message)
        }
    }

    // 메시지 로드
    private fun loadMessages() {
        FirebaseDatabase.getInstance().reference.child("companies").child(companyCode)
            .child("messages").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(Message::class.java)
                        if (message != null) {
                            messageList.add(message)
                        }
                    }
                    chatAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error loading messages: ${error.message}")
                }
            })
    }

    // 현재 시간 반환
    private fun getCurrentTimestamp(): String {

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
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

    private fun showImagePickerDialog() {
        val options = arrayOf("갤러리에서 선택", "카메라로 사진 찍기")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("사진 선택 방법")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openCamera()
                }
            }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        selectedImageUri = uri
                        imageViewPreview.visibility = View.VISIBLE
                        imageViewPreview.setImageURI(selectedImageUri)
                    }
                }

                CAMERA_REQUEST_CODE -> {
                    data?.extras?.get("data")?.let { image ->
                        val tempUri = getImageUri(image)
                        if (tempUri != Uri.EMPTY) {
                            selectedImageUri = tempUri
                            imageViewPreview.visibility = View.VISIBLE
                            imageViewPreview.setImageURI(selectedImageUri)
                        } else {
                            Toast.makeText(requireContext(), "사진 URI 생성 실패", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }
    }
    // 프로필 이미지 URL 가져오기
    private fun getProfileImageUrl(companyCode: String, userId: String, onComplete: (String) -> Unit) {
        val ref = storage.reference.child("profile_images/$companyCode/$userId.jpg")

        ref.downloadUrl
            .addOnSuccessListener { uri ->
                onComplete(uri.toString())  // 성공하면 URL 반환
            }
            .addOnFailureListener {
                onComplete("기본 이미지 URL") // 실패 시 기본 이미지 URL 반환
            }
    }

    private fun getImageUri(image: Any): Uri {
        val imageBitmap = image as Bitmap
        val path = MediaStore.Images.Media.insertImage(
            requireContext().contentResolver, imageBitmap, "Captured Image", null
        )

        return if (path != null) {
            Uri.parse(path)
        } else {
            Uri.EMPTY
        }
    }
}
