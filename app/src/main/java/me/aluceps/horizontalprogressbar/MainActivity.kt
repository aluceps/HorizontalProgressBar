package me.aluceps.horizontalprogressbar

import android.animation.Animator
import android.animation.ValueAnimator
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.SeekBar
import me.aluceps.horizontalprogressbar.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                seekBar?.max?.toFloat()?.let {
                    Log.d("Progress", "progress=${progress / it}")
                    binding.progressBar.setProgress((progress / it))
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                binding.progressBar.blink()
            }
        })
        binding.blink.setOnClickListener {
            ValueAnimator().apply {
                setFloatValues(60.0f)
                addUpdateListener {
                    val p = it.animatedValue as Float
                    binding.progressBar.setProgress((p / 100))
                }
                duration = 1000
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        binding.progressBar.postDelayed({
                            binding.progressBar.blink()
                        }, 200)
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                    }

                    override fun onAnimationStart(animation: Animator?) {
                    }
                })
            }.start()
        }
    }
}
