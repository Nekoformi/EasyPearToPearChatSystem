package Source.Tasks;

import Source.Client;
import Source.Stacks.Node;
import Source.Stacks.Task;
import Source.Stacks.User;
import Source.Utils.Message;
import Source.Utils.Util;

import java.util.*;
import java.util.stream.*;

public class NetworkTask extends Task {
    protected List<Node> nodeStack;
    protected List<NodeStore> nodeStore;

    protected User myProfile;

    protected int timeout = 10000;
    protected int timeoutDecrement = 10;

    protected String requestCommand = "req";
    protected String returnCommand = "ret";

    protected boolean skipSend = false;

    protected class NodeStore {
        Node node;
        String[] data = null;

        public NodeStore(Node node) {
            this.node = node;
        }
    }

    protected class NodeStructure {
        String address = null;
        int port = -1;
        String[] data = null;
        NodeStructure[] children = null;
    }

    @Override
    public NetworkTask set(Client client, Node node, Message work) {
        super.set(client, node, work);

        nodeStack = client.nodeStack.carbon(node); // List: Deep Copy, Node: Shallow Copy
        nodeStore = new ArrayList<NodeStore>();
        myProfile = client.userStack.myProfile;

        return this;
    }

    @Override
    public synchronized void run() {
        super.run();

        if (nodeStack.size() != 0 && !skipSend) {
            nodeStack.forEach(node -> {
                nodeStore.add(new NodeStore(node));

                if (node.delay > 0) {
                    new Thread(() -> {
                        synchronized (this) {
                            try {
                                wait(node.delay);
                            } catch (InterruptedException e) {
                                // None
                            } catch (Exception e) {
                                client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
                            }

                            send(node);
                        }
                    }).start();
                } else {
                    send(node);
                }
            });

            stand();

            try {
                wait(timeout);
            } catch (InterruptedException e) {
                // None
            } catch (Exception e) {
                pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
            }
        }

        dead = true;

        if (!skipSend && nodeStack.size() != countNodeStore()) {
            pushErrorLine("Could not get replies from some nodes.");

            nodeStore.forEach(nodeStore -> {
                if (nodeStore.data == null)
                    timeout(nodeStore.node);
            });
        }

        response();

        done("!");
    }

    @Override
    public synchronized void receive(Node replyNode, Message replyWork) {
        if (dead)
            return;

        if (replyWork.command.equals(returnCommand))
            resolve(replyNode, replyWork);

        if (replyWork.command.equals("dup"))
            reject(replyNode, replyWork);

        if (nodeStack.size() == countNodeStore())
            notifyAll();
    }

    @SuppressWarnings("unused")
    void send(Node node) {
        // This is a function that is overridden by inherited classes.

        if (true)
            return;

        node.sendMessage(requestCommand, work.id, String.valueOf(timeout - timeoutDecrement), "PST");
    }

    @SuppressWarnings("unused")
    void stand() {
        // This is a function that is overridden by inherited classes.

        if (true)
            return;

        client.systemConsole.pushSubLine("Waiting for replies (#" + work.id + ")...");
    }

    @SuppressWarnings("unused")
    void resolve(Node node, Message work) {
        // This is a function that is overridden by inherited classes.

        if (true)
            return;

        updateNodeStore(node, work.getStringData());
    }

    @SuppressWarnings("unused")
    void reject(Node node, Message work) {
        // This is a function that is overridden by inherited classes.

        if (true)
            return;

        updateNodeStore(node, "NUL", "DUP");
    }

    @SuppressWarnings("unused")
    void timeout(Node node) {
        // This is a function that is overridden by inherited classes.

        if (true)
            return;

        updateNodeStore(node, "NUL", "OUT");
    }

    @SuppressWarnings("unused")
    void response() {
        // This is a function that is overridden by inherited classes.

        if (true)
            return;

        // Case 1: Connection destination information of the END NODE is required.

        {
            String res = nodeStack.size() != 0 ? joinNodeStore(',', ';') : "NUL";

            if (isOriginalTask()) {
                client.systemConsole.pushMainLine(res);
            } else {
                node.sendMessage(returnCommand, work.id, res, "OWN");
            }
        }

        // Case 2: Connection destination information of the END NODE is NOT required.

        {
            nodeStore = nodeStore.stream().filter(nodeStore -> !nodeStore.data[1].matches("DUP|OUT")).collect(Collectors.toList());

            String res = nodeStore.size() != 0 ? joinNodeStore(',', ';') : "NUL";

            if (isOriginalTask()) {
                client.systemConsole.pushMainLine(res);
            } else {
                node.sendMessage(returnCommand, work.id, res, "OWN");
            }
        }
    }

    protected boolean isOriginalTask() {
        return node == null;
    }

    protected void setSendUserIfNodeExist(String userId) {
        User user = client.userStack.test(userId);

        if (user != null)
            setSendUserIfNodeExist(user);
    }

    protected void setSendUserIfNodeExist(User user) {
        setSendUserIfNodeExist(Util.createExpandableList(user));
    }

    protected void setSendUserIfNodeExist(List<User> userList) {
        List<Node> newNodeStack = new ArrayList<Node>();

        userList.stream().forEach(user -> {
            if (user.node != null)
                newNodeStack.add(user.node);
        });

        if (newNodeStack.size() > 0)
            nodeStack = newNodeStack;
    }

    protected void pushErrorLine(String text) {
        client.systemConsole.pushErrorLine("Task \"" + requestCommand + "\" ~ \"" + returnCommand + "\" (#" + work.id + "): " + text);
    }

