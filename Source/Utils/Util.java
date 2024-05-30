package Source.Utils;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.net.ssl.*;

public class Util {
    public static final String MAGIC_WORD = "Every way holds mysteries to be explored! ... by Sara Kotova";

    public static final Charset CHARSET = StandardCharsets.UTF_8;

    public static final int INDENT_SPACE = 4;

    public static final String TERMINAL_GREEN = "\u001b[00;32m";
    public static final String TERMINAL_YELLOW = "\u001b[00;33m";
    public static final String TERMINAL_RED = "\u001b[00;31m";
    public static final String TERMINAL_GRAY = "\u001b[00;37m";
    public static final String TERMINAL_END = "\u001b[00;00m";

    public static final String UNSIGNED_INTEGER_REGEX = "[0-9]+";
    public static final String INTEGER_REGEX = "[\\+-]?" + UNSIGNED_INTEGER_REGEX;
    public static final String UNSIGNED_NUMBER_REGEX = "([0-9]+|[0-9]+\\.[0-9]+)";
    public static final String NUMBER_REGEX = "[\\+-]?" + UNSIGNED_NUMBER_REGEX;
    public static final String IP_ADDRESS_RAW_REGEX = "(0*(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}0*(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])";
    public static final String IP_ADDRESS_DOMAIN_REGEX = "([a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]*\\.)+[a-zA-Z]{2,}";
    public static final String IP_ADDRESS_REGEX = "(" + IP_ADDRESS_RAW_REGEX + "|" + IP_ADDRESS_DOMAIN_REGEX + ")";
    public static final String IP_PORT_REGEX = "0*[0-9]{1,5}";
    public static final String IP_ADDRESS_PORT_REGEX = IP_ADDRESS_REGEX + ":" + IP_PORT_REGEX;
    public static final String COMMAND_REGEX = "/[a-z-]+";
    public static final String TASK_ID_REGEX = "#[0-9a-f]{32}";
    public static final String USER_ID_REGEX = "@[0-9a-f]{32}";

    public static final int TYPE_STRING = 1;
    public static final int TYPE_INTEGER = 2;
    public static final int TYPE_UNSIGNED_INTEGER = 4;
    public static final int TYPE_NUMBER = 8;
    public static final int TYPE_UNSIGNED_NUMBER = 16;
    public static final int TYPE_IP_ADDRESS = 32;
    public static final int TYPE_IP_ADDRESS_PORT = 64;
    public static final int TYPE_IP_PORT = 128;
    public static final int TYPE_COMMAND = 256;
    public static final int TYPE_TASK_ID = 512;
    public static final int TYPE_USER_ID = 1024;

    // Message

    public static String getStackTrace(Exception e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        e.printStackTrace(printWriter);

        printWriter.flush();

        return stringWriter.toString().trim().replaceAll("\t", indent(1));
    }

    public static String setExceptionMessage(Exception e, String text) {
        return text + "\n" + getStackTrace(e);
    }

    public static String getSocketInfoString(Socket socket) {
        return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    public static String getSocketInfoString(ServerSocket serverSocket) {
        return serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort();
    }

    public static String getCurrentTimeDisplay() {
        return getCurrentTimeDisplay("yyyy/MM/dd HH:mm:ss.SSSS");
    }

    public static String getCurrentTimeDisplay(String format) {
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);

        return dtf.format(date);
    }

    // Generate

    public static String generateNoiseHexString(int length) {
        return convertByteArrayToHexString(generateNoiseByte(length));
    }

    public static byte[] generateNoiseByte(int length) {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[length];

        random.nextBytes(bytes);

        return bytes;
    }

    public static String[] copyStringArray(String[] rec, int startIndex, int endIndex) {
        if (startIndex < 0)
            startIndex = rec.length + startIndex;

        if (endIndex < 0)
            endIndex = rec.length;

        if (startIndex < 0 || startIndex >= endIndex || endIndex > rec.length)
            return null;

        String[] res = new String[endIndex - startIndex];

        for (int i = 0; i < res.length; i++)
            res[i] = rec[i + startIndex];

        return res;
    }

