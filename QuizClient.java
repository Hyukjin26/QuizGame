import java.io.*;
import java.net.*;

public class QuizClient {
    private static final String CONFIG_FILE = "server_info.dat";
    private static String serverIp = "localhost";
    private static int serverPort = 1234;

    public static void main(String[] args) {
        // Read server info from configuration file
        readServerConfig();

        try (Socket socket = new Socket(serverIp, serverPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to the server.");
            String serverMessage;

            while ((serverMessage = in.readLine()) != null) {
                System.out.println("Server: " + serverMessage);

                if (serverMessage.startsWith("QUESTION:")) {
                    System.out.print("Your answer (e.g., ANSWER:a): ");
                    String answer = userInput.readLine();
                    out.println("ANSWER:" + answer.trim());
                } else if (serverMessage.startsWith("FEEDBACK:")) {
                    // 서버에서 피드백을 받았을 때 출력
                    System.out.println(serverMessage);
                } else if (serverMessage.startsWith("END:")) {
                    System.out.println(serverMessage);
                    break;
                } else {
                    System.out.print("> ");
                    String input = userInput.readLine();
                    out.println(input);
                }
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    private static void readServerConfig() {
        try (BufferedReader configReader = new BufferedReader(new FileReader(CONFIG_FILE))) {
            serverIp = configReader.readLine();
            serverPort = Integer.parseInt(configReader.readLine());
        } catch (IOException e) {
            System.out.println("Configuration file not found. Using default values.");
        }
    }
}
