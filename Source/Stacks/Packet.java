package Source.Stacks;

import java.util.*;

import Source.Client;
import Source.Utils.Util;

public class Packet {
    // Note: Packetは本来なら必要ないのに…自動で処理してくれると思っていた自分がマヌケだった。

    public Client client;

    public String id;
    public int index;
    public int length;
    public byte[] data;

    public Packet(Client client, byte[] data) {
        this.client = client;

        if (data.length <= 24)
            return;

        this.id = Util.convertByteArrayToHexString(Arrays.copyOfRange(data, 0, 16));
        this.index = Util.convertByteArrayToInt(Arrays.copyOfRange(data, 16, 20));
        this.length = Util.convertByteArrayToInt(Arrays.copyOfRange(data, 20, 24));
        this.data = Arrays.copyOfRange(data, 24, data.length);
    }

    public boolean check() {
        if (id == null) {
            pushErrorLine("The packet must have a property.");

            return false;
        }

        if (data == null || data.length <= 0) {
            pushErrorLine("The packet must have a data.");

            return false;
        }

        if (!((index != -1 && data.length == length) || (index == -1 && data.length == 32))) {
            pushErrorLine("Some of data is missing.");

            return false;
        }

        return true;
    }

    public boolean equals(String id) {
        return this.id.equals(id);
    }

    public boolean equals(Packet packet) {
        return equals(packet.id);
    }

    void pushErrorLine(String text) {
        client.systemConsole.pushErrorLine("Packet (#" + id + ":" + String.valueOf(index) + ") ... " + text);
    }
}
