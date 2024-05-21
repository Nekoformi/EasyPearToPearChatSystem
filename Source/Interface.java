package Source;

import Source.Utils.Util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Interface extends JFrame {
    Client client;

    int frameX = 0;
    int frameY = 0;
    int frameW = 640;
    int frameH = 480;
    boolean setCenterPosition = false;
    boolean setMaximizeWindow = false;

    Display logDisplay;
    Display chatDisplay;
    Command chatCommand;
    List memberList;
    Action memberAction;

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

    void setFrame() {
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
        setIconImage(new ImageIcon("./Source/Assets/Icon.png").getImage());

        add(panel, BorderLayout.CENTER);
        setVisible(true);
    }

    void setGridBagConstraints(GridBagConstraints c, int gX, int gY, int gW, int gH, double wX, double wY, int margin) {
        c.gridx = gX;
        c.gridy = gY;
        c.gridwidth = gW;
        c.gridheight = gH;
        c.weightx = wX;
        c.weighty = wY;
        c.insets = new Insets(margin, margin, margin, margin);
        c.fill = GridBagConstraints.BOTH;
    }

    class Display {
        public JPanel panel;
        public JLabel label;
        public JScrollPane scrollPane;
        public JTextPane textPane;
        public JTextArea textArea;

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

    class Command {
        public JPanel panel;
        public JTextField textField;
        public JButton button;

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
        }
    }

    class List {
        public JPanel panel;
        public JLabel label;
        public JScrollPane scrollPane;
        public JList<String> list;

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

    class Action {
        public JPanel panel;
        public JButton[] button;

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

    JPanel log() {
        logDisplay = new Display("Log", true);

        client.systemConsole.setTextPaneScrollPane(logDisplay.scrollPane);
        client.systemConsole.setTextPane(logDisplay.textPane);

        return logDisplay.panel;
    }

    JPanel chat() {
        chatDisplay = new Display("Chat", true);

        client.chatConsole.setTextPaneScrollPane(chatDisplay.scrollPane);
        client.chatConsole.setTextPane(chatDisplay.textPane);

        chatDisplay.panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        chatCommand = new Command();

        chatCommand.panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        ActionListener runCommand = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                client.executeCommand(chatCommand.textField.getText());

                chatCommand.textField.setText("");
            }
        };

        chatCommand.textField.addActionListener(runCommand);
        chatCommand.button.addActionListener(runCommand);

        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(chatDisplay.panel);
        panel.add(chatCommand.panel);

        return panel;
    }

    JPanel member() {
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
}
