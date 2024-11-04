package Source.Tasks;

import Source.Client;
import Source.Stacks.Node;
import Source.Utils.Message;
import Source.Utils.Util;

public class PostChatFile extends NetworkTask {
    @Override
    public PostChatFile set(Client client, Node node, Message work) {
        super.set(client, node, work);

        setProperties(Integer.parseInt(work.getStringData(0)), 10, "pst-cf", "rec-cf");

        String userId = work.getStringData(1).substring(1);
        String fileId = work.getStringData(2).substring(1);
        String fileName = work.getStringData(3);
        String secureHash = work.getStringData(4);

        if (!(isOriginalTask() || (work.check(1, Util.TYPE_USER_ID) && work.check(2, Util.TYPE_FILE_ID)
                && client.checkDataWithUserProfile(userId, fileName + "#" + fileId, secureHash))))
            return this;

        fileName = Util.convertBase64ToString(fileName);

        if (!isOriginalTask())
            client.fileStack.addFileCache(userId, fileId, fileName);

        String userName = isOriginalTask() ? myProfile.name + " (ME)" : client.userStack.get(userId).display();
        String[] chatFileRequestLabel = new String[] { fileName };
        String[] chatFileRequestContent = new String[] { "/fr @" + userId + " #" + fileId };

        client.chatConsole.pushMainLine(Client.getCurrentTimeDisplay() + userName + ":\n$$$ (Click to set a command to download the file!)",
                chatFileRequestLabel, chatFileRequestContent);

        return this;
    }

    @Override
    void send(Node node) {
        String userId = work.getStringData(1);
        String fileId = work.getStringData(2);
        String fileName = work.getStringData(3);
        String secureHash = work.getStringData(4);

        node.sendMessage(requestCommand, work.id, String.valueOf(timeout - timeoutDecrement), userId, fileId, fileName, secureHash);
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
