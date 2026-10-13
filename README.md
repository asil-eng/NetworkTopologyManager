# Network Topology Manager

A Java Swing + JDBC (MySQL) desktop application for **designing, managing and simulating computer networks**, like a mini Cisco Packet Tracer.

- Log in as **Admin** (full control) or **Viewer** (read-only)
- Manage saved networks: create, edit, delete, search
- Drag-and-drop canvas: place **routers, switches, PCs and servers** and connect them with cables
- **Send packets** between devices and watch them travel along the route
- Choose the routing algorithm: **BFS** (fewest hops) or **Dijkstra** (lowest latency)
- **Failure simulation**: switch a router off and packets reroute around it
- **Topology templates**: build a Star, Ring, Mesh, Tree or (simulated) Bus network automatically, with a live preview
- Packet history log and reports (charts and statistics from SQL queries)

---

## 1. Setup

**You need:**
1. **JDK 17 or newer**: https://adoptium.net
2. **MySQL**: either **Docker** (easiest, see below) or MySQL Server 8 from https://dev.mysql.com/downloads/mysql/
3. **MySQL Connector/J** (the JDBC driver): https://dev.mysql.com/downloads/connector/j/
   Choose "Platform Independent", unzip it, and copy `mysql-connector-j-x.x.x.jar` into the `lib` folder.

**First, create your local config files** (they are git-ignored, so your password is never committed):
```bash
cp .env.example .env                       # Windows: copy .env.example .env
cp db.properties.example db.properties     # Windows: copy db.properties.example db.properties
```
Pick a password and set it in **both** files (`MYSQL_ROOT_PASSWORD` in `.env`, `db.password` in `db.properties`).

**Option A: MySQL in Docker (recommended).** One command starts MySQL and creates all the tables and demo data:
```bash
docker compose up -d
```
Stop it with `docker compose down`.
Reset all data with `docker compose down -v`.

