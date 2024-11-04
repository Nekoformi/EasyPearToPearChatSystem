package Source.Stacks;

import Source.Client;
import Source.Tasks.PostOuroborosNodeData;
import Source.Utils.Message;
import Source.Utils.Util;

import java.security.interfaces.*;
import java.util.*;

public class OuroborosNodeStack {
    public static int WAITING_TIME_MIN = 10;
    public static int WAITING_TIME_MAX = 1000;

    Client client;

    List<OuroborosNode> ouroborosNodeStack = new ArrayList<OuroborosNode>();

    class SendDataStore {
        User user;
        String userId;
        RSAPublicKey userPublicKey;
        byte[] encryptedCommonKey;
        byte[] encryptedMessage;

        public SendDataStore() {}

        public SendDataStore(byte[][] rec) {
            getSendData(rec, 0);
        }

        public SendDataStore(byte[][] rec, int index) {
            getSendData(rec, index);
        }

        public void getSendData(byte[][] rec, int index) {
            userId = Util.convertByteArrayToHexString(rec[index + 0]);
            userPublicKey = Util.getRsaPublicKeyFromByteArray(rec[index + 1]);
            encryptedCommonKey = rec[index + 2];
            encryptedMessage = rec[index + 3];

            user = client.userStack.get(userId);

            if (user == null) {
                client.systemConsole.pushWarningLine("The specified user (@" + userId + ") does not exist. Please update the user list with /update.");

                return;
            }

            if (!Arrays.equals(userPublicKey.getEncoded(), user.publicKey.getEncoded())) {
                client.systemConsole
                        .pushWarningLine("The public key (@" + userId + ") does not match the public key received. Please update the user list with /update.");

                return;
            }
        }
    }

    public OuroborosNodeStack(Client client) {
        this.client = client;
    }

    public synchronized OuroborosNode add(String targetUserId, int pickDummyNum) {
        User myself = client.userStack.myProfile;

        if (targetUserId.equals(myself.id)) {
            client.systemConsole.pushErrorLine(typeErrorYourself(targetUserId));

            return null;
        }

        User target = client.userStack.get(targetUserId);

        if (target == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownUser(targetUserId));

            return null;
        }

        OuroborosNode ouroborosNode = new OuroborosNode(client, target, pickDummyNum);

        if (ouroborosNode.check() != 0) {
            client.systemConsole.pushErrorLine("Failed to create a " + typeMap());

            return null;
        }

        if (get(targetUserId) == null) {
            ouroborosNodeStack.add(ouroborosNode);

            client.systemConsole.pushMainLine("Create a " + typeMap(ouroborosNode));
            client.systemConsole.pushMainLine(ouroborosNode.display());
        } else {
            update(ouroborosNode);
        }

