package Source.Stacks;

import Source.Client;
import Source.Tasks.ConnectClient;
import Source.Tasks.DisconnectClient;
import Source.Tasks.GetClientAddress;
import Source.Tasks.GetNodeList;
import Source.Tasks.GetUserList;
import Source.Tasks.PostChatFile;
import Source.Tasks.PostChatMessage;
import Source.Tasks.PostOuroborosNodeData;
import Source.Tasks.PostUserProfile;
import Source.Tasks.RemoveUserProfile;
import Source.Tasks.RequestChatFile;
import Source.Tasks.SendChatFile;
import Source.Tasks.UpdateUserProfile;
import Source.Utils.Message;
import Source.Utils.Util;

import java.io.*;
import java.net.*;

public class Node {
    // Note: Nodeは通信可能なコンピューター（他のNode）に対応する。

    public static final boolean SEND_IN_BINARY = true;

    public Client client;
    public Socket socket;

    public User user;

    InputStream binaryReader;
    OutputStream binaryWriter;

    BufferedReader stringReader;
    PrintWriter stringWriter;

    NodeReceiver nodeReceiver;

    public Node(Client client, Socket socket) {
        this.client = client;
        this.socket = socket;

        client.systemConsole.pushMainLine("Connect to new node: " + Util.getSocketInfoString(socket));

        try {
            if (SEND_IN_BINARY) {
                binaryReader = createBinaryReader();
                binaryWriter = createBinaryWriter();
            } else {
                stringReader = createStringReader();
                stringWriter = createStringWriter();
            }

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

    public InputStream createBinaryReader() throws IOException {
        return socket.getInputStream();
    }

    public OutputStream createBinaryWriter() throws IOException {
        return socket.getOutputStream();
    }

    public BufferedReader createStringReader() throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public PrintWriter createStringWriter() throws IOException {
        return new PrintWriter(socket.getOutputStream(), true);
    }

    public void closeBinaryReader(InputStream reader) throws IOException {
        if (reader != null)
            reader.close();
    }

    public void closeBinaryWriter(OutputStream writer) throws IOException {
        if (writer != null)
            writer.close();
    }

    public void closeStringReader(BufferedReader reader) throws IOException {
        if (reader != null)
            reader.close();
    }

    public void closeStringWriter(PrintWriter writer) throws IOException {
        if (writer != null)
            writer.close();
    }

    public void executeTask(Message message, int dataLength, Task task) {
        client.taskStack.execute(this, message, dataLength, task);
    }

    public void sendMessage(Message message) {
        client.systemConsole.pushSubLine("Send message: \"" + message.display(16) + "\" to " + Util.getSocketInfoString(socket));

        try {
            if (SEND_IN_BINARY) {
                binaryWriter.write(message.binarize());
            } else {
                stringWriter.println(message.stringify());
            }
        } catch (IOException e) {
            client.systemConsole.pushErrorLine("Failed to send message.");
        }
    }

    public void sendMessage(String command, String id, byte[] data) {
        Message message = new Message(client.systemConsole, command, id, data);

        sendMessage(message);
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
                if (SEND_IN_BINARY) {
                    readBinary();
                } else {
                    readString();
                }
            } catch (IOException e) {
                if (!done)
                    client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to run the node stream."));
            } catch (Exception e) {
                client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
            } finally {
                client.systemConsole.pushSubLine("Close the node stream...");

                try {
                    closeBinaryReader(binaryReader);
                    closeBinaryWriter(binaryWriter);

                    closeStringReader(stringReader);
                    closeStringWriter(stringWriter);

                    socket.close();
                } catch (Exception e) {
                    client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
                }

                nodeReceiver = null;
            }
        }

        void readBinary() throws Exception {
            while (!done) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                while (true) {
                    byte[] buffer = new byte[1024];
                    int bufferLength;

                    bufferLength = binaryReader.read(buffer, 0, buffer.length);

                    if (bufferLength == -1) {
                        done = true;

                        break;
                    }

                    byteArrayOutputStream.write(buffer, 0, bufferLength);

                    if (bufferLength < buffer.length)
                        break;
                }

                if (byteArrayOutputStream.size() > 0) {
                    Message message = new Message(client.systemConsole, byteArrayOutputStream.toByteArray(), true).get();

                    executeCommand(message);
                }
            }
        }

