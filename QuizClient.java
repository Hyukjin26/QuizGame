import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * A multithreaded QuizServer that handles multiple client connections.
 * Each client is assigned a thread to handle their quiz session.
 */
public class QuizServer {
    private static final int PORT = 1234; // The port number the server listens on
    private static final int THREAD_POOL_SIZE = 5; // Maximum number of concurrent clients

    // Questions and answer options
    private static final String[][] QUESTIONS = {
            {"What is the capital of China?", "Beijing", "Tokyo", "Shanghai", "Seoul"},
            {"What is the capital of the United States?", "London", "Boston", "New York", "Washington, D.C."},
            {"What is the capital of Brazil?", "São Paulo", "Brasília", "Rio de Janeiro", "Buenos Aires"},
            {"What is the capital of Russia?", "Moscow", "Sochi", "Saint Petersburg", "Kiev"},
            {"What is the capital of Japan?", "Taipei", "Tokyo", "Shanghai", "Hiroshima"}
    };

    // Correct answers corresponding to the questions
    private static final char[] ANSWERS = {'a', 'd', 'b', 'a', 'b'};

    /**
     * The main method to start the server.
     * It initializes a thread pool and waits for client connections.
     */
    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            // Continuously accept client connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                threadPool.execute(new QuizHandler(clientSocket)); // Assign a thread to the new client
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the question and options for the specified question index.
     *
     * @param index The index of the question.
     * @return A string formatted as "Question;OptionA;OptionB;OptionC;OptionD", or null if the index is out of bounds.
     */
    public static String getQuestionData(int index) {
        if (index >= 0 && index < QUESTIONS.length) {
            return QUESTIONS[index][0] + ";" + QUESTIONS[index][1] + ";" + QUESTIONS[index][2] + ";" +
                    QUESTIONS[index][3] + ";" + QUESTIONS[index][4];
        }
        return null;
    }

    /**
     * Gets the correct answer for the specified question index.
     *
     * @param index The index of the question.
     * @return The correct answer as a character ('a', 'b', 'c', 'd').
     */
    public static char getAnswer(int index) {
        return ANSWERS[index];
    }

    /**
     * Gets the total number of questions in the quiz.
     *
     * @return The total number of questions.
     */
    public static int getTotalQuestions() {
        return QUESTIONS.length;
    }
}

/**
 * Handles a quiz session for a single client in a separate thread.
 */
class QuizHandler implements Runnable {
    private final Socket socket;
    private int currentQuestionIndex = 0; // Tracks the current question for the client
    private int score = 0; // Tracks the client's score

    public QuizHandler(Socket socket) {
        this.socket = socket;
    }

    /**
     * The main execution method for handling client communication.
     * Processes commands such as START, NEXT, and ANSWER, and manages the quiz session.
     */
    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String clientAddress = socket.getInetAddress().toString();
            out.println("WELCOME TO THE QUIZ! Type START to begin or QUIT to exit.");

            String command;
            while ((command = in.readLine()) != null) {
                if (command.equalsIgnoreCase("START")) {
                    sendQuestion(out); // Send the first question to the client
                } else if (command.equalsIgnoreCase("NEXT")) {
                    currentQuestionIndex++;
                    if (currentQuestionIndex < QuizServer.getTotalQuestions()) {
                        sendQuestion(out); // Send the next question
                    } else {
                        // Quiz is complete, send final score
                        out.println("END:Quiz completed! Your final score is " + score);
                        System.out.println("Client (" + clientAddress + ") Final Score: " + score + "/" + QuizServer.getTotalQuestions());
                        break;
                    }
                } else if (command.startsWith("ANSWER:")) {
                    char clientAnswer = command.charAt(7); // Extract the client's answer
                    char correctAnswer = QuizServer.getAnswer(currentQuestionIndex);

                    if (clientAnswer == correctAnswer) {
                        score++;
                        out.println("FEEDBACK:Correct!");
                        System.out.println("Client (" + clientAddress + ") answered question " +
                                (currentQuestionIndex + 1) + ": Correct (Score: " + score + ")");
                    } else {
                        out.println("FEEDBACK:Incorrect! The correct answer was " + correctAnswer);
                        System.out.println("Client (" + clientAddress + ") answered question " +
                                (currentQuestionIndex + 1) + ": Incorrect (Answer: " + clientAnswer + ", Correct: " + correctAnswer + ")");
                    }
                } else if (command.equalsIgnoreCase("QUIT")) {
                    // Client quits the quiz early
                    out.println("END:Quiz exited. Your final score is " + score);
                    System.out.println("Client (" + clientAddress + ") quit the quiz. Final Score: " + score + "/" + QuizServer.getTotalQuestions());
                    break;
                } else {
                    out.println("Invalid command. Type START, NEXT, or QUIT.");
                }
            }
        } catch (IOException e) {
            System.err.println("Client disconnected unexpectedly: " + e.getMessage());
        }
    }

    /**
     * Sends the current question to the client.
     *
     * @param out The output stream to send the question data.
     */
    private void sendQuestion(PrintWriter out) {
        String questionData = QuizServer.getQuestionData(currentQuestionIndex);
        if (questionData != null) {
            out.println("QUESTION:" + questionData); // Send the question to the client
            System.out.println("Sent question " + (currentQuestionIndex + 1) + ": " + questionData);
        }
    }
}
