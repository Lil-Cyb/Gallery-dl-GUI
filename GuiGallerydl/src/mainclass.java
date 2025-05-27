import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class mainclass extends JFrame {

    private JTextField urlField;
    private JTextField downloadDirField;
    private JCheckBox noMtimeCheckBox;
    private JCheckBox noDownloadCheckBox;
    private JTextArea logArea;
    private JButton downloadButton;
    private JButton browseButton;

    public mainclass() {
        // Set up the main window
        setTitle("gallery-dl GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null); // Center the window

        // Set up the main panel with a GridBagLayout for flexible arrangement
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10)); // Add some padding

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Padding for components

        // URL Input
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(new JLabel("Gallery URL:"), gbc);

        urlField = new JTextField(50);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // Span across two columns
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(urlField, gbc);

        // Download Directory Input
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(new JLabel("Download Directory:"), gbc);

        downloadDirField = new JTextField(40);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(downloadDirField, gbc);

        browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> browseDirectory());
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE; // Don't stretch button
        gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(browseButton, gbc);

        // Set default download directory
        String userHome = System.getProperty("user.home");
        File defaultDownloadFolder = Paths.get(userHome, "Downloads", "gallery-dl-downloads").toFile();
        if (!defaultDownloadFolder.exists()) {
            defaultDownloadFolder.mkdirs(); // Create the directory if it doesn't exist
        }
        downloadDirField.setText(defaultDownloadFolder.getAbsolutePath());

        // Options Checkboxes
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Options"));

        noMtimeCheckBox = new JCheckBox("--no-mtime (Don't use original modification time)");
        optionsPanel.add(noMtimeCheckBox);

        noDownloadCheckBox = new JCheckBox("--no-download (Simulate download, don't save files)");
        optionsPanel.add(noDownloadCheckBox);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(optionsPanel, gbc);

        // Download Button
        downloadButton = new JButton("Start Download");
        downloadButton.setFont(new Font("Arial", Font.BOLD, 16));
        downloadButton.setPreferredSize(new Dimension(200, 50)); // Make button larger
        downloadButton.addActionListener(e -> startDownload());
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(downloadButton, gbc);

        // Log Area
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; // Allow horizontal resizing
        gbc.weighty = 1.0; // Allow vertical resizing
        mainPanel.add(new JLabel("Download Log:"), gbc);

        logArea = new JTextArea();
        logArea.setEditable(false); // Make log area read-only
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        mainPanel.add(scrollPane, gbc);

        add(mainPanel); // Add the main panel to the frame
    }

    private void browseDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setCurrentDirectory(new File(downloadDirField.getText())); // Set initial directory

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            downloadDirField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    private void logMessage(String message) {
        // Append message to log area and scroll to bottom
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void setUIEnabled(boolean enabled) {
        urlField.setEnabled(enabled);
        downloadDirField.setEnabled(enabled);
        browseButton.setEnabled(enabled);
        noMtimeCheckBox.setEnabled(enabled);
        noDownloadCheckBox.setEnabled(enabled);
        downloadButton.setEnabled(enabled);
    }

    private void startDownload() {
        String url = urlField.getText().trim();
        String downloadDir = downloadDirField.getText().trim();

        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a URL.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (downloadDir.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a download directory.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Clear log and disable UI
        logArea.setText("");
        setUIEnabled(false);
        logMessage("Starting download for: " + url);
        logMessage("Saving to: " + downloadDir);

        // Execute gallery-dl in a background thread using SwingWorker
        new SwingWorker<Integer, String>() {
            @Override
            protected Integer doInBackground() throws Exception {
                List<String> command = new ArrayList<>();
                command.add("gallery-dl"); // Assuming gallery-dl is in PATH
                command.add(url);
                command.add("-D");
                command.add(downloadDir);

                if (noMtimeCheckBox.isSelected()) {
                    command.add("--no-mtime");
                }
                if (noDownloadCheckBox.isSelected()) {
                    command.add("--no-download");
                }

                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.redirectErrorStream(true); // Redirect stderr to stdout

                try {
                    Process process = processBuilder.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        publish(line); // Publish each line to process()
                    }
                    return process.waitFor(); // Wait for the process to complete
                } catch (IOException | InterruptedException e) {
                    publish("Error executing gallery-dl: " + e.getMessage());
                    return 1; // Indicate an error
                }
            }

            @Override
            protected void process(List<String> chunks) {
                // This method runs on the Event Dispatch Thread (EDT)
                for (String line : chunks) {
                    logMessage(line);
                }
            }

            @Override
            protected void done() {
                // This method runs on the Event Dispatch Thread (EDT) after doInBackground completes
                try {
                    Integer returnCode = get(); // Get the return code from doInBackground
                    if (returnCode == 0) {
                        logMessage("\nDownload completed successfully!");
                    } else {
                        logMessage(String.format("\nError: gallery-dl exited with code %d", returnCode));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    logMessage("\nAn unexpected error occurred: " + e.getMessage());
                } finally {
                    setUIEnabled(true); // Re-enable UI controls
                }
            }
        }.execute(); // Start the SwingWorker
    }

    public static void main(String[] args) {
        // Ensure the GUI is created and updated on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            mainclass gui = new mainclass();
            gui.setVisible(true);
        });
    }
}
