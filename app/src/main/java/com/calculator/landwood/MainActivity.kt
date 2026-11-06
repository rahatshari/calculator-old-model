package com.calculator.landwood

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF1F5F9)
                ) {
                    LandWoodApp()
                }
            }
        }
    }
}

// Data class for GitHub release info
data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val downloadUrl: String,
    val releasePage: String,
    val changelog: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandWoodApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedScreen by remember { mutableStateOf(0) }
    val screens = listOf("জমি পরিমাপ", "নল হিসাব", "মাটি কাটা", "কাঠ পরিমাপ")

    // GitHub Repo configuration (default is the user's repository)
    val sharedPrefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var githubRepo by remember {
        mutableStateOf(sharedPrefs.getString("github_repo", "rahatshari/calculator-old-model") ?: "rahatshari/calculator-old-model")
    }

    // Update modal states
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isDownloadingApk by remember { mutableStateOf(false) }
    var downloadProgressText by remember { mutableStateOf("") }

    val currentVersionName = BuildConfig.VERSION_NAME

    // Check for updates function
    fun checkForUpdates(manual: Boolean) {
        isCheckingUpdate = true
        coroutineScope.launch {
            val result = checkGitHubLatestRelease(githubRepo, currentVersionName)
            isCheckingUpdate = false
            updateResult = result
            if (manual || result.hasUpdate) {
                showUpdateDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "জমি ও কাঠ পরিমাপ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "সংস্করণ: v$currentVersionName",
                            fontSize = 12.sp,
                            color = Color(0xFF93C5FD)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { checkForUpdates(manual = true) }) {
                        Text("🔄", fontSize = 18.sp)
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Text("⚙️", fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF123E6D)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                screens.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedScreen == index,
                        onClick = { selectedScreen = index },
                        label = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedScreen == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Text(
                                text = when (index) {
                                    0 -> "📐"
                                    1 -> "📏"
                                    2 -> "🚜"
                                    else -> "🪵"
                                },
                                fontSize = 20.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF123E6D),
                            selectedTextColor = Color(0xFF123E6D),
                            indicatorColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when (selectedScreen) {
                0 -> LandSurveyScreen()
                1 -> NolSurveyScreen()
                2 -> EarthExcavationScreen()
                3 -> TimberScreen()
            }
        }
    }

    // In-App Update Dialog
    if (showUpdateDialog && updateResult != null) {
        val info = updateResult!!
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = {
                Text(
                    text = if (info.hasUpdate) "🎉 নতুন আপডেট উপলব্ধ!" else "✅ অ্যাপটি আপ-টু-ডেট আছে",
                    fontWeight = FontWeight.Bold,
                    color = if (info.hasUpdate) Color(0xFF15803D) else Color(0xFF123E6D)
                )
            },
            text = {
                Column {
                    if (info.hasUpdate) {
                        Text(
                            text = "নতুন সংস্করণ: v${info.latestVersionName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF15803D)
                        )
                        Text(
                            text = "বর্তমান সংস্করণ: v$currentVersionName",
                            fontSize = 13.5.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = info.changelog,
                            fontSize = 13.5.sp,
                            color = Color(0xFF334155)
                        )
                        if (isDownloadingApk) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(downloadProgressText, fontSize = 12.sp, color = Color(0xFF2563EB))
                        }
                    } else {
                        Text(
                            text = "আপনি অ্যাপের সর্বশেষ সংস্করণ (v$currentVersionName) ব্যবহার করছেন। কোনো নতুন আপডেট নেই।",
                            fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (info.hasUpdate) {
                    Button(
                        onClick = {
                            isDownloadingApk = true
                            downloadProgressText = "APK ডাউনলোড হচ্ছে..."
                            coroutineScope.launch {
                                downloadAndInstallApk(
                                    context = context,
                                    downloadUrl = info.downloadUrl,
                                    fileName = "calculator-update.apk",
                                    onProgress = { downloadProgressText = it },
                                    onError = { error ->
                                        isDownloadingApk = false
                                        Toast.makeText(context, "ডাউনলোডে সমস্যা: $error", Toast.LENGTH_LONG).show()
                                        // Fallback to browser
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(info.releasePage))
                                        context.startActivity(browserIntent)
                                    }
                                )
                                isDownloadingApk = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        enabled = !isDownloadingApk
                    ) {
                        Text(if (isDownloadingApk) "ডাউনলোড হচ্ছে..." else "এখনই আপডেট করুন")
                    }
                } else {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("ঠিক আছে")
                    }
                }
            },
            dismissButton = {
                if (info.hasUpdate) {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("পরে করব")
                    }
                }
            }
        )
    }

    // Settings Dialog to customize GitHub Repository
    if (showSettingsDialog) {
        var tempRepo by remember { mutableStateOf(githubRepo) }
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("আপডেট সেটিংস (GitHub Repo)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "যে GitHub রিপোজিটরি থেকে অ্যাপ স্বয়ংক্রিয়ভাবে আপডেট ডাউনলোড করবে:",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tempRepo,
                        onValueChange = { tempRepo = it },
                        label = { Text("ব্যবহারকারী/রিপোজিটরি") },
                        placeholder = { Text("rahatshari/calculator-old-model") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "ডিফল্ট: rahatshari/calculator-old-model",
                        fontSize = 12.sp,
                        color = Color(0xFF2563EB)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        githubRepo = tempRepo.trim()
                        sharedPrefs.edit().putString("github_repo", githubRepo).apply()
                        showSettingsDialog = false
                        Toast.makeText(context, "সেটিংস সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF123E6D))
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// GitHub রিলিজ এপিআই চেক করার ফাংশন
// -------------------------------------------------------------
suspend fun checkGitHubLatestRelease(repo: String, currentVersion: String): UpdateInfo {
    return withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.github.com/repos/$repo/releases/latest"
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 7000
            connection.readTimeout = 7000

            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val tagName = root.optString("tag_name", "").removePrefix("v").trim()
                val htmlUrl = root.optString("html_url", "https://github.com/$repo/releases/latest")
                val body = root.optString("body", "নতুন আপডেট প্রকাশিত হয়েছে।")

                var apkDownloadUrl = "https://github.com/$repo/releases/latest/download/calculator-latest.apk"
                val assets = root.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            apkDownloadUrl = asset.optString("browser_download_url", apkDownloadUrl)
                            break
                        }
                    }
                }

                // Check if newer version
                val hasUpdate = isNewerVersion(tagName, currentVersion)
                UpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersionName = tagName,
                    downloadUrl = apkDownloadUrl,
                    releasePage = htmlUrl,
                    changelog = body
                )
            } else {
                UpdateInfo(false, currentVersion, "", "https://github.com/$repo/releases", "কোনো রিলিজ পাওয়া যায়নি।")
            }
        } catch (e: Exception) {
            UpdateInfo(false, currentVersion, "", "https://github.com/$repo/releases", "ইন্টারনেট কানেকশন চেক করুন।")
        }
    }
}

