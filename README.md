## AlphaCord

## You probably shouldn't care about this branch unless you administrate the Fish servers :3

### Basic Usage

To get a Jar, either build it yourself (detailed below), or download one from [GitHub Actions](https://github.com/Dart25/AlphaCord/actions).

Set the `channelId`, `discordToken`, and `webhookUrl` config options, and then start your server.

### Building a Jar

`gradlew jar` / `./gradlew jar`

Output jar should be in `build/libs`.


### Installing

Simply place the output jar from the step above in your server's `config/mods` directory and restart the server.
List your currently installed plugins/mods by running the `mods` command.
