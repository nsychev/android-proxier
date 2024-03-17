# Proxier

Simple app to launch SOCKS5 proxy on Android and expose it via SSH tunnel.

## Why?

Proxier makes it easier to help someone with network setup without a physical presence.

## How to make it work?

1. You need some remote-accessible host (e.g. VPS) running Linux and openssh (other systems may be OK, but not tested).
2. Create a new user, e.g. `adduser proxier`.
3. Switch to that user (`sudo -u proxier bash`) and switch to its home directory (`cd ~`).
4. Generate SSH key pair: `ssh-keygen -t rsa -m pem`.
5. Add **public** key to `~/.ssh/authorized_keys`.
6. Add the following to `/etc/ssh/sshd_config` and restart the SSH server:
   ```
   Match User <username>
       PermitOpen 127.0.0.1:<port>
       X11Forwarding no
       AllowAgentForwarding no
       ForceCommand /bin/false
       PermitTTY no
       Banner none
   ```

   Don't forget to replace `<username>` and `<port>` with the actual username and chosen port on a remote server.
7. Put private key to `app/src/main/res/raw/private_key.txt` and public key to `app/src/main/res/raw/public_key.txt`.
8. Edit settings in `app/src/main/java/ru/nsychev/proxies/Constants.kt`:
   - Set `SSH_HOST`, `SSH_PORT`, `SSH_USER` to your remote host values
   - Set `SSH_REMOTE_PORT` to the same value as in step 6
9. Build the application (e.g. in Android Studio) and install it.
10. Click “Start” to start proxying.
11. Use `socks5://127.0.0.1:<port>` on your remote server to access the phone's local network.

**WARNING!** UNLESS YOU KNOW WHAT YOU ARE DOING AND WHAT THIS APP DOES:
 - DO NOT RUN IT ON YOUR DEVICE
 - DO NOT DISTRIBUTE THE APP TO THE PEOPLE YOU DON'T TRUST

## License

[The MIT License](LICENSE) applies to the whole repository EXCEPT its logotype, namely resource `proxier.png`.

All rights to the logotype are reserved to Yandex LLC, a legal entity in Russia, and its usage is limited by [the Terms of Service “Shedevrum” (in Russian)](https://yandex.ru/legal/shedevrum_termsofuse/) to the personal non-commercial usage.

