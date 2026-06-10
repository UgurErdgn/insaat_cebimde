package com.sorodeveloper.insaatcebimde.model

enum class ApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class ProjectApplication(
    val applicationId: String = "",
    val projectId: String = "",
    val projectName: String = "", // Helpful for showing to the user which project they applied to
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    var status: ApplicationStatus = ApplicationStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis()
)
