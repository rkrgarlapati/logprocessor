package com.log.table;

import com.log.spark.QueryParams;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.JXDatePicker;

import javax.swing.*;

public class HeaderEditor {

    private QueryParams queryParams = QueryParams.getInstance();

    public HeaderEditor() {
    }

    public Object showEditor(String currentValue) {

        JXDatePicker fromDate = getDateFormat();
        JXDatePicker toDate = getDateFormat();
        String selectDate = currentValue;

        final JComponent[] inputs = new JComponent[]{
                new JLabel("From Date:"),
                fromDate,
                new JLabel("To Date:"),
                toDate
        };

        int result = JOptionPane.showConfirmDialog(null, inputs, "Select date range", JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {

            JFormattedTextField feditor = fromDate.getEditor();
            JFormattedTextField teditor = toDate.getEditor();

            String fromDateStr = feditor.getText();
            String toDateStr = teditor.getText();

            queryParams.setFromDateStr(fromDateStr);
            queryParams.setToDateStr(toDateStr);

            if (StringUtils.isNotBlank(fromDateStr) && StringUtils.isNotBlank(toDateStr)) {
                selectDate = feditor.getText() + " To " + teditor.getText();
            } else {
                selectDate = StringUtils.EMPTY;
            }
        }

        return selectDate;
    }

    public JXDatePicker getDateFormat() {
        JXDatePicker picker = new JXDatePicker();
        picker.setFormats("MM-dd HH:mm:ss");

        return picker;
    }
}
