package Source.Tasks;

import Source.Client;
import Source.Stacks.Node;
import Source.Utils.Message;
import Source.Utils.Util;

public class RemoveUserProfile extends NetworkTask {
    public RemoveUserProfile set(Client client, Node node, Message work) {
        super.set(client, node, work);

        setProperties(Integer.parseInt(work.data[0]), 10, "rem-up", "rec-up");

        if (isOriginalTask())
            return this;

        String userId = work.data[1].substring(1);
        String secureHash = work.data[2];

        if (!work.check(1, Util.TYPE_USER_ID) || !client.checkDataWithUserProfile(userId, work.id, secureHash))
            return this;

        String userDisplay = client.userStack.get(userId).display();

        client.userStack.remove(userId, true);

        client.chatConsole.pushSubLine(Client.getCurrentTimeDisplay() + userDisplay + " has left the network.");

        return this;
    }

    @Override
    void send(Node node) {
        node.sendMessage(requestCommand, work.id, String.valueOf(timeout - timeoutDecrement), work.data[1], work.data[2]);
    }

    @Override
    void resolve(Node node, Message work) {
        updateNodeStore(node, "RES");
    }

    @Override
    void reject(Node node, Message work) {
        updateNodeStore(node, "DUP");
    }

    @Override
    void timeout(Node node) {
        updateNodeStore(node, "OUT");
    }

    @Override
    void response() {
        if (!isOriginalTask())
            node.sendMessage(returnCommand, work.id);
    }
}