// Version comparison helper: returns true if latest > current
fun isNewerVersion(latest: String, current: String): Boolean {
    if (latest.isEmpty()) return false
    val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
    val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

    val length = maxOf(latestParts.size, currentParts.size)
    for (i in 0 until length) {
        val l = latestParts.getOrElse(i) { 0 }
        val c = currentParts.getOrElse(i) { 0 }
        if (l > c) return true
        if (l < c) return false
    }
    return false
}

// -------------------------------------------------------------
// সরাসরি APK ডাউনলোড এবং ইনস্টল করার নিরাপদ ফাংশন
// -------------------------------------------------------------
suspend fun downloadAndInstallApk(
    context: Context,
    downloadUrl: String,
    fileName: String,
    onProgress: (String) -> Unit,
    onError: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            onProgress("ডাউনলোড শুরু হচ্ছে...")
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 10000
            connection.readTimeout = 20000

            var redirectCount = 0
            var currentConnection = connection
            while (currentConnection.responseCode in listOf(301, 302, 303, 307, 308) && redirectCount < 5) {
                val newUrl = currentConnection.getHeaderField("Location")
                currentConnection.disconnect()
                currentConnection = URL(newUrl).openConnection() as HttpURLConnection
                currentConnection.connectTimeout = 10000
                currentConnection.readTimeout = 20000
                redirectCount++
            }

            if (currentConnection.responseCode != 200) {
                withContext(Dispatchers.Main) {
                    onError("সার্ভার রেসপন্স কোড: ${currentConnection.responseCode}")
                }
                return@withContext
            }

            val fileLength = currentConnection.contentLength
            val cacheDir = File(context.cacheDir, "updates")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val apkFile = File(cacheDir, fileName)

            currentConnection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val data = ByteArray(8192)
                    var total: Long = 0
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        output.write(data, 0, count)
                        if (fileLength > 0) {
                            val percent = (total * 100 / fileLength).toInt()
                            withContext(Dispatchers.Main) {
                                onProgress("ডাউনলোড হচ্ছে: $percent%")
                            }
                        }
                    }
                    output.flush()
                }
            }

            withContext(Dispatchers.Main) {
                onProgress("ইনস্টলেশন শুরু হচ্ছে...")
                installApk(context, apkFile)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e.message ?: "অজানা সমস্যা")
            }
        }
    }
}

