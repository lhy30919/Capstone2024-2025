package com.capstone.Algan.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.capstone.Algan.R
import com.capstone.Algan.CommunicationFragment
import com.capstone.Algan.DaeTaFragment
import com.capstone.Algan.NoticeFragment

class NoticeBoardFragment : Fragment() {

    // 액티비티에서 프래그먼트 교체하는 함수
    private fun replaceFragment(fragment: Fragment) {
        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.replace(R.id.frame_container, fragment)
        transaction?.commit()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 레이아웃 뷰
        val view = inflater.inflate(R.layout.fragment_noticeboard, container, false)
        //게시판 메뉴 밑줄
        val underlineDaeta = view.findViewById<View>(R.id.underline_daeta)
        val underlineNotice = view.findViewById<View>(R.id.underline_notice)
        val underlineCommunication = view.findViewById<View>(R.id.underline_communication)

        fun Buttonunderline(underline: View) {
            // 모든 줄 숨기기
            underlineDaeta.visibility = View.GONE
            underlineNotice.visibility = View.GONE
            underlineCommunication.visibility = View.GONE

            // 선택된 줄만 보이게 하기
            underline.visibility = View.VISIBLE
        }

        // 기본 화면을 대타 화면으로 설정
        replaceFragment(DaeTaFragment())

        // 대타 버튼 클릭
        val buttonDaeta = view.findViewById<View>(R.id.button_daeta)
        buttonDaeta.setOnClickListener {
            replaceFragment(DaeTaFragment())
            Buttonunderline(underlineDaeta)
        }

        // 공지 버튼 클릭
        val buttonNotice = view.findViewById<View>(R.id.button_notice)
        buttonNotice.setOnClickListener {
            replaceFragment(NoticeFragment())
            Buttonunderline(underlineNotice)
        }

        // 소통 버튼 클릭
        val buttonCommunication = view.findViewById<View>(R.id.button_communication)
        buttonCommunication.setOnClickListener {
            replaceFragment(CommunicationFragment())
            Buttonunderline(underlineCommunication)
        }
        Buttonunderline(underlineDaeta)
        return view
    }
}
