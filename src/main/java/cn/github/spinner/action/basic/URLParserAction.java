package cn.github.spinner.action.basic;

import cn.github.spinner.ui.URLParameterParser;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class URLParserAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        new URLParameterParser().main(null);
    }
}
