package com.capstone.Algan

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class WorkRecordFragment : Fragment(R.layout.fragment_workrecord) {

    private lateinit var btnClockInOut: Button
    private lateinit var tvDate: TextView
    private lateinit var tvClockIn: TextView
    private lateinit var tvClockOut: TextView
    private lateinit var calendarView: CalendarView

    private var isClockedIn = false
    private var clockInTime: String? = null
    private var clockOutTime: String? = null
    private val attendanceData: MutableMap<String, Pair<String, String>> = mutableMapOf() // 출퇴근 기록 저장

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_workrecord, container, false)

        // 뷰 초기화
        btnClockInOut = view.findViewById(R.id.btnClockInOut)
        tvDate = view.findViewById(R.id.tvDate)
        tvClockIn = view.findViewById(R.id.tvClockIn)
        tvClockOut = view.findViewById(R.id.tvClockOut)
        calendarView = view.findViewById(R.id.calendarView)

        // 현재 날짜 표시
        val currentDate = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Date())
        tvDate.text = currentDate

        // 출근/퇴근 버튼 클릭 이벤트 설정
        btnClockInOut.setOnClickListener {
            val selectedDate = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Date()) // 오늘 날짜를 "yyyy년 MM월 dd일" 형식으로 저장
            if (isClockedIn) {
                // 퇴근 처리
                clockOutTime = getCurrentTime()
                tvClockOut.text = "퇴근 시간: $clockOutTime"
                attendanceData[selectedDate] = Pair(clockInTime ?: "미등록", clockOutTime ?: "미등록")
                Log.d("WorkRecordFragment", "Saved Attendance: $selectedDate - $clockInTime / $clockOutTime") // 디버깅 로그 추가
                btnClockInOut.text = "출근"
                btnClockInOut.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_light)
                isClockedIn = false
            } else {
                // 출근 처리
                clockInTime = getCurrentTime()
                tvClockIn.text = "출근 시간: $clockInTime"
                btnClockInOut.text = "퇴근"
                btnClockInOut.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_light)
                isClockedIn = true
            }
        }
        // 캘린더 날짜 선택 시 출퇴근 기록 창 나타남
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = String.format("%04d년 %02d월 %02d일", year, month + 1, dayOfMonth) // "yyyy년 MM월 dd일" 형식으로 선택된 날짜 저장
            tvDate.text = selectedDate

            // 출퇴근 기록
            showAttendanceDialog(selectedDate)
        }

        return view
    }

    // 출퇴근 기록 창
    private fun showAttendanceDialog(date: String) {
        val attendance = attendanceData[date]
        val message = if (attendance != null) {
            "출근 시간: ${attendance.first}\n퇴근 시간: ${attendance.second}"
        } else {
            "출퇴근 기록이 없습니다"
        }
        
        Log.d("WorkRecordFragment", "Selected Date: $date, Attendance: $message")

        AlertDialog.Builder(requireContext())
            .setTitle("$date 출퇴근 기록")
            .setMessage(message)
            .setPositiveButton("확인", null)
            .show()
    }

    // 현재 시간
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
        return sdf.format(Date())
    }
}
