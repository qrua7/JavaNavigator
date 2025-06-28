package chi.quran0.jsonnavigator.ui

import chi.quran0.jsonnavigator.tree.JsonTreeNodeData
import chi.quran0.jsonnavigator.util.TreeUtils
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.JTree
import javax.swing.JPanel
import javax.swing.JLabel
import javax.swing.JButton
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.ActionEvent
import javax.swing.SwingUtilities

class JsonTreeRenderer(private val tree: JTree) : DefaultTreeCellRenderer() {
    override fun getTreeCellRendererComponent(
        tree: JTree?,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        val node = value as? DefaultMutableTreeNode ?: return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
        val data = node.userObject as? JsonTreeNodeData ?: return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)

        val panel = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = null
        }
        val label = JLabel(data.display).apply {
            isOpaque = false
            foreground = if (selected) this@JsonTreeRenderer.textSelectionColor else this@JsonTreeRenderer.textNonSelectionColor
            background = if (selected) this@JsonTreeRenderer.backgroundSelectionColor else this@JsonTreeRenderer.backgroundNonSelectionColor
        }
        panel.add(label, BorderLayout.CENTER)

        // 右侧+/-按钮
        if (node.childCount > 0) {
            val btn = JButton(if (expanded) "−" else "+").apply {
                preferredSize = java.awt.Dimension(18, 18)
                isFocusPainted = false
                isBorderPainted = false
                isContentAreaFilled = false
                toolTipText = if (expanded) "Collapse subtree" else "Expand subtree"
                addActionListener { e: ActionEvent ->
                    val path = javax.swing.tree.TreePath(node.path)
                    if (expanded) {
                        this@JsonTreeRenderer.tree.collapsePath(path)
                    } else {
                        this@JsonTreeRenderer.tree.expandPath(path)
                    }
                }
            }
            panel.add(btn, BorderLayout.EAST)
        }
        return panel
    }
} 