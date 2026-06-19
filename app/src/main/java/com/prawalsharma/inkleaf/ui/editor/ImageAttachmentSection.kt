package com.prawalsharma.inkleaf.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.prawalsharma.inkleaf.R
import com.prawalsharma.inkleaf.utils.ImageAttachmentStore

@Composable
fun ImageAttachmentSection(
    imageUris: List<String>,
    contentColor: Color,
    onImageAdded: (String) -> Unit,
    onImageRemoved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var fullScreenUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(imageUris) {
        imageUris
            .filter { it.startsWith("content://") }
            .forEach { uriString ->
                val localPath = ImageAttachmentStore.copyIntoAppStorage(context, Uri.parse(uriString))
                if (localPath != null) {
                    onImageAdded(localPath)
                    onImageRemoved(uriString)
                }
            }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            // Keep a local app-owned copy so photos do not disappear when
            // the source provider revokes or deletes temporary URI access.
            val localPath = ImageAttachmentStore.copyIntoAppStorage(context, uri)
            if (localPath != null) {
                onImageAdded(localPath)
                return@forEach
            }

            // Fallback for unusual providers that refuse streams. This may not
            // survive provider cleanup, but preserves the current-session image.
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Provider doesn't support persistent grants; image will still load this session.
            }
            onImageAdded(uri.toString())
        }
    }

    if (imageUris.isEmpty()) {
        // Show an "Attach image" AssistChip
        AssistChip(
            onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            label = { Text(stringResource(R.string.btn_attach_image), style = MaterialTheme.typography.labelSmall) },
            leadingIcon = { Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp)) }
        )
    } else {
        Column(modifier = modifier) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    stringResource(R.string.images_count, imageUris.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.6f)
                )
                Spacer(Modifier.weight(1f))
                AssistChip(
                    onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    label = { Text(stringResource(R.string.cd_add_image), style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp)) }
                )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(imageUris) { uriString ->
                    Box(modifier = Modifier.size(80.dp)) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(ImageAttachmentStore.modelFor(uriString))
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, contentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable { fullScreenUri = uriString }
                        )
                        // Remove button
                        IconButton(
                            onClick = {
                                ImageAttachmentStore.deleteIfOwned(uriString)
                                onImageRemoved(uriString)
                            },
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        ) {
                            Icon(
                                Icons.Default.Close, stringResource(R.string.cd_remove_image),
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Full-screen image viewer
    fullScreenUri?.let { uri ->
        var scale by remember(uri) { mutableFloatStateOf(1f) }
        var offset by remember(uri) { mutableStateOf(Offset.Zero) }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { fullScreenUri = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { fullScreenUri = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(ImageAttachmentStore.modelFor(uri))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(uri) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 6f)
                                scale = nextScale
                                offset = if (nextScale == 1f) {
                                    Offset.Zero
                                } else {
                                    offset + pan
                                }
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
                IconButton(
                    onClick = { fullScreenUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close), tint = Color.White)
                }
            }
        }
    }
}
