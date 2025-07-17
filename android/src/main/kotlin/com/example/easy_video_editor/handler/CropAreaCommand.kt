package com.example.easy_video_editor.handler

import android.content.Context
import androidx.media3.common.util.UnstableApi
import com.example.easy_video_editor.command.Command
import com.example.easy_video_editor.utils.OperationManager
import com.example.easy_video_editor.utils.VideoUtils
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CropAreaCommand(private val context: Context) : Command {
    @UnstableApi
    override fun execute(call: MethodCall, result: MethodChannel.Result) {
        val videoPath = call.argument<String>("videoPath")
        val x = call.argument<Int>("x")
        val y = call.argument<Int>("y")
        val width = call.argument<Int>("width")
        val height = call.argument<Int>("height")

        if (videoPath == null || x == null || y == null || width == null || height == null) {
            result.error(
                "INVALID_ARGUMENTS",
                "Missing required arguments: videoPath, x, y, width, or height",
                null
            )
            return
        }

        // Create a new scope that's tied only to this method call
        val methodScope = CoroutineScope(Dispatchers.Main + Job())

        // Register with operation manager for cancellation support
        val operationId = OperationManager.generateOperationId()
        OperationManager.registerOperation(operationId, methodScope)
        
        methodScope.launch {
            try {
                val outputPath = VideoUtils.cropArea(
                    context = context,
                    videoPath = videoPath,
                    x = x,
                    y = y,
                    width = width,
                    height = height
                )
                result.success(outputPath)
            } catch (e: Exception) {
                // Check if the error is due to user cancellation
                if (e.message?.contains("canceled by user") == true) {
                    // This is a user cancellation, so just return success with a null path
                    result.success(null)
                } else {
                    // This is a genuine error, return it
                    result.error("CROP_AREA_ERROR", e.message, null)
                }
            } finally {
                OperationManager.cancelOperation(operationId)
                methodScope.cancel()
            }
        }
    }
}