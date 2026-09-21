package com.expensevault.core.model

import kotlinx.datetime.Instant

/**
 * Domain model representing a person.
 */
data class Person(
    val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val avatarPath: String? = null,
    val createdAt: Instant
)
