import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.SocketException;

public class Update implements Runnable {

    private DataOutputStream dataOut;
    private DataInputStream dataIn;
    private String username;
    private String chatroomName;

    Update(DataOutputStream dataOut, DataInputStream dataIn, String username, String chatroomName) {
        this.dataOut = dataOut;
        this.dataIn = dataIn;
        this.username = username;
        this.chatroomName = chatroomName;
    }

    @Override
    public void run() {

        while (true) {
            try {
                Thread.sleep(500);
                //Commands.messageAuthor(dataOut, username);
                Commands.messageAuthor(dataOut, username);
                Commands.writeUpdateRequest(dataOut);
                int gotType = Commands.getType(dataIn);
                String message = Commands.readMessage(dataIn, gotType);
                message = message.trim();
                if (!message.equals("")) {
                    System.out.print(message + "\n");
                }
            } catch (SocketException e) {
                System.out.println("Connection terminated"); // TODO fix this.
                break;
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }
    }

}