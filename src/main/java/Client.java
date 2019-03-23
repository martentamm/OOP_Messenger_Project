import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) throws Exception {
        System.out.println("connecting to server");
        try (Socket socket = new Socket("localhost", 1337);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream();
             DataOutputStream dataOut = new DataOutputStream(out);
             DataInputStream dataIn = new DataInputStream(in)) {

            //Path saveToPath;
            //int type = Integer.parseInt(args[0]);
            int type = 1;

            System.out.println("connected; sending data");

            Scanner sc = new Scanner(System.in);

            if (type == 1) {

                while (sc.hasNextLine()) {

                    String toSend = sc.nextLine();

                    if (!(toSend.contains("END"))) {



                        Commands.writeMessage(dataOut, toSend, 1, true);
                        System.out.println("sent " + toSend);

                        /*Commands.getType(dataIn);
                        String clientMessageEcho = Commands.readMessage(dataIn, type);
                        System.out.println("received '" + clientMessageEcho + "' back");*/
                    } else {
                        dataOut.writeInt(0);
                    }
                }

            }
            /*else if (type == 2) {
                String path = args[1];
                saveToPath = Paths.get(args[2]);

                Commands.writeMessage(dataOut, path, 2, true);

                int checkType = Commands.getType(dataIn);

                if (checkType == 3) {
                    throw new IllegalArgumentException("error, server-relative path needed");

                } else if (checkType == 2) {
                    byte[] file = Commands.readFile(dataIn, checkType);
                    Files.write(saveToPath, file);
                    System.out.println("received" + saveToPath.toString());
                    System.out.println("File size: \n" + new File(args[2]).length() + " bytes\n");
                    System.out.println("File was written to: " + saveToPath);

                } else {
                    throw new IllegalArgumentException("error, could not find file");
                }



            }*/

        }

        System.out.println("finished");
        System.out.println();
    }

}