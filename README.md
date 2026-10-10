# Lexmechanic

<p align="center">
  <img src="art/lexmechanic1.jpg" alt="Lexmechanic">
</p>

## Lore

<table>
<tr>
<td align="center" valign="middle" width="280">

<img src="art/emblem.svg" alt="Lexmechanic emblem" width="256">

</td>
<td valign="top">

> A Lexmechanic of the Adeptus Mechanicus, also known as a Calculus Logi, is a specialised Tech-Priest of the Adeptus Mechanicus whose primary duty is the collection, processing, analysis and organisation of information. They are the data specialists, computational analysts and technical archivists of the Imperium of Man.
>
> In the grim darkness of the 41st Millennium, information is power, knowledge is sacred, and an error in a calculation can condemn entire armies or planetary populations to death. The Lexmechanic's task is to ensure that the Imperium's vast machinery of war, industry and government continues to function through accurate information.

</td>
</tr>
</table>

### What does a Lexmechanic actually do?

#### Data processing and translation

A Lexmechanic gathers raw information from cogitators, machine records, reports and other sources, then organises and interprets it into a useful form. This could involve turning thousands of military reports into a coherent operational summary or converting complex technical records into actionable instructions.

#### Archiving sacred knowledge

Lexmechanics compile and rationalise information for storage in central cogitator repositories. They help maintain the enormous administrative and technical knowledge systems upon which the Imperium depends. Think of them as a combination of data analyst, archivist and highly specialised machine-assisted clerk.

#### Statistical and military analysis

They process casualty reports, battlefield statistics, planetary assessments, industrial production figures and economic data. Their mathematical abilities can also be applied in the field, helping commanders interpret complex situations and calculate possible outcomes.

#### Augmented cognition

Lexmechanics are known for processing information with computer-like speed and accuracy. Some undergo the Rite of Clear Thought, a neurological procedure described in the lore that enhances concentration and analytical ability while suppressing emotional distractions. This is not a universal requirement for every Lexmechanic.


## Usage

Lexmechanic is a small daemon. When you press the hotkey (a mouse side button by default), it takes a screenshot, crops it to the dialogue window, has an AI model translate the dialogue into your language, and shows the result in a local web page, which it opens on start.

### Building and running on Linux

Build inside a [distrobox](https://distrobox.it) container: it works the same on any distro, including immutable ones
like SteamOS, and keeps the build toolchain off the host.

```bash
# On the host. Needs distrobox with podman or docker.
# The container shares your home directory, so whatever it builds there is usable on the host.
distrobox create --name lexbox --image docker.io/library/archlinux:latest

# Install dependencies
distrobox enter lexbox -- sudo pacman -Syu --needed --noconfirm clang which curl jdk-openjdk sbt git

# Clone the Lexmechanic source code
git clone https://github.com/stivens/Lexmechanic.git ~/Lexmechanic
cd ~/Lexmechanic

# Build inside the container; the binary lands in the shared home directory
distrobox enter lexbox -- sbt release
install -Dm755 target/out/native0.5/scala-3.9.0/lingua-servitor/lexmechanic ~/.local/bin/lexmechanic

# Lets the daemon read the hotkey from /dev/input; takes effect after you log in again.
# On a Steam Deck, set a password with `passwd` first.
sudo usermod -aG input "$USER"

# Run it
~/.local/bin/lexmechanic
```

To update, run `git pull` and repeat the `sbt release` and `install` lines.

The binary links against the container's glibc. If the host's is older, it fails with `GLIBC_2.xx not found`; create
the box from an image of your host distro instead and install the same packages with its package manager.

### Desktop shortcut

A `.desktop` launcher puts Lexmechanic in the app menu, and optionally on the desktop, with the emblem as its icon.
Run this on the host after the build above:

```bash
install -Dm644 ~/Lexmechanic/art/emblem.svg ~/.local/share/icons/lexmechanic.svg
mkdir -p ~/.cache/lexmechanic ~/.local/share/applications

# EOF is unquoted on purpose: .desktop files don't expand $HOME, so it's filled in here.
cat > ~/.local/share/applications/lexmechanic.desktop <<EOF
[Desktop Entry]
Type=Application
Name=Lexmechanic
Comment=Translate game dialogue
Exec=sh -c "exec $HOME/.local/bin/lexmechanic >> $HOME/.cache/lexmechanic/daemon.log 2>&1"
Icon=$HOME/.local/share/icons/lexmechanic.svg
Terminal=false
Categories=Utility;
Actions=stop;

[Desktop Action stop]
Name=Stop
Exec=pkill -x lexmechanic
EOF

# Optional desktop icon
desktop=$(xdg-user-dir DESKTOP)
cp ~/.local/share/applications/lexmechanic.desktop "$desktop/" && chmod +x "$desktop/lexmechanic.desktop"
```

- **Start:** click it; the daemon starts in the background and opens the UI through `openCommand`. The first time
  you click the desktop icon, KDE may ask whether to trust it.
- **Stop:** right-click the icon or menu entry and choose **Stop**.
- **Log:** there is no terminal, so the output goes to `~/.cache/lexmechanic/daemon.log`.
- Clicking it while the daemon is already running does nothing: the second copy finds the port taken, logs that and
  exits.

### Linux setup

- **Hotkey:** the daemon reads mouse buttons and keys from `/dev/input` (evdev), which works under Wayland and while
  another window, such as GeForce NOW, has focus. Without the `input` group, bind `lexmechanic trigger` to a desktop
  shortcut instead.
- **Screenshots:** the default `captureCommand` uses KDE's `spectacle`. On other desktops, set it to a command that
  saves the full screen as a PNG, e.g. `["grim", "{file}"]` on wlroots compositors.
- **API key:** put it in `apiKeys`. A daemon started by autostart or a desktop launcher does not see keys exported in
  your shell profile.
- **Display:** to open the UI full-screen, set `openCommand` to `["firefox", "--kiosk", "{url}"]`, or
  `["flatpak", "run", "org.mozilla.firefox", "--kiosk", "{url}"]` for the Flatpak (as on SteamOS). To keep it on a
  second screen, such as the Steam Deck's built-in one, pin the window there with a window rule.

---

<p align="center">
  <img src="art/lexmechanic3.jpg" alt="Lexmechanic">
</p>