import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) throws Exception {

        try {
            Socket socket = new Socket("localhost", 9000);

            // read from keyboard
            Scanner scanner = new Scanner(System.in);

            // receive from server
            InputStreamReader input = new InputStreamReader(socket.getInputStream());
            BufferedReader buffered_reader = new BufferedReader(input);

            // send to server
            OutputStreamWriter output = new OutputStreamWriter(socket.getOutputStream());
            BufferedWriter buffered_writer = new BufferedWriter(output);

            String client_message = "";
            String server_message = "";

            boolean play_again = true;

            while (play_again) {

                // read welcome message
                server_message = buffered_reader.readLine();
                System.out.println(server_message);
                System.out.println(); // new line after every message

                // read header
                server_message = buffered_reader.readLine();
                System.out.println(server_message);
                server_message = buffered_reader.readLine();
                System.out.println(server_message);

                while (!client_message.toLowerCase().equals("quit")) {

                    // read prompt
                    server_message = buffered_reader.readLine();
                    System.out.println(server_message);
                    System.out.println(); // new line after every message

                    // user starts entering guesses
                    client_message = scanner.next();

                    // send client message
                    buffered_writer.write(client_message);
                    buffered_writer.newLine();
                    buffered_writer.flush();

                    if (client_message.toLowerCase().equals("quit")) {
                        break;
                    }

                    // receive header
                    System.out.println();
                    server_message = buffered_reader.readLine();
                    System.out.println(server_message);
                    server_message = buffered_reader.readLine();
                    System.out.println(server_message);

                    // receive status
                    String game_end = buffered_reader.readLine();
                    if (game_end.equals("true")) {
                        // game ended, ask if client wants to play again
                        server_message = buffered_reader.readLine();
                        System.out.println(server_message);

                        while (true) {
                            client_message = scanner.next();

                            if (client_message.toLowerCase().equals("yes")) {
                                // play again
                                buffered_writer.write(client_message);
                                buffered_writer.newLine();
                                buffered_writer.flush();

                                play_again = true;
                                break;

                            } else if (client_message.toLowerCase().equals("no")) {
                                // end game

                                buffered_writer.write(client_message);
                                buffered_writer.newLine();
                                buffered_writer.flush();

                                play_again = false;
                                break;
                            } else {
                                System.out.println("Please enter a valid value (yes/no)\n");

                            }
                        }

                    } else {
                        continue;
                    }

                    if (play_again)
                        break;
                    else
                        System.exit(0);

                }

                if (play_again)
                    continue;

                socket.close();
                scanner.close();

            }

        } catch (SocketException e) {
            System.out.println("\nServer Offline");
        } catch (UnknownHostException e) {
            System.out.println("\nPort Number is Incorrect");
        }

    }
}
