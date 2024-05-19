package Source.Stacks;

import Source.Client;
import Source.Utils.Message;

public class Task extends Thread {
    // Note: Taskは特定のNodeもしくはClientによって作成されるが、Commandの内容に応じて複数のNodeが返信を行う場合がある。

    protected volatile boolean dead = false;
    protected volatile boolean done = false;

    public Client client;
    public Node node;
    public Message work;

    public Task() {}

    public Task(Client client, Node node, Message work) {
        set(client, node, work);
    }

    public Task set(Client client, Node node, Message work) {
        this.client = client;
        this.node = node;
        this.work = work;

        return this;
    }

    public void run() {
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
