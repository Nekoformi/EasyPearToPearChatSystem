package Source.Tasks;

import Source.Client;
import Source.Stacks.Node;
import Source.Stacks.Task;
import Source.Stacks.User;
import Source.Utils.Message;

public class ConnectClient extends Task {
    public ConnectClient() {}

    public ConnectClient(Client client, Node node, Message work) {
        super(client, node, work);
    }

    public void run() {
        if (node != null && node.user == null) {
            User newUser = new User(client, work.getStringData(0));
            User currentUser = client.userStack.get(newUser.id);

            if (currentUser == null || currentUser.publicKey.equals(newUser.publicKey)) {
                node.user = client.userStack.add(newUser, false);

                node.user.setNode(node);

                client.userStack.updateUserList();
            } else {
                node.isError = true;
                node.user = newUser;

                node.closeNode();

                client.systemConsole.pushErrorLine("The user with a duplicate ID (@" + newUser.id + ") attempted to join.");
            }
        }

        done("!");
    }
}
