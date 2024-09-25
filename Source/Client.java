package Source;

import Source.Stacks.Node;
import Source.Stacks.NodeStack;
import Source.Stacks.Task;
import Source.Stacks.TaskStack;
import Source.Stacks.User;
import Source.Stacks.UserStack;
import Source.Tasks.DisconnectClient;
import Source.Tasks.GetClientAddress;
import Source.Tasks.PostChatMessage;
import Source.Utils.Catalog;
import Source.Utils.Console;
import Source.Utils.Message;
import Source.Utils.Util;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;

public class Client {
    // Note: Clientは実行されるプログラムに対応する。

    ClassLoader classLoader = this.getClass().getClassLoader();

    public Console systemConsole = new Console();
    public Console chatConsole = new Console();
    public Catalog memberCatalog = new Catalog();

    public UserStack userStack = new UserStack(this);
    public NodeStack nodeStack = new NodeStack(this);
    public TaskStack taskStack = new TaskStack(this);

    public NodeListener nodeListener;

    KeyManagerFactory serverKeyManagerFactory;
    KeyManagerFactory clientKeyManagerFactory;
    TrustManagerFactory authTrustManagerFactory;

    public static final String DEFAULT_SERVER_CERTIFICATE_FILE_PATH = "Source/Assets/DefaultServerCertificate.p12";
    public static final String DEFAULT_SERVER_CERTIFICATE_PASS_PHRASE = "Hello server!";
    public static final String DEFAULT_CLIENT_CERTIFICATE_FILE_PATH = "Source/Assets/DefaultClientCertificate.p12";
    public static final String DEFAULT_CLIENT_CERTIFICATE_PASS_PHRASE = "Hello client!";
    public static final String DEFAULT_AUTH_KEY_STORE_FILE_PATH = "Source/Assets/DefaultRootKeyStore.jks";
    public static final String DEFAULT_AUTH_KEY_STORE_PASS_PHRASE = "Hello root!";

    public boolean useSSL = false;

    public static String TIMEOUT = "10000";

    public Client() {}

    public Client(String name) {
        userStack.myProfile.set(null, name, null);
    }

    public void createNetwork(int listeningPort) {
        if (nodeListener != null) {
            systemConsole.pushErrorLine("If you want to join another network, you need to leave current network using /leave");

            return;
        }

        if (!useSSL) {
            systemConsole.pushMainLine("Create a network:\n" + showInetAddresses(1, listeningPort));

            nodeListener = new NodeListener(listeningPort);
        } else {
            systemConsole.pushMainLine("Create a secure network:\n" + showInetAddresses(1, listeningPort));

            nodeListener = new SecureNodeListener(listeningPort);
        }
    }

    public void joinNetwork(String address, int port, int listeningPort) {
        if (nodeListener != null) {
            systemConsole.pushErrorLine("If you want to join another network, you need to leave current network using /leave");

            return;
        }

        if (joinNetwork(address, port)) {
            systemConsole.pushMainLine("You have joined the network.");
            chatConsole.pushSubLine(getCurrentTimeDisplay() + "You have joined the network.");

            nodeListener = useSSL ? new SecureNodeListener(listeningPort) : new NodeListener(listeningPort);
        }
    }

    public boolean joinNetwork(String address, int port) {
        if (useSSL)
            return joinSecureNetwork(address, port);

        try {
            Socket socket = new Socket();

            systemConsole.pushSubLine("Try to join the network...");

            socket.connect(new InetSocketAddress(address, port), 10000);

            systemConsole.pushMainLine("Join the network: " + Util.getSocketInfoString(socket));

            connectNode(socket, false);

            return true;
        } catch (IOException e) {
            systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to join the network."));
        } catch (Exception e) {
            systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
        }

        return false;
    }

    public boolean joinSecureNetwork(String address, int port) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");

            sslContext.init(clientKeyManagerFactory.getKeyManagers(), authTrustManagerFactory.getTrustManagers(), null);

            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            SSLSocket sslSocket = (SSLSocket)sslSocketFactory.createSocket();

            systemConsole.pushSubLine("Try to join the network...");

            sslSocket.connect(new InetSocketAddress(address, port), 10000);