    public static String indent(int n) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < INDENT_SPACE * n; i++)
            sb.append(" ");

        return sb.toString();
    }

    // Edit

    public static String omitString(String text, int byteLength, boolean displayDataLength) {
        int textLength = text.getBytes(CHARSET).length;
        int textLengthDigit = getNumberDigit(textLength);

        if (text != null && textLength > byteLength) {
            StringBuffer sb = new StringBuffer();

            for (int i = 0; i < text.length(); i++) {
                sb.append(text.charAt(i));

                if (sb.toString().getBytes(CHARSET).length > byteLength - (displayDataLength ? textLengthDigit + 5 : 3)) {
                    sb.deleteCharAt(sb.length() - 1);

                    break;
                }
            }

            return sb.toString() + "..." + (displayDataLength ? "(" + String.valueOf(textLength) + ")" : "");
        } else {
            return text;
        }
    }

    public static String[] specialSplitString(String data, String split, String boxStart, String boxEnd) {
        if (boxStart.length() != boxEnd.length() || data == null)
            return null;

        String[] buf = data.split(split);
        StringBuffer rec = new StringBuffer();
        List<String> res = new ArrayList<String>();

        int boxIndex = -1;

        for (int i = 0; i < buf.length; i++) {
            if (boxIndex == -1) {
                rec.append(buf[i]);

                for (int j = 0; j < boxStart.length(); j++) {
                    if (buf[i].startsWith(String.valueOf(boxStart.charAt(j)))) {
                        boxIndex = j;

                        rec.delete(0, 1);

                        break;
                    }
                }
            } else {
                rec.append(split + buf[i]);

                if (buf[i].endsWith(String.valueOf(boxEnd.charAt(boxIndex)))) {
                    boxIndex = -1;

                    rec.setLength(rec.length() - 1);
                }
            }

            if (boxIndex == -1 || i == buf.length - 1) {
                res.add(rec.toString());

                rec.setLength(0);
            }
        }

        return res.toArray(new String[res.size()]);
    }

    // Convert

    public static int getNumberDigit(int number) {
        return getNumberDigit((double)number);
    }

    public static int getNumberDigit(double number) {
        if (number != 0) {
            return (int)Math.floor(Math.log10(Math.abs(number))) + 1;
        } else {
            return 1;
        }
    }

    public static int getMaximum(int a, int b) {
        return a >= b ? a : b;
    }

    public static int getMinimum(int a, int b) {
        return a <= b ? a : b;
    }

    public static String convertByteArrayToHexString(byte[] data) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < data.length; i++)
            sb.append(String.format("%02x", data[i]));

        return sb.toString();
    }

    public static String convertByteArrayToBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] convertBase64ToByteArray(String data) {
        return Base64.getDecoder().decode(data);
    }

    public static String convertStringToBase64(String data) {
        return Base64.getEncoder().encodeToString(data.getBytes(CHARSET));
    }

    public static String convertBase64ToString(String data) {
        return new String(Base64.getDecoder().decode(data), CHARSET);
    }

    // Check

    public static String checkStringType(String data, int type) {
        switch (type) {
        case TYPE_STRING:
            return null;
        case TYPE_INTEGER:
            if (data.matches(INTEGER_REGEX)) {
                return null;
            } else {
                return "not integer";
            }
        case TYPE_UNSIGNED_INTEGER:
            if (data.matches(UNSIGNED_INTEGER_REGEX)) {
                return null;
            } else {
                return "not unsigned integer";
            }
        case TYPE_NUMBER:
            if (data.matches(NUMBER_REGEX)) {
                return null;
            } else {
                return "not number";
            }
        case TYPE_UNSIGNED_NUMBER:
            if (data.matches(UNSIGNED_NUMBER_REGEX)) {
                return null;
            } else {
                return "not unsigned number";
            }
        case TYPE_IP_ADDRESS:
            if (data.matches(IP_ADDRESS_REGEX)) {
                return null;
            } else {
                return "not network address";
            }
        case TYPE_IP_ADDRESS_PORT:
            if (data.matches(IP_ADDRESS_PORT_REGEX)) {
                return null;
            } else {
                return "not network address + port number";
            }
        case TYPE_IP_PORT:
            if (data.matches(IP_PORT_REGEX)) {
                return null;
            } else {
                return "not network port number";
            }
        case TYPE_COMMAND:
            if (data.matches(COMMAND_REGEX)) {
                return null;
            } else {
                return "not command";
            }
        case TYPE_TASK_ID:
            if (data.matches(TASK_ID_REGEX)) {
                return null;
            } else {
                return "not task ID";
            }
        case TYPE_USER_ID:
            if (data.matches(USER_ID_REGEX)) {
                return null;
            } else {
                return "not user ID";
            }
        default:
            return "unknown type";
        }
    }

    // Security

    public static SecretKey generateAesCommonKey() {
        return new SecretKeySpec(generateNoiseByte(32), "AES");
    }

    public static SecretKey generateAesCommonKeyFromPassword(String password) {
        try {
            return new SecretKeySpec(getSha256(password), "AES");
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static GCMParameterSpec generateGcmParameter() {
        try {
            byte[] fix = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, };

            return new GCMParameterSpec(128, fix);
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static GCMParameterSpec generateGcmParameter(byte[] noise) {
        try {
            return new GCMParameterSpec(128, noise);
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static byte[] getSha256(String data) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");

            sha.update(data.getBytes(CHARSET));

            return sha.digest();
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static SecretKey getAesCommonKeyFromByteArray(byte[] data) {
        try {
            return new SecretKeySpec(data, "AES");
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static RSAPublicKey getRsaPublicKeyFromByteArray(byte[] data) {
        try {
            return (RSAPublicKey)KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(data));
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static RSAPrivateKey getRsaPrivateKeyFromByteArray(byte[] data) {
        try {
            return (RSAPrivateKey)KeyFactory.getInstance("RSA").generatePrivate(new X509EncodedKeySpec(data));
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static KeyManagerFactory setKeyManagerFactory(String filePath, String passPhrase) throws Exception {
        return setKeyManagerFactory(new FileInputStream(filePath), passPhrase);
    }

    public static TrustManagerFactory setTrustManagerFactory(String filePath, String passPhrase) throws Exception {
        return setTrustManagerFactory(new FileInputStream(filePath), passPhrase);
    }

    public static KeyManagerFactory setKeyManagerFactory(InputStream inputStream, String passPhrase) throws Exception {
        if (inputStream == null)
            return null; // Need certificate file path.

        if (passPhrase == null)
            passPhrase = "";

        // FileInputStream p12 = new FileInputStream(filePath);
        KeyStore ks = KeyStore.getInstance("pkcs12");

        ks.load(inputStream, passPhrase.toCharArray());

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");

        keyManagerFactory.init(ks, passPhrase.toCharArray());

        return keyManagerFactory;
    }

    public static TrustManagerFactory setTrustManagerFactory(InputStream inputStream, String passPhrase) throws Exception {
        if (inputStream == null)
            return null; // Need key store file path.

        if (passPhrase == null)
            passPhrase = "";

        // FileInputStream jks = new FileInputStream(filePath);
        KeyStore ks = KeyStore.getInstance("JKS");

        ks.load(inputStream, passPhrase.toCharArray());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");

        trustManagerFactory.init(ks);

        return trustManagerFactory;
    }

    public static String encryptStringWithAesCommonKey(String data, SecretKey commonKey) {
        // Use a single-use common key!

        return encryptStringWithAesCommonKey(data, commonKey, generateGcmParameter());
    }

    public static String encryptStringWithAesCommonKey(String data, SecretKey commonKey, GCMParameterSpec gcmParameter) {
        byte[] aad = MAGIC_WORD.getBytes(CHARSET);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            cipher.init(Cipher.ENCRYPT_MODE, commonKey, gcmParameter);
            cipher.updateAAD(aad);

            return convertByteArrayToBase64(cipher.doFinal(data.getBytes(CHARSET)));
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static String decryptStringWithAesCommonKey(String data, SecretKey commonKey) {
        // Use a single-use common key!

        return decryptStringWithAesCommonKey(data, commonKey, generateGcmParameter());
    }

    public static String decryptStringWithAesCommonKey(String data, SecretKey commonKey, GCMParameterSpec gcmParameter) {
        byte[] aad = MAGIC_WORD.getBytes(CHARSET);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            cipher.init(Cipher.DECRYPT_MODE, commonKey, gcmParameter);
            cipher.updateAAD(aad);

            return new String(cipher.doFinal(convertBase64ToByteArray(data)), CHARSET);
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static byte[] encryptByteArrayWithRsaPublicKey(byte[] data, RSAPublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static byte[] encryptByteArrayWithRsaPrivateKey(byte[] data, RSAPrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

            cipher.init(Cipher.ENCRYPT_MODE, privateKey);

            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static byte[] decryptByteArrayWithRsaPublicKey(byte[] data, RSAPublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

            cipher.init(Cipher.DECRYPT_MODE, publicKey);

            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static byte[] decryptByteArrayWithRsaPrivateKey(byte[] data, RSAPrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static String encryptCommonKeyWithRsaPublicKey(SecretKey commonKey, RSAPublicKey publicKey) {
        return convertByteArrayToBase64(encryptByteArrayWithRsaPublicKey(commonKey.getEncoded(), publicKey));
    }

    public static String encryptCommonKeyWithRsaPrivateKey(SecretKey commonKey, RSAPrivateKey privateKey) {
        return convertByteArrayToBase64(encryptByteArrayWithRsaPrivateKey(commonKey.getEncoded(), privateKey));
    }

    public static SecretKey decryptCommonKeyWithRsaPublicKey(String data, RSAPublicKey publicKey) {
        return getAesCommonKeyFromByteArray(decryptByteArrayWithRsaPublicKey(convertBase64ToByteArray(data), publicKey));
    }

    public static SecretKey decryptCommonKeyWithRsaPrivateKey(String data, RSAPrivateKey privateKey) {
        return getAesCommonKeyFromByteArray(decryptByteArrayWithRsaPrivateKey(convertBase64ToByteArray(data), privateKey));
    }
}
