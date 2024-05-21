# Easy Pear to Pear Chat System (E=CS)

![](./Others/Screenshot.png)

## What is this?

This is a simple chat system that I created to personally study pear to pear (P2P) and security. It does not require a centralized server, and each nodes to easily form a network.

## How to use?

### Boots

You can execute the application (JAR) file by either double-clicking on it or running it from the command line. For example, like this:

```sh:Bash
$ java -jar E=CS.jar
```

You can customize the launch of the application by adding options to the command line. For example, like this:

```sh:Bash
$ java -jar E=CS.jar -x=0 -y=0 -n="Nekoformi" -join=0.0.0.0:20000,20001 -ssl -debug
```

#### General options

| Option | Type | Effect |
| --- | --- | --- |
| `x` `left` | `<INTEGER>` | Set the X position of the window. |
| `y` `top` | `<INTEGER>` | Set the Y position of the window. |
| `w` `width` | `<UNSIGNED INTEGER>` | Set the width of the window. |
| `h` `height` | `<UNSIGNED INTEGER>` | Set the height of the window. |
| `c` `center` | `<VOID>` | Center the window. |
| `m` `maximize` | `<VOID>` | Maximize the window. |
| `n` `name` | `<STRING>` | Set the username. It can also be changed within the application. |
| `t` `timeout` | `<UNSIGNED INTEGER>` | Set the timeout (in milliseconds). |
| `create` | `<LISTENING PORT>` | Create a network at startup. |
| `join` | `<ADDRESS>:<PORT>,<LISTENING PORT>` | Join the network at startup. |
| `ssl` | `<VOID>` | Use SSL (Secure Socket Layer). However, note that the standard state has nonsense on users who know the protocol. |
| `debug` | `<VOID>` | Outputs log to the console. |

#### Options enabled in SSL mode

| Option | Type | Effect |
| --- | --- | --- |
| `pkc-file` | `<P12 FILE>` | Specifies the PKCS (Public Key Cryptography Standard) file. This is specified as the certificate for both client and server, as it is used for communication between nodes. |
| `pkc-pass` | `<STRING>` | Specifies the passphrase for processing the `pkc-file` if required. |
| `jks-file` | `<JKS FILE>` | Specifies the JKS (Java Key Store) file. This can be created using [keytool](https://docs.oracle.com/javase/10/tools/keytool.htm) with the certificate (CRT file) of the CA that authenticates the client. |
| `jks-pass` | `<STRING>` | Specifies the passphrase for processing the `jks-file` if required. |
| `pkc-server-file` | `<P12 FILE>` | Specifies the PKCS file for the server. |
| `pkc-server-pass` | `<STRING>` | Specifies the passphrase for processing the `pkc-server-file` if required. |
| `pkc-client-file` | `<P12 FILE>` | Specifies the PKCS file for the client. |
| `pkc-client-pass` | `<STRING>` | Specifies the passphrase for processing the `pkc-client-file` if required. |

### Operations

You can post messages or execute commands from the text field at the bottom of the window. For example, like this:

```
/n Nekoformi
/j 0.0.0.0:20000 20001
Hello world!
```

| Command | Argument | Detail |
| --- | --- | --- |
| `c` `create` | `<LISTENING PORT>` | Create a network. |
| `j` `join` | `<ADDRESS>:<PORT>` `<LISTENING PORT>` | Join the network. |
| `l` `leave` | `<VOID>` | Leave the network. |
| `n` `name` | `<STRING ...>` | Set the username. |
| `m` `message` | `<STRING ...>` | Send chat message. |
| `u` `update` | `<VOID>` | Update user list. |
| `cls` `clear` | `<VOID>` | Clear chat history. |
| `connect` | `<USER ID>` | Connect the node. |
| `disconnect` | `<USER ID>` | Disconnect the node. |

## Bonus

### How to create PKCS

```sh:Bash
openssl pkcs12 -export \
    -in "<YOUR CERTIFICATE FILE>.crt" \
    -inkey "<YOUR PRIVATE KEY>.key" \
    -certfile "<CA CERTIFICATE FILE>.crt" \
    -passout "<PASS PHRASE>" \
    -out "<EXPORT FILE>.p12"
```

### How to create JKS

```sh:Bash
keytool -import \
    -file "<CA CERTIFICATE FILE>.crt" \
    -storepass "<PASS PHRASE>" \
    -keystore "<EXPORT FILE>.jks"
```

## Used libraries

- [FlatLaf](https://www.formdev.com/flatlaf/)

## Bucket list

- Add function: to send private message.
- Add function: to send binary data.
- Add function: to enhance network security with password.
- Add function: to add clients to blacklist.