        return ouroborosNode;
    }

    public synchronized OuroborosNode add(String mapStructureData) {
        OuroborosNode ouroborosNode = new OuroborosNode(client, mapStructureData);

        if (ouroborosNode.check() != 0) {
            client.systemConsole.pushErrorLine("Failed to generate a " + typeMap());

            return null;
        }

        if (get(ouroborosNode.target.id) == null) {
            ouroborosNodeStack.add(ouroborosNode);

            client.systemConsole.pushMainLine("Generate a " + typeMap(ouroborosNode));
            client.systemConsole.pushMainLine(ouroborosNode.display());
        } else {
            update(ouroborosNode);
        }

        return ouroborosNode;
    }

    public synchronized OuroborosNode add(String postedUserId, String mapStructureData) {
        User posted = client.userStack.get(postedUserId);

        if (posted == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownUser(postedUserId));

            return null;
        }

        OuroborosNode ouroborosNode = new OuroborosNode(client, posted, mapStructureData);

        if (ouroborosNode.check() != 0) {
            client.systemConsole.pushErrorLine("Failed to get a " + typeMap());

            return null;
        }

        if (get(postedUserId) == null) {
            ouroborosNodeStack.add(ouroborosNode);

            client.systemConsole.pushMainLine("Get a " + typeMap(ouroborosNode));
            client.systemConsole.pushMainLine(ouroborosNode.display());

            return ouroborosNode;
        } else {
            return update(ouroborosNode);
        }
    }

    public synchronized OuroborosNode update(OuroborosNode ouroborosNode) {
        if (ouroborosNode == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownOuroborosNode());

            return null;
        }

        OuroborosNode currentOuroborosNode = get(ouroborosNode.target.id);

        if (currentOuroborosNode == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownOuroborosNode(ouroborosNode.target.id));

            return null;
        }

        // ouroborosNodeStack.remove(currentOuroborosNode);
        // ouroborosNodeStack.add(ouroborosNode);

        currentOuroborosNode.set(ouroborosNode.map);

        client.systemConsole.pushMainLine("Update the " + typeMap(currentOuroborosNode));
        client.systemConsole.pushMainLine(currentOuroborosNode.display());

        return currentOuroborosNode;
    }

    public synchronized void remove(OuroborosNode ouroborosNode) {
        if (ouroborosNode == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownOuroborosNode());

            return;
        }

        client.systemConsole.pushMainLine("Remove the " + typeMap(ouroborosNode));

        ouroborosNodeStack.remove(ouroborosNode);
    }

    public synchronized void remove(String userId) {
        OuroborosNode ouroborosNode = get(userId);

        if (ouroborosNode == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownOuroborosNode(userId));

            return;
        }

        remove(ouroborosNode);
    }

    public OuroborosNode get(String userId) {
        User user = client.userStack.get(userId);

        // if (user == null) {
        // client.systemConsole.pushErrorLine(typeErrorUnknownUser(userId));

        // return null;
        // }

        return ouroborosNodeStack.stream().filter(node -> node.target == user).findFirst().orElse(null);
    }

    public void display(String userId) {
        OuroborosNode ouroborosNode = get(userId);

        if (ouroborosNode == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownOuroborosNode(userId));

            return;
        }

        client.systemConsole.pushMainLine("Show the " + typeMap(ouroborosNode));
        client.systemConsole.pushMainLine(ouroborosNode.display());
        client.systemConsole.pushMainLine("--- Map structure:");
        client.systemConsole.pushMainLine(ouroborosNode.encode());
    }

    public void insertNode(String targetMapUserId, String connectBeforeNodeUserId, String connectAfterNodeUserId, String insertNodeUserId, String flag) {
        User targetMapUser = client.userStack.get(targetMapUserId);

        if (targetMapUser == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownUser(targetMapUserId));

            return;
        }

        User connectBeforeNodeUser = client.userStack.get(connectBeforeNodeUserId);

        if (connectBeforeNodeUser == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownUser(connectBeforeNodeUserId));

            return;
        }

        User connectAfterNodeUser = client.userStack.get(connectAfterNodeUserId);

        if (connectAfterNodeUser == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownUser(connectAfterNodeUserId));

            return;
        }

        User insertNodeUser = client.userStack.get(insertNodeUserId);

        if (insertNodeUser == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownUser(insertNodeUserId));

            return;
        }

        if (OuroborosNode.getFlagType(flag) != 3) {
            client.systemConsole.pushErrorLine("You can't specify any flag other than \"DUM\", \"WAI\", or \"REP\".");

            return;
        }

        OuroborosNode targetOuroborosNode = get(targetMapUser.id);

        if (targetOuroborosNode == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownOuroborosNode(targetMapUser.id));

            return;
        }

        if (insertNodeUserId.equals(targetOuroborosNode.myself.id)) {
            client.systemConsole.pushErrorLine(typeErrorYourself(insertNodeUserId));

            return;
        }

        if (targetOuroborosNode.insertNode(connectBeforeNodeUser, connectAfterNodeUser, insertNodeUser, flag)) {
            client.systemConsole.pushMainLine("Insert the node (@" + insertNodeUserId + ") into the " + typeMap(targetOuroborosNode));
            client.systemConsole.pushMainLine(targetOuroborosNode.display());
        }
    }

    public void addNode(String targetMapUserId, String connectNodeUserId, String addNodeUserId, String flag) {
        User targetMapUser = client.userStack.get(targetMapUserId);

        if (targetMapUser == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownUser(targetMapUserId));

            return;
        }

        User connectNodeUser = client.userStack.get(connectNodeUserId);

        if (connectNodeUser == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownUser(connectNodeUserId));

            return;
        }

        User addNodeUser = client.userStack.get(addNodeUserId);

        if (addNodeUser == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownUser(addNodeUserId));

            return;
        }

        if (OuroborosNode.getFlagType(flag) != 3) {
            client.systemConsole.pushErrorLine("You can't specify any flag other than \"DUM\", \"WAI\", or \"REP\".");

            return;
        }

        OuroborosNode targetOuroborosNode = get(targetMapUser.id);

        if (targetOuroborosNode == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownOuroborosNode(targetMapUser.id));

            return;
        }

        if (addNodeUserId.equals(targetOuroborosNode.myself.id)) {
            client.systemConsole.pushErrorLine(typeErrorYourself(addNodeUserId));

            return;
        }

        if (targetOuroborosNode.addNode(connectNodeUser, addNodeUser, flag)) {
            client.systemConsole.pushMainLine("Add the node (@" + addNodeUserId + ") into the " + typeMap(targetOuroborosNode));
            client.systemConsole.pushMainLine(targetOuroborosNode.display());
        }
    }

    public void replaceNode(String targetMapUserId, String targetNodeUserId, String replaceNodeUserId) {
        User targetMapUser = client.userStack.get(targetMapUserId);

        if (targetMapUser == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownUser(targetMapUserId));

            return;
        }

        User targetNodeUser = client.userStack.get(targetNodeUserId);

        if (targetNodeUser == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownUser(targetNodeUserId));

            return;
        }

        User convertNodeUser = client.userStack.get(replaceNodeUserId);

        if (convertNodeUser == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownUser(replaceNodeUserId));

            return;
        }

        OuroborosNode targetOuroborosNode = get(targetMapUser.id);

        if (targetOuroborosNode == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownOuroborosNode(targetMapUser.id));

            return;
        }

        if (replaceNodeUserId.equals(targetOuroborosNode.myself.id)) {
            client.systemConsole.pushErrorLine(typeErrorYourself(replaceNodeUserId));

            return;
        }

        if (targetOuroborosNode.replaceNode(targetNodeUser, convertNodeUser)) {
            client.systemConsole
                    .pushMainLine("Replace the node (@" + targetNodeUserId + " → @" + replaceNodeUserId + ") into the " + typeMap(targetOuroborosNode));
            client.systemConsole.pushMainLine(targetOuroborosNode.display());
        }
    }

    public void rejectNode(String targetMapUserId, String targetNodeUserId) {
        User targetMapUser = client.userStack.get(targetMapUserId);

        if (targetMapUser == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownUser(targetMapUserId));

            return;
        }

        User targetNodeUser = client.userStack.get(targetNodeUserId);

        if (targetNodeUser == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownUser(targetNodeUserId));

            return;
        }

        OuroborosNode targetOuroborosNode = get(targetMapUser.id);

        if (targetOuroborosNode == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownOuroborosNode(targetMapUser.id));

            return;
        }

        if (targetNodeUserId.equals(targetOuroborosNode.myself.id) || targetNodeUserId.equals(targetOuroborosNode.target.id)) {
            client.systemConsole.pushErrorLine("You can't specify the sender or recipient.");

            return;
        }

        if (targetOuroborosNode.rejectNode(targetNodeUser)) {
            client.systemConsole.pushMainLine("Reject the node (@" + targetNodeUserId + ") into the " + typeMap(targetOuroborosNode));
            client.systemConsole.pushMainLine(targetOuroborosNode.display());
        }
    }

    public OuroborosNode postTextData(String targetUserId, String text) {
        OuroborosNode rec = get(targetUserId);

        if (rec == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownOuroborosNode(targetUserId));

            return null;
        }

        byte[] res = rec.createOuroborosData(text);

        processData(res, true);

        return rec;
    }

    public OuroborosNode postFileData(String targetUserId, String filePath) {
        // WIP

        OuroborosNode rec = get(targetUserId);

        if (rec == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownOuroborosNode(targetUserId));

            return null;
        }

        // byte[] res = rec.createOuroborosData(filePath);

        // processData(res, true);

        return rec;
    }

    public Task[] processData(byte[] data, boolean skipPeel) {
        byte[][] rec = OuroborosNode.decodeOuroborosData(client, data, skipPeel ? 2 : 0);

        if (rec == null)
            return null;

        List<Task> res = new ArrayList<Task>();

        byte[] messageId = rec[0];
        byte[] messageSize = rec[1];
        byte flag = rec[2][0];
        byte type = rec[3][0];

        switch (flag) {
        case OuroborosNode.FLAG_BYTE_POST: {
            displayDataSummary(rec, data.length);

            SendDataStore[] sendDataStore = generateSendDataStore(rec);

            for (SendDataStore item : sendDataStore) {
                byte[] rem = OuroborosNode.createOuroborosSendData(client, messageSize, item.encryptedCommonKey, item.encryptedMessage, item.userPublicKey);

                client.systemConsole.pushMainLine("Send ONN data to ... " + item.user.display());

                res.add(sendData(item.userId, rem));
            }
        }
            break;
        case OuroborosNode.FLAG_BYTE_DUMMY: {
            displayDataSummary(rec, data.length);

            SendDataStore[] sendDataStore = generateSendDataStore(rec);

            for (SendDataStore item : sendDataStore) {
                byte[] rem = OuroborosNode.createOuroborosSendData(client, messageSize, item.encryptedCommonKey, item.encryptedMessage, item.userPublicKey);

                client.systemConsole.pushMainLine("Send ONN data to ... " + item.user.display());

                res.add(sendData(item.userId, rem));
            }
        }
            break;
        case OuroborosNode.FLAG_BYTE_WAIT: {
            displayDataSummary(rec, data.length);

            SendDataStore[] sendDataStore = generateSendDataStore(rec);

            for (SendDataStore item : sendDataStore) {
                byte[] rem = OuroborosNode.createOuroborosSendData(client, messageSize, item.encryptedCommonKey, item.encryptedMessage, item.userPublicKey);

                client.systemConsole.pushMainLine("Send ONN data to ... " + item.user.display());

                res.add(sendData(item.userId, rem, Util.generateRandomInt(WAITING_TIME_MIN, WAITING_TIME_MAX)));
            }
        }
            break;
        case OuroborosNode.FLAG_BYTE_REPEAT: {
            displayDataSummary(rec, data.length);

            SendDataStore[] sendDataStore = generateSendDataStore(rec);

            for (SendDataStore item : sendDataStore) {
                byte[] rem = OuroborosNode.createOuroborosSendData(client, messageSize, item.encryptedCommonKey, item.encryptedMessage, item.userPublicKey);

                client.systemConsole.pushMainLine("Send ONN data to ... " + item.user.display());

                res.add(sendData(item.userId, rem));
                res.add(sendData(item.userId, rem, Util.generateRandomInt(WAITING_TIME_MIN, WAITING_TIME_MAX)));
            }
        }
            break;
        case OuroborosNode.FLAG_BYTE_RECEIVE: {
            displayDataSummary(rec, data.length);

            OuroborosNode rem = addOuroborosNodeFromData(rec);

            if (rem != null) {
                if (rem.addMessageStore(messageId)) {
                    if (type == OuroborosNode.MESSAGE_TYPE_STRING) {
                        String postUserName = rem.target.name;
                        String sendUserName = rem.myself.name;
                        String chatMessage = Util.convertByteArrayToString(rec[OuroborosNode.ONN_LAYER_3_PROPERTY_SIZE + 3]).replaceAll("\\\\n", "\n");

                        client.chatConsole.pushMainLine(
                                Client.getCurrentTimeDisplay() + postUserName + " → " + sendUserName + " [Private Message / Send with ONN]:\n" + chatMessage);
                    }
                }

                byte[] buf = rem.createOuroborosFinishData(messageId, messageSize);

                processData(buf, true);
            }
        }
            break;
        case OuroborosNode.FLAG_BYTE_FINISH: {
            displayDataSummary(rec, data.length);

            SendDataStore[] sendDataStore = generateSendDataStore(rec);

            for (SendDataStore item : sendDataStore) {
                byte[] rem = OuroborosNode.createOuroborosSendData(client, messageSize, item.encryptedCommonKey, item.encryptedMessage, item.userPublicKey);

                client.systemConsole.pushMainLine("Send ONN data to ... " + item.user.display());

                res.add(sendData(item.userId, rem));
            }
        }
            break;
        case OuroborosNode.FLAG_BYTE_DELETE:
            displayDataSummary(rec, data.length);

            client.systemConsole.pushMainLine("Discard ONN data.");

            break;
        case OuroborosNode.FLAG_BYTE_NULL:
            displayDataSummary(rec, data.length);

            client.systemConsole.pushWarningLine("Discard ONN data.");

            break;
        }

        return res.toArray(new Task[res.size()]);
    }

    void displayDataSummary(byte[][] rec, int messageSizeA) {
        String messageId = Util.convertByteArrayToHexString(rec[0]);
        int messageSizeB = Util.convertByteArrayToInt(rec[1]);
        String flag = OuroborosNode.convertFlagByteToName(rec[2][0]);
        // String type = Util.convertByteArrayToHexString(rec[3]);

        client.systemConsole
                .pushMainLine("Get ONN data ... ONN#" + messageId + " (" + String.valueOf(messageSizeA) + " byte / " + String.valueOf(messageSizeB) + " byte)");
        client.systemConsole.pushMainLine("Your job flag is ... " + flag);
    }

    SendDataStore[] generateSendDataStore(byte[][] rec) {
        if (rec == null || (rec.length - OuroborosNode.ONN_LAYER_3_PROPERTY_SIZE) % OuroborosNode.ONN_LAYER_4_DUM_DATA_SIZE != 0)
            return null;

        SendDataStore[] res = new SendDataStore[(rec.length - OuroborosNode.ONN_LAYER_3_PROPERTY_SIZE) / OuroborosNode.ONN_LAYER_4_DUM_DATA_SIZE];

        for (int i = 0; i < res.length; i++)
            res[i] = new SendDataStore(rec, OuroborosNode.ONN_LAYER_3_PROPERTY_SIZE + i * OuroborosNode.ONN_LAYER_4_DUM_DATA_SIZE);

        return res;
    }

    OuroborosNode addOuroborosNodeFromData(byte[][] rec) {
        String postUserId = Util.convertByteArrayToHexString(rec[OuroborosNode.ONN_LAYER_3_PROPERTY_SIZE + 0]);
        byte[] postUserPublicKey = rec[OuroborosNode.ONN_LAYER_3_PROPERTY_SIZE + 1];

        OuroborosNode res = add(postUserId, Util.convertByteArrayToString(rec[OuroborosNode.ONN_LAYER_3_PROPERTY_SIZE + 2]));

        if (res == null) {
            client.systemConsole.pushErrorLine("Failed to register received " + typeMap());

            return null;
        }

        if (!Arrays.equals(res.target.publicKey.getEncoded(), postUserPublicKey)) {
            client.systemConsole
                    .pushWarningLine("The public key (@" + postUserId + ") does not match the public key received. Please update the user list with /update.");

            // return null;
        }

        return res;
    }

    public Task sendData(String targetUserId, byte[] data) {
        if (Client.FORCE_STRING_COMMUNICATION) {
            return sendDataFromString(targetUserId, data);
        } else {
            return sendDataFromBinary(targetUserId, data);
        }
    }

    public Task sendData(String targetUserId, byte[] data, int delay) {
        if (Client.FORCE_STRING_COMMUNICATION) {
            return sendDataFromString(targetUserId, data, delay);
        } else {
            return sendDataFromBinary(targetUserId, data, delay);
        }
    }

    public Task sendDataFromString(String targetUserId, byte[] data) {
        return sendDataFromString(targetUserId, data, -1);
    }

    public Task sendDataFromString(String targetUserId, byte[] data, int delay) {
        String userId = "@" + client.userStack.myProfile.id;
        String content = Util.convertByteArrayToBase64(data);
        String secureHash = client.generateSecureHashWithMyProfile(content);

        targetUserId = "@" + targetUserId;

        Message message = new Message(client.systemConsole, "pst-on", "+", Client.TIMEOUT, userId, targetUserId, content, secureHash);

        return client.taskStack.run(new PostOuroborosNodeData().set(client, null, message), delay);
    }

    public Task sendDataFromBinary(String targetUserId, byte[] data) {
        return sendDataFromBinary(targetUserId, data, -1);
    }

    public Task sendDataFromBinary(String targetUserId, byte[] data, int delay) {
        byte[] _timeout = Util.convertIntToByteArray(Integer.parseInt(Client.TIMEOUT));
        byte[] _userId = Util.convertHexStringToByteArray(client.userStack.myProfile.id);
        byte[] _targetUserId = Util.convertHexStringToByteArray(targetUserId);
        byte[] _content = data;
        byte[] _contentSize = Util.convertIntToByteArray(_content.length);
        byte[] _secureHash = client.generateSecureHashWithMyProfile(_content);
        byte[] _secureHashSize = Util.convertIntToByteArray(_secureHash.length);

        Message message = new Message(client.systemConsole, "pst-on", "+",
                Util.concatByteArray(_timeout, _userId, _targetUserId, _contentSize, _content, _secureHashSize, _secureHash));

        // message = {
        // ### timeout[4], userId[16], targetUserId[16],
        // ### contentSize[4], content[...],
        // ### secureHashSize[4], secureHash[...]
        // };

        return client.taskStack.run(new PostOuroborosNodeData().set(client, null, message), delay);
    }

    String typeMap() {
        return "map of the Ouroboros Node Network.";
    }

    String typeMap(OuroborosNode ouroborosNode) {
        String postDisplay = ouroborosNode.myself.display();
        String sendDisplay = ouroborosNode.target.display();

        return "map of the Ouroboros Node Network: " + postDisplay + " ↔ " + sendDisplay;
    }

    String typeErrorYourself(User user) {
        return typeErrorYourself(user.id);
    }

    String typeErrorYourself(String userId) {
        return "You can't set yourself (@" + userId + ") as the recipient.";
    }

    String typeErrorUnknownUser(User user) {
        return typeErrorUnknownUser(user.id);
    }

    String typeErrorUnknownUser(String userId) {
        return "The specified user (@" + userId + ") does not exist.";
    }

    String typeErrorUnknownOuroborosNode() {
        return "The Ouroboros Node does not exist.";
    }

    String typeErrorUnknownOuroborosNode(User user) {
        return typeErrorUnknownOuroborosNode(user.id);
    }

    String typeErrorUnknownOuroborosNode(String userId) {
        return "The Ouroboros Node (send to @" + userId + ") does not exist.";
    }
}
