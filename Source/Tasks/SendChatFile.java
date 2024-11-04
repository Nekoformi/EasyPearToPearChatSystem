package Source.Tasks;

import Source.Client;
import Source.Stacks.Node;
import Source.Utils.Message;
import Source.Utils.Util;

public class SendChatFile extends NetworkTask {
    @Override
    public SendChatFile set(Client client, Node node, Message work) {
        super.set(client, node, work);

        byte[] data = work.getByteData().clone();

        byte[] _timeout = Util.getNextDataOnSize(data, 4);
        data = Util.clearByteArrayOnSize(data, 4);
        int timeout = Util.convertByteArrayToInt(_timeout);

        setProperties(timeout, 10, "snd-cf", "rec-cf");

        if (isOriginalTask())
            return this;

        byte[] _userId = Util.getNextDataOnSize(data, 16);
        data = Util.clearByteArrayOnSize(data, 16);
        String userId = Util.convertByteArrayToHexString(_userId);

        byte[] _targetUserId = Util.getNextDataOnSize(data, 16);
        data = Util.clearByteArrayOnSize(data, 16);
        String targetUserId = Util.convertByteArrayToHexString(_targetUserId);

        if (!myProfile.id.equals(targetUserId))
            return this;

        skipSend = true;

        byte[] _targetFileId = Util.getNextDataOnSize(data, 16);
        data = Util.clearByteArrayOnSize(data, 16);
        String targetFileId = Util.convertByteArrayToHexString(_targetFileId);

        byte[] _partNo = Util.getNextDataOnSize(data, 4);
        data = Util.clearByteArrayOnSize(data, 4);
        int partNo = Util.convertByteArrayToInt(_partNo);

        byte[] _content = Util.getNextDataOnSize(data);
        // byte[] _contentSize = Util.convertIntToByteArray(_content.length);
        data = Util.clearByteArrayOnSize(data);

        byte[] _secureHash = Util.getNextDataOnSize(data);
        // byte[] _secureHashSize = Util.convertIntToByteArray(_secureHash.length);
        data = Util.clearByteArrayOnSize(data);

        if (!client.checkDataWithUserProfile(userId, Util.concatByteArray(_targetUserId, _targetFileId, _partNo, _content), _secureHash))
            return this;

        client.fileStack.receive(userId, targetFileId, partNo, _content);

        return this;
    }

    @Override
    void send(Node node) {
        byte[] data = work.getByteData().clone();

        node.sendMessage(requestCommand, work.id,
                Util.concatByteArray(Util.convertIntToByteArray(timeout - timeoutDecrement), Util.clearByteArrayOnSize(data, 4)));
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
