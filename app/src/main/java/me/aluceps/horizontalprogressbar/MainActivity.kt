package me.aluceps.horizontalprogressbar

import android.animation.Animator
import android.animation.ValueAnimator
import android.databinding.BindingAdapter
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
        binding.progressTickView.apply {
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                ) {
                    seekBar?.max?.toFloat()?.let {
                        Log.d("Progress", "progress=${progress / it}")
                        progressBar.progress = progress / it
                        progressText.text = TEXT_FORMAT.format(progress / it * 100)
                    }
                }
            })
            progress.setOnClickListener {
                progressAnimation(60f) {
                    progressBar.progress = it
                    progressText.text = TEXT_FORMAT.format(it * 100)
                }
            }
            reset.setOnClickListener {
                progressBar.reset()
                progressText.text = "0%"
            }
            0.8f.let {
                progressBar.progress = it
                progressText.text = TEXT_FORMAT.format(it * 100)
            }
        }

        binding.progressLineView.apply {
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                ) {
                    seekBar?.max?.toFloat()?.let {
                        Log.d("Progress", "progress=${progress / it}")
                        progressBar.progress = progress / it
                        progressText.text = TEXT_FORMAT.format(progress / it * 100)
                    }
                }
            })
            progress.setOnClickListener {
                progressAnimation(60f) {
                    progressBar.progress = it
                    progressText.text = TEXT_FORMAT.format(it * 100)
                }
            }
            reset.setOnClickListener {
                progressBar.reset()
                progressText.text = "0%"
            }
            0.8f.let {
                progressBar.progress = it
                progressText.text = TEXT_FORMAT.format(it * 100)
            }
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
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    binding.progressTickView.progressBar.postDelayed({
                        binding.progressTickView.progressBar.blink()
                    }, 200)
                }
            })
        }.start()
    }

    companion object {
        private const val TEXT_FORMAT = "%.0f%%"
    }
}

@BindingAdapter("set_decoration_type")
fun HorizontalProgressBar.setDecorationType(type: DecorationType?) {
    if (type == null) return
    setDecorationType(type)
}
