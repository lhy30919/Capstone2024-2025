package com.capstone.Algan.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.capstone.Algan.R
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.Algan.adapters.ChatAdapter
import com.capstone.Algan.models.Message

class NoticeBoardFragment : Fragment() {

    // 액티비티에서 프래그먼트 교체하는 함수
    private fun replaceFragment(fragment: Fragment) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(R.id.frame_container, fragment)
        transaction?.commit()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_noticeboard, container, false)

        // 기본 화면을 대타 화면으로 설정
        replaceFragment(DaeTaFragment())

        // 대타 버튼 클릭
        val buttonDaeta = view.findViewById<View>(R.id.button_daeta)
        buttonDaeta.setOnClickListener {
            replaceFragment(DaeTaFragment())
        }

        // 공지 버튼 클릭
        val buttonNotice = view.findViewById<View>(R.id.button_notice)
        buttonNotice.setOnClickListener {
            replaceFragment(NoticeFragment())
        }

        // 소통 버튼 클릭
        val buttonCommunication = view.findViewById<View>(R.id.button_communication)
        buttonCommunication.setOnClickListener {
            replaceFragment(CommunicationFragment())
        }

        return view
    }



    // 대타 화면 프래그먼트
    class DaeTaFragment : Fragment() {

        data class SubstituteRequest(
            val timeRange: String,
            val requesterName: String,
            var acceptedBy: String? = null,
            var isApproved: Boolean = false
        )

        private val substituteList = mutableListOf<SubstituteRequest>()
        private var filteredSubstituteList = mutableListOf<SubstituteRequest>()
        private lateinit var adapter: ArrayAdapter<SubstituteRequest>

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.fragment_daeta, container, false)

            val listView = view.findViewById<ListView>(R.id.listViewSubstituteRequests)
            val noRequestsTextView = view.findViewById<TextView>(R.id.textNoRequests)

            // Adapter 초기화
            adapter = object : ArrayAdapter<SubstituteRequest>(requireContext(), R.layout.item_substitute_request, filteredSubstituteList) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val itemView = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_substitute_request, parent, false)

                    val fullTimeRange = filteredSubstituteList[position].timeRange
                    val datePart = fullTimeRange.substring(0, 10)
                    val timePart = fullTimeRange.substring(11)

                    val substituteDateTextView = itemView.findViewById<TextView>(R.id.textViewSubstituteDate)
                    substituteDateTextView.text = datePart

                    val substituteTimeTextView = itemView.findViewById<TextView>(R.id.textViewSubstituteTime)
                    substituteTimeTextView.text = timePart


                    val requesterNameTextView = itemView.findViewById<TextView>(R.id.textViewRequesterName)
                    requesterNameTextView.text = filteredSubstituteList[position].requesterName

                    val acceptedByTextView = itemView.findViewById<TextView>(R.id.textViewAcceptedBy)
                    val acceptedBy = filteredSubstituteList[position].acceptedBy
                    acceptedByTextView.text = if (acceptedBy != null) "수락자: $acceptedBy" else ""

                    val acceptButton = itemView.findViewById<Button>(R.id.buttonAccept)
                    val approveButton = itemView.findViewById<Button>(R.id.buttonApprove)

                // 수락 버튼 초기 색상 설정2024.02.05 수정.
                    acceptButton.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.holo_green_light)

                // 승인 버튼 초기 색상 설정
                    if (filteredSubstituteList[position].isApproved) {
                        approveButton.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.holo_green_light)
                        approveButton.isEnabled = false // 승인된 후 버튼 비활성화
                    } else {
                        approveButton.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.holo_orange_dark)
                    }


                    // 사업주만 승인 버튼을 누를 수 있도록 설정
                    if (isBusinessOwner()) {
                        approveButton.isEnabled = true
                    } else {
                        approveButton.isEnabled = false
                    }

                    acceptButton.setOnClickListener { handleAcceptRequest(position) }
                    approveButton.setOnClickListener { handleApproveRequest(position, approveButton) }

                    return itemView
                }
            }

            listView.adapter = adapter

            updateRequestsView(noRequestsTextView, listView)

            val buttonRequestSubstitute = view.findViewById<ImageButton>(R.id.buttonRequestSubstitute)
            buttonRequestSubstitute.setOnClickListener {
                showDatePicker()
            }

            // "나의 대타 요청 내역" 버튼 클릭 리스너
            val buttonMySubstituteRequest = view.findViewById<Button>(R.id.buttonMySubstituteRequest)
            buttonMySubstituteRequest.setOnClickListener {
                filterSubstituteRequests(isRequest = true) // 내가 요청한 대타만 필터링
                updateRequestsView(noRequestsTextView, listView)
            }

            // "나의 수락 내역" 버튼 클릭 리스너
            val buttonMySubstituteAccept = view.findViewById<Button>(R.id.buttonMySubstituteAccept)
            buttonMySubstituteAccept.setOnClickListener {
                filterSubstituteRequests(isRequest = false) // 내가 수락한 대타만 필터링
                updateRequestsView(noRequestsTextView, listView)
            }

            return view
        }

        private fun updateRequestsView(noRequestsTextView: TextView, listView: ListView) {
            if (filteredSubstituteList.isEmpty()) {
                listView.visibility = View.GONE
                noRequestsTextView.visibility = View.VISIBLE
            } else {
                listView.visibility = View.VISIBLE
                noRequestsTextView.visibility = View.GONE
            }
        }

        private fun getRequesterName(): String {
            val sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            return sharedPreferences.getString("사용자이름", "미확인된 사용자") ?: "미확인된 사용자"
        }

        private fun isBusinessOwner(): Boolean {
            // TODO: 사용자 역할을 DB나 SharedPreferences에서 확인하여 사업주인지 확인
            val sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            val role = sharedPreferences.getString("userRole", "employee") // 예: "employee", "businessOwner"
            return role == "businessOwner" // 사업주인 경우 true, 아니면 false
        }

        private fun showDatePicker() {
            val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTitleText("대타를 신청할 날짜를 선택하세요")
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val selectedDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date(selection))
                showTimeRangePicker(selectedDate)
            }

            datePicker.show(childFragmentManager, "DATE_PICKER")
        }

        private fun showTimeRangePicker(selectedDate: String) {
            val currentTime = java.util.Calendar.getInstance()
            val hour = currentTime.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = currentTime.get(java.util.Calendar.MINUTE)

            val timePickerView = layoutInflater.inflate(R.layout.dialog_time_picker, null)

            val startHourPicker = timePickerView.findViewById<NumberPicker>(R.id.startHourPicker)
            val startMinutePicker = timePickerView.findViewById<NumberPicker>(R.id.startMinutePicker)
            val endHourPicker = timePickerView.findViewById<NumberPicker>(R.id.endHourPicker)
            val endMinutePicker = timePickerView.findViewById<NumberPicker>(R.id.endMinutePicker)

            startHourPicker.value = hour
            startMinutePicker.value = minute
            endHourPicker.value = hour + 1
            endMinutePicker.value = minute

            startHourPicker.minValue = 0
            startHourPicker.maxValue = 23
            startMinutePicker.minValue = 0
            startMinutePicker.maxValue = 59

            endHourPicker.minValue = 0
            endHourPicker.maxValue = 23
            endMinutePicker.minValue = 0
            endMinutePicker.maxValue = 59

            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("대타를 요청할 시간을 설정하세요")
                .setView(timePickerView)
                .setPositiveButton("확인") { _, _ ->
                    val startHour = startHourPicker.value
                    val startMinute = startMinutePicker.value
                    val endHour = endHourPicker.value
                    val endMinute = endMinutePicker.value

                    val startTime = String.format("%02d:%02d", startHour, startMinute)
                    val endTime = String.format("%02d:%02d", endHour, endMinute)

                    val timeRange = "$selectedDate $startTime ~ $endTime"
                    val requesterName = getRequesterName()
                    val newRequest = SubstituteRequest(timeRange, requesterName)
                    addToSubstituteList(newRequest)
                }
                .setNegativeButton("취소", null)
                .create()

            dialog.show()
        }

        private fun addToSubstituteList(request: SubstituteRequest) {
            substituteList.add(request)
            filterSubstituteRequests(isRequest = true) // 내가 요청한 대타만 필터링
            val listView = view?.findViewById<ListView>(R.id.listViewSubstituteRequests)
            val noRequestsTextView = view?.findViewById<TextView>(R.id.textNoRequests)
            updateRequestsView(noRequestsTextView ?: return, listView ?: return)
        }

        private fun filterSubstituteRequests(isRequest: Boolean) {
            filteredSubstituteList = if (isRequest) {
                // 내가 요청한 대타 요청만 필터링
                substituteList.filter { it.requesterName == getRequesterName() }.toMutableList()
            } else {
                // 내가 수락한 대타 요청만 필터링
                substituteList.filter { it.acceptedBy == getRequesterName() }.toMutableList()
            }

            // 필터링된 목록을 adapter에 반영하여 ListView 갱신
            adapter.clear()
            adapter.addAll(filteredSubstituteList)
            adapter.notifyDataSetChanged()

            val listView = view?.findViewById<ListView>(R.id.listViewSubstituteRequests)
            val noRequestsTextView = view?.findViewById<TextView>(R.id.textNoRequests)
            updateRequestsView(noRequestsTextView ?: return, listView ?: return)
        }

        private fun handleAcceptRequest(position: Int) {
            val requesterName = getRequesterName()
            filteredSubstituteList[position].acceptedBy = requesterName
            filteredSubstituteList[position].isApproved = false // 수락 후 승인 대기 상태로 변경
            adapter.notifyDataSetChanged()
        }

        private fun handleApproveRequest(position: Int, approveButton: Button) {
            filteredSubstituteList[position].isApproved = true
            approveButton.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
            approveButton.isEnabled = false // 승인 후 버튼 비활성화
            adapter.notifyDataSetChanged()
        }
    }


    // 공지 화면 프래그먼트
    class NoticeFragment : Fragment() {

            private lateinit var recyclerView: RecyclerView
            private lateinit var editTextMessage: EditText
            private lateinit var buttonSend: Button
            private lateinit var chatAdapter: ChatAdapter

            private val messageList = mutableListOf<Message>()

            override fun onCreateView(
                inflater: LayoutInflater, container: ViewGroup?,
                savedInstanceState: Bundle?
            ): View? {
                val view = inflater.inflate(R.layout.fragment_notice, container, false)

                // UI 초기화
                recyclerView = view.findViewById(R.id.recyclerView_messages)
                editTextMessage = view.findViewById(R.id.editText_message)
                buttonSend = view.findViewById(R.id.button_send)

                // RecyclerView 설정
                chatAdapter = ChatAdapter(messageList)
                recyclerView.layoutManager = LinearLayoutManager(requireContext())
                recyclerView.adapter = chatAdapter

                // 메시지 보내기 버튼 클릭 처리
                buttonSend.setOnClickListener {
                    val messageText = editTextMessage.text.toString()
                    if (messageText.isNotEmpty()) {
                        // 메시지 추가
                        val message = Message(messageText, "2025-02-07 10:00:00") // 임시 시간
                        messageList.add(message)

                        // RecyclerView 갱신
                        chatAdapter.notifyItemInserted(messageList.size - 1)

                        // 입력란 비우기
                        editTextMessage.text.clear()

                        // 메시지 자동 스크롤
                        recyclerView.scrollToPosition(messageList.size - 1)
                    }
                }

                return view
            }
        }

    // 소통 화면 프래그먼트
    class CommunicationFragment : Fragment() {

        private lateinit var recyclerView: RecyclerView
        private lateinit var editTextMessage: EditText
        private lateinit var buttonSend: Button
        private lateinit var chatAdapter: ChatAdapter

        private val messageList = mutableListOf<Message>()

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.fragment_communication, container, false)

            // UI 초기화
            recyclerView = view.findViewById(R.id.recyclerView_messages)
            editTextMessage = view.findViewById(R.id.editText_message)
            buttonSend = view.findViewById(R.id.button_send)

            // RecyclerView 설정
            chatAdapter = ChatAdapter(messageList)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = chatAdapter

            // 메시지 보내기 버튼 클릭 처리
            buttonSend.setOnClickListener {
                val messageText = editTextMessage.text.toString()
                if (messageText.isNotEmpty()) {
                    // 메시지 추가
                    val message = Message(messageText, "2025-02-07 10:00:00") // 임시 시간
                    messageList.add(message)

                    // RecyclerView 갱신
                    chatAdapter.notifyItemInserted(messageList.size - 1)

                    // 입력란 비우기
                    editTextMessage.text.clear()

                    // 메시지 자동 스크롤
                    recyclerView.scrollToPosition(messageList.size - 1)
                }
            }

            return view
        }
}}
