package com.log.treeu;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TestEx extends JFrame {

    private String textForSearch = "";
    private JTable t;
    int rowNum = 0;
    boolean scrollflag = false;

    public TestEx() {

        t = new JTable();

        String[] headers = new String[]{"first", "second", "third"};
        DefaultTableModel model = new DefaultTableModel(0, 0);
        model.setColumnIdentifiers(headers);

        for (int i = 0; i < 1000; i++) {
            model.addRow(new Object[]{i});
            model.addRow(new Object[]{i});
        }

        t.setModel(model);

        /*for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(0).setCellRenderer(getRenderer());
        }*/

        JScrollPane jsp = new JScrollPane(t);
        final RightPanel right = new RightPanel();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(jsp, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
        pack();
        setLocationRelativeTo(null);
    }

    public static void scrollToVisible(JTable table, int rowIndex, int vColIndex) {
        if (!(table.getParent() instanceof JViewport)) return;
        JViewport viewport = (JViewport) table.getParent();
        Rectangle rect = table.getCellRect(rowIndex, vColIndex, true);
        Point pt = viewport.getViewPosition();
        rect.setLocation(rect.x - pt.x, rect.y - pt.y);
        viewport.scrollRectToVisible(rect);
    }

    /*private TableCellRenderer getRenderer() {
        return new TableCellRenderer() {
            JTextField f = new JTextField();

            @Override
            public Component getTableCellRendererComponent(JTable arg0, Object arg1, boolean arg2, boolean arg3, int arg4, int arg5) {
                if (arg1 != null) {
                    f.setText(arg1.toString());
                    String string = arg1.toString();
                    if (string.contains(textForSearch)) {
                        System.out.println(arg4 + "--" + scrollflag);
                        if (arg4 > rowNum && scrollflag) {
                            System.out.println("----inside....");

                            scrollToVisible(t, arg4, 0);
                            rowNum = arg4;
                            scrollflag = false;
                            t.repaint();
                        }

                        int indexOf = string.indexOf(textForSearch);
                        try {
                            f.getHighlighter().addHighlight(indexOf, indexOf + textForSearch.length(), new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(Color.RED));
                        } catch (BadLocationException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    f.setText("");
                    f.getHighlighter().removeAllHighlights();
                }
                return f;
            }
        };
    }*/

    class RightPanel extends JPanel {

        public RightPanel() {
            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(5, 5, 5, 5);
            c.gridy = 0;
            final JTextField f = new JTextField(5);
            add(f, c);
            JButton b = new JButton("search");
            b.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    textForSearch = f.getText();
                    //scrollToVisible(t, 110, 0);
                    int totalRows = t.getRowCount();
                    t.clearSelection();
                    for (int row = rowNum + 1; row < totalRows; row++) {
                        String string = String.valueOf(t.getValueAt(row, 0));
                        System.out.println("vall : " + string);
                        if (string.contains(textForSearch)) {
                            scrollToVisible(t, row, 0);
                            try {
                                int indexOf = string.indexOf(textForSearch);
                                JTextField ff = new JTextField();
                                ff.getHighlighter().addHighlight(indexOf, indexOf + textForSearch.length(),
                                        new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(Color.RED));
                            } catch (BadLocationException e) {
                                e.printStackTrace();
                            }
                            rowNum = row;
                            break;
                        }
                    }

                    t.repaint();
                    scrollflag = true;
                }
            });
            c.gridy++;
            add(b, c);
        }
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                new TestEx().setVisible(true);
            }
        });
    }
}