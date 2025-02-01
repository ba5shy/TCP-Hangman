import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Server {

    static String[] words = { "network", "transfer", "control", "protocol", "computer", "send", "receive", "server",
            "client" };

    public static void main(String[] args) throws Exception {

        ServerSocket server_socket = new ServerSocket(9000);

        while (true) {
            System.out.println("Waiting for client connection...");
            // this loop waits for connections and creates a separate thread for
            // each client
            Socket client_socket = server_socket.accept();

            // create new thread with initial score of zero
            ServerThread client_thread = new ServerThread(client_socket);
            client_thread.start();

        }

    }

}

class ServerThread extends Thread {

    Socket client_socket;
    String word; // word to be guessed by client
    int attempts; // attempts remaining
    int score = 0; // initial score
    StringBuilder current_revealed_letters; // initailly _ _ _ _ ...
    boolean game_end = false;

    // used for formatting & centering text
    static int wordWidth = 15;
    static int attemptsWidth = 20;
    static int scoreWidth = 5;

    public ServerThread(Socket client_socket) {
        // constructor to instantiate a thread
        this.client_socket = client_socket;
    }

    public void run() {
        System.out.println("Client thread started");
        try {

            // used to send to client
            OutputStreamWriter output = new OutputStreamWriter(client_socket.getOutputStream());
            BufferedWriter buffered_writer = new BufferedWriter(output);

            // used to receive from client
            InputStreamReader input = new InputStreamReader(client_socket.getInputStream());
            BufferedReader buffered_reader = new BufferedReader(input);

            String client_message = "";

            boolean play_again = true;

            while (play_again) {
                set_game_details();
                game_end = false;

                // send welcome message
                send_message(buffered_writer, "Welcome to Hangman!");

                sleep(1000);

                sendGameState(buffered_writer);

                display_on_server("Start Game");

                String user_action;

                while (!game_end) {

                    // 3RD SEND -> PROMPT
                    send_message(buffered_writer, "Enter Your Guess: (type \"quit\" to exit)");

                    // 1ST RECEIVE -> Client Message
                    client_message = buffered_reader.readLine();
                    if (client_message.equals("quit")) {
                        display_on_server("Quit Game");
                        break;
                    }

                    update_game_details(client_message);

                    sendGameState(buffered_writer);

                    user_action = "Guess " + client_message;

                    display_on_server(user_action);

                    // send game status to user
                    if (game_end) {

                        send_message(buffered_writer, "true");

                        if (attempts > 0)
                            send_message(buffered_writer, "You win! Would you like to play again? (yes/no)");
                        else
                            send_message(buffered_writer, "You Lose :( Would you like to play again? (yes/no)");
                        
                        client_message = buffered_reader.readLine();

                        if (client_message.equals("yes")) {
                            play_again = true;

                        } else {
                            // end connection (client does not want to play another game)
                            play_again = false;
                            client_socket.close();
                        }

                    } else {

                        send_message(buffered_writer, "false");

                    }
                    if (game_end)
                        break;
                }

                if (play_again == true)
                    continue;

                client_socket.close();
                System.out.println("Client Thread Ended");
            }

        } catch (SocketException e) {
            System.out.println("Client Disconnected");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void set_game_details() {
        // set word to be guessed and number of attempts
        int wordIndex = (int) (Math.random() * Server.words.length);

        word = Server.words[wordIndex]; // word to be guessed

        attempts = word.length(); // number of attempts

        current_revealed_letters = new StringBuilder("_".repeat(word.length()));

    }

    void update_game_details(String client_message) {

        if (client_message.length() == 1) {
            // if client guessed one letter
            // check if letter in word
            if (word.contains(client_message)) {
                // word contains guess, update revealed letters
                for (int i = 0; i < word.length(); i++) {
                    if (word.charAt(i) == client_message.charAt(0)) {
                        current_revealed_letters.setCharAt(i, client_message.charAt(0));
                    }
                }

                // check if word is completely guessed
                if (current_revealed_letters.toString().equals(word)) {
                    game_end = true;
                    score += 1;
                }
            } else {
                attempts -= 1;
                if (attempts == 0) {
                    game_end = true;
                    score -= 1;
                }
            }
        } else {
            // user entered more than character
            // compare entire word
            if (!word.equals(client_message)) {
                attempts -= 1;
                if (attempts == 0) {
                    score -= 1;
                    game_end = true;
                }
            } else {
                score += 1;
                current_revealed_letters = new StringBuilder(word);
                game_end = true;
            }
        }

    }

    void sendGameState(BufferedWriter buffered_writer) throws IOException {
        String client_header = center("Word", wordWidth) + "|" +
                center("Remaining Attempts", attemptsWidth) + "|" +
                center("Score", scoreWidth) + "|";

        buffered_writer.write(client_header);
        buffered_writer.newLine();
        buffered_writer.flush();
        String client_display = center(current_revealed_letters.toString(), wordWidth) + "|" +
                center(String.valueOf(attempts), attemptsWidth) + "|" +
                center(String.valueOf(score), scoreWidth) + "|";
        buffered_writer.write(client_display);
        buffered_writer.newLine();
        buffered_writer.flush();
    }

    void display_on_server(String text) {
        // this method is used to display updated on the server

        String action = "(" + this.getName() + ")" + " User Action";
        String server_header = center(action, 22) + "|" +
                center("Word", wordWidth) + "|" +
                center("Remaining Attempts", attemptsWidth) + "|" +
                center("Score", scoreWidth) + "|" +
                center("Word Chosen by Server", 21);
        String server_display = server_header + "\n" +
                center(text, 22) + "|" +
                center(current_revealed_letters.toString(), wordWidth) + "|" +
                center(String.valueOf(attempts), attemptsWidth) + "|" +
                center(String.valueOf(score), scoreWidth) + "|" +
                center(word, 21);

        System.out.println(server_display);
    }

    void send_message(BufferedWriter buffered_writer, String text) throws IOException {
        // this method is used to send a single message
        buffered_writer.write(text);
        buffered_writer.newLine();
        buffered_writer.flush();
    }

    void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
        }
    }

    // Helper method to center text in a given width
    String center(String text, int width) {
        int padding = (width - text.length()) / 2;
        String paddingSpace = " ".repeat(Math.max(0, padding));
        return paddingSpace + text + paddingSpace + (text.length() % 2 == 0 ? "" : " ");
    }
}
