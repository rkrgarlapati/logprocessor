package com.log.table;

import com.log.customcomponents.JSearchTextField;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class FilterJTextField extends JSearchTextField implements TableCellRenderer {
    private List<String> array;

    public FilterJTextField() {
        super();
    }

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value, boolean isSelected, boolean hasFocus, int row,
                                                   int column) {
        setText(value.toString());
        return this;
    }
}