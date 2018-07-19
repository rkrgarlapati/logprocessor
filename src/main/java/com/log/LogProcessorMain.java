package com.log;

import com.log.common.CleanUpFiles;
import com.log.common.Constants;
import com.log.common.ZipFileReader;
import com.log.customcomponents.JSearchTextField;
import com.log.spark.QueryParams;
import com.log.spark.SparkProcessor;
import com.log.table.*;
import com.log.table.column.EditableHeader;
import com.log.table.column.EditableHeaderTableColumn;
import com.log.table.column.FixedPanel;
import com.log.tree.TreeFrame;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.sql.Row;
import org.apache.tika.Tika;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

class LogProcessorMain extends JFrame {

    private SparkProcessor sparkProcessor;
    private QueryParams queryParams = QueryParams.getInstance();
    private DefaultTableModel dtm;
    private JTextField tagTextField;
    private JTextField msgTextField;
    private JComboBox<String> sevIndexValues;
    private boolean isDeleteTree;
    private JSearchTextField searchInAllFiles;
    private JSearchTextField filterSearchInAllFiles;
    private JTable table;
    private JScrollPane scroller;
    private String textForSearch = "";
    private static int rowIndex = -1;
    private int tableSize;

    public static void main(String[] args) {
        LogProcessorMain main = new LogProcessorMain();
        main.mainWindow(main);
    }

