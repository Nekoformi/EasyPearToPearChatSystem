package Source.Tasks;

import Source.Client;
import Source.Stacks.Node;
import Source.Utils.Message;
import Source.Utils.Util;

public class PostOuroborosNodeData extends NetworkTask {
    @Override
    public PostOuroborosNodeData set(Client client, Node node, Message work) {
        super.set(client, node, work);

        setProperties(Integer.parseInt(work.getStringData(0)), 10, "pst-on", "rec-on");

        if (isOriginalTask())
            return this;

        String userId = work.getStringData(1).substring(1);
        String targetUserId = work.getStringData(3).substring(1);

        if (!myProfile.id.equals(targetUserId))
            return this;

        skipSend = true;

        String content = work.getStringData(2);
        String secureHash = work.getStringData(4);

        if (!(work.check(1, Util.TYPE_USER_ID) && client.checkDataWithUserProfile(userId, content, secureHash)))
            return this;

        client.ouroborosNodeStack.processData(Util.convertBase64ToByteArray(content), false);

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