    protected void setProperties(int timeout, int timeoutDecrement, String requestCommand, String returnCommand) {
        this.timeout = timeout; // ex. Integer.parseInt(work.data[0])
        this.timeoutDecrement = timeoutDecrement;

        this.requestCommand = requestCommand;
        this.returnCommand = returnCommand;
    }

    protected void updateNodeStore(Node node, String... data) {
        NodeStore targetNodeStore = nodeStore.stream().filter(nodeStore -> nodeStore.node == node).findFirst().orElse(null);

        if (targetNodeStore != null) {
            targetNodeStore.data = data;
        } else {
            pushErrorLine("Unknown node selected.");
        }
    }

    protected int countNodeStore(String... data) {
        if (data == null) {
            return Math.toIntExact(nodeStore.stream().filter(nodeStore -> nodeStore.data != null).count());
        } else {
            // data[i] = <STRING>: Verify if the contents match
            // data[i] = <NULL>: Skip

            return Math.toIntExact(nodeStore.stream().filter(nodeStore -> {
                if (nodeStore.data == null || nodeStore.data.length == 0)
                    return false;

                for (int i = 0; i < data.length && i < nodeStore.data.length; i++) {
                    if (nodeStore.data[i] == null || (data[i] != null && !data[i].equals(nodeStore.data[i])))
                        return false;
                }

                return true;
            }).count());
        }
    }

    protected String joinNodeStore(char nodeSplit, char dataSplit) {
        StringBuffer res = new StringBuffer();
        String rec;

        for (int i = 0; i < nodeStore.size(); i++) {
            NodeStore sub = nodeStore.get(i);

            rec = String.join(String.valueOf(dataSplit), sub.data);
            rec = rec != null ? rec : "";

            res.append((i != 0 ? String.valueOf(nodeSplit) : "") + Util.getSocketInfoString(sub.node.socket) + "(" + rec + ")");
        }

        return res.toString(); // "ADDRESS(DATA; DATA; DATA ...), ADDRESS(ADDRESS(...); DATA ...), ADDRESS(ADDRESS(...), ADDRESS(...), ADDRESS(...) ...) ..."
    }

    protected NodeStructure[] analyzeNodeStructure(String nodeStructure, int recursiveDataIndex, char nodeSplit, char dataSplit) {
        List<NodeStructure> res = new ArrayList<NodeStructure>();

        int head = 0;
        int nest = 0;

        String item = null;
        String content = null;

        for (int i = 0; i < nodeStructure.length(); i++) {
            if (nodeStructure.charAt(i) == '(') {
                if (nest == 0) {
                    if (item == null && content == null) {
                        item = nodeStructure.substring(head, i);

                        head = i + 1;
                    } else {
                        res = null;

                        break;
                    }
                }

                nest++;
            } else if (nodeStructure.charAt(i) == ')') {
                nest--;

                if (nest == 0) {
                    if (item != null && content == null) {
                        content = nodeStructure.substring(head, i);

                        NodeStructure rec = setNodeStructure(item, content, recursiveDataIndex, nodeSplit, dataSplit);

                        if (rec != null) {
                            res.add(rec);

                            item = null;
                            content = null;
                        } else {
                            res = null;

                            break;
                        }
                    } else {
                        res = null;

                        break;
                    }

                    if (i == nodeStructure.length() - 1) {
                        break;
                    } else if (nodeStructure.charAt(i + 1) == nodeSplit) {
                        head = i + 2;
                    } else {
                        res = null;

                        break;
                    }
                } else if (nest < 0) {
                    res = null;

                    break;
                }
            }
        }

        if (res == null) {
            pushErrorLine("Incorrect node structure.");

            return null;
        }

        return res.size() != 0 ? res.toArray(new NodeStructure[res.size()]) : null;
    }

    protected NodeStructure setNodeStructure(String item, String content, int recursiveDataIndex, char nodeSplit, char dataSplit) {
        NodeStructure res = new NodeStructure();

        if (!item.matches(Util.IP_ADDRESS_PORT_REGEX))
            return null;

        String[] itemBuf = item.split(":");

        res.address = itemBuf[0];
        res.port = Integer.parseInt(itemBuf[1]);

        List<String> dataBuf = new ArrayList<String>();

        int head = 0;
        int nest = 0;

        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '(') {
                nest++;
            } else if (content.charAt(i) == ')') {
                nest--;
            } else if ((content.charAt(i) == dataSplit || i == content.length() - 1) && nest == 0) {
                int currentDataIndex = dataBuf.size();

                dataBuf.add(content.substring(head, i));

                head = i + 1;

                if (currentDataIndex == recursiveDataIndex)
                    res.children = analyzeNodeStructure(dataBuf.get(currentDataIndex), recursiveDataIndex, nodeSplit, dataSplit);
            }
        }

        if (nest != 0)
            return null;

        res.data = dataBuf.toArray(new String[dataBuf.size()]);

        return res;
    }

    protected NodeStructure[] listNodeStructure(NodeStructure[] nodeStructure) {
        List<NodeStructure> res = new ArrayList<NodeStructure>();

        if (nodeStructure == null)
            return null;

        for (int i = 0; i < nodeStructure.length; i++) {
            res.add(nodeStructure[i]);

            if (nodeStructure[i].children != null)
                Arrays.asList(listNodeStructure(nodeStructure[i].children)).forEach(child -> res.add(child));
        }

        return res.size() != 0 ? res.toArray(new NodeStructure[res.size()]) : null;
    }
}
