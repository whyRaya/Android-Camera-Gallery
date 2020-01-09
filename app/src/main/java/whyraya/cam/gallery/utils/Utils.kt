package whyraya.cam.gallery.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.UserDictionary
import android.util.Log
import android.util.TypedValue
import java.io.File
import kotlin.math.roundToInt


object Utils {

    fun getImagePath(ctx: Context) = "${imageDir(ctx)}sample_${System.currentTimeMillis()}.jpg"

    private fun imageDir(ctx: Context): String {
        val path = "${ctx.getExternalFilesDir(null)?.toString()}/EOku/image/"
        val dir = path.replace("Android/data/com.whyraya.eoku/files/", "")
        directoryExist(File(dir))
        return dir
    }

    fun dpToPx(ctx: Context) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 2f, ctx.resources.displayMetrics).roundToInt()


    private fun directoryExist(dir: File) = dir.exists() || dir.mkdirs()

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    fun getPathFromUri(context: Context, uri: Uri): String? {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            if (isExternalStorageDocument(uri)) { // ExternalStorageProvider
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if ("primary".equals(split[0], ignoreCase = true))
                    return "${context.getExternalFilesDir(null)?.absolutePath}/${split[1]}"
            }
            else if (isMediaDocument(uri)) {
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val contentUri =
                    if ("image" == split[0]) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else null
                return getDataColumn(context, contentUri, "_id=?", arrayOf(split[1]))
            }
            else if (isDownloadsDocument(uri)) {
                val contentUri = ContentUris.
                    withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), docId.toLong()
                    )
                return getDataColumn(context, contentUri, null, null)
            }
        }
        else if ("content".equals(uri.scheme, ignoreCase = true)) { // Return the remote address
            if (isGooglePhotosUri(uri)) return uri.lastPathSegment
            val singleUri =
                ContentUris.withAppendedId(UserDictionary.Words.CONTENT_URI, 316143)
            Log.e("UriPDF", "uri :$singleUri")
            return getDataColumn(context, uri, null, null)
        }
        else if ("file".equals(uri.scheme, ignoreCase = true)) // File
            return uri.path

        return null
    }

    /**
     * Get the value of the DATA column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private fun getDataColumn(context: Context, uri: Uri?,
                              selection: String?, selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor = context.contentResolver.query(
                uri!!, projection, selection, selectionArgs,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(column)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    // @return Whether the Uri authority is ExternalStorageProvider.
    private fun isExternalStorageDocument(uri: Uri?)
            = "com.android.externalstorage.documents" == uri?.authority

    //@return Whether the Uri authority is MediaProvider.
    private fun isMediaDocument(uri: Uri?)
            = "com.android.providers.media.documents" == uri?.authority

    //@return Whether the Uri authority is DownloadsProvider.
    private fun isDownloadsDocument(uri: Uri?)
            = "com.android.providers.downloads.documents" == uri?.authority

    //@return Whether the Uri authority is Google Photos.
    private fun isGooglePhotosUri(uri: Uri?)
            = "com.google.android.apps.photos.content" == uri?.authority
}