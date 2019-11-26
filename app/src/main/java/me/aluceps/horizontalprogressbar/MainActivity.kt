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
        binding.progressView.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                seekBar?.max?.toFloat()?.let {
                    Log.d("Progress", "progress=${progress / it}")
                    binding.progressView.progressBar.setProgress((progress / it))
                    binding.progressView.progressText.text = "%.0f%%".format(progress / it * 100)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                binding.progressView.progressBar.blink()
            }
        })
        binding.button1.setOnClickListener {
            progressAnimation(60f) {
                binding.progressView.progressBar.setProgress(it)
                binding.progressView.progressText.text = "%.0f%%".format(it * 100)
            }
        }
        binding.button2.setOnClickListener {
            binding.progressView.progressBar.reset()
            binding.progressView.progressText.text = "0%"
        }
    }

    private fun progressAnimation(value: Float, progress: (Float) -> Unit) {
        ValueAnimator().apply {
            setFloatValues(value)
            addUpdateListener {
                val p = it.animatedValue as Float
                progress.invoke(p / 100)
            }
            duration = 1000
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    binding.progressView.progressBar.postDelayed({
                        binding.progressView.progressBar.blink()
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
