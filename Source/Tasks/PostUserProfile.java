package Source.Tasks;

import Source.Client;
import Source.Stacks.Node;
import Source.Stacks.User;
import Source.Utils.Message;
import Source.Utils.Util;

public class PostUserProfile extends NetworkTask {
    @Override
    public PostUserProfile set(Client client, Node node, Message work) {
        super.set(client, node, work);

        setProperties(Integer.parseInt(work.data[0]), 10, "pst-up", "rec-up");

        if (!work.check(1, Util.TYPE_USER_ID) || !work.check(3, Util.TYPE_USER_ID))
            return this;

        String userId = work.data[1].substring(1);
        String targetUserId = work.data[3].substring(1);

        if (!myProfile.id.equals(targetUserId))
            return this;

        skipSend = true;

        String content = work.data[2];
        String secureHash = work.data[4];

        if (!client.checkDataWithMyProfile(content, secureHash))
            return this;

        User user = client.userStack.add(userId, content, true);

        client.chatConsole.pushSubLine(Client.getCurrentTimeDisplay() + user.display() + " has joined the network.");

        return this;
    }

    @Override
    void send(Node node) {
        node.sendMessage(requestCommand, work.id, String.valueOf(timeout - timeoutDecrement), work.data[1], work.data[2], work.data[3], work.data[4]);
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
