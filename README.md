# Minecraft Combat Plugin: Bonestrike Havoc

Bonestrike Havoc is a Minecraft combat plugin for Paper 1.21 that adds a custom bone themed mace with fall damage protection, falling attacks, bonus damage, status effects, player stuns, explosion resistance, and a chance to upgrade after kills.

## Install

1. Build the plugin with Java 21 and Maven:

   ```text
   mvn package
   ```

2. Copy the generated `target/plugin.jar` file into your server's `plugins` folder.

3. Start or restart a Paper 1.21 server.

4. Make sure the server uses Java 21 or newer.

## Use

1. Join the server as an operator or a player with the `bonemace.give` permission.

2. Run:

   ```text
   /bonemace
   ```

3. Hold the Bone Handle Mace in either your main hand or off hand.

4. While holding the mace, fall damage is cancelled.

5. Attack a living entity to deal 4 damage, apply Slowness for 10 seconds, and apply Wither for 2 seconds.

6. Attack after falling from at least 4 blocks to deal additional damage based on the fall height and launch the target upward.

7. The mace has a 10% chance to stun a target. A player who is stunned cannot move for 5 seconds.

8. Kill a player or a boss entity such as a Warden, Wither, or Ender Dragon while holding the mace. There is a 20% chance that the mace will upgrade. Other living entities have a 5% upgrade chance.

9. An upgraded mace gains 5 additional attack damage. Successful player kill upgrades increase the stun chance by 10%, up to 100%.

10. A dropped Bone Handle Mace is protected from block and entity explosions.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/c44c2896-8179-4a7e-83a0-b11d06961396?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=c44c2896-8179-4a7e-83a0-b11d06961396
