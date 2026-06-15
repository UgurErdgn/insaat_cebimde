package com.sorodeveloper.insaatcebimde.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.sorodeveloper.insaatcebimde.domain.model.JobTemplate
import com.sorodeveloper.insaatcebimde.domain.model.NodeJob
import com.sorodeveloper.insaatcebimde.domain.model.PropertyTemplate
import com.sorodeveloper.insaatcebimde.domain.model.Project
import com.sorodeveloper.insaatcebimde.domain.model.ProjectNode
import com.sorodeveloper.insaatcebimde.domain.repository.ProjectRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import java.util.UUID

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import com.google.firebase.functions.FirebaseFunctions

class ProjectRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) : ProjectRepository {

    override suspend fun createProject(project: Project): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("Kullanıcı girişi bulunamadı!")
            val projectRef = firestore.collection("projects").document()
            
            val newProject = project.copy(
                id = projectRef.id,
                ownerId = currentUser.uid
            )

            // Projeyi kaydet
            projectRef.set(newProject).await()

            // Saha Düğümünü (Root Node) Otomatik Oluştur
            val sahaNodeRef = firestore.collection("projects").document(projectRef.id)
                .collection("nodes").document()
            
            val sahaNode = ProjectNode(
                id = sahaNodeRef.id,
                projectId = projectRef.id,
                parentId = null,
                ancestors = listOf(sahaNodeRef.id),
                name = "İnşaat Sahası",
                type = "Saha"
            )
            sahaNodeRef.set(sahaNode).await()

