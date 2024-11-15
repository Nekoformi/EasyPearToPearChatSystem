package Source;

import Source.Utils.Util;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

public class Interface extends JFrame {
    private ClassLoader classLoader = this.getClass().getClassLoader();

    private Client client;

    private int frameX = 0;
    private int frameY = 0;
    private int frameW = 640;
    private int frameH = 480;
    private boolean setCenterPosition = false;
    private boolean setMaximizeWindow = false;

    private Display logDisplay;
    private Display chatDisplay;
    private Command chatCommand;
    private List memberList;
    private Action memberAction;

    public Interface(Client client) {
        this.client = client;

        setFrame();
    }

    public Interface(Client client, int x, int y, int w, int h, boolean c, boolean m) {
        this.client = client;

        frameX = x;
        frameY = y;
        frameW = w;
        frameH = h;
        setCenterPosition = c;
        setMaximizeWindow = m;

        setFrame();
    }

    private void setFrame() {
        JPanel log = log();
        JPanel chat = chat();
        JPanel member = member();

        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        JPanel panel = new JPanel();

        panel.setLayout(gridBagLayout);

        setGridBagConstraints(gridBagConstraints, 0, 0, 1, 1, 0.66d, 1.0d, 4);

        panel.add(log, gridBagConstraints);

        setGridBagConstraints(gridBagConstraints, 0, 1, 1, 1, 0.66d, 1.0d, 4);

        panel.add(chat, gridBagConstraints);

        setGridBagConstraints(gridBagConstraints, 1, 0, 1, 2, 0.33d, 1.0d, 4);

        panel.add(member, gridBagConstraints);

        setTitle("Easy Pear to Pear Chat System " + Main.VERSION);

        setSize(frameW, frameH);
        setLocation(frameX, frameY);

        if (setCenterPosition)
            setLocationRelativeTo(null);

        if (setMaximizeWindow)
            setExtendedState(JFrame.MAXIMIZED_BOTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(new ImageIcon(classLoader.getResource("Source/Assets/Icon.png")).getImage());

        add(panel, BorderLayout.CENTER);
        setVisible(true);
    }

    private static void setGridBagConstraints(GridBagConstraints c, int gX, int gY, int gW, int gH, double wX, double wY, int margin) {
        c.gridx = gX;
        c.gridy = gY;
        c.gridwidth = gW;
        c.gridheight = gH;
        c.weightx = wX;
        c.weighty = wY;
        c.insets = new Insets(margin, margin, margin, margin);
        c.fill = GridBagConstraints.BOTH;
    }

    private class Display {
        private JPanel panel;
        private JLabel label;
        private JScrollPane scrollPane;
        private JTextPane textPane;
        private JTextArea textArea;

        public Display(String title, boolean useTextPane) {
            label = new JLabel(title, JLabel.LEFT);

            label.setPreferredSize(new Dimension(Short.MAX_VALUE, label.getMinimumSize().height));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);

            if (useTextPane) {
                textPane = new JTextPane();

                textPane.setEditable(false);

                scrollPane = new JScrollPane(textPane);
            } else {
                textArea = new JTextArea();

                textArea.setEditable(false);
                textArea.setLineWrap(true);

                scrollPane = new JScrollPane(textArea);
            }

            scrollPane.setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
            scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            panel = new JPanel();

            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(label);
            panel.add(scrollPane);
        }
    }

    private class Command {
        private JPanel panel;
        private JTextField textField;
        private JButton button;

        public Command() {
            textField = new JTextField();

            textField.setAlignmentY(Component.CENTER_ALIGNMENT);

            button = new JButton("Run");

            button.setAlignmentY(Component.CENTER_ALIGNMENT);

            int height = Util.getMaximum(textField.getMinimumSize().height, button.getMinimumSize().height);

            textField.setPreferredSize(new Dimension(Short.MAX_VALUE, height));
            button.setPreferredSize(new Dimension(button.getMinimumSize().width, height));

            panel = new JPanel();

            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            panel.add(textField);
            panel.add(button);

            textField.setTransferHandler(new DropFileHandler(textField));
        }

        void clear() {
            textField.setText("");
        }

        String get() {
            return textField.getText();
        }

        void set(String text) {
            textField.setText(text);
        }

        void insert(String text) {
            Util.pasteText(textField, text);
        }

        String pop() {
            String res = get();

            clear();

            return res;
        }
    }

    private class List {
        private JPanel panel;
        private JLabel label;
        private JScrollPane scrollPane;
        private JList<String> list;

        public List(String title) {
            label = new JLabel(title, JLabel.LEFT);

            label.setPreferredSize(new Dimension(Short.MAX_VALUE, label.getMinimumSize().height));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);

            list = new JList<String>();

            scrollPane = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            scrollPane.setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
            scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

