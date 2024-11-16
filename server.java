package quizgame;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class server {
    private static final int PORT = 1294;
    private static final int MAX_CLIENTS = 10;
    
    // 퀴즈 문제 및 정답 목록
    private static final List<Question> questions = Arrays.asList(
        new Question("What is 34 + 5?", "39"),
        new Question("What is 20 * 5?", "100"),
        new Question("What is 5 - 2?", "3")
    );
    
    private static class Question {
        String question;
        String answer;
        
        Question(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }
    }

    // HTTP 스타일 응답 전송
    private static void sendResponse(PrintWriter out, String statusCode, String contentType, String content) {
        // Status Line
        out.println("HTTP/1.1 " + statusCode);
        // Headers
        out.println("Content-Type: " + contentType);
        out.println("Content-Length: " + content.length());
        out.println("Connection: keep-alive");
        out.println();  // Empty line between headers and body
        // Body
        out.println(content);
        out.flush();
    }
    
    public static void main(String[] args) {
        // ThreadPool을 사용한 멀티클라이언트 처리
        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.execute(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    System.out.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        } finally {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException ex) {
                threadPool.shutdownNow();
            }
        }
    }
    
    private static class ClientHandler implements Runnable {
        private Socket socket;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            System.out.println("New client connected: " + socket.getInetAddress());
            
            try (
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                int score = 0;
                
                // 초기 연결 성공 메시지
                sendResponse(out, "200 OK", "text/plain", "CONNECTED");
                
                // Send questions one by one
                for (Question q : questions) {
                    // Send question
                    sendResponse(out, "200 OK", "text/plain", "QUESTION:" + q.question);
                    
                    try {
                        // Get answer with timeout
                        String answer = in.readLine();
                        if (answer == null) throw new IOException("Client disconnected");
                        
                        // Check answer
                        if (q.answer.equals(answer.trim())) {
                            sendResponse(out, "200 OK", "text/plain", "RESULT:Correct!");
                            score++;
                        } else {
                            sendResponse(out, "200 OK", "text/plain", 
                                "RESULT:Wrong! Correct answer was: " + q.answer);
                        }
                    } catch (IOException e) {
                        System.out.println("Error reading client answer: " + e.getMessage());
                        break;
                    }
                }
                
                // Send final scores
                sendResponse(out, "200 OK", "text/plain", 
                    "SCORE:Final Score: " + score + "/" + questions.size());
                
                sendResponse(out, "200 OK", "text/plain", 
                    "SCORE_KOR:최종 점수는 " + score + "점!");
                
                // Send game over
                sendResponse(out, "200 OK", "text/plain", "END:Game Over");
                
            } catch (IOException e) {
                System.out.println("Error handling client: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage());
                }
            }
        }
    }
}