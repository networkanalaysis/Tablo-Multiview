package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.network.tablo.TabloDiscoveredDevice

private val TabloPurple = Color(0xFF6B4EB2)
private val TabloPurpleLight = Color(0xFF8A6DC8)
private val DarkBg = Color(0xFF121316)
private val CardBg = Color(0xFF1E2026)
private val BorderColor = Color(0xFF2E313A)

@Composable
fun TabloSignInScreen(
    discoveredDevices: List<TabloDiscoveredDevice>,
    isDiscovering: Boolean,
    onRefreshDiscovery: () -> Unit,
    onSignIn: (email: String, password: String, hostIp: String) -> Unit,
    isLoading: Boolean,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var selectedDeviceIp by remember { mutableStateOf("") }
    var manualIp by remember { mutableStateOf("") }
    var useManualIp by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Auto-select first discovered device
    LaunchedEffect(discoveredDevices) {
        if (selectedDeviceIp.isBlank() && discoveredDevices.isNotEmpty()) {
            selectedDeviceIp = discoveredDevices.first().ipAddress
        }
    }

    val effectiveIp = if (useManualIp) manualIp.trim() else selectedDeviceIp.trim()
    val canSubmit = email.isNotBlank() && password.isNotBlank() && effectiveIp.isNotBlank() && !isLoading

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tablo App Logo / Icon Header
            Surface(
                shape = CircleShape,
                color = TabloPurple,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "Tablo",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sign in to Tablo",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Sign in with your Tablo account to access your live OTA channels and TV guide",
                color = Color(0xFF9E9E9E),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Error Banner
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF3B1818),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF8A80),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF9E9E9E))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TabloPurple,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = TabloPurpleLight,
                    unfocusedLabelColor = Color(0xFF9E9E9E),
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF9E9E9E))
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = Color(0xFF9E9E9E)
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TabloPurple,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = TabloPurpleLight,
                    unfocusedLabelColor = Color(0xFF9E9E9E),
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Device Selection Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Connected Tablo Device",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(
                    onClick = onRefreshDiscovery,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isDiscovering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = TabloPurpleLight,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Search for Tablo",
                            tint = TabloPurpleLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // List of Discovered Devices
            if (discoveredDevices.isNotEmpty() && !useManualIp) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    discoveredDevices.forEach { dev ->
                        val isSelected = selectedDeviceIp == dev.ipAddress
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) TabloPurple.copy(alpha = 0.2f) else CardBg,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) TabloPurple else BorderColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDeviceIp = dev.ipAddress }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Router,
                                    contentDescription = null,
                                    tint = if (isSelected) TabloPurpleLight else Color(0xFF9E9E9E),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dev.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${dev.ipAddress} • ${dev.model} (${dev.tunerCount} tuners)",
                                        color = Color(0xFF9E9E9E),
                                        fontSize = 12.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = TabloPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (discoveredDevices.isEmpty() && !useManualIp) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isDiscovering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = TabloPurple,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Searching local network for Tablo DVR...",
                                color = Color(0xFF9E9E9E),
                                fontSize = 13.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                tint = Color(0xFF9E9E9E),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "No Tablo automatically detected. Enter IP address below.",
                                color = Color(0xFF9E9E9E),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Manual IP Toggle or Input
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { useManualIp = !useManualIp }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (useManualIp) "← Select from discovered devices" else "+ Enter Tablo IP address manually",
                    color = TabloPurpleLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (useManualIp || (discoveredDevices.isEmpty() && !isDiscovering)) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualIp,
                    onValueChange = { manualIp = it },
                    label = { Text("Tablo Device IP Address (e.g. 192.168.1.150)") },
                    leadingIcon = {
                        Icon(Icons.Default.Router, contentDescription = null, tint = Color(0xFF9E9E9E))
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TabloPurple,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = TabloPurpleLight,
                        unfocusedLabelColor = Color(0xFF9E9E9E),
                        focusedContainerColor = CardBg,
                        unfocusedContainerColor = CardBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Submit Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSignIn(email, password, effectiveIp)
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TabloPurple,
                    disabledContainerColor = TabloPurple.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Signing In & Syncing Channels...",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Sign In & Open Guide",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
