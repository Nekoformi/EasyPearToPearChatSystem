package Source.Tasks;

import Source.Client;
import Source.Stacks.Node;
import Source.Utils.Message;
import Source.Utils.Util;

public class RequestChatFile extends NetworkTask {
    @Override
    public RequestChatFile set(Client client, Node node, Message work) {
        super.set(client, node, work);

        setProperties(Integer.parseInt(work.getStringData(0)), 10, "req-cf", "rec-cf");

        if (isOriginalTask())
            return this;

        String userId = work.getStringData(1).substring(1);
        String targetUserId = work.getStringData(2).substring(1);

        if (!myProfile.id.equals(targetUserId))
            return this;

        skipSend = true;

        String targetFileId = work.getStringData(3).substring(1);
        String partNo = work.getStringData(4);
        String secureHash = work.getStringData(5);

        if (!((work.check(1, Util.TYPE_USER_ID) && work.check(2, Util.TYPE_USER_ID) && work.check(3, Util.TYPE_FILE_ID)
                && client.checkDataWithUserProfile(userId, partNo + "#" + targetFileId + "@" + targetUserId, secureHash))))
            return this;

        client.fileStack.send(userId, targetFileId, Integer.parseInt(partNo));

        return this;
    }

    @Override
    void send(Node node) {
        String userId = work.getStringData(1);
        String targetUserId = work.getStringData(2);
        String targetFileId = work.getStringData(3);
        String partNo = work.getStringData(4);
        String secureHash = work.getStringData(5);

        node.sendMessage(requestCommand, work.id, String.valueOf(timeout - timeoutDecrement), userId, targetUserId, targetFileId, partNo, secureHash);
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
