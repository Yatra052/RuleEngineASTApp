package com.example.ruleengineast.Database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RuleDao {
    @Insert
    fun insertRule(rule: RuleEntity)

    @Query("SELECT * FROM rules WHERE id = :ruleId")
    fun getRuleById(ruleId: Int): RuleEntity
}
