package com.capstone.Algan

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.Algan.databinding.FragmentSalaryBinding
import java.util.Calendar

class SalaryFragment : Fragment() {

    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var spinnerWorker: Spinner
    private lateinit var salaryAmount: TextView
    private lateinit var worktime: TextView
    private lateinit var totalWorkHours: TextView
    private lateinit var hourlyRate: TextView
    private lateinit var deductions: TextView
    private lateinit var recyclerView: RecyclerView

    private lateinit var workerList: List<Worker> // 근로자 목록
    private lateinit var workRecordAdapter: WorkRecordAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentSalaryBinding.inflate(inflater, container, false)

        tvStartDate = binding.tvStartDate
        tvEndDate = binding.tvEndDate
        spinnerWorker = binding.spinnerWorker
        salaryAmount = binding.salaryAmount
        worktime = binding.worktime
        totalWorkHours = binding.totalWorkHours
        hourlyRate = binding.hourlyRate
        deductions = binding.deductions
        recyclerView = binding.recyclerViewRecords

        recyclerView.layoutManager = LinearLayoutManager(context)

        // 근로자 리스트 설정 (예시로 근로자 데이터 추가)
        workerList = listOf(
            Worker("근로자 1", "09:00 ~ 18:00", 8.0, 10000.0, 5.0),
            Worker("근로자 2", "09:00 ~ 18:00", 8.0, 12000.0, 5.0),
            Worker("근로자 3", "09:00 ~ 18:00", 8.0, 15000.0, 10.0),
        )

        // Spinner Adapter 설정
        val workerNames = workerList.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, workerNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWorker.adapter = adapter

        // RecyclerView Adapter 설정
        workRecordAdapter = WorkRecordAdapter(emptyList()) // 초기 빈 리스트
        recyclerView.adapter = workRecordAdapter

        // 급여 입력 다이얼로그 버튼 클릭 리스너
        val btnShowSalaryInput = binding.btnShowSalaryInput
        btnShowSalaryInput.setOnClickListener {
            showSalaryInputDialog()
        }

        // 근로자 선택 시 처리
        spinnerWorker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedWorker = workerList[position]

                // 선택된 근로자의 기록 갱신 (예시)
                updateWorkRecords(selectedWorker)
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // 아무것도 선택되지 않았을 때의 처리
            }
        }

        // 날짜 선택을 위한 DatePickerDialog 설정
        tvStartDate.setOnClickListener {
            showDatePicker { date ->
                tvStartDate.text = date
                //TODO:: 급여 목록 필터
            }
        }

        tvEndDate.setOnClickListener {
            showDatePicker { date ->
                tvEndDate.text = date
                //TODO:: 급여 목록 필터
            }
        }

        return binding.root
    }


    private fun showSalaryInputDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_salary_input)

        val etHourlyRate: EditText = dialog.findViewById(R.id.etHourlyRate)
        val etDeductions: EditText = dialog.findViewById(R.id.etDeductions)
        val etWorkHours: EditText = dialog.findViewById(R.id.etWorkHours)
        val btnSetSalary: Button = dialog.findViewById(R.id.btnSetSalary)

        btnSetSalary.setOnClickListener {
            val hourlyRateValue = etHourlyRate.text.toString().toDoubleOrNull() ?: 0.0
            val deductionsValue = etDeductions.text.toString().toDoubleOrNull() ?: 0.0
            val workHoursValue = etWorkHours.text.toString().toDoubleOrNull() ?: 0.0

            val totalSalary = hourlyRateValue * workHoursValue
            val deductionsAmount = totalSalary * (deductionsValue / 100)
            val finalSalary = totalSalary - deductionsAmount

            // 급여 계산 후 화면에 표시
            salaryAmount.text = "실 지급액: ${finalSalary}원"
            worktime.text = "근무시간: 09:00 ~ 18:00" // 예시
            totalWorkHours.text = "총 근무시간: ${workHoursValue}시간"
            hourlyRate.text = "시급: ${hourlyRateValue}원"
            deductions.text = "공제: ${deductionsValue}%"

            // 다이얼로그 닫기
            dialog.dismiss()
        }
        // 다이얼로그 너비 match_parent
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
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

    // 선택된 근로자의 근무 기록을 업데이트하는 메서드
    private fun updateWorkRecords(worker: Worker) {
        val workRecords = listOf(
            mapOf(
                "number" to "1",
                "workerName" to worker.name,
                "date" to "2025년 02월 16일",
                "clockIn" to "09:00",
                "clockOut" to "18:00",
                "workedHours" to worker.workTime,
                "hours" to worker.totalWorkHours.toString()
            )
        )
        workRecordAdapter.updateData(workRecords)
    }

}

// 근로자 데이터 클래스
data class Worker(
    val name: String,
    val workTime: String,
    val totalWorkHours: Double,
    val hourlyRate: Double,
    val deductions: Double
)

// WorkRecordAdapter 수정
class WorkRecordAdapter(private var workRecordList: List<Map<String, String>>) :
    RecyclerView.Adapter<WorkRecordAdapter.WorkRecordViewHolder>() {

    inner class WorkRecordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumber: TextView = view.findViewById(R.id.tvNumber)
        val tvWorkerName: TextView = view.findViewById(R.id.tvWorkerName)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvClockIn: TextView = view.findViewById(R.id.tvClockIn)
        val tvClockOut: TextView = view.findViewById(R.id.tvClockOut)
        val tvWorkedHours: TextView = view.findViewById(R.id.tvWorkedHours)
        val tvHours: TextView = view.findViewById(R.id.tvHours)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkRecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_work_record, parent, false)
        return WorkRecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkRecordViewHolder, position: Int) {
        val workRecord = workRecordList[position]
        holder.tvNumber.text = workRecord["number"]
        holder.tvWorkerName.text = workRecord["workerName"]
        holder.tvDate.text = workRecord["date"]
        holder.tvClockIn.text = workRecord["clockIn"]
        holder.tvClockOut.text = workRecord["clockOut"]
        holder.tvWorkedHours.text = workRecord["workedHours"]
        holder.tvHours.text = workRecord["hours"]
    }

    override fun getItemCount(): Int = workRecordList.size

    // 데이터 갱신 메서드
    fun updateData(newData: List<Map<String, String>>) {
        workRecordList = newData
        notifyDataSetChanged()
    }
}
