package com.example.fieldhydrotech.utils

import android.app.Activity
import android.content.Intent
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult

object QRCodeScannerUtils {

    fun startQRScanner(activity: Activity) {
        val integrator = IntentIntegrator(activity)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE) // Establece que solo se escanearán códigos QR
        integrator.setPrompt("Scan a QR Code")
        integrator.setBeepEnabled(true)
        integrator.setOrientationLocked(true)
        integrator.setCaptureActivity(PortraitCaptureActivity::class.java) // Usar la actividad personalizada
        integrator.initiateScan()
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?, callback: (String?) -> Unit) {
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result.contents != null) {
            // Llamar al callback con el contenido del QR escaneado
            callback(result.contents)
        } else {
            // Llamar al callback con null si no se encontró contenido
            callback(null)
        }
    }
}
