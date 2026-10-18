# DungeonAPI

**A dungeon regeneration API for Minecraft Forge 1.20.1**

DungeonAPI is a Minecraft mod designed to manage and regenerate custom dungeons using WorldEdit schematics. It allows server administrators to configure dungeon locations, reset structures through in-game commands, and interact with the system through an HTTP API.

Instead of manually rebuilding structures or editing configuration files every time you add a dungeon, DungeonAPI provides commands to register, manage, and regenerate them directly from Minecraft.

## Features

- **Automatic dungeon configuration:** Create dungeon folders and `dungeon.properties` files directly in-game.
- **WorldEdit schematic support:** Use `.schem` files stored in the WorldEdit schematics directory.
- **Dungeon management:** List all configured dungeons and regenerate individual structures or all registered dungeons.
- **Interactive overwrite confirmation:** Confirm or cancel changes when saving a dungeon that already exists.
- **Multi-world support:** Configure dungeons in different Minecraft dimensions, including the Overworld and Nether.
- **HTTP API:** Check the API status and request dungeon regeneration through HTTP endpoints.
- **Configurable API authentication:** Protect HTTP requests using a Bearer API key.
- **Automatic default configuration:** Generate the initial dungeon configuration when the server starts.

## Requirements

- Minecraft **1.20.1**
- Minecraft Forge **47.x**
- WorldEdit for Forge, compatible with your Minecraft and Forge versions
- Java 17

## Installation

1. Install Minecraft Forge 1.20.1 on your server.
2. Install a compatible version of WorldEdit for Forge.
3. Place the DungeonAPI JAR file in your server's `mods` folder.
4. Start the server to generate the default configuration.
5. Place your WorldEdit schematics in:

   ```text
   config/worldedit/schematics/
   ```

DungeonAPI will create its configuration files automatically when the server starts.

## In-game Commands

All DungeonAPI commands require operator permission level 2 or higher.

| Command | Description |
|---|---|
| `/dungeon-api list` | Lists all configured dungeons. |
| `/dungeon-api save <dungeon> <schematic> <pos>` | Creates or updates a dungeon configuration. |
| `/dungeon-api reset <dungeon>` | Regenerates a specific dungeon. |
| `/dungeon-api reset-all` | Regenerates all configured dungeons. |

### Create a dungeon

For example:

```mcfunction
/dungeon-api save castle castle_schematic ~ ~ ~
```

This command registers a dungeon named `castle`, using `castle_schematic.schem` and the position specified by `~ ~ ~`.

DungeonAPI automatically creates the directory and configuration file.

If the dungeon name already exists, the mod asks for confirmation. Type `y` in chat to overwrite the configuration. Any other response cancels the operation.

**Note:** Overwriting a configuration changes the saved settings but does not immediately regenerate the structure in the world.

### List dungeons

```mcfunction
/dungeon-api list
```

Displays the identifiers of all dungeons whose configuration files can be loaded successfully.

### Regenerate a dungeon

```mcfunction
/dungeon-api reset castle
```

Regenerates the specified dungeon using its configured schematic, dimension, and coordinates.

### Regenerate all dungeons

```mcfunction
/dungeon-api reset-all
```

Attempts to regenerate every configured dungeon and reports the number of successful and failed operations.

## Configuration

DungeonAPI stores its main configuration in:

```text
config/dungeonapi.properties
```

Example:

```properties
port=8080
api-key=dungeon-test-123456
```

- `port`: HTTP API listening port.
- `api-key`: Bearer token required to authenticate HTTP requests.

**Security:** Change the default API key before using the HTTP API beyond local testing. Avoid exposing the API port to untrusted networks.

### Dungeon configurations

Each dungeon has its own directory:

```text
config/
└── dungeon-api/
    └── dungeons/
        └── castle/
            └── dungeon.properties
```

Example `dungeon.properties`:

```properties
world=minecraft:overworld
x=120
y=64
z=-35
schematic=castle_schematic
```

| Property | Description |
|---|---|
| `world` | Minecraft dimension in which the dungeon is located. |
| `x`, `y`, `z` | Position where the schematic is pasted. |
| `schematic` | Name of the WorldEdit schematic, without the `.schem` extension. |

Dungeon identifiers and schematic names support letters, numbers, underscores, and hyphens.

## HTTP API

DungeonAPI also provides an HTTP interface for external tools and applications.

### Check API status

```http
GET /dungeon/test
```

Example local request:

```powershell
Invoke-WebRequest http://localhost:8080/dungeon/test
```

This endpoint checks whether the API is responding.

### Request a dungeon reset

```http
POST /dungeon/reset
Authorization: Bearer YOUR_API_KEY
```

The reset endpoint accepts authenticated HTTP requests to trigger dungeon regeneration. The required request body depends on the implementation of the HTTP handler.

The API uses the configured port and API key from `config/dungeonapi.properties`.

## Building from Source

### Prerequisites

- Java Development Kit (JDK) 17
- Git
- An IDE such as IntelliJ IDEA or Eclipse (optional)

### Build the mod

Clone or download the repository, then open a terminal in the project directory.

On Windows:

```powershell
.\gradlew build
```

On Linux or macOS:

```bash
./gradlew build
```

The compiled JAR will be available in:

```text
build/libs/
```

### Run a development server

On Windows:

```powershell
.\gradlew runServer
```

On Linux or macOS:

```bash
./gradlew runServer
```

To launch the development client, use:

```powershell
.\gradlew runClient
```

## Troubleshooting

- **The schematic cannot be found:** Check that the `.schem` file exists in `config/worldedit/schematics/` and that the configured name does not include the file extension.
- **A dungeon does not appear in the list:** Check that its directory contains a valid `dungeon.properties` file.
- **A reset fails:** Verify the configured dimension, coordinates, schematic, and server logs.
- **The HTTP API does not respond:** Check the configured port, server logs, and whether another application is already using that port.
- **The commands are unavailable:** Make sure DungeonAPI loaded correctly and that your player has operator permission level 2 or higher.

## Technologies

- Minecraft Forge 1.20.1
- Java 17
- WorldEdit
- Gradle
- HTTP Server API

## License

Add your project's chosen license here. If you intend to publish the mod as an open-source project, include a `LICENSE` file in the repository.

---

**DungeonAPI — Manage, configure, and regenerate your Minecraft dungeons with ease.**
