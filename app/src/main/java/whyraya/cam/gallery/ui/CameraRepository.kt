package whyraya.cam.gallery.ui

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import android.provider.MediaStore.Images.Media.*
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import whyraya.cam.gallery.data.ImageModel
import java.util.*

@SuppressLint("InlinedApi")
class CameraRepository(val app: Application) {

    private var rowsToLoad = 0

    private var allLoaded = false

    private val message = MutableLiveData<String>()

    private val imagePath = MutableLiveData<String>("")

    fun getMessage(): LiveData<String> = message

    fun getImagePath(): LiveData<String> = imagePath

    fun fetchGalleryImages(rowsPerLoad: Int): List<ImageModel> {
        val imgData = LinkedList<ImageModel>()
        if (!allLoaded) {
            val columns = arrayOf(
                _ID,
                BUCKET_DISPLAY_NAME,
                DATE_TAKEN,
                DISPLAY_NAME
            )
            val whereArgs = arrayOf("image/jpeg", "image/jpg")
            val where = "$MIME_TYPE =? OR $MIME_TYPE =?"

            val cursor = app.contentResolver.query(
                EXTERNAL_CONTENT_URI,
                columns, where, whereArgs, "$DATE_TAKEN DESC"
            )

            cursor?.use {
                val total = it.count
                val start = rowsToLoad

                rowsToLoad += rowsPerLoad
                rowsToLoad = if (rowsToLoad >= total) total else rowsToLoad
                allLoaded = rowsToLoad >= total

                Log.e("Test222", "rowsToLoad : $start ~ $rowsToLoad")
                for (i in start until rowsToLoad) {
                    it.moveToPosition(i)
                    val uri = Uri.withAppendedPath(
                        EXTERNAL_CONTENT_URI,
                        it.getString(it.getColumnIndex(_ID))
                    )
                    imgData.add(ImageModel().copy(uri = uri))
                }
            }
        }
        return imgData
    }

    fun setImagePath(path: String) {
        imagePath.value = path
    }

    fun postImagePath(path: String) {
        imagePath.postValue(path)
    }

    fun showMessage(message: String) {
        this.message.postValue(message)
    }
}