package Source.Stacks;

import Source.Client;
import Source.Utils.Util;

import java.io.*;
import java.util.*;

public class FileStore {
    public Client client;

    String id;
    int partSize = Client.FILE_DATA_PART_SIZE;
    int sumPart = 0;
    boolean isRemoved = false;

    List<User> allowedUserList;

    File file;
    RandomAccessFile randomAccessFile;

    public FileStore(Client client) {
        this.client = client;

        id = Util.generateNoiseHexString(16);
    }

    public FileStore(Client client, int partSize) {
        this.client = client;
        this.partSize = partSize;

        id = Util.generateNoiseHexString(16);
    }

    public boolean set(String filePath) {
        file = new File(filePath);

        if (file != null && file.exists()) {
            try {
                sumPart = (int)((file.length() - 1) / partSize) + 1;

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

    public void setAllowedUserList(List<User> allowedUserList) {
        this.allowedUserList = allowedUserList;
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

        if (part < -2 || part > sumPart) {
            String partNo = String.valueOf(part);
            String fileDataLength = String.valueOf(sumPart) + " * " + String.valueOf(partSize) + " byte";

            client.systemConsole.pushErrorLine("The specified part (" + partNo + ") is larger than the file size (" + fileDataLength + ").");

            return null;
        } else if (part == -1) {
            return Util.convertIntToByteArray(sumPart);
        } else if (part == -2 || part == sumPart) {
            return Util.convertIntToByteArray(0);
        }

        try {
            randomAccessFile.seek(part * partSize);

            byte[] data = new byte[partSize];
            int length = randomAccessFile.read(data, 0, partSize);

            if (length > -1) {
                if (length == partSize) {
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

    public boolean isAllowedUser(String targetUserId) {
        if (allowedUserList == null)
            return true;

        User targetUser = client.userStack.get(targetUserId);

        if (targetUser != null) {
            if (allowedUserList.stream().filter(user -> user.id.equals(targetUser.id)).findFirst().orElse(null) != null) {
                return true;
            } else {
                return false;
            }
        } else {
            client.systemConsole.pushErrorLine("The specified user (@" + targetUserId + ") does not exist.");

            return false;
        }
    }
}
