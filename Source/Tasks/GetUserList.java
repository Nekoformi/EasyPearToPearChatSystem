package Source.Tasks;

import Source.Client;
import Source.Stacks.Node;
import Source.Stacks.User;
import Source.Utils.Message;
import Source.Utils.Util;

import java.util.*;
import java.util.stream.*;
import javax.crypto.*;

public class GetUserList extends NetworkTask {
    @Override
    public GetUserList set(Client client, Node node, Message work) {
        super.set(client, node, work);

        setProperties(Integer.parseInt(work.getStringData(0)), 10, "req-ul", "ret-ul");

        return this;
    }

    @Override
    void send(Node node) {
        node.sendMessage(requestCommand, work.id, String.valueOf(timeout - timeoutDecrement), work.getStringData(1));
    }

    @Override
    void resolve(Node node, Message work) {
        updateNodeStore(node, work.getStringData());
    }

    @Override
    void reject(Node node, Message work) {
        updateNodeStore(node, "NUL", "DUP");
    }

    @Override
    void timeout(Node node) {
        updateNodeStore(node, "NUL", "OUT");
    }

    @Override
    void response() {
        nodeStore = nodeStore.stream().filter(nodeStore -> !nodeStore.data[1].matches("DUP|OUT")).collect(Collectors.toList());

        String nodeStructure = nodeStore.size() != 0 ? joinNodeStore(',', ';') : "NUL";

        if (isOriginalTask()) {
            responseOfMine(nodeStructure);
        } else {
            responseOfOthers(nodeStructure);
        }
    }

    private void responseOfMine(String nodeStructure) {
        NodeStructure[] res = listNodeStructure(analyzeNodeStructure(nodeStructure, 0, ',', ';'));

        if (res == null) {
            pushErrorLine("Failed to get user list.");

            return;
        }

        List<User> currentUserStack = client.userStack.carbon(false);
        List<User> newUserStack = new ArrayList<User>();

        for (int i = 0; i < res.length; i++) {
            SecretKey decryptedCommonKey = Util.decryptBase64ToCommonKeyWithRsaPrivateKey(res[i].data[2], myProfile.privateKey);
            String decryptedUserData = Util.decryptBase64ToStringWithAesCommonKey(res[i].data[1], decryptedCommonKey);

            if (decryptedCommonKey == null || decryptedUserData == null) {
                pushErrorLine("Failed to decrypt data ... may be corrupted or tampered with.");

                continue;
            }

            newUserStack.add(client.userStack.add(decryptedUserData, false));
        }

        currentUserStack.forEach(user -> {
            if (!newUserStack.contains(user)) {
                client.systemConsole.pushWarningLine(user.display() + " may have quietly left the network.");
                client.chatConsole.pushWarningLine(Client.getCurrentTimeDisplay() + user.display() + " probably left the network.");

                client.userStack.remove(user, false);
            }
        });

        client.userStack.updateUserList();
    }

    private void responseOfOthers(String nodeStructure) {
        SecretKey commonKey = Util.generateAesCommonKey();

        String encryptedUserData = Util.encryptStringToBase64WithAesCommonKey(myProfile.stringify(), commonKey);
        String encryptedCommonKey = Util.encryptCommonKeyToBase64WithRsaPublicKey(commonKey,
                Util.getRsaPublicKeyFromByteArray(Util.convertBase64ToByteArray(work.getStringData(1))));

        if (commonKey == null || encryptedUserData == null || encryptedCommonKey == null) {
            pushErrorLine("Failed to encrypt data.");

            node.sendMessage(returnCommand, work.id, nodeStructure, "ERR", "ERR");

            return;
        }

        node.sendMessage(returnCommand, work.id, nodeStructure, encryptedUserData, encryptedCommonKey);
    }
}
