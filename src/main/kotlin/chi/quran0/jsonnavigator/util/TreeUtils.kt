package chi.quran0.jsonnavigator.util

import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JTree
import javax.swing.UIManager

object TreeUtils {
    fun expandAll(tree: JTree?) {
        tree?.let {
            for (i in 0 until it.rowCount) {
                it.expandRow(i)
            }
        }
    }
    fun collapseAll(tree: JTree?) {
        tree?.let {
            for (i in it.rowCount - 1 downTo 1) {
                it.collapseRow(i)
            }
        }
    }
    fun styleTree(tree: JTree?) {
        tree?.let {
            it.font = Font(UIManager.getFont("Label.font").name, Font.PLAIN, 14)
            it.rowHeight = 22
            it.border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
            it.showsRootHandles = true
            it.setRootVisible(true)
            it.setCellRenderer(it.cellRenderer)
        }
    }
} 