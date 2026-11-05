package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AuthScreen(
  onBackToLanding: () -> Unit,
  onLogin: (String, String, (Boolean, String) -> Unit) -> Unit,
  onQuickDemoLogin: (String) -> Unit
) {
  var emailOrId by remember { mutableStateOf("student@attendx.edu") }
  var password by remember { mutableStateOf("password123") }
  var isPasswordVisible by remember { mutableStateOf(false) }
  var rememberMe by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isLoading by remember { mutableStateOf(false) }
  var showForgotPasswordDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBackToLanding) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
          text = "Back to Home",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Branding Header
      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(Brush.linearGradient(listOf(Indigo600, Blue500))),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.School,
          contentDescription = "AttendX Logo",
          tint = Color.White,
          modifier = Modifier.size(36.dp)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "AttendX",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = (-0.5).sp
        )
      )

      Text(
        text = "Smart College Attendance Portal",
        style = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      )

      Spacer(modifier = Modifier.height(28.dp))

      // Quick Role Pill Selector
      Text(
        text = "Select demo profile to fill credentials:",
        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = emailOrId.contains("student") || emailOrId.contains("alex") || emailOrId.contains("psr"),
          onClick = {
            emailOrId = "student@attendx.edu"
            password = "password123"
            errorMessage = null
          },
          label = { Text("Student") },
          leadingIcon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp)) },
          modifier = Modifier.weight(1f)
        )
        FilterChip(
          selected = emailOrId.contains("faculty") || emailOrId.contains("robert"),
          onClick = {
            emailOrId = "faculty@attendx.edu"
            password = "password123"
            errorMessage = null
          },
          label = { Text("Faculty") },
          leadingIcon = { Icon(Icons.Default.CastForEducation, contentDescription = null, modifier = Modifier.size(16.dp)) },
          modifier = Modifier.weight(1f)
        )
        FilterChip(
          selected = emailOrId.contains("admin"),
          onClick = {
            emailOrId = "admin@attendx.edu"
            password = "password123"
            errorMessage = null
          },
          label = { Text("Admin") },
          leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp)) },
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Error Alert
      if (errorMessage != null) {
        Card(
          colors = CardDefaults.cardColors(containerColor = Rose50),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Rose600, modifier = Modifier.size(20.dp))
            Text(errorMessage ?: "", color = Rose600, style = MaterialTheme.typography.bodySmall)
          }
        }
      }

      // Input Form
      OutlinedTextField(
        value = emailOrId,
        onValueChange = {
          emailOrId = it
          errorMessage = null
        },
        label = { Text("Email or Student Roll No") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("login_email_input"),
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = password,
        onValueChange = {
          password = it
          errorMessage = null
        },
        label = { Text("Password") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
          IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
            Icon(
              imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
              contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
            )
          }
        },
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
          isLoading = true
          onLogin(emailOrId, password) { success, msg ->
            isLoading = false
            if (!success) errorMessage = msg
          }
        }),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("login_password_input"),
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Remember me and Forgot Password
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = rememberMe,
            onCheckedChange = { rememberMe = it },
            colors = CheckboxDefaults.colors(checkedColor = Indigo600)
          )
          Text(
            text = "Remember me",
            style = MaterialTheme.typography.bodySmall
          )
        }

        TextButton(onClick = { showForgotPasswordDialog = true }) {
          Text(
            text = "Forgot password?",
            style = MaterialTheme.typography.bodySmall.copy(
              color = Indigo600,
              fontWeight = FontWeight.SemiBold
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Submit Button
      Button(
        onClick = {
          isLoading = true
          onLogin(emailOrId, password) { success, msg ->
            isLoading = false
            if (!success) errorMessage = msg
          }
        },
        enabled = !isLoading && emailOrId.isNotBlank() && password.isNotBlank(),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("login_submit_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
      ) {
        if (isLoading) {
          CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
        } else {
          Text("Sign In to AttendX", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Direct Quick Access Bar
      HorizontalDivider()
      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "⚡ Instant 1-Click Role Login (Bypass typing):",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = { onQuickDemoLogin("STUDENT") },
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(vertical = 10.dp)
        ) {
          Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Student", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        }
        Button(
          onClick = { onQuickDemoLogin("FACULTY") },
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(containerColor = Blue600),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(vertical = 10.dp)
        ) {
          Icon(Icons.Default.CastForEducation, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Faculty", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        }
        Button(
          onClick = { onQuickDemoLogin("ADMIN") },
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(vertical = 10.dp)
        ) {
          Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Admin", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        }
      }
    }
  }

  if (showForgotPasswordDialog) {
    AlertDialog(
      onDismissRequest = { showForgotPasswordDialog = false },
      title = { Text("Reset Password") },
      text = {
        Text("For security purposes, password resets are handled through your college IT administrator or student registrar office. Use default demo password 'password123' to sign in.")
      },
      confirmButton = {
        Button(
          onClick = { showForgotPasswordDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
        ) {
          Text("Understood")
        }
      }
    )
  }
}
