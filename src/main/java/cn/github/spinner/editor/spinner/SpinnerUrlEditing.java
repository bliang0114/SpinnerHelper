package cn.github.spinner.editor.spinner;

import cn.github.spinner.i18n.SpinnerBundle;
import cn.github.spinner.ui.URLFormatterDialog;
import javax.swing.*;
import java.util.function.Predicate;

final class SpinnerUrlEditing {
    static boolean isUrl(String value) {
        return value.strip().matches("(?i)^(https?://|/|\\.\\.?/|\\$\\{[^}]+}/|[^\\s?]+\\.(jsp|html?)(\\?|$)).*");
    }

    static void addAction(JPopupMenu menu, String value, Predicate<String> apply) {
        if (!isUrl(value)) return;
        JMenuItem item = new JMenuItem(SpinnerBundle.message("action.url.parser.text"));
        item.addActionListener(e -> URLFormatterDialog.showWindow(value.strip(), apply));
        menu.add(item);
    }
}
