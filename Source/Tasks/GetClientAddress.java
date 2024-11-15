package Source.Tasks;

import Source.Client;
import Source.Stacks.Node;
import Source.Stacks.User;
import Source.Utils.Message;
import Source.Utils.Util;

import javax.crypto.*;

public class GetClientAddress extends NetworkTask {
    @Override
    public GetClientAddress set(Client client, Node node, Message work) {
        super.set(client, node, work);

        setProperties(Integer.parseInt(work.getStringData(0)), 10, "req-ca", "ret-ca");

        String targetUserId = work.getStringData(1).substring(1);

        setSendUserIfNodeExist(targetUserId);

        if (myProfile.equals(targetUserId))
            skipSend = true;

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
        updateNodeStore(node, "DUP", "NUL");
    }

    @Override
    void timeout(Node node) {
        updateNodeStore(node, "OUT", "NUL");
    }

    @Override
    void response() {
        String targetUserId = work.getStringData(1).substring(1);

        if (isOriginalTask()) {
            responseOfMine(targetUserId);
        } else {
            responseOfOthers(targetUserId);
        }
    }

    private void responseOfMine(String targetUserId) {
        NodeStore res = nodeStore.stream().filter(nodeStore -> !nodeStore.data[0].matches("EMP|DUP|OUT")).findFirst().orElse(null);

        if (res == null || res.data[0].equals("ERR")) {
            pushErrorLine("Failed to get client address.");

            return;
        }

        User targetUser = client.userStack.test(targetUserId);

        if (targetUser == null)
            return;

        SecretKey decryptedCommonKey = Util.decryptBase64ToCommonKeyWithRsaPublicKey(res.data[1], targetUser.publicKey);
        String decryptedUserAddress = Util.decryptBase64ToStringWithAesCommonKey(res.data[0], decryptedCommonKey);

        if (decryptedCommonKey == null || decryptedUserAddress == null) {
            pushErrorLine("Failed to decrypt data ... may be corrupted or tampered with.");

            return;
        }

        if (!decryptedUserAddress.matches(Util.IP_ADDRESS_PORT_REGEX)) {
            pushErrorLine("The data is written incorrectly.");

            return;
        }

        String[] buf = decryptedUserAddress.split(":");

        client.joinNetwork(buf[0], Integer.parseInt(buf[1]));
    }

    private void responseOfOthers(String targetUserId) {
        if (!myProfile.equals(targetUserId)) {
            NodeStore res = nodeStore.stream().filter(nodeStore -> !nodeStore.data[0].matches("EMP|DUP|OUT")).findFirst().orElse(null);

            if (res != null) {
                node.sendMessage(returnCommand, work.id, res.data);
            } else {
                node.sendMessage(returnCommand, work.id, "EMP", "NUL");
            }
        } else {
            SecretKey commonKey = Util.generateAesCommonKey();

            String encryptedUserAddress = Util.encryptStringToBase64WithAesCommonKey(client.nodeListener.getAddress(), commonKey);
            String encryptedCommonKey = Util.encryptCommonKeyToBase64WithRsaPrivateKey(commonKey, myProfile.privateKey);

            if (commonKey == null || encryptedUserAddress == null || encryptedCommonKey == null) {
                pushErrorLine("Failed to encrypt data.");

                node.sendMessage(returnCommand, work.id, "ERR", "ERR");

                return;
            }

            node.sendMessage(returnCommand, work.id, encryptedUserAddress, encryptedCommonKey);
        }
    }
}
