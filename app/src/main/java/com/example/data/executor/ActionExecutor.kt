package com.example.data.executor

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import com.example.data.model.VeloAction
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActionExecutor(private val context: Context) {

    data class ExecutionResult(
        val success: Boolean,
        val message: String,
        val speechNarration: String
    )

    fun execute(action: VeloAction): ExecutionResult {
        return when (action) {
            is VeloAction.OpenApp -> executeOpenApp(action.appName)
            is VeloAction.PlayMusic -> executePlayMusic(action.query)
            is VeloAction.CheckMessages -> executeCheckMessages(action.platform)
            is VeloAction.GetTime -> executeGetTime()
            is VeloAction.GeneralChat -> executeGeneralChat(action.reply)
        }
    }

    private fun executeOpenApp(appName: String): ExecutionResult {
        val cleanName = appName.lowercase().trim()

        // 1. Check system action shortcuts
        when (cleanName) {
            "camera" -> {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return tryStartIntent(cameraIntent, "कैमरा खोला जा रहा है", "Opening Camera app")
            }
            "settings" -> {
                val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return tryStartIntent(settingsIntent, "सेटिंग्स खोली जा रही है", "Opening Settings")
            }
            "calculator" -> {
                val calcPackages = listOf(
                    "com.google.android.calculator",
                    "com.android.calculator2",
                    "com.sec.android.app.popupcalculator"
                )
                for (pkg in calcPackages) {
                    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return ExecutionResult(true, "Calculator launched", "कैलकुलेटर खोला जा रहा है")
                    }
                }
            }
        }

        // 2. Map app aliases to package names and web fallbacks
        val appInfo = resolveAppMapping(cleanName)
        val packageManager = context.packageManager

        // Try direct launch via package manager
        val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return try {
                context.startActivity(launchIntent)
                ExecutionResult(
                    success = true,
                    message = "${appInfo.displayName} app opened",
                    speechNarration = "${appInfo.displayName} खोला जा रहा है"
                )
            } catch (e: Exception) {
                ExecutionResult(
                    success = false,
                    message = "Could not start ${appInfo.displayName}: ${e.localizedMessage}",
                    speechNarration = "${appInfo.displayName} खोलने में समस्या आई"
                )
            }
        }

        // Fallback: Check all installed launchable apps for fuzzy name match
        val matchedPkg = findInstalledAppByName(cleanName, packageManager)
        if (matchedPkg != null) {
            val intent = packageManager.getLaunchIntentForPackage(matchedPkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return try {
                    context.startActivity(intent)
                    ExecutionResult(
                        success = true,
                        message = "App launched: $matchedPkg",
                        speechNarration = "$appName खोला जा रहा है"
                    )
                } catch (e: Exception) {
                    ExecutionResult(false, e.localizedMessage ?: "Failed", "ऐप नहीं खुल पाया")
                }
            }
        }

        // 3. Fallback via browser / play store URL if app is not installed
        if (appInfo.webFallbackUrl != null) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(appInfo.webFallbackUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(webIntent)
                ExecutionResult(
                    success = true,
                    message = "${appInfo.displayName} opened via Web (app not installed)",
                    speechNarration = "${appInfo.displayName} वेब पर खोला जा रहा है"
                )
            } catch (e: Exception) {
                ExecutionResult(false, "App not found: $appName", "$appName फोन में नहीं मिला")
            }
        }

        return ExecutionResult(
            success = false,
            message = "App '$appName' is not installed on this device",
            speechNarration = "$appName आपके फोन में नहीं मिला"
        )
    }

    private fun executePlayMusic(query: String): ExecutionResult {
        val trimmedQuery = query.trim()
        val narration = if (trimmedQuery.isNotEmpty()) {
            "$trimmedQuery बजाया जा रहा है"
        } else {
            "गाना बजाया जा रहा है"
        }

        // Try launching media player with search query
        val mediaIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            putExtra(SearchManager.QUERY, trimmedQuery)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val resolveInfo = context.packageManager.resolveActivity(mediaIntent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolveInfo != null) {
            try {
                context.startActivity(mediaIntent)
                return ExecutionResult(
                    success = true,
                    message = if (trimmedQuery.isNotEmpty()) "Playing: $trimmedQuery" else "Playing music",
                    speechNarration = narration
                )
            } catch (_: Exception) {}
        }

        // Try YouTube / Web search fallback for exact music video
        val searchUrl = if (trimmedQuery.isNotEmpty()) {
            "https://www.youtube.com/results?search_query=" + URLEncoder.encode(trimmedQuery, "UTF-8")
        } else {
            "https://music.youtube.com"
        }

        val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(ytIntent)
            ExecutionResult(
                success = true,
                message = if (trimmedQuery.isNotEmpty()) "Playing '$trimmedQuery' on YouTube" else "Opening Music Player",
                speechNarration = narration
            )
        } catch (e: Exception) {
            ExecutionResult(
                success = false,
                message = "Could not launch music player: ${e.localizedMessage}",
                speechNarration = "गाना बजाने में समस्या आई"
            )
        }
    }

    private fun executeCheckMessages(platform: String): ExecutionResult {
        val cleanPlatform = platform.lowercase().trim()
        return when (cleanPlatform) {
            "whatsapp" -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    ExecutionResult(true, "WhatsApp opened for messages", "व्हाट्सएप खोला जा रहा है")
                } else {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://web.whatsapp.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(webIntent)
                        ExecutionResult(true, "WhatsApp opened in browser", "व्हाट्सएप खोला जा रहा है")
                    } catch (e: Exception) {
                        ExecutionResult(false, "WhatsApp not installed", "व्हाट्सएप फोन में नहीं मिला")
                    }
                }
            }
            "telegram" -> {
                val intent = context.packageManager.getLaunchIntentForPackage("org.telegram.messenger")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    ExecutionResult(true, "Telegram opened", "टेलीग्राम खोला जा रहा है")
                } else {
                    ExecutionResult(false, "Telegram not installed", "टेलीग्राम फोन में नहीं मिला")
                }
            }
            "instagram" -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.instagram.android")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    ExecutionResult(true, "Instagram DMs opened", "इंस्टाग्राम खोला जा रहा है")
                } else {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/direct/inbox/")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(webIntent)
                    ExecutionResult(true, "Instagram Direct opened via Web", "इंस्टाग्राम डायरेक्ट खोला जा रहा है")
                }
            }
            else -> {
                // Default Android SMS / Messaging App
                val smsIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MESSAGING)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val resolve = context.packageManager.resolveActivity(smsIntent, PackageManager.MATCH_DEFAULT_ONLY)
                if (resolve != null) {
                    context.startActivity(smsIntent)
                    ExecutionResult(true, "Messaging app opened", "मैसेज ऐप खोला जा रहा है")
                } else {
                    val fallbackSms = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("sms:")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(fallbackSms)
                        ExecutionResult(true, "SMS Inbox opened", "मैसेज चेक किए जा रहे हैं")
                    } catch (e: Exception) {
                        ExecutionResult(false, "No messaging app found", "मैसेज ऐप नहीं मिला")
                    }
                }
            }
        }
    }

    private fun executeGetTime(): ExecutionResult {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        val now = Date()
        val currentTime = timeFormat.format(now)
        val currentDate = dateFormat.format(now)

        val narration = "अभी $currentTime बजे हैं"
        return ExecutionResult(
            success = true,
            message = "Current Time: $currentTime ($currentDate)",
            speechNarration = narration
        )
    }

    private fun executeGeneralChat(reply: String): ExecutionResult {
        return ExecutionResult(
            success = true,
            message = reply,
            speechNarration = reply
        )
    }

    private fun tryStartIntent(intent: Intent, speech: String, message: String): ExecutionResult {
        return try {
            context.startActivity(intent)
            ExecutionResult(true, message, speech)
        } catch (e: Exception) {
            ExecutionResult(false, "Action failed: ${e.localizedMessage}", "यह एक्शन पूरा नहीं हो सका")
        }
    }

    private data class AppMetadata(
        val displayName: String,
        val packageName: String,
        val webFallbackUrl: String? = null
    )

    private fun resolveAppMapping(name: String): AppMetadata {
        return when (name) {
            "youtube" -> AppMetadata("YouTube", "com.google.android.youtube", "https://youtube.com")
            "instagram", "insta" -> AppMetadata("Instagram", "com.instagram.android", "https://instagram.com")
            "whatsapp" -> AppMetadata("WhatsApp", "com.whatsapp", "https://web.whatsapp.com")
            "spotify" -> AppMetadata("Spotify", "com.spotify.music", "https://open.spotify.com")
            "chrome" -> AppMetadata("Google Chrome", "com.android.chrome", "https://google.com")
            "gmail" -> AppMetadata("Gmail", "com.google.android.gm", "https://mail.google.com")
            "maps" -> AppMetadata("Google Maps", "com.google.android.apps.maps", "https://maps.google.com")
            "facebook" -> AppMetadata("Facebook", "com.facebook.katana", "https://facebook.com")
            "telegram" -> AppMetadata("Telegram", "org.telegram.messenger", "https://web.telegram.org")
            "twitter", "x" -> AppMetadata("X", "com.twitter.android", "https://x.com")
            else -> AppMetadata(name.replaceFirstChar { it.uppercase() }, "com.$name")
        }
    }

    private fun findInstalledAppByName(appName: String, pm: PackageManager): String? {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(intent, 0)
        for (app in apps) {
            val label = app.loadLabel(pm).toString().lowercase()
            if (label.contains(appName) || appName.contains(label)) {
                return app.activityInfo.packageName
            }
        }
        return null
    }
}
