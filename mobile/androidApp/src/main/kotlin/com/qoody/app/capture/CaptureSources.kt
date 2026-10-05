package com.qoody.app.capture

import android.content.Context
import android.provider.Telephony
import com.qoody.app.R
import com.qoody.shared.capture.AppKind
import com.qoody.shared.capture.CapturePolicy

/**
 * The verified allowlist plus the phone's default SMS app, which covers preinstalled messaging apps
 * that are not on Google Play (Samsung Messages, `com.android.mms` on Xiaomi and others).
 * Reading the default SMS package needs no permission.
 */
class CaptureSources(
    private val defaultSmsPackage: () -> String?,
    private val defaultSmsAppName: String,
) {
    constructor(context: Context) : this(
        defaultSmsPackage = { Telephony.Sms.getDefaultSmsPackage(context) },
        defaultSmsAppName = context.getString(R.string.capture_sms_app_name),
    )

    fun kindOf(packageName: String): AppKind? =
        CapturePolicy.kindOf(packageName) ?: AppKind.Sms.takeIf { packageName == defaultSmsPackage() }

    fun appName(packageName: String): String? =
        CapturePolicy.appName(packageName) ?: defaultSmsAppName.takeIf { packageName == defaultSmsPackage() }
}
