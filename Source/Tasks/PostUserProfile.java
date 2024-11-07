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

        setProperties(Integer.parseInt(work.getStringData(0)), 10, "pst-up", "rec-up");

        if (!work.check(1, Util.TYPE_USER_ID) || !work.check(3, Util.TYPE_USER_ID))
            return this;

        String userId = work.getStringData(1).substring(1);
        String targetUserId = work.getStringData(3).substring(1);

        setSendUserIfNodeExist(targetUserId);

        if (!myProfile.equals(targetUserId))
            return this;

        skipSend = true;

        String content = work.getStringData(2);
        String secureHash = work.getStringData(4);

        if (!client.checkDataWithMyProfile(content, secureHash))
            return this;

        User user = client.userStack.add(userId, content, true);

        client.chatConsole.pushSubLine(Client.getCurrentTimeDisplay() + user.display() + " has joined the network.");

        return this;
    }

    @Override
    void send(Node node) {
        String userId = work.getStringData(1);
        String content = work.getStringData(2);
        String targetUserId = work.getStringData(3);
        String secureHash = work.getStringData(4);

        node.sendMessage(requestCommand, work.id, String.valueOf(timeout - timeoutDecrement), userId, content, targetUserId, secureHash);
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
