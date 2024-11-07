package Source.Tasks;

import Source.Client;
import Source.Stacks.Node;
import Source.Utils.Message;
import Source.Utils.Util;

public class PostOuroborosNodeData extends NetworkTask {
    @Override
    public PostOuroborosNodeData set(Client client, Node node, Message work) {
        super.set(client, node, work);

        if (Client.FORCE_STRING_COMMUNICATION) {
            setFromString();
        } else {
            setFromBinary();
        }

        return this;
    }

    void setFromString() {
        setProperties(Integer.parseInt(work.getStringData(0)), 10, "pst-on", "rec-on");

        String userId = work.getStringData(1).substring(1);
        String targetUserId = work.getStringData(2).substring(1);

        setSendUserIfNodeExist(targetUserId);

        if (isOriginalTask() || !myProfile.equals(targetUserId))
            return;

        skipSend = true;

        String content = work.getStringData(3);
        String secureHash = work.getStringData(4);

        if (!(work.check(1, Util.TYPE_USER_ID) && client.checkDataWithUserProfile(userId, content, secureHash)))
            return;

        client.ouroborosNodeStack.processData(Util.convertBase64ToByteArray(content), false);
    }

    void setFromBinary() {
        byte[] data = work.getByteData().clone();

        byte[] _timeout = Util.getNextDataOnSize(data, 4);
        data = Util.clearByteArrayOnSize(data, 4);
        int timeout = Util.convertByteArrayToInt(_timeout);

        setProperties(timeout, 10, "pst-on", "rec-on");

        byte[] _userId = Util.getNextDataOnSize(data, 16);
        data = Util.clearByteArrayOnSize(data, 16);
        String userId = Util.convertByteArrayToHexString(_userId);

        byte[] _targetUserId = Util.getNextDataOnSize(data, 16);
        data = Util.clearByteArrayOnSize(data, 16);
        String targetUserId = Util.convertByteArrayToHexString(_targetUserId);

        setSendUserIfNodeExist(targetUserId);

        if (isOriginalTask() || !myProfile.equals(targetUserId))
            return;

        skipSend = true;

        byte[] _content = Util.getNextDataOnSize(data);
        // byte[] _contentSize = Util.convertIntToByteArray(_content.length);
        data = Util.clearByteArrayOnSize(data);

        byte[] _secureHash = Util.getNextDataOnSize(data);
        // byte[] _secureHashSize = Util.convertIntToByteArray(_secureHash.length);
        data = Util.clearByteArrayOnSize(data);

        if (!client.checkDataWithUserProfile(userId, _content, _secureHash))
            return;

        client.ouroborosNodeStack.processData(_content, false);
    }

    @Override
    void send(Node node) {
        if (Client.FORCE_STRING_COMMUNICATION) {
            sendFromString(node);
        } else {
            sendFromBinary(node);
        }
    }

    void sendFromString(Node node) {
        String userId = work.getStringData(1);
        String targetUserId = work.getStringData(2);
        String content = work.getStringData(3);
        String secureHash = work.getStringData(4);

        node.sendMessage(requestCommand, work.id, String.valueOf(timeout - timeoutDecrement), userId, targetUserId, content, secureHash);
    }

    void sendFromBinary(Node node) {
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
