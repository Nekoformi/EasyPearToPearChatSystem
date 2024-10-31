package Source.Stacks;

import Source.Client;
import Source.Utils.Message;
import Source.Utils.Util;

public class Task extends Thread {
    // Note: Taskは特定のNodeもしくはClientによって作成されるが、Commandの内容に応じて複数のNodeが返信を行う場合がある。

    protected volatile boolean dead = false;
    protected volatile boolean done = false;

    public Client client;
    public Node node;
    public Message work;

    public int waitingTime = -1;

    public Task() {}

    public Task(Client client, Node node, Message work) {
        set(client, node, work);
    }

    public void waitAndStart(int waitingTime) {
        client.systemConsole.pushSubLine("Task (#" + work.id + ") will start in " + String.valueOf(waitingTime) + " milliseconds...");

        this.waitingTime = waitingTime;

        start();
    }

    public Task set(Client client, Node node, Message work) {
        this.client = client;
        this.node = node;
        this.work = work;

        return this;
    }

    public void run() {
        if (waitingTime > 0) {
            try {
                wait(waitingTime);
            } catch (InterruptedException e) {
                // None
            } catch (Exception e) {
                client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "Something is wrong."));

                return;
            }
        }

        // This is a function that is overridden by inherited classes.
    }

    public void receive(Node replyNode, Message replyWork) {
        // This is a function that is overridden by inherited classes.
    }

    public void done(String id) {
        if (id.equals("!") || id.equals("-") || id.equals(work.id)) {
            done = true;

            if (!id.equals("-")) {
                client.taskStack.discard(work.id);

                client.systemConsole.pushSubLine("Close the task (#" + work.id + ").");
            }

            interrupt();
        }
    }
}
