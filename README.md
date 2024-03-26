# NSF File Server

This project is a Domino HTTP OSGi application that that launches an SFTP server backed by one or more NSFs, storing the files and directories as user-side documents.

### Configuration

The server is configured by creating a database using the included "fileserverconfig.ntf" template and placing it in the root of the server's data directory as "fileserverconfig.nsf". The path to the config NSF can be overridden using the `SFTPConfigPath` notes.ini parameter.

Inside the config NSF, you can specify per-server configuration for whether to enable the service and which port to use, as well as the sub directory "mounts" that will appear as the root of the SFTP server. There are currently two types supported:

- The NSF document layout, which is a database created using the included "filestore.ntf" template and which stores files as individual documents with the file data as an attachment. It presents a POSIX-like view of the NSF documents as a filesystem
- Generic URIs, which will be passed to the Java NIO `Paths.get(uri)` method to be handled if an applicable provider exists. Note that this support is incomplete

### Authentication

The spawned SSH server uses the current server's Domino directories for authentication. It supports two methods:

- Password authentication using the "HTTPPassword" item in the user's person document
- RSA public key authentication using a public key stored in the "sshPublicKey" item in the user's person document. This should be a text item containing the same contents as a "id_rsa.pub" file from OpenSSH, and it can be multi-value via line breaks

## Building

Compilation requires the availability of Mavenized Domino artifacts as done via [`generate-domino-update-site`](https://github.com/OpenNTF/generate-domino-update-site/) with the group ID `com.ibm.xsp`.

### Extending

New filesystem mount types can be introduced by registering an instance of `org.openntf.nsffile.ssh.spi.FileSystemMountProvider` via the IBM Commons ExtensionManager system (i.e. plugin.xml entries on Domino). These implementations provide a name that can be specified in the NSF as well as a data source whose meaning depends on the provider. The incoming `env` object will be pre-populated with a "username" key mapped to the authenticated username as a string.

## License

The code in the project is licensed under the Apache License 2.0. The dependencies in the binary distribution are licensed under compatible licenses - see NOTICE for details.