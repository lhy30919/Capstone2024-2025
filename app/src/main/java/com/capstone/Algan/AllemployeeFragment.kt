package com.capstone.Algan

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.capstone.Algan.utils.UserData

class AllemployeeFragment : Fragment() {

    private lateinit var listView: ListView
    private val userList = mutableListOf<Employee>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_allemployee, container, false)

        // ListView 초기화
        listView = view.findViewById(R.id.all_list)

        loadUserData()

        return view
    }

    private fun loadUserData() {
        val sharedPreferences = requireActivity().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", null)

        if (userId == null) {
            Toast.makeText(context, "로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 회사 코드 가져오기
        UserData.loadUserCompanyCode(requireContext(), userId) { companyCode ->
            if (companyCode == null) {
                Toast.makeText(context, "회사 코드를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                return@loadUserCompanyCode
            }

            // 회사 구성원 가져오기
            UserData.loadAllUsersFromCompany(requireContext(), companyCode) { users ->
                if (users.isEmpty()) {
                    Log.d("AllemployeeFragment", "같은 회사에 속한 구성원이 없습니다.")
                } else {
                    Log.d("AllemployeeFragment", "구성원 목록: ${users.size}명")
                }

                userList.clear()
                userList.addAll(users)

                // ListView 어댑터 설정
                val adapter = EmployeeAdapter(requireContext(), userList)
                listView.adapter = adapter
            }
        }
    }

    // EmployeeAdapter 클래스
    private class EmployeeAdapter(
        context: Context,
        private val userList: List<Employee>
    ) : ArrayAdapter<Employee>(context, R.layout.item_allemployee, userList) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_allemployee, parent, false)

            val employee = userList[position]

            val nameTextView: TextView = view.findViewById(R.id.tvUsername)
            val roleTextView: TextView = view.findViewById(R.id.tvRole)
            val profileImageView: ImageView = view.findViewById(R.id.imageView_profile)

            nameTextView.text = employee.username
            roleTextView.text = employee.role
            profileImageView.setImageResource(R.drawable.baseline_person_2_24) // 프로필 이미지 고정

            return view
        }
    }
}