package Source.Stacks;

import Source.Client;
import Source.Tasks.PostChatFile;
import Source.Tasks.RequestChatFile;
import Source.Tasks.SendChatFile;
import Source.Utils.Message;
import Source.Utils.Util;

import java.util.*;

public class FileStack {
    Client client;

    List<FileStore> fileStoreStack = new ArrayList<FileStore>();
    List<FileCache> fileCacheStack = new ArrayList<FileCache>();

    public FileStack(Client client) {
        this.client = client;
    }

    public void addFileStore(String filePath) {
        FileStore fileStore = new FileStore(client);

        if (fileStore.set(filePath)) {
            if (getFileStore(fileStore.getFileId()) == null) {
                fileStoreStack.add(fileStore);

                postFile(fileStore);
            } else {
                client.systemConsole.pushErrorLine("You are very lucky ... but please try the same operation again.");
            }
        }
    }

    public void addFileCache(String userId, String fileId, String fileName) {
        FileCache fileCache = new FileCache(client);

        if (fileCache.set(userId, fileId, fileName))
            fileCacheStack.add(fileCache);
    }

    public FileStore getFileStore(String fileId) {
        FileStore res = fileStoreStack.stream().filter(item -> item.getFileId().equals(fileId)).findFirst().orElse(null);

        if (res != null) {
            return res;
        } else {
            // client.systemConsole.pushErrorLine("The specified file (#" + fileId + ") does not exist.");

            return null;
        }
    }

    public FileCache getFileCache(String userId, String fileId) {
        FileCache res = fileCacheStack.stream().filter(item -> item.getUserId().equals(userId) && item.getFileId().equals(fileId)).findFirst().orElse(null);

        if (res != null) {
            return res;
        } else {
            // client.systemConsole.pushErrorLine("The specified file (#" + fileId + " by @" + userId + ") does not exist.");

            return null;
        }
    }

    public void request(String userId, String fileId) {
        FileCache fileCache = getFileCache(userId, fileId);

        if (fileCache != null) {
            if (fileCache.requestFileData()) {
                client.systemConsole.pushMainLine("Request the file (#" + fileId + ") from the user (@" + userId + ").");

                requestFile(fileCache, -1);
            }
        } else {
            client.systemConsole.pushErrorLine("The specified file (#" + fileId + " by @" + userId + ") does not exist.");
        }
    }

    public void send(String userId, String fileId, int part) {
        FileStore fileStore = getFileStore(fileId);

        if (fileStore != null) {
            if (part == -1) {
                if (!fileStore.isRemoved()) {
                    client.systemConsole.pushMainLine("The user (@" + userId + ") requested the file (#" + fileId + ").");
                } else {
                    client.systemConsole.pushWarningLine("The user (@" + userId + ") requested removed file (#" + fileId + ").");
                }
            } else {
                String partDisplay = String.valueOf(part + 1) + " / " + String.valueOf(fileStore.getFileSumPart());

                client.systemConsole.pushMainLine("The user (@" + userId + ") requested part (" + partDisplay + ") of the file (#" + fileId + ").");
            }

            sendFileFromBinary(userId, fileId, part, fileStore.readFileData(part));
        } else {
            client.systemConsole.pushErrorLine("The user (@" + userId + ") requested an unknown file (#" + fileId + ").");
        }
    }

    public void receive(String userId, String fileId, int part, byte[] data) {
        FileCache fileCache = getFileCache(userId, fileId);

        if (fileCache != null) {
            int rec = fileCache.writeFileData(part, data);

            if (rec > -1)
                requestFile(fileCache, rec);
        } else {
            client.systemConsole.pushErrorLine("An unknown file (#" + fileId + " by @" + userId + ") received.");
        }
    }

    public void free(String fileId) {
        FileStore fileStore = getFileStore(fileId);

        if (fileStore != null) {
            fileStore.free();

            client.systemConsole.pushMainLine("The file (#" + fileId + ") was removed from the stock.");
        } else {
            client.systemConsole.pushErrorLine("The specified file (#" + fileId + ") does not exist.");
        }
    }

    public Task postFile(FileStore fileStore) {
        String userId = "@" + client.userStack.myProfile.id;
        String fileId = "#" + fileStore.getFileId();
        String fileName = Util.convertStringToBase64(fileStore.getFileName());
        String secureHash = client.generateSecureHashWithMyProfile(fileName + fileId);

        Message message = new Message(client.systemConsole, "pst-cf", "+", Client.TIMEOUT, userId, fileId, fileName, secureHash);

        return client.taskStack.run(new PostChatFile().set(client, null, message));
    }

    public Task requestFile(FileCache fileCache, int part) {
        String userId = "@" + client.userStack.myProfile.id;
        String targetUserId = "@" + fileCache.getUserId();
        String targetFileId = "#" + fileCache.getFileId();
        String partNo = String.valueOf(part);
        String secureHash = client.generateSecureHashWithMyProfile(partNo + targetFileId + targetUserId);

        Message message = new Message(client.systemConsole, "req-cf", "+", Client.TIMEOUT, userId, targetUserId, targetFileId, partNo, secureHash);

        return client.taskStack.run(new RequestChatFile().set(client, null, message));
    }

    public Task sendFileFromString(String userId, String fileId, int part, byte[] content) {
        String myselfUserId = "@" + client.userStack.myProfile.id;
        String targetUserId = "@" + userId;
        String targetFileId = "#" + fileId;
        String partNo = String.valueOf(part);
        String contentData = Util.convertByteArrayToBase64(content);
        String secureHash = client.generateSecureHashWithMyProfile(contentData + ":" + partNo + targetFileId + targetUserId);

        Message message = new Message(client.systemConsole, "snd-cf", "+", Client.TIMEOUT, myselfUserId, targetUserId, targetFileId, partNo, contentData,
                secureHash);

        return client.taskStack.run(new SendChatFile().set(client, null, message));
    }

    public Task sendFileFromBinary(String userId, String fileId, int part, byte[] content) {
        byte[] _timeout = Util.convertIntToByteArray(Integer.parseInt(Client.TIMEOUT));
        byte[] _myselfUserId = Util.convertHexStringToByteArray(client.userStack.myProfile.id);
        byte[] _targetUserId = Util.convertHexStringToByteArray(userId);
        byte[] _targetFileId = Util.convertHexStringToByteArray(fileId);
        byte[] _partNo = Util.convertIntToByteArray(part);
        byte[] _content = content;
        byte[] _contentSize = Util.convertIntToByteArray(_content.length);
        byte[] _secureHash = client.generateSecureHashWithMyProfile(Util.concatByteArray(_targetUserId, _targetFileId, _partNo, _content));
        byte[] _secureHashSize = Util.convertIntToByteArray(_secureHash.length);

        Message message = new Message(client.systemConsole, "snd-cf", "+",
                Util.concatByteArray(_timeout, _myselfUserId, _targetUserId, _targetFileId, _partNo, _contentSize, _content, _secureHashSize, _secureHash));

        // message = {
        // ### timeout[4], myselfUserId[16], targetUserId[16], targetFileId[16], partNo[4],
        // ### contentSize[4], content[...],
        // ### secureHashSize[4], secureHash[...]
        // };

        return client.taskStack.run(new SendChatFile().set(client, null, message));
    }
}