package chi.quran0.jsonnavigator.ui

import chi.quran0.jsonnavigator.tree.JsonTreeBuilder
import chi.quran0.jsonnavigator.util.TreeUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiManager
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import java.awt.BorderLayout
import javax.swing.*
import chi.quran0.jsonnavigator.tree.JsonTreeNodeData
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.editor.LogicalPosition
import javax.swing.event.TreeSelectionListener
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.EditorFactory
import javax.swing.tree.TreePath
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import org.json.JSONObject
import chi.quran0.jsonnavigator.tree.JsonTreeFilter
import java.awt.FlowLayout
import chi.quran0.jsonnavigator.util.TreeContextMenuUtils
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import javax.swing.AbstractAction
import chi.quran0.jsonnavigator.ui.JsonTreeRenderer

class JsonTreePanel(private val project: Project) : JPanel(BorderLayout()) {
    private var tree: JTree? = null
    private var originalRoot: javax.swing.tree.DefaultMutableTreeNode? = null
    private val searchField = JTextField(20)
    private val clearButton = JButton("×").apply {
        toolTipText = "Clear search"
        isVisible = false
        isFocusPainted = false
        isBorderPainted = false
        isContentAreaFilled = false
        preferredSize = java.awt.Dimension(24, 24)
        addActionListener {
            searchField.text = ""
            filterTree()
        }
    }
    private val helpButton = JButton("?").apply {
        toolTipText = "Show search examples"
        addActionListener {
            JOptionPane.showMessageDialog(
                this@JsonTreePanel,
                "Search examples:\n" +
                "- Keyword: name\n" +
                "- Path: odds_change.odds.market.id\n" +
                "- Path + Value: {\"odds_change.odds.market.id\":115}\n" +
                "- Type: type:array",
                "Search Help",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
        preferredSize = java.awt.Dimension(32, 32)
        margin = java.awt.Insets(2, 6, 2, 6)
    }
    private val searchPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        add(JLabel("Search: "))
        add(searchField)
        add(clearButton)
        add(helpButton)
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    }
    private val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
        val expandBtn = JButton("Expand All")
        val collapseBtn = JButton("Collapse All")
        expandBtn.addActionListener { TreeUtils.expandAll(tree) }
        collapseBtn.addActionListener { TreeUtils.collapseAll(tree) }
        add(expandBtn)
        add(collapseBtn)
        isOpaque = false
        border = BorderFactory.createEmptyBorder(0, 5, 5, 5)
        maximumSize = java.awt.Dimension(Int.MAX_VALUE, 36)
        preferredSize = java.awt.Dimension(300, 36)
    }
    private var caretListener: CaretListener? = null
    private var currentEditor: com.intellij.openapi.editor.Editor? = null
    init {
        layout = BorderLayout()
        add(searchPanel, BorderLayout.NORTH)
        add(buttonPanel, BorderLayout.AFTER_LAST_LINE)
        searchField.toolTipText = "Search (by key/type/path or JSON path expression)"
        searchField.addActionListener { filterTree() }
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                filterTree()
                clearButton.isVisible = searchField.text.isNotEmpty()
            }
            override fun removeUpdate(e: DocumentEvent?) {
                filterTree()
                clearButton.isVisible = searchField.text.isNotEmpty()
            }
            override fun changedUpdate(e: DocumentEvent?) {
                filterTree()
                clearButton.isVisible = searchField.text.isNotEmpty()
            }
        })
        updateTreeForCurrentFile()
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    updateTreeForCurrentFile()
                }
            }
        )
    }
    private fun updateTreeForCurrentFile() {
        ApplicationManager.getApplication().invokeLater {
            val file = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
            removeAll()
            add(searchPanel, BorderLayout.NORTH)
            add(buttonPanel, BorderLayout.AFTER_LAST_LINE)
            if (file != null && isJsonFile(file)) {
                val psiFile = PsiManager.getInstance(project).findFile(file)
                if (psiFile is JsonFile) {
                    val rootNode = JsonTreeBuilder.buildJsonTree(psiFile)
                    originalRoot = rootNode
                    tree = JTree(rootNode)
                    TreeUtils.styleTree(tree)
                    val t = tree ?: return@invokeLater
                    t.cellRenderer = JsonTreeRenderer(t)
                    t.addTreeSelectionListener(TreeSelectionListener { _ ->
                        val node = t.lastSelectedPathComponent as? DefaultMutableTreeNode
                        val data = node?.userObject as? JsonTreeNodeData
                        val psi = data?.psi
                        if (psi != null && psi.isValid) {
                            val offset = psi.textOffset
                            OpenFileDescriptor(project, file, offset).navigate(true)
                        }
                    })
                    t.addMouseListener(object : MouseAdapter() {
                        override fun mousePressed(e: MouseEvent) {
                            maybeShowPopup(e)
                        }
                        override fun mouseReleased(e: MouseEvent) {
                            maybeShowPopup(e)
                        }
                        private fun maybeShowPopup(e: MouseEvent) {
                            if (SwingUtilities.isRightMouseButton(e)) {
                                val row = t.getRowForLocation(e.x, e.y)
                                if (row != -1) {
                                    t.setSelectionRow(row)
                                    val path = t.getPathForRow(row)
                                    val node = path.lastPathComponent as? DefaultMutableTreeNode
                                    if (node != null) {
                                        val popup = TreeContextMenuUtils.createContextMenu(node, t, project)
                                        popup.show(t, e.x, e.y)
                                        return
                                    }
                                }
                                // 兜底：如果没点到任何行，但有选中节点，也弹菜单
                                val selNode = t.lastSelectedPathComponent as? DefaultMutableTreeNode
                                if (selNode != null) {
                                    val popup = TreeContextMenuUtils.createContextMenu(selNode, t, project)
                                    popup.show(t, e.x, e.y)
                                }
                            }
                        }
                    })
                    t.inputMap.put(KeyStroke.getKeyStroke("control C"), "copyNodeValue")
                    t.actionMap.put("copyNodeValue", object : AbstractAction() {
                        override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                            val node = t.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
                            val value = TreeContextMenuUtils
                                .let { it.javaClass.getDeclaredMethod("getNodeJsonValue", DefaultMutableTreeNode::class.java).apply { isAccessible = true } }
                                .invoke(TreeContextMenuUtils, node) as String
                            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                            val selection = java.awt.datatransfer.StringSelection(value)
                            clipboard.setContents(selection, selection)
                        }
                    })
                    add(JScrollPane(t), BorderLayout.CENTER)
                    setupCaretListener(file, rootNode)
                } else {
                    tree = null
                    originalRoot = null
                    add(JLabel("Failed to parse JSON structure."), BorderLayout.CENTER)
                    removeCaretListener()
                }
            } else {
                tree = null
                originalRoot = null
                add(JLabel("Please open a JSON file to view its structure."), BorderLayout.CENTER)
                removeCaretListener()
            }
            revalidate()
            repaint()
        }
    }
    private fun setupCaretListener(file: com.intellij.openapi.vfs.VirtualFile, rootNode: javax.swing.tree.DefaultMutableTreeNode) {
        removeCaretListener()
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        currentEditor = editor
        caretListener = object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                val offset = event.caret?.offset ?: return
                val path = findTreePathForOffset(rootNode, offset)
                if (path != null) {
                    tree?.selectionPath = path
                    tree?.scrollPathToVisible(path)
                }
            }
        }
        editor.caretModel.addCaretListener(caretListener!!)
    }
    private fun removeCaretListener() {
        currentEditor?.caretModel?.removeCaretListener(caretListener ?: return)
        caretListener = null
        currentEditor = null
    }
    private fun findTreePathForOffset(node: javax.swing.tree.DefaultMutableTreeNode, offset: Int, path: MutableList<Any> = mutableListOf()): TreePath? {
        val data = node.userObject as? JsonTreeNodeData
        val psi = data?.psi
        if (psi != null && psi.textRange.containsOffset(offset)) {
            path.add(node)
            for (i in 0 until node.childCount) {
                val child = node.getChildAt(i) as javax.swing.tree.DefaultMutableTreeNode
                val childPath = findTreePathForOffset(child, offset, path.toMutableList())
                if (childPath != null) return childPath
            }
            return TreePath(path.toTypedArray())
        }
        return null
    }
    private fun isJsonFile(file: VirtualFile): Boolean {
        val fileType = FileTypeManager.getInstance().getFileTypeByFile(file)
        return fileType.defaultExtension.equals("json", ignoreCase = true)
    }
    private fun filterTree() {
        val keyword = searchField.text.trim()
        val root = originalRoot ?: return
        val filteredRoot = JsonTreeFilter.filterTree(root, keyword)
        if (filteredRoot != null) {
            tree?.model = javax.swing.tree.DefaultTreeModel(filteredRoot)
            TreeUtils.expandAll(tree)
        }
    }
} 