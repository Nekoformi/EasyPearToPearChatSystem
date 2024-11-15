package Source.Utils;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

public class Console {
    private StringBuffer stringBuffer = new StringBuffer();
    private JScrollPane textAreaScrollPane;
    private JTextArea textArea;
    private int textAreaLastScrollFill = 0;

    private DefaultStyledDocument document = new DefaultStyledDocument();
    private JScrollPane textPaneScrollPane;
    private JTextPane textPane;
    private int textPaneLastScrollFill = 0;

    public static int GAP = 5;
    public static boolean DEBUG_LOG = false;
    public static boolean DEBUG_LOG_COLOR = false;

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

    private void updateTextArea() {
        if (textArea == null)
            return;

        textArea.setText(stringBuffer.toString());

        if (textAreaScrollPane != null)
            textAreaLastScrollFill = updateScrollPane(textAreaScrollPane, textArea, textAreaLastScrollFill);
    }

    private void updateTextPane() {
        if (textPane == null)
            return;

        textPane.setDocument(document);

        if (textPaneScrollPane != null)
            textPaneLastScrollFill = updateScrollPane(textPaneScrollPane, textPane, textPaneLastScrollFill);
    }

    private void clearTextArea() {
        if (textArea == null)
            return;

        textArea.setText(stringBuffer.toString());

        if (textAreaScrollPane != null)
            textAreaLastScrollFill = resetScrollPane(textAreaScrollPane, textArea);
    }

    private void clearTextPane() {
        if (textPane == null)
            return;

        textPane.setDocument(document);

        if (textPaneScrollPane != null)
            textPaneLastScrollFill = resetScrollPane(textPaneScrollPane, textPane);
    }

    private int updateScrollPane(JScrollPane scrollPane, JTextComponent textComponent, int lastScrollFill) {
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

    private int resetScrollPane(JScrollPane scrollPane, JTextComponent textComponent) {
        JScrollBar scrollBar = scrollPane.getVerticalScrollBar();

        textComponent.setCaretPosition(0);
        scrollBar.setValue(0);

        return 0;
    }

    private void pushLineToTextArea(String text) {
        stringBuffer.append((stringBuffer.length() == 0 ? "" : "\n") + text);

        updateTextArea();
    }

    private void pushLineToTextPane(String text, Color color) {
        try {
            document.insertString(document.getLength(), (document.getLength() == 0 ? "" : "\n") + text, createColorTextAttribute(color));
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        updateTextPane();
    }

    private void pushLineToConsole(String text, String terminalLabel, String terminalColor) {
        if (DEBUG_LOG_COLOR) {
            terminalLabel = terminalColor + terminalLabel + Util.TERMINAL_END;
            text = terminalColor + text + Util.TERMINAL_END;
        }

        System.out.println(getCurrentTimeDisplay() + " [" + terminalLabel + "] " + text);
    }

    private SimpleAttributeSet createColorTextAttribute(Color color) {
        SimpleAttributeSet attribute = new SimpleAttributeSet();

        attribute.addAttribute(StyleConstants.Foreground, color);

        return attribute;
    }

    private SimpleAttributeSet createHyperLinkAttribute(String url, Color color) {
        SimpleAttributeSet attribute = new SimpleAttributeSet();

        attribute.addAttribute(HTML.Attribute.HREF, url);
        attribute.addAttribute(StyleConstants.Underline, true);
        attribute.addAttribute(StyleConstants.Foreground, color);

        return attribute;
    }

    private void clearAllLineToTextArea() {
        stringBuffer.setLength(0);

        clearTextArea();
    }

    private void clearAllLineToTextPane() {
        document = new DefaultStyledDocument();

        clearTextPane();
    }

    public synchronized void pushMainLine(String text) {
        if (textArea != null)
            SwingUtilities.invokeLater(() -> pushLineToTextArea(text));

        if (textPane != null)
            SwingUtilities.invokeLater(() -> pushLineToTextPane(text, new Color(128, 255, 128)));

        if (DEBUG_LOG)
            pushLineToConsole(text, "MSG", Util.TERMINAL_GREEN);
    }

    public synchronized void pushMainLine(String text, String[] commandLabel, String[] commandContent) {
        pushActionLine(text, new Color(128, 255, 128), "MSG", Util.TERMINAL_GREEN, commandLabel, commandContent);
    }

    public synchronized void pushSubLine(String text) {
        if (textArea != null)
            SwingUtilities.invokeLater(() -> pushLineToTextArea(text));

        if (textPane != null)
            SwingUtilities.invokeLater(() -> pushLineToTextPane(text, new Color(128, 128, 128)));

        if (DEBUG_LOG)
            pushLineToConsole(text, "SYS", Util.TERMINAL_GRAY);
    }

    public synchronized void pushWarningLine(String text) {
        if (textArea != null)
            SwingUtilities.invokeLater(() -> pushLineToTextArea(text));

        if (textPane != null)
            SwingUtilities.invokeLater(() -> pushLineToTextPane(text, new Color(255, 255, 128)));

        if (DEBUG_LOG)
            pushLineToConsole(text, "WNG", Util.TERMINAL_YELLOW);
    }

    public synchronized void pushErrorLine(String text) {
        if (textArea != null)
            SwingUtilities.invokeLater(() -> pushLineToTextArea(text));

        if (textPane != null)
            SwingUtilities.invokeLater(() -> pushLineToTextPane(text, new Color(255, 128, 128)));

        if (DEBUG_LOG)
            pushLineToConsole(text, "ERR", Util.TERMINAL_RED);
    }

    public synchronized void pushActionLine(String text, Color color, String terminalLabel, String terminalColor, String[] commandLabel,
            String[] commandContent) {
        String[] textArray = text.split("\\$\\$\\$", -1);
        StringBuffer textPlain = new StringBuffer(textArray[0]);

        if (commandLabel.length != commandContent.length || commandLabel.length != textArray.length - 1)
            return;

        for (int i = 0; i < commandLabel.length; i++)
            textPlain.append("[" + commandLabel[i] + "](" + commandContent[i] + ")" + textArray[i + 1]);

        if (textArea != null)
            SwingUtilities.invokeLater(() -> pushLineToTextArea(textPlain.toString()));

        if (textPane != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    SimpleAttributeSet attribute = createColorTextAttribute(color);

                    document.insertString(document.getLength(), (document.getLength() == 0 ? "" : "\n") + textArray[0], attribute);

                    for (int i = 0; i < commandLabel.length; i++) {
                        document.insertString(document.getLength(), commandLabel[i], createHyperLinkAttribute(commandContent[i], color));
                        document.insertString(document.getLength(), textArray[i + 1], attribute);
                    }
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }

                updateTextPane();
            });
        }

        if (DEBUG_LOG)
            pushLineToConsole(textPlain.toString(), terminalLabel, terminalColor);
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
