package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.AttendanceSessionEntity
import com.example.data.model.AuditLogEntity
import com.example.data.model.DepartmentEntity
import com.example.data.model.LeaveRequestEntity
import com.example.data.model.NoticeEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TimetableEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendXDao {
  // --- USERS ---
  @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
  fun getUserById(id: String): Flow<UserEntity?>

  @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) OR LOWER(rollNo) = LOWER(:email) LIMIT 1")
  suspend fun findUserByEmailOrId(email: String): UserEntity?

  @Query("SELECT COUNT(*) FROM users")
  suspend fun getUserCount(): Int

  @Query("SELECT * FROM users WHERE role = :role LIMIT 1")
  suspend fun getFirstUserByRole(role: String): UserEntity?

  @Query("SELECT * FROM users WHERE role = :role ORDER BY name ASC")
  fun getUsersByRole(role: String): Flow<List<UserEntity>>

  @Query("SELECT * FROM users WHERE role = 'STUDENT' AND section = :section ORDER BY rollNo ASC")
  fun getStudentsBySection(section: String): Flow<List<UserEntity>>

  @Query("SELECT * FROM users ORDER BY name ASC")
  fun getAllUsers(): Flow<List<UserEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUser(user: UserEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUsers(users: List<UserEntity>)

  @Update
  suspend fun updateUser(user: UserEntity)

  @Delete
  suspend fun deleteUser(user: UserEntity)

  // --- DEPARTMENTS ---
  @Query("SELECT * FROM departments ORDER BY name ASC")
  fun getAllDepartments(): Flow<List<DepartmentEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDepartments(departments: List<DepartmentEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDepartment(department: DepartmentEntity)

  @Delete
  suspend fun deleteDepartment(department: DepartmentEntity)

  // --- SUBJECTS ---
  @Query("SELECT * FROM subjects ORDER BY name ASC")
  fun getAllSubjects(): Flow<List<SubjectEntity>>

  @Query("SELECT * FROM subjects WHERE facultyId = :facultyId ORDER BY name ASC")
  fun getSubjectsByFaculty(facultyId: String): Flow<List<SubjectEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSubjects(subjects: List<SubjectEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSubject(subject: SubjectEntity)

  @Delete
  suspend fun deleteSubject(subject: SubjectEntity)

  // --- TIMETABLE ---
  @Query("SELECT * FROM timetables ORDER BY startTime ASC")
  fun getAllTimetables(): Flow<List<TimetableEntity>>

  @Query("SELECT * FROM timetables WHERE dayOfWeek = :day ORDER BY startTime ASC")
  fun getTimetableForDay(day: String): Flow<List<TimetableEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTimetables(timetables: List<TimetableEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTimetable(timetable: TimetableEntity)

  @Delete
  suspend fun deleteTimetable(timetable: TimetableEntity)

  // --- ATTENDANCE SESSIONS ---
  @Query("SELECT * FROM attendance_sessions WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
  fun getActiveSessions(): Flow<List<AttendanceSessionEntity>>

  @Query("SELECT * FROM attendance_sessions WHERE id = :id LIMIT 1")
  fun getSessionById(id: String): Flow<AttendanceSessionEntity?>

  @Query("SELECT * FROM attendance_sessions WHERE facultyId = :facultyId ORDER BY createdAt DESC")
  fun getSessionsByFaculty(facultyId: String): Flow<List<AttendanceSessionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSession(session: AttendanceSessionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSessions(sessions: List<AttendanceSessionEntity>)

  @Query("SELECT * FROM attendance_sessions WHERE subjectId = :subjectId AND status = 'ACTIVE' LIMIT 1")
  suspend fun getActiveSessionForSubject(subjectId: String): AttendanceSessionEntity?

  @Update
  suspend fun updateSession(session: AttendanceSessionEntity)

  // --- ATTENDANCE RECORDS ---
  @Query("SELECT * FROM attendance_records ORDER BY createdAt DESC")
  fun getAllAttendanceRecords(): Flow<List<AttendanceRecordEntity>>

  @Query("SELECT * FROM attendance_records WHERE studentId = :studentId ORDER BY date DESC, time DESC")
  fun getAttendanceForStudent(studentId: String): Flow<List<AttendanceRecordEntity>>

  @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY createdAt DESC")
  fun getAttendanceForSession(sessionId: String): Flow<List<AttendanceRecordEntity>>

  @Query("UPDATE attendance_sessions SET status = 'CLOSED' WHERE status = 'ACTIVE'")
  suspend fun closeAllActiveSessions()

  @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND sessionId = :sessionId LIMIT 1")
  suspend fun checkStudentInSession(studentId: String, sessionId: String): AttendanceRecordEntity?

  @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND subjectId = :subjectId AND date = :date LIMIT 1")
  suspend fun checkStudentInSubjectOnDate(studentId: String, subjectId: String, date: String): AttendanceRecordEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttendanceRecord(record: AttendanceRecordEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttendanceRecords(records: List<AttendanceRecordEntity>)

  @Delete
  suspend fun deleteAttendanceRecord(record: AttendanceRecordEntity)

  @Query("DELETE FROM attendance_records WHERE sessionId LIKE 'SES-HIST-%' OR id LIKE 'REC-1%' OR id = 'REC-TODAY-01' OR markedVia = 'MANUAL'")
  suspend fun deleteLegacyDemoAttendanceRecords()

  @Query("DELETE FROM attendance_records")
  suspend fun deleteAllAttendanceRecords()

  // --- NOTIFICATIONS ---
  @Query("SELECT * FROM notifications WHERE userId = :userId OR userId = 'ALL' ORDER BY timestamp DESC")
  fun getNotificationsForUser(userId: String): Flow<List<NotificationEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNotification(notification: NotificationEntity)

  @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
  suspend fun markNotificationAsRead(id: String)

  // --- AUDIT LOGS ---
  @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
  fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAuditLog(log: AuditLogEntity)

  // --- LEAVE REQUESTS ---
  @Query("SELECT * FROM leave_requests ORDER BY appliedAt DESC")
  fun getAllLeaveRequests(): Flow<List<LeaveRequestEntity>>

  @Query("SELECT * FROM leave_requests WHERE studentId = :studentId OR studentRollNo = :studentRollNo OR studentRollNo = :studentId ORDER BY appliedAt DESC")
  fun getLeaveRequestsForStudent(studentId: String, studentRollNo: String = ""): Flow<List<LeaveRequestEntity>>

  @Query("""
    SELECT * FROM leave_requests 
    WHERE facultyId = :facultyId 
       OR facultyId = :facultyName 
       OR facultyName = :facultyName 
       OR facultyName LIKE '%' || :facultyName || '%'
       OR facultyId = ''
       OR :facultyRole = 'ADMIN'
    ORDER BY appliedAt DESC
  """)
  fun getLeaveRequestsForFaculty(facultyId: String, facultyName: String = "", facultyRole: String = ""): Flow<List<LeaveRequestEntity>>

  @Query("SELECT * FROM leave_requests WHERE status = 'PENDING' ORDER BY appliedAt DESC")
  fun getPendingLeaveRequests(): Flow<List<LeaveRequestEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLeaveRequest(request: LeaveRequestEntity)

  @Update
  suspend fun updateLeaveRequest(request: LeaveRequestEntity)

  @Query("UPDATE leave_requests SET status = :status, facultyRemarks = :remarks, reviewedAt = :reviewedAt WHERE id = :id")
  suspend fun updateLeaveStatus(id: String, status: String, remarks: String, reviewedAt: Long = System.currentTimeMillis())

  @Query("UPDATE leave_requests SET status = 'CANCELLED', facultyRemarks = :remarks, reviewedAt = :reviewedAt WHERE id = :id")
  suspend fun markLeaveAsCancelled(id: String, remarks: String = "Cancelled by student", reviewedAt: Long = System.currentTimeMillis()): Int

  @Query("DELETE FROM leave_requests WHERE id = :id")
  suspend fun deleteLeaveById(id: String): Int

  @Delete
  suspend fun deleteLeaveRequest(request: LeaveRequestEntity)

  @Query("DELETE FROM notifications WHERE message LIKE '%' || :keyword || '%' OR title LIKE '%' || :keyword || '%'")
  suspend fun deleteNotificationsContaining(keyword: String): Int

  // --- NOTICES / CAMPUS ANNOUNCEMENTS ---
  @Query("SELECT * FROM notices ORDER BY isPinned DESC, postedAt DESC")
  fun getAllNotices(): Flow<List<NoticeEntity>>

  @Query("SELECT * FROM notices WHERE targetAudience = 'ALL' OR targetAudience = :audience OR targetAudience = :role OR :role = 'ADMIN' ORDER BY isPinned DESC, postedAt DESC")
  fun getNoticesForTarget(audience: String, role: String): Flow<List<NoticeEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNotice(notice: NoticeEntity)

  @Delete
  suspend fun deleteNotice(notice: NoticeEntity)

  @Query("DELETE FROM notices WHERE id = :id")
  suspend fun deleteNoticeById(id: String): Int

  @Query("UPDATE notices SET isPinned = :isPinned WHERE id = :id")
  suspend fun updateNoticePin(id: String, isPinned: Boolean)
}
