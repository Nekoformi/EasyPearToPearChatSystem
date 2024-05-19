package Source.Tasks;

import Source.Client;
import Source.Stacks.Node;
import Source.Stacks.Task;
import Source.Utils.Message;

public class Duplicate extends Task {
    public Duplicate() {}

    public Duplicate(Client client, Node node, Message work) {
        super(client, node, work);
    }

    public void run() {
        node.sendMessage("dup", work.id);

        done("-");
    }
}
