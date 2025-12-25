package com.example.auralens.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class CameraManager(
    private val context: Context
) {
    private var imageCapture: ImageCapture? = null
    // Create a dedicated background executor for camera operations
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e("CameraManager", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    suspend fun takePhoto(): Bitmap = suspendCancellableCoroutine { cont ->
        val imageCapture = imageCapture ?: return@suspendCancellableCoroutine cont.resumeWithException(
            IllegalStateException("Camera not initialized")
        )

        // Capture on the background executor to avoid blocking Main Thread
        imageCapture.takePicture(
            cameraExecutor, 
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraManager", "Photo capture failed: ${exc.message}", exc)
                    cont.resumeWithException(exc)
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val bitmap = image.toBitmap()
                        val rotation = image.imageInfo.rotationDegrees
                        
                        // optimization: downscale if too large (e.g., max 1024px) to speed up Gemini processing
                        // and reduce memory pressure.
                        val scaledBitmap = scaleBitmapDown(bitmap, 1024)
                        
                        val finalBitmap = if (rotation != 0) {
                            rotateBitmap(scaledBitmap, rotation.toFloat())
                        } else {
                            scaledBitmap
                        }
                        
                        // If we didn't use the original bitmap, we might want to recycle it if mutable, 
                        // but toBitmap() grants us ownership.
                        
                        cont.resume(finalBitmap)
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    } finally {
                        image.close()
                    }
                }
            }
        )
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var newWidth = originalWidth
        var newHeight = originalHeight

        if (originalWidth > maxDimension || originalHeight > maxDimension) {
            val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
            if (originalWidth > originalHeight) {
                newWidth = maxDimension
                newHeight = (newWidth / aspectRatio).toInt()
            } else {
                newHeight = maxDimension
                newWidth = (newHeight * aspectRatio).toInt()
            }
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
        return bitmap
    }
    
    fun shutdown() {
        cameraExecutor.shutdown()
    }
}
