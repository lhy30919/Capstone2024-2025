package com.capstone.Algan

import android.app.Dialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.Algan.databinding.FragmentSalaryBinding

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
    private lateinit var tvrate1:TextView
    private lateinit var tvrate2:TextView
    private lateinit var tvrate3:TextView
    private lateinit var tvrate4:TextView



    private val isBusinessOwner = true // 사업주 테스트
    //private val isBusinessOwner = false // 근로자 테스트

    private lateinit var workerList: List<Worker> // 근로자 목록
    private lateinit var salaryRecordAdapter: SalaryRecordAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        val binding = FragmentSalaryBinding.inflate(inflater, container, false)

        if(isBusinessOwner){
            binding.LinOwner.visibility = View.VISIBLE
            binding.btnShowSalaryInputOwner.visibility = View.VISIBLE
        } else{binding.LinOwner.visibility = View.GONE
        binding.btnShowSalaryInputOwner.visibility = View.GONE}

        tvStartDate = binding.tvStartDate
        tvEndDate = binding.tvEndDate
        spinnerWorker = binding.spinnerWorker
        salaryAmount = binding.salaryAmount
        worktime = binding.worktime
        totalWorkHours = binding.totalWorkHours
        hourlyRate = binding.hourlyRate
        deductions = binding.deductions
        recyclerView = binding.SalaryRecyclerView
        tvrate1 = binding.tvrate1
        tvrate2 = binding.tvrate2
        tvrate3 = binding.tvrate3
        tvrate4 = binding.tvrate4




        recyclerView.layoutManager = LinearLayoutManager(context)

        // 근로자 리스트 설정 (예시로 근로자 데이터 추가)
        workerList = listOf(
            Worker("근로자 1", "09:00 ~ 18:00", 80000.0, 8.0, 8.0, 10000.0, 5.0),
            Worker("근로자 2", "09:00 ~ 18:00", 96000.0, 8.0, 8.0, 12000.0, 10.0),
            Worker("근로자 3", "09:00 ~ 18:00", 120000.0, 8.0, 8.0, 15000.0, 15.0)
        )

        // Spinner Adapter 설정
        val workerNames = workerList.map { it.name }
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, workerNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWorker.adapter = adapter

        // RecyclerView Adapter 설정
        salaryRecordAdapter = SalaryRecordAdapter(requireContext(), workerList)
        recyclerView.adapter = salaryRecordAdapter

        // 급여 입력 다이얼로그 버튼 클릭 리스너
        val btnShowSalaryInput = binding.btnShowSalaryInput
        btnShowSalaryInput.setOnClickListener {
            showSalaryInputDialog()
        }
        // 사업주의 전체 급여 설정 리스너
        val btnShowSalaryInputOwner = binding.btnShowSalaryInputOwner
        btnShowSalaryInputOwner.setOnClickListener {
            showSalaryInputOwnerDialog()
        }
        // 근로자 선택 시 처리
        spinnerWorker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedWorker = workerList[position]

                // 선택된 근로자의 기록 갱신 (예시)
                updateSalaryRecords(selectedWorker)
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // 아무것도 선택되지 않았을 때의 처리
            }
        }

        return binding.root
    }


    // 완료 => 미완료 버튼 바꾸는 함수. 알람 로직 추가 필요.
    private fun updateButtonStatus(button: Button, isCompleted: Boolean) {
        if (!isAdded) return // 프래그먼트가 유효하지 않으면 리턴

        val text = if (isCompleted) "+" else "-"

        button.text = text

        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
        }

        button.background = drawable
    }




    private fun showSalaryInputOwnerDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_salary_input_whole)

        val etHourlyRate: EditText = dialog.findViewById(R.id.etHourlyRateWhole)
        val etDeductions: EditText = dialog.findViewById(R.id.etDeductionsWhole)
        val btnSetSalary: Button = dialog.findViewById(R.id.btnSetSalaryWhole)
        val etRate1:EditText = dialog.findViewById(R.id.etRate1)
        val etRate2:EditText = dialog.findViewById(R.id.etRate2)
        val etRate3:EditText = dialog.findViewById(R.id.etRate3)
        val etRate4:EditText = dialog.findViewById(R.id.etRate4)



        btnSetSalary.setOnClickListener {
            val hourlyRateValue = etHourlyRate.text.toString().toDoubleOrNull() ?: 0.0
            val deductionsValue = etDeductions.text.toString().toDoubleOrNull() ?: 0.0
            val etRate1Value =  etRate1.text.toString().toDoubleOrNull()?:0.0
            val etRate2Value =  etRate2.text.toString().toDoubleOrNull()?:0.0
            val etRate3Value =  etRate3.text.toString().toDoubleOrNull()?:0.0
            val etRate4Value =  etRate4.text.toString().toDoubleOrNull()?:0.0
            // 급여 계산 후 화면에 표시
            worktime.text = "근무시간: 09:00 ~ 18:00" // 예시
            hourlyRate.text = "시급: ${hourlyRateValue}원"
            deductions.text = "공제: ${deductionsValue}%"
            tvrate1.text = "비율1 : ${etRate1Value}"
            tvrate2.text = "비율1 : ${etRate2Value}"
            tvrate3.text = "비율1 : ${etRate3Value}"
            tvrate4.text = "비율1 : ${etRate4Value}"
            // 다이얼로그 닫기
            dialog.dismiss()
        }

        // Set the dialog width to match the parent width
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        dialog.show()
    }

    private fun showSalaryInputDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_salary_input_calc)

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

            // 다이얼로그 닫기
            dialog.dismiss()

            // Show output dialog
            showSalaryOutputDialog(
                finalSalary,
                "09:00 ~ 18:00",
                workHoursValue,
                hourlyRateValue,
                deductionsValue
            )
        }

        // Set the dialog width to match the parent width
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        dialog.show()
    }

    private fun showSalaryOutputDialog(
        finalSalary: Double,
        workTime: String,
        workHours: Double,
        hourlyRate: Double,
        deductions: Double
    ) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_salary_output_calc)

        val salaryAmountD: TextView = dialog.findViewById(R.id.salaryAmountD)
        val totalWorkHoursD: TextView = dialog.findViewById(R.id.totalWorkHoursD)
        val hourlyRateD: TextView = dialog.findViewById(R.id.hourlyRateD)
        val deductionsD: TextView = dialog.findViewById(R.id.deductionsD)
        val btnCloseOutput: Button = dialog.findViewById(R.id.btnCloseOutput)

        // 급여 계산 후 화면에 표시
        salaryAmountD.text = "실 지급액: ${finalSalary}원"
        totalWorkHoursD.text = "총 근무시간: ${workHours}시간"
        hourlyRateD.text = "시급: ${hourlyRate}원"
        deductionsD.text = "공제: ${deductions}%"

        btnCloseOutput.setOnClickListener {
            dialog.dismiss()
        }

        // Set the dialog width to match the parent width
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        dialog.show()
    }

    // 선택된 근로자의 근무 기록을 업데이트하는 메서드
    private fun updateSalaryRecords(worker: Worker) {
        val workRecords = listOf(
            worker
        )
        salaryRecordAdapter.updateData(workRecords)
    }
}

