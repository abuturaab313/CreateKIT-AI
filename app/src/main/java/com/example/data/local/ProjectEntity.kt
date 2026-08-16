package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val toolType: String,
    val previewPath: String,
    val outputPath: String,
    val fileType: String,
    val fileSize: Long,
    val width: Int = 0,
    val height: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)
