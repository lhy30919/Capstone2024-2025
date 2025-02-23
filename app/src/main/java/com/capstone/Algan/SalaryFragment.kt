package com.capstone.Algan

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.capstone.Algan.databinding.FragmentSalaryBinding
import java.util.*

class SalaryFragment : Fragment() {

    private lateinit var etStartTime: TextView
    private lateinit var etEndTime: TextView
    private lateinit var btnSetSalary: Button
    private lateinit var etHourlyRate: EditText
    private lateinit var etDeductions: EditText
    private lateinit var salaryAmount: TextView
    private lateinit var worktime: TextView
    private lateinit var totalWorkHours: TextView
    private lateinit var hourlyRate: TextView
    private lateinit var deductions: TextView
    private lateinit var salaryInputLayout: LinearLayout
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var spinnerWorker: Spinner

    private var isEmployer = false // 사업주 여부

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_salary, container, false)

        // UI 요소 초기화
        etStartTime = view.findViewById(R.id.etStartTime)
        etEndTime = view.findViewById(R.id.etEndTime)
        btnSetSalary = view.findViewById(R.id.btnSetSalary)
        etHourlyRate = view.findViewById(R.id.etHourlyRate)
        etDeductions = view.findViewById(R.id.etDeductions)
        salaryAmount = view.findViewById(R.id.salaryAmount)
        worktime = view.findViewById(R.id.worktime)
        totalWorkHours = view.findViewById(R.id.totalWorkHours)
        hourlyRate = view.findViewById(R.id.hourlyRate)
        deductions = view.findViewById(R.id.deductions)
        salaryInputLayout = view.findViewById(R.id.salaryInputLayout)
        tvStartDate = view.findViewById(R.id.tvStartDate)
        tvEndDate = view.findViewById(R.id.tvEndDate)
        spinnerWorker = view.findViewById(R.id.spinnerWorker)

        // 사업주 여부 체크 (실제 데이터에 맞게 변경 필요)
        isEmployer = true // 예제에서는 사업주라고 가정

        // 사업주일 때만 급여 입력 부분 보이도록
        if (isEmployer) {
            salaryInputLayout.visibility = View.VISIBLE
        } else {
            salaryInputLayout.visibility = View.GONE
        }

        // 날짜 선택 리스너 설정
        tvStartDate.setOnClickListener {
            showDatePicker(tvStartDate)
        }

        tvEndDate.setOnClickListener {
            showDatePicker(tvEndDate)
        }

        // 근로자 선택 (사업주일 때만 활성화)
        if (isEmployer) {
            val workers = listOf("전체","근로자1", "근로자2", "근로자3") // 예시로 근로자 리스트 설정
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, workers)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerWorker.adapter = adapter
        }

        // 출퇴근 시간 선택 리스너 설정
        etStartTime.setOnClickListener {
            showTimePicker(etStartTime)
        }

        etEndTime.setOnClickListener {
            showTimePicker(etEndTime)
        }

        // 급여 계산 버튼 클릭 시
        btnSetSalary.setOnClickListener {
            calculateSalary()
        }

        return view
    }

    // 날짜 선택 다이얼로그
    private fun showDatePicker(targetTextView: TextView) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val formattedDate = String.format("%04d년 %02d월 %02d일", year, month + 1, dayOfMonth)
                targetTextView.text = formattedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    // 타임피커 다이얼로그 표시
    private fun showTimePicker(targetTextView: TextView) {
        val timePickerDialog = TimePickerDialog(
            requireContext(), { _, hourOfDay, minute ->
                val formattedTime = String.format("%02d:%02d", hourOfDay, minute)
                targetTextView.text = formattedTime
            }, 9, 0, true // 기본 시간: 오전 9시
        )
        timePickerDialog.show()
    }

    // 급여 계산
    private fun calculateSalary() {
        val hourlyRateValue = etHourlyRate.text.toString().toDoubleOrNull()
        val deductionsValue = etDeductions.text.toString().toDoubleOrNull()

        if (hourlyRateValue != null && deductionsValue != null) {
            val totalHours = calculateTotalWorkHours()
            val netSalary = calculateNetSalary(hourlyRateValue, deductionsValue, totalHours)
            updateSalaryInfo(netSalary, totalHours)
        } else {
            Toast.makeText(requireContext(), "시급과 공제 비율을 올바르게 입력하세요.", Toast.LENGTH_SHORT).show()
        }
    }

    // 총 근무 시간 계산 (야간 근무 고려)
    private fun calculateTotalWorkHours(): Double {
        val startTime = etStartTime.text.toString()
        val endTime = etEndTime.text.toString()

        if (startTime.isNotEmpty() && endTime.isNotEmpty()) {
            val start = parseTimeToMinutes(startTime)
            val end = parseTimeToMinutes(endTime)

            return if (end >= start) {
                (end - start) / 60.0 // 같은 날 근무
            } else {
                ((24 * 60 - start) + end) / 60.0 // 다음날까지 근무
            }
        }
        return 0.0
    }

    // 시간 문자열을 분 단위로 변환
    private fun parseTimeToMinutes(time: String): Int {
        val timeParts = time.split(":")
        val hours = timeParts[0].toInt()
        val minutes = timeParts[1].toInt()
        return hours * 60 + minutes
    }

    // 실 지급액 계산
    private fun calculateNetSalary(hourlyRate: Double, deductions: Double, totalHours: Double): Double {
        val grossSalary = hourlyRate * totalHours
        return grossSalary - (grossSalary * (deductions / 100))
    }

    // UI 업데이트
    private fun updateSalaryInfo(netSalary: Double, totalHours: Double) {
        salaryAmount.text = "실 지급액: ${netSalary.toInt()}원"
        worktime.text = "출퇴근 시간: ${etStartTime.text} ~ ${etEndTime.text}"
        totalWorkHours.text = "총 근무시간: ${String.format("%.1f", totalHours)}시간"
        hourlyRate.text = "시급: ${etHourlyRate.text}원"
        deductions.text = "공제: ${etDeductions.text}%"
    }
}
