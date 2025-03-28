package com.capstone.Algan

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.capstone.Algan.databinding.FragmentChecklistBinding
import java.text.SimpleDateFormat
import java.util.*
import android.text.TextUtils
class ChecklistFragment : Fragment() {

    private var _binding: FragmentChecklistBinding? = null
    private val binding get() = _binding!!

    // 사업주 여부 확인 변수 =>(실제 로그인 상태에 맞게 Db연결로 수정해 주세요)
    private val isBusinessOwner = true  // 사업주로 테스트 하기 위한 코드
    //private val isBusinessOwner = false // 근로자로 테스트 하기 위한 코드
    // 근로자 목록 (테스트용으로 "테스트근로자" 사용)
    private val employeeList = listOf("근로자 1", "근로자 2", "근로자 3", "테스트근로자")

    // 그룹에 해당하는 근로자 리스트
    private val employeesByGroup = mapOf(
        "전체" to listOf("근로자 1", "근로자 2", "근로자 3", "테스트근로자")
    )
    // 로그인한 근로자 정보 (테스트용: 테스트근로자, 오전 근무)
    private val loggedInUser = Employee("테스트근로자", "오전")

    private val checklistItems = mutableListOf<ChecklistItem>()
    private lateinit var listViewAdapter: ChecklistAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChecklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 사업주일 경우에만 체크리스트 추가 뷰가 보이게
        if (isBusinessOwner) {
            binding.tvselectEmployees.visibility = View.VISIBLE //근로자 선택 텍스트
            binding.spinnerEmployees.visibility = View.VISIBLE // 근로자 선택 스피너
            binding.textViewItemContent.visibility = View.VISIBLE //내용입력 텍스트
            binding.editTextItemContent.visibility = View.VISIBLE //입력 창
            binding.buttonAddItem.visibility = View.VISIBLE // 체크리스트 보내기 버튼
        } else {
            binding.tvselectEmployees.visibility = View.GONE
            binding.spinnerEmployees.visibility = View.GONE
            binding.textViewItemContent.visibility = View.GONE
            binding.editTextItemContent.visibility = View.GONE
            binding.buttonAddItem.visibility = View.GONE
        }
        val listView = binding.listViewItems

        // ListView 높이를 데이터 개수에 맞게 설정하는 함수
        fun setListViewHeightBasedOnItems() {
            val adapter = listView.adapter ?: return
            var totalHeight = 0
            for (i in 0 until adapter.count) {
                val listItem = adapter.getView(i, null, listView)
                listItem.measure(
                    View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.UNSPECIFIED
                )
                totalHeight += listItem.measuredHeight
            }
            val layoutParams = listView.layoutParams
            layoutParams.height = totalHeight + (listView.dividerHeight * (adapter.count - 1))
            listView.layoutParams = layoutParams
            listView.requestLayout()
        }

        // 스크롤 뷰의 스크롤 비활성화
        val scrollView = binding.root as ScrollView
        scrollView.isVerticalScrollBarEnabled = false

        // 데이터가 변경될 때 높이 재설정
        listView.viewTreeObserver.addOnGlobalLayoutListener {
            setListViewHeightBasedOnItems()
        }

        setupEmployeeSpinner()
        setupListView()
        setupAddButton()

