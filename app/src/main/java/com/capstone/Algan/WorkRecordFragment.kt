package com.capstone.Algan

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
    private lateinit var tvStartDate: TextView // 변경된 부분
    private lateinit var tvEndDate: TextView // 변경된 부분
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
        tvStartDate = view.findViewById(R.id.tvStartDate) // 변경된 부분
        tvEndDate = view.findViewById(R.id.tvEndDate) // 변경된 부분
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

        // 날짜 선택을 위한 DatePickerDialog 설정
        tvStartDate.setOnClickListener {
            showDatePicker { date ->
                tvStartDate.text = date
                filterRecordsByDate()
            }
        }

        tvEndDate.setOnClickListener {
            showDatePicker { date ->
                tvEndDate.text = date
                filterRecordsByDate()
            }
        }

        return view
    }

    // 날짜 선택을 위한 DatePickerDialog 표시
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%04d년 %02d월 %02d일", selectedYear, selectedMonth + 1, selectedDay)
            onDateSelected(formattedDate)
        }, year, month, day)

        datePickerDialog.show()
    }

    // 날짜 범위에 맞춰 기록 필터링
    private fun filterRecordsByDate() {
        val startDate = tvStartDate.text.toString()
        val endDate = tvEndDate.text.toString()

        val filteredRecords = records.filter {
            val recordDate = it.date
            (startDate.isEmpty() || recordDate >= startDate) && (endDate.isEmpty() || recordDate <= endDate)
        }

        // 필터링된 기록을 RecyclerView에 표시
        recordAdapter.updateRecords(filteredRecords)
    }

    private fun saveAttendance(date: String, clockIn: String, clockOut: String) {
        // 새로운 출퇴근 기록을 리스트에 추가
        val workedHours = calculateWorkedHours(clockIn, clockOut)
        val newRecord = AttendanceRecordWithTime(records.size + 1, date, clockIn, clockOut, workedHours)
        records.add(newRecord)
        recordAdapter.notifyItemInserted(records.size - 1)  // 새로운 항목 추가 후 RecyclerView 갱신
    }

    private fun loadAttendance(date: String): Pair<String, String>? {
        // 리스트에서 해당 날짜의 출퇴근 기록 찾기
        for (record in records) {
            if (record.date == date) {
                return Pair(record.clockIn, record.clockOut)
            }
        }
        return null
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
        return sdf.format(Date())
    }

    // 근무 시간을 계산하는 함수
    private fun calculateWorkedHours(clockIn: String, clockOut: String): String {
        val format = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
        val inTime = format.parse(clockIn)
        val outTime = format.parse(clockOut)
        val diffInMillis = outTime.time - inTime.time
        val hours = (diffInMillis / (1000 * 60 * 60)).toInt()
        val minutes = ((diffInMillis / (1000 * 60)) % 60).toInt()
        return String.format("%02d:%02d", hours, minutes)
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
    class RecordAdapter(private val records: MutableList<AttendanceRecordWithTime>) :
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

        // RecyclerView의 기록 갱신
        fun updateRecords(newRecords: List<AttendanceRecordWithTime>) {
            records.clear()
            records.addAll(newRecords)
            notifyDataSetChanged()
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
