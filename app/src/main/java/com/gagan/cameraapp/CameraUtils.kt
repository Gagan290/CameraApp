package com.gagan.cameraapp

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*

class CameraUtils {

    companion object {
        /**
         * Refreshes gallery on adding new image/video. Gallery won't be refreshed
         * on older devices until device is rebooted
         */
        fun refreshGallery(context: Context?, filePath: String) {
            // ScanFile so it will be appeared on Gallery
            MediaScannerConnection.scanFile(
                context, arrayOf(filePath), null
            ) { path, uri -> }
        }

        fun checkPermissions(context: Context?): Boolean {
            return ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            /*&& ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED*/
        }

        /**
         * Downsizing the bitmap to avoid OutOfMemory exceptions
         */
        fun optimizeBitmap(sampleSize: Int, filePath: String?): Bitmap {
            // bitmap factory
            val options = BitmapFactory.Options()

            // downsizing image as it throws OutOfMemory Exception for larger
            // images
            options.inSampleSize = sampleSize
            return BitmapFactory.decodeFile(filePath, options)
        }

        /**
         * Checks whether device has camera or not. This method not necessary if
         * android:required="true" is used in manifest file
         */
        fun isDeviceSupportCamera(context: Context): Boolean {
            return context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_CAMERA
            )
        }

        /**
         * Open device app settings to allow user to enable permissions
         */
        /*fun openSettings(context: Context) {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }*/



        fun getOutputMediaFileUri(context: Context, file: File?): Uri? {
            return file?.let {
                FileProvider.getUriForFile(
                    context, context.packageName + ".provider", it
                )
            }
        }

        /**
         * Creates and returns the image or video file before opening the camera
         */
        fun getOutputMediaFile(
            context: Context?,
            type: Int,
            name: String,
            extension: String
        ): File? {

            // External sdcard location
            val getImage = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val mediaStorageDir = File(
                getImage?.path,
                Constants.GALLERY_DIRECTORY_NAME
            )

            /*val mediaStorageDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Constants.GALLERY_DIRECTORY_NAME
            )*/

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.e(
                        Constants.GALLERY_DIRECTORY_NAME, "Oops! Failed create "
                                + Constants.GALLERY_DIRECTORY_NAME + " directory"
                    )
                    return null
                }
            }

            // Preparing media file naming convention
            // adds timestamp
            //val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val mediaFile: File = when (type) {
                Constants.MEDIA_TYPE_IMAGE -> {
                    /*File(
                        mediaStorageDir.path + File.separator
                                + "TPM_IMAGE_" + timeStamp + "." + Constants.IMAGE_EXTENSION
                    )*/

                    File(
                        mediaStorageDir.path + File.separator
                                + name + "." + extension
                    )
                }
                Constants.MEDIA_TYPE_VIDEO -> {
                    File(
                        mediaStorageDir.path + File.separator
                                + name + "." + extension
                    )
                }
                else -> {
                    return null
                }
            }
            return mediaFile
        }


        //**************Image capture************

        fun getPickerImageChooserIntent(context: Context?, uri: Uri?): Intent? {

            // Create a chooser from the main intent
            var chooserIntent: Intent? = null

            context?.let {
                // Determine Uri of camera image to save.
                //val outputFileUri = getCaptureImageOutputUri(context)
                val outputFileUri: Uri? = uri
                val allIntents: ArrayList<Intent> = ArrayList()
                val packageManager: PackageManager = context.packageManager

                // collect all camera intents
                val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val listCam: List<ResolveInfo> =
                    packageManager.queryIntentActivities(captureIntent, 0)
                for (res in listCam) {
                    val intent = Intent(captureIntent)
                    intent.component =
                        ComponentName(res.activityInfo.packageName, res.activityInfo.name)
                    intent.setPackage(res.activityInfo.packageName)
                    if (outputFileUri != null) {
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
                    }
                    allIntents.add(intent)
                }

                // collect all gallery intents
                val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
                galleryIntent.type = "image/*"
                val listGallery: List<ResolveInfo> =
                    packageManager.queryIntentActivities(galleryIntent, 0)
                for (res in listGallery) {
                    val intent = Intent(galleryIntent)
                    intent.component =
                        ComponentName(res.activityInfo.packageName, res.activityInfo.name)
                    intent.setPackage(res.activityInfo.packageName)
                    allIntents.add(intent)
                }

                // the main intent is the last in the list (android) so pickup the useless one
                var mainIntent: Intent? = allIntents[allIntents.size - 1]
                for (intent in allIntents) {
                    if (intent.component!!.className == "com.android.documentsui.DocumentsActivity") {
                        mainIntent = intent
                        break
                    }
                }
                allIntents.remove(mainIntent)

                // Create a chooser from the main intent
                chooserIntent = Intent.createChooser(mainIntent, "Select source")

                // Add all other intents
                chooserIntent?.putExtra(
                    Intent.EXTRA_INITIAL_INTENTS,
                    allIntents.toArray(arrayOfNulls<Parcelable>(allIntents.size))
                )
            }
            return chooserIntent
        }

        //Get URI to image received from capture by camera.
        fun getCaptureImageOutputUri(context: Context?, imageName: String): Uri? {
            var outputFileUri: Uri? = null
            //val getImage = externalCacheDir
            val getImage = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (getImage != null) {
                outputFileUri = Uri.fromFile(File(getImage.path, "$imageName.png"))
            }
            return outputFileUri
        }

        @Throws(IOException::class)
        fun rotateImageIfRequired(context: Context?, img: Bitmap, selectedImage: Uri): Bitmap? {
            val input: InputStream? =
                selectedImage?.let { it1 -> context?.contentResolver?.openInputStream(it1) }

            val ei = input?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    ExifInterface(it)
                } else {
                    TODO("VERSION.SDK_INT < N")
                }
            }

            //val ei = ExifInterface(selectedImage.path.toString())
            val orientation: Int? =
                ei?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
                else -> img
            }
        }

        private fun rotateImage(img: Bitmap, degree: Int): Bitmap? {
            //img.recycle()
            return Bitmap.createBitmap(img, 0, 0, img.width, img.height, Matrix().apply {
                postRotate(degree.toFloat())
            }, true)
        }

        fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap? {
            var width = image.width
            var height = image.height
            val bitmapRatio = width.toFloat() / height.toFloat()
            if (bitmapRatio > 0) {
                width = maxSize
                height = (width / bitmapRatio).toInt()
            } else {
                height = maxSize
                width = (height * bitmapRatio).toInt()
            }
            return Bitmap.createScaledBitmap(image, width, height, true)
        }

        fun getPickImageResultUri(context: Context?, data: Intent?, uri: Uri?): Uri? {
            var isCamera = true
            if (data != null) {
                val action = data.action
                isCamera = action != null && action == MediaStore.ACTION_IMAGE_CAPTURE
            }
            //return if (isCamera) getCaptureImageOutputUri(context) else data?.data
            return if (isCamera) uri else data?.data
        }
    }
}