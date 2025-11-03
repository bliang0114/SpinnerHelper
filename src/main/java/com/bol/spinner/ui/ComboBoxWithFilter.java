package com.bol.spinner.ui;

import com.intellij.openapi.ui.ComboBox;
import lombok.extern.slf4j.Slf4j;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

@Slf4j
public class ComboBoxWithFilter<E> extends ComboBox<E> {

    public ComboBoxWithFilter(List<E> itemList) {
        this(itemList, null);
    }

    public ComboBoxWithFilter(List<E> itemList, E defaultValue) {
        super();
        DefaultComboBoxModel<E> comboBoxModel = new DefaultComboBoxModel<>();
        for (E item : itemList) {
            comboBoxModel.addElement(item);
        }
        setModel(comboBoxModel);
        setEditable(true);
        AutoCompleteDecorator.decorate(this);
        if (defaultValue != null) {
            if (itemList.contains(defaultValue)) {
                setSelectedItem(defaultValue);
            } else {
                setItem(defaultValue);
            }
        }
    }

    public ComboBoxWithFilter(E @NotNull [] items) {
        super(items);
        setEditable(true);
        AutoCompleteDecorator.decorate(this);
    }
}
