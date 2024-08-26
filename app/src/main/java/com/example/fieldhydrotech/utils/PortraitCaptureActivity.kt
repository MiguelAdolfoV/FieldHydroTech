package com.example.fieldhydrotech.utils

import com.journeyapps.barcodescanner.CaptureActivity
import android.os.Bundle
import android.content.pm.ActivityInfo

class PortraitCaptureActivity : CaptureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}
