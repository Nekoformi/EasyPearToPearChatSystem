package Source;

import Source.Utils.Argument;
import Source.Utils.Argument.ArgumentItem;
import Source.Utils.Console;
import Source.Utils.Util;

import com.formdev.flatlaf.FlatDarculaLaf;

public class Main {
    public static void main(String[] argv) {
        String name = null;

        int x = 0;
        int y = 0;
        int w = 640;
        int h = 480;
        boolean c = false;
        boolean m = false;

        String joinAddressPort = null;
        String listeningPort = null;

        ArgumentItem[] argument = new Argument(argv).get();

        if (argument != null) {
            for (int i = 0; i < argument.length; i++) {
                ArgumentItem item = argument[i];

                switch (item.name) {
                case "x":
                case "left":
                    if (Argument.check(item, Util.TYPE_INTEGER))
                        x = Integer.parseInt(item.content);

                    break;
                case "y":
                case "top":
                    if (Argument.check(item, Util.TYPE_INTEGER))
                        y = Integer.parseInt(item.content);

                    break;
                case "w":
                case "width":
                    if (Argument.check(item, Util.TYPE_UNSIGNED_INTEGER))
                        w = Integer.parseInt(item.content);

                    break;
                case "h":
                case "height":
                    if (Argument.check(item, Util.TYPE_UNSIGNED_INTEGER))
                        h = Integer.parseInt(item.content);

                    break;
                case "c":
                case "center":
                    c = true;

                    break;
                case "m":
                case "maximize":
                    m = true;

                    break;
                case "n":
                case "name":
                    if (Argument.check(item, Util.TYPE_STRING))
                        name = item.content;

                    break;
                case "create":
                    if (Argument.check(item, Util.TYPE_IP_PORT))
                        listeningPort = item.content;

                    break;
                case "join":
                    if (Argument.check(item, "network address + port number & port number", Util.IP_ADDRESS_PORT_REGEX + "," + Util.IP_PORT_REGEX)) {
                        String[] buf = item.content.split(",");

                        joinAddressPort = buf[0];
                        listeningPort = buf[1];
                    }

                    break;
                case "debug":
                    Console.DEBUG_LOG = true;

                    break;
                default:
                    System.err.println("The argument \"" + item.name + "\" is a non-existent option.");

                    break;
                }
            }
        }

        Client client = new Client(name);

        FlatDarculaLaf.setup();

        new Interface(client, x, y, w, h, c, m);

        client.userStack.updateUserList();

        StringBuffer title = new StringBuffer("Welcome to Easy Pear to Pear Chat System (E=CS)");

        title.append("\n- Set your name with /name <YOUR NAME>");
        title.append("\n- Create a network with /create <LISTENING PORT>");
        title.append("\n- Join the network with /join <ADDRESS>:<PORT> <LISTENING PORT>");
        title.append("\n- Leave the network with /leave");

        client.systemConsole.pushMainLine(title.toString());

        if (joinAddressPort == null && listeningPort != null)
            client.executeCommand("/create " + listeningPort);

        if (joinAddressPort != null && listeningPort != null)
            client.executeCommand("/join " + joinAddressPort + " " + listeningPort);
    }
}
