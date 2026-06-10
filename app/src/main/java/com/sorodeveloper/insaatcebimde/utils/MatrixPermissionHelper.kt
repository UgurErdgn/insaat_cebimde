package com.sorodeveloper.insaatcebimde.utils

import com.sorodeveloper.insaatcebimde.model.JobNode
import com.sorodeveloper.insaatcebimde.model.ProjectNode
import com.sorodeveloper.insaatcebimde.model.User
import com.sorodeveloper.insaatcebimde.model.MatrixPermission

object MatrixPermissionHelper {

    /**
     * Checks if a user has access to a specific location and (optionally) a specific job.
     */
    fun hasAccess(
        user: User,
        projectId: String,
        locationPath: String,
        jobPath: String? = null
    ): Boolean {
        // Fetch permissions specific to the project
        val projectPerms = user.projectPermissions[projectId] ?: return false

        for (perm in projectPerms) {
            // Check if user's granted location covers the requested location
            // e.g. granted "Etap1", requested "Etap1/Blok1" -> allowed
            val hasLocationAccess = isPathCovered(perm.locationPath, locationPath)

            if (hasLocationAccess) {
                // If there is no specific job requested, location access is enough (e.g. just viewing the block)
                if (jobPath == null || jobPath.isEmpty()) {
                    return true
                }

                // If a specific job is requested, check if the user's job permission covers it
                // e.g. granted "Mobilya", requested "Mobilya/Kapi" -> allowed
                // If perm.jobPath is empty, it means they have access to all jobs in that location
                val hasJobAccess = perm.jobPath.isEmpty() || isPathCovered(perm.jobPath, jobPath)
                if (hasJobAccess) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Checks if a user is allowed to delegate a specific permission to someone else.
     * The requested permission MUST be a subset of the delegator's permission.
     */
    fun canDelegate(
        delegator: User,
        projectId: String,
        requestedLocationPath: String,
        requestedJobPath: String
    ): Boolean {
        val projectPerms = delegator.projectPermissions[projectId] ?: return false

        for (perm in projectPerms) {
            if (perm.canDelegate) {
                val coversLocation = isPathCovered(perm.locationPath, requestedLocationPath)
                // Empty jobPath in perm means delegator has access to ALL jobs.
                val coversJob = perm.jobPath.isEmpty() || isPathCovered(perm.jobPath, requestedJobPath)

                if (coversLocation && coversJob) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Checks if `grantedPath` is a valid prefix (ancestor) of, or equal to, `requestedPath`.
     * e.g. granted: "A", requested: "A/B/C" -> true
     * e.g. granted: "A/B", requested: "A" -> false (cannot look upwards)
     */
    private fun isPathCovered(grantedPath: String, requestedPath: String): Boolean {
        if (grantedPath.isEmpty()) return true // Empty granted path means root/all access in this context
        if (requestedPath == grantedPath) return true
        return requestedPath.startsWith("$grantedPath/")
    }
}