**Option B: your own MySQL install.** Create the database:
```bash
mysql -u root -p < database/schema.sql
```
Then put your MySQL root password in `db.properties` (you don't need `.env`).

**Run:**
- macOS / Linux: `./run.sh`
- Windows: double-click `run.bat`
- IntelliJ: open the folder, mark `src` as Sources Root, add the jar in `lib` as a library, and run `ntm.Main`.
  The working directory must be the project folder so `db.properties` is found.

**Logins:** `admin` / `admin123` and `viewer` / `viewer123`

---

## 2. Demo script (for the presentation)

1. Log in as **admin** and open **College Computer Lab**.
2. Choose **Send Packet** and click **PC1**, then **SRV1**. Watch the packet travel; each device logs what it does.
3. Change the routing to **Dijkstra** and send again. It now goes **R1 → R3 → R2** (20 ms) instead of **R1 → R2** (50 ms).
   BFS picks the fewest hops; Dijkstra picks the lowest latency.
4. Choose **Select**, click **R3**, and press **Toggle On/Off**. Send again: the packet **reroutes** through R1 → R2.
5. Turn **R1** off and send again. You get a `NoRouteException` error, and the failure is saved in the log.
6. Try to connect **PC1** directly to **PC2**. This is refused, because PCs can't be plugged into each other.
7. Click **From Template...**, choose **Ring** with 6 routers, and press Create. Send a packet across the ring, turn one router off, and send again: it goes the other way round.
8. Open **Packet Log** and **Reports** to show the SQL results.
9. Log out, log in as **viewer**, and show that the editing buttons are disabled.

---

## 3. Project structure

```
NetworkTopologyManager/
├── database/schema.sql      MySQL tables + demo data
├── db.properties.example    template for db.properties (MySQL url, username, password)
├── .env.example             template for .env (Docker MySQL password)
├── docker-compose.yml       runs MySQL in Docker
├── lib/                     put mysql-connector-j-x.x.x.jar here
├── run.sh / run.bat         compile + run
└── src/ntm/
    ├── Main.java            starts the app
    ├── model/               the OOP core (classes that represent things)
    │   ├── Device.java          ABSTRACT base class of all devices
    │   ├── Router/Switch/PC/Server.java   subclasses of Device
    │   ├── DeviceType.java      enum of the 4 device types
    │   ├── DeviceFactory.java   creates the right subclass (Factory pattern)
    │   ├── Drawable.java        interface: draw(), contains()
    │   ├── Connectable.java     interface: cabling rules
    │   ├── Link.java            a cable between two devices
    │   ├── Network.java         the graph: list of devices + list of links
    │   ├── Packet.java          data travelling through the network
    │   ├── PacketLog.java       one row of packet history
    │   └── User/Admin/Viewer.java   ABSTRACT user + 2 subclasses
    ├── service/
    │   ├── RoutingService.java  BFS and Dijkstra
    │   └── TopologyBuilder.java saves a template's devices + cables in ONE transaction
    ├── topology/                ready-made network shapes
    │   ├── TopologyTemplate.java    ABSTRACT: createPlan(size)
    │   ├── Star/Ring/Mesh/Tree/BusTopology.java   subclasses
    │   └── TopologyPlan.java    the blueprint: nodes + edges (Java records)
    ├── db/DBConnection.java     single shared DB connection (Singleton pattern)
    ├── dao/                     all SQL lives here (DAO pattern)
    │   ├── CrudDAO.java         GENERIC interface CrudDAO<T>: save/update/delete
    │   ├── *DAO.java            interfaces
    │   ├── *DAOImpl.java        JDBC implementations
    │   └── ReportDAO.java       GROUP BY / JOIN report queries
    ├── exception/               custom exceptions
    ├── util/PasswordUtil.java   SHA-256 password hashing
    └── ui/                      Swing windows
        ├── LoginFrame.java
        ├── DashboardFrame.java      list of networks
        ├── NetworkEditorFrame.java  the editor (toolbar, canvas, table, log)
        ├── CanvasPanel.java         drawing + mouse + packet animation
        ├── DeviceForm.java          add/edit device form
        ├── TemplateDialog.java      pick a topology, with a live preview
        ├── UsersDialog.java         admin: manage users
        ├── PacketLogDialog.java
        ├── ReportsDialog.java       includes a hand-drawn bar chart
        └── UiUtil.java
```

The app is split into **layers**: `ui` (what you see) → `service` (logic) → `dao` (SQL) → MySQL.
The UI never writes SQL itself; it asks a DAO.

---

## 4. Where each OOP concept is used

| Concept | Where | Explanation |
|---|---|---|
| **Classes and objects** | `model` package | Every device, cable, network, packet and user is an object |
| **Encapsulation** | `Device`, `Link`, `Network` | Fields are `private`. `setIpAddress()` rejects bad IPs, `setLatencyMs()` rejects values outside 1–1000, `updateDetails()` changes all fields or none |
| **Inheritance** | `Router`, `Switch`, `PC` and `Server` extend `Device`; `Admin` and `Viewer` extend `User` | Shared code (position, name, IP, drawing labels) is written once in the parent |
| **Abstraction** | `abstract class Device`, `abstract class User` | You can't create a plain "Device", only a specific kind |
| **Polymorphism** | `handlePacket()`, `drawShape()`, `getMaxConnections()`, `canForwardTraffic()`, `canEditNetworks()`, `TopologyTemplate.createPlan()` | The same method call behaves differently for each subclass. `CanvasPanel` just calls `device.draw()` and each device draws its own icon |
| **Interfaces** | `Drawable`, `Connectable`, `CrudDAO<T>`, every `*DAO` | Define *what* must be done; classes decide *how* |
| **Generics** | `CrudDAO<T>`, `Map<Device, List<Link>>`, `PriorityQueue<QueueEntry>` | Type-safe reusable code |
| **Exception handling** | `InvalidIPException`, `NoRouteException`, `DeviceOfflineException` + try/catch for `SQLException` | Custom checked exceptions with clear messages shown to the user |
| **Enums** | `DeviceType`, `RoutingService.Algorithm`, editor `Mode` | Fixed sets of values |
| **Collections** | `ArrayList`, `HashMap`, `HashSet`, `ArrayDeque`, `PriorityQueue`, `LinkedList` | Used for the graph and both algorithms |
| **Design patterns** | Singleton (`DBConnection`), Factory (`DeviceFactory`), DAO (`dao` package), Template Method (`Device.draw()` → `drawShape()`) | |
| **Event handling** | Mouse listeners in `CanvasPanel`, action listeners on buttons | |
| **Timer / animation** | `javax.swing.Timer` in `CanvasPanel.animatePacket()` | Moves the packet at about 60 frames per second without freezing the window |

---

## 5. Database design

| Table | Purpose | Key points |
|---|---|---|
| `users` | Login accounts | Password stored as a SHA-256 hash; `role` is ADMIN or VIEWER |
| `networks` | Saved network designs | `created_by` → `users.id` |
| `devices` | Devices on a network | `network_id` → `networks.id`, **ON DELETE CASCADE**; IP is unique within a network |
| `connections` | Cables | Two foreign keys to `devices`; `latency_ms` is the Dijkstra weight |
| `packet_logs` | Packet history | Delivered / failed, route, hops, latency |

**SQL features used:** `PreparedStatement` (prevents SQL injection), **transactions** (`commit` / `rollback` in
`TopologyBuilder`), `INSERT` with generated keys, `UPDATE`, `DELETE`,
`LIKE` search, `LEFT JOIN` (network creator, device counts, packet log names), `GROUP BY` + `COUNT` / `SUM` / `AVG`
(reports), and foreign keys with `CASCADE` / `SET NULL`.

---

## 6. How routing works

The network is a **graph**: devices are nodes and cables are edges. `Network.buildAdjacencyList()` turns it into a
`Map<Device, List<Link>>`.

- **BFS** explores neighbours level by level with a queue, so the first time it reaches the destination is the path with the **fewest cables**.
- **Dijkstra** always expands the device with the **lowest total latency so far** (using a `PriorityQueue`), so it finds the **fastest** path even when it has more hops.

Both algorithms skip devices that are **offline**, and never pass *through* a PC or server
(`canForwardTraffic()` is false for them). That's how failures make packets reroute.

**In real networks:** Dijkstra is the algorithm OSPF uses (with link cost instead of latency). Fewest-hops routing is
what RIP does, although RIP computes it with Bellman-Ford rather than BFS.

---

## 7. Topology templates

| Template | Built from | Notes |
|---|---|---|
| Star | 1 switch + 3–20 PCs | If the centre switch fails, every PC is cut off |
| Ring | 3–12 routers | Two ways around: one failure doesn't split it |
| Mesh | 3–8 routers, every pair cabled | n × (n − 1) / 2 cables; max 8 because a router has 8 ports |
| Tree | 1 router, 2–5 switches, 3 PCs each | The usual school / office layout |
| Bus (simulated) | A chain of 2–8 switches, one PC each | A real bus is one shared cable; point-to-point cables can only imitate it |

PCs can't form a ring or mesh because each PC has only one port and never forwards traffic, which is how real
networks work too.
