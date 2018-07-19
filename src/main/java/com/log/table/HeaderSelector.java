package com.log.table;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class HeaderSelector extends MouseAdapter {
    JTable table;
    HeaderEditor editor;

    public HeaderSelector(JTable t) {
        table = t;
        editor = new HeaderEditor();
    }

    public void mousePressed(MouseEvent e) {
        JTableHeader th = (JTableHeader) e.getSource();
        Point point = e.getPoint();

        int scolumn = table.columnAtPoint(point);
        if (scolumn == 0) {
            int col = getColumn(th, point);
            TableColumn column = th.getColumnModel().getColumn(col);
            String oldValue = (String) column.getHeaderValue();
            Object value = editor.showEditor(oldValue);
            column.setHeaderValue(value);
            th.resizeAndRepaint();
        }
    }

    private int getColumn(JTableHeader th, Point p) {
        TableColumnModel model = th.getColumnModel();
        for (int col = 0; col < model.getColumnCount(); col++)
            if (th.getHeaderRect(col).contains(p))
                return col;
        return -1;
    }
}