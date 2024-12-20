package Source.Stacks;

import Source.Client;
import Source.Tasks.GetUserList;
import Source.Tasks.PostUserProfile;
import Source.Tasks.RemoveUserProfile;
import Source.Tasks.UpdateUserProfile;
import Source.Utils.Message;
import Source.Utils.Util;

import java.awt.event.*;
import java.util.*;
import java.util.stream.*;
import javax.swing.event.*;

public class UserStack {
    private Client client;

    private List<User> userStack = new ArrayList<User>();

    public User myProfile;

    public UserStack(Client client) {
        this.client = client;

        this.myProfile = new User(client);
    }

    public synchronized User add(User user, boolean updateUserList) {
        if (user == null) {
            client.systemConsole.pushErrorLine("User does not exist.");

            return null;
        }

        User currentUser = get(user.id);

        if (currentUser == null) {
            userStack.add(user);

            client.systemConsole.pushMainLine("Add user profile: " + user.display());

            if (updateUserList)
                updateUserList();

            return user;
        } else {
            return update(user, updateUserList);
        }
    }

    public synchronized User add(String raw, boolean updateUserList) {
        User user = new User(client, raw).get();

        if (user != null) {
            return add(user, updateUserList);
        } else {
            return null;
        }
    }

    public synchronized User add(String id, String raw, boolean updateUserList) {
        User user = get(id);

        if (user == null) {
            return add(raw, updateUserList);
        } else {
            return update(user, updateUserList);
        }
    }

    public synchronized User update(User user, boolean updateUserList) {
        if (user == null) {
            client.systemConsole.pushErrorLine("User does not exist.");

            return null;
        }

        User currentUser = test(user.id);

        if (currentUser == null)
            return null;

        currentUser = currentUser.set(user.id, user.name, user.publicKey);

        if (currentUser == null) {
            client.systemConsole.pushErrorLine("Failed to update user profile.");

            return null;
        }

        client.systemConsole.pushMainLine("Update user profile: " + currentUser.display());

        if (updateUserList)
            updateUserList();

        return currentUser;
    }

    public synchronized User update(String raw, boolean updateUserList) {
        return update(new User(client, raw).get(), updateUserList);
    }

    public synchronized User update(String id, String raw, boolean updateUserList) {
        User user = test(id);

        if (user == null)
            return null;

        user = user.set(raw);

        if (user == null) {
            client.systemConsole.pushErrorLine("Failed to update user profile.");

            return null;
        }

        client.systemConsole.pushMainLine("Update user profile: " + user.display());

        if (updateUserList)
            updateUserList();

        return user;
    }

    public synchronized void remove(User user, boolean updateUserList) {
        if (user == null) {
            client.systemConsole.pushErrorLine("User does not exist.");

            return;
        }

        User currentUser = test(user.id);

        if (currentUser == null)
            return;

        client.systemConsole.pushMainLine("Remove user profile: " + currentUser.display());

        userStack.remove(currentUser);

        if (updateUserList)
            updateUserList();
    }

    public synchronized void remove(String id, boolean updateUserList) {
        User user = test(id);

        if (user == null)
            return;

        client.systemConsole.pushMainLine("Remove user profile: " + user.display());

        userStack.remove(user);

        if (updateUserList)
            updateUserList();
    }

    public User test(String id) {
        User res = get(id);

        if (res == null)
            client.systemConsole.pushErrorLine("The specified user (@" + id + ") does not exist.");

        return res;
    }

    public User get(String id) {
        if (myProfile.equals(id)) {
            return myProfile;
        } else {
            return userStack.stream().filter(user -> user.equals(id)).findFirst().orElse(null);
        }
    }

    public int count(boolean includeMyProfile) {
        return userStack.size() + (includeMyProfile ? 1 : 0);
    }

    public List<User> carbon(boolean includeMyProfile) {
        List<User> res = userStack.stream().collect(Collectors.toList());

        if (includeMyProfile)
            res.add(0, myProfile);

        return res;
    }

    public String[] list(boolean includeMyProfile) {
        List<String> res = userStack.stream().map(user -> user.display()).collect(Collectors.toList());

        if (includeMyProfile)
            res.add(0, myProfile.name + " (ME)");

        return res.toArray(new String[res.size()]);
    }

