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
import chi.quran0.jsonnavigator.util.MarkdownExportUtils
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

class JsonTreePanel(private val project: Project) : JPanel(BorderLayout()) {
    private var tree: JTree? = null
    private var originalRoot: javax.swing.tree.DefaultMutableTreeNode? = null
    private val searchField = JTextField(20)
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
        add(helpButton)
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    }
    private val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
        val expandBtn = JButton("Expand All")
        val collapseBtn = JButton("Collapse All")
        val exportBtn = JButton("Export Markdown")
        expandBtn.addActionListener { TreeUtils.expandAll(tree) }
        collapseBtn.addActionListener { TreeUtils.collapseAll(tree) }
        exportBtn.addActionListener { exportMarkdown() }
        add(expandBtn)
        add(collapseBtn)
        add(exportBtn)
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
            override fun insertUpdate(e: DocumentEvent?) = filterTree()
            override fun removeUpdate(e: DocumentEvent?) = filterTree()
            override fun changedUpdate(e: DocumentEvent?) = filterTree()
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
                    tree?.addTreeSelectionListener(TreeSelectionListener { e ->
                        val node = tree?.lastSelectedPathComponent as? javax.swing.tree.DefaultMutableTreeNode
                        val data = node?.userObject as? JsonTreeNodeData
                        val psi = data?.psi
                        if (psi != null && psi.isValid) {
                            val offset = psi.textOffset
                            OpenFileDescriptor(project, file, offset).navigate(true)
                        }
                    })
                    add(JScrollPane(tree), BorderLayout.CENTER)
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
    private fun exportMarkdown() {
        val root = tree?.model?.root as? javax.swing.tree.DefaultMutableTreeNode ?: return
        val md = MarkdownExportUtils.treeToMarkdown(root)
        val chooser = JFileChooser().apply {
            dialogTitle = "Export Markdown"
            fileFilter = FileNameExtensionFilter("Markdown Files", "md")
            selectedFile = File("structure.md")
        }
        val result = chooser.showSaveDialog(this)
        if (result == JFileChooser.APPROVE_OPTION) {
            val file = chooser.selectedFile
            file.writeText(md)
            JOptionPane.showMessageDialog(this, "Exported to: ${file.absolutePath}", "Export Success", JOptionPane.INFORMATION_MESSAGE)
        }
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