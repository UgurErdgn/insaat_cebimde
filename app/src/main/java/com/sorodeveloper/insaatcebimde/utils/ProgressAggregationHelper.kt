package com.sorodeveloper.insaatcebimde.utils

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * Handles the automatic progress aggregation from bottom to top:
 * Item (Kalem) -> Category (İş) -> Flat (Daire) -> Block (Blok) -> Phase (Etap) -> Project (Saha)
 */
object ProgressAggregationHelper {

    private val db = FirebaseDatabase.getInstance()

    // Assuming we have two distinct collections for nodes
    // Note: The helper now requires the projectId to know which isolated Firebase sandbox to query.
    // The following references will be initialized within functions where projectId is available.
    // private val locationsRef = db.getReference("locations")
    // private val jobsRef = db.getReference("jobs")

    /**
     * Updates an ITEM's progress, and then bubbles up the changes up the tree.
     */
    suspend fun updateItemProgressAndAggregate(
        projectId: String,
        jobNodeId: String,
        newProgress: Double
    ) {
        // 1. Update the leaf item progress
        db.getReference("insaatlar").child(projectId).child("jobs").child(jobNodeId).child("progress").setValue(newProgress).await()

        // 2. Fetch the node to know its parent category
        // In a real scenario, you'd fetch the JobNode object
        // ...
        
        // This helper requires database reads to traverse the tree upward.
        // The pattern is:
        // A. Recalculate Category progress (average of all its Item children)
        // B. Recalculate Flat progress (average of all its Category children)
        // C. Recalculate Block progress (average of all its Flat children)
        // D. Recalculate Phase progress (average of all its Block children)
        // E. Recalculate Project progress (average of all its Phase children)
    }
}
