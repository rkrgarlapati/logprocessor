package com.log;

import com.log.common.CleanUpFiles;
import com.log.common.Utility;
import com.log.common.ZipFileReader;
import com.log.db.DBConnection;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class LogProcessorMain extends JFrame {

    public static void main(String[] args) {
        LogProcessorMain main = new LogProcessorMain();
        main.mainWindow();
    }

    public void mainWindow() {
        SwingUtilities.invokeLater(() -> {

            final DBConnection db = DBConnection.getInstance();

            //JFrame frame = new JFrame("Log Processor");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            JPanel panel = new JPanel(new BorderLayout(10, 3));
            panel.setOpaque(true);

            JPanel tablePanel = new JPanel(new GridLayout(1, 1));
            tablePanel.setOpaque(true);

            JPanel rowDiscripPanel = new FixedPanel(100, 40);
            rowDiscripPanel.setLayout(new BorderLayout());
            JTextArea textArea = new JTextArea();
            textArea.setEditable(false);
            textArea.setFont(Font.getFont(Font.SANS_SERIF));
            textArea.setLineWrap(true);
            JScrollPane scroll = new JScrollPane(textArea);
            rowDiscripPanel.add(scroll, BorderLayout.CENTER);

            String[] header = new String[]{"timestamp", "sev", "tag", "message"};
            DefaultTableModel dtm = new DefaultTableModel(0, 0);
            final JTable table = new JTable();
            dtm.setColumnIdentifiers(header);
            table.setModel(dtm);
            ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer())
                    .setHorizontalAlignment(JLabel.CENTER);

            setJTableColumnsWidth(table, 940, 30, 7, 35, 100);

            JScrollPane scroller = new JScrollPane(table);
            scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            JPanel allui = new FixedPanel(100, 40);
            JPanel inputpanel = new JPanel();

            JButton open = new JButton("Browse zip or logcat file");
            JTextField input = new JTextField(20);
            JButton search = new JButton("Search");
            DefaultCaret caret = (DefaultCaret) textArea.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            JComboBox<String> tagIndexValues = new JComboBox<>();
            JComboBox<String> sevIndexValues = new JComboBox<>();

            final JFileChooser fileChooser = new JFileChooser();
            final Connection conn = db.connection();

            open.addActionListener(ae -> {
                int result = fileChooser.showOpenDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {

                    final JDialog jDialog = getJDialog("Please wait, data loading...");

                    File file = fileChooser.getSelectedFile();
                    try {
                        String fileType = new Tika().detect(file);

                        final List<File> files = new ArrayList<>();

                        if (fileType.contains("zip")) {

                            /*JDialog zipPross = getJDialog("Decompressing zip file...");

                            new SwingWorker<Boolean, String>() {
                                @Override
                                protected Boolean doInBackground() throws Exception {*/

                            ZipFileReader zipReader = ZipFileReader.getInstance();
                            files.addAll(zipReader.zipParser(file.getPath()));

                                    /*return true;
                                }

                                @Override
                                protected void done() {
                                    super.done();
                                    zipPross.dispose();
                                }
                            }.execute();

                            zipPross.setVisible(true);*/

                        } else if (fileType.contains("text")) {
                            files.add(file);
                        }

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                        System.out.println("Started  : " + sdf.format(new Date()));

                        String sql = "INSERT INTO DATA (TIMESTAMP, sev, tag, message)" +
                                " VALUES (?,?,?,?)";

                        PreparedStatement pstmt = conn.prepareStatement(sql);

                        String deleteData = "DELETE FROM DATA";
                        dtm.setRowCount(0);

                        if (sevIndexValues.getItemAt(0) != null) {
                            /*JDialog oldLogs = getJDialog("Clearing old logs...");

                            new SwingWorker<Boolean, String>() {
                                @Override
                                protected Boolean doInBackground() throws Exception {*/

                                    System.out.println("Deleting old data started...");

                                    PreparedStatement deletePrep = conn.prepareStatement(deleteData);
                                    deletePrep.execute();
                                    tagIndexValues.removeAllItems();
                                    sevIndexValues.removeAllItems();

                                    System.out.println("Deleting old data completed...");

                                    /*return true;
                                }

                                @Override
                                protected void done() {
                                    super.done();
                                    oldLogs.dispose();
                                }
                            }.execute();

                            oldLogs.setVisible(true);*/
                        }

                        List<String> fileNames = new CopyOnWriteArrayList<>();

                        for (File cfile : files) {

                            String fileName = cfile.getName();

                            if (cfile.getPath().contains("logs/logcat") ||
                                    cfile.getPath().contains("logcat")) {
                                //System.out.println("file name:" + fileName);
                                fileNames.add(fileName);

                                BufferedInputStream in = new BufferedInputStream(new FileInputStream(cfile));
                                BufferedReader br = new BufferedReader(
                                        new InputStreamReader(in, StandardCharsets.UTF_8));

                                new SwingWorker<Boolean, String>() {
                                    @Override
                                    protected Boolean doInBackground() throws Exception {

                                        String line;
                                        while ((line = br.readLine()) != null) {
                                            line = line.trim().replaceAll(" +", " ");
                                            publish(line);
                                        }

                                        br.close();

                                        return true;
                                    }

                                    @Override
                                    protected void process(List<String> chunks) {
                                        try {
                                            for (String line : chunks) {
                                                String[] sp = line.split(" ");
                                                try {
                                                    String msg = line.substring(line.indexOf(sp[5]));

                                                    dtm.addRow(new String[]{sp[0] + " " + sp[1], sp[4], sp[5], msg});

                                                    pstmt.setString(1, sp[0] + " " + sp[1]);

                                                    pstmt.setString(2, sp[4]);
                                                    pstmt.setString(3, sp[5]);
                                                    pstmt.setString(4, msg);

                                                    pstmt.addBatch();

                                                } catch (Exception ex) {
                                                }
                                            }

                                            pstmt.executeBatch();
                                            conn.commit();

                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }

                                        System.out.println("Number of records processed: " + chunks.size());
                                    }

                                    @Override
                                    protected void done() {
                                        super.done();

                                        System.out.println("File completed processing:" + fileName);
                                        fileNames.remove(fileName);

                                        if (fileNames.isEmpty()) {
                                            System.out.println("Finished  : " + sdf.format(new Date()));
                                            jDialog.dispose();

                                            List<String> indexLines = db.getTagIndexData();
                                            String[] sev = {"V", "D", "I", "W", "E", "F"};

                                            tagIndexValues.addItem("Select...");
                                            indexLines.stream().forEach(row -> {
                                                tagIndexValues.addItem(row);
                                            });

                                            tagIndexValues.addActionListener(event -> {
                                                JComboBox<String> combo = (JComboBox<String>) event.getSource();
                                                String selectedVal = (String) combo.getSelectedItem();
                                                if (selectedVal != null && !selectedVal.equals("Select...")) {

                                                    dtm.setRowCount(0);

                                                    String sevVal = sevIndexValues.getSelectedItem().toString();
                                                    List<String[]> allLines = new ArrayList<>();

                                                    if (!sevVal.equals("Select...")) {
                                                        allLines =
                                                                db.readSelectedIndexValues(selectedVal.split(" ")[0],
                                                                        Utility.getSevValues(sev, sevVal));
                                                    } else {
                                                        allLines = db.readSelectedTag(selectedVal.split(" ")[0]);
                                                    }

                                                    allLines.stream().forEach(row -> {
                                                        dtm.addRow(row);
                                                    });
                                                }
                                            });
                                            tagIndexValues.setSelectedItem("Select...");

                                            List<String> indexSevLines = Arrays.asList(sev);

                                            sevIndexValues.addItem("Select...");
                                            indexSevLines.stream().forEach(row -> {
                                                sevIndexValues.addItem(row);
                                            });

                                            sevIndexValues.addActionListener(event -> {
                                                JComboBox<String> combo = (JComboBox<String>) event.getSource();
                                                String select = (String) combo.getSelectedItem();
                                                if (select != null && !select.equals("Select...")) {
                                                    dtm.setRowCount(0);
                                                    String tagVal = tagIndexValues.getSelectedItem().toString();
                                                    List<String[]> allLines = new ArrayList<>();
                                                    if (!tagVal.equals("Select...")) {
                                                        String tag = tagIndexValues.getSelectedItem().toString().split(" ")[0];
                                                        allLines = db.readSelectedIndexValues(tag,
                                                                Utility.getSevValues(sev, select));
                                                    } else {
                                                        allLines = db.readSelectedSev(Utility.getSevValues(sev, select));
                                                    }
                                                    allLines.stream().forEach(row -> {
                                                        dtm.addRow(row);
                                                    });
                                                }
                                            });
                                            sevIndexValues.setSelectedItem("Select...");

                                        }

                                        cleanUpFiles();
                                    }
                                }.execute();

                            }
                        }

                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } finally {
                        cleanUpFiles();
                    }

                    jDialog.setVisible(true);

                }
            });


            search.addActionListener(ae -> {
                dtm.setRowCount(0);
                java.util.List<String[]> allLines = db.readData(input.getText());

                allLines.stream().forEach(row -> {
                    dtm.addRow(row);
                });
            });

            table.getSelectionModel().addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                    String selectedRow = table.getValueAt(table.getSelectedRow(), 3).toString();

                    textArea.setText(selectedRow);

                }
            });

            inputpanel.add(open);
            inputpanel.add(input);
            inputpanel.add(search);
            inputpanel.add(tagIndexValues);
            inputpanel.add(sevIndexValues);

            allui.add(inputpanel);

            tablePanel.add(scroller, BorderLayout.NORTH);

            panel.setPreferredSize(new Dimension(1140, 680));
            panel.add(allui, BorderLayout.NORTH);
            panel.add(tablePanel, BorderLayout.CENTER);
            panel.add(rowDiscripPanel, BorderLayout.SOUTH);

            getContentPane().add(BorderLayout.CENTER, panel);
            pack();
            setLocationByPlatform(true);
            setLocationRelativeTo(null);

            setVisible(true);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            input.requestFocus();
        });
    }


    private class MyRenderer extends JTextArea implements TableCellRenderer {
        public MyRenderer() {
            setOpaque(true);
            setLineWrap(true);
            setWrapStyleWord(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {

            this.setText(value == null ? "" : value.toString());
            return this;
        }

    }

    public static void setJTableColumnsWidth(JTable table, int tablePreferredWidth,
                                             double... percentages) {
        double total = 0;
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            total += percentages[i];
        }

        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth((int)
                    (tablePreferredWidth * (percentages[i] / total)));
        }
    }

    public JDialog getJDialog(String msg) {
        final JDialog jDialog = new JDialog();
        JPanel p1 = new JPanel(new GridBagLayout());
        p1.add(new JLabel(msg), new GridBagConstraints());
        jDialog.getContentPane().add(p1);

        jDialog.setSize(200, 50);
        jDialog.setLocationRelativeTo(this);
        jDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        jDialog.setModal(true);

        return jDialog;
    }

    private void cleanUpFiles() {

        CleanUpFiles cleanUpFiles = CleanUpFiles.getInstance();

        FileUtils.deleteQuietly(new File(cleanUpFiles.getFolderName()));
        FileUtils.deleteQuietly(new File(cleanUpFiles.getZipName()));
    }

}

class FixedPanel extends JPanel {
    public FixedPanel(int W, int H) {
        this.setPreferredSize(new Dimension(W, H));
    }
}