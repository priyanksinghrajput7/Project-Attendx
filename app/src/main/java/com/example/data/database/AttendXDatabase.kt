package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AttendXDao
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
  entities = [
    UserEntity::class,
    DepartmentEntity::class,
    SubjectEntity::class,
    TimetableEntity::class,
    AttendanceSessionEntity::class,
    AttendanceRecordEntity::class,
    NotificationEntity::class,
    AuditLogEntity::class,
    LeaveRequestEntity::class,
    NoticeEntity::class
  ],
  version = 3,
  exportSchema = false
)
abstract class AttendXDatabase : RoomDatabase() {
  abstract fun attendXDao(): AttendXDao

  companion object {
    @Volatile
    private var INSTANCE: AttendXDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope): AttendXDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AttendXDatabase::class.java,
          "attendx_college_database"
        )
        .addCallback(object : RoomDatabase.Callback() {
          override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            scope.launch(Dispatchers.IO) {
              INSTANCE?.let { populateInitialData(it.attendXDao()) }
            }
          }
          override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            scope.launch(Dispatchers.IO) {
              INSTANCE?.let { database ->
                val dao = database.attendXDao()
                if (dao.getUserCount() == 0 || dao.findUserByEmailOrId("admin@attendx.edu") == null) {
                  populateInitialData(dao)
                }
              }
            }
          }
        })
        .fallbackToDestructiveMigration()
        .build()
        INSTANCE = instance

        // Immediate background check to guarantee data is seeded on startup
        scope.launch(Dispatchers.IO) {
          try {
            val dao = instance.attendXDao()
            if (dao.getUserCount() == 0 || dao.findUserByEmailOrId("admin@attendx.edu") == null) {
              populateInitialData(dao)
            }
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }

        instance
      }
    }

    suspend fun populateInitialData(dao: AttendXDao) {
      // 1. Initial Users
      val users = listOf(
        UserEntity(
          id = "U-ADM-001",
          email = "admin@attendx.edu",
          password = "password123",
          name = "Dr. Eleanor Vance",
          role = "ADMIN",
          department = "Administration",
          phone = "+1 (555) 019-2831",
          employeeId = "EMP-ADM-01",
          designation = "Dean of Academic Affairs"
        ),
        UserEntity(
          id = "U-FAC-000",
          email = "faculty@attendx.edu",
          password = "password123",
          name = "Prof. Robert Chen",
          role = "FACULTY",
          department = "Computer Science & Engineering",
          phone = "+1 (555) 432-8760",
          employeeId = "EMP-CSE-100",
          designation = "Associate Professor"
        ),
        UserEntity(
          id = "U-FAC-001",
          email = "robert.chen@attendx.edu",
          password = "password123",
          name = "Prof. Robert Chen",
          role = "FACULTY",
          department = "Computer Science & Engineering",
          phone = "+1 (555) 432-8765",
          employeeId = "EMP-CSE-104",
          designation = "Associate Professor"
        ),
        UserEntity(
          id = "U-FAC-002",
          email = "sarah.jenkins@attendx.edu",
          password = "password123",
          name = "Dr. Sarah Jenkins",
          role = "FACULTY",
          department = "Computer Science & Engineering",
          phone = "+1 (555) 890-1234",
          employeeId = "EMP-CSE-109",
          designation = "Professor & HOD"
        ),
        UserEntity(
          id = "U-FAC-003",
          email = "david.miller@attendx.edu",
          password = "password123",
          name = "Prof. David Miller",
          role = "FACULTY",
          department = "Computer Science & Engineering",
          phone = "+1 (555) 678-4321",
          employeeId = "EMP-CSE-112",
          designation = "Assistant Professor"
        ),
        // Primary Demo Student: Student Demo / Alex Sharma
        UserEntity(
          id = "U-STD-000",
          email = "student@attendx.edu",
          password = "password123",
          name = "Alex Sharma",
          role = "STUDENT",
          department = "Computer Science & Engineering",
          phone = "+1 (555) 789-0120",
          rollNo = "CSE24-100",
          regNo = "REG-2024-8890",
          course = "B.Tech Computer Science",
          semester = "4th Semester",
          section = "CSE-4A"
        ),
        UserEntity(
          id = "U-STD-001",
          email = "alex.sharma@attendx.edu",
          password = "password123",
          name = "Alex Sharma",
          role = "STUDENT",
          department = "Computer Science & Engineering",
          phone = "+1 (555) 789-0123",
          rollNo = "CSE24-001",
          regNo = "REG-2024-8891",
          course = "B.Tech Computer Science",
          semester = "4th Semester",
          section = "CSE-4A"
        ),
        UserEntity(
          id = "U-STD-PSR",
          email = "psr@gmail.com",
          password = "123456",
          name = "PSR",
          role = "STUDENT",
          department = "Computer Science & Engineering",
          phone = "+1 (555) 789-0199",
          rollNo = "CSE24-099",
          regNo = "REG-2024-2027",
          course = "Diploma Computer Science",
          semester = "5th Semester",
          section = "CSE-4A"
        ),
        UserEntity(
          id = "U-STD-002",
          email = "priya.patel@attendx.edu",
          password = "password123",
          name = "Priya Patel",
          role = "STUDENT",
          department = "Computer Science & Engineering",
          phone = "+1 (555) 123-4567",
          rollNo = "CSE24-002",
          regNo = "REG-2024-8892",
          course = "B.Tech Computer Science",
          semester = "4th Semester",
          section = "CSE-4A"
        ),
        UserEntity(
          id = "U-STD-003",
          email = "marcus.johnson@attendx.edu",
          password = "password123",
          name = "Marcus Johnson",
          role = "STUDENT",
          department = "Computer Science & Engineering",
          phone = "+1 (555) 234-5678",
          rollNo = "CSE24-003",
          regNo = "REG-2024-8893",
          course = "B.Tech Computer Science",
          semester = "4th Semester",
          section = "CSE-4A"
        ),
        UserEntity(
          id = "U-STD-004",
          email = "ananya.sen@attendx.edu",
          password = "password123",
          name = "Ananya Sen",
          role = "STUDENT",
          department = "Computer Science & Engineering",
          phone = "+1 (555) 345-6789",
          rollNo = "CSE24-004",
          regNo = "REG-2024-8894",
          course = "B.Tech Computer Science",
          semester = "4th Semester",
          section = "CSE-4A"
        ),
        UserEntity(
          id = "U-STD-005",
          email = "liam.wilson@attendx.edu",
          password = "password123",
          name = "Liam Wilson",
          role = "STUDENT",
          department = "Computer Science & Engineering",
          phone = "+1 (555) 456-7890",
          rollNo = "CSE24-005",
          regNo = "REG-2024-8895",
          course = "B.Tech Computer Science",
          semester = "4th Semester",
          section = "CSE-4A"
        ),
        UserEntity(
          id = "U-STD-006",
          email = "chloe.zhao@attendx.edu",
          password = "password123",
          name = "Chloe Zhao",
          role = "STUDENT",
          department = "Computer Science & Engineering",
          phone = "+1 (555) 567-8901",
          rollNo = "CSE24-006",
          regNo = "REG-2024-8896",
          course = "B.Tech Computer Science",
          semester = "4th Semester",
          section = "CSE-4A"
        ),
        UserEntity(
          id = "U-STD-007",
          email = "dev.malhotra@attendx.edu",
          password = "password123",
          name = "Dev Malhotra",
          role = "STUDENT",
          department = "Computer Science & Engineering",
          phone = "+1 (555) 678-9012",
          rollNo = "CSE24-007",
          regNo = "REG-2024-8897",
          course = "B.Tech Computer Science",
          semester = "4th Semester",
          section = "CSE-4A"
        ),
        UserEntity(
          id = "U-STD-008",
          email = "sophia.martinez@attendx.edu",
          password = "password123",
          name = "Sophia Martinez",
          role = "STUDENT",
          department = "Computer Science & Engineering",
          phone = "+1 (555) 789-0124",
          rollNo = "CSE24-008",
          regNo = "REG-2024-8898",
          course = "B.Tech Computer Science",
          semester = "4th Semester",
          section = "CSE-4A"
        )
      )
      dao.insertUsers(users)

      // 2. Departments
      val departments = listOf(
        DepartmentEntity("DEP-01", "Computer Science & Engineering", "CSE", "Dr. Sarah Jenkins", 240, 18),
        DepartmentEntity("DEP-02", "Information Technology", "IT", "Dr. Rajesh Gupta", 180, 14),
        DepartmentEntity("DEP-03", "Electronics & Communication", "ECE", "Dr. Kenneth Cole", 200, 16),
        DepartmentEntity("DEP-04", "Mechanical Engineering", "ME", "Dr. Arthur Vance", 150, 12)
      )
      dao.insertDepartments(departments)

      // 3. Subjects
      val subjects = listOf(
        SubjectEntity("SUB-01", "CS401", "Computer Networks", "CSE", "4th Semester", "U-FAC-001", "Prof. Robert Chen", "Lab 204", 45, 75f),
        SubjectEntity("SUB-02", "CS402", "Operating Systems", "CSE", "4th Semester", "U-FAC-002", "Dr. Sarah Jenkins", "Hall B-12", 48, 75f),
        SubjectEntity("SUB-03", "CS403", "Database Management", "CSE", "4th Semester", "U-FAC-003", "Prof. David Miller", "Lab 102", 44, 75f),
        SubjectEntity("SUB-04", "MA401", "Discrete Mathematics", "CSE", "4th Semester", "U-FAC-002", "Dr. Alan Turing", "Room 305", 42, 75f),
        SubjectEntity("SUB-05", "CS404", "Software Engineering", "CSE", "4th Semester", "U-FAC-001", "Prof. Robert Chen", "Room 208", 40, 75f)
      )
      dao.insertSubjects(subjects)

      // 4. Timetable
      val timetables = listOf(
        TimetableEntity("TT-01", "Monday", "09:00 AM", "10:00 AM", "SUB-04", "Discrete Mathematics", "Dr. Alan Turing", "Room 305", "CSE-4A"),
        TimetableEntity("TT-02", "Monday", "10:00 AM", "11:00 AM", "SUB-01", "Computer Networks", "Prof. Robert Chen", "Lab 204", "CSE-4A"),
        TimetableEntity("TT-03", "Monday", "11:30 AM", "12:30 PM", "SUB-02", "Operating Systems", "Dr. Sarah Jenkins", "Hall B-12", "CSE-4A"),
        TimetableEntity("TT-04", "Monday", "02:00 PM", "03:00 PM", "SUB-03", "Database Management", "Prof. David Miller", "Lab 102", "CSE-4A"),

        TimetableEntity("TT-05", "Tuesday", "09:00 AM", "10:00 AM", "SUB-05", "Software Engineering", "Prof. Robert Chen", "Room 208", "CSE-4A"),
        TimetableEntity("TT-06", "Tuesday", "10:00 AM", "11:00 AM", "SUB-01", "Computer Networks", "Prof. Robert Chen", "Lab 204", "CSE-4A"),
        TimetableEntity("TT-07", "Tuesday", "01:00 PM", "03:00 PM", "SUB-03", "DBMS Practical Lab", "Prof. David Miller", "Lab 102", "CSE-4A"),

        TimetableEntity("TT-08", "Wednesday", "09:00 AM", "10:00 AM", "SUB-02", "Operating Systems", "Dr. Sarah Jenkins", "Hall B-12", "CSE-4A"),
        TimetableEntity("TT-09", "Wednesday", "10:00 AM", "11:00 AM", "SUB-04", "Discrete Mathematics", "Dr. Alan Turing", "Room 305", "CSE-4A"),
        TimetableEntity("TT-10", "Wednesday", "11:30 AM", "12:30 PM", "SUB-05", "Software Engineering", "Prof. Robert Chen", "Room 208", "CSE-4A"),

        TimetableEntity("TT-11", "Thursday", "10:00 AM", "11:00 AM", "SUB-01", "Computer Networks", "Prof. Robert Chen", "Lab 204", "CSE-4A"),
        TimetableEntity("TT-12", "Thursday", "11:30 AM", "12:30 PM", "SUB-03", "Database Management", "Prof. David Miller", "Lab 102", "CSE-4A"),
        TimetableEntity("TT-13", "Thursday", "02:00 PM", "04:00 PM", "SUB-01", "Networks Simulation Lab", "Prof. Robert Chen", "Lab 204", "CSE-4A"),

        TimetableEntity("TT-14", "Friday", "09:00 AM", "10:00 AM", "SUB-05", "Software Engineering", "Prof. Robert Chen", "Room 208", "CSE-4A"),
        TimetableEntity("TT-15", "Friday", "10:00 AM", "11:00 AM", "SUB-02", "Operating Systems", "Dr. Sarah Jenkins", "Hall B-12", "CSE-4A"),
        TimetableEntity("TT-16", "Friday", "11:30 AM", "12:30 PM", "SUB-04", "Math Tutorial & Problem Solving", "Dr. Alan Turing", "Room 305", "CSE-4A")
      )
      dao.insertTimetables(timetables)

      // 5. Active attendance sessions start empty - created only when faculty initiates a live session

      // 6. Attendance records start empty - created strictly when students scan live faculty QR codes

      // 7. Notifications
      val notifications = listOf(
        NotificationEntity(
          id = "NOTIF-WELCOME-01",
          userId = "ALL",
          title = "Welcome to AttendX 🎓",
          message = "Smart Attendance System is active. Scan the live QR code in class to record your attendance.",
          type = "ANNOUNCEMENT",
          timestamp = System.currentTimeMillis() - 60 * 60 * 1000,
          isRead = false
        ),
        NotificationEntity(
          id = "NOTIF-03",
          userId = "ALL",
          title = "College Announcement 📢",
          message = "Mid-term examination schedule for 4th Semester has been published on the student noticeboard.",
          type = "ANNOUNCEMENT",
          timestamp = System.currentTimeMillis() - 24 * 3600 * 1000,
          isRead = true
        )
      )
      for (notif in notifications) {
        dao.insertNotification(notif)
      }

      // 8. Audit Logs
      val auditLogs = listOf(
        AuditLogEntity(
          id = "LOG-01",
          actorName = "Prof. Robert Chen",
          actorRole = "FACULTY",
          action = "Session Created",
          details = "Started attendance session SES-2026-9821 for Computer Networks (CSE-4A)",
          timestamp = System.currentTimeMillis() - 40 * 60 * 1000
        ),
        AuditLogEntity(
          id = "LOG-02",
          actorName = "Alex Sharma",
          actorRole = "STUDENT",
          action = "QR Attendance Verified",
          details = "Student CSE24-001 verified attendance via QR code for session SES-2026-9821",
          timestamp = System.currentTimeMillis() - 15 * 60 * 1000
        ),
        AuditLogEntity(
          id = "LOG-03",
          actorName = "Dr. Eleanor Vance",
          actorRole = "ADMIN",
          action = "Timetable Updated",
          details = "Modified Friday schedule for Room 305 Math Tutorial session",
          timestamp = System.currentTimeMillis() - 3 * 3600 * 1000
        ),
        AuditLogEntity(
          id = "LOG-04",
          actorName = "Dr. Eleanor Vance",
          actorRole = "ADMIN",
          action = "User Account Activated",
          details = "Activated student profile CSE24-008 (Sophia Martinez)",
          timestamp = System.currentTimeMillis() - 24 * 3600 * 1000
        )
      )
      for (log in auditLogs) {
        dao.insertAuditLog(log)
      }

      // 9. Initial Student Leave Requests
      val sampleLeaves = listOf(
        LeaveRequestEntity(
          id = "LV-101",
          studentId = "U-STD-001",
          studentName = "PSR",
          studentRollNo = "CSE24-001",
          studentRegNo = "REG-2024-2027",
          studentSection = "CSE-4A",
          facultyId = "U-FAC-001",
          facultyName = "Prof. Robert Chen",
          leaveType = "SICK_LEAVE",
          startDate = "2026-09-24",
          endDate = "2026-09-25",
          numberOfDays = 2,
          reason = "High viral fever and severe headache. Doctor advised 2 days of bed rest.",
          status = "PENDING",
          appliedAt = System.currentTimeMillis() - 2 * 3600 * 1000
        ),
        LeaveRequestEntity(
          id = "LV-102",
          studentId = "U-STD-002",
          studentName = "Priya Patel",
          studentRollNo = "CSE24-002",
          studentRegNo = "REG-2024-8892",
          studentSection = "CSE-4A",
          facultyId = "U-FAC-001",
          facultyName = "Prof. Robert Chen",
          leaveType = "DUTY_LEAVE",
          startDate = "2026-09-22",
          endDate = "2026-09-22",
          numberOfDays = 1,
          reason = "Representing our university in the State Inter-College Hackathon finals.",
          status = "APPROVED",
          facultyRemarks = "Approved. Great initiative. Please submit event certificate upon return.",
          appliedAt = System.currentTimeMillis() - 24 * 3600 * 1000,
          reviewedAt = System.currentTimeMillis() - 18 * 3600 * 1000
        ),
        LeaveRequestEntity(
          id = "LV-103",
          studentId = "U-STD-003",
          studentName = "Marcus Johnson",
          studentRollNo = "CSE24-003",
          studentRegNo = "REG-2024-8893",
          studentSection = "CSE-4A",
          facultyId = "U-FAC-002",
          facultyName = "Dr. Sarah Jenkins",
          leaveType = "CASUAL_LEAVE",
          startDate = "2026-09-28",
          endDate = "2026-09-29",
          numberOfDays = 2,
          reason = "Attending family function / elder sister's wedding in home town.",
          status = "PENDING",
          appliedAt = System.currentTimeMillis() - 5 * 3600 * 1000
        )
      )
      for (leave in sampleLeaves) {
        dao.insertLeaveRequest(leave)
      }

      // 10. Official Campus Notices & Circulars
      val sampleNotices = listOf(
        NoticeEntity(
          id = "NTC-101",
          title = "Mid-Term Examination Schedule - Fall 2026",
          content = "All B.Tech students are hereby informed that Mid-Term examinations commence from October 12, 2026. Hall tickets will be released exclusively to students having 75% or above aggregate attendance. Please verify your subject attendance with respective course faculty before October 05.",
          postedBy = "Dr. Eleanor Vance",
          authorRole = "ADMIN",
          authorId = "U-ADM-001",
          targetAudience = "ALL",
          category = "EXAM",
          priority = "URGENT",
          isPinned = true,
          postedAt = System.currentTimeMillis() - 10 * 3600 * 1000
        ),
        NoticeEntity(
          id = "NTC-102",
          title = "DBMS & Computer Networks Lab Evaluation Deadline",
          content = "CSE-4A and CSE-4B students must complete and get their Lab Cycle 2 code records signed by Friday 4:00 PM. External viva will be scheduled following the submission deadline.",
          postedBy = "Prof. Robert Chen",
          authorRole = "FACULTY",
          authorId = "U-FAC-001",
          targetAudience = "CSE-4A",
          category = "ACADEMIC",
          priority = "HIGH",
          isPinned = true,
          postedAt = System.currentTimeMillis() - 20 * 3600 * 1000
        ),
        NoticeEntity(
          id = "NTC-103",
          title = "Upcoming University Campus Holiday Notice",
          content = "The University campus, academic blocks, central library, and cafeterias will remain closed on scheduled statutory holidays. Online portal for assignment submission will remain active.",
          postedBy = "Dean Academic Affairs",
          authorRole = "ADMIN",
          authorId = "U-ADM-001",
          targetAudience = "ALL",
          category = "HOLIDAY",
          priority = "NORMAL",
          isPinned = false,
          postedAt = System.currentTimeMillis() - 48 * 3600 * 1000
        ),
        NoticeEntity(
          id = "NTC-104",
          title = "Annual Inter-College Hackathon & TechFest 2026",
          content = "Registrations are officially open for InnovateX 2026 36-hour hackathon. Shortlisted projects will receive university incubation grants and direct industry mentorship. Register via the campus portal by Oct 15.",
          postedBy = "Prof. Michael Davis",
          authorRole = "FACULTY",
          authorId = "U-FAC-003",
          targetAudience = "ALL",
          category = "EVENT",
          priority = "NORMAL",
          isPinned = false,
          postedAt = System.currentTimeMillis() - 72 * 3600 * 1000
        )
      )
      for (notice in sampleNotices) {
        dao.insertNotice(notice)
      }
    }
  }
}
