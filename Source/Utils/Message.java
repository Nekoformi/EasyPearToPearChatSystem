package Source.Utils;

import java.util.*;

public class Message {
    public Console console;

    public String command;
    public String id;
    public byte[] dataByte;
    public String[] dataString;

    public static final byte SIGN_DUPLICATE = 1;
    public static final byte SIGN_ALIGNMENT = 2;
    public static final byte SIGN_LEFT = 3;

    public static final byte SIGN_REQUEST_NODE_LIST = 4;
    public static final byte SIGN_RETURN_NODE_LIST = 5;
    public static final byte SIGN_REQUEST_USER_LIST = 6;
    public static final byte SIGN_RETURN_USER_LIST = 7;
    public static final byte SIGN_REQUEST_CLIENT_ADDRESS = 8;
    public static final byte SIGN_RETURN_CLIENT_ADDRESS = 9;

    public static final byte SIGN_POST_USER_PROFILE = 10;
    public static final byte SIGN_UPDATE_USER_PROFILE = 11;
    public static final byte SIGN_REMOVE_USER_PROFILE = 12;
    public static final byte SIGN_RECEIVE_USER_PROFILE = 13;

    public static final byte SIGN_POST_CHAT_MESSAGE = 14;
    public static final byte SIGN_RECEIVE_CHAT_MESSAGE = 15;

    public static final byte SIGN_POST_OUROBOROS_NODE_DATA = 16;
    public static final byte SIGN_RECEIVE_OUROBOROS_NODE_DATA = 17;

    public static final String NAME_DUPLICATE = "dup";
    public static final String NAME_ALIGNMENT = "alg";
    public static final String NAME_LEFT = "let";

    public static final String NAME_REQUEST_NODE_LIST = "req-nl";
    public static final String NAME_RETURN_NODE_LIST = "ret-nl";
    public static final String NAME_REQUEST_USER_LIST = "req-ul";
    public static final String NAME_RETURN_USER_LIST = "ret-ul";
    public static final String NAME_REQUEST_CLIENT_ADDRESS = "req-ca";
    public static final String NAME_RETURN_CLIENT_ADDRESS = "ret-ca";

    public static final String NAME_POST_USER_PROFILE = "pst-up";
    public static final String NAME_UPDATE_USER_PROFILE = "upd-up";
    public static final String NAME_REMOVE_USER_PROFILE = "rem-up";
    public static final String NAME_RECEIVE_USER_PROFILE = "rec-up";

    public static final String NAME_POST_CHAT_MESSAGE = "pst-cm";
    public static final String NAME_RECEIVE_CHAT_MESSAGE = "rec-cm";

    public static final String NAME_POST_OUROBOROS_NODE_DATA = "pst-on";
    public static final String NAME_RECEIVE_OUROBOROS_NODE_DATA = "rec-on";

    public static final byte MESSAGE_TYPE_STRING = (byte)0x00;
    public static final byte MESSAGE_TYPE_BINARY = (byte)0x01;

    public Message() {
        console = null;

        command = null;
        id = null;
        dataByte = null;
        dataString = null;
    }

    public Message(Console console, byte[] message, boolean needTaskId) {
        this.console = console;

        set(message, needTaskId); // command[1], mode[1], id[16], data[...]
    }

    public Message(Console console, String message, boolean needTaskId) {
        this.console = console;

        set(message, needTaskId); // "/COMMAND #ID DATA DATA DATA ..."
    }

    public Message(Console console, String command, String id, byte[] data) {
        this.console = console;

        set(command, id, data);
    }

    public Message(Console console, String command, String id, String... data) {
        this.console = console;

        set(command, id, data);
    }

    public Message get() {
        if (command != null && id != null) {
            return this;
        } else {
            return null;
        }
    }

    public byte[] getByteData() {
        if (dataByte == null) {
            // pushErrorLine("Data (type: byte[]) does not exist.");

            return null;
        } else {
            return dataByte;
        }
    }

    public String[] getStringData() {
        if (dataString == null) {
            // pushErrorLine("Data (type: String[]) does not exist.");

            return null;
        } else {
            return dataString;
        }
    }

    public String getStringData(int dataIndex) {
        if (dataString == null) {
            // pushErrorLine("Data (type: String[]) does not exist.");

            return null;
        } else if (dataString.length < dataIndex + 1 || dataString[dataIndex] == null) {
            // pushErrorLine("Data (type: String) does not exist.", dataIndex);

            return null;
        } else {
            return dataString[dataIndex];
        }
    }

