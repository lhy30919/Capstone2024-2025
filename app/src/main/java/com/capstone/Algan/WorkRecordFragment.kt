package com.capstone.Algan

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.Algan.databinding.FragmentWorkrecordBinding
import java.text.SimpleDateFormat
import java.util.*

class WorkRecordFragment : Fragment(R.layout.fragment_workrecord) {

    private lateinit var btnClockInOut: Button
    private lateinit var tvDate: TextView
    private lateinit var tvClockIn: TextView
    private lateinit var tvClockOut: TextView
    private lateinit var calendarView: CalendarView
    private lateinit var recyclerView: RecyclerView

    private var isClockedIn = false
    private var clockInTime: String? = null
    private var clockOutTime: String? = null

    private lateinit var recordAdapter: RecordAdapter
    private val records = mutableListOf<AttendanceRecordWithTime>()

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
        recyclerView = view.findViewById(R.id.recyclerViewRecords)

        // RecyclerView 설정
        recordAdapter = RecordAdapter(records)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = recordAdapter

        // 현재 날짜 표시
        val currentDate = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Date())
        tvDate.text = currentDate

        // 오늘 기록 불러와서 화면에 표시
        val todayRecord = loadAttendance(currentDate)
        if (todayRecord != null) {
            tvClockIn.text = "출근 시간: ${todayRecord.first}"
            tvClockOut.text = "퇴근 시간: ${todayRecord.second}"

            isClockedIn = todayRecord.second == "미등록"
            if (isClockedIn) {
                clockInTime = todayRecord.first
                btnClockInOut.text = "퇴근"
                btnClockInOut.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_light)
            } else {
                btnClockInOut.text = "출근"
                btnClockInOut.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_light)
            }
        }

        // 출근/퇴근 버튼 클릭 이벤트 설정
        btnClockInOut.setOnClickListener {
            val selectedDate = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Date())
            if (isClockedIn) {
                // 퇴근 처리
                clockOutTime = getCurrentTime()
                tvClockOut.text = "퇴근 시간: $clockOutTime"
                saveAttendance(selectedDate, clockInTime ?: "미등록", clockOutTime ?: "미등록")
                btnClockInOut.text = "출근"
                btnClockInOut.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_light)
                isClockedIn = false
            } else {
                // 출근 처리
                clockInTime = getCurrentTime()
                tvClockIn.text = "출근 시간: $clockInTime"
                clockOutTime = null
                tvClockOut.text = "퇴근 시간: 미등록"
                btnClockInOut.text = "퇴근"
                btnClockInOut.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_light)
                isClockedIn = true
            }
        }

        // 캘린더 날짜 선택 시 출퇴근 기록 창 나타남
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = String.format("%04d년 %02d월 %02d일", year, month + 1, dayOfMonth)
            tvDate.text = selectedDate
            showAttendanceDialog(selectedDate)
        }

        return view
    }

    private fun showAttendanceDialog(date: String) {
        val attendance = loadAttendance(date)
        val message = if (attendance != null) {
            "출근 시간: ${attendance.first}\n퇴근 시간: ${attendance.second}"
        } else {
            "출퇴근 기록이 없습니다"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("$date 출퇴근 기록")
            .setMessage(message)
            .setPositiveButton("확인", null)
            .show()
    }

    private fun saveAttendance(date: String, clockIn: String, clockOut: String) {
        val sharedPref = requireActivity().getSharedPreferences("attendanceData", android.content.Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("${date}_clockIn", clockIn)
            putString("${date}_clockOut", clockOut)
            apply()
        }
    }

    private fun loadAttendance(date: String): Pair<String, String>? {
        val sharedPref = requireActivity().getSharedPreferences("attendanceData", android.content.Context.MODE_PRIVATE)
        val clockIn = sharedPref.getString("${date}_clockIn", null)
        val clockOut = sharedPref.getString("${date}_clockOut", null)
        return if (clockIn != null && clockOut != null) {
            Pair(clockIn, clockOut)
        } else {
            null
        }
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
        return sdf.format(Date())
    }

    // 데이터 클래스 정의
    data class AttendanceRecordWithTime(
        val id: Int,
        val date: String,
        val clockIn: String, // 출근 시간
        val clockOut: String, // 퇴근 시간
        val workedHours: String // 근무 시간
    )

    // 어댑터 정의
    class RecordAdapter(private val records: List<AttendanceRecordWithTime>) :
        RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_work_record, parent, false)
            return RecordViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
            val record = records[position]
            holder.tvNumber.text = (position + 1).toString() // 번호 1부터 시작
            holder.tvDate.text = record.date
            holder.tvClockIn.text = formatTimeForTable(record.clockIn)
            holder.tvClockOut.text = formatTimeForTable(record.clockOut)
            holder.tvWorkedHours.text = record.workedHours
        }

        override fun getItemCount(): Int {
            return records.size
        }

        inner class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvNumber: TextView = itemView.findViewById(R.id.tvNumber)
            val tvDate: TextView = itemView.findViewById(R.id.tvDate)
            val tvClockIn: TextView = itemView.findViewById(R.id.tvClockIn)
            val tvClockOut: TextView = itemView.findViewById(R.id.tvClockOut)
            val tvWorkedHours: TextView = itemView.findViewById(R.id.tvWorkedHours)
        }

        // 표에 시간 형식 변경: 시:분만 표시
        private fun formatTimeForTable(time: String): String {
            val timeParts = time.split(":")
            return if (timeParts.size >= 2) {
                "${timeParts[0]}:${timeParts[1]}" // 시:분만 반환
            } else {
                time
            }
        }
    }
}
