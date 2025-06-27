package chi.quran0.jsonnavigator.tree

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonArray
import com.intellij.psi.PsiElement
import javax.swing.tree.DefaultMutableTreeNode

data class JsonTreeNodeData(val display: String, val psi: PsiElement?) {
    override fun toString(): String = display
}

object JsonTreeBuilder {
    fun buildJsonTree(psiFile: JsonFile): DefaultMutableTreeNode {
        val rootPsi = psiFile.firstChild
        return when (rootPsi) {
            is JsonObject -> {
                val rootNode = DefaultMutableTreeNode(JsonTreeNodeData("root {Object}", rootPsi))
                buildJsonObjectTree(rootPsi, rootNode)
                rootNode
            }
            is JsonArray -> {
                val rootNode = DefaultMutableTreeNode(JsonTreeNodeData("root [Array, size=${rootPsi.valueList.size}]", rootPsi))
                buildJsonArrayTree(rootPsi, rootNode)
                rootNode
            }
            else -> DefaultMutableTreeNode(JsonTreeNodeData("root (empty or unknown)", null))
        }
    }
    private fun buildJsonObjectTree(jsonObject: JsonObject, parentNode: DefaultMutableTreeNode) {
        for (property in jsonObject.propertyList) {
            val key = property.name
            val value = property.value
            when (value) {
                is JsonObject -> {
                    val objNode = DefaultMutableTreeNode(JsonTreeNodeData("$key: {Object}", value))
                    buildJsonObjectTree(value, objNode)
                    parentNode.add(objNode)
                }
                is JsonArray -> {
                    val arrNode = DefaultMutableTreeNode(JsonTreeNodeData("$key: [Array, size=${value.valueList.size}]", value))
                    buildJsonArrayTree(value, arrNode)
                    parentNode.add(arrNode)
                }
                else -> {
                    parentNode.add(DefaultMutableTreeNode(JsonTreeNodeData("$key: ${value?.text ?: "null"}", value)))
                }
            }
        }
    }
    private fun buildJsonArrayTree(jsonArray: JsonArray, parentNode: DefaultMutableTreeNode) {
        val values = jsonArray.valueList
        for ((index, value) in values.withIndex()) {
            when (value) {
                is JsonObject -> {
                    val objNode = DefaultMutableTreeNode(JsonTreeNodeData("[$index]: {Object}", value))
                    buildJsonObjectTree(value, objNode)
                    parentNode.add(objNode)
                }
                is JsonArray -> {
                    val arrNode = DefaultMutableTreeNode(JsonTreeNodeData("[$index]: [Array, size=${value.valueList.size}]", value))
                    buildJsonArrayTree(value, arrNode)
                    parentNode.add(arrNode)
                }
                else -> {
                    parentNode.add(DefaultMutableTreeNode(JsonTreeNodeData("[$index]: ${value?.text ?: "null"}", value)))
                }
            }
        }
    }
} 