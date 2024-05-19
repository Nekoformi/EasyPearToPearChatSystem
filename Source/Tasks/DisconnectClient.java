package Source.Tasks;

import Source.Client;
import Source.Stacks.Node;
import Source.Stacks.Task;
import Source.Utils.Message;

public class DisconnectClient extends Task {
    public DisconnectClient() {}

    public DisconnectClient(Client client, Node node, Message work) {
        super(client, node, work);
    }

    public void run() {
        if (node != null) {
            if (node.user != null) {
                node.user.setNode(null);

                node.user = null;
            }

            node.closeNode();

            client.userStack.updateUserList();

            if (work.data == null && client.nodeStack.carbon().size() == 0) {
                client.systemConsole.pushWarningLine("You have been disconnected from all nodes.");

                client.leaveNetwork();
            }
        }

        done("!");
    }
}