    public Message set(byte[] message, boolean needTaskId) {
        if (message.length < 18) {
            pushErrorLine("Invalid message.");

            return null;
        }

        String command = convertCommandSignToName(message[0]);

        if (command == null) {
            pushErrorLine("No command name specified.");

            return null;
        } else if (!("/" + command).matches(Util.COMMAND_REGEX)) {
            pushErrorLine("The command name is written incorrectly.");

            return null;
        }

        boolean isStringData = message[1] == MESSAGE_TYPE_STRING;

        String id = Util.convertByteArrayToHexString(Arrays.copyOfRange(message, 2, 18));

        if (needTaskId) {
            if (id == null) {
                pushErrorLine("No task ID specified.");

                return null;
            } else if (!("#" + id).matches(Util.TASK_ID_REGEX)) {
                pushErrorLine("The task ID is written incorrectly.");

                return null;
            } else if (id.equals("0".repeat(32)) || id.equals("F".repeat(32))) {
                pushErrorLine("Unauthorized task ID.");

                return null;
            }
        } else if (id != null) {
            if (id.equals("0".repeat(32)))
                id = "?";

            if (id.equals("F".repeat(32)))
                id = "+";
        }

        byte[] data = Arrays.copyOfRange(message, 18, message.length);

        if (data.length != 0) {
            if (isStringData) {
                return set(command, id, Util.specialSplitString(Util.convertByteArrayToString(data), " ", "\"\'", "\"\'"));
            } else {
                return set(command, id, data);
            }
        } else {
            return set(command, id);
        }
    }

    public Message set(String message, boolean needTaskId) {
        String[] buf = Util.specialSplitString(message, " ", "\"\'", "\"\'");

        if (buf == null || buf.length == 0) {
            pushErrorLine("No command name specified.");

            return null;
        }

        if (!buf[0].matches(Util.COMMAND_REGEX)) {
            pushErrorLine("The command name is written incorrectly.");

            return null;
        }

        if (needTaskId && buf.length == 1) {
            pushErrorLine("No task ID specified.");

            return null;
        }

        if (needTaskId && !buf[1].matches(Util.TASK_ID_REGEX)) {
            pushErrorLine("The task ID is written incorrectly.");

            return null;
        }

        if (buf.length >= 2 && buf[1].matches(Util.TASK_ID_REGEX)) {
            return set(buf[0].substring(1), buf[1].substring(1), Util.copyStringArray(buf, 2, -1));
        } else {
            return set(buf[0].substring(1), "?", Util.copyStringArray(buf, 1, -1));
        }
    }

    public Message set(String command, String id, byte[] data) {
        if (command != null)
            this.command = command;

        if (id != null) {
            if (id.equals("+")) {
                this.id = Util.generateNoiseHexString(16);
            } else {
                this.id = id;
            }
        }

        this.dataByte = data;
        this.dataString = null;

        return get();
    }

    public Message set(String command, String id, String... data) {
        if (command != null)
            this.command = command;

        if (id != null) {
            if (id.equals("+")) {
                this.id = Util.generateNoiseHexString(16);
            } else {
                this.id = id;
            }
        }

        this.dataByte = null;
        this.dataString = data;

        return get();
    }

    public byte[] binarize() {
        byte[] recData;
        byte[] recType;

        if (dataByte != null) {
            recData = dataByte;
            recType = new byte[] { MESSAGE_TYPE_BINARY };
        } else {
            recData = Util.convertStringToByteArray(String.join(" ", dataString));
            recType = new byte[] { MESSAGE_TYPE_STRING };
        }

        byte[] command = new byte[] { convertCommandNameToSign(this.command) };
        byte[] id = Util.convertHexStringToByteArray(this.id);

        return Util.concatByteArray(command, recType, id, recData);
    }

    public String stringify() {
        StringBuffer res = new StringBuffer("/" + command + " #" + id);

        if (dataByte != null && dataByte.length != 0) {
            res.append(" " + Util.convertByteArrayToBase64(dataByte));
        } else if (dataString != null && dataString.length != 0) {
            for (int i = 0; i < dataString.length; i++)
                res.append(" " + dataString[i]);
        }

        return res.toString();
    }

    public String display(int omitDataByteLength) {
        StringBuffer res = new StringBuffer("/" + command + " #" + id);

        if (dataByte != null && dataByte.length != 0) {
            res.append(" " + Util.omitString(Util.convertByteArrayToBase64(dataByte), omitDataByteLength, true));
        } else if (dataString != null && dataString.length != 0) {
            for (int i = 0; i < dataString.length; i++)
                res.append(" " + Util.omitString(dataString[i], omitDataByteLength, true));
        }

        return res.toString();
    }

    public boolean check(int dataIndex, int type) {
        if (dataString == null || dataString.length < dataIndex + 1 || dataString[dataIndex] == null) {
            pushErrorLine("Data (type: String) does not exist.", dataIndex);

            return false;
        }

        String rec = Util.checkStringType(dataString[dataIndex], type);

        if (rec == null) {
            return true;
        } else {
            pushErrorLine("The data is " + rec + ".", dataIndex);

            return false;
        }
    }

    public String join(int dataIndex) {
        if (dataString != null) {
            return String.join(" ", Util.copyStringArray(dataString, dataIndex, -1));
        } else {
            pushErrorLine("Data (type: String) does not exist.", dataIndex);

            return null;
        }
    }

