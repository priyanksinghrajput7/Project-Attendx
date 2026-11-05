package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
  @PrimaryKey val id: String,
  val email: String,
  val password: String = "password123",
  val name: String,
  val role: String, // "STUDENT", "FACULTY", "ADMIN"
  val department: String = "Computer Science & Engineering",
  val phone: String = "+1 (555) 234-5678",
  val isActive: Boolean = true,
  val avatarUrl: String = "",
  // Student attributes
  val rollNo: String = "",
  val regNo: String = "",
  val course: String = "B.Tech",
  val semester: String = "4th Semester",
  val section: String = "CSE-4A",
  // Faculty attributes
  val employeeId: String = "",
  val designation: String = "Assistant Professor"
)

@Entity(tableName = "departments")
data class DepartmentEntity(
  @PrimaryKey val id: String,
  val name: String,
  val code: String,
  val headOfDepartment: String,
  val studentCount: Int = 120,
  val facultyCount: Int = 12
)

@Entity(tableName = "subjects")
data class SubjectEntity(
  @PrimaryKey val id: String,
  val code: String,
  val name: String,
  val department: String,
  val semester: String,
  val facultyId: String,
  val facultyName: String,
  val room: String,
  val totalClasses: Int = 45,
  val targetAttendancePercent: Float = 75f
)

@Entity(tableName = "timetables")
data class TimetableEntity(
  @PrimaryKey val id: String,
  val dayOfWeek: String, // "Monday", "Tuesday", etc.
  val startTime: String,
  val endTime: String,
  val subjectId: String,
  val subjectName: String,
  val facultyName: String,
  val room: String,
  val section: String
)

@Entity(tableName = "attendance_sessions")
data class AttendanceSessionEntity(
  @PrimaryKey val id: String,
  val subjectId: String,
  val subjectName: String,
  val facultyId: String,
  val facultyName: String,
  val section: String,
  val date: String, // YYYY-MM-DD
  val startTime: String,
  val endTime: String,
  val room: String,
  val status: String = "ACTIVE", // "ACTIVE", "CLOSED"
  val qrToken: String,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendance_records")
data class AttendanceRecordEntity(
  @PrimaryKey val id: String,
  val studentId: String,
  val studentName: String,
  val studentRollNo: String,
  val subjectId: String,
  val subjectName: String,
  val facultyId: String,
  val section: String,
  val date: String,
  val time: String,
  val status: String, // "PRESENT", "ABSENT", "LATE"
  val sessionId: String = "",
  val markedVia: String = "MANUAL", // "MANUAL", "QR_CODE"
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
  @PrimaryKey val id: String,
  val userId: String, // student id or "ALL"
  val title: String,
  val message: String,
  val type: String, // "ATTENDANCE", "WARNING", "TIMETABLE", "ANNOUNCEMENT"
  val timestamp: Long = System.currentTimeMillis(),
  val isRead: Boolean = false
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
  @PrimaryKey val id: String,
  val actorName: String,
  val actorRole: String,
  val action: String,
  val details: String,
  val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "leave_requests")
data class LeaveRequestEntity(
  @PrimaryKey val id: String,
  val studentId: String,
  val studentName: String,
  val studentRollNo: String,
  val studentRegNo: String = "",
  val studentSection: String = "",
  val facultyId: String = "",
  val facultyName: String = "",
  val leaveType: String, // "SICK_LEAVE", "CASUAL_LEAVE", "MEDICAL", "DUTY_LEAVE", "EMERGENCY"
  val startDate: String, // YYYY-MM-DD
  val endDate: String,   // YYYY-MM-DD
  val numberOfDays: Int = 1,
  val reason: String,
  val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
  val facultyRemarks: String = "",
  val attachmentUri: String? = null,
  val appliedAt: Long = System.currentTimeMillis(),
  val reviewedAt: Long? = null
)

@Entity(tableName = "notices")
data class NoticeEntity(
  @PrimaryKey val id: String,
  val title: String,
  val content: String,
  val postedBy: String,       // e.g. "Prof. Robert Chen", "Dean of Academic Affairs"
  val authorRole: String,     // "ADMIN" or "FACULTY"
  val authorId: String = "",
  val targetAudience: String = "ALL", // "ALL", "CSE-4A", "CSE-4B", "STUDENTS", "FACULTY"
  val category: String = "GENERAL",   // "ACADEMIC", "EXAM", "HOLIDAY", "EVENT", "URGENT", "ATTENDANCE"
  val priority: String = "NORMAL",    // "NORMAL", "HIGH", "URGENT"
  val isPinned: Boolean = false,
  val attachmentUri: String? = null,
  val postedAt: Long = System.currentTimeMillis()
)

