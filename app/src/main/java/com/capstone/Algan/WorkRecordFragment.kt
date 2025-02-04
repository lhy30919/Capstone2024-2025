package com.capstone.Algan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class WorkRecordFragment : Fragment(R.layout.fragment_workrecord) {
    private lateinit var btnClockInOut: Button
    private lateinit var btnAttendanceCheck: Button
    private lateinit var tvDate: TextView
    private lateinit var tvClockIn: TextView
    private lateinit var tvClockOut: TextView

    private var isClockedIn = false
    private var clockInTime: String? = null
    private var clockOutTime: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_workrecord, container, false)

        // 뷰 초기화
        btnClockInOut = view.findViewById(R.id.btnClockInOut)
        btnAttendanceCheck = view.findViewById(R.id.btnAttendanceCheck)
        tvDate = view.findViewById(R.id.tvDate)
        tvClockIn = view.findViewById(R.id.tvClockIn)
        tvClockOut = view.findViewById(R.id.tvClockOut)

        // 현재 날짜 표시
        val currentDate = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Date())
        tvDate.text = currentDate

        // 출근/퇴근 버튼 클릭 이벤트 설정
        btnClockInOut.setOnClickListener {
            if (isClockedIn) {
                // 퇴근 처리
                clockOutTime = getCurrentTime()
                tvClockOut.text = "퇴근 시간: $clockOutTime"
                btnClockInOut.text = "출근"
                btnClockInOut.setBackgroundColor(resources.getColor(android.R.color.holo_green_light, null))
                isClockedIn = false
            } else {
                // 출근 처리
                clockInTime = getCurrentTime()
                tvClockIn.text = "출근 시간: $clockInTime"
                btnClockInOut.text = "퇴근"
                btnClockInOut.setBackgroundColor(resources.getColor(android.R.color.holo_red_light, null))
                isClockedIn = true
            }
        }

        return view
    }

    // 현재 시간 구하는 함수
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
        return sdf.format(Date())
    }
}
