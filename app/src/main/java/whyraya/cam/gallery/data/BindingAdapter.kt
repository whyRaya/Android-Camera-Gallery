package whyraya.cam.gallery.data

import android.net.Uri
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import whyraya.cam.gallery.utils.camera.CameraPreview

object BindingAdapter {

    @BindingAdapter("android:frame")
    @JvmStatic
    fun addView(v: FrameLayout, preview: CameraPreview?) {
        preview?.also {
            v.removeAllViews()
            v.addView(it)
        }
    }

    @BindingAdapter("android:image")
    @JvmStatic
    fun loadImage(v: ImageView, uri: Uri?) {
        Glide.with(v.context).load(uri).into(v)
    }

    @BindingAdapter("android:src")
    @JvmStatic
    fun setImageResource(v: ImageButton, resource: Int) {
        v.setImageResource(resource)
    }
}