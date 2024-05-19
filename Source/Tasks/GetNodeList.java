package Source.Tasks;

import Source.Client;
import Source.Stacks.Node;
import Source.Utils.Message;

public class GetNodeList extends NetworkTask {
    @Override
    public GetNodeList set(Client client, Node node, Message work) {
        super.set(client, node, work);

        setProperties(Integer.parseInt(work.data[0]), 10, "req-nl", "ret-nl");

        return this;
    }

    @Override
    void send(Node node) {
        node.sendMessage(requestCommand, work.id, String.valueOf(timeout - timeoutDecrement));
    }

    @Override
    void resolve(Node node, Message work) {
        updateNodeStore(node, work.data[0]);
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
        String res = nodeStack.size() != 0 ? joinNodeStore(',', ';') : "NUL";

        if (isOriginalTask()) {
            // Usage is unknown...
        } else {
            node.sendMessage(returnCommand, work.id, res);
        }
    }
}
