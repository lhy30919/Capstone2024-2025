package com.capstone.Algan

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Locale

class DaeTaFragment : Fragment() {

    data class SubstituteRequest(
        val timeRange: String = "",
        val requesterName: String = "",
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
        adapter = object : ArrayAdapter<SubstituteRequest>(
            requireContext(), R.layout.item_substitute_request, filteredSubstituteList
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val itemView = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_substitute_request, parent, false)

                // 대타 요청 데이터 설정
                val request = filteredSubstituteList[position]
                val fullTimeRange = request.timeRange
                val datePart = fullTimeRange.substring(0, 10)
                val timePart = fullTimeRange.substring(11)

                // 각 텍스트 뷰에 데이터 설정
                itemView.findViewById<TextView>(R.id.textViewSubstituteDate).text = datePart
                itemView.findViewById<TextView>(R.id.textViewSubstituteTime).text = timePart
                itemView.findViewById<TextView>(R.id.textViewRequesterName).text = request.requesterName

                // 수락자 이름 설정
                val acceptedBy = request.acceptedBy
                itemView.findViewById<TextView>(R.id.textViewAcceptedBy).text =
                    acceptedBy?.let { "수락자: $it" } ?: "수락자: 없음"

                // 버튼 설정
                val acceptButton = itemView.findViewById<Button>(R.id.buttonAccept)

                // 버튼 초기 색상 설정
                acceptButton.backgroundTintList =
                    ContextCompat.getColorStateList(context, android.R.color.holo_green_light)

// 근로자만 하단 전체대타, 나의대타요청, 수락내역, 대타 생성 버튼 보이게
                isEmployee { isEmployee ->
                    acceptButton.isEnabled = isEmployee // 근로자만 승인 버튼 활성화
                    val LinownerDaeta = view.findViewById<LinearLayout>(R.id.LinownerDaeta)
                    LinownerDaeta.visibility = if (isEmployee) View.VISIBLE else View.GONE
                }

                // 사업주만 승인 버튼을 누를 수 있도록 설정


                // 버튼 클릭 리스너 설정
                acceptButton.setOnClickListener { handleAcceptRequest(position) }

                return itemView
            }
        }

        listView.adapter = adapter

        // 기본적으로 모든 대타 요청 불러오기
        loadAllSubstituteRequests()

        // 버튼 클릭 리스너
        val buttonRequestSubstitute = view.findViewById<ImageButton>(R.id.buttonRequestSubstitute)
        buttonRequestSubstitute.setOnClickListener { showDatePicker() }

        // "나의 대타 요청 내역" 버튼 클릭 리스너
        val buttonMySubstituteRequest = view.findViewById<Button>(R.id.buttonMySubstituteRequest)
        buttonMySubstituteRequest.setOnClickListener {
            filterSubstituteRequests("REQUEST") // 내가 요청한 대타만 필터링
            updateRequestsView(noRequestsTextView, listView)
        }

        // "나의 수락 내역" 버튼 클릭 리스너
        val buttonMySubstituteAccept = view.findViewById<Button>(R.id.buttonMySubstituteAccept)
        buttonMySubstituteAccept.setOnClickListener {
            filterSubstituteRequests("ACCEPTED") // 내가 수락한 대타 요청만 필터링
            updateRequestsView(noRequestsTextView, listView)
        }

        // "모든 대타 요청 보기" 버튼 클릭 리스너
        val buttonAllSubstituteRequests = view.findViewById<Button>(R.id.buttonAllSubstituteRequests)
        buttonAllSubstituteRequests.setOnClickListener {
            filterSubstituteRequests("ALL") // 모든 요청 및 수락 정보 포함
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
        return sharedPreferences.getString("username", "미확인된 사용자") ?: "미확인된 사용자"
    }

    private fun getCompanyCode(): String {
        val sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("companyCode", "defaultCompanyCode") ?: "defaultCompanyCode"
    }

    private fun isBusinessOwner(): Boolean {
        val sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val role = sharedPreferences.getString("userRole", "employee") // 예: "employee", "businessOwner"
        return role == "businessOwner" // 사업주인 경우 true, 아니면 false
    }

    private fun showDatePicker() {
        val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
            .setTitleText("대타를 신청할 날짜를 선택하세요")
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date(selection))
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

                val startTime = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute)
                val endTime = String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute)

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
        val companyCode = getCompanyCode() // 회사 코드를 가져오는 함수
        val database = FirebaseDatabase.getInstance()
        val requestsRef = database.getReference("substituteRequests/$companyCode")

        // Firebase에 데이터 저장
        requestsRef.push().setValue(request).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // 새로운 요청을 추가할 때 기존 요청을 삭제하지 않도록 수정
                substituteList.add(request) // 요청 추가
                loadAllSubstituteRequests() // 모든 요청 다시 불러오기
            } else {
                // 에러 처리
                Toast.makeText(requireContext(), "대타 요청 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun loadAllSubstituteRequests() {
        val companyCode = getCompanyCode()
        val database = FirebaseDatabase.getInstance()
        val requestsRef = database.getReference("substituteRequests/$companyCode")

        requestsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                substituteList.clear()
                for (requestSnapshot in snapshot.children) {
                    val request = requestSnapshot.getValue(SubstituteRequest::class.java)
                    if (request != null) {
                        substituteList.add(request) // 모든 대타 요청 추가
                    }
                }

                // 최신 요청이 위로 가도록 정렬
                substituteList.sortByDescending { it.timeRange }

                // 모든 요청을 보여줌
                filterSubstituteRequests("ALL") // 모든 요청 및 수락 정보 포함
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "데이터를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun filterSubstituteRequests(mode: String) {
        filteredSubstituteList = when (mode) {
            "REQUEST" -> {
                // 내가 요청한 대타 요청만 필터링
                substituteList.filter { it.requesterName == getRequesterName() }.toMutableList()
            }
            "ACCEPTED" -> {
                // 내가 수락한 대타 요청만 필터링
                substituteList.filter { it.acceptedBy == getRequesterName() }.toMutableList()
            }
            "ALL" -> {
                // 모든 대타 요청 및 수락 정보 포함
                substituteList.toMutableList()
            }
            else -> mutableListOf() // 기본값
        }

        // 필터링된 목록을 adapter에 반영하여 ListView 갱신
        adapter.clear()
        adapter.addAll(filteredSubstituteList)
        adapter.notifyDataSetChanged()
    }

    private fun handleAcceptRequest(position: Int) {
        val requesterName = getRequesterName()
        val currentRequest = filteredSubstituteList[position]

        // 수락자 설정
        currentRequest.acceptedBy = requesterName
        currentRequest.isApproved = false // 승인 대기 상태로 변경

        // Firebase에서 해당 요청을 업데이트
        val database = FirebaseDatabase.getInstance()
        val requestsRef = database.getReference("substituteRequests/${getCompanyCode()}")

        // 해당 요청을 Firebase에 업데이트
        requestsRef.orderByChild("timeRange").equalTo(currentRequest.timeRange)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Fragment가 여전히 연결되어 있는지 확인
                    if (!isAdded) return

                    // 기존 요청을 Firebase에서 업데이트
                    for (requestSnapshot in snapshot.children) {
                        // Firebase에 요청 업데이트
                        requestSnapshot.ref.child("acceptedBy").setValue(requesterName)
                        requestSnapshot.ref.child("isApproved").setValue(false) // 승인 대기 상태로 변경
                    }

                    // 요청 업데이트 후 다시 불러오기
                    loadAllSubstituteRequests() // 모든 요청 다시 불러오기
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "대타 수락에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            })

        adapter.notifyDataSetChanged() // UI 갱신
    }


    private fun isEmployee(callback: (Boolean) -> Unit) {
        val companyCode = getCompanyCode() // 회사 코드 가져오기
        val database = FirebaseDatabase.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return callback(false) // 로그인한 사용자 UID

        val employeeRef = database.getReference("companies/$companyCode/employees/$userId")

        employeeRef.get().addOnSuccessListener { snapshot ->
            callback(snapshot.exists()) // employees/{userId}가 존재하면 true (근로자)
        }.addOnFailureListener {
            callback(false) // 실패 시 기본값 false (사업주로 간주)
        }
    }

}