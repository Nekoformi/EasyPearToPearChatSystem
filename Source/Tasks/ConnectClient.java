package Source.Tasks;

import Source.Client;
import Source.Stacks.Node;
import Source.Stacks.Task;
import Source.Utils.Message;

public class ConnectClient extends Task {
    public ConnectClient() {}

    public ConnectClient(Client client, Node node, Message work) {
        super(client, node, work);
    }

    public void run() {
        if (node != null && node.user == null) {
            node.user = client.userStack.add(work.getStringData(0), false);

            node.user.setNode(node);

            client.userStack.updateUserList();
        }

        done("!");
    }
}
