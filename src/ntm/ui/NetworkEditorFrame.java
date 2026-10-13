package ntm.ui;

import ntm.dao.DeviceDAO;
import ntm.dao.DeviceDAOImpl;
import ntm.dao.LinkDAO;
import ntm.dao.LinkDAOImpl;
import ntm.dao.PacketLogDAO;
import ntm.dao.PacketLogDAOImpl;
import ntm.exception.DeviceOfflineException;
import ntm.exception.InvalidIPException;
import ntm.exception.NoRouteException;
import ntm.model.Device;
import ntm.model.DeviceFactory;
import ntm.model.DeviceType;
import ntm.model.Link;
import ntm.model.Network;
import ntm.model.Packet;
import ntm.model.PacketLog;
import ntm.model.User;
import ntm.service.RoutingService;
import ntm.service.TopologyBuilder;
import ntm.topology.TopologyPlan;
import ntm.topology.TopologyTemplate;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The network editor: toolbar on top, drawing canvas in the middle,
 * device table on the right, and the activity log at the bottom.
 */
public class NetworkEditorFrame extends JFrame {

    /** What a click on the canvas currently does. */
    private enum Mode {
        SELECT("Select", null, "Click a device to select it, drag to move it. Double-click a device or cable to edit it."),
        ADD_ROUTER("+ Router", DeviceType.ROUTER, "Click an empty spot on the canvas to place a new router."),
        ADD_SWITCH("+ Switch", DeviceType.SWITCH, "Click an empty spot on the canvas to place a new switch."),
        ADD_PC("+ PC", DeviceType.PC, "Click an empty spot on the canvas to place a new PC."),
        ADD_SERVER("+ Server", DeviceType.SERVER, "Click an empty spot on the canvas to place a new server."),
        CONNECT("Connect", null, "Click the first device, then the second device, to connect them with a cable."),
        DELETE("Delete", null, "Click a device or a cable to delete it."),
        SEND_PACKET("Send Packet", null, "Click the SOURCE device, then the DESTINATION device.");

        private final String label;
        private final DeviceType deviceType;
        private final String hint;

        Mode(String label, DeviceType deviceType, String hint) {
            this.label = label;
            this.deviceType = deviceType;
            this.hint = hint;
        }

        /** Modes that change the network - not available to viewers. */
        boolean changesNetwork() {
            return this != SELECT && this != SEND_PACKET;
        }
    }

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final User user;
    private final Network network;
    private final DeviceDAO deviceDAO = new DeviceDAOImpl();
    private final LinkDAO linkDAO = new LinkDAOImpl();
    private final PacketLogDAO packetLogDAO = new PacketLogDAOImpl();
    private final RoutingService routingService = new RoutingService();
    private final TopologyBuilder topologyBuilder = new TopologyBuilder(deviceDAO, linkDAO);

