package com.capstone.Algan

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash)

        // 스플래시 화면을 비활성화하려면 아래 코드 추가
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android 12 이상에서는 SplashScreen을 수동으로 비활성화
            window.insetsController?.hide(WindowInsets.Type.statusBars()) // 상태바 숨기기
        }

        // 2초 후에 LoginActivity 넘어가기
        Handler().postDelayed({
            // BeaconService 시작
            val intent = Intent(this, BeaconService::class.java)
            startService(intent)

            // 다음 화면으로 이동
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 2000)  // 2000ms (2초)
    }
}
