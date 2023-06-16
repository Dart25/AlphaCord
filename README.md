## AlphaCord

### Setup

Clone this repository first.
To edit the plugin display name and other data, take a look at `plugin.json`.
Edit the name of the project itself by going into `settings.gradle`.

### Basic Usage

Set the `channelId`, `discordToken`, and `webhookUrl` config options, and then start your server.

### Building a Jar

`gradlew jar` / `./gradlew jar`

Output jar should be in `build/libs`.


### Installing

Simply place the output jar from the step above in your server's `config/mods` directory and restart the server.
List your currently installed plugins/mods by running the `mods` command.