        // 근로자일 경우의 테스트 항목(삭제해주세요)(예시로 "test 체크리스트" 추가)
        addTestChecklistItem()
    }

    private fun setupEmployeeSpinner() {
        val employeeSpinnerList = employeeList.toMutableList()
        employeeSpinnerList.add(0, "전체") // "전체" 항목을 추가

        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, employeeSpinnerList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEmployees.adapter = spinnerAdapter

        if (isBusinessOwner) {
            binding.spinnerEmployees.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    filterChecklistItems()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } else {
            // 근로자일 경우 본인만 선택 가능하게
            binding.spinnerEmployees.setSelection(employeeList.indexOf(loggedInUser.name) + 1) // +1은 "전체"를 제외한 인덱스
        }
    }

    private fun setupListView() {
        listViewAdapter = ChecklistAdapter(requireContext(), checklistItems)
        binding.listViewItems.adapter = listViewAdapter
    }

    private fun setupAddButton() {
        binding.buttonAddItem.setOnClickListener {
            val content = binding.editTextItemContent.text.toString().trim()
            val employeeName = binding.spinnerEmployees.selectedItem?.toString()

            if (content.isNotEmpty() && employeeName != null) {
                val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

                // 선택된 근로자에게 보내는 항목 추가
                if (employeeName == "전체") {
                    // "전체" 선택 시 해당 그룹에 속한 모든 근로자에게 항목을 추가
                    employeesByGroup["전체"]?.forEach { employee ->
                        val newItem = ChecklistItem(currentDate, employee, content, false)
                        checklistItems.add(0, newItem)
                    }
                } else {
                    // 개별 근로자에게 항목을 추가
                    val newItem = ChecklistItem(currentDate, employeeName, content, false)
                    checklistItems.add(0, newItem)
                }

                listViewAdapter.notifyDataSetChanged()
                binding.editTextItemContent.text.clear()
                Toast.makeText(requireContext(), "항목이 추가되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "내용과 근로자를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterChecklistItems() {
        val selectedEmployee = binding.spinnerEmployees.selectedItem?.toString()

        checklistItems.forEach { item ->
            item.isVisible = when {
                selectedEmployee == "전체" -> true
                selectedEmployee == item.employeeName -> true
                else -> false
            }
        }

        listViewAdapter.notifyDataSetChanged()
    }

    private fun addTestChecklistItem() {
        // 기본 체크리스트 항목 추가 (예: "test 체크리스트")
        val testItem = ChecklistItem(
            date = SimpleDateFormat("yyyy-MM-dd \nHH:mm", Locale.getDefault()).format(Date()),
            employeeName = "테스트근로자",  // "테스트근로자"가 포함된 체크리스트 항목
            content = "test 체크리스트",
            isCompleted = false
        )
        checklistItems.add(testItem)
        listViewAdapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        binding.spinnerEmployees.setSelection(0) // "전체"로 초기화
        filterChecklistItems()
        // 체크리스트 갱신
        listViewAdapter.notifyDataSetChanged()
    }

    // 근로자 정보 클래스
    data class Employee(val name: String, val group: String)

    data class ChecklistItem(
        val date: String,
        val employeeName: String,
        val content: String,
        var isCompleted: Boolean,
        var isVisible: Boolean = true
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class ChecklistAdapter(
        private val context: android.content.Context,
        private val items: MutableList<ChecklistItem>
    ) : BaseAdapter() {

        override fun getCount(): Int = items.count { it.isVisible }

        override fun getItem(position: Int): Any {
            val visibleItems = items.filter { it.isVisible }
            return visibleItems[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val visibleItems = items.filter { it.isVisible }
            val item = visibleItems[position]

            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_checklist, parent, false)
            val tvDate = view.findViewById<TextView>(R.id.tvDate)
            val tvEmployeeName = view.findViewById<TextView>(R.id.tvEmployeeName)
            val tvContent = view.findViewById<TextView>(R.id.tvContent)
            val buttonStatus = view.findViewById<Button>(R.id.buttonStatus)
            val buttonExpand = view.findViewById<Button>(R.id.buttonExpand)

            tvDate.text = item.date
            tvEmployeeName.text = item.employeeName
            tvContent.text = item.content

            view.post {
                if (tvContent.lineCount > 1) {
                    tvContent.maxLines = 2
                    tvContent.ellipsize = TextUtils.TruncateAt.END
                }
            }

            buttonExpand.setOnClickListener {
                if (tvContent.maxLines == 2) {
                    tvContent.text = item.content + "\n"
                    tvContent.maxLines = Int.MAX_VALUE
                    buttonExpand.text = "-"
                } else {
                    tvContent.text = item.content
                    tvContent.maxLines = 2
                    buttonExpand.text = "+"
                }
            }

            updateButtonStatus(buttonStatus, item.isCompleted)

            buttonStatus.setOnClickListener {
                item.isCompleted = !item.isCompleted
                updateButtonStatus(buttonStatus, item.isCompleted)
            }

            return view
        }
    }

    // 완료 => 미완료 버튼 바꾸는 함수. 알람 로직 추가 필요.
    private fun updateButtonStatus(button: Button, isCompleted: Boolean) {
        if (!isAdded) return // 프래그먼트가 유효하지 않으면 리턴

        val color = if (isCompleted) android.R.color.holo_green_light else android.R.color.holo_orange_light
        val text = if (isCompleted) "완료" else "미완료"

        button.text = text

        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
            setColor(resources.getColor(color, null))
        }

        button.background = drawable
    }
}
