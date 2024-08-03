package Source.Tasks;

import Source.Client;
import Source.Stacks.Node;
import Source.Utils.Message;
import Source.Utils.Util;

public class PostChatMessage extends NetworkTask {
    @Override
    public PostChatMessage set(Client client, Node node, Message work) {
        super.set(client, node, work);

        setProperties(Integer.parseInt(work.getStringData(0)), 10, "pst-cm", "rec-cm");

        String userId = work.getStringData(1).substring(1);
        String content = work.getStringData(2);
        String secureHash = work.getStringData(3);

        if (!(isOriginalTask() || (work.check(1, Util.TYPE_USER_ID) && client.checkDataWithUserProfile(userId, content, secureHash))))
            return this;

        String userName = isOriginalTask() ? myProfile.name + " (ME)" : client.userStack.get(userId).display();
        String chatMessage = Util.convertBase64ToString(content).replaceAll("\\\\n", "\n");

        client.chatConsole.pushMainLine(Client.getCurrentTimeDisplay() + userName + ":\n" + chatMessage);

        return this;
    }

    @Override
    void send(Node node) {
        String userId = work.getStringData(1);
        String content = work.getStringData(2);
        String secureHash = work.getStringData(3);

        node.sendMessage(requestCommand, work.id, String.valueOf(timeout - timeoutDecrement), userId, content, secureHash);
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
