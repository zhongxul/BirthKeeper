package com.zhongxul.birthkeeper.feature.capture

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.zhongxul.birthkeeper.core.domain.usecase.ParseIdCardResult
import com.zhongxul.birthkeeper.core.domain.usecase.ParseIdCardUseCase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val CAPTURE_ROUTE = "capture"
const val TAG_CAPTURE_GALLERY_BUTTON = "capture_gallery_button"
const val TAG_CAPTURE_NAME_INPUT = "capture_name_input"
const val TAG_CAPTURE_ID_INPUT = "capture_id_input"
const val TAG_CAPTURE_APPLY_BUTTON = "capture_apply_button"

data class CapturePrefillResult(
    val name: String?,
    val idNumber: String?
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CaptureRoute(
    onBack: () -> Unit,
    onApplyResult: (CapturePrefillResult) -> Unit
) {
    val context = LocalContext.current
    val parseIdCardUseCase = remember { ParseIdCardUseCase() }
    val textRecognizer = remember {
        TextRecognition.getClient(
            ChineseTextRecognizerOptions.Builder().build()
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            textRecognizer.close()
        }
    }

    var name by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var rawText by remember { mutableStateOf("") }
    var idCandidates by remember { mutableStateOf<List<String>>(emptyList()) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isRecognizing by remember { mutableStateOf(false) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    fun applyParseResult(recognizedText: String) {
        rawText = recognizedText
        val parseResult = IdCardOcrParser.parse(recognizedText) { candidate ->
            parseIdCardUseCase(candidate) is ParseIdCardResult.Valid
        }
        if (!parseResult.name.isNullOrBlank()) {
            name = parseResult.name
        }
        if (!parseResult.idNumber.isNullOrBlank()) {
            idNumber = parseResult.idNumber
        }
        idCandidates = parseResult.idCandidates
        errorText = if (parseResult.idNumber.isNullOrBlank()) {
            "未识别到合法身份证号，请尝试重新拍照或手动修正"
        } else {
            null
        }
    }

    fun recognize(inputImage: InputImage, deleteTempAfterDone: Boolean) {
        isRecognizing = true
        textRecognizer.process(inputImage)
            .addOnSuccessListener { result ->
                applyParseResult(result.text)
            }
            .addOnFailureListener {
                errorText = "识别失败，请重试或手动录入"
            }
            .addOnCompleteListener {
                isRecognizing = false
                if (deleteTempAfterDone) {
                    pendingCameraFile?.delete()
                    pendingCameraFile = null
                }
            }
    }

    fun recognizeFromUri(uri: Uri, deleteTempAfterDone: Boolean) {
        runCatching {
            InputImage.fromFilePath(context, uri)
        }.onSuccess { inputImage ->
            recognize(inputImage, deleteTempAfterDone)
        }.onFailure {
            errorText = "图片读取失败，请重试"
            if (deleteTempAfterDone) {
                pendingCameraFile?.delete()
                pendingCameraFile = null
            }
        }
    }

    fun recognizeFromBitmap(bitmap: Bitmap) {
        runCatching {
            InputImage.fromBitmap(bitmap, 0)
        }.onSuccess { inputImage ->
            recognize(inputImage, deleteTempAfterDone = false)
        }.onFailure {
            errorText = "拍照结果读取失败，请重试"
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            recognizeFromUri(uri, deleteTempAfterDone = false)
        }
    }
    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            recognizeFromBitmap(bitmap)
        }
    }
    val cameraFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        if (success && file != null) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.capture.fileprovider",
                file
            )
            recognizeFromUri(uri, deleteTempAfterDone = true)
        } else {
            pendingCameraFile?.delete()
            pendingCameraFile = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "OCR 识别") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        errorText = null
                        cameraPreviewLauncher.launch(null)
                    }
                ) {
                    Text(text = "快速拍照")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        errorText = null
                        val tempFile = createTempImageFile(context)
                        pendingCameraFile = tempFile
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.capture.fileprovider",
                            tempFile
                        )
                        cameraFileLauncher.launch(uri)
                    }
                ) {
                    Text(text = "拍照（带清理）")
                }
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TAG_CAPTURE_GALLERY_BUTTON),
                onClick = {
                    errorText = null
                    galleryLauncher.launch("image/*")
                }
            ) {
                Text(text = "相册选图")
            }
            if (isRecognizing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text(
                        text = " 正在识别...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TAG_CAPTURE_NAME_INPUT),
                label = { Text(text = "姓名（可手动修正）") },
                singleLine = true
            )
            OutlinedTextField(
                value = idNumber,
                onValueChange = { idNumber = it.uppercase() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TAG_CAPTURE_ID_INPUT),
                label = { Text(text = "身份证号（可手动修正）") },
                singleLine = true
            )
            if (idCandidates.isNotEmpty()) {
                Text(
                    text = "候选身份证号：${idCandidates.joinToString(" / ")}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!errorText.isNullOrBlank()) {
                Text(
                    text = errorText.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            OutlinedTextField(
                value = rawText,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "OCR 原始文本") },
                readOnly = true,
                minLines = 4
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TAG_CAPTURE_APPLY_BUTTON),
                onClick = {
                    onApplyResult(
                        CapturePrefillResult(
                            name = name.ifBlank { null },
                            idNumber = idNumber.ifBlank { null }
                        )
                    )
                },
                enabled = name.isNotBlank() || idNumber.isNotBlank()
            ) {
                Text(text = "回填到联系人")
            }
        }
    }
}

private fun createTempImageFile(context: Context): File {
    val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date())
    val dir = File(context.cacheDir, "ocr").apply { mkdirs() }
    return File.createTempFile("ocr_$time", ".jpg", dir)
}
