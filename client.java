package quizgame;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class client {
    private static final String DEFAULT_SERVER_IP = "localhost";
    private static final int DEFAULT_SERVER_PORT = 1294;  
    private static final String CONFIG_FILE = "server_info.dat";
    
    private static class ServerConfig {
        String ip;
        int port;
        
        ServerConfig() {
            try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
                this.ip = reader.readLine();
                this.port = Integer.parseInt(reader.readLine().trim());
                System.out.println("Server configuration loaded from file");
            } catch (IOException | NumberFormatException e) {
                this.ip = DEFAULT_SERVER_IP;
                this.port = DEFAULT_SERVER_PORT;
                System.out.println("Using default server configuration (localhost:1394)");
            }
        }
    }
    
    private static class Response {
        String type;
        String content;
        
        Response(BufferedReader in) throws IOException {
            // Read response line by line until we get content
            String line;
            while ((line = in.readLine()) != null) {
                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }
                
                // Parse content line
                if (line.startsWith("QUESTION:") || 
                    line.startsWith("RESULT:") || 
                    line.startsWith("SCORE:") || 
                    line.startsWith("SCORE_KOR:") ||
                    line.startsWith("END:")) {
                    
                    int colonIndex = line.indexOf(':');
                    this.type = line.substring(0, colonIndex);
                    this.content = line.substring(colonIndex + 1);
                    break;
                }
            }
            
            if (line == null) {
                throw new IOException("Server closed connection");
            }
        }
    }
    
    public static void main(String[] args) {
        ServerConfig config = new ServerConfig();
        
        try (
            Socket socket = new Socket(config.ip, config.port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("Connected to server at " + config.ip + ":" + config.port + "\n");
            
            while (true) {
                try {
                    Response response = new Response(in);
                    
                    switch (response.type) {
                        case "QUESTION":
                            System.out.println("Question: " + response.content);
                            System.out.print("Your answer: ");
                            String answer = scanner.nextLine();
                            out.println(answer);
                            break;
                            
                        case "RESULT":
                            System.out.println(response.content);
                            System.out.println();
                            break;
                            
                        case "SCORE":
                        case "SCORE_KOR":
                            System.out.println(response.content);
                            break;
                            
                        case "END":
                            System.out.println(response.content);
                            return;
                    }
                    
                } catch (IOException e) {
                    System.out.println("Error reading from server: " + e.getMessage());
                    break;
                }
            }
            
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }
}