package com.log.treeu;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Something extends JFrame {

    private String textForSearch = "";
    private JTable t;
    private static int rowIndex = -1;
    DefaultTableModel dtm;

    public Something() {

        t = new JTable();

        String[] headers = new String[]{"first", "second", "third"};
        dtm = new DefaultTableModel(0, 0);
        dtm.setColumnIdentifiers(headers);

        for (int i = 0; i < 1555; i++) {
            dtm.addRow(new Object[]{i});
        }

        t.setModel(dtm);
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(0).setCellRenderer(new MyTextFieldRenderer()); //Add JTextField as renderer
        }

        JScrollPane jsp = new JScrollPane(t);
        final RightPanel right = new RightPanel();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(jsp, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
        pack();
        setLocationRelativeTo(null);
    }

    private class MyTextFieldRenderer extends JTextField implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable arg0, Object arg1, boolean arg2, boolean arg3, int row,
                                                       int col) {
            String string = String.valueOf(arg1);
            this.setText(string);
            System.out.println("invoked..." + arg1);
            if (row == rowIndex) //Check if the row of the found text matches this value
            {
                int indexOf = getText().indexOf(textForSearch);
                try {
                    getHighlighter().addHighlight(indexOf, indexOf + textForSearch.length(),
                            new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(Color.RED));
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
            this.setBorder (BorderFactory.createEmptyBorder ());
            return this;
        }

    }

    class RightPanel extends JPanel {

        public RightPanel() {
            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(5, 5, 5, 5);
            c.gridy = 0;
            final JTextField f = new JTextField(11);
            add(f, c);
            JButton b = new JButton("search");
            b.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    //Start all over again
                    int tempRow = rowIndex;
                    /*if (rowIndex + 1 >= t.getRowCount()) {
                        rowIndex = -1;
                        Toolkit.getDefaultToolkit().beep(); //Notepad++ beeps i think
                    }*/
                    for (int row = rowIndex + 1; row < t.getRowCount(); row++) { //Start checking all rows
                        Component cc = t.prepareRenderer(t.getCellRenderer(row, 0), row, 0);
                        if (cc instanceof MyTextFieldRenderer) {
                            MyTextFieldRenderer textField = (MyTextFieldRenderer) cc; //Grab the text of the renderer
                            if (textField.getText().contains(f.getText())) { //We got a match!
                                textForSearch = f.getText();
                                rowIndex = row;
                                int selRow = t.getSelectedRow();

                                scrollToCenter(t, row, 0);
                                /*t.getSelectionModel().setSelectionInterval(row, row); //Scroll to the value
                                t.scrollRectToVisible(new Rectangle(t.getCellRect(row, 0, true)));*/
                                if (selRow != -1) //Something was selected
                                    t.setRowSelectionInterval(selRow, selRow); //Restore selection
                                break;
                            }
                        }
                    }
                    if (rowIndex == tempRow) //Index didn't change, means there is no value for this search
                    {
                        //Toolkit.getDefaultToolkit().beep();
                        int count = t.getRowCount();
                        for (int i = t.getRowCount(); i < count + 1000; i++) {
                            dtm.addRow(new Object[]{i});
                        }
                    }
                    t.repaint();
                }
            });
            c.gridy++;
            add(b, c);
        }
    }

    public static void scrollToCenter(JTable table, int rowIndex, int vColIndex) {
        if (!(table.getParent() instanceof JViewport)) {
            return;
        }
        JViewport viewport = (JViewport) table.getParent();
        Rectangle rect = table.getCellRect(rowIndex, vColIndex, true);
        Rectangle viewRect = viewport.getViewRect();
        rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);

        int centerX = (viewRect.width - rect.width) / 2;
        int centerY = (viewRect.height - rect.height) / 2;
        if (rect.x < centerX) {
            centerX = -centerX;
        }
        if (rect.y < centerY) {
            centerY = -centerY;
        }
        rect.translate(centerX, centerY);
        viewport.scrollRectToVisible(rect);
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                new Something().setVisible(true);
            }
        });
    }
}
