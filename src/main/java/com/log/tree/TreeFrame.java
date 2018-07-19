package com.log.tree;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collections;
import java.util.Vector;

public class TreeFrame {
    public static void main(String[] args) {

        JFrame frame = createFrame();

        JPanel browsePanel = new JPanel();

        JButton open = new JButton();
        open.setPreferredSize(new Dimension(70, 25));
        open.setText("Open");

        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        JTree tree = new JTree();
        JScrollPane scrollPane = new JScrollPane(tree);

        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {

                TreePath path = e.getPath();

                if (path != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    File dir = new File(node.getUserObject().toString());
                    if (!dir.isDirectory()) {
                        String filepath = node.getParent()+"/"+node.getUserObject().toString();
                        dir = new File(filepath);
                        System.out.println("Selected :" + dir + "  isFile :" + dir.isFile() + " isDirectory" + dir.isDirectory());
                    }
                }
            }
        });


        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                int result = fileChooser.showOpenDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {

                    File file = fileChooser.getSelectedFile();

                    DefaultTreeModel tmodel = (DefaultTreeModel) tree.getModel();
                    DefaultMutableTreeNode root = (DefaultMutableTreeNode) tmodel.getRoot();
                    root.add(TreeFrame.addNodes(null, file));
                    tmodel.reload(root);

                    /*TreeModel model1 = new FileTreeModel(file);
                    tree.setModel(model1);

                    scrollPane.add(tree);*/
                }
            }
        };

        open.addActionListener(action);
        browsePanel.add(open);

        frame.add(browsePanel, BorderLayout.WEST);
        frame.add(scrollPane, BorderLayout.EAST);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static JFrame createFrame() {
        JFrame frame = new JFrame("JTree Expand/Collapse example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(500, 400));
        frame.setLayout(new GridLayout(1, 2));
        return frame;
    }

    /**
     * Add nodes from under "dir" into curTop. Highly recursive.
     */
    public static DefaultMutableTreeNode addNodes(DefaultMutableTreeNode curTop, File dir) {
        DefaultMutableTreeNode curDir = new DefaultMutableTreeNode();
        if (dir != null) {

            //dir.getName()

            String curPath = dir.getPath();
            curDir.setUserObject(curPath);
            if (curTop != null) { // should only be null at root
                curTop.add(curDir);
            }
            Vector ol = new Vector();
            String[] tmp = dir.list();
            if(tmp != null) {
                for (int i = 0; i < tmp.length; i++)
                    ol.addElement(tmp[i]);
            }
            Collections.sort(ol, String.CASE_INSENSITIVE_ORDER);
            File f;
            Vector files = new Vector();
            // Make two passes, one for Dirs and one for Files. This is #1.
            for (int i = 0; i < ol.size(); i++) {
                String thisObject = (String) ol.elementAt(i);
                String newPath;
                if (curPath.equals("."))
                    newPath = thisObject;
                else
                    newPath = curPath + File.separator + thisObject;
                if ((f = new File(newPath)).isDirectory())
                    addNodes(curDir, f);
                else
                    files.addElement(thisObject);
            }
            // Pass two: for files.
            for (int fnum = 0; fnum < files.size(); fnum++) {
                curDir.add(new DefaultMutableTreeNode(files.elementAt(fnum)));
            }
        }
        return curDir;
    }
}