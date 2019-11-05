# NSF File Server

This project is a WAR web application that launches an SFTP server backed by a file storage NSF, storing the files and directories as user-side documents. The server uses the running Domino environment for authentication.

### Running

The generated application requires a Jakarta EE server with an active Notes runtime. The most straightforward way to create this is to use the [Domino Open Liberty Runtime](https://github.com/OpenNTF/openliberty-domino) project.

The WAR has been developed and tested on Open Liberty 19.0.0.10, but should work on any JEE server that provides Servlet 3+, CDI 2, and MicroProfile Config 1.3.

### Configuration

The server supports two configuration properties loaded via [MicroProfile Config](https://github.com/eclipse/microprofile-config):

* `SFTPNSFPath`: the path to the NSF to use as a file store. Defaults to `filestore.nsf` and can be either a base database path or an API path in "server!!path.nsf" format
* `SFTPNSFPort`: the port used by the SFTP server. Defaults to `9022`

## Building

Compilation requires the availability of Mavenized Domino artifacts as done via [`generate-domino-update-site`](https://github.com/OpenNTF/generate-domino-update-site/) with the group ID `com.ibm.xsp`.

## License

The code in the project is licensed under the Apache License 2.0. The dependencies in the binary distribution are licensed under compatible licenses - see NOTICE for details.