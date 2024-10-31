package Source;

import Source.Utils.Argument;
import Source.Utils.Argument.ArgumentItem;
import Source.Utils.Console;
import Source.Utils.Util;

import com.formdev.flatlaf.FlatDarculaLaf;

public class Main {
    public static final String VERSION = "1.2";

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

        boolean useSSL = false;

        String n_cfp = null; // Custom node certificate file path
        String n_cpp = null; // Custom node certificate pass phrase
        String a_kfp = null; // Custom auth key store file path
        String a_kpp = null; // Custom auth key store pass phrase
        String c_cfp = null; // Custom server certificate file path
        String c_cpp = null; // Custom server certificate pass phrase
        String s_cfp = null; // Custom client certificate file path
        String s_cpp = null; // Custom client certificate pass phrase

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
                case "t":
                case "timeout":
                    if (Argument.check(item, Util.TYPE_UNSIGNED_INTEGER))
                        Client.TIMEOUT = item.content;

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
                case "ssl":
                    useSSL = true;

                    break;
                case "pkc-file":
                    if (Argument.check(item, Util.TYPE_STRING))
                        n_cfp = item.content;

                    break;
                case "pkc-pass":
                    if (Argument.check(item, Util.TYPE_STRING))
                        n_cpp = item.content;

                    break;
                case "jks-file":
                    if (Argument.check(item, Util.TYPE_STRING))
                        a_kfp = item.content;

                    break;
                case "jks-pass":
                    if (Argument.check(item, Util.TYPE_STRING))
                        a_kpp = item.content;

                    break;
                case "pkc-server-file":
                    if (Argument.check(item, Util.TYPE_STRING))
                        s_cfp = item.content;

                    break;
                case "pkc-server-pass":
                    if (Argument.check(item, Util.TYPE_STRING))
                        s_cpp = item.content;

                    break;
                case "pkc-client-file":
                    if (Argument.check(item, Util.TYPE_STRING))
                        c_cfp = item.content;

                    break;
                case "pkc-client-pass":
                    if (Argument.check(item, Util.TYPE_STRING))
                        c_cpp = item.content;

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

        if (useSSL) {
            if (n_cfp != null || s_cfp == null || c_cfp == null) {
                s_cfp = n_cfp;
                c_cfp = n_cfp;
            }

            if (n_cpp != null || s_cpp == null || c_cpp == null) {
                s_cpp = n_cpp;
                c_cpp = n_cpp;
            }

            client.initializeSSL(s_cfp, s_cpp, c_cfp, c_cpp, a_kfp, a_kpp);
        }

        if (joinAddressPort == null && listeningPort != null)
            client.executeCommand("/create " + listeningPort);

        if (joinAddressPort != null && listeningPort != null)
            client.executeCommand("/join " + joinAddressPort + " " + listeningPort);
    }
}
