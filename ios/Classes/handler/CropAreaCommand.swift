import Flutter
import AVFoundation
import Foundation

class CropAreaCommand: Command {
    func execute(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let arguments = call.arguments as? [String: Any],
              let videoPath = arguments["videoPath"] as? String,
              let x = arguments["x"] as? Int,
              let y = arguments["y"] as? Int,
              let width = arguments["width"] as? Int,
              let height = arguments["height"] as? Int else {
            result(FlutterError(
                code: "INVALID_ARGUMENTS",
                message: "Missing required arguments: videoPath, x, y, width, or height",
                details: nil
            ))
            return
        }

        // Create operation ID to track cancellation
        let operationId = OperationManager.shared.generateOperationId()

        // Declare lazy workItem to avoid early capture
        lazy var workItem: DispatchWorkItem = DispatchWorkItem {
            // Check if operation was canceled before starting
            if workItem.isCancelled {
                DispatchQueue.main.async {
                    result(nil)
                }
                return
            }

            do {
                let outputPath = try VideoUtils.cropArea(
                    videoPath: videoPath,
                    x: x,
                    y: y,
                    width: width,
                    height: height,
                    workItem: workItem
                )

                // Check if operation was canceled after processing
                if workItem.isCancelled {
                    try? FileManager.default.removeItem(atPath: outputPath)
                    DispatchQueue.main.async {
                        result(nil)
                    }
                } else {
                    DispatchQueue.main.async {
                        result(outputPath)
                    }
                }
            } catch {
                // Silently handle errors without showing error message
                DispatchQueue.main.async {
                    result(nil)
                }
            }

            // Cancel operation when completed
            OperationManager.shared.cancelOperation(operationId)
        }

        // Register work item with operation manager for possible cancellation
        OperationManager.shared.registerOperation(id: operationId, workItem: workItem)

        // Start the operation
        DispatchQueue.global(qos: .userInitiated).async(execute: workItem)
    }
}