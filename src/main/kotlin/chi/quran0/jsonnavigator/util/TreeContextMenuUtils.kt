package chi.quran0.jsonnavigator.util

import chi.quran0.jsonnavigator.tree.JsonTreeNodeData
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import javax.swing.JPopupMenu
import javax.swing.JMenuItem
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.JTree
import java.awt.datatransfer.StringSelection

object TreeContextMenuUtils {
    fun createContextMenu(
        node: DefaultMutableTreeNode,
        tree: JTree,
        project: Project
    ): JPopupMenu {
        val popup = JPopupMenu()
        val data = node.userObject as? JsonTreeNodeData ?: return popup

        // 复制字段路径
        val copyPathItem = JMenuItem("Copy Field Path")
        copyPathItem.addActionListener {
            val path = getFieldPath(node)
            CopyPasteManager.getInstance().setContents(StringSelection(path))
        }
        popup.add(copyPathItem)

        // 复制节点值（递归导出JSON）
        val copyValueItem = JMenuItem("Copy Node Value")
        copyValueItem.addActionListener {
            val value = getNodeJsonValue(node)
            CopyPasteManager.getInstance().setContents(StringSelection(value))
        }
        popup.add(copyValueItem)

        // 折叠/展开子树
        val pathObj = javax.swing.tree.TreePath(node.path)
        val expandCollapseItem = JMenuItem(if (tree.isExpanded(pathObj)) "Collapse Subtree" else "Expand Subtree")
        expandCollapseItem.addActionListener {
            if (tree.isExpanded(pathObj)) {
                tree.collapsePath(pathObj)
            } else {
                tree.expandPath(pathObj)
            }
        }
        popup.add(expandCollapseItem)

        return popup
    }

    private fun getFieldPath(node: DefaultMutableTreeNode): String {
        val path = mutableListOf<String>()
        var current: DefaultMutableTreeNode? = node
        while (current?.parent != null) {
            val data = current.userObject as? JsonTreeNodeData
            val display = data?.display ?: ""
            // 提取字段名
            val fieldName = when {
                display.startsWith("root") -> "root"
                display.contains(": {Object}") -> display.substringBefore(": {Object}")
                display.contains(": [Array") -> display.substringBefore(": [Array")
                display.startsWith("[") && display.contains("]:") -> {
                    val index = display.substringAfter("[").substringBefore("]")
                    "[$index]"
                }
                display.contains(": ") -> display.substringBefore(": ")
                else -> display
            }
            if (fieldName.isNotEmpty()) {
                path.add(0, fieldName)
            }
            current = current.parent as? DefaultMutableTreeNode
        }
        // 确保根节点为root
        if (path.isNotEmpty() && path[0] != "root") {
            path.add(0, "root")
        }
        return path.joinToString(".")
    }

    // 递归导出节点及其所有子节点为JSON字符串
    private fun getNodeJsonValue(node: DefaultMutableTreeNode): String {
        val data = node.userObject as? JsonTreeNodeData ?: return "{}"
        // 叶子节点直接返回display右侧的值
        if (node.childCount == 0) {
            val display = data.display
            return if (display.contains(": ")) display.substringAfter(": ").trim() else display
        }
        // 判断是对象还是数组
        val isArray = (0 until node.childCount).all {
            val child = node.getChildAt(it) as? DefaultMutableTreeNode
            val childDisplay = (child?.userObject as? JsonTreeNodeData)?.display ?: ""
            childDisplay.startsWith("[")
        }
        return if (isArray) {
            val arr = (0 until node.childCount).joinToString(", ") {
                val child = node.getChildAt(it) as DefaultMutableTreeNode
                getNodeJsonValue(child)
            }
            "[ $arr ]"
        } else {
            val obj = (0 until node.childCount).joinToString(", ") {
                val child = node.getChildAt(it) as DefaultMutableTreeNode
                val childData = child.userObject as? JsonTreeNodeData
                val key = childData?.display?.substringBefore(":")?.trim(' ', '"') ?: ""
                val value = getNodeJsonValue(child)
                "\"$key\": $value"
            }
            "{ $obj }"
        }
    }
} 