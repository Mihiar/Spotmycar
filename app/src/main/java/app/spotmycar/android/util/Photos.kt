package app.spotmycar.android.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.max

/** Photos de la place : rangées dans le dossier privé de l'app, jamais dans la galerie. */
object Photos {
    fun newPhotoFile(context: Context): File {
        val folder = File(context.filesDir, "photos")
        folder.mkdirs()
        return File(folder, "spot_${System.currentTimeMillis()}.jpg")
    }

    /** Adresse à donner à l'appareil photo (ou à l'app de partage) pour ce fichier. */
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Charge la photo en taille raisonnable et dans le bon sens. */
    fun load(path: String, maxDimension: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sampleSize = 1
            while (max(bounds.outWidth, bounds.outHeight) / sampleSize > maxDimension) sampleSize *= 2

            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeFile(path, options) ?: return null

            val orientation = ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees == 0f) {
                bitmap
            } else {
                val matrix = Matrix().apply { postRotate(degrees) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }
}
