package chi.quran0.jsonnavigator.tree

import org.json.JSONObject
import javax.swing.tree.DefaultMutableTreeNode

object JsonTreeFilter {
    fun filterTree(root: DefaultMutableTreeNode, keyword: String): DefaultMutableTreeNode? {
        if (keyword.isEmpty()) return root
        return if (keyword.startsWith("{") && keyword.endsWith("}")) {
            try {
                val json = JSONObject(keyword)
                val path = json.keys().asSequence().firstOrNull() ?: ""
                val value = json.opt(path)
                filterNodeByPathAndValue(root, path, value)
            } catch (e: Exception) {
                filterNode(root, keyword.lowercase())
            }
        } else {
            filterNode(root, keyword.lowercase())
        }
    }
    // 命中节点及其所有子节点都显示，父节点用于追溯路径
    private fun filterNode(node: DefaultMutableTreeNode, keyword: String): DefaultMutableTreeNode? {
        val data = node.userObject as? JsonTreeNodeData
        val match = data?.display?.lowercase()?.contains(keyword) == true
        val children = mutableListOf<DefaultMutableTreeNode>()
        for (i in 0 until node.childCount) {
            val child = filterNode(node.getChildAt(i) as DefaultMutableTreeNode, keyword)
            if (child != null) children.add(child)
        }
        return when {
            match -> {
                val newNode = DefaultMutableTreeNode(data)
                for (i in 0 until node.childCount) {
                    newNode.add(cloneNodeWithChildren(node.getChildAt(i) as DefaultMutableTreeNode))
                }
                newNode
            }
            children.isNotEmpty() -> {
                val newNode = DefaultMutableTreeNode(data)
                children.forEach { newNode.add(it) }
                newNode
            }
            else -> null
        }
    }
    private fun cloneNodeWithChildren(node: DefaultMutableTreeNode): DefaultMutableTreeNode {
        val newNode = DefaultMutableTreeNode(node.userObject)
        for (i in 0 until node.childCount) {
            newNode.add(cloneNodeWithChildren(node.getChildAt(i) as DefaultMutableTreeNode))
        }
        return newNode
    }
    fun getPsiPath(psi: com.intellij.psi.PsiElement?): String {
        if (psi == null) return ""
        val path = mutableListOf<String>()
        var cur = psi
        while (cur != null && cur !is com.intellij.json.psi.JsonFile) {
            if (cur is com.intellij.json.psi.JsonProperty) {
                path.add(cur.name)
            }
            cur = cur.parent
        }
        return path.asReversed().joinToString(".")
    }
    private fun filterNodeByPathAndValue(node: DefaultMutableTreeNode, path: String, value: Any?): DefaultMutableTreeNode? {
        val data = node.userObject as? JsonTreeNodeData
        val psi = data?.psi
        val currentPath = getPsiPath(psi)
        val match = currentPath == path && psi?.text?.replace("\"", "") == value.toString()
        val children = mutableListOf<DefaultMutableTreeNode>()
        for (i in 0 until node.childCount) {
            val child = filterNodeByPathAndValue(node.getChildAt(i) as DefaultMutableTreeNode, path, value)
            if (child != null) children.add(child)
        }
        return when {
            match -> {
                val newNode = DefaultMutableTreeNode(data)
                for (i in 0 until node.childCount) {
                    newNode.add(cloneNodeWithChildren(node.getChildAt(i) as DefaultMutableTreeNode))
                }
                newNode
            }
            children.isNotEmpty() -> {
                val newNode = DefaultMutableTreeNode(data)
                children.forEach { newNode.add(it) }
                newNode
            }
            else -> null
        }
    }
} 