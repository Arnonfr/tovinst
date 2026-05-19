package me.vinst.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class VinstShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedUrl = extractSharedUrl(intent)
        if (sharedUrl.isNullOrBlank()) {
            toastAndFinish(MSG_NO_CONNECTION)
            return
        }

        val pm = packageManager
        val targetPackage = resolveInstalledVinstPackage(pm, sharedUrl)

        if (targetPackage == null) {
            copyToClipboard(sharedUrl)
            Toast.makeText(this, MSG_VINST_NOT_INSTALLED, Toast.LENGTH_SHORT).show()
            finishAndReturnToSource()
            return
        }

        val sent = forwardToVinst(sharedUrl, targetPackage, pm)
        if (sent) {
            Toast.makeText(this, MSG_SENT, Toast.LENGTH_SHORT).show()
        } else {
            fallbackCopyAndOpenVinst(sharedUrl, targetPackage)
            Toast.makeText(this, MSG_COPIED_FALLBACK, Toast.LENGTH_SHORT).show()
        }

        finishAndReturnToSource()
    }

    private fun extractSharedUrl(incoming: Intent?): String? {
        if (incoming?.action != Intent.ACTION_SEND) return null

        incoming.getStringExtra(Intent.EXTRA_TEXT)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

        val clipText = incoming.clipData?.let { clipData ->
            (0 until clipData.itemCount)
                .asSequence()
                .mapNotNull { idx -> clipData.getItemAt(idx).coerceToText(this)?.toString() }
                .firstOrNull { it.isNotBlank() }
        }

        return clipText?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun resolveInstalledVinstPackage(pm: PackageManager, url: String): String? {
        VINST_PACKAGE_CANDIDATES.firstOrNull { packageName ->
            pm.getLaunchIntentForPackage(packageName) != null
        }?.let { return it }

        val sendProbe = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }

        pm.queryIntentActivities(sendProbe, PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { it.activityInfo?.packageName }
            .distinct()
            .firstOrNull { pkg ->
                val lower = pkg.lowercase()
                lower.contains("vinst") || lower.contains("tovinst")
            }
            ?.let { return it }

        return pm.getInstalledApplications(0)
            .firstOrNull { appInfo ->
                val pkg = appInfo.packageName.lowercase()
                pkg.contains("vinst") || pkg.contains("tovinst")
            }
            ?.packageName
    }

    private fun forwardToVinst(url: String, targetPackage: String, pm: PackageManager): Boolean {
        buildExplicitSendIntent(url, targetPackage, pm)?.let {
            startActivity(it)
            return true
        }

        buildPackageSendIntent(url, targetPackage, pm)?.let {
            startActivity(it)
            return true
        }

        buildDeepLinkIntent(url, targetPackage, pm)?.let {
            startActivity(it)
            return true
        }

        return false
    }

    private fun buildExplicitSendIntent(url: String, targetPackage: String, pm: PackageManager): Intent? {
        val probe = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = targetPackage
        }

        val targetActivity = pm.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY)
            .firstOrNull { it.activityInfo?.exported == true }
            ?.activityInfo
            ?: return null

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            setClassName(targetActivity.packageName, targetActivity.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    private fun buildPackageSendIntent(url: String, targetPackage: String, pm: PackageManager): Intent? {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            `package` = targetPackage
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        return sendIntent.takeIf { it.resolveActivity(pm) != null }
    }

    private fun buildDeepLinkIntent(url: String, targetPackage: String, pm: PackageManager): Intent? {
        val deepLinks = listOf(
            "vinst://share?url=${Uri.encode(url)}",
            "vinst://open?url=${Uri.encode(url)}",
            "https://vinst.app/share?url=${Uri.encode(url)}"
        )

        return deepLinks.asSequence()
            .map { link ->
                Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                    `package` = targetPackage
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            }
            .firstOrNull { it.resolveActivity(pm) != null }
    }

    private fun fallbackCopyAndOpenVinst(url: String, targetPackage: String) {
        copyToClipboard(url)

        packageManager.getLaunchIntentForPackage(targetPackage)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(it)
        }
    }

    private fun copyToClipboard(url: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Vinst URL", url))
    }

    private fun toastAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finishAndReturnToSource()
    }

    private fun finishAndReturnToSource() {
        moveTaskToBack(true)
        finish()
    }

    companion object {
        private val VINST_PACKAGE_CANDIDATES = listOf(
            "me.vinst.app",
            "app.tovinst",
            "com.vinst.app"
        )

        private const val MSG_SENT = "נשלח לוינסט"
        private const val MSG_NO_CONNECTION = "לא נמצא חיבור לוינסט"
        private const val MSG_COPIED_FALLBACK = "הקישור הועתק, הדבק בתוך וינסט"
        private const val MSG_VINST_NOT_INSTALLED = "וינסט לא מותקן"
    }
}
