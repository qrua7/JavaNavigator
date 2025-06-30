# Json Navigator

Json Navigator is a plugin for the IntelliJ IDEA platform, designed to provide structured navigation and enhanced editing/reading experience for large JSON files.

## Features
- Tree view navigation: Display JSON structure as a tree, supporting objects, arrays, and fields with expand/collapse
- Node navigation: Click a tree node to jump to the corresponding position in the editor
- Search/filter: Filter nodes by keyword, path, type, and more
- Context menu: Copy field path, copy node value, expand/collapse subtree
- +/- button on node right: Quickly expand/collapse subtrees
- Clear button in search box: One-click clear search content
- User interface with standard interactions, supports large files

## Installation
### Method 1: Local installation
1. Build the plugin with `./gradlew buildPlugin`, the zip package will be in `build/distributions/`
2. In IDEA, go to `Settings > Plugins > Install Plugin from Disk...`
3. Select the zip package, install and restart IDEA

### Method 2: JetBrains Marketplace (coming soon)
1. Search for `Json Navigator` in the plugin marketplace
2. Install with one click and restart IDEA

## Usage
- Open any JSON file, the Json Navigator tool window will appear in the sidebar
- Use the tree to quickly locate, jump, expand/collapse nodes
- Context menu and shortcuts are supported for efficient editing
- The search box supports various expressions for quick field location

## Compatibility
- Compatible with IntelliJ IDEA 2022.1 and above (adjust `<idea-version>` in plugin.xml as needed)
- Depends on IntelliJ platform's built-in JSON support

## Contribution & Feedback
- Issues, PRs, and suggestions are welcome!
- For questions, contact the author via JetBrains Marketplace or project homepage

## AI Development Notes
This project was developed entirely by AI. Related documentation includes:
- `ai_summary.md`: Project overview, stage progress, current to-do items
- `ai_memory.md`: Detailed development process records, decision history, legacy issues

**If you want to continue AI development or contribute, please make sure to sync these two files to ensure AI can understand the project context and development history.**

## License
This project is licensed under the MIT License. See the LICENSE file for details. 