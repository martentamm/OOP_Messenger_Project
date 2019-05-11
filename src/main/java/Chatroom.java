import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class Chatroom {

    private String name;
    private Path path;
    private List<Message> messageList;
    private HashMap<String, List<Message>> userAndMessages;
    private int size;

    Chatroom(String name, Path path, int size) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.messageList = new ArrayList<>();
        this.userAndMessages = new HashMap<>();
    }

    List<Message> getMessageList() {
        return messageList;
    }

    void addToMessageList(Message message) {
        messageList.add(message);
    }

    HashMap<String, List<Message>> getUserAndMessages() {
        return userAndMessages;
    }

    void addUserToChatroom(String key) {
        userAndMessages.put(key, new ArrayList<>(messageList));
    }

    void addMessageToUser(String key, Message value) {
        userAndMessages.get(key).add(value);
    }

    String getName() {
        return name;
    }

    Path getPath() {
        return path;
    }

    int getSize() {
        return size;
    }
}
