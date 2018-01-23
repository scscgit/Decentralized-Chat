import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChatLayout {
    private JTabbedPane tabbedPane1;
    private JPanel mainPanel;
    private JTextField nodeIpTextField;
    private JTextField nodePortTextField;
    private JButton connectButton;
    private JButton hostButton;
    private JTextField hostPortTextField;

    public ChatLayout() {
        connectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Connect to IP
                tabbedPane1.addTab(nodeIpTextField.getText() + ":" + nodePortTextField.getText(), new JPanel());
            }
        });
        hostButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Host new server
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("ChatLayout");
        frame.setContentPane(new ChatLayout().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
