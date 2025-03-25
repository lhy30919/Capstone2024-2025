package com.capstone.Algan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.Algan.adapters.ChatAdapter
import com.capstone.Algan.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class CommunicationFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: Button
    private lateinit var buttonAttach: Button
    private lateinit var imageViewPreview: ImageView
    private lateinit var chatAdapter: ChatAdapter
    private var selectedImageUri: Uri? = null

    private val messageList = mutableListOf<Message>()

    private lateinit var userRole: String
    private lateinit var companyCode: String
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_communication, container, false)

        // UI 초기화
        recyclerView = view.findViewById(R.id.recyclerView_messages)
        editTextMessage = view.findViewById(R.id.editText_message)
        buttonSend = view.findViewById(R.id.button_send)
        buttonAttach = view.findViewById(R.id.button_select_image)
        imageViewPreview = view.findViewById(R.id.imageView_preview)

        // SharedPreferences 초기화
        sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Activity.MODE_PRIVATE)

        // 현재 로그인한 사용자 이름 가져오기 (FirebaseAuth 대신 SharedPreferences 활용)
        val currentUser = getSavedUserName()

        // RecyclerView 설정
        chatAdapter = ChatAdapter(messageList, currentUser)
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
                        companyCode = companySnapshot.key.toString()
                        userRole = "사업주"
                        loadMessages()
                        companyFound = true
                        break
                    } else if (employeesSnapshot.hasChild(userId!!)) {
                        companyCode = companySnapshot.key.toString()
                        userRole = "근로자"
                        loadMessages()
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
                Toast.makeText(requireContext(), "사용자 정보를 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val messageText = editTextMessage.text.toString()
            val currentUserName = getSavedUserName() // SharedPreferences에서 사용자 이름 가져오기


            if (messageText.isNotEmpty() || selectedImageUri != null) {
                val message = Message(
                    content = messageText,
                    timestamp = getCurrentTimestamp(),
                    username = currentUserName,
                    profileImageUrl = getProfileImageUrl(),
                    imageUri = selectedImageUri?.toString(),
                    companyCode = companyCode
                )

                saveMessageToFirebase(message)
                messageList.add(message)
                chatAdapter.notifyItemInserted(messageList.size - 1)

                editTextMessage.text.clear()
                selectedImageUri = null
                imageViewPreview.setImageDrawable(null)
                imageViewPreview.visibility = View.GONE
                recyclerView.scrollToPosition(messageList.size - 1)
            } else {
                Toast.makeText(requireContext(), "메시지를 입력해주세요.", Toast.LENGTH_SHORT).show()
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
            .child("chatMessages").push().key
        if (messageId != null) {
            FirebaseDatabase.getInstance().reference.child("companies").child(companyCode)
                .child("chatMessages").child(messageId).setValue(message)
        }
    }

    // 메시지 로드
    private fun loadMessages() {
        FirebaseDatabase.getInstance().reference.child("companies").child(companyCode)
            .child("chatMessages").addValueEventListener(object : ValueEventListener {
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

    // 프로필 이미지 URL 가져오기
    private fun getProfileImageUrl(): String {
        return FirebaseAuth.getInstance().currentUser?.photoUrl?.toString() ?: "기본 이미지 URL"
    }
}
