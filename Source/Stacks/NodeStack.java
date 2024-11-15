package Source.Stacks;

import Source.Client;
import Source.Tasks.GetNodeList;
import Source.Utils.Message;

import java.util.*;
import java.util.stream.*;

public class NodeStack {
    Client client;

    List<Node> nodeStack = new ArrayList<Node>();

    public NodeStack(Client client) {
        this.client = client;
    }

    public void add(Node node) {
        nodeStack.add(node);
    }

    public void remove(Node node) {
        nodeStack.remove(node);
    }

    public List<Node> carbon() {
        return nodeStack.stream().collect(Collectors.toList());
    }

    public List<Node> carbon(Node exclude) {
        return nodeStack.stream().filter(node -> node != exclude).collect(Collectors.toList());
    }

    public Task getNodeList() {
        client.systemConsole.pushSubLine("Request node list...");

        Message message = new Message(client.systemConsole, "req-nl", "+", String.valueOf(Client.TIMEOUT));

        return client.taskStack.run(new GetNodeList().set(client, null, message));
    }

    public void setNodeDelay(String userId, int delay) {
        User user = client.userStack.test(userId);

        if (user != null) {
            if (user.node != null) {
                if (delay < Client.TIMEOUT) {
                    user.node.delay = delay;

                    client.systemConsole
                            .pushMainLine("Communication with user (@" + userId + ") will be delayed by " + String.valueOf(delay) + " milliseconds.");
                } else {
                    client.systemConsole.pushErrorLine("You can't set a delay greater than the timeout (" + String.valueOf(Client.TIMEOUT) + " milliseconds).");
                }
            } else {
                client.systemConsole.pushErrorLine("User (@" + userId + ") is not connected as a node.");
            }
        }
    }
}
