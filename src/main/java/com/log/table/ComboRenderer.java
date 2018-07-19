package com.log.table;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ComboRenderer extends FilterComboBox implements TableCellRenderer {

    public ComboRenderer(String[] items) {
        for (int i = 0; i < items.length; i++) {
            addItem(items[i]);
        }
    }

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value, boolean isSelected, boolean hasFocus, int row,
                                                   int column) {
        setSelectedItem(value);
        return this;
    }
}