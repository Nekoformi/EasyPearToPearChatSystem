package Source.Utils;

public class Message {
    public Console console;

    public String command;
    public String id;
    public String[] data;

    public Message() {
        console = null;

        command = null;
        id = null;
        data = null;
    }

    public Message(Console console, String message, boolean needTaskId) {
        this.console = console;

        set(message, needTaskId); // "/COMMAND #ID DATA DATA DATA ..."
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

        this.data = data;

        return get();
    }

    public String stringify() {
        StringBuffer res = new StringBuffer("/" + command + " #" + id);

        if (data != null && data.length != 0) {
            for (int i = 0; i < data.length; i++)
                res.append(" " + data[i]);
        }

        return res.toString();
    }

    public String display(int omitDataByteLength) {
        StringBuffer res = new StringBuffer("/" + command + " #" + id);

        if (data != null && data.length != 0) {
            for (int i = 0; i < data.length; i++)
                res.append(" " + Util.omitString(data[i], omitDataByteLength, true));
        }

        return res.toString();
    }

    public boolean check(int dataIndex, int type) {
        if (data == null || data.length < dataIndex + 1 || data[dataIndex] == null) {
            pushErrorLine("Data does not exist.", dataIndex);

            return false;
        }

        String rec = Util.checkStringType(data[dataIndex], type);

        if (rec == null) {
            return true;
        } else {
            pushErrorLine("The data is " + rec + ".", dataIndex);

            return false;
        }
    }

    public String join(int dataIndex) {
        return String.join(" ", Util.copyStringArray(data, dataIndex, -1));
    }

    void pushErrorLine(String text) {
        console.pushErrorLine("Invalid message received: " + text);
    }

    void pushErrorLine(String text, int index) {
        console.pushErrorLine("Invalid message received (Argument " + String.valueOf(index) + "): " + text);
    }
}