    private final CanvasPanel canvas;
    private final DefaultTableModel deviceTableModel = new DefaultTableModel(
            new String[]{"Name", "Type", "IP Address", "Status", "Ports"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable deviceTable = new JTable(deviceTableModel);
    private final List<Device> tableDevices = new ArrayList<>(); // devices shown in the table, in row order
    private final JTextField deviceSearchField = new JTextField();
    private final JTextArea logArea = new JTextArea(7, 40);
    private final JLabel hintLabel = new JLabel();
    private final JComboBox<RoutingService.Algorithm> algorithmBox = new JComboBox<>(RoutingService.Algorithm.values());

    private Mode mode = Mode.SELECT;
    private Device pendingDevice;   // first device clicked in Connect / Send Packet mode
    private boolean updatingTable;  // stops table <-> canvas selection from looping

    public NetworkEditorFrame(User user, Network network) {
        super("Network: " + network.getName() + (user.canEditNetworks() ? "" : "   (read-only)"));
        this.user = user;
        this.network = network;
        this.canvas = new CanvasPanel(network, this);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        add(buildToolBar(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(canvas), buildDevicePanel());
        split.setResizeWeight(1.0);
        add(split, BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        setSize(1300, 820);
        setLocationRelativeTo(null);
        loadNetwork();
        setMode(Mode.SELECT);
    }

    // ==================================================================
    // Building the window
    // ==================================================================

    private JToolBar buildToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        ButtonGroup group = new ButtonGroup();
        for (Mode m : Mode.values()) {
            JToggleButton button = new JToggleButton(m.label);
            button.setEnabled(user.canEditNetworks() || !m.changesNetwork());
            button.setSelected(m == Mode.SELECT);
            button.addActionListener(e -> setMode(m));
            group.add(button);
            bar.add(button);
            if (m == Mode.SELECT || m == Mode.ADD_SERVER || m == Mode.DELETE) {
                bar.addSeparator();
            }
        }

        JButton templateButton = new JButton("From Template...");
        templateButton.setToolTipText("Build a star, ring, mesh, tree or bus topology automatically");
        templateButton.setEnabled(user.canEditNetworks());
        templateButton.addActionListener(e -> createFromTemplate());
        bar.add(templateButton);
        bar.addSeparator();

        bar.add(new JLabel(" Routing: "));
        algorithmBox.setMaximumSize(algorithmBox.getPreferredSize());
        bar.add(algorithmBox);
        bar.addSeparator();

        JButton toggleButton = new JButton("Toggle On/Off");
        toggleButton.setToolTipText("Switch the selected device on or off to simulate a failure");
        toggleButton.setEnabled(user.canEditNetworks());
        toggleButton.addActionListener(e -> toggleSelectedDevice());
        bar.add(toggleButton);

        JButton logButton = new JButton("Packet Log");
        logButton.addActionListener(e -> new PacketLogDialog(this, network, user).setVisible(true));
        bar.add(logButton);

        JButton reportsButton = new JButton("Reports");
        reportsButton.addActionListener(e -> new ReportsDialog(this, network).setVisible(true));
        bar.add(reportsButton);
        return bar;
    }

    private JPanel buildDevicePanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Devices"));
        panel.setPreferredSize(new Dimension(380, 0));

        JPanel search = new JPanel(new BorderLayout(4, 0));
        search.add(new JLabel("Search: "), BorderLayout.WEST);
        search.add(deviceSearchField, BorderLayout.CENTER);
        JButton goButton = new JButton("Go");
        search.add(goButton, BorderLayout.EAST);
        goButton.addActionListener(e -> refreshDeviceTable());
        deviceSearchField.addActionListener(e -> refreshDeviceTable());
        panel.add(search, BorderLayout.NORTH);

        deviceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deviceTable.setRowHeight(22);
        deviceTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || updatingTable) {
                return;
            }
            int row = deviceTable.getSelectedRow();
            canvas.setSelectedDevice(row >= 0 ? tableDevices.get(row) : null);
        });
        panel.add(new JScrollPane(deviceTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton editButton = new JButton(user.canEditNetworks() ? "Edit Selected Device" : "View Selected Device");
        editButton.addActionListener(e -> {
            Device selected = canvas.getSelectedDevice();
            if (selected == null) {
                UiUtil.showInfo(this, "Select a device in the table or on the canvas first.");
            } else {
                editDevice(selected);
            }
        });
        buttons.add(editButton);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        hintLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        hintLabel.setFont(hintLabel.getFont().deriveFont(Font.ITALIC));
        panel.add(hintLabel, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Activity Log"));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ==================================================================
    // Loading and showing data
    // ==================================================================

    private void loadNetwork() {
        try {
            network.clear();
            for (Device device : deviceDAO.findByNetwork(network.getId())) {
                network.addDevice(device);
            }
            for (Link link : linkDAO.findByNetwork(network)) {
                network.addLink(link);
            }
            log("Loaded \"" + network.getName() + "\": " + network.getDevices().size() + " devices, "
                    + network.getLinks().size() + " cables.");
        } catch (SQLException ex) {
            UiUtil.showDatabaseError(this, ex);
        }
        refreshDeviceTable();
        canvas.repaint();
    }

    /** Fills the device table. If there is a search keyword, the database does the searching. */
    private void refreshDeviceTable() {
        String keyword = deviceSearchField.getText().trim();
        List<Device> rows;
        if (keyword.isEmpty()) {
            rows = new ArrayList<>(network.getDevices());
        } else {
            rows = new ArrayList<>();
            try {
                for (Device found : deviceDAO.search(network.getId(), keyword)) {
                    Device live = network.findById(found.getId());
                    if (live != null) {
                        rows.add(live);
                    }
                }
            } catch (SQLException ex) {
                UiUtil.showDatabaseError(this, ex);
            }
        }

        updatingTable = true;
        tableDevices.clear();
        tableDevices.addAll(rows);
        deviceTableModel.setRowCount(0);
        for (Device d : rows) {
            deviceTableModel.addRow(new Object[]{
                    d.getName(), d.getType().getLabel(), d.getIpAddress(), d.isOnline() ? "Online" : "OFFLINE",
                    network.countLinks(d) + " / " + d.getMaxConnections()
            });
        }
        updatingTable = false;
        selectInTable(canvas.getSelectedDevice());
    }

    private void selectInTable(Device device) {
        updatingTable = true;
        int row = tableDevices.indexOf(device);
        if (row >= 0) {
            deviceTable.setRowSelectionInterval(row, row);
            deviceTable.scrollRectToVisible(deviceTable.getCellRect(row, 0, true));
        } else {
            deviceTable.clearSelection();
        }
        updatingTable = false;
    }

    private void log(String message) {
        logArea.append("[" + LocalTime.now().format(TIME_FORMAT) + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void setMode(Mode newMode) {
        mode = newMode;
        pendingDevice = null;
        if (newMode != Mode.SELECT) {
            canvas.setSelectedDevice(null);
            selectInTable(null);
        }
        canvas.setCursor(Cursor.getPredefinedCursor(
                newMode == Mode.SELECT ? Cursor.DEFAULT_CURSOR : Cursor.CROSSHAIR_CURSOR));
        hintLabel.setText("Mode: " + newMode.label + "  -  " + newMode.hint);
    }

    // ==================================================================
    // Called by CanvasPanel
    // ==================================================================

    boolean isDragAllowed() {
        return mode == Mode.SELECT && user.canEditNetworks() && !canvas.isAnimating();
    }

    void onDeviceMoved(Device device) {
        try {
            deviceDAO.updatePosition(device);
        } catch (SQLException ex) {
            UiUtil.showDatabaseError(this, ex);
        }
        canvas.revalidate(); // the canvas may need to grow
    }

    void onCanvasClicked(int x, int y, int clickCount) {
        if (canvas.isAnimating()) {
            return; // wait for the packet to arrive
        }
        Device clicked = network.findDeviceAt(x, y);
        switch (mode) {
            case SELECT -> {
                canvas.setSelectedDevice(clicked);
                selectInTable(clicked);
                if (clickCount == 2) {
                    if (clicked != null) {
                        editDevice(clicked);
                    } else {
                        Link link = network.findLinkNear(x, y);
                        if (link != null) {
                            editLink(link);
                        }
                    }
                }
            }
            case ADD_ROUTER, ADD_SWITCH, ADD_PC, ADD_SERVER -> {
                if (clicked == null) {
                    addDevice(mode.deviceType, x, y);
                }
            }
            case CONNECT -> handleConnectClick(clicked);
            case DELETE -> handleDeleteClick(clicked, x, y);
            case SEND_PACKET -> handleSendClick(clicked);
        }
    }

    // ==================================================================
    // Device actions
    // ==================================================================

    private void addDevice(DeviceType type, int x, int y) {
        DeviceForm form = new DeviceForm(type, network.suggestName(type), network.suggestIp(), true);
        while (form.showDialog(this, "Add " + type.getLabel())) {
            String error = validateDeviceForm(form, null);
            if (error != null) {
                UiUtil.showError(this, error);
                continue;
            }
            try {
                Device device = DeviceFactory.create(type, 0, network.getId(), form.getName(), form.getIp(),
                        x - Device.SIZE / 2, y - Device.SIZE / 2);
                device.setOnline(form.isOnline());
                deviceDAO.save(device);
                network.addDevice(device);
                canvas.setSelectedDevice(device);
                refreshDeviceTable();
                log("Added " + type.getLabel() + " " + device);
                return;
            } catch (InvalidIPException | IllegalArgumentException ex) {
                UiUtil.showError(this, ex.getMessage());
            } catch (SQLException ex) {
                UiUtil.showDatabaseError(this, ex);
                return;
            }
        }
    }

    private void editDevice(Device device) {
        if (!user.canEditNetworks()) {
            UiUtil.showInfo(this, "Name: " + device.getName() + "\nType: " + device.getType().getLabel()
                    + "\nIP address: " + device.getIpAddress()
                    + "\nStatus: " + (device.isOnline() ? "Online" : "Offline")
                    + "\nCables: " + network.countLinks(device) + " of " + device.getMaxConnections());
            return;
        }

        DeviceForm form = new DeviceForm(device.getType(), device.getName(), device.getIpAddress(), device.isOnline());
        while (form.showDialog(this, "Edit " + device.getName())) {
            String error = validateDeviceForm(form, device);
            if (error != null) {
                UiUtil.showError(this, error);
                continue;
            }
            String oldName = device.getName();
            String oldIp = device.getIpAddress();
            boolean oldOnline = device.isOnline();
            try {
                device.updateDetails(form.getName(), form.getIp(), form.isOnline());
                deviceDAO.update(device);
                log("Updated " + device);
            } catch (InvalidIPException | IllegalArgumentException ex) {
                UiUtil.showError(this, ex.getMessage());
                continue;
            } catch (SQLException ex) {
                restoreDevice(device, oldName, oldIp, oldOnline);
                UiUtil.showDatabaseError(this, ex);
            }
            break;
        }
        refreshDeviceTable();
        canvas.repaint();
    }

    private void restoreDevice(Device device, String name, String ip, boolean online) {
        try {
            device.updateDetails(name, ip, online);
        } catch (InvalidIPException ignored) {
            // the old values were valid, so this cannot happen
        }
    }

    /** Returns an error message, or null if the form is OK. */
    private String validateDeviceForm(DeviceForm form, Device editing) {
        if (form.getName().isEmpty()) {
            return "Device name cannot be empty.";
        }
        if (network.isNameInUse(form.getName(), editing)) {
            return "Another device is already called \"" + form.getName() + "\".";
        }
        if (!Device.isValidIP(form.getIp())) {
            return new InvalidIPException(form.getIp()).getMessage();
        }
        if (network.isIpInUse(form.getIp(), editing)) {
            return "IP address " + form.getIp() + " is already used by another device.\nEvery device needs its own IP.";
        }
        return null;
    }

    private void toggleSelectedDevice() {
        Device device = canvas.getSelectedDevice();
        if (device == null) {
            UiUtil.showInfo(this, "In Select mode, click a device first, then press Toggle On/Off.");
            return;
        }
        if (canvas.isAnimating()) {
            return;
        }
        device.setOnline(!device.isOnline());
        try {
            deviceDAO.updateStatus(device);
            log(device.getName() + " is now " + (device.isOnline() ? "ONLINE" : "OFFLINE"));
        } catch (SQLException ex) {
            device.setOnline(!device.isOnline()); // undo
            UiUtil.showDatabaseError(this, ex);
        }
        refreshDeviceTable();
        canvas.repaint();
    }

    // ==================================================================
    // Topology templates
    // ==================================================================

    private void createFromTemplate() {
        if (canvas.isAnimating()) {
            return;
        }
        TemplateDialog dialog = new TemplateDialog(this);
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return;
        }
        TopologyTemplate template = dialog.getTemplate();
        TopologyPlan plan = template.createPlan(dialog.getTopologySize());

        // Place the new topology to the right of everything already on the canvas.
        int originX = 0;
        for (Device device : network.getDevices()) {
            originX = Math.max(originX, device.getX() + Device.SIZE + 80);
        }
        int originY = 0;

        try {
            List<Device> created = topologyBuilder.build(network, plan, originX, originY);
            log("Created a " + template.getName() + " topology: " + created.size() + " devices, "
                    + plan.getEdges().size() + " cables.");
        } catch (SQLException ex) {
            UiUtil.showDatabaseError(this, ex);
        } catch (InvalidIPException | IllegalArgumentException ex) {
            UiUtil.showError(this, "Could not create the topology:\n" + ex.getMessage());
        }

        refreshDeviceTable();
        canvas.revalidate();
        canvas.repaint();
        Rectangle area = new Rectangle(originX, originY, plan.getWidth() + 40, plan.getHeight() + 40);
        SwingUtilities.invokeLater(() -> canvas.scrollRectToVisible(area));
    }

    // ==================================================================
    // Cable actions
    // ==================================================================

    private void handleConnectClick(Device clicked) {
        if (clicked == null) {
            return;
        }
        if (pendingDevice == null) {
            pendingDevice = clicked;
            canvas.setSelectedDevice(clicked);
            hintLabel.setText("Mode: Connect  -  Now click the device to connect " + clicked.getName() + " to.");
            return;
        }

        Device first = pendingDevice;
        pendingDevice = null;
        canvas.setSelectedDevice(null);
        setMode(Mode.CONNECT);

        try {
            network.validateNewLink(first, clicked);
            String input = JOptionPane.showInputDialog(this,
                    "Cable latency between " + first.getName() + " and " + clicked.getName() + " (ms):", "10");
            if (input == null) {
                return; // cancelled
            }
            Link link = new Link(0, network.getId(), first, clicked, Integer.parseInt(input.trim()));
            linkDAO.save(link);
            network.addLink(link);
            log("Connected " + first.getName() + " <-> " + clicked.getName() + " (" + link.getLatencyMs() + " ms)");
        } catch (NumberFormatException ex) {
            UiUtil.showError(this, "Latency must be a whole number, e.g. 10");
        } catch (IllegalArgumentException ex) {
            UiUtil.showError(this, ex.getMessage());
        } catch (SQLException ex) {
            UiUtil.showDatabaseError(this, ex);
        }
        refreshDeviceTable();
        canvas.repaint();
    }

    private void editLink(Link link) {
        if (!user.canEditNetworks()) {
            return;
        }
        String input = JOptionPane.showInputDialog(this, "New latency for the cable "
                + link.getDeviceA().getName() + " <-> " + link.getDeviceB().getName() + " (ms):", link.getLatencyMs());
        if (input == null) {
            return;
        }
        int oldLatency = link.getLatencyMs();
        try {
            link.setLatencyMs(Integer.parseInt(input.trim()));
            linkDAO.update(link);
            log("Cable " + link.getDeviceA().getName() + " <-> " + link.getDeviceB().getName()
                    + " latency changed to " + link.getLatencyMs() + " ms");
        } catch (NumberFormatException ex) {
            UiUtil.showError(this, "Latency must be a whole number, e.g. 10");
        } catch (IllegalArgumentException ex) {
            UiUtil.showError(this, ex.getMessage());
        } catch (SQLException ex) {
            link.setLatencyMs(oldLatency);
            UiUtil.showDatabaseError(this, ex);
        }
        canvas.repaint();
    }

    private void handleDeleteClick(Device clicked, int x, int y) {
        try {
            if (clicked != null) {
                if (UiUtil.confirm(this, "Delete " + clicked.getName() + "?\nIts cables will be removed too.")) {
                    deviceDAO.delete(clicked.getId());
                    network.removeDevice(clicked);
                    log("Deleted " + clicked);
                }
            } else {
                Link link = network.findLinkNear(x, y);
                if (link != null && UiUtil.confirm(this, "Delete the cable between "
                        + link.getDeviceA().getName() + " and " + link.getDeviceB().getName() + "?")) {
                    linkDAO.delete(link.getId());
                    network.removeLink(link);
                    log("Deleted cable " + link.getDeviceA().getName() + " <-> " + link.getDeviceB().getName());
                }
            }
        } catch (SQLException ex) {
            UiUtil.showDatabaseError(this, ex);
        }
        refreshDeviceTable();
        canvas.repaint();
    }

    // ==================================================================
    // Packet simulation
    // ==================================================================

    private void handleSendClick(Device clicked) {
        if (clicked == null) {
            return;
        }
        if (pendingDevice == null) {
            pendingDevice = clicked;
            canvas.setSelectedDevice(clicked);
            hintLabel.setText("Mode: Send Packet  -  Source is " + clicked.getName()
                    + ". Now click the DESTINATION device.");
            return;
        }
        Device source = pendingDevice;
        setMode(Mode.SEND_PACKET);
        sendPacket(source, clicked);
    }

    private void sendPacket(Device source, Device destination) {
        RoutingService.Algorithm algorithm = (RoutingService.Algorithm) algorithmBox.getSelectedItem();
        log("---- Sending packet " + source.getName() + " -> " + destination.getName() + " using " + algorithm + " ----");
        try {
            List<Device> path = routingService.findRoute(network, source, destination, algorithm);
            int latency = routingService.calculateLatency(network, path);
            Packet packet = new Packet(source, destination, "Hello from " + source.getName(), path);
            log("Route: " + pathToString(path) + "   (" + packet.getHopCount() + " hops, " + latency + " ms)");

            // Each device on the path handles the packet in its own way (polymorphism).
            canvas.animatePacket(packet,
                    index -> log("   " + path.get(index).handlePacket(packet)),
                    () -> {
                        log("Packet delivered successfully.");
                        saveLog(source, destination, pathToString(path), algorithm, packet.getHopCount(), latency, "DELIVERED");
                    });
        } catch (NoRouteException | DeviceOfflineException ex) {
            log("FAILED: " + ex.getMessage().replace("\n", " "));
            saveLog(source, destination, "-", algorithm, 0, 0, "FAILED");
            UiUtil.showError(this, ex.getMessage());
        }
    }

    private void saveLog(Device source, Device destination, String path, RoutingService.Algorithm algorithm,
                         int hops, int latency, String status) {
        try {
            packetLogDAO.save(new PacketLog(network.getId(), source.getId(), destination.getId(), path,
                    algorithm.name(), hops, latency, status));
        } catch (SQLException ex) {
            log("Could not save packet log: " + ex.getMessage());
        }
    }

    private String pathToString(List<Device> path) {
        return path.stream().map(Device::getName).collect(Collectors.joining(" -> "));
    }

    @Override
    public void dispose() {
        canvas.stopAnimation();
        super.dispose();
    }
}
