import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ThreadSocket implements Runnable {

    private String username;
    private Chatroom chatroom;
    private List<Chatroom> chatrooms;
    private List<String> users;
    private final Socket socket;
    private final Argon2 argon2 = Argon2Factory.create();

    ThreadSocket(List<Chatroom> chatrooms, List<String> users, Socket ss) {
        this.chatrooms = chatrooms;
        this.users = users;
        this.socket = ss;
    }

    private int registerUser(DataInputStream socketIn) throws IOException {
        String userName = socketIn.readUTF();
        String passWord = socketIn.readUTF();

        String hash = argon2.hash(30, 65536, 1, passWord.toCharArray());

        if (!Files.exists(Path.of("credentials.txt"))) {
            Files.createFile(Path.of("credentials.txt"));
        }

        List<String> credentials = Files.readAllLines(Path.of("credentials.txt"));

        for (String credential : credentials) {
            String[] split = credential.split("\t");
            if (userName.equals(split[0])) {
                return MessageTypes.REGISTRATION_WRONG_USERNAME.value();
            }
            if (passWord.equals(split[1])) {
                return MessageTypes.REGISTRATION_WRONG_PASSWORD.value();
            }
        }

        Path path = Path.of("credentials.txt");
        String newUser = userName + "\t" + hash + "\t" + "true";
        Files.write(path, Collections.singletonList(newUser), StandardCharsets.UTF_8,
                StandardOpenOption.APPEND);
        return MessageTypes.REGISTRATION_SUCCESS.value();
    }

    private int loginUser(DataInputStream socketIn) throws IOException {
        String userName = socketIn.readUTF();
        String passWord = socketIn.readUTF();

        if (users.contains(userName)) {
            return MessageTypes.LOGIN_USER_ALREADY_IN.value();
        }

        if (!Files.exists(Path.of("credentials.txt"))) {
            return MessageTypes.LOGIN_MISSING_DB.value();
        }

        List<String> credentials = Files.readAllLines(Path.of("credentials.txt"));

        for (String credential : credentials) {
            String[] split = credential.split("\t");
            if (userName.equals(split[0])) {
                if (argon2.verify(split[1], passWord)) {
                    //onlineStatusFromFalseToTrue(username);
                    username = userName;
                    users.add(username);
                    return MessageTypes.LOGIN_SUCCESS.value();
                } else {
                    return MessageTypes.LOGIN_WRONG_PASSWORD.value();
                }
            }
        }
        return MessageTypes.LOGIN_WRONG_USERNAME.value();
    }

    private void sendChatroomNames(DataInputStream dataIn, DataOutputStream dataOut) throws IOException {
        int length = chatrooms.size();
        dataOut.writeInt(length);
        if (length != 0) {
            for (Chatroom cr : chatrooms) {
                dataOut.writeUTF(cr.getName());
            }
        }
        if (dataIn.readInt() == MessageTypes.CHATROOMS_LIST_SUCCESS.value()) {
            System.out.println("successfully sent chatrooms list to " + socket);
        }
    }

    private void exitUser() throws IOException {
        onlineStatusFromTrueToFalse();
    }

    @Override
    public void run() {
        try (socket;
             DataInputStream dataIn = new DataInputStream(socket.getInputStream());
             DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream())) {
            System.out.println("client connected");

            while (true) {

                int type = Commands.getType(dataIn);

                if (type == MessageTypes.REGISTRATION_REQ.value()) {
                    int response = registerUser(dataIn);
                    dataOut.writeInt(response);
                    continue;
                }

                if (type == MessageTypes.LOGIN_REQ.value()) {
                    int response = loginUser(dataIn);
                    dataOut.writeInt(response);
                    continue;
                }

                if (type == MessageTypes.CHATROOMS_LIST_REQ.value()) {
                    File f = new File("chatrooms");
                    var files = f.listFiles();
                    if (files == null) {
                        throw new IOException("failed to list " + f);
                    }

                    List<String> FileChatroomNames = new ArrayList<>();

                    for (File file : files) {
                        List<String> chatroomContents = Files.readAllLines(file.toPath());
                        String chatroomName = chatroomContents.get(0);

                        FileChatroomNames.add(chatroomName);

                    }

                    List<String> currentChatroomNames = new ArrayList<>();

                    for (Chatroom cr : chatrooms) {
                        currentChatroomNames.add(cr.getName());
                    }

                    for (String fileChatName : FileChatroomNames) {
                        if (!currentChatroomNames.contains(fileChatName)) {
                            Chatroom newChatroom = new Chatroom(fileChatName,
                                    Path.of("chatrooms", fileChatName + ".txt"));
                            chatrooms.add(newChatroom);
                        }
                    }

                    sendChatroomNames(dataIn, dataOut);
                    continue;
                }

                if (type == MessageTypes.CHATROOM_CONNECT_USER.value()) {
                    String chatroomName = dataIn.readUTF();

                    boolean isChatroomInList = false;

                    for (Chatroom cr : chatrooms) {
                        if (cr.getName().equals(chatroomName)) {
                            this.chatroom = cr;

                            List<String> chatroomContents = Files.readAllLines(Path.of("chatrooms", chatroomName + ".txt"));

                            if (chatroom.getMessageList().isEmpty()) {
                                for (int i = 1; i < chatroomContents.size(); i++) {
                                    String[] messageInfo = chatroomContents.get(i).split("\t");
                                    long timestamp = Long.valueOf(messageInfo[0]);
                                    String author = messageInfo[1];
                                    String text = messageInfo[2];
                                    Message message = new Message(timestamp, author, text);
                                    chatroom.addToMessageList(message);
                                }
                            }

                            chatroom.addUserToChatroom(username);
                            isChatroomInList = true;
                            break;
                        }
                    }

                    if (!isChatroomInList) {
                        Path path = Path.of("chatrooms", chatroomName + ".txt");
                        Files.createFile(path);

                        Files.write(path, Collections.singletonList(chatroomName), StandardCharsets.UTF_8,
                                StandardOpenOption.APPEND);

                        Chatroom cr = new Chatroom(chatroomName, path);
                        this.chatroom = cr;
                        chatrooms.add(cr);
                        chatroom.addUserToChatroom(username);
                    }

                    dataOut.writeInt(MessageTypes.CHATROOMS_USER_CONNECTED.value());
                    System.out.println("connected user " + username + " to chatroom " + chatroomName);
                    continue;
                }

                if (type == MessageTypes.UPDATE_REQ.value()) {

                    if (!chatroom.getUserAndMessages().get(username).isEmpty()) {

                        List<Message> messageList = chatroom.getUserAndMessages().get(username);
                        int length = messageList.size();
                        dataOut.writeInt(length);

                        for (Message m : messageList) {
                            String message = m.getMessage();
                            long timestamp = m.getTimestamp();
                            String author = m.getAuthor();

                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            Date resultDate = new Date(timestamp);
                            message = "[" + sdf.format(resultDate) + "] " + author + ">>" + message;

                            System.out.println(message);

                            Commands.writeMessage(dataOut, message, MessageTypes.TEXT.value(), false);
                        }

                        chatroom.getUserAndMessages().get(username).clear();

                    } else {
                        dataOut.writeInt(0);
                        continue;
                    }
                }

                if (type == MessageTypes.EXIT_CHATROOM.value()) {
                    System.out.println("user " + username + " exited chatroom " + chatroom.getName());
                    continue;
                }

                if (type == MessageTypes.END_SESSION.value()) {
                    break;
                }

                //////////////////// READ ACTUAL MESSAGE ///////////////////////

                if (type == MessageTypes.TEXT.value()) {

                    String clientMessage = dataIn.readUTF(); //Commands.readMessage(dataIn, type);

                    if (clientMessage.isBlank()) {
                        continue;
                    }

                    Message message = new Message(System.currentTimeMillis(), username, clientMessage);

                    chatroom.addToMessageList(message);

                    for (String key : chatroom.getUserAndMessages().keySet()) { //Username -> Chatroom; Chatroom(Username -> Message)
                        if (!key.equals(username)) {
                            chatroom.addMessageToUser(key, message);
                        }
                    }

                    clientMessage = message.getTimestamp() + "\t" + message.getAuthor() + "\t" + message.getMessage();
                    Files.write(chatroom.getPath(), Collections.singletonList(clientMessage), StandardCharsets.UTF_8,
                            StandardOpenOption.APPEND);

                    System.out.println(chatroom.getName() + " received message from " + clientMessage + "\n");
                }
            }

        } catch (Exception e) {
            users.remove(username);
            throw new RuntimeException();
        }

        System.out.println("ended connection with user " + username + " at " + socket);
    }

    private void onlineStatusFromFalseToTrue(String username) throws IOException {
        Path path = Path.of("credentials.txt");
        List<String> newCredentialsText = new ArrayList<>();
        List<String> credentials = Files.readAllLines(path);

        for (String credential : credentials) {
            String[] split = credential.split("\t");
            if (username.equals(split[0]))
                newCredentialsText.add(credential.replace("false", "true"));
            else
                newCredentialsText.add(credential);
        }
        Files.write(path, newCredentialsText, StandardCharsets.UTF_8, StandardOpenOption.WRITE);


    }

    private void onlineStatusFromTrueToFalse() throws IOException {
        Path path = Path.of("credentials.txt");
        List<String> newCredentialsText = new ArrayList<>();
        List<String> credentials = Files.readAllLines(path);

        for (String credential : credentials) {
            String[] split = credential.split("\t");
            if (username.equals(split[0]))
                newCredentialsText.add(credential.replace("true", "false"));

            else
                newCredentialsText.add(credential);

        }
        Files.write(path, newCredentialsText, StandardCharsets.UTF_8, StandardOpenOption.WRITE);


    }
}