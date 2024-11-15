import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

/**
 * A GUI-based Quiz Client that communicates with a Quiz Server.
 * Allows the user to answer quiz questions and receive feedback.
 */
public class QuizClientGUI {
    private static final String CONFIG_FILE = "src/server_info.dat"; // Configuration file for server info
    private static String serverIp = "localhost"; // Default server IP
    private static int serverPort = 1234; // Default server port

    private JFrame frame; // Main application window
    private JTextArea questionArea; // Area to display the question
    private JRadioButton optionA, optionB, optionC, optionD; // Radio buttons for answer options
    private ButtonGroup optionGroup; // Group to manage radio button selection
    private JButton checkButton, nextButton; // Buttons to submit an answer or request the next question
    private Socket socket; // Socket to connect to the server
    private PrintWriter out; // Output stream to send messages to the server
    private BufferedReader in; // Input stream to receive messages from the server

    /**
     * Constructor that initializes the GUI and connects to the server.
     */
    public QuizClientGUI() {
        initializeGUI();
        readServerConfig(); // Load server IP and port from the configuration file
        connectToServer();
    }

    /**
     * Initializes the GUI components.
     * Sets up the question display area, answer options, and control buttons.
     */
    private void initializeGUI() {
        frame = new JFrame("Quiz Game"); // Main frame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLayout(new BorderLayout());

        // Text area for displaying the current question
        questionArea = new JTextArea();
        questionArea.setFont(new Font("Arial", Font.BOLD, 16));
        questionArea.setEditable(false); // Prevent user input
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(questionArea);
        frame.add(scrollPane, BorderLayout.NORTH);

        // Panel for radio buttons (answer options)
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridLayout(4, 1)); // 4 rows, 1 column
        optionA = new JRadioButton();
        optionB = new JRadioButton();
        optionC = new JRadioButton();
        optionD = new JRadioButton();

        optionGroup = new ButtonGroup(); // Group radio buttons to allow only one selection
        optionGroup.add(optionA);
        optionGroup.add(optionB);
        optionGroup.add(optionC);
        optionGroup.add(optionD);

        optionsPanel.add(optionA);
        optionsPanel.add(optionB);
        optionsPanel.add(optionC);
        optionsPanel.add(optionD);

        frame.add(optionsPanel, BorderLayout.CENTER);

        // Panel for control buttons (Check and Next)
        JPanel buttonPanel = new JPanel();
        checkButton = new JButton("Check");
        checkButton.addActionListener(e -> submitAnswer()); // Submit the selected answer
        buttonPanel.add(checkButton);

        nextButton = new JButton("Next");
        nextButton.addActionListener(e -> requestNextQuestion()); // Request the next question
        nextButton.setEnabled(false); // Initially disabled
        buttonPanel.add(nextButton);

        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    /**
     * Reads the server IP and port from a configuration file.
     * If the file is not found or is invalid, default values are used.
     */
    private void readServerConfig() {
        try (BufferedReader configReader = new BufferedReader(new FileReader(CONFIG_FILE))) {
            serverIp = configReader.readLine(); // Read server IP from the first line
            serverPort = Integer.parseInt(configReader.readLine()); // Read server port from the second line
        } catch (IOException | NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Configuration file not found or invalid. Using default values.");
        }
    }

    /**
     * Connects to the Quiz Server and starts a background thread to handle server messages.
     */
    private void connectToServer() {
        try {
            socket = new Socket(serverIp, serverPort); // Connect to the server using loaded IP and port
            out = new PrintWriter(socket.getOutputStream(), true); // Create output stream to send messages
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Create input stream to receive messages

            // Background thread to handle server messages
            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        if (serverMessage.startsWith("QUESTION:")) {
                            displayQuestion(serverMessage.substring(9)); // Display the question
                        } else if (serverMessage.startsWith("FEEDBACK:")) {
                            JOptionPane.showMessageDialog(frame, serverMessage.substring(9)); // Show feedback for the answer
                            checkButton.setEnabled(false); // Disable "Check" after feedback
                            nextButton.setEnabled(true); // Enable "Next" for the next question
                        } else if (serverMessage.startsWith("END:")) {
                            JOptionPane.showMessageDialog(frame, serverMessage.substring(4)); // Show the final score
                            disableAllButtons(); // Disable all buttons
                        }
                    }
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(frame, "Connection lost."); // Handle disconnection
                }
            }).start();

            out.println("START"); // Start the quiz
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Could not connect to the server."); // Handle connection errors
        }
    }

    /**
     * Displays the received question and answer options in the GUI.
     *
     * @param questionData The question and options formatted as "Question;OptionA;OptionB;OptionC;OptionD".
     */
    private void displayQuestion(String questionData) {
        String[] parts = questionData.split(";"); // Split the data into question and options
        questionArea.setText(parts[0]); // Display the question
        optionA.setText(parts[1]); // Option A
        optionB.setText(parts[2]); // Option B
        optionC.setText(parts[3]); // Option C
        optionD.setText(parts[4]); // Option D
        optionGroup.clearSelection(); // Clear previous selection
        checkButton.setEnabled(true); // Enable "Check" button
        nextButton.setEnabled(false); // Disable "Next" button until answer is checked
    }

    /**
     * Submits the selected answer to the server.
     * If no answer is selected, shows a warning message.
     */
    private void submitAnswer() {
        String selectedAnswer = null;
        if (optionA.isSelected()) selectedAnswer = "a";
        if (optionB.isSelected()) selectedAnswer = "b";
        if (optionC.isSelected()) selectedAnswer = "c";
        if (optionD.isSelected()) selectedAnswer = "d";

        if (selectedAnswer != null) {
            out.println("ANSWER:" + selectedAnswer); // Send the selected answer to the server
        } else {
            JOptionPane.showMessageDialog(frame, "Please select an answer."); // Warn if no option is selected
        }
    }

    /**
     * Requests the next question from the server.
     */
    private void requestNextQuestion() {
        out.println("NEXT"); // Request the next question
    }

    /**
     * Disables all interactive buttons and options.
     * Called when the quiz is finished or connection is lost.
     */
    private void disableAllButtons() {
        optionA.setEnabled(false);
        optionB.setEnabled(false);
        optionC.setEnabled(false);
        optionD.setEnabled(false);
        checkButton.setEnabled(false);
        nextButton.setEnabled(false);
    }

    /**
     * Main method to launch the Quiz Client GUI.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(QuizClientGUI::new); // Create and display the GUI on the Event Dispatch Thread
    }
}
