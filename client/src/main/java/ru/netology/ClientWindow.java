package ru.netology;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.*;

import static ru.netology.Logger.*;

public class ClientWindow extends JFrame implements ActionListener, ConnectionListener {
    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;
    private final JTextArea logConsole = new JTextArea();
    private static final File logFile = new File("client/src/main/resources/file.log");
    private static final File settingsFile = new File("client/src/main/resources/settingsClient.txt");
    private final JTextField fieldNickname = new JTextField();
    private final JTextField fieldInput = new JTextField();
    private Connection connection;

    public static void main(String[] args) throws IOException {
        createSettingsFile(settingsFile);
        createLogFile(logFile);
        SwingUtilities.invokeLater(ClientWindow::new);
    }

    private ClientWindow() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);

        logConsole.setEditable(false);
        logConsole.setWrapStyleWord(true);
        add(logConsole, BorderLayout.CENTER);

        fieldInput.addActionListener(this);
        add(fieldInput, BorderLayout.SOUTH);
        add(fieldNickname, BorderLayout.NORTH);

        setVisible(true);
        String ip;
        int port;
        try {
            String[] array = getIPAddressAndPort().split(" ");
            ip = array[0];
            port = Integer.parseInt(array[1]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            connection = new Connection(this, ip, port);
        } catch (IOException e) {
            logMsg(loggingInFile(logFile, CONNECTION_EXCEPTION + e));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String msg = fieldInput.getText();
        if (msg.equals("")) {
            return;
        } else if (msg.equals("/exit")) {
            onDisconnect(connection);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            ClientWindow.this.dispatchEvent(new WindowEvent(ClientWindow.this, WindowEvent.WINDOW_CLOSING));
        }
        fieldInput.setText(null);
        connection.sendMsg(fieldNickname.getText() + ": " + msg);
    }

    private String getIPAddressAndPort() throws IOException {
        try (BufferedReader brPort = new BufferedReader(new FileReader(settingsFile))) {
            String string;
            StringBuilder addressAndPort = new StringBuilder();
            while ((string = brPort.readLine()) != null) {
                int indexSpace = string.indexOf(" ");
                addressAndPort.append(" ").append(string.substring(indexSpace + 1));
            }
            return addressAndPort.toString().trim();
        }
    }

    @Override
    public void onConnectionReady(Connection connection) {
        logMsg(CLIENT_CONNECTED);
        loggingInFile(logFile, CLIENT_CONNECTED);
    }

    @Override
    public void onReceiveString(Connection connection, String msg) {
        logMsg(msg);
        loggingInFile(logFile, msg);
    }

    @Override
    public void onDisconnect(Connection connection) {
        logMsg(CONNECTION_CLOSE);
        loggingInFile(logFile, CONNECTION_CLOSE);
    }

    @Override
    public void onException(Connection connection, Exception e) {
        logMsg(CONNECTION_EXCEPTION + e);
        loggingInFile(logFile, CONNECTION_EXCEPTION + e);
    }

    private synchronized void logMsg(String msg) {
        SwingUtilities.invokeLater(() -> {
            logConsole.append(msg + "\n");
            logConsole.setCaretPosition(logConsole.getDocument().getLength());
        });
    }
}
