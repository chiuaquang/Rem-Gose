package com.remgose.rem.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** So do rut gon tu RenDroid: 2 bang games + runtimes la du cho V1. */
@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val name: String,              // ten thu muc, duy nhat
    val displayName: String?,
    val creationTimestamp: Long,
    val deletionTimestamp: Long? = null,       // soft-delete, Worker don sau
    val sourceUri: String,                     // uri file/thu muc nguoi dung chon
    val path: String?,                         // duong dan sau khi nhap xong
    val renPyVersion: String?,
    val gameVersion: String? = null,
    val icon: String? = null,
    val runtimeVersionOverride: String? = null, // nguoi dung chon tay khi auto-adapt sai
    val statusPhase: String = "QUEUED",        // QUEUED/IMPORTING/READY/ERROR/RUNNING
    val statusMessage: String? = null,
)

@Entity(tableName = "runtimes")
data class RuntimeEntity(
    @PrimaryKey val version: String,
    val creationTimestamp: Long,
    val deletionTimestamp: Long? = null,
    val statusPhase: String = "NOT_INSTALLED", // NOT_INSTALLED/INSTALLING/READY/ERROR
    val statusMessage: String? = null,
)
