package cn.github.spinner.editor;

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate;
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MQLCommentEnterHandler extends EnterHandlerDelegateAdapter {
    @Override
    public @NotNull EnterHandlerDelegate.Result preprocessEnter(
            @NotNull PsiFile file,
            @NotNull Editor editor,
            @NotNull Ref<Integer> caretOffsetRef,
            @NotNull Ref<Integer> caretAdvance,
            @NotNull DataContext dataContext,
            @Nullable EditorActionHandler originalHandler) {
        if (!file.getLanguage().isKindOf(MQLLanguage.INSTANCE)) {
            return EnterHandlerDelegate.Result.Continue;
        }

        Document document = editor.getDocument();
        int offset = caretOffsetRef.get();
        int lineNumber = document.getLineNumber(offset);
        int lineStart = document.getLineStartOffset(lineNumber);
        int lineEnd = document.getLineEndOffset(lineNumber);
        CharSequence chars = document.getCharsSequence();
        String beforeCaret = chars.subSequence(lineStart, offset).toString();
        String afterCaret = chars.subSequence(offset, lineEnd).toString();
        String trimmed = beforeCaret.trim();
        if (!afterCaret.isBlank()) {
            return EnterHandlerDelegate.Result.Continue;
        }

        String indentation = leadingWhitespace(beforeCaret);
        String insertion;
        int caretOffset;
        if (trimmed.equals("/*") || trimmed.equals("/**")) {
            insertion = "\n" + indentation + " * \n" + indentation + " */";
            caretOffset = offset + indentation.length() + 4;
        } else if (trimmed.startsWith("*") && !trimmed.startsWith("*/") && isInsideBlockComment(chars, offset)) {
            insertion = "\n" + indentation + "* ";
            caretOffset = offset + insertion.length();
        } else {
            return EnterHandlerDelegate.Result.Continue;
        }

        document.insertString(offset, insertion);
        editor.getCaretModel().moveToOffset(caretOffset);
        return EnterHandlerDelegate.Result.Stop;
    }

    private static boolean isInsideBlockComment(@NotNull CharSequence chars, int offset) {
        String precedingText = chars.subSequence(0, offset).toString();
        return precedingText.lastIndexOf("/*") > precedingText.lastIndexOf("*/");
    }

    private static @NotNull String leadingWhitespace(@NotNull String text) {
        int length = 0;
        while (length < text.length()) {
            char current = text.charAt(length);
            if (current != ' ' && current != '\t') {
                break;
            }
            length++;
        }
        return text.substring(0, length);
    }
}
