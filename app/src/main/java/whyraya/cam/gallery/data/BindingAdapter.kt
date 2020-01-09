package whyraya.cam.gallery.data

import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

object BindingAdapter {

    @BindingAdapter("android:frame")
    @JvmStatic
    fun addView(v: FrameLayout, child: View?) {
        child?.let {
            v.removeAllViews()
            v.addView(it)
        }
    }

    @BindingAdapter("android:image")
    @JvmStatic
    fun loadImage(v: ImageView, uri: Uri?) {
        Glide.with(v.context).load(uri).into(v)
    }
}