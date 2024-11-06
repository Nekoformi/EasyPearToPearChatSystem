package Source.Stacks;

import Source.Client;
import Source.Utils.Util;

import java.security.*;
import java.security.interfaces.*;
import java.util.*;
import java.util.stream.*;

import javax.crypto.*;

public class OuroborosNode {
    public static final boolean DISPLAY_ONN_PROCESS = true;
    public static final boolean DISPLAY_ONN_PROCESS_FULL_CONTENT = false;

    public static final int MAX_MESSAGE_DATA_SIZE = 65536 / 2;
    public static final int MIN_DUMMIES_NUM = 2;

    public static final byte FLAG_BYTE_NULL = 0;
    public static final byte FLAG_BYTE_SYNCHRONIZE = 1;
    public static final byte FLAG_BYTE_ACKNOWLEDGE = 2;
    public static final byte FLAG_BYTE_DUMMY = 3;
    public static final byte FLAG_BYTE_POST = 4;
    public static final byte FLAG_BYTE_RECEIVE = 5;
    public static final byte FLAG_BYTE_FINISH = 6;
    public static final byte FLAG_BYTE_DELETE = 7;
    public static final byte FLAG_BYTE_WAIT = 8;
    public static final byte FLAG_BYTE_REPEAT = 9;

    public static final String FLAG_NAME_NULL = "NUL";
    public static final String FLAG_NAME_SYNCHRONIZE = "SYN";
    public static final String FLAG_NAME_ACKNOWLEDGE = "ACK";
    public static final String FLAG_NAME_DUMMY = "DUM";
    public static final String FLAG_NAME_POST = "PST";
    public static final String FLAG_NAME_RECEIVE = "REC";
    public static final String FLAG_NAME_FINISH = "FIN";
    public static final String FLAG_NAME_DELETE = "DEL";
    public static final String FLAG_NAME_WAIT = "WAI";
    public static final String FLAG_NAME_REPEAT = "REP";

    public static final int AES_BLOCK_SIZE = 16; // 128 bit = 16 byte
    public static final int AES_COMMON_KEY_SIZE = 32; // 256 bit = 32 byte
    public static final int RSA_PUBLIC_KEY_SIZE = 162; // 1024 bit = 128 byte
    public static final int RSA_PRIVATE_KEY_SIZE = 128; // 1024 bit = 128 byte
    public static final int AES_COMMON_KEY_SIZE_ENCRYPTED_BY_RSA_PUBLIC_KEY = 128;
    public static final int DEFAULT_PROPERTY_SIZE = 16 + 4 + 1 + 1 + 4 + 4;

    public static final byte MESSAGE_TYPE_STRING = (byte)0x00;
    public static final byte MESSAGE_TYPE_BINARY_SND = (byte)0x01;
    public static final byte MESSAGE_TYPE_BINARY_REQ = (byte)0x02;
    public static final byte MESSAGE_TYPE_FINISH = (byte)0x03;
    public static final byte MESSAGE_TYPE_SYNCHRONIZE = (byte)0x04;
    public static final byte MESSAGE_TYPE_ACKNOWLEDGE = (byte)0x05;
    public static final byte MESSAGE_TYPE_NULL = (byte)0xFF;

    public static final int ONN_LAYER_3_PROPERTY_SIZE = 4;
    public static final int ONN_LAYER_4_DUM_DATA_SIZE = 4;
    public static final int ONN_LAYER_4_REC_DATA_SIZE = 4;

    public Client client;

    public User myself;
    public User target;

    public class MapStructure {
        // WARNING: Never create infinite loops! (Seriously, I'm not ouroboros joke!)

        public User user;
        public String flag = FLAG_NAME_DUMMY;
        public List<MapStructure> next;

        public MapStructure() {}

        public MapStructure(User user) {
            this.user = user;
        }

        public MapStructure(User user, String flag) {
            this.user = user;
            this.flag = flag;
        }

        public MapStructure(User user, MapStructure next) {
            this.user = user;
            this.next = Util.createExpandableList(next);
        }

        public MapStructure(User user, List<MapStructure> next) {
            this.user = user;
            this.next = new ArrayList<MapStructure>(next);
        }

        public MapStructure(MapStructure original, boolean copyNext) {
            this.user = original.user;
            this.flag = original.flag;

            if (copyNext)
                this.next = new ArrayList<MapStructure>(original.next);
        }

        public MapStructure(MapStructure original, MapStructure next) {
            this.user = original.user;
            this.flag = original.flag;
            this.next = Util.createExpandableList(next);
        }

        public MapStructure(MapStructure original, List<MapStructure> next) {
            this.user = original.user;
            this.flag = original.flag;
            this.next = new ArrayList<MapStructure>(next);
        }

        public MapStructure(List<User> userList) {
            this.user = userList.remove(0);

            if (userList.size() > 0)
                this.next = Util.createExpandableList(new MapStructure(userList));
        }

        // public MapStructure(List<User> userList, String data, char userSplit, char itemSplit) {}

