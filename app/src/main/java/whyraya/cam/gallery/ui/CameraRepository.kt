package whyraya.cam.gallery.ui

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import android.os.Build
import android.provider.MediaStore.Images.Media.*
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import whyraya.cam.gallery.data.ImageModel
import whyraya.cam.gallery.utils.camera.CameraPreview
import whyraya.cam.gallery.utils.camera.CameraUtil
import whyraya.cam.gallery.utils.camera2.Camera2Util
import java.util.*

@SuppressLint("InlinedApi")
class CameraRepository(val app: Application) {

    private var rowsToLoad = 0

    private var allLoaded = false

    var camera: CameraUtil? = null

    var camera2: Camera2Util? = null

    private val loadedImage = MutableLiveData<LinkedList<ImageModel>>(LinkedList())

    private val mPreview = MutableLiveData<CameraPreview>()

    private val flashSupported = MutableLiveData(false)

    private val flashMode = MutableLiveData(0)

    private val message = MutableLiveData<String>()

    private val imagePath = MutableLiveData<String>("")

    fun getMessage(): LiveData<String> = message

    fun getImagePath(): LiveData<String> = imagePath

    fun getCameraPreview(): LiveData<CameraPreview> = mPreview

    fun isSupportFlash(): LiveData<Boolean> = flashSupported

    fun getFlashMode(): LiveData<Int> = flashMode

    fun getLoadedImage(): LiveData<LinkedList<ImageModel>> = loadedImage

    /**
     * Fetch the data for Gallery view
     * */
    fun fetchGalleryImages(rowsPerLoad: Int) {
        val imgData = LinkedList<ImageModel>()
        try {
            // if not loaded or the fetched data still below total count
            if (!allLoaded) {
                val columns = arrayOf(
                    _ID,
                    DATE_TAKEN
                )
                val whereArgs = arrayOf("image/jpeg", "image/jpg")
                val where = "$MIME_TYPE =? OR $MIME_TYPE =?"

                // SELECT imageData WHERE MIME_TYPE =? ORDER BY DATE DESC
                val cursor = app.contentResolver.query(
                    EXTERNAL_CONTENT_URI,
                    columns, where, whereArgs, "$DATE_TAKEN DESC"
                )

                cursor?.use {
                    // get total data and set start value = past rowsToLoad value
                    val total = it.count
                    val start = rowsToLoad

                    // the update the rowsToLoad value & check if rowsPerLoad == total (allLoaded)
                    rowsToLoad += rowsPerLoad
                    rowsToLoad = if (rowsToLoad >= total) total else rowsToLoad
                    allLoaded = rowsToLoad >= total

                    // so it will fetch in sequence 0 ~ 25, 25 ~ 50, etc
                    for (i in start until rowsToLoad) {
                        it.moveToPosition(i)
                        // since MediaStore DATA is deprecated, we use the _ID to get the Uri
                        val uri = Uri.withAppendedPath(
                            EXTERNAL_CONTENT_URI,
                            it.getString(it.getColumnIndex(_ID))
                        )
                        imgData.add(ImageModel(uri))
                    }
                }
            }
        } catch (e: Exception) {
            showMessage(e.toString())
        }
        // update the value, which will automatically trigger the observer and change the view
        loadedImage.value = imgData
    }

    fun capture(capture: Boolean) {
        if (capture) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                camera2?.lockFocus()
            else
                camera?.takePicture()
        }
    }

    fun switchCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            camera2?.switchCamera()
        else {
            camera?.switchCamera()
            onResume()
        }
    }

    fun setSupportedFlash(support: Boolean) {
        flashSupported.value = support
    }

    fun setFlashMode() {
        if (flashSupported.value == true) {
            val value = ((flashMode.value ?: 0) + 1).let { if (it == 3) 0 else it }
            flashMode.value = value
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                camera2?.setFlashMode(value)
            else
                camera?.setFlashMode(value)
        }
    }

    fun onResume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            camera2?.onResume()
        else {
            camera?.onResume()
            camera?.mCamera?.let { mPreview.value = CameraPreview(app, it) }
        }

    }

    fun onPause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            camera2?.onPause()
        else {
            camera?.onPause()
            mPreview.value?.apply {
                if (this.parent != null) (this.parent as ViewGroup).removeAllViews()
            }
            mPreview.value = null
        }
    }

    fun clear() {
        loadedImage.value?.clear()
        imagePath.value = ""
        message.value = ""
    }

    fun postImagePath(path: String) {
        imagePath.postValue(path)
    }

    fun showMessage(message: String) {
        this.message.postValue(message)
    }
}