fun installApk(context: Context, file: File) {
    try {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "ইনস্টলার খুলতে সমস্যা: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// -------------------------------------------------------------
// ১. জমি পরিমাপ স্ক্রিন
// -------------------------------------------------------------
@Composable
fun LandSurveyScreen() {
    var subTab by remember { mutableStateOf(0) }
    val subTabs = listOf("৪ বাহু (গড়)", "৩ বাহু (হেরন্স)")

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        TabRow(
            selectedTabIndex = subTab,
            containerColor = Color(0xFFE2E8F0),
            contentColor = Color(0xFF123E6D)
        ) {
            subTabs.forEachIndexed { index, tabTitle ->
                Tab(
                    selected = subTab == index,
                    onClick = { subTab = index },
                    text = { Text(tabTitle, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (subTab == 0) {
            FourSidedLandCard()
        } else {
            ThreeSidedHeronsCard()
        }
    }
}

@Composable
fun FourSidedLandCard() {
    var l1Feet by remember { mutableStateOf("") }
    var l1Inch by remember { mutableStateOf("") }
    var l2Feet by remember { mutableStateOf("") }
    var l2Inch by remember { mutableStateOf("") }

    var w1Feet by remember { mutableStateOf("") }
    var w1Inch by remember { mutableStateOf("") }
    var w2Feet by remember { mutableStateOf("") }
    var w2Inch by remember { mutableStateOf("") }

    var resultText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("দৈর্ঘ্য ১ ও দৈর্ঘ্য ২ (ফুট ও ইঞ্চি)", fontWeight = FontWeight.Bold, color = Color(0xFF123E6D))
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InputBox(value = l1Feet, onValueChange = { l1Feet = it }, label = "দৈর্ঘ্য ১ (ফুট)", modifier = Modifier.weight(1f))
                InputBox(value = l1Inch, onValueChange = { l1Inch = it }, label = "ইঞ্চি", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InputBox(value = l2Feet, onValueChange = { l2Feet = it }, label = "দৈর্ঘ্য ২ (ফুট)", modifier = Modifier.weight(1f))
                InputBox(value = l2Inch, onValueChange = { l2Inch = it }, label = "ইঞ্চি", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("প্রস্থ ১ ও প্রস্থ ২ (ফুট ও ইঞ্চি)", fontWeight = FontWeight.Bold, color = Color(0xFF123E6D))
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InputBox(value = w1Feet, onValueChange = { w1Feet = it }, label = "প্রস্থ ১ (ফুট)", modifier = Modifier.weight(1f))
                InputBox(value = w1Inch, onValueChange = { w1Inch = it }, label = "ইঞ্চি", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InputBox(value = w2Feet, onValueChange = { w2Feet = it }, label = "প্রস্থ ২ (ফুট)", modifier = Modifier.weight(1f))
                InputBox(value = w2Inch, onValueChange = { w2Inch = it }, label = "ইঞ্চি", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val l1 = (l1Feet.toDoubleOrNull() ?: 0.0) + ((l1Inch.toDoubleOrNull() ?: 0.0) / 12.0)
                    val l2Raw = (l2Feet.toDoubleOrNull() ?: 0.0) + ((l2Inch.toDoubleOrNull() ?: 0.0) / 12.0)
                    val l2 = if (l2Raw > 0.0) l2Raw else l1

                    val w1 = (w1Feet.toDoubleOrNull() ?: 0.0) + ((w1Inch.toDoubleOrNull() ?: 0.0) / 12.0)
                    val w2Raw = (w2Feet.toDoubleOrNull() ?: 0.0) + ((w2Inch.toDoubleOrNull() ?: 0.0) / 12.0)
                    val w2 = if (w2Raw > 0.0) w2Raw else w1

                    val avgL = (l1 + l2) / 2.0
                    val avgW = (w1 + w2) / 2.0
                    val sqft = avgL * avgW

                    if (sqft > 0.0) {
                        val shotok = sqft / 435.6
                        val katha = sqft / 720.0
                        val bigha = sqft / 14400.0
                        val acre = sqft / 43560.0
                        val ganda = shotok / 2.0

                        resultText = "📐 মোট ক্ষেত্রফল: ${"%.2f".format(sqft)} বর্গফুট\n\n" +
                                     "⭐ শতাংশ (শতক): ${"%.3f".format(shotok)} শতক\n" +
                                     "🌿 কাঠা: ${"%.3f".format(katha)} কাঠা\n" +
                                     "🌾 বিঘা: ${"%.4f".format(bigha)} বিঘা\n" +
                                     "🗺️ একর: ${"%.4f".format(acre)} একর\n" +
                                     "🔹 গণ্ডা: ${"%.2f".format(ganda)} গণ্ডা"
                    } else {
                        resultText = "দয়া করে দৈর্ঘ্য ও প্রস্থের সঠিক মান দিন।"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF123E6D)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("জমি হিসাব করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (resultText.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        ResultCard(resultText)
    }
}

@Composable
fun ThreeSidedHeronsCard() {
    var aSide by remember { mutableStateOf("") }
    var bSide by remember { mutableStateOf("") }
    var cSide by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("বিষমবাহু ত্রিভুজ (হেরন্স ফর্মুলা)", fontWeight = FontWeight.Bold, color = Color(0xFF123E6D))
            Text("৩টি বাহুর দৈর্ঘ্য (ফুট এককে) দিন:", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(10.dp))

            InputBox(value = aSide, onValueChange = { aSide = it }, label = "১ম বাহু A (ফুট)")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(value = bSide, onValueChange = { bSide = it }, label = "২য় বাহু B (ফুট)")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(value = cSide, onValueChange = { cSide = it }, label = "৩য় বাহু C (কর্ণ / ফুট)")

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val a = aSide.toDoubleOrNull() ?: 0.0
                    val b = bSide.toDoubleOrNull() ?: 0.0
                    val c = cSide.toDoubleOrNull() ?: 0.0

                    if (a + b > c && a + c > b && b + c > a) {
                        val s = (a + b + c) / 2.0
                        val sqft = sqrt(s * (s - a) * (s - b) * (s - c))
                        val shotok = sqft / 435.6
                        val katha = sqft / 720.0

                        resultText = "📐 ত্রিভুজের ক্ষেত্রফল: ${"%.2f".format(sqft)} বর্গফুট\n" +
                                     "⭐ শতক: ${"%.3f".format(shotok)} শতক\n" +
                                     "🌿 কাঠা: ${"%.3f".format(katha)} কাঠা"
                    } else {
                        resultText = "⚠️ পরিমাপগুলো দিয়ে ত্রিভুজ গঠন সম্ভব নয়। যেকোনো দুই বাহুর যোগফল তৃতীয় বাহু অপেক্ষা বড় হতে হবে।"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF123E6D)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("ত্রিভুজ হিসাব করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (resultText.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        ResultCard(resultText)
    }
}

// -------------------------------------------------------------
// ২. নল দ্বারা জমি পরিমাপ স্ক্রিন
// -------------------------------------------------------------
@Composable
fun NolSurveyScreen() {
    var nolSizeFeet by remember { mutableStateOf("10.5") }
    var lenNol by remember { mutableStateOf("") }
    var widNol by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("নল দ্বারা জমি পরিমাপ", fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                Spacer(modifier = Modifier.height(10.dp))

                InputBox(value = nolSizeFeet, onValueChange = { nolSizeFeet = it }, label = "১ নলের পরিমাপ (ফুট, যেমন ১০.৫)")
                Spacer(modifier = Modifier.height(8.dp))
                InputBox(value = lenNol, onValueChange = { lenNol = it }, label = "দৈর্ঘ্য (নলে)")
                Spacer(modifier = Modifier.height(8.dp))
                InputBox(value = widNol, onValueChange = { widNol = it }, label = "প্রস্থ (নলে)")

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val nolSize = nolSizeFeet.toDoubleOrNull() ?: 10.5
                        val l = lenNol.toDoubleOrNull() ?: 0.0
                        val w = widNol.toDoubleOrNull() ?: 0.0

                        val squareNol = l * w
                        val totalSqft = squareNol * (nolSize * nolSize)
                        val shotok = totalSqft / 435.6
                        val kani = totalSqft / (120.0 * nolSize * nolSize)

                        resultText = "📏 মোট বর্গনল: ${"%.2f".format(squareNol)} বর্গনল\n" +
                                     "📐 মোট ক্ষেত্রফল: ${"%.2f".format(totalSqft)} বর্গফুট\n" +
                                     "⭐ শতক: ${"%.3f".format(shotok)} শতক\n" +
                                     "🌾 কানি: ${"%.3f".format(kani)} কানি"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("নল হিসাব করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        if (resultText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ResultCard(resultText)
        }
    }
}

// -------------------------------------------------------------
// ৩. মাটি কাটার হিসাব স্ক্রিন
// -------------------------------------------------------------
@Composable
fun EarthExcavationScreen() {
    var length by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var depth by remember { mutableStateOf("") }
    var ratePerCft by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("মাটি কাটার হিসাব (CFT)", fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                Spacer(modifier = Modifier.height(10.dp))

                InputBox(value = length, onValueChange = { length = it }, label = "দৈর্ঘ্য (ফুট)")
                Spacer(modifier = Modifier.height(8.dp))
                InputBox(value = width, onValueChange = { width = it }, label = "প্রস্থ (ফুট)")
                Spacer(modifier = Modifier.height(8.dp))
                InputBox(value = depth, onValueChange = { depth = it }, label = "গভীরতা বা উচ্চতা (ফুট)")
                Spacer(modifier = Modifier.height(8.dp))
                InputBox(value = ratePerCft, onValueChange = { ratePerCft = it }, label = "প্রতি CFT কাটার খরচ (টাকা, ঐচ্ছিক)")

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val l = length.toDoubleOrNull() ?: 0.0
                        val w = width.toDoubleOrNull() ?: 0.0
                        val d = depth.toDoubleOrNull() ?: 0.0
                        val rate = ratePerCft.toDoubleOrNull() ?: 0.0

                        val cft = l * w * d
                        val trucks = cft / 100.0
                        val trolleys = cft / 50.0
                        val cost = cft * rate

                        var res = "🚜 মোট মাটির পরিমাণ: ${"%.2f".format(cft)} CFT (ঘনফুট)\n\n" +
                                  "🚛 ট্রাক সংখ্যা: ${"%.1f".format(trucks)} টি (১০০ cft ধরে)\n" +
                                  "🚜 ট্রলি সংখ্যা: ${"%.1f".format(trolleys)} টি (৫০ cft ধরে)"
                        if (rate > 0) {
                            res += "\n💰 মোট মাটি কাটার খরচ: ${cost.roundToInt()} টাকা"
                        }
                        resultText = res
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("মাটি হিসাব করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        if (resultText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ResultCard(resultText)
        }
    }
}

// -------------------------------------------------------------
// ৪. কাঠ পরিমাপ স্ক্রিন
// -------------------------------------------------------------
@Composable
fun TimberScreen() {
    var woodTab by remember { mutableStateOf(0) }
    val woodTabs = listOf("গোল কাঠ", "চ্যাপ্টা / তক্তা")

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        TabRow(
            selectedTabIndex = woodTab,
            containerColor = Color(0xFFE2E8F0),
            contentColor = Color(0xFF10B981)
        ) {
            woodTabs.forEachIndexed { index, tabTitle ->
                Tab(
                    selected = woodTab == index,
                    onClick = { woodTab = index },
                    text = { Text(tabTitle, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (woodTab == 0) {
            RoundWoodCard()
        } else {
            PlankWoodCard()
        }
    }
}

@Composable
fun RoundWoodCard() {
    var girthInches by remember { mutableStateOf("") }
    var lengthFeet by remember { mutableStateOf("") }
    var ratePerCft by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("গোল কাঠের হিসাব (CFT)", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            Text("ফর্মুলা: (গোলাই × গোলাই × দৈর্ঘ্য) ÷ ২৩০৪", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(10.dp))

            InputBox(value = girthInches, onValueChange = { girthInches = it }, label = "মাঝের গোলাই বা বেড় (ইঞ্চি)")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(value = lengthFeet, onValueChange = { lengthFeet = it }, label = "কাঠের দৈর্ঘ্য (ফুট)")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(value = ratePerCft, onValueChange = { ratePerCft = it }, label = "প্রতি CFT দর (টাকা, ঐচ্ছিক)")

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val g = girthInches.toDoubleOrNull() ?: 0.0
                    val l = lengthFeet.toDoubleOrNull() ?: 0.0
                    val rate = ratePerCft.toDoubleOrNull() ?: 0.0

                    if (g > 0 && l > 0) {
                        val cft = (g * g * l) / 2304.0
                        val totalPrice = cft * rate

                        var res = "🪵 কাঠের পরিমাণ: ${"%.2f".format(cft)} CFT\n"
                        if (rate > 0) {
                            res += "💵 মোট দাম: ${totalPrice.roundToInt()} টাকা"
                        }
                        resultText = res
                    } else {
                        resultText = "দয়া করে বেড় এবং দৈর্ঘ্যের সঠিক মান দিন।"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("গোল কাঠ হিসাব করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (resultText.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        ResultCard(resultText)
    }
}

@Composable
fun PlankWoodCard() {
    var lengthFeet by remember { mutableStateOf("") }
    var widthInches by remember { mutableStateOf("") }
    var thicknessInches by remember { mutableStateOf("") }
    var piecesCount by remember { mutableStateOf("1") }
    var ratePerCft by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("চ্যাপ্টা / তক্তা কাঠের হিসাব (CFT)", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            Text("ফর্মুলা: (দৈর্ঘ্য × প্রস্থ × পুরুত্ব × সংখ্যা) ÷ ১৪৪", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(10.dp))

            InputBox(value = lengthFeet, onValueChange = { lengthFeet = it }, label = "দৈর্ঘ্য (ফুট)")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(value = widthInches, onValueChange = { widthInches = it }, label = "প্রস্থ (ইঞ্চি)")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(value = thicknessInches, onValueChange = { thicknessInches = it }, label = "পুরুত্ব (ইঞ্চি)")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(value = piecesCount, onValueChange = { piecesCount = it }, label = "তক্তার সংখ্যা (টি)")
            Spacer(modifier = Modifier.height(8.dp))
            InputBox(value = ratePerCft, onValueChange = { ratePerCft = it }, label = "প্রতি CFT দর (টাকা, ঐচ্ছিক)")

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val l = lengthFeet.toDoubleOrNull() ?: 0.0
                    val w = widthInches.toDoubleOrNull() ?: 0.0
                    val t = thicknessInches.toDoubleOrNull() ?: 0.0
                    val count = piecesCount.toIntOrNull() ?: 1
                    val rate = ratePerCft.toDoubleOrNull() ?: 0.0

                    if (l > 0 && w > 0 && t > 0) {
                        val cft = (l * w * t * count) / 144.0
                        val totalPrice = cft * rate

                        var res = "🪵 মোট তক্তার পরিমাণ: ${"%.2f".format(cft)} CFT\n" +
                                  "🔢 সংখ্যা: $count টি\n"
                        if (rate > 0) {
                            res += "💵 মোট দাম: ${totalPrice.roundToInt()} টাকা"
                        }
                        resultText = res
                    } else {
                        resultText = "দয়া করে দৈর্ঘ্য, প্রস্থ এবং পুরুত্বের সঠিক মান দিন।"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("তক্তা হিসাব করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (resultText.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        ResultCard(resultText)
    }
}

// -------------------------------------------------------------
// কমন UI হেল্পার ফাংশন
// -------------------------------------------------------------
@Composable
fun InputBox(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF123E6D),
            unfocusedBorderColor = Color(0xFFCBD5E1)
        )
    )
}

@Composable
fun ResultCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "হিসাবের ফলাফল:",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E40AF),
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0F172A),
                lineHeight = 22.sp
            )
        }
    }
}