        public int getTargetIndex(User target, boolean searchSubBranch) {
            if (target.equals(user)) {
                return 0;
            } else if (next != null) {
                int rec = -1;

                if (searchSubBranch) {
                    for (MapStructure item : next) {
                        int buf = item.getTargetIndex(target, searchSubBranch);

                        if (buf != -1) {
                            rec = buf;

                            break;
                        }
                    }
                } else {
                    rec = next.get(0).getTargetIndex(target, searchSubBranch);
                }

                if (rec != -1) {
                    return rec + 1;
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        }

        public MapStructure getMainBranch(int index) {
            if (index > 0) {
                return next.get(0).getMainBranch(index - 1);
            } else {
                return this;
            }
        }

        public int getMainBranchLength() {
            if (next != null) {
                return next.get(0).getMainBranchLength() + 1;
            } else {
                return 1;
            }
        }

        public List<User> getMainBranchArray() {
            if (next != null && next.size() > 0) {
                List<User> rec = next.get(0).getMainBranchArray();

                rec.add(0, user);

                return rec;
            } else {
                return Util.createExpandableList(user);
            }
        }

        public MapStructure getMainBranchPart(int offset, int length, String terminalFlag) {
            if (offset > 0) {
                if (next != null) {
                    return next.get(0).getMainBranchPart(offset - 1, length, terminalFlag);
                } else {
                    return null;
                }
            } else if (next != null && (length > 1 || length == -1)) {
                List<MapStructure> res = new ArrayList<MapStructure>();

                for (int i = 0; i < next.size(); i++) {
                    MapStructure item = next.get(i);

                    if (length != -1 && i == 0) {
                        res.add(item.getMainBranchPart(0, length - 1, terminalFlag));
                    } else {
                        res.add(item.getMainBranchPart(0, -1, terminalFlag));
                    }
                }

                return new MapStructure(this, res);
            } else if (length != 0) {
                return new MapStructure(user, terminalFlag != null ? terminalFlag : flag);
            } else {
                return null;
            }
        }

        public int count() {
            int res = 1;

            if (next != null)
                for (MapStructure item : next)
                    res += item.count();

            return res;
        }

        public void createCircleTail() {
            SecureRandom random = new SecureRandom();

            createCircleTail(random.nextInt(getMainBranchLength() + 1), this);
        }

        public void createCircleTail(int length) {
            createCircleTail(length, this);
        }

        public void createCircleTail(int length, MapStructure root) {
            if (next != null) {
                next.get(0).createCircleTail(length, root);
            } else {
                if (length > 0) {
                    next = Util.createExpandableList(root.getMainBranchPart(0, length, "DEL"));
                } else {
                    flag = "DEL";
                }
            }
        }

        public String getFlag(User user) {
            String res = null;

            if (this.user.equals(user)) {
                res = flag;
            } else if (next != null) {
                for (MapStructure item : next) {
                    String rec = item.getFlag(user);

                    if (rec != null) {
                        res = rec;

                        break;
                    }
                }
            }

            return res;
        }

        public void setFlag(User user, String flag) {
            if (this.user.equals(user)) {
                this.flag = flag;
            } else if (next != null) {
                for (MapStructure item : next)
                    item.setFlag(user, flag);
            }
        }

        public void convertFlag(String before, String after) {
            if (flag.equals(before))
                flag = after;

            if (next != null)
                for (MapStructure item : next)
                    item.convertFlag(before, after);
        }

        public void swapFlag(User userA, User userB) {
            String userFlagA = getFlag(userA);
            String userFlagB = getFlag(userB);

            setFlag(userA, userFlagB);
            setFlag(userB, userFlagA);
        }

        public void getUser(List<User> res, String flag) {
            if (this.flag.equals(flag)) {
                res.add(user);
            } else if (next != null) {
                for (MapStructure item : next)
                    item.getUser(res, flag);
            }
        }

        public MapStructure copy() {
            MapStructure res = new MapStructure();

            res.user = user;
            res.flag = flag;

            if (next != null) {
                res.next = new ArrayList<MapStructure>();

                for (MapStructure item : next)
                    res.next.add(item.copy());
            }

            return res;
        }

        public void concat(MapStructure prop) {
            if (next == null) {
                next = Util.createExpandableList(prop);
            } else {
                next.get(0).concat(prop);
            }
        }

        public void concat(MapStructure prop, boolean searchSubBranch) {
            if (prop.user.equals(user)) {
                if (next == null)
                    next = new ArrayList<MapStructure>();

                next.addAll(prop.next);
            } else if (next != null) {
                if (searchSubBranch) {
                    for (MapStructure item : next)
                        item.concat(prop, searchSubBranch);
                } else {
                    next.get(0).concat(prop, searchSubBranch);
                }
            }
        }

        public void concat(MapStructure prop, int index, boolean searchSubBranch) {
            if (index <= 0) {
                if (next == null)
                    next = new ArrayList<MapStructure>();

                next.add(prop);
            } else if (next != null) {
                if (searchSubBranch) {
                    for (MapStructure item : next)
                        item.concat(prop, index - 1, searchSubBranch);
                } else {
                    next.get(0).concat(prop, index - 1, searchSubBranch);
                }
            }
        }

        public void concat(MapStructure prop, User target, boolean searchSubBranch) {
            if (target.equals(user)) {
                if (next == null)
                    next = new ArrayList<MapStructure>();

                next.add(prop);
            } else if (next != null) {
                if (searchSubBranch) {
                    for (MapStructure item : next)
                        item.concat(prop, target, searchSubBranch);
                } else {
                    next.get(0).concat(prop, target, searchSubBranch);
                }
            }
        }

        public int insert(User connectBeforeNodeUser, User connectAfterNodeUser, User insertNodeUser, String flag) {
            int res = 0;

            if (next != null) {
                if (this.user.equals(connectBeforeNodeUser)) {
                    for (int i = 0; i < next.size(); i++) {
                        MapStructure rec = next.get(i);

                        if (rec.user.equals(connectAfterNodeUser)) {
                            MapStructure rem = new MapStructure(insertNodeUser, flag);

                            rem.next = Util.createExpandableList(rec);

                            next.set(i, rem);

                            res++;
                        }
                    }
                }

                for (MapStructure item : next)
                    res += item.insert(connectBeforeNodeUser, connectAfterNodeUser, insertNodeUser, flag);
            }

            return res;
        }

        public int add(User connectNodeUser, User addNodeUser, String flag) {
            int res = 0;

            if (this.user.equals(connectNodeUser)) {
                MapStructure rec = new MapStructure(addNodeUser, flag);

                if (next != null) {
                    next.add(rec);
                } else {
                    next = Util.createExpandableList(rec);
                }

                res++;
            }

            if (next != null)
                for (MapStructure item : next)
                    res += item.add(connectNodeUser, addNodeUser, flag);

            return res;
        }

        public int replace(User targetNodeUser, User replaceNodeUser) {
            int res = 0;

            if (this.user.equals(targetNodeUser)) {
                this.user = replaceNodeUser;

                res++;
            }

            if (next != null)
                for (MapStructure item : next)
                    res += item.replace(targetNodeUser, replaceNodeUser);

            return res;
        }

        public int reject(User targetNodeUser) {
            int res = 0;

            if (next != null) {
                for (int i = 0; i < next.size(); i++) {
                    MapStructure rec = next.get(i);

                    res += rec.reject(targetNodeUser);

                    if (rec.user.equals(targetNodeUser)) {
                        next.remove(rec);

                        if (rec.next != null)
                            next.addAll(rec.next);

                        res++;
                    }
                }
            }

            return res;
        }

        public static MapStructure swap(MapStructure mapStructure, User split) {
            int splitIndex = mapStructure.getTargetIndex(split, false);
            MapStructure splitBuf = mapStructure.getMainBranchPart(0, splitIndex, null);

            mapStructure.concat(splitBuf);

            return mapStructure.getMainBranch(splitIndex);
        }

        public static MapStructure swap(MapStructure mapStructure, User userA, User userB) {
            int userIndexA = mapStructure.getTargetIndex(userA, false);
            int userIndexB = mapStructure.getTargetIndex(userB, false);

            if (userIndexA < userIndexB) {
                MapStructure mapStructureStoA = mapStructure.getMainBranchPart(0, userIndexA, null);
                MapStructure mapStructureAtoB = mapStructure.getMainBranchPart(userIndexA, userIndexB - userIndexA, null);
                MapStructure mapStructureBtoE = mapStructure.getMainBranchPart(userIndexB, -1, null);

                if (mapStructureStoA == null || mapStructureAtoB == null || mapStructureBtoE == null)
                    return null;

                mapStructureBtoE.concat(mapStructureAtoB);
                mapStructureStoA.concat(mapStructureBtoE);

                return mapStructureStoA;
            } else {
                MapStructure mapStructureStoB = mapStructure.getMainBranchPart(0, userIndexB, null);
                MapStructure mapStructureBtoA = mapStructure.getMainBranchPart(userIndexB, userIndexA - userIndexB, null);
                MapStructure mapStructureAtoE = mapStructure.getMainBranchPart(userIndexA, -1, null);

                if (mapStructureStoB == null || mapStructureBtoA == null || mapStructureAtoE == null)
                    return null;

                mapStructureAtoE.concat(mapStructureBtoA);
                mapStructureStoB.concat(mapStructureAtoE);

                return mapStructureStoB;
            }
        }

        public String encode(char userSplit, char itemSplit) {
            StringBuffer rec = new StringBuffer();

            if (next != null)
                for (int i = 0; i < next.size(); i++)
                    rec.append(next.get(i).encode(userSplit, itemSplit) + (i != next.size() - 1 ? String.valueOf(itemSplit) : ""));

            return user.id + String.valueOf(userSplit) + flag + "(" + rec.toString() + ")"; // "USER:FLAG(USER:FLAG(), USER:FLAG(...) ...)"
        }

        public MapStructure decode(List<User> userList, String data, char userSplit, char itemSplit) {
            int head = 0;
            int nest = 0;
            int prop = -1;

            String userData = null;
            String itemData = null;

            for (int i = 0; i < data.length(); i++) {
                if (data.charAt(i) == '(') {
                    if (nest == 0) {
                        if (userData == null && itemData == null) {
                            userData = data.substring(head, i);

                            head = i + 1;
                            prop = 0;
                        } else {
                            prop = -1;

                            break;
                        }
                    }

                    nest++;
                } else if (data.charAt(i) == ')') {
                    nest--;

                    if (userData != null && nest == 1) {
                        itemData = data.substring(head, i + 1);

                        MapStructure rec = new MapStructure().decode(userList, itemData, userSplit, itemSplit);

                        if (rec == null) {
                            prop = -1;

                            break;
                        } else {
                            prop++;
                        }

                        if (next == null) {
                            next = Util.createExpandableList(rec);
                        } else {
                            next.add(rec);
                        }

                        if (data.charAt(i + 1) == itemSplit) {
                            head = i + 2;
                        } else {
                            break;
                        }
                    }
                }
            }

            if (prop == -1)
                return null;

            String[] userDataBuf = userData.split(String.valueOf(userSplit));

            if (userDataBuf.length != 2 || !("@" + userDataBuf[0]).matches(Util.USER_ID_REGEX) || !userDataBuf[1].matches(Util.ONN_FLAG_REGEX))
                return null;

            user = userList.stream().filter(user -> user.equals(userDataBuf[0])).findFirst().orElse(null);
            flag = userDataBuf[1];

            if (user == null || flag == null)
                return null;

            return this;
        }

        public String display() {
            return display(0, 0);
        }

        String display(int nest, int branchType) {
            StringBuffer res = new StringBuffer(Util.repeat(": ", nest) + (branchType == 0 ? "\\ " : "| ") + "[" + flag + "] " + user.display());

            if (next != null) {
                if (next.size() >= 2)
                    for (int i = 1; i < next.size(); i++)
                        res.append("\n" + next.get(i).display(nest + 1, 0));

                if (next.size() >= 1)
                    res.append("\n" + next.get(0).display(nest, 1));
            }

            return res.toString();
        }
    }

    public class Map {
        List<User> dummy;
        List<User> map;

        MapStructure mapStructure;

        User post;
        User send;

        int postIndex = -1; // = 0;
        int sendIndex = -1;

        public Map(List<User> dummy, User post, User send) {
            this.post = post;
            this.send = send;

            setDummy(dummy);
            setTarget(post, send);

            createMapStructure();
        }

        public Map(List<User> candidate, User post, User send, int pickDummyNum) {
            this.post = post;
            this.send = send;

            setDummy(candidate, pickDummyNum);
            setTarget(post, send);

            createMapStructure();
        }

        public Map(List<User> member, String mapStructureData) {
            if (!decodeMapStructure(member, mapStructureData))
                return;

            List<User> postBuf = new ArrayList<User>();
            List<User> sendBuf = new ArrayList<User>();

            mapStructure.getUser(postBuf, FLAG_NAME_POST);
            mapStructure.getUser(sendBuf, FLAG_NAME_RECEIVE);

            if (postBuf.size() != 1 || sendBuf.size() != 1)
                return;

            post = postBuf.get(0);
            send = sendBuf.get(0);

            setMap(mapStructure.getMainBranchArray());
        }

        public Map(List<User> member, User post, User send, String mapStructureData) {
            if (!decodeMapStructure(member, mapStructureData))
                return;

            setMap(mapStructure.getMainBranchArray(), post, send);
        }

        public int check() {
            if (dummy == null || dummy.size() <= 0) {
                client.systemConsole.pushErrorLine("The dummy list is not set correctly.");

                return 1;
            } else if (map == null || map.size() <= 0) {
                client.systemConsole.pushErrorLine("The map list is not set correctly.");

                return 2;
            } else if (mapStructure == null) {
                client.systemConsole.pushErrorLine("The map structure is not set correctly.");

                return 3;
            } else if (post == null || send == null) {
                client.systemConsole.pushErrorLine("The sender or recipient is not set correctly.");

                return 4;
            } else if (postIndex == -1 || sendIndex == -1) {
                client.systemConsole.pushErrorLine("The sender index or recipient index is not set correctly.");

                return 5;
            } else if (post.equals(send)) {
                client.systemConsole.pushErrorLine("Can't set yourself as the recipient.");

                return 6;
            } else {
                return 0;
            }
        }

        public int checkUserList(User targetUser) {
            List<User> rec = map.stream().filter(user -> user.equals(targetUser)).collect(Collectors.toList());

            switch (rec.size()) {
            case 0:
                return 0;
            case 1:
                return 1;
            default:
                return -1;
            }
        }

        List<User> filterList(List<User> userList) {
            return userList.stream().filter(user -> (post == null || !user.equals(post)) && (send == null || !user.equals(send))).collect(Collectors.toList());
        }

        void setDummy(List<User> dummy) {
            this.dummy = filterList(dummy);
        }

        void setDummy(List<User> candidate, int pickDummyNum) {
            dummy = Util.pickRandomArray(filterList(candidate), pickDummyNum);
        }

        void setTarget(User post, User send) {
            if (dummy == null)
                return;

            map = new ArrayList<User>(dummy);

            // this.post = post;
            // this.send = send;

            SecureRandom random = new SecureRandom();

            postIndex = 0; // random.nextInt(map.size()) + 1;
            map.add(postIndex, post);

            sendIndex = random.nextInt(map.size()) + 1;
            map.add(sendIndex, send);

            // if (postIndex >= sendIndex)
            // postIndex++;
        }

        void setMap(List<User> map) {
            this.map = new ArrayList<User>(map);

            setDummy(this.map);
            setTargetIndex();
        }

        void setMap(List<User> map, User post, User send) {
            this.map = new ArrayList<User>(map);

            this.post = post;
            this.send = send;

            setDummy(this.map);
            setTargetIndex();
        }

        void setTargetIndex() {
            postIndex = map.indexOf(map.stream().filter(user -> user.equals(post)).findFirst().orElse(null));
            sendIndex = map.indexOf(map.stream().filter(user -> user.equals(send)).findFirst().orElse(null));
        }

        void setTargetIndex(User post, User send) {
            this.post = post;
            this.send = send;

            setTargetIndex();
        }

        public int swap() {
            if (map == null || dummy == null)
                return 1;

            if (postIndex == -1 || sendIndex == -1)
                return 2;

            swapMap();

            if ((mapStructure = MapStructure.swap(mapStructure, post)) == null)
                return 3;

            mapStructure.swapFlag(post, send);

            return 0;
        }

        void swapMap() {
            List<User> rec = new ArrayList<User>();

            if (postIndex < sendIndex) {
                if (postIndex != 0)
                    rec.addAll(map.subList(0, postIndex));

                rec.addAll(map.subList(sendIndex, map.size()));
                rec.addAll(map.subList(postIndex, sendIndex));
            } else {
                if (sendIndex != 0)
                    rec.addAll(map.subList(0, sendIndex));

                rec.addAll(map.subList(postIndex, map.size()));
                rec.addAll(map.subList(sendIndex, postIndex));
            }

            swapTarget();

            map = rec;

            setDummy(map);
        }

        void swapTarget() {
            User postBuf = post;
            User sendBuf = send;

            post = sendBuf;
            send = postBuf;

            int postIndexBuf = postIndex;
            int sendIndexBuf = sendIndex;

            postIndex = sendIndexBuf;
            sendIndex = postIndexBuf;
        }

        public void createMapStructure() {
            mapStructure = new MapStructure(new ArrayList<User>(map));

            mapStructure.setFlag(myself, "PST");
            mapStructure.setFlag(target, "REC");
        }

        public MapStructure getMapStructure() {
            return mapStructure.copy();
        }

        public MapStructure getMapStructure(String newRecipientFlag) {
            MapStructure res = mapStructure.copy();

            res.convertFlag(FLAG_NAME_RECEIVE, newRecipientFlag);

            return res;
        }

        public MapStructure getMapStructureAddCircleTail() {
            return getMapStructureAddCircleTail(FLAG_NAME_FINISH);
        }

        public MapStructure getMapStructureAddCircleTail(int length) {
            return getMapStructureAddCircleTail(length, FLAG_NAME_FINISH);
        }

        public MapStructure getMapStructureAddCircleTail(String newRecipientFlag) {
            MapStructure res = mapStructure.copy();

            res.convertFlag(FLAG_NAME_RECEIVE, newRecipientFlag);
            res.createCircleTail();

            return res;
        }

        public MapStructure getMapStructureAddCircleTail(int length, String newRecipientFlag) {
            MapStructure res = mapStructure.copy();

            res.convertFlag(FLAG_NAME_RECEIVE, newRecipientFlag);
            res.createCircleTail(length);

            return res;
        }

        public void addThunder() {
            // WIP
        }

        public void addBranch() {
            // WIP
        }

        public void addCircle() {
            // WIP
        }

        public String encodeMapStructure() {
            return mapStructure.encode(':', ',');
        }

        public boolean decodeMapStructure(List<User> userList, String data) {
            mapStructure = new MapStructure().decode(userList, data, ':', ',');

            if (mapStructure == null) {
                client.systemConsole.pushErrorLine("Failed to decode map structure.");

                return false;
            } else {
                return true;
            }
        }

        public String displayMapStructure() {
            return mapStructure.display();
        }

        public boolean insertNodeToMapStructure(User connectBeforeNodeUser, User connectAfterNodeUser, User insertNodeUser, String flag) {
            int res = mapStructure.insert(connectBeforeNodeUser, connectAfterNodeUser, insertNodeUser, flag);

            if (res <= 0) {
                client.systemConsole.pushErrorLine("There is no user (@" + connectBeforeNodeUser.id + ") or user (@" + connectAfterNodeUser.id
                        + ") in the map of the Ouroboros Node Network.");

                return false;
            } else if (res >= 2) {
                client.systemConsole.pushWarningLine("Users (@" + connectBeforeNodeUser.id + " and @" + connectAfterNodeUser.id
                        + ") are duplicated in the map of the Ouroboros Node Network.");
            }

            return true;
        }

        public boolean addNodeToMapStructure(User connectNodeUser, User addNodeUser, String flag) {
            int res = mapStructure.add(connectNodeUser, addNodeUser, flag);

            if (res <= 0) {
                client.systemConsole.pushErrorLine("There is no user (@" + connectNodeUser.id + ") in the map of the Ouroboros Node Network.");

                return false;
            } else if (res >= 2) {
                client.systemConsole.pushWarningLine("User (@" + connectNodeUser.id + ") is duplicated in the map of the Ouroboros Node Network.");
            }

            return true;
        }

        public boolean replaceNodeToMapStructure(User targetNodeUser, User convertNodeUser) {
            int res = mapStructure.replace(targetNodeUser, convertNodeUser);

            if (res <= 0) {
                client.systemConsole.pushErrorLine("There is no user (@" + targetNodeUser.id + ") in the map of the Ouroboros Node Network.");

                return false;
            } else if (res >= 2) {
                client.systemConsole.pushWarningLine("User (@" + targetNodeUser.id + ") is duplicated in the map of the Ouroboros Node Network.");
            }

            return true;
        }

        public boolean rejectNodeToMapStructure(User targetNodeUser) {
            int res = mapStructure.reject(targetNodeUser);

            if (res <= 0) {
                client.systemConsole.pushErrorLine("There is no user (@" + targetNodeUser.id + ") in the map of the Ouroboros Node Network.");

                return false;
            } else if (res >= 2) {
                client.systemConsole.pushWarningLine("User (@" + targetNodeUser.id + ") is duplicated in the map of the Ouroboros Node Network.");
            }

            return true;
        }
    }

    Map map;

    List<String> messageStore = new ArrayList<String>();

    public OuroborosNode(Client client, User target, int pickDummyNum) {
        this.client = client;

        this.myself = client.userStack.myProfile;
        this.target = target;

        List<User> candidate = client.userStack.carbon(true);

        if (candidate.size() < 2 + MIN_DUMMIES_NUM) {
            client.systemConsole.pushErrorLine("There are not enough users to create ONN, which means the existence of ONN is pointless.");

            return;
        }

        int maxPickDummyNum = candidate.size() - 2;
        int minPickDummyNum = MIN_DUMMIES_NUM;

        if (pickDummyNum > maxPickDummyNum) {
            client.systemConsole.pushWarningLine("The maximum number of dummies is " + String.valueOf(maxPickDummyNum) + "!");

            map = new Map(candidate, myself, target, maxPickDummyNum);
        } else if (pickDummyNum < minPickDummyNum) {
            client.systemConsole.pushWarningLine("The minimum number of dummies is " + String.valueOf(minPickDummyNum) + "!");

            map = new Map(candidate, myself, target, minPickDummyNum);
        } else {
            map = new Map(candidate, myself, target, pickDummyNum);
        }
    }

    public OuroborosNode(Client client, String mapStructureData) {
        this.client = client;

        map = new Map(client.userStack.carbon(true), mapStructureData);

        myself = map.post;
        target = map.send;
    }

    public OuroborosNode(Client client, User posted, String mapStructureData) {
        this.client = client;

        this.myself = client.userStack.myProfile;
        this.target = posted;

        map = new Map(client.userStack.carbon(true), target, myself, mapStructureData);

        map.swap();
    }

    public int check() {
        if (client == null || myself == null || target == null) {
            return 1;
        } else if (map == null || map.check() != 0) {
            return 2;
        } else {
            return 0;
        }
    }

    public void set(Map map) {
        this.map = map;
    }

    public String encode() {
        return map.encodeMapStructure();
    }

    public String display() {
        return map.displayMapStructure();
    }

    public boolean insertNode(User connectBeforeNodeUser, User connectAfterNodeUser, User insertNodeUser, String flag) {
        return map.insertNodeToMapStructure(connectBeforeNodeUser, connectAfterNodeUser, insertNodeUser, flag);
    }

    public boolean addNode(User connectNodeUser, User addNodeUser, String flag) {
        return map.addNodeToMapStructure(connectNodeUser, addNodeUser, flag);
    }

    public boolean replaceNode(User targetNodeUser, User replaceNodeUser) {
        return map.replaceNodeToMapStructure(targetNodeUser, replaceNodeUser);
    }

    public boolean rejectNode(User targetNodeUser) {
        return map.rejectNodeToMapStructure(targetNodeUser);
    }

    public boolean addMessageStore(byte[] messageId) {
        return addMessageStore(Util.convertByteArrayToHexString(messageId));
    }

    public boolean addMessageStore(String messageId) {
        if (checkMessageStore(messageId)) {
            messageStore.add(messageId);

            return true;
        } else {
            client.systemConsole.pushWarningLine("Duplicate message (ONN#" + messageId + ") received.");

            return false;
        }
    }

    public boolean checkMessageStore(byte[] messageId) {
        return checkMessageStore(Util.convertByteArrayToHexString(messageId));
    }

    public boolean checkMessageStore(String messageId) {
        return messageStore.stream().filter(item -> item.equals(messageId)).findFirst().orElse(null) == null;
    }

    public byte[] createOuroborosData(String messageData) {
        byte[] messageId = Util.generateNoiseByte(16);

        return createOuroborosData(messageId, Util.convertStringToByteArray(messageData), MESSAGE_TYPE_STRING);
    }

    public byte[] createOuroborosData(byte[] messageData, byte type) {
        byte[] messageId = Util.generateNoiseByte(16);

        return createOuroborosData(messageId, messageData, type);
    }

    public byte[] createOuroborosSynchronizeData() {
        byte[] messageId = Util.generateNoiseByte(16);
        byte[] messageData = Util.convertStringToByteArray(FLAG_NAME_SYNCHRONIZE);
        MapStructure mapStructure = map.getMapStructure(FLAG_NAME_SYNCHRONIZE);

        return createOuroborosData(messageId, messageData, MESSAGE_TYPE_SYNCHRONIZE, mapStructure);
    }

    public byte[] createOuroborosAcknowledgeData(byte[] messageId, byte[] messageSize) {
        byte[] messageData = Util.convertStringToByteArray(FLAG_NAME_ACKNOWLEDGE);
        MapStructure mapStructure = map.getMapStructureAddCircleTail(FLAG_NAME_ACKNOWLEDGE);

        return createOuroborosData(messageId, messageSize, messageData, MESSAGE_TYPE_ACKNOWLEDGE, mapStructure, false);
    }

    public byte[] createOuroborosFinishData(byte[] messageId, byte[] messageSize) {
        byte[] messageData = Util.convertStringToByteArray(FLAG_NAME_FINISH);
        MapStructure mapStructure = map.getMapStructureAddCircleTail();

        return createOuroborosData(messageId, messageSize, messageData, MESSAGE_TYPE_FINISH, mapStructure, false);
    }

    public byte[] createOuroborosData(byte[] messageId, byte[] messageData, byte type) {
        return createOuroborosData(messageId, messageData, type, map.getMapStructure());
    }

    public byte[] createOuroborosData(byte[] messageId, byte[] messageSize, String messageData) {
        return createOuroborosData(messageId, messageSize, Util.convertStringToByteArray(messageData), MESSAGE_TYPE_STRING, map.getMapStructure(), false);
    }

    public byte[] createOuroborosData(byte[] messageId, byte[] messageSize, byte[] messageData, byte type) {
        return createOuroborosData(messageId, messageSize, messageData, type, map.getMapStructure(), false);
    }

    public byte[] createOuroborosData(byte[] messageId, byte[] messageData, byte type, MapStructure mapStructure) {
        List<Integer> noiseStore = generateSpaciousNoiseStore(messageData, mapStructure);
        byte[] messageSize = Util.convertIntToByteArray(calcOuroborosDataSize(messageData, mapStructure, new ArrayList<Integer>(noiseStore)));

        return createOuroborosData(messageId, messageSize, messageData, type, mapStructure, noiseStore);
    }

    public byte[] createOuroborosData(byte[] messageId, byte[] messageSize, byte[] messageData, byte type, MapStructure mapStructure, boolean isFinalData) {
        List<Integer> noiseStore = generateNoiseStore(messageData.length, Util.convertByteArrayToInt(messageSize), mapStructure, isFinalData);

        return createOuroborosData(messageId, messageSize, messageData, type, mapStructure, noiseStore);
    }

    List<Integer> generateSpaciousNoiseStore(byte[] messageData, MapStructure mapStructure) {
        return generateSpaciousNoiseStore(messageData.length, mapStructure);
    }

    List<Integer> generateSpaciousNoiseStore(int messageDataSize, MapStructure mapStructure) {
        MapStructure targetFinalMaximumMapStructure = getTargetFinalMaximumMapStructure(mapStructure);

        List<Integer> testNoiseStore = generateNoiseStore(targetFinalMaximumMapStructure, false);

        int testMessageSizeA = MAX_MESSAGE_DATA_SIZE + calcOuroborosDataSize(0, targetFinalMaximumMapStructure, testNoiseStore);
        int testMessageSizeB = calcOuroborosDataSize(messageDataSize, mapStructure, null);

        return generateNoiseStore(testMessageSizeA - testMessageSizeB, mapStructure, true);
    }

    MapStructure getTargetFinalMaximumMapStructure(MapStructure mapStructure) {
        MapStructure res = MapStructure.swap(mapStructure.copy(), target);

        res.swapFlag(myself, target);
        res.convertFlag(FLAG_NAME_RECEIVE, FLAG_NAME_FINISH);
        res.createCircleTail(res.getMainBranchLength());

        return res;
    }

    List<Integer> generateNoiseStore(MapStructure mapStructure, boolean stopTarget) {
        int sumNoiseSize = DEFAULT_PROPERTY_SIZE * 2 * mapStructure.count() * 2;

        return generateNoiseStore(sumNoiseSize, mapStructure, stopTarget);
    }

    List<Integer> generateNoiseStore(int newMessageSize, int prevMessageSize, MapStructure mapStructure, boolean stopTarget) {
        int rawMessageSize = calcOuroborosDataSize(newMessageSize, mapStructure, null);

        return generateNoiseStore(prevMessageSize - rawMessageSize, mapStructure, stopTarget);
    }

    List<Integer> generateNoiseStore(int sumNoiseSize, MapStructure mapStructure, boolean stopTarget) {
        if (stopTarget) {
            int targetIndex = mapStructure.getTargetIndex(target, false);
            int targetCount = mapStructure.getMainBranchPart(0, targetIndex + 1, null).count();

            return generateNoiseStore(sumNoiseSize, targetCount);
        } else {
            return generateNoiseStore(sumNoiseSize, mapStructure.count());
        }
    }

    List<Integer> generateNoiseStore(int sumNoiseSize, int storeCount) {
        SecureRandom random = new SecureRandom();

        List<Integer> res = new ArrayList<Integer>();

        int minNoiseSize = DEFAULT_PROPERTY_SIZE * 2;
        int maxNoiseSize = sumNoiseSize / storeCount;
        int setNoiseSize = 0;

        for (int i = 0; i < storeCount; i++) {
            int buf = Util.generateRandomInt(random, minNoiseSize, maxNoiseSize);

            res.add(buf);

            setNoiseSize += buf;
        }

        int remNoiseSize = (sumNoiseSize - setNoiseSize) / storeCount;

        for (int i = 0; i < storeCount; i++) {
            int buf = res.get(i);

            res.set(i, buf + remNoiseSize);
        }

        return res;
    }

    int calcOuroborosDataSize(byte[] messageData, MapStructure mapStructure, List<Integer> noiseStore) {
        return calcOuroborosDataSize(messageData.length, mapStructure, noiseStore);
    }

    int calcOuroborosDataSize(int messageDataSize, MapStructure mapStructure, List<Integer> noiseStore) {
        int res;

        int flagType = getFlagType(mapStructure.flag);

        if (flagType == 0 || flagType == 2) {
            res = calcOuroborosDataLayerSize2B(messageDataSize);
            res = calcOuroborosDataLayerSize1A(res, noiseStore != null ? Util.popListItem(noiseStore) : 0);
        } else if ((flagType == 1 || flagType == 3) && mapStructure.next != null) {
            if (mapStructure.next.size() == 1) {
                MapStructure next = mapStructure.next.get(0);

                res = calcOuroborosDataLayerSize2A(calcOuroborosDataSize(messageDataSize, next, noiseStore));
                res = calcOuroborosDataLayerSize1A(res, noiseStore != null ? Util.popListItem(noiseStore) : 0);
            } else if (mapStructure.next.size() >= 2) {
                int length = mapStructure.next.size();
                int[] rec = new int[length];

                for (int i = 0; i < length; i++) {
                    MapStructure next = mapStructure.next.get(i);

                    rec[i] = calcOuroborosDataLayerSize2A(calcOuroborosDataSize(messageDataSize, next, noiseStore));
                }

                res = calcOuroborosDataLayerSize1B(rec, noiseStore != null ? Util.popListItem(noiseStore) : 0);
            } else {
                res = 0;
            }
        } else if (flagType == 4 || mapStructure.next == null) {
            res = calcOuroborosDataLayerSize1C(0, noiseStore != null ? Util.popListItem(noiseStore) : 0);
        } else {
            res = 0;
        }

        return res;
    }

    byte[] createOuroborosData(byte[] messageId, byte[] messageSize, byte[] messageData, byte messageType, MapStructure mapStructure,
            List<Integer> noiseStore) {
        byte[] res;

        int flagType = getFlagType(mapStructure.flag);

        if (flagType == 0 || flagType == 2) {
            res = createOuroborosDataLayer2B(messageData);
            res = createOuroborosDataLayer1A(messageId, messageSize, convertFlagNameToByte(mapStructure.flag), messageType, res,
                    noiseStore != null ? Util.popListItem(noiseStore) : 0);
        } else if ((flagType == 1 || flagType == 3) && mapStructure.next != null) {
            if (mapStructure.next.size() == 1) {
                MapStructure next = mapStructure.next.get(0);

                res = createOuroborosDataLayer2A(createOuroborosData(messageId, messageSize, messageData, messageType, next, noiseStore), next.user);
                res = createOuroborosDataLayer1A(messageId, messageSize, convertFlagNameToByte(mapStructure.flag), MESSAGE_TYPE_NULL, res,
                        noiseStore != null ? Util.popListItem(noiseStore) : 0);
            } else {
                int length = mapStructure.next.size();
                byte[][] rec = new byte[length][];

                for (int i = 0; i < length; i++) {
                    MapStructure next = mapStructure.next.get(i);

                    rec[i] = createOuroborosDataLayer2A(createOuroborosData(messageId, messageSize, messageData, messageType, next, noiseStore), next.user);
                }

                res = createOuroborosDataLayer1B(messageId, messageSize, convertFlagNameToByte(mapStructure.flag), MESSAGE_TYPE_NULL, rec,
                        noiseStore != null ? Util.popListItem(noiseStore) : 0);
            }
        } else if (flagType == 4 || mapStructure.next == null) {
            res = createOuroborosDataLayer1C(messageId, messageSize, FLAG_BYTE_DELETE, MESSAGE_TYPE_NULL, 0,
                    noiseStore != null ? Util.popListItem(noiseStore) : 0);
        } else {
            res = null;
        }

        return res;
    }

    byte[] createOuroborosDataLayer2A(byte[] message, User next) {
        SecretKey commonKey = Util.generateAesCommonKey();

        byte[] _userId = Util.convertHexStringToByteArray(next.id);
        byte[] _userPublicKey = next.publicKey.getEncoded(); // = Util.convertBase64ToByteArray(next.publicKeyString);
        byte[] _userPublicKeySize = Util.convertIntToByteArray(RSA_PUBLIC_KEY_SIZE);
        byte[] _encryptedCommonKey = Util.encryptCommonKeyToByteArrayWithRsaPublicKey(commonKey, next.publicKey);
        byte[] _encryptedCommonKeySize = Util.convertIntToByteArray(AES_COMMON_KEY_SIZE_ENCRYPTED_BY_RSA_PUBLIC_KEY);
        byte[] _encryptedMessage = Util.encryptByteArrayWithAesCommonKey(message, commonKey);
        byte[] _encryptedMessageSize = Util.convertIntToByteArray(_encryptedMessage.length);

        if (DISPLAY_ONN_PROCESS) {
            StringBuffer log = new StringBuffer("Create ONN data:");

            appendProcessLogData(log, "Layer", "2-A (Data for Dummy)", -1);
            appendProcessLogData(log, "Next user ID", _userId, -1);
            appendProcessLogData(log, "Next user's public key", _userPublicKey, 16);
            appendProcessLogData(log, "New common key", commonKey.getEncoded(), 16);
            appendProcessLogData(log, "Encrypted common key", _encryptedCommonKey, 16);
            appendProcessLogData(log, "Target message", message, 16);
            appendProcessLogData(log, "Encrypted message", _encryptedMessage, 16);

            client.systemConsole.pushSubLine(log.toString());
        }

        return Util.concatByteArray(_userId, _userPublicKeySize, _userPublicKey, _encryptedCommonKeySize, _encryptedCommonKey, _encryptedMessageSize,
                _encryptedMessage);

        // data = {
        // ### userId[16], userPublicKeySize[4], userPublicKey[...],
        // ### encryptedCommonKeySize[4], encryptedCommonKey[...],
        // ### encryptedMessageSize[4], encryptedMessage[...]
        // };
    }

    int calcOuroborosDataLayerSize2A(int messageSize) {
        int userPublicKeySize = RSA_PUBLIC_KEY_SIZE;
        int encryptedCommonKeySize = AES_COMMON_KEY_SIZE_ENCRYPTED_BY_RSA_PUBLIC_KEY;
        int encryptedMessageSize = messageSize + 16; // = 16 * (messageSize / 16 + 1);

        return 16 + 4 + userPublicKeySize + 4 + encryptedCommonKeySize + 4 + encryptedMessageSize;
    }

    byte[] createOuroborosDataLayer2B(byte[] message) {
        byte[] _postUserId = Util.convertHexStringToByteArray(myself.id);
        byte[] _postUserPublicKey = myself.publicKey.getEncoded();
        byte[] _postUserPublicKeySize = Util.convertIntToByteArray(RSA_PUBLIC_KEY_SIZE);
        byte[] _map = Util.convertStringToByteArray(map.encodeMapStructure());
        byte[] _mapSize = Util.convertIntToByteArray(_map.length);
        byte[] _message = message;
        byte[] _messageSize = Util.convertIntToByteArray(_message.length);

        if (DISPLAY_ONN_PROCESS) {
            StringBuffer log = new StringBuffer("Create ONN data:");

            appendProcessLogData(log, "Layer", "2-B (Data for Recipient)", -1);
            appendProcessLogData(log, "Post user ID", _postUserId, -1);
            appendProcessLogData(log, "Post user's public key", _postUserPublicKey, 16);
            appendProcessLogData(log, "Map", _map, 16);
            appendProcessLogData(log, "Message", _message, 16);

            client.systemConsole.pushSubLine(log.toString());
        }

        return Util.concatByteArray(_postUserId, _postUserPublicKeySize, _postUserPublicKey, _mapSize, _map, _messageSize, _message);

        // data = {
        // ### postUserId[16], postUserPublicKeySize[4], postUserPublicKey[...],
        // ### mapSize[4], map[...],
        // ### messageSize[4], message[...]
        // };
    }

    int calcOuroborosDataLayerSize2B(int messageSize) {
        int userPublicKeySize = RSA_PUBLIC_KEY_SIZE;
        int mapSize = map.encodeMapStructure().length();

        return 16 + 4 + userPublicKeySize + 4 + mapSize + 4 + messageSize;
    }

    byte[] createOuroborosDataLayer1A(byte[] messageId, byte[] messageSize, byte flag, byte type, byte[] data, int noiseSize) {
        byte[] _data = data;
        byte[] _dataSize = Util.convertIntToByteArray(_data.length);
        byte[] _noise = Util.generateNoiseByte(noiseSize);
        byte[] _noiseSize = Util.convertIntToByteArray(noiseSize);

        if (DISPLAY_ONN_PROCESS) {
            StringBuffer log = new StringBuffer("Create ONN data:");

            appendProcessLogData(log, "Layer", "1-A (Single Data Message)", -1);
            appendProcessLogData(log, "Message ID", messageId, -1);
            appendProcessLogData(log, "Message size", Util.convertByteArrayToInt(messageSize));
            appendProcessLogData(log, "Flag", convertFlagByteToName(flag), -1);
            appendProcessLogData(log, "Type", type);
            appendProcessLogData(log, "Noise", _noise, 16);
            appendProcessLogData(log, "Data", _data, 16);

            client.systemConsole.pushSubLine(log.toString());
        }

        return Util.concatByteArray(messageId, messageSize, new byte[] { flag, type }, _noiseSize, _noise, _dataSize, _data);

        // message = {
        // ### messageId[16], messageSize[4], flag[1], type[1],
        // ### noiseSize[4], noise[...],
        // ### dataSize[4], data[...]
        // };
    }

    int calcOuroborosDataLayerSize1A(int dataSize, int noiseSize) {
        return 16 + 4 + 1 + 1 + 4 + noiseSize + 4 + dataSize;
    }

    byte[] createOuroborosDataLayer1B(byte[] messageId, byte[] messageSize, byte flag, byte type, byte[][] data, int noiseSize) {
        byte[] _dataArray = new byte[0];

        for (byte[] item : data)
            _dataArray = Util.concatByteArray(_dataArray, Util.convertIntToByteArray(item.length), item);

        byte[] _noise = Util.generateNoiseByte(noiseSize);
        byte[] _noiseSize = Util.convertIntToByteArray(noiseSize);

        if (DISPLAY_ONN_PROCESS) {
            StringBuffer log = new StringBuffer("Create ONN data:");

            appendProcessLogData(log, "Layer", "1-B (Multi Data Message)", -1);
            appendProcessLogData(log, "Message ID", messageId, -1);
            appendProcessLogData(log, "Message size", Util.convertByteArrayToInt(messageSize));
            appendProcessLogData(log, "Flag", convertFlagByteToName(flag), -1);
            appendProcessLogData(log, "Type", type);
            appendProcessLogData(log, "Noise", _noise, 16);

            for (int i = 0; i < data.length; i++)
                appendProcessLogData(log, "Data [" + String.valueOf(i) + "]", data[i], 16);

            client.systemConsole.pushSubLine(log.toString());
        }

        return Util.concatByteArray(messageId, messageSize, new byte[] { flag, type }, _noiseSize, _noise, _dataArray);

        // message = {
        // ### messageId[16], messageSize[4], flag[1], type[1],
        // ### noiseSize[4], noise[...],
        // ### { dataSize[4], data[...] } * [...]
        // };
    }

    int calcOuroborosDataLayerSize1B(int[] dataSize, int noiseSize) {
        int res = 16 + 4 + 1 + 1 + 4 + noiseSize;

        for (int i = 0; i < dataSize.length; i++)
            res += 4 + dataSize[i];

        return res;
    }

    byte[] createOuroborosDataLayer1C(byte[] messageId, byte[] messageSize, byte flag, byte type, int dataSize, int noiseSize) {
        byte[] _noise = Util.generateNoiseByte(dataSize + noiseSize);
        byte[] _noiseSize = Util.convertIntToByteArray(dataSize + noiseSize);

        if (DISPLAY_ONN_PROCESS) {
            StringBuffer log = new StringBuffer("Create ONN data:");

            appendProcessLogData(log, "Layer", "1-C (Null Data Message)", -1);
            appendProcessLogData(log, "Message ID", messageId, -1);
            appendProcessLogData(log, "Message size", Util.convertByteArrayToInt(messageSize));
            appendProcessLogData(log, "Flag", convertFlagByteToName(flag), -1);
            appendProcessLogData(log, "Type", type);
            appendProcessLogData(log, "Noise", _noise, 16);

            client.systemConsole.pushSubLine(log.toString());
        }

        return Util.concatByteArray(messageId, messageSize, new byte[] { flag, type }, _noiseSize, _noise);

        // message = {
        // ### messageId[16], messageSize[4], flag[1], type[1],
        // ### noiseSize[4], noise[...]
        // };
    }

    int calcOuroborosDataLayerSize1C(int dataSize, int noiseSize) {
        return 16 + 4 + 1 + 1 + 4 + dataSize + noiseSize;
    }

    public byte[] createOuroborosSendData(byte[] messageSize, byte[] nextCommonKey, byte[] nextMessage, RSAPublicKey nextPublicKey) {
        return createOuroborosSendData(client, messageSize, nextCommonKey, nextMessage, nextPublicKey);
    }

    public static byte[] createOuroborosSendData(Client client, byte[] messageSize, byte[] nextCommonKey, byte[] nextMessage, RSAPublicKey nextPublicKey) {
        int beforeMessageSize;

        beforeMessageSize = calcOuroborosSendDataSizeLayer2(nextMessage.length, 0);
        beforeMessageSize = calcOuroborosSendDataSizeLayer1(beforeMessageSize);

        int noiseSize = Util.convertByteArrayToInt(messageSize) - beforeMessageSize;

        byte[] res;

        res = createOuroborosSendDataLayer2(client, nextCommonKey, nextMessage, noiseSize);
        res = createOuroborosSendDataLayer1(client, nextPublicKey, res);

        return res;
    }

    static byte[] createOuroborosSendDataLayer2(Client client, byte[] nextCommonKey, byte[] nextMessage, int noiseSize) {
        byte[] _commonKey = nextCommonKey;
        byte[] _commonKeySize = Util.convertIntToByteArray(AES_COMMON_KEY_SIZE_ENCRYPTED_BY_RSA_PUBLIC_KEY);
        byte[] _message = nextMessage;
        byte[] _messageSize = Util.convertIntToByteArray(_message.length);
        byte[] _noise = Util.generateNoiseByte(noiseSize);
        byte[] _noiseSize = Util.convertIntToByteArray(noiseSize);

        if (DISPLAY_ONN_PROCESS) {
            StringBuffer log = new StringBuffer("Create ONN send data:");

            appendProcessLogData(log, "Layer", "2 (Content)", -1);
            appendProcessLogData(log, "Next user's common key", _commonKey, 16);
            appendProcessLogData(log, "Next user's message", _message, 16);
            appendProcessLogData(log, "Noise", _noise, 16);

            client.systemConsole.pushSubLine(log.toString());
        }

        return Util.concatByteArray(_commonKeySize, _commonKey, _messageSize, _message, _noiseSize, _noise);

        // content = {
        // ### commonKeySize[4], commonKey[...],
        // ### messageSize[4], message[...],
        // ### noiseSize[4], noise[...]
        // };
    }

    static int calcOuroborosSendDataSizeLayer2(int nextMessageSize, int noiseSize) {
        int encryptedCommonKeySize = AES_COMMON_KEY_SIZE_ENCRYPTED_BY_RSA_PUBLIC_KEY;

        return 4 + encryptedCommonKeySize + 4 + nextMessageSize + 4 + noiseSize;
    }

    static byte[] createOuroborosSendDataLayer1(Client client, User next, byte[] content) {
        return createOuroborosSendDataLayer1(client, next.publicKey, content);
    }

    static byte[] createOuroborosSendDataLayer1(Client client, byte[] publicKey, byte[] content) {
        return createOuroborosSendDataLayer1(client, Util.getRsaPublicKeyFromByteArray(publicKey), content);
    }

    static byte[] createOuroborosSendDataLayer1(Client client, RSAPublicKey publicKey, byte[] content) {
        SecretKey commonKey = Util.generateAesCommonKey();

        byte[] _encryptedCommonKey = Util.encryptCommonKeyToByteArrayWithRsaPublicKey(commonKey, publicKey);
        byte[] _encryptedCommonKeySize = Util.convertIntToByteArray(AES_COMMON_KEY_SIZE_ENCRYPTED_BY_RSA_PUBLIC_KEY);
        byte[] _encryptedContent = Util.encryptByteArrayWithAesCommonKey(content, commonKey);
        byte[] _encryptedContentSize = Util.convertIntToByteArray(_encryptedContent.length);

        if (DISPLAY_ONN_PROCESS) {
            StringBuffer log = new StringBuffer("Create ONN send data:");

            appendProcessLogData(log, "Layer", "1 (Root)", -1);
            appendProcessLogData(log, "Next user's public key", publicKey.getEncoded(), 16);
            appendProcessLogData(log, "New common key", commonKey.getEncoded(), 16);
            appendProcessLogData(log, "Encrypted common key", _encryptedCommonKey, 16);
            appendProcessLogData(log, "Target content", content, 16);
            appendProcessLogData(log, "Encrypted content", _encryptedContent, 16);

            client.systemConsole.pushSubLine(log.toString());
        }

        return Util.concatByteArray(_encryptedCommonKeySize, _encryptedCommonKey, _encryptedContentSize, _encryptedContent);

        // send = {
        // ### encryptedCommonKeySize[4], encryptedCommonKey[...],
        // ### encryptedContentSize[4], encryptedContent[...]
        // };
    }

    static int calcOuroborosSendDataSizeLayer1(int contentSize) {
        int encryptedCommonKeySize = AES_COMMON_KEY_SIZE_ENCRYPTED_BY_RSA_PUBLIC_KEY;
        int encryptedContentSize = contentSize + 16; // = 16 * (contentSize / 16 + 1);

        return 4 + encryptedCommonKeySize + 4 + encryptedContentSize;
    }

    public byte[][] decodeOuroborosData(byte[] rec, int skipLayer) {
        return decodeOuroborosData(rec, skipLayer);
    }

    public static byte[][] decodeOuroborosData(Client client, byte[] rec, int skipLayer) {
        try {
            rec = skipLayer < 1 ? decodeOuroborosDataLayer1(client, rec) : rec;

            if (rec == null) {
                client.systemConsole.pushErrorLine("Failed to decode ONN received data (Layer: 1).");

                return null;
            }

            rec = skipLayer < 2 ? decodeOuroborosDataLayer2(client, rec) : rec;

            if (rec == null) {
                client.systemConsole.pushErrorLine("Failed to decode ONN received data (Layer: 2).");

                return null;
            }

            byte[][] res = decodeOuroborosDataLayer3(client, rec);

            if (res == null) {
                client.systemConsole.pushErrorLine("Failed to decode ONN received data (Layer: 3).");

                return null;
            }

            // res[0]: messageId (16)
            // res[1]: messageSize (4)
            // res[2]: flag (1)
            // res[3]: type (1)

            int flagType = getFlagType(res[2][0]);

            if (flagType == 1 || flagType == 3) {
                byte[][] rem = new byte[ONN_LAYER_3_PROPERTY_SIZE + ONN_LAYER_4_DUM_DATA_SIZE * (res.length - ONN_LAYER_3_PROPERTY_SIZE)][];

                for (int i = 0; i < ONN_LAYER_3_PROPERTY_SIZE; i++)
                    rem[i] = res[i];

                for (int i = 0; i < res.length - ONN_LAYER_3_PROPERTY_SIZE; i++) {
                    byte[][] buf = decodeOuroborosDataLayer4A(client, res[i + ONN_LAYER_3_PROPERTY_SIZE]);

                    if (buf == null) {
                        client.systemConsole.pushErrorLine("Failed to decode ONN received data (Layer: 4).");

                        return null;
                    }

                    for (int j = 0; j < ONN_LAYER_4_DUM_DATA_SIZE; j++)
                        rem[ONN_LAYER_3_PROPERTY_SIZE + i * ONN_LAYER_4_DUM_DATA_SIZE + j] = buf[j];
                }

                res = rem;
            } else if (flagType == 2) {
                byte[][] rem = new byte[ONN_LAYER_3_PROPERTY_SIZE + ONN_LAYER_4_REC_DATA_SIZE][];

                for (int i = 0; i < ONN_LAYER_3_PROPERTY_SIZE; i++)
                    rem[i] = res[i];

                byte[][] buf = decodeOuroborosDataLayer4B(client, res[ONN_LAYER_3_PROPERTY_SIZE], res[3][0]);

                if (buf == null) {
                    client.systemConsole.pushErrorLine("Failed to decode ONN received data (Layer: 4).");

                    return null;
                }

                for (int j = 0; j < ONN_LAYER_4_REC_DATA_SIZE; j++)
                    rem[ONN_LAYER_3_PROPERTY_SIZE + j] = buf[j];

                res = rem;
            }

            return res;
        } catch (Exception e) {
            client.systemConsole.pushErrorLine(Util.setExceptionMessage(e, "An unexpected error has occurred."));

            return null;
        }
    }

    static byte[][] decodeOuroborosDataLayer4A(Client client, byte[] data) throws Exception {
        byte[] _userId = Util.getNextDataOnSize(data, 16);
        data = Util.clearByteArrayOnSize(data, 16);

        byte[] _userPublicKey = Util.getNextDataOnSize(data);
        data = Util.clearByteArrayOnSize(data);

        byte[] _encryptedCommonKey = Util.getNextDataOnSize(data);
        data = Util.clearByteArrayOnSize(data);

        byte[] _encryptedMessage = Util.getNextDataOnSize(data);
        data = Util.clearByteArrayOnSize(data);

        if (_userId == null || _userPublicKey == null || _encryptedCommonKey == null || _encryptedMessage == null)
            return null;

        if (DISPLAY_ONN_PROCESS) {
            StringBuffer log = new StringBuffer("Decode ONN received data:");

            appendProcessLogData(log, "Layer", "4 (Data)", -1);
            appendProcessLogData(log, "Send user ID", _userId, -1);
            appendProcessLogData(log, "Send user's public key", _userPublicKey, 16);
            appendProcessLogData(log, "Encrypted common key", _encryptedCommonKey, 16);
            appendProcessLogData(log, "Encrypted message", _encryptedMessage, 16);

            client.systemConsole.pushSubLine(log.toString());
        }

        return new byte[][] { _userId, _userPublicKey, _encryptedCommonKey, _encryptedMessage };
    }

    static byte[][] decodeOuroborosDataLayer4B(Client client, byte[] data, byte type) throws Exception {
        byte[] _postUserId = Util.getNextDataOnSize(data, 16);
        data = Util.clearByteArrayOnSize(data, 16);

        byte[] _postUserPublicKey = Util.getNextDataOnSize(data);
        data = Util.clearByteArrayOnSize(data);

        byte[] _map = Util.getNextDataOnSize(data);
        data = Util.clearByteArrayOnSize(data);

        byte[] _message = Util.getNextDataOnSize(data);
        data = Util.clearByteArrayOnSize(data);

        if (_map == null || _message == null)
            return null;

        if (DISPLAY_ONN_PROCESS) {
            StringBuffer log = new StringBuffer("Decode ONN received data:");

            appendProcessLogData(log, "Layer", "4 (Data)", -1);
            appendProcessLogData(log, "Posted user ID", _postUserId, -1);
            appendProcessLogData(log, "Posted user's public key", _postUserPublicKey, 16);
            appendProcessLogData(log, "Map", _map, 16);

            if (type == MESSAGE_TYPE_STRING) {
                appendProcessLogData(log, "Message", Util.convertByteArrayToString(_message), 16);
            } else {
                appendProcessLogData(log, "Message", _message, 16);
            }

            client.systemConsole.pushSubLine(log.toString());
        }

        return new byte[][] { _postUserId, _postUserPublicKey, _map, _message };
    }

    static byte[][] decodeOuroborosDataLayer3(Client client, byte[] data) throws Exception {
        byte[] _messageId = Util.getNextDataOnSize(data, 16);
        data = Util.clearByteArrayOnSize(data, 16);

        byte[] _messageSize = Util.getNextDataOnSize(data, 4);
        data = Util.clearByteArrayOnSize(data, 4);

        byte[] _flag = Util.getNextDataOnSize(data, 1);
        data = Util.clearByteArrayOnSize(data, 1);

        byte[] _type = Util.getNextDataOnSize(data, 1);
        data = Util.clearByteArrayOnSize(data, 1);

        byte[] _noise = Util.getNextDataOnSize(data);
        data = Util.clearByteArrayOnSize(data);

        if (_messageId == null || _messageSize == null || _flag == null || _type == null || _noise == null)
            return null;

        StringBuffer log = DISPLAY_ONN_PROCESS ? new StringBuffer("Decode ONN received data:") : null;

        if (DISPLAY_ONN_PROCESS) {
            appendProcessLogData(log, "Layer", "3 (Message)", -1);
            appendProcessLogData(log, "Message ID", _messageId, -1);
            appendProcessLogData(log, "Message size", Util.convertByteArrayToInt(_messageSize));
            appendProcessLogData(log, "Flag", convertFlagByteToName(_flag[0]), -1);
            appendProcessLogData(log, "Type", _type, -1);
            appendProcessLogData(log, "Noise", _noise, 16);
        }

        int flagType = getFlagType(_flag[0]);

        if (flagType == 1 || flagType == 2 || flagType == 3) {
            List<byte[]> res = new ArrayList<byte[]>();

            res.add(_messageId);
            res.add(_messageSize);
            res.add(_flag);
            res.add(_type);

            while (data != null) {
                byte[] rec = Util.getNextDataOnSize(data);

                if (rec == null)
                    break;

                res.add(rec);

                data = Util.clearByteArrayOnSize(data);

                if (data.length == 0)
                    break;
            }

            if (DISPLAY_ONN_PROCESS) {
                for (int i = 1; i < res.size(); i++)
                    appendProcessLogData(log, "Data [" + String.valueOf(i - 1) + "]", res.get(i), 16);

                client.systemConsole.pushSubLine(log.toString());
            }

            return res.toArray(new byte[res.size()][]);
        } else if (flagType == 0 || flagType == 4) {
            return new byte[][] { _messageId, _messageSize, _flag, _type };
        } else {
            return null;
        }
    }

    static byte[] decodeOuroborosDataLayer2(Client client, byte[] data) throws Exception {
        byte[] _encryptedCommonKey = Util.getNextDataOnSize(data);
        data = Util.clearByteArrayOnSize(data);

        byte[] _encryptedMessage = Util.getNextDataOnSize(data);
        data = Util.clearByteArrayOnSize(data);

        byte[] _noise = Util.getNextDataOnSize(data);
        data = Util.clearByteArrayOnSize(data);

        if (data.length != 0 || _encryptedCommonKey == null || _encryptedMessage == null || _noise == null)
            return null;

        SecretKey commonKey = Util.decryptByteArrayToCommonKeyWithRsaPrivateKey(_encryptedCommonKey, client.userStack.myProfile.privateKey);
        byte[] message = Util.decryptByteArrayWithAesCommonKey(_encryptedMessage, commonKey);

        if (commonKey == null || message == null)
            return null;

        if (DISPLAY_ONN_PROCESS) {
            StringBuffer log = new StringBuffer("Decode ONN received data:");

            appendProcessLogData(log, "Layer", "2 (Content)", -1);
            appendProcessLogData(log, "Encrypted common key", _encryptedCommonKey, 16);
            appendProcessLogData(log, "Encrypted message", _encryptedMessage, 16);
            appendProcessLogData(log, "Decrypted common key", commonKey.getEncoded(), 16);
            appendProcessLogData(log, "Decrypted message", message, 16);
            appendProcessLogData(log, "Noise", _noise, 16);

            client.systemConsole.pushSubLine(log.toString());
        }

        return message;
    }

    static byte[] decodeOuroborosDataLayer1(Client client, byte[] data) throws Exception {
        byte[] _encryptedCommonKey = Util.getNextDataOnSize(data);
        data = Util.clearByteArrayOnSize(data);

        byte[] _encryptedContent = Util.getNextDataOnSize(data);
        data = Util.clearByteArrayOnSize(data);

        if (data.length != 0 || _encryptedCommonKey == null || _encryptedContent == null)
            return null;

        SecretKey commonKey = Util.decryptByteArrayToCommonKeyWithRsaPrivateKey(_encryptedCommonKey, client.userStack.myProfile.privateKey);
        byte[] content = Util.decryptByteArrayWithAesCommonKey(_encryptedContent, commonKey);

        if (commonKey == null || content == null)
            return null;

        if (DISPLAY_ONN_PROCESS) {
            StringBuffer log = new StringBuffer("Decode ONN received data:");

            appendProcessLogData(log, "Layer", "1 (Receive)", -1);
            appendProcessLogData(log, "Encrypted common key", _encryptedCommonKey, 16);
            appendProcessLogData(log, "Encrypted content", _encryptedContent, 16);
            appendProcessLogData(log, "Decrypted common key", commonKey.getEncoded(), 16);
            appendProcessLogData(log, "Decrypted content", content, 16);

            client.systemConsole.pushSubLine(log.toString());
        }

        return content;
    }

    static void appendProcessLogData(StringBuffer sb, String name, int data) {
        sb.append("\n- " + name + ": " + String.valueOf(data));
    }

    static void appendProcessLogData(StringBuffer sb, String name, String data, int displayDataLength) {
        if (!DISPLAY_ONN_PROCESS_FULL_CONTENT && displayDataLength != -1) {
            sb.append("\n- " + name + ": " + Util.omitString(data, displayDataLength, true));
        } else {
            sb.append("\n- " + name + ": " + data);
        }
    }

    static void appendProcessLogData(StringBuffer sb, String name, byte data) {
        sb.append("\n- " + name + ": " + String.format("%02x", data));
    }

    static void appendProcessLogData(StringBuffer sb, String name, byte[] data, int displayCharLength) {
        if (!DISPLAY_ONN_PROCESS_FULL_CONTENT && displayCharLength != -1) {
            sb.append("\n- " + name + ": " + Util.omitByteArrayToHexString(data, displayCharLength, true));
        } else {
            sb.append("\n- " + name + ": " + Util.convertByteArrayToHexString(data));
        }
    }

    public static int getFlagType(String flag) {
        return getFlagType(convertFlagNameToByte(flag));
    }

    public static int getFlagType(byte flag) {
        switch (flag) {
        case FLAG_BYTE_SYNCHRONIZE:
        case FLAG_BYTE_ACKNOWLEDGE:
            return 0;
        case FLAG_BYTE_POST:
        case FLAG_BYTE_FINISH:
            return 1;
        case FLAG_BYTE_RECEIVE:
            return 2;
        case FLAG_BYTE_DUMMY:
        case FLAG_BYTE_WAIT:
        case FLAG_BYTE_REPEAT:
            return 3;
        case FLAG_BYTE_DELETE:
        case FLAG_BYTE_NULL:
            return 4;
        default:
            return -1;
        }
    }

    public static byte convertFlagNameToByte(String flag) {
        switch (flag) {
        case FLAG_NAME_SYNCHRONIZE:
            return FLAG_BYTE_SYNCHRONIZE;
        case FLAG_NAME_ACKNOWLEDGE:
            return FLAG_BYTE_ACKNOWLEDGE;
        case FLAG_NAME_DUMMY:
            return FLAG_BYTE_DUMMY;
        case FLAG_NAME_POST:
            return FLAG_BYTE_POST;
        case FLAG_NAME_RECEIVE:
            return FLAG_BYTE_RECEIVE;
        case FLAG_NAME_FINISH:
            return FLAG_BYTE_FINISH;
        case FLAG_NAME_DELETE:
            return FLAG_BYTE_DELETE;
        case FLAG_NAME_WAIT:
            return FLAG_BYTE_WAIT;
        case FLAG_NAME_REPEAT:
            return FLAG_BYTE_REPEAT;
        default:
            return FLAG_BYTE_NULL;
        }
    }

    public static String convertFlagByteToName(byte flag) {
        switch (flag) {
        case FLAG_BYTE_SYNCHRONIZE:
            return FLAG_NAME_SYNCHRONIZE;
        case FLAG_BYTE_ACKNOWLEDGE:
            return FLAG_NAME_ACKNOWLEDGE;
        case FLAG_BYTE_DUMMY:
            return FLAG_NAME_DUMMY;
        case FLAG_BYTE_POST:
            return FLAG_NAME_POST;
        case FLAG_BYTE_RECEIVE:
            return FLAG_NAME_RECEIVE;
        case FLAG_BYTE_FINISH:
            return FLAG_NAME_FINISH;
        case FLAG_BYTE_DELETE:
            return FLAG_NAME_DELETE;
        case FLAG_BYTE_WAIT:
            return FLAG_NAME_WAIT;
        case FLAG_BYTE_REPEAT:
            return FLAG_NAME_REPEAT;
        default:
            return FLAG_NAME_NULL;
        }
    }
}
