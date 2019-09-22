package com.ralph.punchpower

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.AnimationUtils.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    // 측정된 최대 펀치력
    var maxPower = 0.0

    // 펀치력 측정이 시작되었는지 나타내는 변수
    var isStart = false

    // 펀치력 측정이 시작된 시간
    var startTime = 0L

    // Sensor 관리자 객체. lazy로 실제 사용될 때 초기화 한다.
    val sensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    // 센서 이벤트를 처리하는 리스너
    val eventListener: SensorEventListener = object: SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                // 측정된 센서 값이 선형 가속도 타입이 아니면 바로 리턴
                if(event.sensor.type != Sensor.TYPE_LINEAR_ACCELERATION) return@let

                // 각 좌표값을 제곱하여 음수값을 없애고 값의 차이를 그대화
                val power = Math.pow(event.values[0].toDouble(), 2.0) + Math.pow(event.values[1].toDouble(), 2.0) + Math.pow(event.values[2].toDouble(), 2.0)

                // 측정된 펀치력이 20을 넘고 아직 측정이 시작되지 않은 경우
                if(power > 20 && !isStart) {
                    // 측정 시작
                    startTime = System.currentTimeMillis()
                    isStart = true
                }

                // 측정이 시작된 경우
                if(isStart) {
                    // 애니메이션 중지
                    imageView.clearAnimation()

                    // 5초간 최대값을 측정. 현재 측정된 값이 지금까지 측정된 최대값보다 크면 최대값을 현재 값으로 변경
                    if(maxPower < power)
                        maxPower = power

                    // 측정중인 것을 사용자에게 알려줌
                    startLabel.text = "펀치력을 측정하고 있습니다"

                    // 최초 측정 후 3초가 지났으면 측정을 끝낸다.
                    if(System.currentTimeMillis() > startTime + 3000) {
                        isStart = false
                        punchPowerTestComplete(maxPower)
                    }
                }
            }
        }
    }

    // 펀치력 측정이 완료된 경우 처리 함수
    fun punchPowerTestComplete(power: Double) {
        Log.d("MainActivity", "측정완료: power: ${String.format("%.5f", power)}")

        sensorManager.unregisterListener(eventListener)
        val intent = Intent(this@MainActivity, ResultActivity::class.java)
        intent.putExtra("power", power)
        startActivity(intent)
    }

    // 게임을 초기화 한다
    fun initGame() {
        maxPower = 0.0
        isStart = false
        startTime = 0L
        startLabel.text = "핸드폰을 손에 쥐고 주먹을 내지르세요"

        // 센서의 변화값을 처리할 리스너를 등록한다.
        // TYPE_LINEAR_ACCELERATION은 중력값을 제외하고 X, Y, Z 축에 측정된 가속도만 계산되어 나온다.
        sensorManager.registerListener(eventListener, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL)

        // 애니메이션 시작
        //imageView.startAnimation(AnimationUtils.loadAnimation(this@MainActivity, R.anim.tran))
        //imageView.startAnimation(AnimationUtils.loadAnimation(this@MainActivity, R.anim.rotate))
//        val ani = AnimationUtils.loadAnimation(this@MainActivity, R.anim.alpha_scale)
//        imageView.startAnimation(ani)
//
//        // 애니메이션 리스너 설정
//        ani.setAnimationListener(object: Animation.AnimationListener {
//            override fun onAnimationRepeat(animation: Animation?) {
//                // 애니메이션이 반복될 때의 처리
//            }
//
//            override fun onAnimationEnd(animation: Animation?) {
//                // 애니메이션이 종료될 때의 처리
//            }
//
//            override fun onAnimationStart(animation: Animation?) {
//                // 애니메이션이 시작될 때의 처리
//            }
//        })

        // 속성 애니메이션을 볼러옴. apply 함수를 사용하면 로딩된 Animator가 this로 지정됨
        AnimatorInflater.loadAnimator(this@MainActivity, R.animator.property_animation).apply {
            // 애니메이션 종료 리스너를 추가
            addListener(object: AnimatorListenerAdapter() {
                // 애니메이션 종료될 때 애니메이션을 다시 시작
                override fun onAnimationEnd(aniation: Animator?) {
                    start()
                }
            })
            // 속성 애니메이션 타겟을 글로브 이미지뷰로 지정
            setTarget(imageView)

            // 애니메이션 시작
            start()
        }
    }

    // 화면이 최초 생성될 때 호출
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    // Activity가 사라졌다 다시 보일 때마다 호출
    override fun onStart() {
        super.onStart()
        initGame()
    }

    // Activity가 화면에서 사라지면 호출되는 함수
    override fun onStop() {
        super.onStop()
        try {
            sensorManager.unregisterListener(eventListener)
        } catch(e:Exception) {}
    }
}
