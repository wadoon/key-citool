package org.key_project.ide

import javafx.scene.control.Tab
import javafx.scene.control.TableView
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.readonlyColumn

class IssueList(context: Context) : TitledPanel("Issues") {
    //val view = TreeTableView<IssueEntry>()
    val view = TableView<IssueEntry>()
    val tab: Tab = Tab().also { it.content = ui }

    init {
        context.register(this)

        tab.text = "Issues"
        tab.graphic = FontIcon(FontAwesomeRegular.BELL)
        ui.center = view
        lblHeader.graphic = FontIcon(FontAwesomeRegular.BELL)

        view.readonlyColumn("Title", IssueEntry::title)
        view.readonlyColumn("Category", IssueEntry::category)
        view.readonlyColumn("Offset", IssueEntry::offset)
    }
}

data class IssueEntry(val title: String, val category: String, val offset: Int) {
    //val children = mutableListOf<IssueEntry>()
}
