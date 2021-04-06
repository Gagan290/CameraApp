package com.gagan.cameraapp

class Constants {

    companion object {
        // Gallery directory name to store the images or videos
        var GALLERY_DIRECTORY_NAME: String = "TPM"

        // Image and Video file extensions
        const val IMAGE_EXTENSION = "jpg"
        const val VIDEO_EXTENSION = "mp4"

        // Activity request codes
        const val CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100
        const val CAMERA_CAPTURE_VIDEO_REQUEST_CODE = 200

        // key to store image path in savedInstance state
        const val KEY_IMAGE_STORAGE_PATH = "image_path"

        const val MEDIA_TYPE_IMAGE = 1
        const val MEDIA_TYPE_VIDEO = 2

        // Bitmap sampling size
        const val BITMAP_SAMPLE_SIZE = 8
    }
}