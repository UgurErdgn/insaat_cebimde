package com.sorodeveloper.insaatcebimde.domain.repository

import com.sorodeveloper.insaatcebimde.domain.model.Project
import com.sorodeveloper.insaatcebimde.domain.model.ProjectNode
import kotlinx.coroutines.flow.Flow
import com.sorodeveloper.insaatcebimde.domain.model.JobTemplate
import com.sorodeveloper.insaatcebimde.domain.model.NodeJob
import com.sorodeveloper.insaatcebimde.domain.model.PropertyTemplate

import com.sorodeveloper.insaatcebimde.domain.model.MemberInfo
import com.sorodeveloper.insaatcebimde.domain.model.MemberScopes

interface ProjectRepository {
    // Yeni saha oluştur
    suspend fun createProject(project: Project): Result<Unit>
    
    // Belirli bir projeyi ID ile getir
    suspend fun getProjectById(projectId: String): Result<Project>
    
    // Projeyi güncelle
    suspend fun updateProject(project: Project): Result<Unit>
    
    // Kullanıcının yetkili olduğu sahaları getir (Tek seferlik okuma)
    suspend fun getUserProjects(): Result<List<Project>>
    
    // Kullanıcının yetkili olduğu sahaları dinle (Flow - Offline First)
    fun observeUserProjects(): Flow<List<Project>>
    
    // Hiyerarşi (Düğümler) İşlemleri
    suspend fun createProjectNode(node: ProjectNode): Result<Unit>
    suspend fun updateProjectNode(node: ProjectNode): Result<Unit>
    suspend fun getProjectNodes(projectId: String, parentId: String? = null): Result<List<ProjectNode>>
    suspend fun getDeletedNodesWithDetails(projectId: String): Result<List<com.sorodeveloper.insaatcebimde.domain.model.DeletedNodeDetail>>
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

    // Tüm düğümleri ve kategorileri getir (Scope seçimi için)
    suspend fun getAllProjectNodes(projectId: String): Result<List<ProjectNode>>
    suspend fun getAllJobCategories(projectId: String): Result<List<String>>

    // ---- NodeJob İşlemleri ----
    suspend fun getNodeJobs(projectId: String, nodeId: String): Result<List<NodeJob>>
    fun observeNodeJobs(projectId: String, nodeId: String): Flow<List<NodeJob>>
    suspend fun updateNodeJobProgress(projectId: String, nodeId: String, jobId: String, progress: Int): Result<Unit>

    // ---- Üye Yönetimi (Members) ----
    /** Projenin üye listesini getir (Cache-First, proje dökümanından okunur — ekstra Read yok) */
    suspend fun getProjectMembers(projectId: String): Result<List<MemberInfo>>

    /** Projenin üye listesini canlı dinle (Snapshot Listener) */
    fun observeProjectMembers(projectId: String): Flow<List<MemberInfo>>

    /** Üyenin yetkilerini güncelleme isteği at (Cloud Function doğrulayacak) */
    suspend fun requestMemberUpdate(
        projectId: String,
        targetUserId: String,
        newPermissions: List<String>,
        newDelegablePermissions: List<String>,
        newScopes: MemberScopes,
        newRoleName: String
    ): Result<Unit>

    /** Üyeyi projeden çıkar (Cloud Function subset kontrolü yapacak) */
    suspend fun removeMember(projectId: String, targetUserId: String): Result<Unit>
}
