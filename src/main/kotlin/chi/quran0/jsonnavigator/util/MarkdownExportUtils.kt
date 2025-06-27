package chi.quran0.jsonnavigator.util

import chi.quran0.jsonnavigator.tree.JsonTreeNodeData
import com.intellij.json.psi.*
import javax.swing.tree.DefaultMutableTreeNode

object MarkdownExportUtils {
    fun treeToMarkdown(root: DefaultMutableTreeNode): String {
        val sb = StringBuilder()
        sb.append("| Field | Type | Description | Example |\n")
        sb.append("| ----- | ---- | ----------- | ------- |\n")
        fun walk(node: DefaultMutableTreeNode, depth: Int) {
            val data = node.userObject as? JsonTreeNodeData ?: return
            val psi = data.psi
            val (field, typeExample) = when (psi) {
                is JsonProperty -> {
                    val key = psi.name
                    val value = psi.value
                    when (value) {
                        is JsonObject -> Pair(key, "object" to "{...}")
                        is JsonArray -> Pair("$key[]", "array" to value.text)
                        is JsonStringLiteral -> Pair(key, "string" to value.value)
                        is JsonNumberLiteral -> Pair(key, "number" to value.text)
                        is JsonBooleanLiteral -> Pair(key, "boolean" to value.text)
                        is JsonNullLiteral -> Pair(key, "null" to "null")
                        else -> Pair(key, "unknown" to (value?.text ?: ""))
                    }
                }
                is JsonObject -> Pair("object", "object" to "{...}")
                is JsonArray -> Pair("array[]", "array" to psi.text)
                is JsonStringLiteral -> Pair("string", "string" to psi.value)
                is JsonNumberLiteral -> Pair("number", "number" to psi.text)
                is JsonBooleanLiteral -> Pair("boolean", "boolean" to psi.text)
                is JsonNullLiteral -> Pair("null", "null" to "null")
                else -> Pair(data.display, "" to "")
            }
            val type = typeExample.first
            val example = typeExample.second
            val prefix = "-".repeat(depth) + if (depth > 0) " " else ""
            sb.append("| $prefix$field | $type |  | $example |\n")
            if (psi is JsonArray && node.childCount > 0) {
                walk(node.getChildAt(0) as DefaultMutableTreeNode, depth + 1)
            } else {
                for (i in 0 until node.childCount) {
                    walk(node.getChildAt(i) as DefaultMutableTreeNode, depth + 1)
                }
            }
        }
        // 跳过root节点本身，直接导出其子节点
        for (i in 0 until root.childCount) {
            walk(root.getChildAt(i) as DefaultMutableTreeNode, 0)
        }
        return sb.toString()
    }
} 