package Source.Tasks;

import Source.Client;
import Source.Stacks.Node;
import Source.Utils.Message;
import Source.Utils.Util;

public class UpdateUserProfile extends NetworkTask {
    @Override
    public UpdateUserProfile set(Client client, Node node, Message work) {
        super.set(client, node, work);

        setProperties(Integer.parseInt(work.data[0]), 10, "upd-up", "rec-up");

        if (isOriginalTask())
            return this;

        String userId = work.data[1].substring(1);
        String content = work.data[2];
        String secureHash = work.data[3];

        if (!work.check(1, Util.TYPE_USER_ID) || !client.checkDataWithUserProfile(userId, content, secureHash))
            return this;

        client.userStack.update(userId, content, true);

        return this;
    }

    @Override
    void send(Node node) {
        node.sendMessage(requestCommand, work.id, String.valueOf(timeout - timeoutDecrement), work.data[1], work.data[2], work.data[3]);
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
