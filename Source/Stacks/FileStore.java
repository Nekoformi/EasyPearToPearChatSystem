package Source.Stacks;

import Source.Client;
import Source.Utils.Util;

import java.io.*;

public class FileStore {
    public static final int DATA_PART_SIZE = 65536;

    public Client client;

    String id;
    int sumPart = 0;
    boolean isRemoved = false;

    File file;
    RandomAccessFile randomAccessFile;

    public FileStore(Client client) {
        this.client = client;

        id = Util.generateNoiseHexString(16);
    }

    public boolean set(String filePath) {
        file = new File(filePath);

        if (file != null && file.exists()) {
            try {
                sumPart = (int)((file.length() - 1) / DATA_PART_SIZE) + 1;

                randomAccessFile = new RandomAccessFile(file, "r");

                client.systemConsole.pushMainLine("The file (" + filePath + ") was stocked as #" + id + ".");

                return true;
            } catch (FileNotFoundException e) {
                client.systemConsole.pushErrorLine("File (" + filePath + ") does not exist.");

                return false;
            }
        } else {
            client.systemConsole.pushErrorLine("File (" + filePath + ") does not exist.");

            return false;
        }
    }

    public void free() {
        isRemoved = true;

        try {
            randomAccessFile.close();
        } catch (IOException e) {
            client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
        }
    }

    public byte[] readFileData(int part) {
        if (isRemoved)
            return Util.convertIntToByteArray(-1);

        if (part < -1 || part > sumPart) {
            String partNo = String.valueOf(part);
            String fileDataLength = String.valueOf(sumPart) + " * " + String.valueOf(DATA_PART_SIZE) + " byte";

            client.systemConsole.pushErrorLine("The specified part (" + partNo + ") is larger than the file size (" + fileDataLength + ").");

            return null;
        } else if (part == -1) {
            return Util.convertIntToByteArray(sumPart);
        } else if (part == sumPart) {
            return Util.convertIntToByteArray(0);
        }

        try {
            randomAccessFile.seek(part * DATA_PART_SIZE);

            byte[] data = new byte[DATA_PART_SIZE];
            int length = randomAccessFile.read(data, 0, DATA_PART_SIZE);

            if (length > -1) {
                if (length == DATA_PART_SIZE) {
                    return Util.concatByteArray(Util.convertIntToByteArray(length), data);
                } else {
                    return Util.concatByteArray(Util.convertIntToByteArray(length), Util.getNextDataOnSize(data, length));
                }
            } else {
                return Util.convertIntToByteArray(0);
            }
        } catch (IOException e) {
            client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to read file."));

            return null;
        }
    }

    public String getFileId() {
        return id;
    }

    public String getFileName() {
        if (file != null && file.exists()) {
            return file.getName();
        } else {
            return null;
        }
    }

    public int getFileSumPart() {
        return sumPart;
    }

    public boolean isRemoved() {
        return isRemoved;
    }
}
