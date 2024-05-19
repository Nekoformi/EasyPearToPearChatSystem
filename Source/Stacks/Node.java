package Source.Stacks;

import Source.Client;
import Source.Tasks.ConnectClient;
import Source.Tasks.DisconnectClient;
import Source.Tasks.GetClientAddress;
import Source.Tasks.GetNodeList;
import Source.Tasks.GetUserList;
import Source.Tasks.PostChatMessage;
import Source.Tasks.PostUserProfile;
import Source.Tasks.RemoveUserProfile;
import Source.Tasks.UpdateUserProfile;
import Source.Utils.Message;
import Source.Utils.Util;

import java.io.*;
import java.net.*;

public class Node {
    // Note: Nodeは通信可能なコンピューター（他のNode）に対応する。

    public Client client;
    public Socket socket;

    public User user;

    BufferedReader reader;
    PrintWriter writer;

    NodeReceiver nodeReceiver;

    public Node(Client client, Socket socket) {
        this.client = client;
        this.socket = socket;

        client.systemConsole.pushMainLine("Connect to new node: " + Util.getSocketInfoString(socket));

        try {
            reader = createReader();
            writer = createWriter();

            client.systemConsole.pushSubLine("Start a node stream...");

            nodeReceiver = new NodeReceiver(this);

            sendMessage("alg", "+", client.userStack.myProfile.stringify());
        } catch (IOException e) {
            client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to create a stream reader / writer."));
        } catch (Exception e) {
            client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
        }
    }

    public void closeNode() {
        client.systemConsole.pushMainLine("Disconnect from the node: " + Util.getSocketInfoString(socket));

        if (nodeReceiver != null)
            nodeReceiver.done();

        client.nodeStack.remove(this);
    }

    public BufferedReader createReader() throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public PrintWriter createWriter() throws IOException {
        return new PrintWriter(socket.getOutputStream(), true);
    }

    public void closeReader(BufferedReader reader) throws IOException {
        if (reader != null)
            reader.close();
    }

    public void closeWriter(PrintWriter writer) throws IOException {
        if (writer != null)
            writer.close();
    }

    public void executeTask(Message message, int dataLength, Task task) {
        client.taskStack.execute(this, message, dataLength, task);
    }

    public void sendMessage(Message message) {
        client.systemConsole.pushSubLine("Send message: \"" + message.display(16) + "\" to " + Util.getSocketInfoString(socket));

        writer.println(message.stringify());
    }

    public void sendMessage(String command, String id, String... data) {
        Message message = new Message(client.systemConsole, command, id, data);

        sendMessage(message);
    }

    class NodeReceiver extends Thread {
        private volatile boolean done = false;

        Node node;

        public NodeReceiver(Node node) {
            this.node = node;

            start();
        }

        public void run() {
            try {
                while (!done) {
                    String line = reader.readLine();

                    if (line == null)
                        continue;

                    Message message = new Message(client.systemConsole, line, true).get();

                    executeCommand(message);
                }
            } catch (IOException e) {
                if (!done)
                    client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to run the node stream."));
            } catch (Exception e) {
                client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
            } finally {
                client.systemConsole.pushSubLine("Close the node stream...");

                try {
                    closeReader(reader);
                    closeWriter(writer);

                    socket.close();
                } catch (Exception e) {
                    client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
                }

                nodeReceiver = null;
            }
        }

        public synchronized void done() {
            client.systemConsole.pushSubLine("Finish listening to message.");

            done = true;

            try {
                socket.close();
            } catch (Exception e) {
                client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
            }
        }
    }

    void executeCommand(Message message) {
        if (message == null)
            return;

        // <Falsification> | <Impersonation> | <Message Spams>

        // 0: Impossible.
        // 1: Possible, but does not make sense.
        // 2: Possible.
        // 3: Possible, and a fatal vulnerability.

        switch (message.command) {
        case "req-nl": // - | 1 | 1
            executeTask(message, 1, new GetNodeList()); // TIMEOUT

            break;
        case "ret-nl": // 2 | - | 0
            executeTask(message, 1, null); // NODE STRUCTURE

            break;
        case "req-ul": // - | 1 | 1
            executeTask(message, 2, new GetUserList()); // TIMEOUT, PUBLIC KEY

            break;
        case "ret-ul": // 0 | 1 | 0
            executeTask(message, 3, null); // NODE STRUCTURE, ENCRYPTED USER DATA, ENCRYPTED COMMON KEY

            break;
        case "req-ca": // - | 1 | 1
            executeTask(message, 2, new GetClientAddress()); // TIMEOUT, TARGET USER ID

            break;
        case "ret-ca": // 0 | 1 | 0
            executeTask(message, 2, null); // ENCRYPTED USER ADDRESS, ENCRYPTED COMMON KEY

            break;
        case "pst-up": // 0 | 0 | 1
            executeTask(message, 5, new PostUserProfile()); // TIMEOUT, USER ID, USER DATA, TARGET USER ID, SECURE HASH

            break;
        case "upd-up": // 0 | 0 | 1
            executeTask(message, 4, new UpdateUserProfile()); // TIMEOUT, USER ID, USER DATA, SECURE HASH

            break;
        case "rem-up": // 0 | 0 | 3
            executeTask(message, 3, new RemoveUserProfile()); // TIMEOUT, USER ID, SECURE HASH

            break;
        case "rec-up": // - | - | 0
            executeTask(message, 0, null); // NULL

            break;
        case "pst-cm": // 0 | 0 | 3
            executeTask(message, 4, new PostChatMessage()); // TIMEOUT, USER ID, MESSAGE, SECURE HASH

            break;
        case "rec-cm": // - | - | 0
            executeTask(message, 0, null); // NULL

            break;
        case "dup": // - | - | 0
            executeTask(message, 0, null); // NULL

            break;
        case "alg": // - | 1 | 1
            executeTask(message, 1, new ConnectClient()); // USER DATA

            break;
        case "let": // - | - | 1
            executeTask(message, 0, new DisconnectClient()); // NULL

            break;
        default:
            client.systemConsole.pushErrorLine("Invalid message received: Command \"" + message.command + "\" does not exist.");

            break;
        }
    }
}
