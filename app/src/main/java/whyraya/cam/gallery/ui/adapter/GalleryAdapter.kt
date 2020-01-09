package whyraya.cam.gallery.ui.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import whyraya.cam.gallery.R
import whyraya.cam.gallery.data.ImageModel
import whyraya.cam.gallery.databinding.CameraGalleryItemBinding
import java.util.*

class GalleryAdapter(
    var mData: ArrayList<ImageModel>,
    private val mListener: Listener?
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    fun updateList(data: ArrayList<ImageModel>) {
        mData = data
        this.notifyDataSetChanged()
    }

    fun addList(data: ArrayList<ImageModel>) {
        mData.addAll(data)
        notifyItemRangeInserted(mData.size, data.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: CameraGalleryItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context), R.layout.camera_gallery_item, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(vh: ViewHolder, position: Int) {
        with(vh.binding) {
            Glide.with(vh.itemView.context).load(mData[position].image).into(image)
            image.setOnClickListener {
                mData[position].uri?.let { mListener?.onChooseImg(it) }
            }
        }
    }


    override fun getItemCount() = mData.size

    class ViewHolder (val binding: CameraGalleryItemBinding) : RecyclerView.ViewHolder(binding.root)

    interface Listener {
        fun onChooseImg(uri: Uri)
    }
}