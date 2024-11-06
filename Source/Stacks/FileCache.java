package Source.Stacks;

import Source.Client;
import Source.Utils.Util;

import java.io.*;
import java.nio.file.*;

public class FileCache {
    public Client client;

    User user;
    String fileId;
    String fileName;
    int sumPart;

    int status = 0;
    int part = -1;

    File file;
    File bufferedFile;

    FileOutputStream fileOutputStream;
    BufferedOutputStream bufferedOutputStream;

    public FileCache(Client client) {
        this.client = client;
    }

    public boolean set(String userId, String fileId, String fileName) {
        User user = client.userStack.test(userId);

        if (user == null)
            return false;

        this.user = user;
        this.fileId = fileId;
        this.fileName = Paths.get(fileName).getFileName().toString(); // This is important!

        client.systemConsole.pushMainLine("The file (" + fileName + ") was stocked as #" + fileId + " by @" + userId + ".");

        return true;
    }

    public void finish() {
        try {
            fileOutputStream.close();
        } catch (IOException e) {
            client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
        }

        status = 0;
    }

    public void discard() {
        finish();

        try {
            Files.deleteIfExists(bufferedFile.toPath());
        } catch (IOException e) {
            client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));
        }
    }

    public boolean prepareFileData() {
        part = -1;
        sumPart = -1;

        if (!setFolder())
            return false;

        if (!setFilePath())
            return false;

        if (!setOutputStream())
            return false;

        return true;
    }

    boolean setFolder() {
        Path folderPath = Paths.get(Client.DOWNLOAD_PATH);

        if (!Files.exists(folderPath)) {
            try {
                Files.createDirectories(folderPath);
            } catch (IOException e) {
                client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to prepare download folder (" + folderPath.toString() + ")."));

                return false;
            }
        }

        return true;
    }

    boolean setFilePath() {
        Path filePath = generateFilePath();
        Path bufferedFilePath = generateFilePath("tmp");

        for (int i = 2; true; i++) {
            if (Files.exists(filePath) || Files.exists(bufferedFilePath)) {
                filePath = generateFilePath(i);
                bufferedFilePath = generateFilePath(i, "tmp");
            } else {
                break;
            }
        }

        file = new File(filePath.toString());
        bufferedFile = new File(bufferedFilePath.toString());

        return true;
    }

    boolean setOutputStream() {
        try {
            fileOutputStream = new FileOutputStream(bufferedFile);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

            status = 1;

            return true;
        } catch (FileNotFoundException e) {
            client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to prepare file (#" + fileId + ")."));

            status = 0;

            return false;
        }
    }

    Path generateFilePath() {
        return generateFilePath(0, null);
    }

    Path generateFilePath(int fileNo) {
        return generateFilePath(fileNo, null);
    }

    Path generateFilePath(String option) {
        return generateFilePath(0, option);
    }

    Path generateFilePath(int fileNo, String option) {
        String fileBaseName;
        String fileExtension;

        int fileExtensionIndex = fileName.lastIndexOf(".");

        if (fileExtensionIndex != -1) {
            fileBaseName = fileName.substring(0, fileExtensionIndex);
            fileExtension = "." + fileName.substring(fileExtensionIndex + 1);
        } else {
            fileBaseName = fileName;
            fileExtension = "";
        }

        if (fileNo > 0)
            fileBaseName += " (" + String.valueOf(fileNo) + ")";

        if (option != null)
            fileExtension += "." + option;

        return Paths.get(Client.DOWNLOAD_PATH).resolve(fileBaseName + fileExtension);
    }

    public int writeFileData(int part, byte[] data) {
        if (status == 1 && part == -1) {
            return setSumPart(Util.convertByteArrayToInt(data));
        } else if (status == 2 && part == this.part) {
            if (data.length > 4) {
                try {
                    byte[] content = Util.getNextDataOnSize(data);

                    bufferedOutputStream.write(content, 0, content.length);
                    bufferedOutputStream.flush();

                    double percent = (double)(part + 1) / (double)sumPart * 100.0;

                    client.systemConsole.pushMainLine("Downloading file (#" + fileId + ") ... " + String.format("%.2f", percent) + "%");
                } catch (IOException e) {
                    client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Failed to write file (#" + fileId + ")."));

                    finish();

                    return -2;
                }
            }

            if (data.length > 4 && part + 1 < sumPart) {
                return ++this.part;
            } else {
                if (bufferedFile.renameTo(file)) {
                    client.systemConsole.pushMainLine("The file (#" + fileId + ") has finished downloading.");

                    finish();

                    return -1;
                } else {
                    client.systemConsole.pushErrorLine("Failed to rename file (#" + fileId + ").");

                    finish();

                    return -2;
                }
            }
        } else {
            client.systemConsole.pushErrorLine("Failed to download file (#" + fileId + ").");

            discard();

            return -2;
        }
    }

    public int setSumPart(int sumPart) {
        this.sumPart = sumPart;

        if (sumPart > 0) {
            client.systemConsole.pushMainLine("Downloading file (#" + fileId + ") ... 0%");

            status = 2;

            return ++this.part;
        } else if (sumPart == -1) {
            client.systemConsole.pushWarningLine("The file (#" + fileId + ") has been removed from the stock by its owner.");

            discard();

            return -1;
        } else {
            client.systemConsole.pushErrorLine("Failed to receive file (#" + fileId + ").");

            discard();

            return -2;
        }
    }

    public String getUserId() {
        return user != null ? user.id : null;
    }

    public String getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }
}