    public void mainWindow(final LogProcessorMain main) {
        SwingUtilities.invokeLater(() -> {

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

            JTree tree = new JTree(TreeFrame.addNodes(null, null));

            JScrollPane listScrollPane = new JScrollPane(tree,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            listScrollPane.setPreferredSize(new Dimension(0, 400));

            JTree searchtree = new JTree(TreeFrame.addNodes(null, null));
            JScrollPane searchlistScrollPane = new JScrollPane(searchtree,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            searchlistScrollPane.setPreferredSize(new Dimension(0, 1));

            searchInAllFiles = new JSearchTextField();
            searchInAllFiles.setTextWhenNotFocused("Search in files");
            searchInAllFiles.setPreferredSize(new Dimension(250, 30));
            //searchFiles.setUI(new JTextFieldHintUI(" Search Text in files", Color.gray));


            filterSearchInAllFiles = new JSearchTextField();
            filterSearchInAllFiles.setTextWhenNotFocused("Filter search in files");

            String[] headers = new String[]{"timestamp", "sev", "", ""};
            dtm = new DefaultTableModel(0, 0);

            table = new JTable() {
                public boolean getScrollableTracksViewportWidth() {
                    return getPreferredSize().width < getParent().getWidth();
                }
            };
            dtm.setColumnIdentifiers(headers);
            table.setModel(dtm);
            table.setDefaultRenderer(Object.class, new CellHighlightRenderer());
            /*((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer())
                    .setHorizontalAlignment(JLabel.CENTER);*/

            setJTableColumnsWidth(table, 1500, 35, 15, 30, 255);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            TableColumnModel columnModel = table.getColumnModel();
            table.setTableHeader(new EditableHeader(columnModel));
            table.getTableHeader().addMouseListener(new HeaderSelector(table));
            table.setRowSelectionAllowed(true);


            scroller = new JScrollPane(table);
            //scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            //scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scroller.setColumnHeader(new JViewport() {
                @Override
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    d.height = 25;
                    return d;
                }
            });


            JPanel allui = new FixedPanel(100, 40);
            JPanel inputpanel = new JPanel();
            inputpanel.setLayout(new BoxLayout(inputpanel, BoxLayout.LINE_AXIS));

            Icon openIcon = new ImageIcon("open.png");
            JButton open = new JButton(openIcon);
            open.setContentAreaFilled(false);
            open.setPreferredSize(new Dimension(70, 30));
            open.setText("Open");
            open.setToolTipText("Open File/Folder/Zip");

            Icon binIcon = new ImageIcon("bin.png");
            JButton bin = new JButton(binIcon);
            bin.setContentAreaFilled(false);
            bin.setPreferredSize(new Dimension(90, 30));
            bin.setText("Delete All");
            bin.setToolTipText("Clear all logs from the buffer.");

            Icon upIcon = new ImageIcon("up.png");
            JButton top = new JButton(upIcon);
            top.setContentAreaFilled(false);
            top.setPreferredSize(new Dimension(90, 30));
            top.setText("Scroll Top");
            top.setToolTipText("Scroll to the top.");

            //final JTextField globalSearchField = new JTextField(20);

            JLabel rowCountLabel = new JLabel();
            tagTextField = new JTextField(15);
            msgTextField = new JTextField(35);
            tagTextField.setPreferredSize(new Dimension(200, 15));

            JButton search = new JButton("Filter Search");
            search.setPreferredSize(new Dimension(110, 30));

            DefaultCaret caret = (DefaultCaret) textArea.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            JComboBox<String> tagIndexValues = new JComboBox<>();
            sevIndexValues = new JComboBox<>();

            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

            //final Connection conn = db.connection();

            sparkProcessor = new SparkProcessor();

            Action openaction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    int result = fileChooser.showOpenDialog(main);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        final JDialog jDialog = getJDialog("Please wait, data loading...");
                        File fileBrowse = fileChooser.getSelectedFile();

                        try {
                            final List<File> files = validateInputSource(fileBrowse);

                            if (files != null) {

                                if (!fileBrowse.isDirectory()) {
                                    String fileType = new Tika().detect(fileBrowse);
                                    if (fileType.contains("bzip2")) {
                                        fileBrowse = ZipFileReader.getInstance().getUnzippedfolder();
                                    }
                                }

                                DefaultTreeModel tmodel = (DefaultTreeModel) tree.getModel();
                                DefaultMutableTreeNode root = (DefaultMutableTreeNode) tmodel.getRoot();
                                root.add(TreeFrame.addNodes(null, fileBrowse));
                                tmodel.reload(root);

                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                                System.out.println("Started processing : " + sdf.format(new Date()));

                                List<String> fileNames = new CopyOnWriteArrayList<>();

                                File finalFileBrowse = fileBrowse;
                                new SwingWorker<Boolean, String>() {
                                    @Override
                                    protected Boolean doInBackground() throws Exception {

                                        //sparkProcessor.writeFileToSchema(file.getPath() + "/logs");

                                        String filePath = finalFileBrowse.getPath();

                                        sparkProcessor.writeFileToSchema(filePath);

                                        for (File cfile : files) {

                                            String fileName = cfile.getName();

                                            if (cfile.getPath().contains("logs/logcat") ||
                                                    cfile.getName().startsWith("logcat")) {

                                                //app.writeFileToSchema(cfile.getPath());
                                                //System.out.println("UI processing file name:" + fileName);
                                                fileNames.add(fileName);

                                                BufferedInputStream in = new BufferedInputStream(new FileInputStream(cfile));
                                                BufferedReader br = new BufferedReader(
                                                        new InputStreamReader(in, StandardCharsets.UTF_8));

                                                new SwingWorker<Boolean, String>() {
                                                    @Override
                                                    protected Boolean doInBackground() throws Exception {

                                                        //By default, initially Table shows only 1000 records on screen.
                                                        if (dtm.getRowCount() < 1000) {
                                                            String line;
                                                            while ((line = br.readLine()) != null) {
                                                                line = line.trim().replaceAll(" +", " ");
                                                                publish(line);
                                                            }
                                                            br.close();

                                                            return true;
                                                        }

                                                        return false;
                                                    }

                                                    @Override
                                                    protected void process(List<String> chunks) {
                                                        try {
                                                            if (dtm.getRowCount() < 1000) {
                                                                for (String line : chunks) {
                                                                    String[] sp = line.split(" ");

                                                                    String msg = line.substring(line.indexOf(sp[5]));
                                                                    String[] row = new String[]{sp[0] + " " + sp[1], sp[4], sp[5], msg};
                                                                    dtm.addRow(row);
                                                                }
                                                            }
                                                        } catch (Exception e) {
                                                            //row split issues
                                                        }
                                                    }

                                                    @Override
                                                    protected void done() {
                                                        super.done();

                                                        fileNames.remove(fileName);

                                                        if (fileNames.isEmpty()) {

                                                            System.out.println("Finished  : " + sdf.format(new Date()));
                                                            //System.out.println("Total rows count : " + dtm.getRowCount());

                                                            //table.getColumnModel().getColumn(3).setCellRenderer(new MyTextFieldRenderer());

                                                            sevIndexValues.removeAllItems();

                                                            List<String> indexSevLines = Arrays.asList(Constants.sev);
                                                            indexSevLines.stream().forEach(row -> {
                                                                sevIndexValues.addItem(row);
                                                            });

                                                            FilterComboBox renderer = new FilterComboBox(indexSevLines);

                                                            EditableHeaderTableColumn sevcol;
                                                            sevcol = (EditableHeaderTableColumn) table.getColumnModel().getColumn(1);
                                                            sevcol.setHeaderValue(sevIndexValues.getItemAt(0));
                                                            sevcol.setHeaderRenderer(renderer);
                                                            sevcol.setHeaderEditor(new DefaultCellEditor(sevIndexValues));

                                                            FilterJTextField tagEditTextField = new FilterJTextField();
                                                            tagEditTextField.setTextWhenNotFocused("Search Tag");
                                                            //tagEditTextField.setUI(new JTextFieldHintUI("  Search Tag", Color.gray));
                                                            EditableHeaderTableColumn tagcol;
                                                            tagcol = (EditableHeaderTableColumn) table.getColumnModel().getColumn(2);
                                                            tagcol.setHeaderRenderer(tagEditTextField);
                                                            tagcol.setHeaderEditor(new DefaultCellEditor(tagTextField));


                                                            FilterJTextField msgEditTextField = new FilterJTextField();
                                                            msgEditTextField.setTextWhenNotFocused("Search Message");
                                                            //msgEditTextField.setUI(new JTextFieldHintUI("  Search message", Color.gray));
                                                            EditableHeaderTableColumn msgcol;
                                                            msgcol = (EditableHeaderTableColumn) table.getColumnModel().getColumn(3);
                                                            msgcol.setHeaderRenderer(msgEditTextField);
                                                            msgcol.setHeaderEditor(new DefaultCellEditor(msgTextField));

                                                            rowCountLabel.setText("Total Rows : " + sparkProcessor.getRowcount());

                                                            tableSize = dtm.getRowCount();
                                                            queryParams.setInitialIndex(tableSize);

                                                            setDeleteTree(false);
                                                            scroller.revalidate();
                                                            scroller.repaint();

                                                            jDialog.dispose();
                                                        }

                                                        cleanUpUnZipFiles();
                                                    }
                                                }.execute();

                                            } // if statement
                                        } // for loop

                                        return true;
                                    }
                                }.execute();

                                jDialog.setVisible(true);
                            } else {
                                jDialog.dispose();
                                return;
                            }

                        } catch (java.io.IOException e) {
                            e.printStackTrace();
                        } finally {
                            cleanUpUnZipFiles();
                        }
                    }
                }
            };

            open.addActionListener(openaction);

            Action binaction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent ae) {

                    sparkProcessor.truncateLogs();
                    queryParams.clearAll();
                    setDeleteTree(true);
                    clearFilters();

                    DefaultTreeModel ttmodel = (DefaultTreeModel) tree.getModel();
                    DefaultMutableTreeNode troot = (DefaultMutableTreeNode) ttmodel.getRoot();
                    troot.removeAllChildren();
                    ttmodel.reload();

                    DefaultTreeModel stmodel = (DefaultTreeModel) searchtree.getModel();
                    DefaultMutableTreeNode sroot = (DefaultMutableTreeNode) stmodel.getRoot();
                    sroot.removeAllChildren();
                    stmodel.reload();

                    dtm.setRowCount(0);

                    rowCountLabel.setText("Total Rows : " + sparkProcessor.getRowcount());

                    EditableHeaderTableColumn tagcol;
                    tagcol = (EditableHeaderTableColumn) table.getColumnModel().getColumn(2);
                    tagcol.setHeaderValue("");

                    EditableHeaderTableColumn msgcol;
                    msgcol = (EditableHeaderTableColumn) table.getColumnModel().getColumn(3);
                    msgcol.setHeaderValue("");

                    scroller.revalidate();
                    scroller.repaint();
                }
            };

            bin.addActionListener(binaction);


            Action topaction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    //table.getSelectionModel().setSelectionInterval(0, 0);
                    //table.scrollRectToVisible(new Rectangle(table.getCellRect(0, 0, true)));

                    table.scrollRectToVisible(table.getCellRect(0, 0, true));
                    rowIndex = -1;
                }
            };

            top.addActionListener(topaction);

            Action tagSearchTextAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setHighlightWords(tagTextField.getText());
                    queryFilteredData();
                }
            };

            Action msgSearchTextAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    //queryParams.setHighlightTxt(msgTextField.getText());
                    setHighlightWords(msgTextField.getText());
                    queryFilteredData();
                    setDeleteTree(false);
                }
            };

            tagTextField.addActionListener(tagSearchTextAction);
            msgTextField.addActionListener(msgSearchTextAction);
            search.addActionListener(msgSearchTextAction);

            table.getSelectionModel().addListSelectionListener(event -> {
                if (!event.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                    String selectedRow = table.getValueAt(table.getSelectedRow(), 3).toString();
                    textArea.setText(selectedRow);
                }
            });

            tree.addTreeSelectionListener(e -> {

                if (!isDeleteTree()) {
                    TreePath path = e.getPath();

                    if (path != null) {
                        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (treeNode.getUserObject() != null) {
                            String selectNode = treeNode.getUserObject().toString();

                            //For an individual file
                            if (selectNode.startsWith("Jtree/")) {
                                selectNode = selectNode.replaceFirst(Pattern.quote("JTree/"), "");
                            }

                            File dir = new File(selectNode);

                            if (!dir.isDirectory()) {

                                String parentNode = treeNode.getParent().toString();

                                String filepath = selectNode;

                                //For an individual file, parent node would be blank
                                if (StringUtils.isNotBlank(parentNode)) {
                                    filepath = parentNode.concat("/").concat(selectNode);
                                }

                                queryParams.setFilename(filepath);

                            } else {
                                queryParams.setFilename(selectNode + "%");
                            }

                            queryFilteredData();

                            setHighlightWords(msgTextField.getText());
                        }
                    }
                }
            });

            searchtree.addTreeSelectionListener(e -> {

                if (!isDeleteTree()) {

                    TreePath path = e.getPath();

                    if (path != null) {
                        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (treeNode.getUserObject() != null) {
                            String selectNode = treeNode.getUserObject().toString();

                            int index = selectNode.indexOf("(");
                            if (index >= 0) {
                                selectNode = selectNode.substring(0, selectNode.indexOf("("));
                            }

                            //queryParams.setHighlightTxt(searchFiles.getText());
                            String searchText = filterSearchInAllFiles.getText();
                            setHighlightWords(searchText);

                            System.out.println("search selectNode : " + selectNode);
                            queryParams.setFilename(selectNode);

                            queryFilteredData(searchText, selectNode);
                        }
                    }
                }
            });

            Action filterTxtSearchInFilesAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    String searchText = filterSearchInAllFiles.getText();
                    setHighlightWords(searchText);
                    //queryParams.setHighlightTxt(searchText);

                    final JDialog jDialog = getJDialog("Searching...");
                    new SwingWorker<Boolean, String>() {
                        @Override
                        protected Boolean doInBackground() throws Exception {

                            List<Row> files = sparkProcessor.getFilteredByFileName(searchText);

                            DefaultTreeModel tmodel = (DefaultTreeModel) searchtree.getModel();
                            DefaultMutableTreeNode root = (DefaultMutableTreeNode) tmodel.getRoot();
                            root.removeAllChildren();

                            files.forEach(fil ->
                                    root.add(TreeFrame.addNodes(null,
                                            new File(fil.getString(0) + "(" + fil.getLong(1) + ")")))
                            );

                            tmodel.reload(root);

                            java.util.List<String[]> allLines = sparkProcessor.getAllRowsFilteredByFileName(searchText);

                            dtm.setRowCount(0);

                            allLines.stream().forEach(row -> {
                                dtm.addRow(row);
                            });

                            return true;
                        }

                        @Override
                        protected void done() {
                            jDialog.dispose();
                        }
                    }.execute();
                }
            };

            Action searchTextInFilesAction = new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent arg0) {

                    textForSearch = searchInAllFiles.getText();

                    setHighlightWords(textForSearch);

                    if (!StringUtils.equals(queryParams.getSearchTxtInFiles(), textForSearch)) {
                        queryParams.setSearchTxtInFiles(textForSearch);
                        queryParams.setInitialIndex(tableSize);
                        rowIndex = -1;
                    }

                    //Start all over again
                    int tempRow = rowIndex;

                    scrollViewSearchText();

                    if (rowIndex == tempRow) { //Index didn't change, means there is no value for this search
                        java.util.List<String[]> allLines = new ArrayList<>();
                        final JDialog jDialog = getJDialog("Searching...");
                        new SwingWorker<Boolean, String>() {
                            @Override
                            protected Boolean doInBackground() throws Exception {
                                List<String[]> results = sparkProcessor.searchAllFiles(textForSearch);
                                allLines.addAll(results);

                                return true;
                            }

                            @Override
                            protected void done() {
                                jDialog.dispose();
                            }
                        }.execute();

                        jDialog.setVisible(true);

                        if (CollectionUtils.isNotEmpty(allLines)) {
                            allLines.stream().forEach(row -> {
                                dtm.addRow(row);
                            });

                            scrollViewSearchText();
                        } else {
                            rowIndex = -1;
                            Toolkit.getDefaultToolkit().beep();
                        }
                    }
                    table.repaint();
                }
            };

            filterSearchInAllFiles.addActionListener(filterTxtSearchInFilesAction);
            searchInAllFiles.addActionListener(searchTextInFilesAction);

            inputpanel.add(bin);
            inputpanel.add(getSeparator());
            inputpanel.add(open);
            inputpanel.add(getSeparator());
            inputpanel.add(search);
            inputpanel.add(getSeparator());
            inputpanel.add(top);
            inputpanel.add(getSeparator());
            inputpanel.add(searchInAllFiles);
            inputpanel.add(getSeparator());
            inputpanel.add(rowCountLabel);

            allui.add(inputpanel);

            /*JSplitPane searchsplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                    searchInAllFiles, listScrollPane);
            JSplitPane splitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                    filterSearchInAllFiles, searchsplitPane);
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    searchsplitPane, scroller);*/

            JSplitPane searchsplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                    listScrollPane, searchlistScrollPane);
            JSplitPane splitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                    filterSearchInAllFiles, searchsplitPane);
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    splitPane1, scroller);

            splitPane.setOneTouchExpandable(true);
            splitPane.setDividerLocation(150);

            panel.setPreferredSize(new Dimension(1140, 680));
            panel.add(allui, BorderLayout.NORTH);
            panel.add(splitPane, BorderLayout.CENTER);
            panel.add(rowDiscripPanel, BorderLayout.SOUTH);

            getContentPane().add(BorderLayout.CENTER, panel);
            pack();
            setLocationByPlatform(true);
            setLocationRelativeTo(null);

            setVisible(true);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            tagTextField.requestFocus();
        });
    }

    public void scrollViewSearchText() {
        for (int row = rowIndex + 1; row < table.getRowCount(); row++) { //Start checking all rows
            Component cc = table.prepareRenderer(table.getCellRenderer(row, 3), row, 3);
            if (cc instanceof CellHighlightRenderer) {
                CellHighlightRenderer textField = (CellHighlightRenderer) cc; //Grab the text of the renderer
                if (textField.getText().contains(textForSearch)) { //We got a match!
                    //textForSearch = globalSearchField.getText();
                    rowIndex = row;
                    int selRow = table.getSelectedRow();

                    scrollToCenter(table, row, 3);
                                /*t.getSelectionModel().setSelectionInterval(row, row); //Scroll to the value
                                t.scrollRectToVisible(new Rectangle(t.getCellRect(row, 0, true)));*/
                    if (selRow != -1) //Something was selected
                        table.setRowSelectionInterval(selRow, selRow); //Restore selection
                    break;
                }
            }
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

    /*private TableCellRenderer getRenderer() {
        return new TableCellRenderer() {
            JTextField f = new JTextField();

            @Override
            public Component getTableCellRendererComponent(JTable arg0, Object arg1, boolean arg2, boolean arg3, int arg4, int arg5) {
                if (arg1 != null) {
                    f.setText(arg1.toString());
                    String string = arg1.toString();
                    if (string.contains(textForSearch)) {
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

    private void setHighlightWords(String text) {
        //String text = msgTextField.getText();
        HashMap<String, Color> words = queryParams.getHighlightWords();
        words.clear();
        if (text.trim().length() > 0) {
            String[] splitPattern = text.split(","); //Split the words
            for (String s : splitPattern) {
                words.put(s.trim(), getARandomColor()); //Put each word with a RANDOM color.
            }
        }
    }

    /*To get light colors combination*/
    private Color getARandomColor() {
        Random rand = new Random();
        return new Color((float) (rand.nextFloat() / 2f + 0.5),
                (float) (rand.nextFloat() / 2f + 0.5),
                (float) (rand.nextFloat() / 2f + 0.5));
    }

    private void queryFilteredData(String text, String selectNode) {
        java.util.List<String[]> allLines = sparkProcessor.getFilteredData(text, selectNode);

        dtm.setRowCount(0);

        allLines.stream().forEach(row -> {
            dtm.addRow(row);
        });
    }

    private void clearFilters() {
        if (sevIndexValues.getSelectedItem() != null) {
            EditableHeaderTableColumn sevcol;
            sevcol = (EditableHeaderTableColumn) table.getColumnModel().getColumn(1);
            sevcol.setHeaderValue(sevIndexValues.getItemAt(0));
        }
        String empty = StringUtils.EMPTY;
        tagTextField.setText(empty);
        msgTextField.setText(empty);
        searchInAllFiles.setText(empty);
        filterSearchInAllFiles.setText(empty);
    }

    private void queryFilteredData() {

        if (dtm.getRowCount() > 0) {
            queryParams.setSev(sevIndexValues.getSelectedItem().toString());
            queryParams.setTag(tagTextField.getText());
            queryParams.setMessage(msgTextField.getText());

            java.util.List<String[]> allLines = sparkProcessor.getFilteredData();

            dtm.setRowCount(0);

            allLines.stream().forEach(row -> {
                dtm.addRow(row);
            });

            rowIndex = -1;
            queryParams.setInitialIndex(1);
        }
    }

    private JSeparator getSeparator() {
        return new JSeparator(SwingConstants.VERTICAL);
    }

    public List<File> validateInputSource(File file) throws IOException {

        final List<File> files = new ArrayList<>();

        if (file.isDirectory()) {
            getFilesFromFolder(file.getPath(), files);
        } else {

            String fileType = new Tika().detect(file);

            if (fileType.contains("bzip2")) {

                JDialog zipPross = getJDialog("Decompressing zip file...");

                new SwingWorker<Boolean, String>() {
                    @Override
                    protected Boolean doInBackground() throws Exception {

                        ZipFileReader zipReader = ZipFileReader.getInstance();
                        files.addAll(zipReader.zipParser(file.getPath()));

                        return true;
                    }

                    @Override
                    protected void done() {
                        super.done();
                        zipPross.dispose();
                    }
                }.execute();

                zipPross.setVisible(true);

            } else if (fileType.contains("text")) {
                files.add(file);
            } else {
                showErrorMessage("File type not supported.");
                return null;
            }
        }

        if (files.isEmpty()) {
            showErrorMessage("No logs to process.");
            return null;
        }

        return files;
    }

    private void showErrorMessage(String msg) {
        int error = JOptionPane.ERROR_MESSAGE;
        JOptionPane.showMessageDialog(this, msg, "Wrong Source", error);
    }


    public void getFilesFromFolder(String directoryName, List<File> files) {
        File directory = new File(directoryName);

        // get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile() && file.getName().startsWith("logcat")) {
                files.add(file);
            } else if (file.isDirectory()) {
                getFilesFromFolder(file.getAbsolutePath(), files);
            }
        }
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
        JDialog jDialog = new JDialog(this);
        JPanel p1 = new JPanel(new FlowLayout());
        p1.add(new JLabel(msg));
        jDialog.setSize(200, 50);
        jDialog.setUndecorated(true);
        jDialog.getContentPane().add(p1);
        jDialog.pack();
        jDialog.setLocationRelativeTo(this);
        jDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        jDialog.setModal(true);

        return jDialog;
    }

    private void cleanUpUnZipFiles() {

        CleanUpFiles cleanUpFiles = CleanUpFiles.getInstance();

        if (cleanUpFiles.getFolderName() != null) {
            FileUtils.deleteQuietly(new File(cleanUpFiles.getFolderName()));
        }
        if (cleanUpFiles.getZipName() != null) {
            FileUtils.deleteQuietly(new File(cleanUpFiles.getZipName()));
        }
    }

    public boolean isDeleteTree() {
        return isDeleteTree;
    }

    public void setDeleteTree(boolean deleteTree) {
        isDeleteTree = deleteTree;
    }

    private class MyTextFieldRenderer extends JTextField implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            String string = String.valueOf(value);

            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());

            setFont(table.getFont());
            setValue(value);

            this.setText(string);

            if (row == rowIndex) { //Check if the row of the found text matches this value

                int indexOf = getText().indexOf(textForSearch);
                try {
                    getHighlighter().addHighlight(indexOf, indexOf + textForSearch.length(),
                            new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW));
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
            //this.setBorder(BorderFactory.createEmptyBorder());
            return this;
        }

        protected void setValue(Object value) {
            setText((value == null) ? "" : value.toString());
        }
    }
}

