package org.key_project.ide

import tornadofx.App
import tornadofx.launch


object KeyIde {
    @JvmStatic
    fun main(args: Array<String>) {
        launch<IdeApp>(args)
    }
}

val ROOT_CONTEXT = Context().also { context ->
    val appData = ApplicationData().also { it.load() }
    val userConfig = UserConfig().also { it.load() }
    val recentFiles = RecentFiles().also { it.load() }
    val themeManager = ThemeManager(userConfig, appData)
    context.register(appData)
    context.register(userConfig)
    context.register(recentFiles)
    context.register(themeManager)
}

class IdeApp : App(MainView::class) {
    /*
val projectDir = parameters.named["p"]
projectDir?.let {
    val p = Paths.get(it)
    if (p.exists())
        main.fileNavigator.rootFile = p
}

for (file in parameters.unnamed) {
    if (file == "%") {
        main.addEditorTabPane()
        continue
    }
    val p = Paths.get(file)
    if (p.exists()) main.open(p)
}
stage.scene = main.scene

Nodes.addInputMap(
    main.root, sequence(
        consume(keyPressed(KeyCode.F12)) {
            val ld = find<LayoutDebugger>()
            ld.debuggingScene = main.scene
            ld.openModal(modality = Modality.NONE, owner = stage)
        },
    )
)
stage.show()
*/
}