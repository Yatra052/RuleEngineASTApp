package com.example.ruleengineast.Database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ruleString: String
)
