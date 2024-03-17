package ru.nsychev.proxies

import java.net.InetAddress

const val INTENT_ACTION = "ru.nsychev.proxies.PROXY_STATE"

const val SSH_HOST = "..."
const val SSH_PORT = 22
const val SSH_USER = "..."
const val SSH_REMOTE_HOST = "127.0.0.1"
const val SSH_REMOTE_PORT = 1337

val LOCAL_HOST = InetAddress.getLocalHost()
const val LOCAL_PORT = 1337