        void readString() throws Exception {
            while (!done) {
                String line = stringReader.readLine();

                if (line == null) {
                    done = true;

                    break;
                }

                if (line.length() > 0) {
                    Message message = new Message(client.systemConsole, line, true).get();

                    executeCommand(message);
                }
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
        case Message.NAME_REQUEST_NODE_LIST: // - | 1 | 1
            executeTask(message, 1, new GetNodeList()); // TIMEOUT

            break;
        case Message.NAME_RETURN_NODE_LIST: // 2 | - | 0
            executeTask(message, 1, null); // NODE STRUCTURE

            break;
        case Message.NAME_REQUEST_USER_LIST: // - | 1 | 1
            executeTask(message, 2, new GetUserList()); // TIMEOUT, PUBLIC KEY

            break;
        case Message.NAME_RETURN_USER_LIST: // 0 | 1 | 0
            executeTask(message, 3, null); // NODE STRUCTURE, ENCRYPTED USER DATA, ENCRYPTED COMMON KEY

            break;
        case Message.NAME_REQUEST_CLIENT_ADDRESS: // - | 1 | 1
            executeTask(message, 2, new GetClientAddress()); // TIMEOUT, TARGET USER ID

            break;
        case Message.NAME_RETURN_CLIENT_ADDRESS: // 0 | 1 | 0
            executeTask(message, 2, null); // ENCRYPTED USER ADDRESS, ENCRYPTED COMMON KEY

            break;
        case Message.NAME_POST_USER_PROFILE: // 0 | 0 | 1
            executeTask(message, 5, new PostUserProfile()); // TIMEOUT, USER ID, USER DATA, TARGET USER ID, SECURE HASH

            break;
        case Message.NAME_UPDATE_USER_PROFILE: // 0 | 0 | 1
            executeTask(message, 4, new UpdateUserProfile()); // TIMEOUT, USER ID, USER DATA, SECURE HASH

            break;
        case Message.NAME_REMOVE_USER_PROFILE: // 0 | 0 | 3
            executeTask(message, 3, new RemoveUserProfile()); // TIMEOUT, USER ID, SECURE HASH

            break;
        case Message.NAME_RECEIVE_USER_PROFILE: // - | - | 0
            executeTask(message, 0, null); // NULL

            break;
        case Message.NAME_POST_CHAT_MESSAGE: // 0 | 0 | 3
            executeTask(message, 4, new PostChatMessage()); // TIMEOUT, USER ID, MESSAGE, SECURE HASH

            break;
        case Message.NAME_RECEIVE_CHAT_MESSAGE: // - | - | 0
            executeTask(message, 0, null); // NULL

            break;
        case Message.NAME_DUPLICATE: // - | - | 0
            executeTask(message, 0, null); // NULL

            break;
        case Message.NAME_ALIGNMENT: // - | 1 | 1
            executeTask(message, 1, new ConnectClient()); // USER DATA

            break;
        case Message.NAME_LEFT: // - | - | 1
            executeTask(message, 0, new DisconnectClient()); // NULL

            break;
        case Message.NAME_POST_CHAT_FILE: // 0 | 0 | 3
            executeTask(message, 5, new PostChatFile()); // TIMEOUT, USER ID, FILE ID, FILE NAME, SECURE HASH

            break;
        case Message.NAME_REQUEST_CHAT_FILE: // 0 | 0 | 3
            executeTask(message, 6, new RequestChatFile()); // TIMEOUT, USER ID, TARGET USER ID, TARGET FILE ID, PART NUMBER, SECURE HASH

            break;
        case Message.NAME_SEND_CHAT_FILE: // 0 | 0 | 3
            executeTask(message, Client.FORCE_STRING_COMMUNICATION ? 7 : 0, new SendChatFile()); // DATA

            break;
        case Message.NAME_RECEIVE_CHAT_FILE: // - | - | 0
            executeTask(message, 0, null); // NULL

            break;
        case Message.NAME_POST_OUROBOROS_NODE_DATA: // 0 | 0 | 1
            executeTask(message, Client.FORCE_STRING_COMMUNICATION ? 5 : 0, new PostOuroborosNodeData()); // DATA

            break;
        case Message.NAME_RECEIVE_OUROBOROS_NODE_DATA: // - | - | 0
            executeTask(message, 0, null); // NULL

            break;
        default:
            client.systemConsole.pushErrorLine("Invalid message received: Command \"" + message.command + "\" does not exist.");

            break;
        }
    }
}
