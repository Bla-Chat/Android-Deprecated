BlaChat
=======

A secure and open source android chat application, to give you insight what happens to your data.

And all implemented in only about 3000 to 4000 lines of code.

It works with any server, that implements the protocol it uses.
Currently the only software implementing this protocol is BlaChatServer (https://github.com/penguinmenac3/BlaChat-Server).

Protocol
========

Bla chat is based on XJCP (Extensible JSON Chat Protocol).

Features
========

Bla chat supports multiple conversations with any amount of users.

It supports:

*Plain text mssages

*Photos

*Images from galery

*Notifications (Synched across devices)

*Groups (with unlimited users)

*Auto-updates

*Encrypted connection

*Any server can be used (username@server for login)


Related work and projects
=========================

In early 2014 the underlying protocol was put into an extra project to ensure it is not developed for easy and hacky implementation but for performance and security. https://www.ssl-id.de/bla.f-online.net/api/XJCP-Spec.pdf

To make developing clients easier there is now a java client side protocol implementation. https://github.com/penguinmenac3/XJCP-Interface

There also exists an open source server implementation.
https://github.com/penguinmenac3/BlaChat-Server

The whole BlaChat-Project is more than 10k lines of code.
