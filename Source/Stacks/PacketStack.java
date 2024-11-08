package Source.Stacks;

import java.io.*;
import java.util.*;

import Source.Client;
import Source.Utils.Util;

public class PacketStack {
    Client client;

    class PacketCollection {
        String id;
        int sumPart = -1;
        byte[] hash;

        List<Packet> packetStore = new ArrayList<Packet>();

        public PacketCollection(Packet packet) {
            if (packet == null) {
                pushErrorLine("Packet does not exist.");

                return;
            }

            id = packet.id;

            add(packet);
        }

        public boolean add(Packet packet) {
            if (packet == null) {
                pushErrorLine("Packet does not exist.");

                return false;
            }

            if (!id.equals(packet.id)) {
                pushErrorLine("Packet with different ID (#" + packet.id + ") can't be added.");

                return false;
            }

            if (packet.index == -1) {
                if (sumPart == -1 && hash == null) {
                    sumPart = packet.length;
                    hash = packet.data;
                } else {
                    packet.pushErrorLine("Invalid end packet received.");

                    return false;
                }
            } else {
                if (get(packet.index) == null) {
                    packetStore.add(packet);
                } else {
                    packet.pushErrorLine("Invalid packet received.");

                    return false;
                }
            }

            return true;
        }

        public Packet test(int index) {
            Packet res = get(index);

            if (res == null)
                pushErrorLine("The specified packet [" + index + "] does not exist.");

            return res;
        }

        public Packet get(int index) {
            return packetStore.stream().filter(item -> item.index == index).findFirst().orElse(null);
        }

        public boolean equals(String id) {
            return this.id.equals(id);
        }

        public boolean equals(PacketCollection packetCollection) {
            return equals(packetCollection.id);
        }

        public byte[] process() {
            if (sumPart != -1 && sumPart == packetStore.size()) {
                packetStore.sort(Comparator.comparing(PacketCollection::getPacketIndex, Comparator.naturalOrder()));

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                for (int i = 0; i < packetStore.size(); i++) {
                    byte[] rec = packetStore.get(i).data;

                    byteArrayOutputStream.write(rec, 0, rec.length);
                }

                byte[] res = byteArrayOutputStream.toByteArray();

                if (Arrays.equals(Util.getSha256(res), hash)) {
                    remove(this);

                    return res;
                } else {
                    pushErrorLine("The data does not match the hash value.");

                    remove(this);

                    return null;
                }
            } else {
                return null;
            }
        }

        static int getPacketIndex(Packet packet) {
            return packet.index;
        }

        void pushErrorLine(String text) {
            client.systemConsole.pushErrorLine("Packet (#" + id + ") ... " + text);
        }
    }

    List<PacketCollection> packetStack = new ArrayList<PacketCollection>();

    public PacketStack(Client client) {
        this.client = client;
    }

    public void add(PacketCollection packetCollection) {
        packetStack.add(packetCollection);
    }

    public void remove(PacketCollection packetCollection) {
        packetStack.remove(packetCollection);
    }

    public PacketCollection test(String id) {
        PacketCollection res = get(id);

        if (res == null)
            client.systemConsole.pushErrorLine("The specified packet collection (#" + id + ") does not exist.");

        return res;
    }

    public PacketCollection get(String id) {
        return packetStack.stream().filter(item -> item.equals(id)).findFirst().orElse(null);
    }

    public static void splitAndSendMessageFromBinary(OutputStream binaryWriter, byte[] message) throws IOException {
        int messageDataPartSize = Util.getMaximum(Client.MESSAGE_DATA_PART_SIZE, Client.PACKET_DATA_PART_SIZE);

        byte[] id = Util.generateNoiseByte(16);
        int sumPart = message.length / messageDataPartSize + 1;

        for (int i = 0; i < sumPart; i++) {
            int startIndex = i * messageDataPartSize;
            int endIndex = i != sumPart - 1 ? (i + 1) * messageDataPartSize : message.length;

            byte[] index = Util.convertIntToByteArray(i);
            byte[] length = Util.convertIntToByteArray(endIndex - startIndex);
            byte[] data = Arrays.copyOfRange(message, startIndex, endIndex);

            binaryWriter.write(Util.concatByteArray(id, index, length, data));
        }

        {
            byte[] index = Util.convertIntToByteArray(-1);
            byte[] length = Util.convertIntToByteArray(sumPart);
            byte[] hash = Util.getSha256(message);

            binaryWriter.write(Util.concatByteArray(id, index, length, hash));
        }
    }

    public byte[] receiveAndJoinMessageFromBinary(byte[] data) {
        Packet packet = new Packet(client, data);

        if (!packet.check())
            return null;

        PacketCollection packetCollection = get(packet.id);

        if (packetCollection == null) {
            add(new PacketCollection(packet));

            return null;
        } else {
            packetCollection.add(packet);

            return packetCollection.process();
        }
    }
}
