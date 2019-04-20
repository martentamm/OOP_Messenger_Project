import java.io.DataInputStream;
import java.io.DataOutputStream;

class Commands {

    static void writeEnd(DataOutputStream socketOut) throws Exception {
        socketOut.writeInt(MessageTypes.END_SESSION.value());
    }

    static void writeUpdateRequest(DataOutputStream socketOut) throws Exception {
        socketOut.writeInt(MessageTypes.UPDATE_REQ.value());
    }

    static void writeUserToMap(DataOutputStream socketOut, String username) throws Exception {
        socketOut.writeInt(MessageTypes.USER_MAP.value());
        socketOut.writeUTF(username);
    }

    static void messageAuthor(DataOutputStream socketOut, String username) throws Exception {
        socketOut.writeInt(MessageTypes.AUTHOR_SIGNATURE.value());
        socketOut.writeUTF(username);
    }

    static void writeMessage(DataOutputStream socketOut, String message, int type, boolean isRequest) throws Exception {
        if (isRequest) {
            socketOut.writeInt(type);
            socketOut.writeUTF(CurseFilter.replaceCurseWordsWithAsterisks(message));

        } else {
            if (type == MessageTypes.TEXT.value()) {
                socketOut.writeInt(type);
                socketOut.writeUTF(CurseFilter.replaceCurseWordsWithAsterisks(message));

            }
        }
    }

    static int getType(DataInputStream socketIn) throws Exception {
        return socketIn.readInt();
    }

    static String getUsername(DataInputStream socketIn) throws Exception {
        return socketIn.readUTF();
    }

    static String readMessage(DataInputStream socketIn, int type) throws Exception {

        if (type == MessageTypes.END_SESSION.value() || type == MessageTypes.UPDATE_REQ.value()) {
            return "";
        } else if (type == MessageTypes.TEXT.value() || type == MessageTypes.USER_MAP.value() || type == MessageTypes.AUTHOR_SIGNATURE.value()) {
            return processMessage1(socketIn);
        } else {
            throw new IllegalArgumentException("type " + type);
        }
    }

    static private String processMessage1(DataInputStream value) throws Exception {
        return value.readUTF();
    }
}
