package Source.Stacks;

import Source.Client;
import Source.Utils.Util;

import java.security.*;
import java.security.interfaces.*;

public class User {
    // Note: Userはネットワークに参加しているクライアントに対応する。

    public Client client;

    public Node node;

    public String id;
    public String name;

    public RSAPublicKey publicKey;
    public RSAPrivateKey privateKey;

    public String publicKeyString;
    public String privateKeyString;

    public User(Client client) {
        this.client = client;
        this.node = null;

        id = Util.generateNoiseHexString(16);
        name = "Anonymous";

        generateKey();
    }

    public User(Client client, String raw) {
        this.client = client;
        this.node = null;

        set(raw);
    }

    public User(Client client, String id, String name, RSAPublicKey publicKey) {
        this.client = client;
        this.node = null;

        set(id, name, publicKey);
    }

    public User get() {
        if (id != null && name != null && publicKey != null) {
            return this;
        } else {
            return null;
        }
    }

    public User set(String raw) {
        String[] buf = raw.split(",");

        if (buf == null || buf.length != 3) {
            pushErrorLine("Invalid raw data.");

            return null;
        }

        if (!buf[0].matches(Util.USER_ID_REGEX)) {
            pushErrorLine("The user ID is written incorrectly.");

            return null;
        }

        String name = Util.convertBase64ToString(buf[1]);

        if (name == null) {
            pushErrorLine("The user name is written incorrectly.");

            return null;
        }

        RSAPublicKey publicKey = Util.getRsaPublicKeyFromByteArray(Util.convertBase64ToByteArray(buf[2]));

        if (publicKey == null) {
            pushErrorLine("The public key is written incorrectly.");

            return null;
        }

        return set(buf[0].substring(1), name, publicKey);
    }

    public User set(String id, String name, RSAPublicKey publicKey) {
        if (id != null)
            this.id = id;

        if (name != null)
            this.name = name;

        if (publicKey != null) {
            this.publicKey = publicKey;
            this.publicKeyString = Util.convertByteArrayToBase64(publicKey.getEncoded());
        }

        return get();
    }

    public boolean equals(String id) {
        return this.id.equals(id);
    }

    public boolean equals(User user) {
        return equals(user.id);
    }

    public String stringify() {
        return "@" + id + "," + Util.convertStringToBase64(name) + "," + publicKeyString;
    }

    public String display() {
        return name + " (@" + id + ")";
    }

    public void generateKey() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");

            keyGen.initialize(1024);

            KeyPair keyPair = keyGen.generateKeyPair();

            publicKey = (RSAPublicKey)keyPair.getPublic();
            privateKey = (RSAPrivateKey)keyPair.getPrivate();

            publicKeyString = Util.convertByteArrayToBase64(publicKey.getEncoded());
            privateKeyString = Util.convertByteArrayToBase64(privateKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to create RSA key."));
        } catch (Exception e) {
            client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    void pushErrorLine(String text) {
        client.systemConsole.pushErrorLine("Invalid user data received: " + text);
    }
}
