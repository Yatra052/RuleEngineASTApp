package com.example.ruleengineast.Activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.ruleengineast.Database.AppDatabase
import com.example.ruleengineast.Database.RuleDao
import com.example.ruleengineast.Database.RuleEntity
import com.example.ruleengineast.Model.ASTNode
import com.example.ruleengineast.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var database: AppDatabase
    private lateinit var ruleDao: RuleDao
    private val ruleList = mutableListOf<ASTNode>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "rules_db").build()
        ruleDao = database.ruleDao()

        val ruleInput = findViewById<EditText>(R.id.ruleInput)
        val createRuleBtn = findViewById<Button>(R.id.createRuleButton)
        val dataInput = findViewById<EditText>(R.id.dataInput)
        val evaluateRuleBtn = findViewById<Button>(R.id.evaluateButton)
        val resultTextView = findViewById<TextView>(R.id.resultText)
        val combineRulesBtn = findViewById<Button>(R.id.combineRulesButton)

        createRuleBtn.setOnClickListener {
            val ruleString = ruleInput.text.toString()
            val ruleAST = create_rule(ruleString)
            if (ruleAST != null) {
                ruleList.add(ruleAST)
                val ruleEntity = RuleEntity(ruleString = ruleString)

                // Use coroutine to insert rule into the database
                CoroutineScope(Dispatchers.IO).launch {
                    ruleDao.insertRule(ruleEntity)
                    withContext(Dispatchers.Main) {
                        resultTextView.text = "Rule created successfully! AST: $ruleAST"
                    }
                }
                Log.d("MainActivity", "Created rule: $ruleAST")
            } else {
                resultTextView.text = "Invalid rule format!"
            }
        }

        combineRulesBtn.setOnClickListener {
            val combinedAST = combine_rules(ruleList)
            if (combinedAST != null) {
                resultTextView.append("\nCombined AST: $combinedAST")
                Log.d("MainActivity", "Combined AST: $combinedAST")
            } else {
                resultTextView.text = "No rules to combine."
            }
        }

        evaluateRuleBtn.setOnClickListener {
            val userData = parseUserData(dataInput.text.toString())
            val combinedAST = combine_rules(ruleList) // Combine before evaluation
            val result = combinedAST?.let { evaluate_rule(it, userData) } ?: false
            resultTextView.append("\nEvaluation Result: $result")
            Log.d("MainActivity", "Evaluation Result: $result")
        }
    }

    private fun parseUserData(dataString: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        dataString.split(",").forEach {
            val (key, value) = it.split(":").map { it.trim() }
            map[key] = value.toIntOrNull() ?: value // Handle both Int and String values
        }
        return map
    }

    private fun create_rule(rule: String): ASTNode? {
        val tokens = rule.split(" ")
        return if (tokens.contains("AND") || tokens.contains("OR")) {
            val operator = if (tokens.contains("AND")) "AND" else "OR"
            val operatorIndex = tokens.indexOf(operator)

            val leftOperand = tokens.subList(0, operatorIndex).joinToString(" ")
            val rightOperand = tokens.subList(operatorIndex + 1, tokens.size).joinToString(" ")

            ASTNode.OperatorNode(
                type = operator,
                left = ASTNode.OperandNode(leftOperand),
                right = ASTNode.OperandNode(rightOperand)
            )
        } else {
            ASTNode.OperandNode(rule)
        }
    }

    private fun combine_rules(rules: List<ASTNode>): ASTNode? {
        if (rules.isEmpty()) return null

        // Count occurrences of operators
        val operatorCount = mutableMapOf<String, Int>()
        rules.forEach {
            if (it is ASTNode.OperatorNode) {
                operatorCount[it.type] = operatorCount.getOrDefault(it.type, 0) + 1
            }
        }

        // Determine the most frequent operator
        val mostFrequentOperator = operatorCount.maxByOrNull { it.value }?.key ?: "OR"

        var combinedAST = rules[0]
        for (i in 1 until rules.size) {
            combinedAST = ASTNode.OperatorNode(mostFrequentOperator, combinedAST, rules[i])
        }
        return combinedAST
    }

    private fun evaluate_rule(ast: ASTNode, data: Map<String, Any>): Boolean {
        return when (ast) {
            is ASTNode.OperandNode -> evaluate_condition(ast.value, data)
            is ASTNode.OperatorNode -> {
                val leftResult = evaluate_rule(ast.left, data)
                val rightResult = evaluate_rule(ast.right, data)
                when (ast.type) {
                    "AND" -> leftResult && rightResult
                    "OR" -> leftResult || rightResult
                    else -> throw IllegalArgumentException("Invalid operator: ${ast.type}")
                }
            }
        }
    }

    private fun evaluate_condition(condition: String, data: Map<String, Any>): Boolean {
        val parts = condition.split(" ")
        if (parts.size < 3) throw IllegalArgumentException("Invalid condition format")

        val attribute = parts[0]
        val operator = parts[1]
        val value = parts[2].replace("'", "")

        val dataValue = data[attribute]

        return when (operator) {
            ">" -> (dataValue as? Int ?: 0) > value.toInt()
            "<" -> (dataValue as? Int ?: 0) < value.toInt()
            "=" -> dataValue.toString() == value
            else -> throw IllegalArgumentException("Invalid operator in condition")
        }
    }
}
