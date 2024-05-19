package Source.Utils;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;

public class Console {
    StringBuffer stringBuffer = new StringBuffer();
    JScrollPane textAreaScrollPane;
    JTextArea textArea;
    int textAreaLastScrollFill = 0;

    DefaultStyledDocument document = new DefaultStyledDocument();
    JScrollPane textPaneScrollPane;
    JTextPane textPane;
    int textPaneLastScrollFill = 0;

    public static int GAP = 5;
    public static boolean DEBUG_LOG = false;

    public Console() {}

    public Console(JScrollPane scrollPane, JTextArea textArea) {
        setTextAreaScrollPane(scrollPane);
        setTextArea(textArea);
    }

    public Console(JScrollPane scrollPane, JTextPane textPane) {
        setTextPaneScrollPane(scrollPane);
        setTextPane(textPane);
    }

    public void setTextArea(JTextArea textArea) {
        this.textArea = textArea;

        updateTextArea();
    }

    public void setTextPane(JTextPane textPane) {
        this.textPane = textPane;

        updateTextPane();
    }

    public void setTextAreaScrollPane(JScrollPane scrollPane) {
        textAreaScrollPane = scrollPane;
    }

    public void setTextPaneScrollPane(JScrollPane scrollPane) {
        textPaneScrollPane = scrollPane;
    }

    void updateTextArea() {
        if (textArea == null)
            return;

        textArea.setText(stringBuffer.toString());

        if (textAreaScrollPane != null)
            textAreaLastScrollFill = updateScrollPane(textAreaScrollPane, textArea, textAreaLastScrollFill);
    }

    void updateTextPane() {
        if (textPane == null)
            return;

        textPane.setDocument(document);

        if (textPaneScrollPane != null)
            textPaneLastScrollFill = updateScrollPane(textPaneScrollPane, textPane, textPaneLastScrollFill);
    }

    void clearTextArea() {
        if (textArea == null)
            return;

        textArea.setText(stringBuffer.toString());

        if (textAreaScrollPane != null)
            textAreaLastScrollFill = resetScrollPane(textAreaScrollPane, textArea);
    }

    void clearTextPane() {
        if (textPane == null)
            return;

        textPane.setDocument(document);

        if (textPaneScrollPane != null)
            textPaneLastScrollFill = resetScrollPane(textPaneScrollPane, textPane);
    }

    int updateScrollPane(JScrollPane scrollPane, JTextComponent textComponent, int lastScrollFill) {
        JScrollBar scrollBar = scrollPane.getVerticalScrollBar();

        int scrollValue = scrollBar.getValue();
        int scrollHeight = scrollBar.getMaximum() - scrollBar.getHeight();
        int scrollFill = Util.getMinimum(scrollHeight, lastScrollFill);

        if (scrollValue >= scrollFill - GAP) {
            lastScrollFill = scrollHeight;

            textComponent.setCaretPosition(textComponent.getDocument().getLength());
        } else {
            scrollBar.setValue(scrollValue); // SwingUtilities.invokeLater(() -> scrollBar.setValue(scrollValue));
        }

        return lastScrollFill;
    }

    int resetScrollPane(JScrollPane scrollPane, JTextComponent textComponent) {
        JScrollBar scrollBar = scrollPane.getVerticalScrollBar();

        textComponent.setCaretPosition(0);
        scrollBar.setValue(0);

        return 0;
    }

    void pushLineToTextArea(String text) {
        stringBuffer.append((stringBuffer.length() == 0 ? "" : "\n") + text);

        updateTextArea();
    }

    void pushLineToTextPane(String text, Color color) {
        SimpleAttributeSet attribute = new SimpleAttributeSet();

        attribute.addAttribute(StyleConstants.Foreground, color);

        try {
            document.insertString(document.getLength(), (document.getLength() == 0 ? "" : "\n") + text, attribute);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        updateTextPane();
    }

    void clearAllLineToTextArea() {
        stringBuffer.setLength(0);

        clearTextArea();
    }

    void clearAllLineToTextPane() {
        document = new DefaultStyledDocument();

        clearTextPane();
    }

    public synchronized void pushMainLine(String text) {
        if (textArea != null)
            SwingUtilities.invokeLater(() -> pushLineToTextArea(text));

        if (textPane != null)
            SwingUtilities.invokeLater(() -> pushLineToTextPane(text, new Color(128, 255, 128)));

        if (DEBUG_LOG)
            System.out.println(getCurrentTimeDisplay() + " " + Util.TERMINAL_GREEN + text + Util.TERMINAL_END);
    }

    public synchronized void pushSubLine(String text) {
        if (textArea != null)
            SwingUtilities.invokeLater(() -> pushLineToTextArea(text));

        if (textPane != null)
            SwingUtilities.invokeLater(() -> pushLineToTextPane(text, new Color(128, 128, 128)));

        if (DEBUG_LOG)
            System.out.println(getCurrentTimeDisplay() + " " + Util.TERMINAL_GRAY + text + Util.TERMINAL_END);
    }

    public synchronized void pushWarningLine(String text) {
        if (textArea != null)
            SwingUtilities.invokeLater(() -> pushLineToTextArea(text));

        if (textPane != null)
            SwingUtilities.invokeLater(() -> pushLineToTextPane(text, new Color(255, 255, 128)));

        if (DEBUG_LOG)
            System.out.println(getCurrentTimeDisplay() + " " + Util.TERMINAL_YELLOW + text + Util.TERMINAL_END);
    }

    public synchronized void pushErrorLine(String text) {
        if (textArea != null)
            SwingUtilities.invokeLater(() -> pushLineToTextArea(text));

        if (textPane != null)
            SwingUtilities.invokeLater(() -> pushLineToTextPane(text, new Color(255, 128, 128)));

        if (DEBUG_LOG)
            System.err.println(getCurrentTimeDisplay() + " " + Util.TERMINAL_RED + text + Util.TERMINAL_END);
    }

    public synchronized void clearAllLine() {
        if (textArea != null)
            SwingUtilities.invokeLater(() -> clearAllLineToTextArea());

        if (textPane != null)
            SwingUtilities.invokeLater(() -> clearAllLineToTextPane());
    }

    public String getCurrentTimeDisplay() {
        return "[" + Util.getCurrentTimeDisplay() + "]";
    }
}
