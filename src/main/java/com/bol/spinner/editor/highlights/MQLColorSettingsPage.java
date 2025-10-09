package com.bol.spinner.editor.highlights;

import com.bol.spinner.editor.icons.MQLIcons;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

public class MQLColorSettingsPage implements ColorSettingsPage {
    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
            new AttributesDescriptor("Keyword", MQLSyntaxHighlighter.KEYWORD),
            new AttributesDescriptor("Function", MQLSyntaxHighlighter.FUNCTION),
            new AttributesDescriptor("Number", MQLSyntaxHighlighter.NUMBER),
            new AttributesDescriptor("String", MQLSyntaxHighlighter.STRING),
            new AttributesDescriptor("Comment", MQLSyntaxHighlighter.COMMENT),
            new AttributesDescriptor("Preprocessor", MQLSyntaxHighlighter.PREPROCESSOR),
            new AttributesDescriptor("Type", MQLSyntaxHighlighter.TYPE),
    };

    @Nullable
    @Override
    public Icon getIcon() {
        return MQLIcons.FILE;
    }

    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new MQLSyntaxHighlighter();
    }

    @NotNull
    @Override
    public String getDemoText() {
        return "";
    }

    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @NotNull
    @Override
    public AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @NotNull
    @Override
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "MQL";
    }
}