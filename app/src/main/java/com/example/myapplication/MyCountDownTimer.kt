import android.os.CountDownTimer
import com.example.myapplication.MainActivity

class MyCountDownTimer(private val totalTime: Long, private val interval: Long,val activity: MainActivity) :
    CountDownTimer(totalTime, interval) {

    override fun onTick(millisUntilFinished: Long) {
        var secondsRemaining : Double = 0.0
        try {
         secondsRemaining = (millisUntilFinished / 1000).toDouble()
        } catch (ex : Exception) {
        }
        activity.onTick(secondsRemaining.toString())
    }

    override fun onFinish() {
        activity.onFinish()
    }

    interface timerListener {
        fun onTick(inputData : String)
        fun onFinish()
    }
}