// 근로자 데이터 클래스
data class Worker(
    val name: String,
    val workTime: String,
    val salaryAmount: Double, // 실 지급액
    val worktime: Double, // 근무 시간
    val totalWorkHours: Double, // 총 근무시간
    val hourlyRate: Double, // 시급
    val deductions: Double // 공제
)

// SalaryRecordAdapter 수정
class SalaryRecordAdapter(
    private val context: android.content.Context,
    private var workRecordList: List<Worker>
) : RecyclerView.Adapter<SalaryRecordAdapter.SalaryViewHolder>() {

    inner class SalaryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumber: TextView = view.findViewById(R.id.tvNumber)
        val tvWorkerName: TextView = view.findViewById(R.id.tvWorkerName)
        val salaryAmount: TextView = view.findViewById(R.id.salaryAmount)
        val worktime: TextView = view.findViewById(R.id.worktime)
        val totalWorkHours: TextView = view.findViewById(R.id.totalWorkHours)
        val hourlyRate: TextView = view.findViewById(R.id.hourlyRate)
        val deductions: TextView = view.findViewById(R.id.deductions)
        val buttonExpand: Button = view.findViewById(R.id.buttonExpand)
        val btnSalaryReset: Button = view.findViewById(R.id.btnSalaryReset)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_salary, parent, false)
        return SalaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SalaryViewHolder, position: Int) {
        val workRecord = workRecordList[position]
        holder.tvNumber.text = (position + 1).toString()
        holder.tvWorkerName.text = workRecord.name
        holder.salaryAmount.text = "급여: ${workRecord.salaryAmount}원"
        holder.worktime.text = "근무시간: ${workRecord.workTime}"
        holder.totalWorkHours.text = "총 근무시간: ${workRecord.totalWorkHours}"
        holder.hourlyRate.text = "시급: ${workRecord.hourlyRate}원"
        holder.deductions.text = "공제: ${workRecord.deductions}%"

        // Set up button listeners if needed for expand and modify actions
        holder.buttonExpand.setOnClickListener {
            // Expand action logic here
        }

        holder.btnSalaryReset.setOnClickListener {
            showSalaryResetDialog(holder.adapterPosition)
        }
    }

    private fun showSalaryResetDialog(position: Int) {
        val workRecord = workRecordList[position]

        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_salary_input_emp)

        val etHourlyRateEmp: EditText = dialog.findViewById(R.id.etHourlyRateEmp)
        val etDeductionsEmp: EditText = dialog.findViewById(R.id.etDeductionsEmp)
        val etWorkHoursEmp: EditText = dialog.findViewById(R.id.etWorkHoursEmp)
        val btnSetSalaryEmp: Button = dialog.findViewById(R.id.btnSetSalaryEmp)

        // Set initial values in the dialog
        etHourlyRateEmp.setText(workRecord.hourlyRate.toString())
        etDeductionsEmp.setText(workRecord.deductions.toString())
        etWorkHoursEmp.setText(workRecord.totalWorkHours.toString())

        btnSetSalaryEmp.setOnClickListener {
            val hourlyRateValue = etHourlyRateEmp.text.toString().toDoubleOrNull() ?: 0.0
            val deductionsValue = etDeductionsEmp.text.toString().toDoubleOrNull() ?: 0.0
            val workHoursValue = etWorkHoursEmp.text.toString().toDoubleOrNull() ?: 0.0

            val totalSalary = hourlyRateValue * workHoursValue
            val deductionsAmount = totalSalary * (deductionsValue / 100)
            val finalSalary = totalSalary - deductionsAmount

            // Update the work record
            val updatedRecord = workRecord.copy(
                hourlyRate = hourlyRateValue,
                deductions = deductionsValue,
                totalWorkHours = workHoursValue,
                salaryAmount = finalSalary
            )

            workRecordList = workRecordList.toMutableList().apply {
                set(position, updatedRecord)
            }

            // Notify the adapter of the change
            notifyItemChanged(position)

            // 다이얼로그 닫기
            dialog.dismiss()
        }

        // Set the dialog width to match the parent width
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        dialog.show()
    }

    override fun getItemCount(): Int = workRecordList.size

    // 데이터 갱신 메서드
    fun updateData(newData: List<Worker>) {
        workRecordList = newData
        notifyDataSetChanged()
    }
}