            systemConsole.pushMainLine("Join the secure network: " + Util.getSocketInfoString(sslSocket));

            sslSocket.startHandshake();

            connectNode(sslSocket, false);

            return true;
        } catch (IOException e) {
            systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to join the network."));
        } catch (Exception e) {
            systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
        }

        return false;
    }

    public void leaveNetwork() {
        Runnable done = () -> {
            if (nodeListener != null)
                nodeListener.done();

            userStack.clear();

            systemConsole.pushMainLine("You have left the network.");
            chatConsole.pushSubLine(getCurrentTimeDisplay() + "You have left the network.");
        };

        if (userStack.carbon().size() >= 1) {
            new Thread(() -> {
                try {
                    userStack.removeMyProfile().join();

                    disconnectAllNodes();

                    done.run();
                } catch (Exception e) {
                    systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
                }
            }).start();
        } else {
            done.run();
        }
    }

    public void connectNode(Socket socket, boolean isServer) {
        Node node = new Node(this, socket);

        if (isServer) {
            nodeStack.add(node);
        } else {
            nodeStack.add(node);

            if (userStack.carbon().size() <= 1) {
                new Thread(() -> {
                    synchronized (this) {
                        try {
                            while (node.user == null)
                                wait(10);

                            userStack.getUserList().join();
                            userStack.updateUserList();
                            userStack.postMyProfile();
                        } catch (Exception e) {
                            systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));

                            return;
                        }
                    }
                }).start();
            }
        }
    }

    public void connectNode(String userId) {
        User user = userStack.get(userId);

        if (user.node != null) {
            systemConsole.pushErrorLine("Already connected to the node: " + Util.getSocketInfoString(user.node.socket));

            return;
        }

        Message message = new Message(systemConsole, "req-ca", "+", Client.TIMEOUT, "@" + userId);

        taskStack.run(new GetClientAddress().set(this, null, message));
    }

    public void disconnectNode(Node node, boolean force) {
        if (!force && nodeStack.carbon().size() == 1) {
            systemConsole.pushErrorLine("You can't disconnect the only node connected to the network. If you want to leave the network, you must use /leave");

            return;
        }

        if (!force && nodeStack.carbon().size() == 0) {
            systemConsole.pushErrorLine("Something is wrong.");

            return;
        }

        Message message = new Message(systemConsole, "let", "+");

        node.sendMessage(message);

        taskStack.run(new DisconnectClient().set(this, node, message.set(null, null, "OWN")));
    }

    public void disconnectNode(String userId, boolean force) {
        User user = userStack.get(userId);

        if (user.node == null) {
            systemConsole.pushErrorLine("Not connected to the node: " + user.display());

            return;
        }

        disconnectNode(user.node, force);
    }

    public void disconnectAllNodes() {
        nodeStack.carbon().stream().forEach(node -> disconnectNode(node, true));
    }

    Task postChatMessage(String text) {
        String id = "@" + userStack.myProfile.id;
        String content = Util.convertStringToBase64(text);
        String secureHash = generateSecureHashWithMyProfile(content);

        Message message = new Message(systemConsole, "pst-cm", "+", Client.TIMEOUT, id, content, secureHash);

        return taskStack.run(new PostChatMessage().set(this, null, message));
    }

    public void executeCommand(String text) {
        if (text.startsWith("/")) {
            systemConsole.pushMainLine("▶ " + text);

            Message message = new Message(systemConsole, text, false).get();

            executeCommand(message);
        } else if (!text.equals("")) {
            systemConsole.pushMainLine("▶ /message " + text);

            postChatMessage(text);
        }
    }

    public void executeCommand(Message message) {
        if (message == null)
            return;

        switch (message.command) {
        case "create":
        case "c":
            if (message.check(0, Util.TYPE_IP_PORT))
                createNetwork(Integer.parseInt(message.getStringData(0)));

            break;
        case "join":
        case "j":
            if (message.check(0, Util.TYPE_IP_ADDRESS_PORT) && message.check(1, Util.TYPE_IP_PORT)) {
                String[] buf = message.getStringData(0).split(":");

                joinNetwork(buf[0], Integer.parseInt(buf[1]), Integer.parseInt(message.getStringData(1)));
            }

            break;
        case "leave":
        case "l":
            leaveNetwork();

            break;
        case "name":
        case "n":
            if (message.check(0, Util.TYPE_STRING)) {
                userStack.setMyName(message.join(0));

                systemConsole.pushMainLine("Hello " + userStack.myProfile.name + "!");
            }

            break;
        case "message":
        case "m":
            if (message.check(0, Util.TYPE_STRING))
                postChatMessage(message.join(0));

            break;
        case "update":
        case "u":
            userStack.getUserList();

            break;
        case "list":
        case "ls":
            userStack.display();

            break;
        case "clear-chat":
        case "clc":
            chatConsole.clearAllLine();

            systemConsole.pushSubLine("Cleared chat history.");

            break;
        case "clear-log":
        case "cll":
            systemConsole.clearAllLine();

            systemConsole.pushSubLine("Cleared log history.");

            break;
        case "clear":
        case "cls":
            chatConsole.clearAllLine();
            systemConsole.clearAllLine();

            systemConsole.pushSubLine("Cleared chat & log history.");

            break;
        case "connect":
            if (message.check(0, Util.TYPE_USER_ID))
                connectNode(message.getStringData(0).substring(1));

            break;
        case "disconnect":
            if (message.check(0, Util.TYPE_USER_ID))
                disconnectNode(message.getStringData(0).substring(1), false);

            break;
        default:
            systemConsole.pushErrorLine("Invalid command received: Command \"" + message.command + "\" does not exist.");

            break;
        }
    }

    public String generateSecureHashWithUserProfile(String id, String data) {
        User user = userStack.get(id);

        if (user == null) {
            systemConsole.pushErrorLine("Invalid data received: User (ID and public key) does not exist. Please get the user list with /update");

            return null;
        }

        return Util.convertByteArrayToBase64(Util.encryptByteArrayWithRsaPublicKey(Util.getSha256(data), user.publicKey));
    }

    public String generateSecureHashWithMyProfile(String data) {
        return Util.convertByteArrayToBase64(Util.encryptByteArrayWithRsaPrivateKey(Util.getSha256(data), userStack.myProfile.privateKey));
    }

    public boolean checkDataWithUserProfile(String id, String data, String secureHash) {
        User user = userStack.get(id);

        if (user == null) {
            systemConsole.pushErrorLine("Invalid data received: User (ID and public key) does not exist. Please get the user list with /update");

            return false;
        }

        byte[] hashA = Util.getSha256(data);
        byte[] hashB = Util.decryptByteArrayWithRsaPublicKey(Util.convertBase64ToByteArray(secureHash), user.publicKey);

        if (Arrays.equals(hashA, hashB)) {
            return true;
        } else {
            systemConsole.pushErrorLine("Invalid data received: The hashes do not match. The data may be corrupted or tampered with.");

            return false;
        }
    }

    public boolean checkDataWithMyProfile(String data, String secureHash) {
        byte[] hashA = Util.getSha256(data);
        byte[] hashB = Util.decryptByteArrayWithRsaPrivateKey(Util.convertBase64ToByteArray(secureHash), userStack.myProfile.privateKey);

        if (Arrays.equals(hashA, hashB)) {
            return true;
        } else {
            systemConsole.pushErrorLine("Invalid data received: The hashes do not match. The data may be corrupted or tampered with.");

            return false;
        }
    }

    public static String getCurrentTimeDisplay() {
        return Util.getCurrentTimeDisplay("HH:mm:ss") + " | ";
    }

    String[][] getInetAddresses() {
        List<String[]> res = new ArrayList<String[]>(); // DISPLAY NAME, NAME, ADDRESS...

        try {
            {
                List<String> rec = new ArrayList<String>();

                rec.add("Local");
                rec.add("localhost");
                rec.add(InetAddress.getLocalHost().getHostAddress());

                res.add(rec.toArray(new String[rec.size()]));
            }

            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();

            if (nis == null)
                return null;

            while (nis.hasMoreElements()) {
                List<String> rec = new ArrayList<String>();

                NetworkInterface ni = nis.nextElement();

                rec.add(ni.getDisplayName());
                rec.add(ni.getName());

                Enumeration<InetAddress> ias = ni.getInetAddresses();

                while (ias.hasMoreElements()) {
                    InetAddress ia = ias.nextElement();

                    rec.add(ia.getHostAddress());
                }

                res.add(rec.toArray(new String[rec.size()]));
            }
        } catch (UnknownHostException e) {
            systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to get localhost address."));
        } catch (SocketException e) {
            systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to get some IP addresses."));
        } catch (Exception e) {
            systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
        }

        return res.size() != 0 ? res.toArray(new String[res.size()][]) : null;
    }

    String getGoodInetAddress(String badInetAddress) {
        String en = null;
        String wl = null;

        String[][] inetAddresses = getInetAddresses();

        if (inetAddresses == null)
            return badInetAddress;

        for (String[] inetAddress : inetAddresses) {
            String name = inetAddress[1];

            if (name.matches("(en|wl).*") && inetAddress.length > 2) {
                for (int i = 2; i < inetAddress.length; i++) {
                    String address = inetAddress[i];

                    if (address.matches(Util.IP_ADDRESS_REGEX)) {
                        if (name.matches("en.*")) {
                            en = address;
                        } else if (name.matches("wl.*")) {
                            wl = address;
                        }

                        break;
                    }
                }
            }
        }

        if (en != null) {
            return en;
        } else if (wl != null) {
            return wl;
        } else {
            return badInetAddress;
        }
    }

    String showInetAddresses(int indent, int port) {
        StringBuffer res = new StringBuffer();

        String[][] inetAddresses = getInetAddresses();
        String portDisplay = port >= 0 ? ":" + Integer.toString(port) : "";

        if (inetAddresses != null) {
            for (int i = 0; i < inetAddresses.length; i++) {
                res.append((i == 0 ? "" : "\n") + Util.indent(indent) + inetAddresses[i][0] + " (" + inetAddresses[i][1] + "):");

                if (inetAddresses[i].length > 2) {
                    for (int j = 2; j < inetAddresses[i].length; j++)
                        res.append("\n" + Util.indent(indent + 1) + inetAddresses[i][j] + portDisplay);
                } else {
                    res.append("\n" + Util.indent(indent + 1) + "NULL");
                }
            }

            return res.toString();
        } else {
            return "NULL";
        }
    }

    public void initializeSSL() {
        useSSL = true;

        setServerCertificate(null, null);
        setClientCertificate(null, null);
        setAuthKeyStore(null, null);
    }

    public void initializeSSL(String s_cfp, String s_cpp, String c_cfp, String c_cpp, String a_kfp, String a_kpp) {
        useSSL = true;

        setServerCertificate(s_cfp, s_cpp);
        setClientCertificate(c_cfp, c_cpp);
        setAuthKeyStore(a_kfp, a_kpp);
    }

    public void setServerCertificate(String filePath, String passPhrase) {
        if (filePath == null && passPhrase == null) {
            filePath = DEFAULT_SERVER_CERTIFICATE_FILE_PATH;
            passPhrase = DEFAULT_SERVER_CERTIFICATE_PASS_PHRASE;

            try {
                serverKeyManagerFactory = Util.setKeyManagerFactory(classLoader.getResourceAsStream(filePath), passPhrase);
            } catch (Exception e) {
                systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to set server certificate."));
            }
        } else {
            if (filePath == null) {
                systemConsole.pushErrorLine("No server certificate specified.");

                return;
            }

            try {
                serverKeyManagerFactory = Util.setKeyManagerFactory(filePath, passPhrase);
            } catch (Exception e) {
                systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to set server certificate."));
            }
        }
    }

    public void setClientCertificate(String filePath, String passPhrase) {
        if (filePath == null && passPhrase == null) {
            filePath = DEFAULT_CLIENT_CERTIFICATE_FILE_PATH;
            passPhrase = DEFAULT_CLIENT_CERTIFICATE_PASS_PHRASE;

            try {
                clientKeyManagerFactory = Util.setKeyManagerFactory(classLoader.getResourceAsStream(filePath), passPhrase);
            } catch (Exception e) {
                systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to set client certificate."));
            }
        } else {
            if (filePath == null) {
                systemConsole.pushErrorLine("No client certificate specified.");

                return;
            }

            try {
                clientKeyManagerFactory = Util.setKeyManagerFactory(filePath, passPhrase);
            } catch (Exception e) {
                systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to set client certificate."));
            }
        }
    }

    public void setAuthKeyStore(String filePath, String passPhrase) {
        if (filePath == null && passPhrase == null) {
            filePath = DEFAULT_AUTH_KEY_STORE_FILE_PATH;
            passPhrase = DEFAULT_AUTH_KEY_STORE_PASS_PHRASE;

            try {
                authTrustManagerFactory = Util.setTrustManagerFactory(classLoader.getResourceAsStream(filePath), passPhrase);
            } catch (Exception e) {
                systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to set auth key store."));
            }
        } else {
            if (filePath == null) {
                systemConsole.pushErrorLine("No auth key store specified.");

                return;
            }

            try {
                authTrustManagerFactory = Util.setTrustManagerFactory(filePath, passPhrase);
            } catch (Exception e) {
                systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to set auth key store."));
            }
        }
    }

    public class NodeListener extends Thread {
        protected volatile boolean done = false;

        String address;
        int port = -1;

        public NodeListener(int port) {
            systemConsole.pushSubLine("Start listening to new node.");

            this.port = port;

            start();
        }

        public String getAddress() {
            if (address != null && port != -1) {
                return address + ":" + String.valueOf(port);
            } else {
                return null;
            }
        }

        public void run() {
            ServerSocket serverSocket = null;

            try {
                serverSocket = new ServerSocket(port);

                address = getGoodInetAddress(serverSocket.getInetAddress().getHostAddress());

                systemConsole.pushMainLine("Listening: " + address + ":" + Integer.toString(port));

                while (!done) {
                    systemConsole.pushSubLine("Waiting for new node connection...");

                    Socket socket = serverSocket.accept();

                    if (!done)
                        connectNode(socket, true);
                }
            } catch (IOException e) {
                systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to listening to new node."));
            } catch (Exception e) {
                systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
            } finally {
                if (serverSocket != null) {
                    systemConsole.pushSubLine("Finish waiting for new node connection...");

                    try {
                        serverSocket.close();
                    } catch (Exception e) {
                        systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
                    }
                }

                nodeListener = null;
            }
        }

        public synchronized void done() {
            systemConsole.pushSubLine("Finish listening to new node.");

            done = true;

            try {
                Socket socket = new Socket("127.0.0.1", port);

                socket.close();
            } catch (Exception e) {
                systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
            }
        }
    }

    public class SecureNodeListener extends NodeListener {
        public SecureNodeListener(int port) {
            super(port);
        }

        public void run() {
            SSLServerSocket sslServerSocket = null;

            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");

                sslContext.init(serverKeyManagerFactory.getKeyManagers(), authTrustManagerFactory.getTrustManagers(), null);

                SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();

                sslServerSocket = (SSLServerSocket)sslServerSocketFactory.createServerSocket(port);

                sslServerSocket.setNeedClientAuth(true);

                address = getGoodInetAddress(sslServerSocket.getInetAddress().getHostAddress());

                systemConsole.pushMainLine("Listening: " + address + ":" + Integer.toString(port));

                while (!done) {
                    systemConsole.pushSubLine("Waiting for new node connection...");

                    SSLSocket sslSocket = (SSLSocket)sslServerSocket.accept();

                    if (!done)
                        connectNode(sslSocket, true);
                }
            } catch (IOException e) {
                systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to listening to new node."));
            } catch (Exception e) {
                systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
            } finally {
                if (sslServerSocket != null) {
                    systemConsole.pushSubLine("Finish waiting for new node connection...");

                    try {
                        sslServerSocket.close();
                    } catch (Exception e) {
                        systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
                    }
                }

                nodeListener = null;
            }
        }

        public synchronized void done() {
            systemConsole.pushSubLine("Finish listening to new node.");

            done = true;

            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");

                sslContext.init(clientKeyManagerFactory.getKeyManagers(), authTrustManagerFactory.getTrustManagers(), null);

                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                SSLSocket sslSocket = (SSLSocket)sslSocketFactory.createSocket("127.0.0.1", port);

                sslSocket.close();
            } catch (Exception e) {
                systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
            }
        }
    }
}
