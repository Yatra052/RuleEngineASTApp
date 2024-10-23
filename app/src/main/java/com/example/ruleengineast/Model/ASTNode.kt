package com.example.ruleengineast.Model


sealed class ASTNode {
    data class OperandNode(val value: String) : ASTNode() {
        override fun toString(): String {
            return "Operand($value)"
        }
    }

    data class OperatorNode(val type: String, val left: ASTNode, val right: ASTNode) : ASTNode() {
        override fun toString(): String {
            return "Operator($type, Left: [$left], Right: [$right])"
        }
    }
}
