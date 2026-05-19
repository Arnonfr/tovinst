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
            toastAndFinish("לא נמצא חיבור לוינסט")
            return
        }

        val sent = forwardToVinst(sharedUrl)
        if (sent) {
            Toast.makeText(this, "נשלח לוינסט", Toast.LENGTH_SHORT).show()
        } else {
            fallbackCopyAndOpenVinst(sharedUrl)
            Toast.makeText(this, "לא נמצא חיבור לוינסט", Toast.LENGTH_SHORT).show()
        }

        finishAndReturnToSource()
    }

    private fun extractSharedUrl(incoming: Intent?): String? {
        if (incoming?.action != Intent.ACTION_SEND) return null

        incoming.getStringExtra(Intent.EXTRA_TEXT)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

        val clipText = incoming.clipData?.let { clipData ->
            (0 until clipData.itemCount)
                .asSequence()
                .mapNotNull { index -> clipData.getItemAt(index).coerceToText(this)?.toString() }
                .firstOrNull { it.isNotBlank() }
        }
        return clipText?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun forwardToVinst(url: String): Boolean {
        val pm = packageManager

        buildExplicitSendIntent(url, pm)?.let {
            startActivity(it)
            return true
        }

        buildPackageSendIntent(url, pm)?.let {
            startActivity(it)
            return true
        }

        buildDeepLinkIntent(url, pm)?.let {
            startActivity(it)
            return true
        }

        return false
    }

    private fun buildExplicitSendIntent(url: String, pm: PackageManager): Intent? {
        val probe = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = VINST_PACKAGE
        }

        val matches = pm.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY)
            .filter { it.activityInfo?.exported == true }

        val targetActivity = matches.firstOrNull()?.activityInfo ?: return null

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            setClassName(targetActivity.packageName, targetActivity.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    private fun buildPackageSendIntent(url: String, pm: PackageManager): Intent? {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            `package` = VINST_PACKAGE
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return intent.takeIf { it.resolveActivity(pm) != null }
    }

    private fun buildDeepLinkIntent(url: String, pm: PackageManager): Intent? {
        val candidates = listOf(
            "vinst://share?url=${Uri.encode(url)}",
            "vinst://open?url=${Uri.encode(url)}",
            "https://vinst.app/share?url=${Uri.encode(url)}"
        )

        return candidates
            .asSequence()
            .map { uri ->
                Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                    `package` = VINST_PACKAGE
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            }
            .firstOrNull { candidate -> candidate.resolveActivity(pm) != null }
    }

    private fun fallbackCopyAndOpenVinst(url: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Vinst URL", url))

        val launchIntent = packageManager.getLaunchIntentForPackage(VINST_PACKAGE)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        if (launchIntent != null) {
            startActivity(launchIntent)
        }
    }

    private fun finishAndReturnToSource() {
        moveTaskToBack(true)
        finish()
    }

    private fun toastAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finishAndReturnToSource()
    }

    companion object {
        private const val VINST_PACKAGE = "me.vinst.app"
    }
}