    public void displayUser() {
        StringBuffer res = new StringBuffer("Member:");

        carbon(true).stream().forEach(user -> res.append("\n- " + user.display()));

        client.systemConsole.pushMainLine(res.toString());
    }

    public void displayUserPublicKey() {
        StringBuffer res = new StringBuffer("Member's public key:");

        carbon(true).stream().forEach(user -> {
            res.append("\n" + Util.indent(0) + "- " + user.display() + ":");
            res.append("\n" + Util.indent(1) + "- Public key:");
            res.append("\n" + Util.indent(2) + "- Hex:");
            res.append("\n" + Util.breakByteArrayToHexString(user.publicKey.getEncoded(), 80, ' '));
            res.append("\n" + Util.indent(2) + "- Base64:");
            res.append("\n" + Util.breakString(user.publicKeyString, 80));
        });

        client.systemConsole.pushMainLine(res.toString());
    }

    public void clear() {
        userStack.clear();

        updateUserList();
    }

    public void setMyName(String userName) {
        myProfile.setName(userName);

        updateUserList();
        updateMyProfile();
    }

    public Task getUserList() {
        client.systemConsole.pushSubLine("Request user list...");

        Message message = new Message(client.systemConsole, "req-ul", "+", String.valueOf(Client.TIMEOUT), myProfile.publicKeyString);

        return client.taskStack.run(new GetUserList().set(client, null, message));
    }

    public void updateUserList() {
        List<User> userStack = carbon(false);

        client.memberCatalog.setListData(list(true));

        client.memberCatalog.setActionEvent(0, null);
        client.memberCatalog.setActionEvent(1, null);

        client.memberCatalog.setButtonLabel(0, null);
        client.memberCatalog.setButtonLabel(1, null);

        client.memberCatalog.setSelectEvent(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;

                int index = client.memberCatalog.getSelectedIndex() - 1;

                if (index >= 0) {
                    User user = userStack.get(index);

                    client.memberCatalog.setActionEvent(0, new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if (user.node == null) {
                                client.connectNode(user.id);
                            } else {
                                client.disconnectNode(user.node, false);
                            }
                        }
                    });

                    client.memberCatalog.setActionEvent(1, new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            // WIP
                        }
                    });

                    client.memberCatalog.setButtonLabel(0, user.node == null ? "Connect Node" : "Disconnect Node");
                    client.memberCatalog.setButtonLabel(1, null);
                } else if (index == -1) {
                    client.memberCatalog.setActionEvent(0, null);
                    client.memberCatalog.setActionEvent(1, null);

                    client.memberCatalog.setButtonLabel(0, null);
                    client.memberCatalog.setButtonLabel(1, null);
                }
            }
        });
    }

    public Task[] postMyProfile() {
        List<Task> res = new ArrayList<Task>();

        String id = "@" + myProfile.id;
        String content = myProfile.stringify();

        userStack.forEach(user -> {
            String targetId = "@" + user.id;
            String secureHash = client.generateSecureHashWithUserProfile(user.id, content);

            Message message = new Message(client.systemConsole, "pst-up", "+", String.valueOf(Client.TIMEOUT), id, content, targetId, secureHash);

            res.add(client.taskStack.run(new PostUserProfile().set(client, null, message)));
        });

        return res.toArray(new Task[res.size()]);
    }

    public Task updateMyProfile() {
        String id = "@" + myProfile.id;
        String content = myProfile.stringify();
        String secureHash = client.generateSecureHashWithMyProfile(content);

        Message message = new Message(client.systemConsole, "upd-up", "+", String.valueOf(Client.TIMEOUT), id, content, secureHash);

        return client.taskStack.run(new UpdateUserProfile().set(client, null, message));
    }

    public Task removeMyProfile() {
        String userId = "@" + myProfile.id;
        String taskId = Util.generateNoiseHexString(16);
        String secureHash = client.generateSecureHashWithMyProfile(taskId);

        Message message = new Message(client.systemConsole, "rem-up", taskId, String.valueOf(Client.TIMEOUT), userId, secureHash);

        return client.taskStack.run(new RemoveUserProfile().set(client, null, message));
    }
}
