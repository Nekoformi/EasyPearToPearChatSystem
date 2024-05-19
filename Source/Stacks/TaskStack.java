package Source.Stacks;

import Source.Client;
import Source.Tasks.Duplicate;
import Source.Utils.Message;
import Source.Utils.Util;

import java.util.*;

public class TaskStack {
    Client client;

    List<Task> taskStack = new ArrayList<Task>();

    public TaskStack(Client client) {
        this.client = client;
    }

    public Task run(Task task) {
        taskStack.add(task);

        task.start();

        return task;
    }

    public Task get(String id) {
        return taskStack.stream().filter(task -> task.work.id.equals(id)).findFirst().orElse(null);
    }

    public synchronized Task execute(Node node, Message message, int dataLength, Task task) {
        if ((dataLength == 0 && message.data == null) || dataLength == message.data.length) {
            Task registeredTask = get(message.id);

            if (registeredTask == null && task != null) {
                setTaskLog("new", message, node);

                return run(task.set(client, node, message));
            } else if (registeredTask != null && task == null) {
                if (message.command.equals("dup")) {
                    setTaskLog("rejected", registeredTask.work, node);
                } else {
                    setTaskLog("replied", message, node);
                }

                registeredTask.receive(node, message);

                return registeredTask;
            } else if (registeredTask != null && task != null) {
                setTaskLog("duplicated", message, node);

                new Duplicate(client, node, message.set("dup", null)).start();
            } else {
                setTaskLog("unknown", message, node);
            }
        } else {
            setTaskLog("invalid", message, node);
        }

        return null;
    }

    public synchronized void discard(String id) {
        taskStack.remove(taskStack.indexOf(get(id)));
    }

    public void setTaskLog(String type, Message message, Node node) {
        client.systemConsole
                .pushSubLine("Receive " + type + " task (#" + message.id + "): \"" + message.command + "\" from " + Util.getSocketInfoString(node.socket));
    }
}
