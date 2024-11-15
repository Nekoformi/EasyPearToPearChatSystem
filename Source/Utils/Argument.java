package Source.Utils;

import java.util.*;

public class Argument {
    public class ArgumentItem {
        public String name;
        public String content;

        public ArgumentItem(String item) {
            set(item);
        }

        public ArgumentItem(String name, String content) {
            set(name, content);
        }

        public ArgumentItem get() {
            if (name != null) {
                return this;
            } else {
                return null;
            }
        }

        public ArgumentItem set(String item) {
            if (item.matches("-[^=]+=[^=]+")) {
                String[] buf = item.split("=");

                return set(buf[0].substring(1), buf[1]);
            } else if (item.matches("-[^=]+=?")) {
                return set(item.substring(1), null);
            } else {
                System.err.println("The argument \"" + item + "\" is not a correct statement.");

                return null;
            }
        }

        public ArgumentItem set(String name, String content) {
            if (name != null)
                this.name = name;

            if (content != null)
                this.content = content;

            return get();
        }
    }

    private List<ArgumentItem> argument = new ArrayList<ArgumentItem>();

    public Argument(String line) {
        set(line);
    }

    public Argument(String[] argv) {
        set(argv);
    }

    public ArgumentItem[] get() {
        if (argument.size() != 0) {
            return argument.toArray(new ArgumentItem[argument.size()]);
        } else {
            return null;
        }
    }

    public ArgumentItem[] set(String line) {
        return set(Util.specialSplitString(line, " ", "\"\'", "\"\'"));
    }

    public ArgumentItem[] set(String[] argv) {
        if (argv == null || argv.length == 0)
            return null;

        for (int i = 0; i < argv.length; i++) {
            ArgumentItem item = new ArgumentItem(argv[i]).get();

            if (item != null)
                argument.add(item);
        }

        return get();
    }

    public static boolean check(ArgumentItem item, int type) {
        if (item == null || item.get() == null) {
            System.err.println("Item does not exist.");

            return false;
        }

        String rec = Util.checkStringType(item.content, type);

        if (rec == null) {
            return true;
        } else {
            System.err.println("The item is " + rec + ".");

            return false;
        }
    }

    public static boolean check(ArgumentItem item, String name, String regex) {
        if (item == null || item.get() == null) {
            System.err.println("Item does not exist.");

            return false;
        }

        if (item.content.matches(regex)) {
            return true;
        } else {
            System.err.println("The item is not " + name + ".");

            return false;
        }
    }
}
