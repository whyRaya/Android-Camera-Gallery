package whyraya.cam.gallery.data

import android.view.View
import android.widget.FrameLayout
import androidx.databinding.BindingAdapter

object BindingAdapter {

    @BindingAdapter("android:frame")
    @JvmStatic
    fun addView(v: FrameLayout, child: View?) {
        child?.let {
            v.removeAllViews()
            v.addView(it)
        }

    }
}