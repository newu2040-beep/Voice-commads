package com.example.voice

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

class ActionExecutor(private val context: Context) {

    fun executeCommand(command: String): String {
        val lowerCommand = command.lowercase().trim()

        return when {
            lowerCommand.contains("turn on flashlight") || lowerCommand.contains("flashlight on") -> toggleFlashlight(true)
            lowerCommand.contains("turn off flashlight") || lowerCommand.contains("flashlight off") -> toggleFlashlight(false)
            lowerCommand.contains("open camera") -> openCamera()
            lowerCommand.contains("open gallery") -> openGallery()
            lowerCommand.startsWith("open ") -> {
                val appName = lowerCommand.removePrefix("open ").trim()
                findAndOpenApp(appName)
            }
            lowerCommand.startsWith("call ") -> {
                val contactName = lowerCommand.removePrefix("call ").trim()
                callContact(contactName)
            }
            lowerCommand.contains("bluetooth") -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth settings")
            lowerCommand.contains("wifi") || lowerCommand.contains("wi-fi") -> openSettings(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi settings")
            else -> "Command not supported."
        }
    }

    private fun toggleFlashlight(turnOn: Boolean): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, turnOn)
                "Flashlight turned ${if (turnOn) "on" else "off"}."
            } else {
                "No flashlight found on this device."
            }
        } catch (e: CameraAccessException) {
            Log.e("ActionExecutor", "CameraAccessException", e)
            "Failed to access flashlight."
        } catch (e: Exception) {
            Log.e("ActionExecutor", "Exception", e)
            "An error occurred with the flashlight."
        }
    }

    private fun openCamera(): String {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startIntent(intent, "Opening camera.")
    }

    private fun openGallery(): String {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.type = "image/*"
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startIntent(intent, "Opening gallery.")
    }

    private fun findAndOpenApp(appName: String): String {
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (appInfo in installedApps) {
            val appLabel = packageManager.getApplicationLabel(appInfo).toString().lowercase()
            if (appLabel == appName.lowercase()) {
                val intent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    return startIntent(intent, "Opening ${packageManager.getApplicationLabel(appInfo)}.")
                }
            }
        }
        
        // WhatsApp special fallback case
        if (appName.contains("whatsapp")) {
            val intent = packageManager.getLaunchIntentForPackage("com.whatsapp")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return startIntent(intent, "Opening WhatsApp.")
            }
        }
        
        return "Could not find app $appName."
    }

    private fun callContact(name: String): String {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return "Missing permissions to call contacts."
        }

        var phoneNumber: String? = null
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$name%"),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    phoneNumber = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                }
            }
        } catch (e: Exception) {
            Log.e("ActionExecutor", "Error reading contacts", e)
        }

        return if (phoneNumber != null) {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$phoneNumber")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startIntent(intent, "Calling $name.")
        } else {
            "Contact $name not found."
        }
    }

    private fun openSettings(action: String, name: String): String {
        val intent = Intent(action)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startIntent(intent, "Opening $name.")
    }

    private fun startIntent(intent: Intent, successMessage: String): String {
        return try {
            context.startActivity(intent)
            successMessage
        } catch (e: Exception) {
            Log.e("ActionExecutor", "Failed to start intent", e)
            "Failed to perform action."
        }
    }
}
