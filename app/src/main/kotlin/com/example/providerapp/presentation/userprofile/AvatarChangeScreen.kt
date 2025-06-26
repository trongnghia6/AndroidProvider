package com.example.providerapp.presentation.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarChangeScreen(
    currentAvatarUrl: String? = null,
    onBackClick: () -> Unit,
    onAvatarSelected: (String?) -> Unit
) {
    var selectedAvatar by remember { mutableStateOf(currentAvatarUrl) }
    var isUploading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Thay đổi avatar") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Quay lại"
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = { 
                        onAvatarSelected(selectedAvatar)
                        onBackClick()
                    },
                    enabled = !isUploading
                ) {
                    Text("Lưu")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Current Avatar Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (selectedAvatar != null) {
                            // TODO: Load image with Coil
                            // AsyncImage(model = selectedAvatar, ...)
                            Text(
                                text = "IMG",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        } else {
                            Text(
                                text = "?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            )
                        }
                    }
                }
            }

            // Upload Options
            Text(
                text = "Chọn cách tải ảnh",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Camera Option
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { 
                            // TODO: Implement camera capture
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chụp ảnh",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Gallery Option
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { 
                            // TODO: Implement gallery picker
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "Gallery",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Thư viện",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Predefined Avatars
            Text(
                text = "Hoặc chọn avatar có sẵn",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(getDefaultAvatars()) { avatar ->
                    PredefinedAvatarItem(
                        avatar = avatar,
                        isSelected = selectedAvatar == avatar.url,
                        onClick = { selectedAvatar = avatar.url }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Remove Avatar Option
            OutlinedButton(
                onClick = { selectedAvatar = null },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Xóa avatar")
            }
        }
    }
}

@Composable
fun PredefinedAvatarItem(
    avatar: DefaultAvatar,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = avatar.backgroundColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = avatar.emoji,
                    fontSize = 24.sp
                )
            }
        }

        if (isSelected) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Đã chọn",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

data class DefaultAvatar(
    val id: String,
    val emoji: String,
    val backgroundColor: Color,
    val url: String
)

private fun getDefaultAvatars(): List<DefaultAvatar> {
    return listOf(
        DefaultAvatar("1", "😀", Color(0xFF42A5F5), "default_avatar_1"),
        DefaultAvatar("2", "😊", Color(0xFF66BB6A), "default_avatar_2"),
        DefaultAvatar("3", "😎", Color(0xFFFF7043), "default_avatar_3"),
        DefaultAvatar("4", "🤗", Color(0xFFAB47BC), "default_avatar_4"),
        DefaultAvatar("5", "😇", Color(0xFFFFCA28), "default_avatar_5"),
        DefaultAvatar("6", "🥰", Color(0xFFEF5350), "default_avatar_6"),
        DefaultAvatar("7", "😋", Color(0xFF26A69A), "default_avatar_7"),
        DefaultAvatar("8", "🤔", Color(0xFF78909C), "default_avatar_8"),
        DefaultAvatar("9", "😴", Color(0xFF9575CD), "default_avatar_9"),
        DefaultAvatar("10", "🤩", Color(0xFFFFB74D), "default_avatar_10"),
        DefaultAvatar("11", "😏", Color(0xFF81C784), "default_avatar_11"),
        DefaultAvatar("12", "🤨", Color(0xFFE57373), "default_avatar_12")
    )
} 