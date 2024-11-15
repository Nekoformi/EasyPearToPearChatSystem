package Source.Utils;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class Catalog {
    private JList<String> list;
    private JButton[] button;

    public Catalog() {}

    public Catalog(JList<String> list, JButton[] button) {
        setList(list);
        setButton(button);
    }

    public void setList(JList<String> list) {
        this.list = list;
    }

    public void setButton(JButton[] button) {
        this.button = button;
    }

    public int getSelectedIndex() {
        return list.getSelectedIndex();
    }

    public Object getSelectedValue() {
        return list.getSelectedValue();
    }

    public void setListData(String[] listData) {
        if (listData != null)
            list.setListData(listData);
    }

    public void setButtonLabel(int index, String text) {
        if (button == null || button.length < index + 1 || button[index] == null)
            return;

        JButton targetButton = button[index];

        if (text != null) {
            targetButton.setEnabled(true);

            targetButton.setText(text);
        } else {
            targetButton.setEnabled(false);

            targetButton.setText("");
        }
    }

    public void setSelectEvent(ListSelectionListener listSelectionListener) {
        if (list == null)
            return;

        for (ListSelectionListener listener : list.getListSelectionListeners())
            list.removeListSelectionListener(listener);

        if (listSelectionListener != null)
            list.addListSelectionListener(listSelectionListener);
    }

    public void setActionEvent(int index, ActionListener actionListener) {
        if (button == null || button.length < index + 1 || button[index] == null)
            return;

        JButton targetButton = button[index];

        for (ActionListener listener : targetButton.getActionListeners())
            targetButton.removeActionListener(listener);

        if (actionListener != null)
            targetButton.addActionListener(actionListener);
    }
}
