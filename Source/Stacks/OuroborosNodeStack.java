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

        if (myself.equals(targetUserId)) {
            client.systemConsole.pushErrorLine(typeErrorYourself(targetUserId));

            return null;
        }

        User target = client.userStack.test(targetUserId);

        if (target == null)
            return null;

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
        User posted = client.userStack.test(postedUserId);

        if (posted == null)
            return null;

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

    public OuroborosNode test(String userId) {
        User user = client.userStack.test(userId);

        if (user == null)
            return null;

        return ouroborosNodeStack.stream().filter(node -> node.target == user).findFirst().orElse(null);
    }

    public OuroborosNode get(String userId) {
        User user = client.userStack.get(userId);

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
        User targetMapUser = client.userStack.test(targetMapUserId);

        if (targetMapUser == null)
            return;

        User connectBeforeNodeUser = client.userStack.test(connectBeforeNodeUserId);

        if (connectBeforeNodeUser == null)
            return;

        User connectAfterNodeUser = client.userStack.test(connectAfterNodeUserId);

        if (connectAfterNodeUser == null)
            return;

        User insertNodeUser = client.userStack.test(insertNodeUserId);

        if (insertNodeUser == null)
            return;

        if (OuroborosNode.getFlagType(flag) != 3) {
            client.systemConsole.pushErrorLine("You can't specify any flag other than \"DUM\", \"WAI\", or \"REP\".");

            return;
        }

        OuroborosNode targetOuroborosNode = get(targetMapUser.id);

        if (targetOuroborosNode == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownOuroborosNode(targetMapUser.id));

            return;
        }

        if (targetOuroborosNode.myself.equals(insertNodeUserId)) {
            client.systemConsole.pushErrorLine(typeErrorYourself(insertNodeUserId));

            return;
        }

        if (targetOuroborosNode.insertNode(connectBeforeNodeUser, connectAfterNodeUser, insertNodeUser, flag)) {
            client.systemConsole.pushMainLine("Insert the node (@" + insertNodeUserId + ") into the " + typeMap(targetOuroborosNode));
            client.systemConsole.pushMainLine(targetOuroborosNode.display());
        }
    }

    public void addNode(String targetMapUserId, String connectNodeUserId, String addNodeUserId, String flag) {
        User targetMapUser = client.userStack.test(targetMapUserId);

        if (targetMapUser == null)
            return;

        User connectNodeUser = client.userStack.test(connectNodeUserId);

        if (connectNodeUser == null)
            return;

        User addNodeUser = client.userStack.test(addNodeUserId);

        if (addNodeUser == null)
            return;

        if (OuroborosNode.getFlagType(flag) != 3) {
            client.systemConsole.pushErrorLine("You can't specify any flag other than \"DUM\", \"WAI\", or \"REP\".");

            return;
        }

        OuroborosNode targetOuroborosNode = get(targetMapUser.id);

        if (targetOuroborosNode == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownOuroborosNode(targetMapUser.id));

            return;
        }

        if (targetOuroborosNode.myself.equals(addNodeUserId)) {
            client.systemConsole.pushErrorLine(typeErrorYourself(addNodeUserId));

            return;
        }

        if (targetOuroborosNode.addNode(connectNodeUser, addNodeUser, flag)) {
            client.systemConsole.pushMainLine("Add the node (@" + addNodeUserId + ") into the " + typeMap(targetOuroborosNode));
            client.systemConsole.pushMainLine(targetOuroborosNode.display());
        }
    }

    public void replaceNode(String targetMapUserId, String targetNodeUserId, String replaceNodeUserId) {
        User targetMapUser = client.userStack.test(targetMapUserId);

        if (targetMapUser == null)
            return;

        User targetNodeUser = client.userStack.test(targetNodeUserId);

        if (targetNodeUser == null)
            return;

        User convertNodeUser = client.userStack.test(replaceNodeUserId);

        if (convertNodeUser == null)
            return;

        OuroborosNode targetOuroborosNode = get(targetMapUser.id);

        if (targetOuroborosNode == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownOuroborosNode(targetMapUser.id));

            return;
        }

        if (targetOuroborosNode.myself.equals(replaceNodeUserId)) {
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
        User targetMapUser = client.userStack.test(targetMapUserId);

        if (targetMapUser == null)
            return;

        User targetNodeUser = client.userStack.test(targetNodeUserId);

        if (targetNodeUser == null)
            return;

        OuroborosNode targetOuroborosNode = get(targetMapUser.id);

        if (targetOuroborosNode == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownOuroborosNode(targetMapUser.id));

            return;
        }

        if (targetOuroborosNode.myself.equals(targetNodeUserId) || targetOuroborosNode.target.equals(targetNodeUserId)) {
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
        OuroborosNode ouroborosNode = get(targetUserId);

        if (ouroborosNode == null) {
            client.systemConsole.pushErrorLine(typeErrorUnknownOuroborosNode(targetUserId));

            return null;
        }

        FileStore fileStore = client.fileStack.addPrivateFileStore(filePath, ouroborosNode.target, OuroborosNode.MAX_MESSAGE_DATA_SIZE - (16 + 4 + 4));

        if (fileStore == null) {
            client.systemConsole.pushErrorLine("Failed to prepare file.");

            return null;
        }

        byte[] fileId = Util.convertHexStringToByteArray(fileStore.getFileId());
        byte[] filePartNo = Util.convertIntToByteArray(-1);
        byte[] fileSumPart = Util.convertIntToByteArray(fileStore.getFileSumPart());
        byte[] fileName = Util.convertStringToByteArray(fileStore.getFileName());
        byte[] fileNameSize = Util.convertIntToByteArray(fileName.length);

        byte[] res = ouroborosNode.createOuroborosData(Util.concatByteArray(fileId, filePartNo, fileSumPart, fileNameSize, fileName),
                OuroborosNode.MESSAGE_TYPE_BINARY_SND);

        // res = {
        // ### fileId[16], filePartNo[4], fileSumPart[4],
        // ### fileNameSize[4], fileName[...]
        // };

        processData(res, true);

        return ouroborosNode;
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
            byte[] message = rec[OuroborosNode.ONN_LAYER_3_PROPERTY_SIZE + 3];

            displayDataSummary(rec, data.length);

            OuroborosNode rem = addOuroborosNodeFromData(rec);

            if (rem != null) {
                if (true /* rem.addMessageStore(messageId) */) {
                    if (type == OuroborosNode.MESSAGE_TYPE_STRING) {
                        String chatMessage = Util.convertByteArrayToString(message).replaceAll("\\\\n", "\n");

                        client.chatConsole.pushMainLine(typeChatMessage(rem, chatMessage));

                        processData(rem.createOuroborosFinishData(messageId, messageSize), true);
                    } else {
                        byte[] buf;

                        try {
                            switch (type) {
                            case OuroborosNode.MESSAGE_TYPE_BINARY_SND: {
                                byte[] nextMessage = receiveFile(rem, message);

                                if (nextMessage != null) {
                                    buf = rem.createOuroborosData(messageId, messageSize, nextMessage, OuroborosNode.MESSAGE_TYPE_BINARY_REQ);
                                } else {
                                    buf = rem.createOuroborosFinishData(messageId, messageSize);
                                }
                            }
                                break;
                            case OuroborosNode.MESSAGE_TYPE_BINARY_REQ: {
                                byte[] nextMessage = sendFile(rem, message);

                                if (nextMessage != null) {
                                    buf = rem.createOuroborosData(messageId, messageSize, nextMessage, OuroborosNode.MESSAGE_TYPE_BINARY_SND);
                                } else {
                                    buf = rem.createOuroborosFinishData(messageId, messageSize);
                                }
                            }
                                break;
                            default:
                                buf = rem.createOuroborosFinishData(messageId, messageSize);

                                break;
                            }
                        } catch (Exception e) {
                            client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));

                            buf = rem.createOuroborosFinishData(messageId, messageSize);
                        }

                        processData(buf, true);
                    }
                }
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

    byte[] receiveFile(OuroborosNode ouroborosNode, byte[] message) throws Exception {
        String userId = ouroborosNode.target.id;

        byte[] _fileId = Util.getNextDataOnSize(message, 16);
        message = Util.clearByteArrayOnSize(message, 16);
        String fileId = Util.convertByteArrayToHexString(_fileId);

        byte[] _filePartNo = Util.getNextDataOnSize(message, 4);
        message = Util.clearByteArrayOnSize(message, 4);
        int filePartNo = Util.convertByteArrayToInt(_filePartNo);

        FileCache fileCache = client.fileStack.getFileCache(userId, fileId);

        if (fileCache == null && filePartNo == -1) {
            byte[] _fileSumPart = Util.getNextDataOnSize(message, 4);
            message = Util.clearByteArrayOnSize(message, 4);
            int fileSumPart = Util.convertByteArrayToInt(_fileSumPart);

            byte[] _fileName = Util.getNextDataOnSize(message);
            message = Util.clearByteArrayOnSize(message);
            String fileName = Util.convertByteArrayToString(_fileName);

            fileCache = client.fileStack.addFileCache(userId, fileId, fileName);

            if (fileCache != null && fileCache.prepareFileData()) {
                int rec = fileCache.setSumPart(fileSumPart);

                if (rec > -1) {
                    client.chatConsole.pushSubLine(typeChatMessage(ouroborosNode, "Start receiving the file: " + fileName));

                    return Util.concatByteArray(_fileId, Util.convertIntToByteArray(0));
                }
            }
        } else if (fileCache != null) {
            int rec = fileCache.writeFileData(filePartNo, message);

            if (rec > -1) {
                return Util.concatByteArray(_fileId, Util.convertIntToByteArray(rec));
            } else if (rec == -1) {
                client.chatConsole.pushSubLine(typeChatMessage(ouroborosNode, "Finish receiving the file: " + fileCache.getFileName()));

                return Util.concatByteArray(_fileId, Util.convertIntToByteArray(-2));
            }
        }

        if (fileCache == null) {
            client.chatConsole.pushErrorLine(typeChatMessage(ouroborosNode, "Failed to receive file."));
        } else {
            client.chatConsole.pushErrorLine(typeChatMessage(ouroborosNode, "Failed to receive file: " + fileCache.getFileName()));
        }

        return null;
    }

    byte[] sendFile(OuroborosNode ouroborosNode, byte[] message) throws Exception {
        String userId = ouroborosNode.target.id;

        byte[] _fileId = Util.getNextDataOnSize(message, 16);
        message = Util.clearByteArrayOnSize(message, 16);
        String fileId = Util.convertByteArrayToHexString(_fileId);

        byte[] _filePartNo = Util.getNextDataOnSize(message, 4);
        message = Util.clearByteArrayOnSize(message, 4);
        int filePartNo = Util.convertByteArrayToInt(_filePartNo);

        byte[] fileData = client.fileStack.read(userId, fileId, filePartNo);

        if (fileData == null) {
            client.chatConsole.pushErrorLine(typeChatMessage(ouroborosNode, "Failed to send file."));

            return null;
        }

        if (filePartNo == -2) {
            FileStore fileStore = client.fileStack.getFileStore(fileId);

            if (fileStore != null) {
                client.chatConsole.pushSubLine(typeChatMessage(ouroborosNode, "Finish sending the file: " + fileStore.getFileName()));
            } else {
                client.chatConsole.pushErrorLine(typeChatMessage(ouroborosNode, "This message should not appear ... why?"));
            }

            return null;
        }

        return Util.concatByteArray(_fileId, _filePartNo, fileData);
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

    Task sendDataFromString(String targetUserId, byte[] data) {
        return sendDataFromString(targetUserId, data, -1);
    }

    Task sendDataFromString(String targetUserId, byte[] data, int delay) {
        String userId = "@" + client.userStack.myProfile.id;
        String content = Util.convertByteArrayToBase64(data);
        String secureHash = client.generateSecureHashWithMyProfile(content);

        targetUserId = "@" + targetUserId;

        Message message = new Message(client.systemConsole, "pst-on", "+", Client.TIMEOUT, userId, targetUserId, content, secureHash);

        return client.taskStack.run(new PostOuroborosNodeData().set(client, null, message), delay);
    }

    Task sendDataFromBinary(String targetUserId, byte[] data) {
        return sendDataFromBinary(targetUserId, data, -1);
    }

    Task sendDataFromBinary(String targetUserId, byte[] data, int delay) {
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

    public static String typeChatMessage(OuroborosNode ouroborosNode, String message) {
        String postUserName = ouroborosNode.target.name;
        String sendUserName = ouroborosNode.myself.name;

        return Client.getCurrentTimeDisplay() + postUserName + " → " + sendUserName + " [Private Message / Send with ONN]:\n" + message;
    }

    public static String typeMap() {
        return "map of the Ouroboros Node Network.";
    }

    public static String typeMap(OuroborosNode ouroborosNode) {
        String postDisplay = ouroborosNode.myself.display();
        String sendDisplay = ouroborosNode.target.display();

        return "map of the Ouroboros Node Network: " + postDisplay + " ↔ " + sendDisplay;
    }

    public static String typeErrorYourself(User user) {
        return typeErrorYourself(user.id);
    }

    public static String typeErrorYourself(String userId) {
        return "You can't set yourself (@" + userId + ") as the recipient.";
    }

    public static String typeErrorUnknownUser(User user) {
        return typeErrorUnknownUser(user.id);
    }

    public static String typeErrorUnknownUser(String userId) {
        return "The specified user (@" + userId + ") does not exist.";
    }

    public static String typeErrorUnknownOuroborosNode() {
        return "The Ouroboros Node does not exist.";
    }

    public static String typeErrorUnknownOuroborosNode(User user) {
        return typeErrorUnknownOuroborosNode(user.id);
    }

    public static String typeErrorUnknownOuroborosNode(String userId) {
        return "The Ouroboros Node (send to @" + userId + ") does not exist.";
    }
}
