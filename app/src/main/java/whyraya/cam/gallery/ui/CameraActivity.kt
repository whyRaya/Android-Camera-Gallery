package whyraya.cam.gallery.ui

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import whyraya.cam.gallery.R
import whyraya.cam.gallery.data.ImageModel
import whyraya.cam.gallery.databinding.CameraBinding
import whyraya.cam.gallery.ui.adapter.GalleryAdapter
import whyraya.cam.gallery.utils.GridDecoration
import whyraya.cam.gallery.utils.PermissionHelper
import whyraya.cam.gallery.utils.Utils.dpToPx
import whyraya.cam.gallery.utils.Utils.getPathFromUri
import java.io.File

class CameraActivity: AppCompatActivity(),
    GalleryAdapter.Listener, PermissionHelper.PermissionListener {

    private lateinit var binding: CameraBinding

    private lateinit var viewModel: CameraViewModel

    private lateinit var layoutManager: GridLayoutManager

    private lateinit var adapter: GalleryAdapter

    private var vPosition = 0

    private var touchDown = false

    private var reInit = true

    private val loadImage = 25

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.camera)
        binding.lifecycleOwner = this

        viewModel = ViewModelProviders.of(this).get(CameraViewModel::class.java)
        binding.viewModel = viewModel

        viewModel.permission = PermissionHelper(this)
        viewModel.permission?.setPermissionListener(this)
        viewModel.clear()

        layoutManager = GridLayoutManager(
            this, 1, GridLayoutManager.HORIZONTAL, false)
        binding.itemImageList.itemAnimator = DefaultItemAnimator()
        binding.itemImageList.layoutManager = layoutManager
        binding.itemImageList.isNestedScrollingEnabled = false

        binding.galleryList.addItemDecoration(GridDecoration(3, dpToPx(this), true))
        binding.galleryList.itemAnimator = DefaultItemAnimator()
        binding.galleryList.layoutManager = GridLayoutManager(this, 3)
        binding.galleryList.isNestedScrollingEnabled = false

        (viewModel.imageData.value?: ArrayList()).let {
            adapter = GalleryAdapter(it, this)
            binding.itemImageList.adapter = adapter
            binding.galleryList.adapter = adapter
        }

        viewModel.imagePath.observe(this, Observer {
            it?.let {
                if (it.isNotEmpty()) {
                    viewModel.imageData.value?.add(0, ImageModel(File(it).toUri()))
                    adapter.notifyItemRangeInserted(0, 1)
                    showInfo("Located at $it")
                }
            }
        })

        viewModel.message.observe(this, Observer {
            it?.let { if (it.isNotEmpty()) showInfo(it) }
        })

        binding.appbar.setExpanded(true)
        trackMovement()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun trackMovement() {
        binding.itemImageList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (adapter.itemCount >= loadImage &&
                    layoutManager.findLastVisibleItemPosition() == adapter.itemCount - 1) {
                    loadPictures()
                }
            }
        })

        binding.appbar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener {
                appBarLayout, _ ->
            viewModel.onOffsetChanged(appBarLayout)
        })

        binding.nested.setOnScrollChangeListener {
                v: NestedScrollView, _: Int, scrollY: Int, _: Int, _: Int ->
            vPosition = scrollY
            if (vPosition > 0 && vPosition == (v.getChildAt(0).measuredHeight) - v.measuredHeight)
                loadPictures()
        }

        viewModel.mCollapseAlpha.observe(this, Observer {
            it?.let {
                if (it > 0 && touchDown) {
                    touchDown = false
                    binding.nested.scrollTo(0, 0)
                    binding.appbar.setExpanded(true)
                }
            }
        })

        binding.coordinator.setOnTouchListener { _, event ->
            if (event?.action == MotionEvent.ACTION_UP) {
                binding.appbar.setExpanded(viewModel.mCollapseAlpha.value?:0f >= 0.75f)
                true
            }
            else false
        }

        binding.nested.setOnTouchListener { _, event ->
            touchDown = false
            if (vPosition == 0 && event?.action == MotionEvent.ACTION_UP) {
                binding.appbar.setExpanded(viewModel.mCollapseAlpha.value?:0f >= 0.1f)
                true
            }
            else {
                if (event?.action == MotionEvent.ACTION_UP)
                    touchDown = true
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val listPermissions = java.util.ArrayList<String>()
            listPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            listPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            listPermissions.add(Manifest.permission.CAMERA)
            viewModel.permission?.checkAndRequestPermissions(listPermissions)
        } else
            onPermissionCheckDone()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    override fun onPermissionCheckDone() {
        if (reInit)
            viewModel.openCamera(this, binding.texture)

        reInit = false
        viewModel.onResume()
        if ((viewModel.imageData.value?: ArrayList()).size == 0)
            loadPictures()
    }

    private fun loadPictures() {
        viewModel.getImagesFromGallery(loadImage) {
            if (it.isNotEmpty()) {
                viewModel.imageData.value?.addAll(it)
                adapter.notifyItemRangeInserted(adapter.itemCount - it.size, it.size)
            }
        }
    }

    override fun onChooseImg(uri: Uri) {
        showInfo("Located at ${getPathFromUri(this, uri)?:""}")
    }

    private fun showInfo(info: String) {
        val snackBar = Snackbar.make(
            findViewById(android.R.id.content),
            info,
            Snackbar.LENGTH_INDEFINITE
        )
        snackBar.setAction("OK") { snackBar.dismiss() }
        snackBar.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        viewModel.permission?.onRequestCallBack(requestCode, permissions, grantResults)
    }
}