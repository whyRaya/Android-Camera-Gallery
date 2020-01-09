package whyraya.cam.gallery.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import whyraya.cam.gallery.R
import whyraya.cam.gallery.databinding.CameraBinding
import whyraya.cam.gallery.ui.adapter.GalleryAdapter
import whyraya.cam.gallery.utils.GridDecoration
import whyraya.cam.gallery.utils.PermissionHelper
import whyraya.cam.gallery.utils.Utils.dpToPx
import whyraya.cam.gallery.utils.Utils.getPathFromUri

class CameraActivity: AppCompatActivity(),
    GalleryAdapter.Listener, PermissionHelper.PermissionListener {

    private lateinit var binding: CameraBinding

    private lateinit var viewModel: CameraViewModel

    private lateinit var layoutManager1: GridLayoutManager

    private lateinit var layoutManager2: GridLayoutManager

    private lateinit var adapterHorizontal: GalleryAdapter

    private lateinit var adapterVertical: GalleryAdapter

    private var vPosition = 0

    private var touchDown = false

    private var reInit = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.camera)
        binding.lifecycleOwner = this

        viewModel = ViewModelProviders.of(this).get(CameraViewModel::class.java)
        binding.viewModel = viewModel

        viewModel.permission = PermissionHelper(this)
        viewModel.permission?.setPermissionListener(this)

        layoutManager1 = GridLayoutManager(
            this, 1, GridLayoutManager.HORIZONTAL, false)
        binding.itemImageList.itemAnimator = DefaultItemAnimator()
        binding.itemImageList.layoutManager = layoutManager1
        binding.itemImageList.isNestedScrollingEnabled = false

        layoutManager2 = GridLayoutManager(this, 3)
        binding.galleryList.addItemDecoration(GridDecoration(3, dpToPx(this), true))
        binding.galleryList.itemAnimator = DefaultItemAnimator()
        binding.galleryList.layoutManager = layoutManager2
        binding.galleryList.isNestedScrollingEnabled = false

        (viewModel.imageData.value?: ArrayList()).let {
            adapterHorizontal = GalleryAdapter(it, this)
            binding.itemImageList.adapter = adapterHorizontal

            adapterVertical = GalleryAdapter(it, this)
            binding.galleryList.adapter = adapterVertical
        }

        viewModel.imagePath.observe(this, Observer {
            it?.let {
                if (it.isNotEmpty()) {
                    setResult(Activity.RESULT_OK, Intent().putExtra("image", it))
                    finish()
                }
            }
        })

        trackMovement()
        binding.appbar.setExpanded(true)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun trackMovement() {
        binding.itemImageList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (layoutManager1.findLastVisibleItemPosition() == adapterHorizontal.mData.lastIndex)
                    loadPictures()
            }
        })

        binding.galleryList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (layoutManager2.findLastVisibleItemPosition() == adapterVertical.mData.lastIndex)
                    loadPictures()
            }
        })

        binding.appbar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener {
                appBarLayout, _ ->
            viewModel.onOffsetChanged(appBarLayout)
        })

        binding.nested.setOnScrollChangeListener {
                _: NestedScrollView?, _: Int, scrollY: Int, _: Int, _: Int ->
            vPosition = scrollY
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
        if ((viewModel.imageData.value?: ArrayList()).size == 0) loadPictures()
    }

    private fun loadPictures() {
        viewModel.getImagesFromGallery(25) {
            if (it.isNotEmpty()) {
                adapterHorizontal.addList(ArrayList(it))
                adapterVertical.addList(ArrayList(it))
                viewModel.imageData.value?.addAll(it)
            }
        }
    }

    override fun onChooseImg(uri: Uri) {
        viewModel.setImagePath(getPathFromUri(this@CameraActivity, uri)?:"")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        viewModel.permission?.onRequestCallBack(requestCode, permissions, grantResults)
    }
}