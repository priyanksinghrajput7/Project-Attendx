package com.example.data.model

import java.util.UUID

/**
 * Data model for a student's leave request containing the student's name,
 * roll number, reason for leave, date range, and current status.
 */
data class LeaveRequestModel(
  val id: String = UUID.randomUUID().toString(),
  val studentName: String,
  val rollNumber: String,
  val reason: String,
  val startDate: String, // YYYY-MM-DD
  val endDate: String,   // YYYY-MM-DD
  val numberOfDays: Int = 1,
  val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
  val leaveType: String = "SICK_LEAVE",
  val appliedAt: Long = System.currentTimeMillis()
)

/**
 * Extension mapper from Room entity to LeaveRequestModel
 */
fun LeaveRequestEntity.toModel(): LeaveRequestModel {
  return LeaveRequestModel(
    id = this.id,
    studentName = this.studentName,
    rollNumber = this.studentRollNo,
    reason = this.reason,
    startDate = this.startDate,
    endDate = this.endDate,
    numberOfDays = this.numberOfDays,
    status = this.status,
    leaveType = this.leaveType,
    appliedAt = this.appliedAt
  )
}

/**
 * Extension mapper from LeaveRequestModel to Room entity
 */
fun LeaveRequestModel.toEntity(studentId: String = "", section: String = "", facultyId: String = ""): LeaveRequestEntity {
  return LeaveRequestEntity(
    id = this.id,
    studentId = studentId.ifEmpty { "STU-${this.rollNumber}" },
    studentName = this.studentName,
    studentRollNo = this.rollNumber,
    studentRegNo = "",
    studentSection = section.ifEmpty { "General" },
    facultyId = facultyId,
    facultyName = "Assigned Faculty",
    leaveType = this.leaveType,
    startDate = this.startDate,
    endDate = this.endDate,
    numberOfDays = this.numberOfDays,
    reason = this.reason,
    status = this.status,
    facultyRemarks = "",
    appliedAt = this.appliedAt,
    reviewedAt = null
  )
}
