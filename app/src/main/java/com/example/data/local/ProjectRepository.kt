package com.example.data.local

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val dao: ProjectDao) {
    val allProjects: Flow<List<ProjectEntity>> = dao.getAllProjects()

    suspend fun getProjectById(id: Long): ProjectEntity? = dao.getProjectById(id)

    fun getProjectsByTool(toolType: String): Flow<List<ProjectEntity>> = dao.getProjectsByTool(toolType)

    suspend fun insertProject(project: ProjectEntity): Long = dao.insertProject(project)

    suspend fun updateProject(project: ProjectEntity) = dao.updateProject(project)

    suspend fun deleteProject(id: Long) = dao.deleteProjectById(id)
}
