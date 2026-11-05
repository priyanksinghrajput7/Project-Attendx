package com.example.ui.screens.landing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun LandingScreen(
  onNavigateToLogin: () -> Unit,
  onQuickDemoLogin: (String) -> Unit,
  onToggleTheme: () -> Unit,
  isDarkMode: Boolean
) {
  Scaffold(
    topBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(Indigo600, Blue500))),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.School, contentDescription = null, tint = Color.White)
            }
            Text(
              text = "AttendX",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
              )
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            IconButton(onClick = onToggleTheme) {
              Icon(
                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle Theme"
              )
            }
            Button(
              onClick = onNavigateToLogin,
              colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("landing_login_button")
            ) {
              Text("Login", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(bottom = 32.dp)
    ) {
      // 1. HERO SECTION
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.verticalGradient(
                listOf(
                  Indigo600.copy(alpha = 0.08f),
                  MaterialTheme.colorScheme.background
                )
              )
            )
            .padding(horizontal = 24.dp, vertical = 32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
          ) {
            // Pill badge
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = Indigo100,
              modifier = Modifier.padding(bottom = 16.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  Icons.Default.Verified,
                  contentDescription = null,
                  tint = Indigo600,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "Track. Attend. Achieve.",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Indigo900
                  )
                )
              }
            }

            Text(
              text = "Smart Attendance.\nSmarter Campus.",
              style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp
              )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = "Manage attendance, track student performance, and simplify classroom management — all in one powerful platform.",
              style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
              ),
              modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // CTA Buttons
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                  .weight(1f)
                  .height(50.dp)
                  .testTag("get_started_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
              ) {
                Text("Get Started", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
              }

              OutlinedButton(
                onClick = { onQuickDemoLogin("STUDENT") },
                modifier = Modifier
                  .weight(1f)
                  .height(50.dp)
                  .testTag("try_demo_button"),
                shape = RoundedCornerShape(12.dp)
              ) {
                Text("Live Demo", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // 2. WHY ATTENDX?
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
          Text(
            text = "Why AttendX?",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
          )
          Text(
            text = "Built specifically for modern universities and colleges to eliminate proxy attendance and automate reports.",
            style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
          )

          FeatureHighlightCard(
            icon = Icons.Default.QrCodeScanner,
            title = "Dynamic QR Code Attendance",
            description = "Time-limited, single-device session tokens that prevent duplicate or proxy check-ins.",
            color = Indigo600
          )
          Spacer(modifier = Modifier.height(10.dp))
          FeatureHighlightCard(
            icon = Icons.Default.Analytics,
            title = "Real-Time Campus Analytics",
            description = "College, department, and semester attendance trends visualized with interactive charts.",
            color = Blue600
          )
          Spacer(modifier = Modifier.height(10.dp))
          FeatureHighlightCard(
            icon = Icons.Default.WarningAmber,
            title = "Smart Low-Attendance Alerts",
            description = "Automatic warnings when student attendance drops below college threshold (75%).",
            color = Amber600
          )
          Spacer(modifier = Modifier.height(10.dp))
          FeatureHighlightCard(
            icon = Icons.Default.PictureAsPdf,
            title = "Instant PDF & CSV Exports",
            description = "One-tap semester and monthly reports for accreditation, faculty reviews, and students.",
            color = Emerald600
          )
        }
      }

      // 3. HOW IT WORKS
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
          Text(
            text = "How It Works",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
          )
          Spacer(modifier = Modifier.height(12.dp))

          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              StepItem(
                stepNum = "1",
                title = "Teacher Launches Class Session",
                desc = "Faculty picks subject from today's timetable and starts a unique attendance session with live QR."
              )
              StepItem(
                stepNum = "2",
                title = "Student Scans with Phone",
                desc = "Students open AttendX on their device and scan the dynamic screen QR within the timer limit."
              )
              StepItem(
                stepNum = "3",
                title = "Automated Verification & Sync",
                desc = "System checks registration, records timestamp, alerts low attendance, and syncs to Room DB."
              )
            }
          }
        }
      }

      // 4. FAQ
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
          Text(
            text = "Frequently Asked Questions",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
          )
          Spacer(modifier = Modifier.height(12.dp))

          FAQItem(
            question = "Can students modify their own attendance?",
            answer = "Never. Strict role-based authorization prevents students from altering records. Only authorized faculty and college administrators have editing privileges."
          )
          FAQItem(
            question = "Does AttendX work offline?",
            answer = "Yes! Attendance records are stored locally with Room database persistence and sync reactively across sessions."
          )
          FAQItem(
            question = "How does the attendance calculator work?",
            answer = "It computes the exact number of consecutive classes required to reach the 75% college threshold and simulates the impact if a student misses the next class."
          )
        }
      }
    }
  }
}

@Composable
private fun FeatureHighlightCard(
  icon: ImageVector,
  title: String,
  description: String,
  color: Color
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          description,
          style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        )
      }
    }
  }
}

@Composable
private fun StepItem(stepNum: String, title: String, desc: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(
      modifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
        .background(Indigo600),
      contentAlignment = Alignment.Center
    ) {
      Text(stepNum, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
      Text(
        desc,
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
      )
    }
  }
}

@Composable
private fun FAQItem(question: String, answer: String) {
  var expanded by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .clickable { expanded = !expanded },
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = question,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
          modifier = Modifier.weight(1f)
        )
        Icon(
          imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
          contentDescription = null
        )
      }
      AnimatedVisibility(visible = expanded) {
        Text(
          text = answer,
          style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          modifier = Modifier.padding(top = 8.dp)
        )
      }
    }
  }
}
