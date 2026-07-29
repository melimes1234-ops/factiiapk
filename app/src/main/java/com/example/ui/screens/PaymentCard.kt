package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.InvoiceViewModel
import com.example.ui.components.SelectOnFocusTextField
import com.example.util.Helper
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentCard(viewModel: InvoiceViewModel, isRtl: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isRtl) "وضعیت پرداخت و تسویه" else "Payment Status",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            val statusOptions = if (isRtl) listOf("تسویه نشده", "پرداخت بخشی از مبلغ", "پرداخت شده / تسویه کامل") else listOf("Pending", "PartiallyPaid", "Paid")
            val currentStatusDisplay = when (viewModel.editorStatus) {
                "Paid" -> if (isRtl) "پرداخت شده / تسویه کامل" else "Paid"
                "PartiallyPaid" -> if (isRtl) "پرداخت بخشی از مبلغ" else "PartiallyPaid"
                else -> if (isRtl) "تسویه نشده" else "Pending"
            }

            var statusExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = it }
            ) {
                OutlinedTextField(
                    value = currentStatusDisplay,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isRtl) "وضعیت فاکتور" else "Invoice Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    statusOptions.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                viewModel.editorStatus = when (selectionOption) {
                                    "پرداخت شده / تسویه کامل", "Paid" -> "Paid"
                                    "پرداخت بخشی از مبلغ", "PartiallyPaid" -> "PartiallyPaid"
                                    else -> "Pending"
                                }
                                
                                if (viewModel.editorStatus == "Paid") {
                                    viewModel.editorAdvancePayment = viewModel.getDraftTotal()
                                } else if (viewModel.editorStatus == "Pending") {
                                    viewModel.editorAdvancePayment = 0.0
                                }
                                statusExpanded = false
                            }
                        )
                    }
                }
            }

            if (viewModel.editorStatus == "PartiallyPaid") {
                SelectOnFocusTextField(
                    value = Helper.formatWithCommas(viewModel.editorAdvancePayment, false),
                    onValueChange = { input ->
                        val parsed = Helper.parseFormattedToDouble(input)
                        viewModel.editorAdvancePayment = parsed
                    },
                    label = { Text(if (isRtl) "مبلغ پرداخت شده (تومان)" else "Paid Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            if (viewModel.editorStatus == "PartiallyPaid" || viewModel.editorStatus == "Paid") {
                val methodOptions = if (isRtl) listOf("نقد", "کارت به کارت", "حواله پایا/ساتنا", "چک", "دستگاه کارتخوان", "سایر") else listOf("Cash", "Card to Card", "Bank Transfer", "Check", "POS", "Other")
                var methodExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = methodExpanded,
                    onExpandedChange = { methodExpanded = it }
                ) {
                    OutlinedTextField(
                        value = viewModel.editorPaymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isRtl) "روش پرداخت" else "Payment Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = methodExpanded,
                        onDismissRequest = { methodExpanded = false }
                    ) {
                        methodOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    viewModel.editorPaymentMethod = selectionOption
                                    methodExpanded = false
                                }
                            )
                        }
                    }
                }

                SelectOnFocusTextField(
                    value = viewModel.editorPaymentDetails,
                    onValueChange = { viewModel.editorPaymentDetails = it },
                    label = { Text(if (isRtl) "مشخصات پرداخت (شماره چک، شماره تراکنش و ...)" else "Payment Details (Check No, Ref No, etc)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                // --- Payment Image Documents Section ---
                PaymentDocumentsPicker(
                    documentPaths = viewModel.editorPaymentDocumentPaths,
                    onPathsChanged = { newPaths ->
                        viewModel.editorPaymentDocumentPaths = newPaths
                    },
                    isRtl = isRtl
                )
            }
        }
    }
}

@Composable
fun PaymentDocumentsPicker(
    documentPaths: String,
    onPathsChanged: (String) -> Unit,
    isRtl: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val docList = remember(documentPaths) {
        documentPaths.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    var selectedImageForZoom by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newUris = uris.map { uri ->
                try {
                    val docsDir = File(context.filesDir, "payment_docs")
                    if (!docsDir.exists()) docsDir.mkdirs()
                    val file = File(docsDir, "doc_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Uri.fromFile(file).toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                    uri.toString()
                }
            }
            val combined = (docList + newUris).distinct().joinToString(",")
            onPathsChanged(combined)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val docsDir = File(context.filesDir, "payment_docs")
                if (!docsDir.exists()) docsDir.mkdirs()
                val file = File(docsDir, "doc_cam_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                val newUri = Uri.fromFile(file).toString()
                val combined = (docList + newUri).distinct().joinToString(",")
                onPathsChanged(combined)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Zoomable Dialog Popup
    selectedImageForZoom?.let { imagePath ->
        ZoomableImageDialog(
            imagePath = imagePath,
            isRtl = isRtl,
            onDismiss = { selectedImageForZoom = null }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isRtl) "مستندات تصویری پرداخت" else "Payment Image Proofs",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Responsive Status Tag
            val hasDocs = docList.isNotEmpty()
            val tagBg = if (hasDocs) Color(0xFFD1FAE5) else Color(0xFFFFEDD5)
            val tagColor = if (hasDocs) Color(0xFF047857) else Color(0xFFC2410C)
            val tagText = if (hasDocs) {
                if (isRtl) "${Helper.toPersianDigits(docList.size.toString())} مستند" else "${docList.size} Docs"
            } else {
                if (isRtl) "بدون مستند" else "No Docs"
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(tagBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = tagText,
                    color = tagColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        // Horizontal Scroll list of thumbnail cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            docList.forEachIndexed { index, path ->
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable { selectedImageForZoom = path }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(path)
                            .crossfade(true)
                            .build(),
                        contentDescription = "مستند پرداخت",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Delete button overlay
                    IconButton(
                        onClick = {
                            val updatedList = docList.filterIndexed { i, _ -> i != index }
                            onPathsChanged(updatedList.joinToString(","))
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(20.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "حذف مستند",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Button tile: Select from Gallery
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                    .clickable { galleryLauncher.launch("image/*") }
                    .testTag("add_doc_gallery_btn"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "گالری",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isRtl) "گالری" else "Gallery",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Button tile: Take photo with Camera
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(10.dp))
                    .clickable { cameraLauncher.launch(null) }
                    .testTag("add_doc_camera_btn"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "دوربین",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isRtl) "دوربین" else "Camera",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ZoomableImageDialog(
    imagePath: String,
    isRtl: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.92f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Zoomable AsyncImage
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imagePath)
                        .crossfade(true)
                        .build(),
                    contentDescription = "مشاهده بزرگ‌نمایی مستند",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                )

                // Top Close Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRtl) "مشاهده مستند (امکان بزرگ‌نمایی)" else "Document Viewer (Pinch to Zoom)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