            // Oluşturan kullanıcıya bu projenin yetkilerini "OWNER" olarak ata
            val userRef = firestore.collection("users").document(currentUser.uid)
            val permissionsUpdate = mapOf(
                "projectPermissions.${projectRef.id}" to mapOf(
                    "role" to "OWNER",
                    "canDelegate" to true
                )
            )
            userRef.update(permissionsUpdate).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProjectById(projectId: String): Result<Project> {
        return try {
            val projectSnapshot = firestore.collection("projects").document(projectId).get().await()
            val project = projectSnapshot.toObject(Project::class.java)
            if (project != null) {
                Result.success(project)
            } else {
                Result.failure(Exception("Proje bulunamadı!"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProject(project: Project): Result<Unit> {
        return try {
            firestore.collection("projects").document(project.id).set(project).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserProjects(): Result<List<Project>> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("Kullanıcı girişi bulunamadı!")
            
            // Kullanıcının profil belgesini al
            val userSnapshot = firestore.collection("users").document(currentUser.uid).get().await()
            val permissions = userSnapshot.get("projectPermissions") as? Map<String, Any> ?: emptyMap()
            
            val projectIds = permissions.keys.toList()
            if (projectIds.isEmpty()) return Result.success(emptyList())

            // Proje detaylarını paralel şekilde çek
            // Firestore 'in' sorgusu en fazla 10 eleman aldığı için mapNotNull ile tek tek çekmek daha güvenlidir.
            val projects = projectIds.mapNotNull { projectId ->
                val projectSnapshot = firestore.collection("projects").document(projectId).get().await()
                projectSnapshot.toObject(Project::class.java)
            }
            
            Result.success(projects.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- Hiyerarşi (Düğümler) İşlemleri ----

    override suspend fun createProjectNode(node: ProjectNode): Result<Unit> {
        return try {
            val nodeRef = firestore.collection("projects").document(node.projectId)
                .collection("nodes").document()
            
            val newNode = node.copy(
                id = nodeRef.id,
                ancestors = node.ancestors + nodeRef.id // Kendi ID'sini de atalar dizisine ekle
            )
            nodeRef.set(newNode).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProjectNode(node: ProjectNode): Result<Unit> {
        return try {
            firestore.collection("projects").document(node.projectId)
                .collection("nodes").document(node.id)
                .set(node).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProjectNodes(projectId: String, parentId: String?): Result<List<ProjectNode>> {
        return try {
            val query = if (parentId == null) {
                firestore.collection("projects").document(projectId)
                    .collection("nodes").whereEqualTo("parentId", null)
            } else {
                firestore.collection("projects").document(projectId)
                    .collection("nodes").whereEqualTo("parentId", parentId)
            }
            val snapshot = query.get().await()
            val nodes = snapshot.documents.mapNotNull { doc -> 
                val node = doc.toObject(ProjectNode::class.java)
                val isDeleted = doc.getBoolean("isDeleted") ?: false
                if (isDeleted) null else node
            }
            Result.success(nodes.sortedBy { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeProjectNodes(projectId: String, parentId: String?): Flow<List<ProjectNode>> = callbackFlow {
        val query = if (parentId == null) {
            firestore.collection("projects").document(projectId)
                .collection("nodes").whereEqualTo("parentId", null)
        } else {
            firestore.collection("projects").document(projectId)
                .collection("nodes").whereEqualTo("parentId", parentId)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                val nodes = snapshot.documents.mapNotNull { 
                    val node = it.toObject(ProjectNode::class.java)
                    if (node?.isDeleted == true) null else node
                }
                trySend(nodes.sortedBy { it.createdAt })
            }
        }

        awaitClose { listener.remove() }
    }

    override fun observeProjectNode(projectId: String, nodeId: String): Flow<ProjectNode?> = callbackFlow {
        val docRef = firestore.collection("projects").document(projectId)
            .collection("nodes").document(nodeId)
            
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val node = snapshot.toObject(ProjectNode::class.java)
                trySend(node)
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun toggleNodeDelete(projectId: String, nodeId: String, isDeleted: Boolean): Result<Unit> {
        return try {
            val functions = FirebaseFunctions.getInstance("europe-west3")
            val data = hashMapOf(
                "projectId" to projectId,
                "nodeId" to nodeId,
                "isDeleted" to isDeleted
            )
            functions.getHttpsCallable("toggleNodeDelete").call(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createJobTemplate(template: JobTemplate): Result<Unit> {
        return try {
            val templateRef = firestore.collection("projects").document(template.projectId)
                .collection("jobTemplates").document()
            
            val newTemplate = template.copy(id = templateRef.id)
            templateRef.set(newTemplate).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateJobTemplate(template: JobTemplate): Result<Unit> {
        return try {
            firestore.collection("projects").document(template.projectId)
                .collection("jobTemplates").document(template.id)
                .set(template).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getJobTemplates(projectId: String): Result<List<JobTemplate>> {
        return try {
            val snapshot = firestore.collection("projects").document(projectId)
                .collection("jobTemplates")
                .get().await()
                
            val templates = snapshot.documents.mapNotNull { it.toObject(JobTemplate::class.java) }
            Result.success(templates.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadImages(uris: List<String>, projectId: String): Result<List<String>> {
        return try {
            val downloadUrls = mutableListOf<String>()
            val storageRef = storage.reference.child("projects").child(projectId).child("templates")
            
            for (uriString in uris) {
                val uri = android.net.Uri.parse(uriString)
                val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}"
                val fileRef = storageRef.child(fileName)
                
                // Resmi yükle
                fileRef.putFile(uri).await()
                
                // İndirme (Download) URL'sini al
                val downloadUrl = fileRef.downloadUrl.await().toString()
                downloadUrls.add(downloadUrl)
            }
            Result.success(downloadUrls)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- Mülk Şablonları ----

    override suspend fun createPropertyTemplate(template: PropertyTemplate): Result<Unit> {
        return try {
            val ref = firestore.collection("projects").document(template.projectId)
                .collection("propertyTemplates").document()
            val newTemplate = template.copy(id = ref.id)
            ref.set(newTemplate).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePropertyTemplate(template: PropertyTemplate): Result<Unit> {
        return try {
            firestore.collection("projects").document(template.projectId)
                .collection("propertyTemplates").document(template.id)
                .set(template).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPropertyTemplates(projectId: String): Result<List<PropertyTemplate>> {
        return try {
            val snapshot = firestore.collection("projects").document(projectId)
                .collection("propertyTemplates")
                .get().await()
            val templates = snapshot.documents.mapNotNull { doc -> 
                val template = doc.toObject(PropertyTemplate::class.java)
                val isDeleted = doc.getBoolean("isDeleted") ?: false
                if (isDeleted) null else template
            }
            Result.success(templates.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- NodeJob İşlemleri ----

    override suspend fun getNodeJobs(projectId: String, nodeId: String): Result<List<NodeJob>> {
        return try {
            val snapshot = firestore.collection("projects").document(projectId)
                .collection("nodes").document(nodeId)
                .collection("jobs")
                .get().await()
            val jobs = snapshot.documents.mapNotNull { 
                val job = it.toObject(NodeJob::class.java)
                if (job?.isDeleted == true) null else job
            }
            Result.success(jobs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeNodeJobs(projectId: String, nodeId: String): Flow<List<NodeJob>> = callbackFlow {
        val query = firestore.collection("projects").document(projectId)
            .collection("nodes").document(nodeId)
            .collection("jobs")
            
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                val jobs = snapshot.documents.mapNotNull { doc ->
                    val job = doc.toObject(NodeJob::class.java)
                    val isDeleted = doc.getBoolean("isDeleted") ?: false
                    if (isDeleted) null else job
                }
                trySend(jobs)
            }
        }
        
        awaitClose { listener.remove() }
    }

    override suspend fun updateNodeJobProgress(
        projectId: String,
        nodeId: String,
        jobId: String,
        progress: Int
    ): Result<Unit> {
        return try {
            firestore.collection("projects").document(projectId)
                .collection("nodes").document(nodeId)
                .collection("jobs").document(jobId)
                .update("progress", progress).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
