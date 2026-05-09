package com.alex.monitorsanatate.di

import android.graphics.Bitmap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EcgSnapshotHolder @Inject constructor() {
    @Volatile var pendingBitmap: Bitmap? = null
}
