# Chain of Fun

A NeoForge 1.21.1 mod that chains you to a friend with an elastic rope. Swing off cliffs, reel each other up, yank each other around. Nobody wanders off alone.

**Install on:** every player's game *and* the server.

## How to play

| What | How |
|---|---|
| Craft a **Chain of Fun** | 3 sticks in a diagonal line |
| Chain up | Right-click your friend while holding it |
| Unchain | **Both** sneak + right-click each other within 5 seconds (you get the chain back) |
| Brace / anchor | Sneak while standing on solid ground |
| Reel in / out (2–16 blocks) | While braced: hold **Left Alt** + scroll (up = in, down = out) |
| Yank your partner | **G** (3 s cooldown) |

Rebind the keys under Options → Controls → Key Binds → *Chain of Fun*.

## Rules

- The rope is slightly elastic. Inside its length you move freely; past it, it pulls.
- A braced player barely moves; a standing player slides; an airborne player gets dragged.
- When the rope catches a fall, you take no fall damage.
- It works while riding horses, boats and while flying with elytra.
- When one of you dies, the survivor is pulled to wherever the other respawns.
- Portals only work if you go through together.
- The chain never breaks on its own.

## Admin commands (op level 2)

```
/chainoffun link <a> <b>
/chainoffun unlink <player>
/chainoffun length <player> <blocks>
/chainoffun list
```

## Config

`world/serverconfig/chainoffun-server.toml`: rope lengths, reel step, stiffness, damping, anchor strengths, yank strength and cooldown, unlink window, portal rules.

## Building

Grab the jar from the **Releases** page (the `latest` release updates on every push), or from the Actions tab → latest run → artifact `chain-of-fun-jar`.
Locally: install Java 21 and Gradle 8.10+, then run `gradle build`. The jar lands in `build/libs/`.
