package com.example.mathapplock.ui.main

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.mathapplock.Grade8MathEngine
import com.example.mathapplock.R

// Extension to convert Android Drawable to Compose ImageBitmap
fun Drawable.toImageBitmap(): ImageBitmap {
    if (this is BitmapDrawable) {
        return this.bitmap.asImageBitmap()
    }
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, width, height)
    draw(canvas)
    return bitmap.asImageBitmap()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: ((NavKey) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel? = null,
) {
    val context = LocalContext.current
    val actualViewModel: MainScreenViewModel = viewModel ?: viewModel {
        MainScreenViewModel(
            sharedPrefs = context.getSharedPreferences("app_lock_prefs", Context.MODE_PRIVATE),
            fetchApps = {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(intent, 0)
                val lockedPackages = context.getSharedPreferences("app_lock_prefs", Context.MODE_PRIVATE)
                    .getStringSet("locked_packages", emptySet()) ?: emptySet()
                val ownPackage = context.packageName

                resolveInfos
                    .map { resolveInfo ->
                        val appInfo = resolveInfo.activityInfo.applicationInfo
                        val name = pm.getApplicationLabel(appInfo).toString()
                        val packageName = appInfo.packageName
                        val icon = pm.getApplicationIcon(appInfo)
                        AppInfoItem(name, packageName, icon, lockedPackages.contains(packageName))
                    }
                    .filter { it.packageName != ownPackage }
                    .sortedBy { it.name.lowercase() }
                    .distinctBy { it.packageName }
            },
            sendUpdateBroadcast = {
                val updateIntent = Intent("com.example.mathapplock.UPDATE_LOCK_LIST").apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(updateIntent)
            },
            mathEngine = Grade8MathEngine(context)
        )
    }
    val state by actualViewModel.uiState.collectAsStateWithLifecycle()
    val challenge by actualViewModel.challengeState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    challenge?.let { activeChallenge ->
        var answerText by remember { mutableStateOf("") }
        var isErrorVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                actualViewModel.cancelUnlockChallenge()
            },
            title = {
                Text(text = "Unlock Authentication", color = Color.White)
            },
            text = {
                Column {
                    Text(
                        text = "Solve this Grade 8 equation to unlock ${activeChallenge.packageName}:",
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = activeChallenge.question.questionText,
                        color = Color(0xFFE0B0FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = answerText,
                        onValueChange = {
                            answerText = it
                            isErrorVisible = false
                        },
                        label = { Text("Answer", color = Color.LightGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val answerVal = answerText.trim().toIntOrNull()
                                if (answerVal != null && actualViewModel.verifyUnlockChallenge(answerVal)) {
                                    // Correct answer handled in VM
                                } else {
                                    isErrorVisible = true
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF9D4EDD),
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    if (isErrorVisible) {
                        Text(
                            text = "Incorrect answer. Try again.",
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val answerVal = answerText.trim().toIntOrNull()
                        if (answerVal != null && actualViewModel.verifyUnlockChallenge(answerVal)) {
                            // Correct answer handled in VM
                        } else {
                            isErrorVisible = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D4EDD))
                ) {
                    Text("Verify to Unlock")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        actualViewModel.cancelUnlockChallenge()
                    }
                ) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF2E1C4E)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_list_title),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A122B)
                )
            )
        },
        containerColor = Color(0xFF1A122B),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_placeholder), color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF9D4EDD),
                    unfocusedBorderColor = Color(0xFF3C2A59),
                    focusedContainerColor = Color(0xFF2E1C4E),
                    unfocusedContainerColor = Color(0xFF2E1C4E)
                ),
                singleLine = true
            )

            when (val uiState = state) {
                MainScreenUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF9D4EDD))
                    }
                }
                is MainScreenUiState.Success -> {
                    val filteredApps = uiState.apps.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredApps.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.no_apps_found),
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredApps, key = { it.packageName }) { appItem ->
                                AppRow(appItem = appItem, onToggle = {
                                    actualViewModel.toggleAppLock(appItem.packageName)
                                })
                            }
                        }
                    }
                }
                is MainScreenUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Error: ${uiState.throwable.localizedMessage}",
                            color = Color.Red,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppRow(appItem: AppInfoItem, onToggle: () -> Unit) {
    val context = LocalContext.current
    val imageBitmap = remember(appItem.packageName) {
        appItem.icon.toImageBitmap()
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2E1C4E)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = appItem.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = appItem.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = appItem.packageName,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Checkbox(
                checked = appItem.isLocked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF9D4EDD),
                    checkmarkColor = Color.White
                )
            )
        }
    }
}