            panel = new JPanel();

            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(label);
            panel.add(scrollPane);
        }
    }

    private class Action {
        private JPanel panel;
        private JButton[] button;

        public Action(String... buttonLabel) {
            panel = new JPanel();
            button = new JButton[buttonLabel.length];

            panel.setLayout(new GridLayout(1, button.length));

            for (int i = 0; i < button.length; i++) {
                button[i] = setButton(buttonLabel[i]);

                panel.add(button[i]);
            }
        }

        JButton setButton(String label) {
            JButton button = new JButton(label);

            button.setMinimumSize(new Dimension(0, button.getMinimumSize().height));
            button.setMaximumSize(new Dimension(0, button.getMinimumSize().height));
            button.setPreferredSize(new Dimension(0, button.getMinimumSize().height));

            Insets margin = button.getMargin();

            button.setMargin(new Insets(margin.top, 4, margin.bottom, 4));

            return button;
        }
    }

    private JPanel log() {
        logDisplay = new Display("Log", true);

        client.systemConsole.setTextPaneScrollPane(logDisplay.scrollPane);
        client.systemConsole.setTextPane(logDisplay.textPane);

        return logDisplay.panel;
    }

    private JPanel chat() {
        chatDisplay = new Display("Chat", true);

        client.chatConsole.setTextPaneScrollPane(chatDisplay.scrollPane);
        client.chatConsole.setTextPane(chatDisplay.textPane);

        chatDisplay.panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        chatCommand = new Command();

        chatCommand.panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        ActionListener runCommand = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                client.executeCommand(chatCommand.pop());
            }
        };

        chatCommand.textField.addActionListener(runCommand);
        chatCommand.button.addActionListener(runCommand);

        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(chatDisplay.panel);
        panel.add(chatCommand.panel);

        chatDisplay.textPane.addMouseListener(new HyperLinkController());

        return panel;
    }

    private JPanel member() {
        memberList = new List("Member");

        memberList.panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        client.memberCatalog.setList(memberList.list);

        memberAction = new Action("-", "-");

        client.memberCatalog.setButton(memberAction.button);
        client.memberCatalog.setButtonLabel(0, null);
        client.memberCatalog.setButtonLabel(1, null);

        memberAction.panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(memberList.panel);
        panel.add(memberAction.panel);

        return panel;
    }

    private class DropFileHandler extends TransferHandler {
        private Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        public DropFileHandler(JTextComponent component) {
            super();

            setKeyAction(component);
        }

        private void setKeyAction(JTextComponent component) {
            AbstractAction cutText = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    StringSelection stringSelection = new StringSelection(Util.cutText(component));

                    clipboard.setContents(stringSelection, stringSelection);
                }
            };

            AbstractAction copyText = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    StringSelection stringSelection = new StringSelection(Util.copyText(component));

                    clipboard.setContents(stringSelection, stringSelection);
                }
            };

            component.getInputMap().put(KeyStroke.getKeyStroke("ctrl X"), "cutText");
            component.getActionMap().put("cutText", cutText);

            component.getInputMap().put(KeyStroke.getKeyStroke("ctrl C"), "copyText");
            component.getActionMap().put("copyText", copyText);
        }

        @Override
        public boolean canImport(TransferSupport transferSupport) {
            if (!transferSupport.isDrop()) {
                // client.systemConsole.pushErrorLine("Invalid operation (D&D).");

                // return false;
            }

            if (!transferSupport.isDataFlavorSupported(DataFlavor.stringFlavor) && !transferSupport.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                client.systemConsole.pushErrorLine("You can't D&D anything other than file(s) and text.");

                return false;
            }

            return true;
        }

        @Override
        public boolean importData(TransferSupport transferSupport) {
            if (!canImport(transferSupport))
                return false;

            Transferable transferable = transferSupport.getTransferable();

            try {
                if (transferSupport.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    @SuppressWarnings("unchecked")
                    java.util.List<File> rec = (java.util.List<File>)transferable.getTransferData(DataFlavor.javaFileListFlavor);

                    chatCommand.insert(rec.get(0).getPath());
                } else if (transferSupport.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    String rec = transferable.getTransferData(DataFlavor.stringFlavor).toString();

                    chatCommand.insert(rec);
                }
            } catch (UnsupportedFlavorException | IOException e) {
                client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to load the file(s) or text."));
            }

            return true;
        }
    }

    private class HyperLinkController extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            JTextPane textPane = (JTextPane)e.getSource();
            int pos = textPane.viewToModel2D(new Point(e.getX(), e.getY()));

            if (pos >= 0) {
                Document document = textPane.getDocument();

                if (document instanceof DefaultStyledDocument) {
                    DefaultStyledDocument defaultStyledDocument = (DefaultStyledDocument)document;
                    Element element = defaultStyledDocument.getCharacterElement(pos);
                    AttributeSet attribute = element.getAttributes();
                    String href = (String)attribute.getAttribute(HTML.Attribute.HREF);

                    if (href != null)
                        chatCommand.set(href);
                }
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            JTextPane textPane = (JTextPane)e.getSource();
            int pos = textPane.viewToModel2D(new Point(e.getX(), e.getY()));

            if (pos >= 0) {
                Document document = textPane.getDocument();

                if (document instanceof DefaultStyledDocument) {
                    DefaultStyledDocument defaultStyledDocument = (DefaultStyledDocument)document;
                    Element element = defaultStyledDocument.getCharacterElement(pos);
                    AttributeSet attribute = element.getAttributes();
                    String href = (String)attribute.getAttribute(HTML.Attribute.HREF);

                    Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
                    Cursor defaultCursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);

                    if (href != null) {
                        if (getCursor() != handCursor)
                            textPane.setCursor(handCursor);
                    } else {
                        if (getCursor() != defaultCursor)
                            textPane.setCursor(defaultCursor);
                    }
                }
            }
        }
    }
}