    public void toLowerCase(int dataIndex) {
        if (dataString == null || dataString.length < dataIndex + 1 || dataString[dataIndex] == null) {
            pushErrorLine("Data (type: String) does not exist.", dataIndex);

            return;
        }

        dataString[dataIndex] = dataString[dataIndex].toLowerCase();
    }

    public void toUpperCase(int dataIndex) {
        if (dataString == null || dataString.length < dataIndex + 1 || dataString[dataIndex] == null) {
            pushErrorLine("Data (type: String) does not exist.", dataIndex);

            return;
        }

        dataString[dataIndex] = dataString[dataIndex].toUpperCase();
    }

    public static String convertCommandSignToName(byte command) {
        switch (Byte.toUnsignedInt(command)) {
        case SIGN_DUPLICATE:
            return NAME_DUPLICATE;
        case SIGN_ALIGNMENT:
            return NAME_ALIGNMENT;
        case SIGN_LEFT:
            return NAME_LEFT;
        case SIGN_REQUEST_NODE_LIST:
            return NAME_REQUEST_NODE_LIST;
        case SIGN_RETURN_NODE_LIST:
            return NAME_RETURN_NODE_LIST;
        case SIGN_REQUEST_USER_LIST:
            return NAME_REQUEST_USER_LIST;
        case SIGN_RETURN_USER_LIST:
            return NAME_RETURN_USER_LIST;
        case SIGN_REQUEST_CLIENT_ADDRESS:
            return NAME_REQUEST_CLIENT_ADDRESS;
        case SIGN_RETURN_CLIENT_ADDRESS:
            return NAME_RETURN_CLIENT_ADDRESS;
        case SIGN_POST_USER_PROFILE:
            return NAME_POST_USER_PROFILE;
        case SIGN_UPDATE_USER_PROFILE:
            return NAME_UPDATE_USER_PROFILE;
        case SIGN_REMOVE_USER_PROFILE:
            return NAME_REMOVE_USER_PROFILE;
        case SIGN_RECEIVE_USER_PROFILE:
            return NAME_RECEIVE_USER_PROFILE;
        case SIGN_POST_CHAT_MESSAGE:
            return NAME_POST_CHAT_MESSAGE;
        case SIGN_RECEIVE_CHAT_MESSAGE:
            return NAME_RECEIVE_CHAT_MESSAGE;
        case SIGN_POST_OUROBOROS_NODE_DATA:
            return NAME_POST_OUROBOROS_NODE_DATA;
        case SIGN_RECEIVE_OUROBOROS_NODE_DATA:
            return NAME_RECEIVE_OUROBOROS_NODE_DATA;
        default:
            return null;
        }
    }

    public static byte convertCommandNameToSign(String command) {
        switch (command) {
        case NAME_DUPLICATE:
            return SIGN_DUPLICATE;
        case NAME_ALIGNMENT:
            return SIGN_ALIGNMENT;
        case NAME_LEFT:
            return SIGN_LEFT;
        case NAME_REQUEST_NODE_LIST:
            return SIGN_REQUEST_NODE_LIST;
        case NAME_RETURN_NODE_LIST:
            return SIGN_RETURN_NODE_LIST;
        case NAME_REQUEST_USER_LIST:
            return SIGN_REQUEST_USER_LIST;
        case NAME_RETURN_USER_LIST:
            return SIGN_RETURN_USER_LIST;
        case NAME_REQUEST_CLIENT_ADDRESS:
            return SIGN_REQUEST_CLIENT_ADDRESS;
        case NAME_RETURN_CLIENT_ADDRESS:
            return SIGN_RETURN_CLIENT_ADDRESS;
        case NAME_POST_USER_PROFILE:
            return SIGN_POST_USER_PROFILE;
        case NAME_UPDATE_USER_PROFILE:
            return SIGN_UPDATE_USER_PROFILE;
        case NAME_REMOVE_USER_PROFILE:
            return SIGN_REMOVE_USER_PROFILE;
        case NAME_RECEIVE_USER_PROFILE:
            return SIGN_RECEIVE_USER_PROFILE;
        case NAME_POST_CHAT_MESSAGE:
            return SIGN_POST_CHAT_MESSAGE;
        case NAME_RECEIVE_CHAT_MESSAGE:
            return SIGN_RECEIVE_CHAT_MESSAGE;
        case NAME_POST_OUROBOROS_NODE_DATA:
            return SIGN_POST_OUROBOROS_NODE_DATA;
        case NAME_RECEIVE_OUROBOROS_NODE_DATA:
            return SIGN_RECEIVE_OUROBOROS_NODE_DATA;
        default:
            return 0;
        }
    }

    void pushErrorLine(String text) {
        console.pushErrorLine("Invalid message received: " + text);
    }

    void pushErrorLine(String text, int index) {
        console.pushErrorLine("Invalid message received (Argument " + String.valueOf(index) + "): " + text);
    }
}
