package com.log.table;

import com.log.spark.QueryParams;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.util.HashMap;

public class CellHighlightRenderer extends JTextField implements TableCellRenderer {

    private QueryParams params = QueryParams.getInstance();
    public DefaultHighlighter high = new DefaultHighlighter();
    public DefaultHighlighter.DefaultHighlightPainter highlight_painter = new DefaultHighlighter.DefaultHighlightPainter(
            Color.YELLOW);

    public CellHighlightRenderer() {
        setBorder(BorderFactory.createEmptyBorder());
        setHighlighter(high);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {

        setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());

        setFont(table.getFont());
        setValue(value);
        int pos = 0;

        HashMap<String, Color> words = params.getHighlightWords();

        if (words.size() > 0) {
            for (String word : words.keySet()) {
                while ((pos = value.toString().indexOf(word, pos)) >= 0) {
                    try {
                        highlight_painter = new DefaultHighlighter.DefaultHighlightPainter(words.get(word));
                        // high.addHighlight(first, last, highlight_painter);
                        high.addHighlight(pos, pos + word.length(), highlight_painter);
                        pos += word.length();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return this;
    }

    protected void setValue(Object value) {
        setText((value == null) ? "" : value.toString());
    }
}