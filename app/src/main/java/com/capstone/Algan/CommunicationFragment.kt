package com.capstone.Algan

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.Algan.adapters.ChatAdapter
import com.capstone.Algan.models.Message
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 소통 화면 프래그먼트
class CommunicationFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: Button
    private lateinit var buttonAttach: Button // 사진 첨부 버튼 추가
    private lateinit var imageViewPreview: ImageView
    private lateinit var chatAdapter: ChatAdapter
    private var selectedImageUri: Uri? = null // 사진 URI는 Nullable로 설정

    private val messageList = mutableListOf<Message>()

    private val GALLERY_REQUEST_CODE = 100
    private val CAMERA_REQUEST_CODE = 101

    // 권한 요청을 위한 ActivityResultContracts
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val readPermissionGranted =
                permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
            val cameraPermissionGranted =
                permissions[Manifest.permission.CAMERA] ?: false

            if (readPermissionGranted && cameraPermissionGranted) {
                // 권한이 허용되면 카메라나 갤러리 열기
                showImagePickerDialog()
            } else {
                Toast.makeText(requireContext(), "권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_communication, container, false)

        // UI 초기화
        recyclerView = view.findViewById(R.id.recyclerView_messages)
        editTextMessage = view.findViewById(R.id.editText_message)
        buttonSend = view.findViewById(R.id.button_send)
        buttonAttach = view.findViewById(R.id.button_select_image) // 버튼 추가
        imageViewPreview = view.findViewById(R.id.imageView_preview)

        // 현재 로그인한 사용자 이름 가져오기
        val currentUser = FirebaseAuth.getInstance().currentUser?.displayName ?: "Unknown User"

        // RecyclerView 설정
        chatAdapter = ChatAdapter(messageList, currentUser)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = chatAdapter

        // 메시지 보내기 버튼 클릭 처리
        buttonSend.setOnClickListener {
            val messageText = editTextMessage.text.toString()
            if (messageText.isNotEmpty() || selectedImageUri != null) {
                // 메시지 추가
                val message = Message(
                    content = messageText,
                    timestamp = getCurrentTimestamp(), // 현재 시간 가져오기
                    username = currentUser, // 현재 사용자 이름 설정
                    profileImageUrl = getProfileImageUrl(), // 프로필 이미지 URL 설정
                    imageUri = selectedImageUri?.toString() // 이미지 URI가 있을 경우 추가
                )
                messageList.add(message)

                // RecyclerView 갱신
                chatAdapter.notifyItemInserted(messageList.size - 1)

                // 입력란 비우기
                editTextMessage.text.clear()

                // 이미지 URI 초기화
                selectedImageUri = null
                // 사진 미리보기 초기화 (+보이지 않게 설정)
                imageViewPreview.setImageDrawable(null)
                imageViewPreview.visibility = View.GONE
                // 메시지 자동 스크롤
                recyclerView.scrollToPosition(messageList.size - 1)
            }
        }

        // 사진 첨부 버튼 클릭 처리
        buttonAttach.setOnClickListener {
            // 권한 요청
            requestPermissionLauncher.launch(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.CAMERA
                    )
                } else {
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                    )
                }
            )
        }

        return view
    }

    // 현재 시간 반환 (예시: "2025-02-07 10:00:00" 형식)
    private fun getCurrentTimestamp(): String {
        val currentTime = System.currentTimeMillis()
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(currentTime))
    }

    // 프로필 이미지 URL을 가져오는 방법 (예시, Firebase에서 가져올 수 있음)
    private fun getProfileImageUrl(): String {
        // 예시로 Firebase에서 프로필 이미지 URL을 가져온다고 가정
        return FirebaseAuth.getInstance().currentUser?.photoUrl?.toString() ?: "기본 이미지 URL"
    }

    // 갤러리에서 이미지 선택하기
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    // 카메라로 사진 찍기
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    // 이미지 선택 방법을 고를 수 있는 다이얼로그 표시
    private fun showImagePickerDialog() {
        val options = arrayOf("갤러리에서 선택", "카메라로 사진 찍기")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("사진 선택 방법")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openGallery() // 갤러리 선택
                    1 -> openCamera()  // 카메라 촬영
                }
            }
            .show()
    }

    // 이미지 선택 결과 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        selectedImageUri = uri // 갤러리에서 선택된 이미지 URI
                        // 사진 미리보기 표시
                        imageViewPreview.visibility = View.VISIBLE
                        imageViewPreview.setImageURI(selectedImageUri)
                    }
                }

                CAMERA_REQUEST_CODE -> {
                    data?.extras?.get("data")?.let { image ->
                        val tempUri = getImageUri(image) // 촬영한 사진 URI 변환
                        if (tempUri != Uri.EMPTY) {
                            selectedImageUri = tempUri
                            // 사진 미리보기 표시
                            imageViewPreview.visibility = View.VISIBLE
                            imageViewPreview.setImageURI(selectedImageUri)
                        } else {
                            // 사진 URI가 제대로 생성되지 않은 경우 처리
                            Toast.makeText(requireContext(), "사진 URI 생성 실패", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }
    }

    // 카메라에서 찍은 이미지를 Uri로 변환
    private fun getImageUri(image: Any): Uri {
        val imageBitmap = image as Bitmap
        val path = MediaStore.Images.Media.insertImage(
            requireContext().contentResolver, imageBitmap, "Captured Image", null
        )

        // path가 null인 경우 처리
        return if (path != null) {
            Uri.parse(path)
        } else {
            // path가 null이면 예외를 피하고 기본 Uri를 반환하도록 처리
            Uri.EMPTY
        }
    }
}