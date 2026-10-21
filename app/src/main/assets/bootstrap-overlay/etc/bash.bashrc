# System-wide .bashrc file for interactive bash(1) shells in Termux Pro.
# Configured for prefix: /data/data/com.termuxpro.app/files/usr

if [ "$TERM" = "dumb" ] || [ -n "$NO_COLOR" ]; then
    PS1='\w \$ '
else
    PS1='\[\033[01;36m\]\w\[\033[00m\] \$ '
fi

# Source profile.d directory scripts if present
if [ -d "/data/data/com.termuxpro.app/files/usr/etc/profile.d" ]; then
    for f in /data/data/com.termuxpro.app/files/usr/etc/profile.d/*.sh; do
        [ -r "$f" ] && . "$f"
    done
    unset f
fi
