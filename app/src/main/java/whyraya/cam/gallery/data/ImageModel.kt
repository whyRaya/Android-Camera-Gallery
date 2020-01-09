package whyraya.cam.gallery.data

import android.graphics.Bitmap
import android.net.Uri

data class ImageModel(var bitmap: Bitmap? = null, var filePath: String = "", var uri: Uri? = null) {

    val imgExist: Boolean get() = bitmap != null || filePath.isNotEmpty()

    val image get() = if (filePath.isNotEmpty()) filePath else uri
}
