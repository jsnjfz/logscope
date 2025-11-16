package com.github.jsnjfz.logscope.toolWindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the tool window and injects the V2EX panel.
 */
public class V2EXToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        V2EXNewsPanel panel = new V2EXNewsPanel(project);
        Content content = ContentFactory.getInstance().createContent(
                panel.getContent(),
                "",
                false
        );
        toolWindow.getContentManager().addContent(content);
    }
}
