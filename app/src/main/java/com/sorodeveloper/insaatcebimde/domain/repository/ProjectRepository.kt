package com.sorodeveloper.insaatcebimde.domain.repository

import com.sorodeveloper.insaatcebimde.domain.model.Project
import com.sorodeveloper.insaatcebimde.domain.model.ProjectNode
import kotlinx.coroutines.flow.Flow
import com.sorodeveloper.insaatcebimde.domain.model.JobTemplate
import com.sorodeveloper.insaatcebimde.domain.model.NodeJob
import com.sorodeveloper.insaatcebimde.domain.model.PropertyTemplate

interface ProjectRepository {
    // Yeni saha oluştur
    suspend fun createProject(project: Project): Result<Unit>
    
    // Belirli bir projeyi ID ile getir
    suspend fun getProjectById(projectId: String): Result<Project>
    
    // Projeyi güncelle
    suspend fun updateProject(project: Project): Result<Unit>
    
    // Kullanıcının yetkili olduğu sahaları getir
    suspend fun getUserProjects(): Result<List<Project>>
    
    // Hiyerarşi (Düğümler) İşlemleri
    suspend fun createProjectNode(node: ProjectNode): Result<Unit>
    suspend fun updateProjectNode(node: ProjectNode): Result<Unit>
    suspend fun getProjectNodes(projectId: String, parentId: String? = null): Result<List<ProjectNode>>
    fun observeProjectNodes(projectId: String, parentId: String? = null): kotlinx.coroutines.flow.Flow<List<ProjectNode>>
    fun observeProjectNode(projectId: String, nodeId: String): kotlinx.coroutines.flow.Flow<ProjectNode?>
    suspend fun toggleNodeDelete(projectId: String, nodeId: String, isDeleted: Boolean): Result<Unit>

    // Şablon İşlemleri
    suspend fun createJobTemplate(template: JobTemplate): Result<Unit>
    suspend fun updateJobTemplate(template: JobTemplate): Result<Unit>
    suspend fun getJobTemplates(projectId: String): Result<List<JobTemplate>>
    suspend fun uploadImages(uris: List<String>, projectId: String): Result<List<String>>

    // Mülk Şablonu İşlemleri
    suspend fun createPropertyTemplate(template: PropertyTemplate): Result<Unit>
    suspend fun updatePropertyTemplate(template: PropertyTemplate): Result<Unit>
    suspend fun getPropertyTemplates(projectId: String): Result<List<PropertyTemplate>>

    // ---- NodeJob İşlemleri ----
    suspend fun getNodeJobs(projectId: String, nodeId: String): Result<List<NodeJob>>
    fun observeNodeJobs(projectId: String, nodeId: String): Flow<List<NodeJob>>
    suspend fun updateNodeJobProgress(projectId: String, nodeId: String, jobId: String, progress: Int): Result<Unit>
}
