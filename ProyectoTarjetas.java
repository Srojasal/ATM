import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

public class ProyectoTarjetas {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setSystemLookAndFeel();
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private enum Role {
        CLIENT,
        ADMIN
    }

    private enum Currency {
        CRC,
        USD
    }

    private enum TransactionType {
        COMPRA,
        PAGO,
        RETIRO
    }

    private static final class Transaction {
        private final LocalDateTime timestamp;
        private final TransactionType type;
        private final Currency currency;
        private final BigDecimal amount;
        private final BigDecimal amountInCrc;
        private final String detail;

        private Transaction(TransactionType type, Currency currency, BigDecimal amount, BigDecimal amountInCrc, String detail) {
            this.timestamp = LocalDateTime.now();
            this.type = type;
            this.currency = currency;
            this.amount = amount.setScale(2, RoundingMode.HALF_UP);
            this.amountInCrc = amountInCrc.setScale(2, RoundingMode.HALF_UP);
            this.detail = detail;
        }
    }

    private static final class User {
        private final String id;
        private String password;
        private final Role role;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private int age;
        private String sex;
        private String address;
        private boolean active;
        private BigDecimal balanceCrc;
        private final List<Transaction> transactions;

        private User(
                String id,
                String password,
                Role role,
                String firstName,
                String lastName,
                String email,
                String phone,
                int age,
                String sex,
                String address,
                BigDecimal balanceCrc
        ) {
            this.id = id;
            this.password = password;
            this.role = role;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.phone = phone;
            this.age = age;
            this.sex = sex;
            this.address = address;
            this.balanceCrc = balanceCrc.setScale(2, RoundingMode.HALF_UP);
            this.active = true;
            this.transactions = new ArrayList<>();
        }

        private String getFullName() {
            return firstName + " " + lastName;
        }
    }

    private static final class BankSystem {
        private static final BigDecimal DEFAULT_USD_RATE = new BigDecimal("520.00");

        private final Map<String, User> usersById = new LinkedHashMap<>();
        private BigDecimal usdToCrcRate = DEFAULT_USD_RATE;

        private BankSystem() {
            seedData();
        }

        private void seedData() {
            User admin = new User(
                    "admin",
                    "admin123",
                    Role.ADMIN,
                    "Admin",
                    "Principal",
                    "admin@proyectotarjetas.local",
                    "0000-0000",
                    30,
                    "N/A",
                    "Oficina central",
                    BigDecimal.ZERO
            );
            usersById.put(admin.id, admin);

            User client = new User(
                    "1001",
                    "1234",
                    Role.CLIENT,
                    "Sebastian",
                    "Rojas",
                    "sebastian@correo.com",
                    "8888-1111",
                    25,
                    "Masculino",
                    "San Jose, Costa Rica",
                    new BigDecimal("350000.00")
            );
            client.transactions.add(new Transaction(
                    TransactionType.PAGO,
                    Currency.CRC,
                    new BigDecimal("350000.00"),
                    new BigDecimal("350000.00"),
                    "Saldo inicial de demostracion"
            ));
            usersById.put(client.id, client);
        }

        private synchronized User authenticate(String id, String password, Role expectedRole) {
            User user = usersById.get(normalizeId(id));
            if (user == null || user.role != expectedRole) {
                return null;
            }
            if (!user.active || !user.password.equals(password)) {
                return null;
            }
            return user;
        }

        private synchronized List<User> getClients() {
            List<User> clients = new ArrayList<>();
            for (User user : usersById.values()) {
                if (user.role == Role.CLIENT) {
                    clients.add(user);
                }
            }
            return clients;
        }

        private synchronized BigDecimal getUsdToCrcRate() {
            return usdToCrcRate;
        }

        private synchronized void updateUsdRate(BigDecimal newRate) {
            usdToCrcRate = newRate.setScale(2, RoundingMode.HALF_UP);
        }

        private synchronized void createClient(
                String id,
                String password,
                String firstName,
                String lastName,
                String email,
                String phone,
                int age,
                String sex,
                String address,
                BigDecimal initialBalanceCrc
        ) {
            String normalizedId = normalizeId(id);
            if (normalizedId.isEmpty()) {
                throw new IllegalArgumentException("El ID del cliente es obligatorio.");
            }
            if (usersById.containsKey(normalizedId)) {
                throw new IllegalArgumentException("Ya existe un cliente con ese ID.");
            }
            if (password == null || password.trim().isEmpty()) {
                throw new IllegalArgumentException("La clave del cliente es obligatoria.");
            }

            User client = new User(
                    normalizedId,
                    password.trim(),
                    Role.CLIENT,
                    required(firstName, "El nombre es obligatorio."),
                    required(lastName, "El apellido es obligatorio."),
                    required(email, "El correo es obligatorio."),
                    required(phone, "El telefono es obligatorio."),
                    age,
                    required(sex, "El sexo es obligatorio."),
                    required(address, "La direccion es obligatoria."),
                    initialBalanceCrc
            );
            client.transactions.add(new Transaction(
                    TransactionType.PAGO,
                    Currency.CRC,
                    initialBalanceCrc,
                    initialBalanceCrc,
                    "Saldo inicial asignado por administracion"
            ));
            usersById.put(client.id, client);
        }

        private synchronized void setClientActive(String id, boolean active) {
            User user = usersById.get(normalizeId(id));
            if (user == null || user.role != Role.CLIENT) {
                throw new IllegalArgumentException("No se encontro el cliente indicado.");
            }
            user.active = active;
        }

        private synchronized void registerPurchase(User user, BigDecimal amount, Currency currency, String detail) {
            BigDecimal amountInCrc = convertToCrc(amount, currency);
            ensureEnoughBalance(user, amountInCrc);
            user.balanceCrc = user.balanceCrc.subtract(amountInCrc);
            user.transactions.add(new Transaction(TransactionType.COMPRA, currency, amount, amountInCrc, detail));
        }

        private synchronized void registerPayment(User user, BigDecimal amount, Currency currency, String detail) {
            BigDecimal amountInCrc = convertToCrc(amount, currency);
            user.balanceCrc = user.balanceCrc.add(amountInCrc);
            user.transactions.add(new Transaction(TransactionType.PAGO, currency, amount, amountInCrc, detail));
        }

        private synchronized void registerWithdrawal(User user, BigDecimal amount, Currency currency, String pin) {
            if (!user.password.equals(pin)) {
                throw new IllegalArgumentException("PIN incorrecto.");
            }
            BigDecimal amountInCrc = convertToCrc(amount, currency);
            ensureEnoughBalance(user, amountInCrc);
            user.balanceCrc = user.balanceCrc.subtract(amountInCrc);
            user.transactions.add(new Transaction(
                    TransactionType.RETIRO,
                    currency,
                    amount,
                    amountInCrc,
                    "Retiro en cajero"
            ));
        }

        private synchronized BigDecimal convertToCrc(BigDecimal amount, Currency currency) {
            if (currency == Currency.CRC) {
                return amount.setScale(2, RoundingMode.HALF_UP);
            }
            return amount.multiply(usdToCrcRate).setScale(2, RoundingMode.HALF_UP);
        }

        private synchronized BigDecimal getBalanceInCrc(User user) {
            return user.balanceCrc.setScale(2, RoundingMode.HALF_UP);
        }

        private synchronized BigDecimal getBalanceInUsd(User user) {
            if (usdToCrcRate.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            return user.balanceCrc.divide(usdToCrcRate, 2, RoundingMode.HALF_UP);
        }

        private synchronized List<Transaction> getTransactions(User user) {
            return new ArrayList<>(user.transactions);
        }

        private void ensureEnoughBalance(User user, BigDecimal amountInCrc) {
            if (user.balanceCrc.compareTo(amountInCrc) < 0) {
                throw new IllegalArgumentException("Saldo insuficiente para completar la operacion.");
            }
        }

        private static String normalizeId(String value) {
            return value == null ? "" : value.trim();
        }

        private static String required(String value, String message) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(message);
            }
            return value.trim();
        }
    }

    private static final class MainFrame extends JFrame {
        private static final String CARD_LOGIN = "login";
        private static final String CARD_CLIENT = "client";
        private static final String CARD_ADMIN = "admin";

        private final BankSystem bankSystem;
        private final CardLayout cardLayout;
        private final JPanel contentPanel;
        private final LoginPanel loginPanel;
        private final ClientPanel clientPanel;
        private final AdminPanel adminPanel;

        private MainFrame() {
            super("Sistema de Tarjetas");
            this.bankSystem = new BankSystem();
            this.cardLayout = new CardLayout();
            this.contentPanel = new JPanel(cardLayout);
            this.loginPanel = new LoginPanel(this, bankSystem);
            this.clientPanel = new ClientPanel(this, bankSystem);
            this.adminPanel = new AdminPanel(this, bankSystem);

            contentPanel.add(loginPanel, CARD_LOGIN);
            contentPanel.add(clientPanel, CARD_CLIENT);
            contentPanel.add(adminPanel, CARD_ADMIN);

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setMinimumSize(new Dimension(980, 640));
            setLocationRelativeTo(null);
            setContentPane(contentPanel);
            showLogin();
        }

        private void showLogin() {
            loginPanel.reset();
            cardLayout.show(contentPanel, CARD_LOGIN);
        }

        private void loginClient(User user) {
            clientPanel.setCurrentUser(user);
            cardLayout.show(contentPanel, CARD_CLIENT);
        }

        private void loginAdmin(User user) {
            adminPanel.setCurrentUser(user);
            cardLayout.show(contentPanel, CARD_ADMIN);
        }
    }

    private static final class LoginPanel extends JPanel {
        private final MainFrame mainFrame;
        private final BankSystem bankSystem;
        private final JTextField idField;
        private final JPasswordField passwordField;
        private final JComboBox<Role> roleCombo;

        private LoginPanel(MainFrame mainFrame, BankSystem bankSystem) {
            this.mainFrame = mainFrame;
            this.bankSystem = bankSystem;
            this.idField = new JTextField(16);
            this.passwordField = new JPasswordField(16);
            this.roleCombo = new JComboBox<>(Role.values());

            setLayout(new GridBagLayout());
            JPanel card = new JPanel(new GridBagLayout());
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(24, 24, 24, 24),
                    BorderFactory.createTitledBorder("Acceso al sistema")
            ));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel title = new JLabel("Proyecto Tarjetas");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
            JLabel subtitle = new JLabel("Version profesional, autocontenida y lista para demostrar.");
            JLabel demoInfo = new JLabel("Demo cliente: 1001 / 1234    Demo admin: admin / admin123");

            JButton loginButton = new JButton("Ingresar");
            loginButton.addActionListener(e -> login());

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            card.add(title, gbc);

            gbc.gridy++;
            card.add(subtitle, gbc);

            gbc.gridy++;
            card.add(demoInfo, gbc);

            gbc.gridwidth = 1;
            gbc.gridy++;
            card.add(new JLabel("Rol:"), gbc);
            gbc.gridx = 1;
            card.add(roleCombo, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            card.add(new JLabel("ID:"), gbc);
            gbc.gridx = 1;
            card.add(idField, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            card.add(new JLabel("Clave / PIN:"), gbc);
            gbc.gridx = 1;
            card.add(passwordField, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            card.add(loginButton, gbc);

            add(card);
        }

        private void reset() {
            idField.setText("");
            passwordField.setText("");
            roleCombo.setSelectedItem(Role.CLIENT);
        }

        private void login() {
            String id = idField.getText().trim();
            String password = new String(passwordField.getPassword());
            Role role = (Role) roleCombo.getSelectedItem();

            if (id.isEmpty() || password.isEmpty()) {
                showError("Debes ingresar ID y clave.");
                return;
            }

            User user = bankSystem.authenticate(id, password, role);
            if (user == null) {
                showError("Credenciales invalidas, usuario inactivo o rol incorrecto.");
                return;
            }

            if (role == Role.CLIENT) {
                mainFrame.loginClient(user);
            } else {
                mainFrame.loginAdmin(user);
            }
        }

        private void showError(String message) {
            JOptionPane.showMessageDialog(this, message, "Acceso denegado", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static final class ClientPanel extends JPanel {
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        private final MainFrame mainFrame;
        private final BankSystem bankSystem;
        private final JTextArea profileArea;
        private final DefaultTableModel transactionModel;
        private User currentUser;

        private ClientPanel(MainFrame mainFrame, BankSystem bankSystem) {
            this.mainFrame = mainFrame;
            this.bankSystem = bankSystem;
            this.profileArea = new JTextArea();
            this.transactionModel = new DefaultTableModel(
                    new Object[]{"Fecha", "Tipo", "Moneda", "Monto", "Monto CRC", "Detalle"}, 0
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            setLayout(new BorderLayout(12, 12));
            setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

            JLabel title = new JLabel("Portal del Cliente");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.add(title, BorderLayout.WEST);

            JButton logoutButton = new JButton("Cerrar sesion");
            logoutButton.addActionListener(e -> logout());
            topPanel.add(logoutButton, BorderLayout.EAST);
            add(topPanel, BorderLayout.NORTH);

            profileArea.setEditable(false);
            profileArea.setLineWrap(true);
            profileArea.setWrapStyleWord(true);
            profileArea.setBorder(BorderFactory.createTitledBorder("Perfil y saldos"));

            JTable transactionTable = new JTable(transactionModel);
            JScrollPane transactionScroll = new JScrollPane(transactionTable);
            transactionScroll.setBorder(BorderFactory.createTitledBorder("Historial de movimientos"));

            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            JButton purchaseButton = new JButton("Registrar compra");
            JButton paymentButton = new JButton("Registrar pago");
            JButton withdrawalButton = new JButton("Retirar efectivo");
            JButton refreshButton = new JButton("Actualizar");

            purchaseButton.addActionListener(e -> registerPurchase());
            paymentButton.addActionListener(e -> registerPayment());
            withdrawalButton.addActionListener(e -> registerWithdrawal());
            refreshButton.addActionListener(e -> refreshView());

            actionPanel.add(purchaseButton);
            actionPanel.add(paymentButton);
            actionPanel.add(withdrawalButton);
            actionPanel.add(refreshButton);

            JPanel centerPanel = new JPanel(new BorderLayout(12, 12));
            centerPanel.add(new JScrollPane(profileArea), BorderLayout.NORTH);
            centerPanel.add(actionPanel, BorderLayout.CENTER);
            centerPanel.add(transactionScroll, BorderLayout.SOUTH);

            add(centerPanel, BorderLayout.CENTER);
        }

        private void setCurrentUser(User user) {
            this.currentUser = user;
            refreshView();
        }

        private void refreshView() {
            if (currentUser == null) {
                return;
            }

            BigDecimal balanceCrc = bankSystem.getBalanceInCrc(currentUser);
            BigDecimal balanceUsd = bankSystem.getBalanceInUsd(currentUser);
            BigDecimal rate = bankSystem.getUsdToCrcRate();

            String profileText =
                    "Cliente: " + currentUser.getFullName() + "\n" +
                    "ID: " + currentUser.id + "\n" +
                    "Correo: " + currentUser.email + "\n" +
                    "Telefono: " + currentUser.phone + "\n" +
                    "Edad: " + currentUser.age + "\n" +
                    "Sexo: " + currentUser.sex + "\n" +
                    "Direccion: " + currentUser.address + "\n" +
                    "Estado: " + (currentUser.active ? "Activo" : "Inactivo") + "\n\n" +
                    "Saldo disponible CRC: " + formatMoney(balanceCrc, Currency.CRC) + "\n" +
                    "Saldo equivalente USD: " + formatMoney(balanceUsd, Currency.USD) + "\n" +
                    "Tipo de cambio actual: 1 USD = " + formatMoney(rate, Currency.CRC);

            profileArea.setText(profileText);

            transactionModel.setRowCount(0);
            for (Transaction transaction : bankSystem.getTransactions(currentUser)) {
                transactionModel.addRow(new Object[]{
                        transaction.timestamp.format(DATE_FORMATTER),
                        transaction.type.name(),
                        transaction.currency.name(),
                        formatMoney(transaction.amount, transaction.currency),
                        formatMoney(transaction.amountInCrc, Currency.CRC),
                        transaction.detail
                });
            }
        }

        private void registerPurchase() {
            TransactionRequest request = TransactionDialog.show(this, "Registrar compra", true);
            if (request == null) {
                return;
            }

            try {
                bankSystem.registerPurchase(currentUser, request.amount, request.currency, request.detail);
                refreshView();
                showInfo("Compra registrada correctamente.");
            } catch (IllegalArgumentException ex) {
                showError(ex.getMessage());
            }
        }

        private void registerPayment() {
            TransactionRequest request = TransactionDialog.show(this, "Registrar pago", true);
            if (request == null) {
                return;
            }

            try {
                bankSystem.registerPayment(currentUser, request.amount, request.currency, request.detail);
                refreshView();
                showInfo("Pago registrado correctamente.");
            } catch (IllegalArgumentException ex) {
                showError(ex.getMessage());
            }
        }

        private void registerWithdrawal() {
            TransactionRequest request = TransactionDialog.show(this, "Retirar efectivo", false);
            if (request == null) {
                return;
            }

            String pin = JOptionPane.showInputDialog(this, "Ingresa tu PIN para confirmar el retiro:");
            if (pin == null) {
                return;
            }

            try {
                bankSystem.registerWithdrawal(currentUser, request.amount, request.currency, pin.trim());
                refreshView();
                showInfo("Retiro registrado correctamente.");
            } catch (IllegalArgumentException ex) {
                showError(ex.getMessage());
            }
        }

        private void logout() {
            currentUser = null;
            mainFrame.showLogin();
        }

        private void showInfo(String message) {
            JOptionPane.showMessageDialog(this, message, "Operacion exitosa", JOptionPane.INFORMATION_MESSAGE);
        }

        private void showError(String message) {
            JOptionPane.showMessageDialog(this, message, "Operacion no realizada", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static final class AdminPanel extends JPanel {
        private final MainFrame mainFrame;
        private final BankSystem bankSystem;
        private final JLabel sessionLabel;
        private final JLabel rateLabel;
        private final DefaultTableModel clientModel;
        private User currentUser;

        private AdminPanel(MainFrame mainFrame, BankSystem bankSystem) {
            this.mainFrame = mainFrame;
            this.bankSystem = bankSystem;
            this.sessionLabel = new JLabel();
            this.rateLabel = new JLabel();
            this.clientModel = new DefaultTableModel(
                    new Object[]{"ID", "Nombre", "Correo", "Telefono", "Estado", "Saldo CRC", "Saldo USD"}, 0
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            setLayout(new BorderLayout(12, 12));
            setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

            JLabel title = new JLabel("Panel de Administracion");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

            JButton logoutButton = new JButton("Cerrar sesion");
            logoutButton.addActionListener(e -> logout());

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.add(title, BorderLayout.WEST);
            topPanel.add(logoutButton, BorderLayout.EAST);
            add(topPanel, BorderLayout.NORTH);

            JPanel summaryPanel = new JPanel(new GridBagLayout());
            summaryPanel.setBorder(BorderFactory.createTitledBorder("Resumen"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 6, 6);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.gridx = 0;
            gbc.gridy = 0;
            summaryPanel.add(sessionLabel, gbc);
            gbc.gridy++;
            summaryPanel.add(rateLabel, gbc);

            JTable clientTable = new JTable(clientModel);
            JScrollPane clientScroll = new JScrollPane(clientTable);
            clientScroll.setBorder(BorderFactory.createTitledBorder("Clientes"));

            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            JButton createClientButton = new JButton("Nuevo cliente");
            JButton deactivateButton = new JButton("Inactivar cliente");
            JButton activateButton = new JButton("Reactivar cliente");
            JButton changeRateButton = new JButton("Cambiar tipo de cambio");
            JButton refreshButton = new JButton("Actualizar");

            createClientButton.addActionListener(e -> createClient());
            deactivateButton.addActionListener(e -> toggleClientStatus(false));
            activateButton.addActionListener(e -> toggleClientStatus(true));
            changeRateButton.addActionListener(e -> updateExchangeRate());
            refreshButton.addActionListener(e -> refreshView());

            actionPanel.add(createClientButton);
            actionPanel.add(deactivateButton);
            actionPanel.add(activateButton);
            actionPanel.add(changeRateButton);
            actionPanel.add(refreshButton);

            add(summaryPanel, BorderLayout.WEST);
            add(clientScroll, BorderLayout.CENTER);
            add(actionPanel, BorderLayout.SOUTH);
        }

        private void setCurrentUser(User user) {
            this.currentUser = user;
            refreshView();
        }

        private void refreshView() {
            if (currentUser == null) {
                return;
            }

            sessionLabel.setText("Sesion actual: " + currentUser.getFullName() + " (" + currentUser.id + ")");
            rateLabel.setText("Tipo de cambio vigente: 1 USD = " + formatMoney(bankSystem.getUsdToCrcRate(), Currency.CRC));

            clientModel.setRowCount(0);
            for (User client : bankSystem.getClients()) {
                clientModel.addRow(new Object[]{
                        client.id,
                        client.getFullName(),
                        client.email,
                        client.phone,
                        client.active ? "Activo" : "Inactivo",
                        formatMoney(bankSystem.getBalanceInCrc(client), Currency.CRC),
                        formatMoney(bankSystem.getBalanceInUsd(client), Currency.USD)
                });
            }
        }

        private void createClient() {
            JTextField idField = new JTextField();
            JPasswordField passwordField = new JPasswordField();
            JTextField nameField = new JTextField();
            JTextField lastNameField = new JTextField();
            JTextField emailField = new JTextField();
            JTextField phoneField = new JTextField();
            JTextField ageField = new JTextField();
            JTextField sexField = new JTextField();
            JTextField addressField = new JTextField();
            JTextField initialBalanceField = new JTextField("0.00");

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            addField(panel, gbc, 0, "ID cliente:", idField);
            addField(panel, gbc, 1, "Clave:", passwordField);
            addField(panel, gbc, 2, "Nombre:", nameField);
            addField(panel, gbc, 3, "Apellido:", lastNameField);
            addField(panel, gbc, 4, "Correo:", emailField);
            addField(panel, gbc, 5, "Telefono:", phoneField);
            addField(panel, gbc, 6, "Edad:", ageField);
            addField(panel, gbc, 7, "Sexo:", sexField);
            addField(panel, gbc, 8, "Direccion:", addressField);
            addField(panel, gbc, 9, "Saldo inicial CRC:", initialBalanceField);

            int option = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "Registrar nuevo cliente",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (option != JOptionPane.OK_OPTION) {
                return;
            }

            try {
                int age = parseInt(ageField.getText().trim(), "La edad debe ser un numero entero.");
                BigDecimal initialBalance = parsePositiveAmountAllowZero(initialBalanceField.getText().trim());
                bankSystem.createClient(
                        idField.getText(),
                        new String(passwordField.getPassword()),
                        nameField.getText(),
                        lastNameField.getText(),
                        emailField.getText(),
                        phoneField.getText(),
                        age,
                        sexField.getText(),
                        addressField.getText(),
                        initialBalance
                );
                refreshView();
                showInfo("Cliente creado correctamente.");
            } catch (IllegalArgumentException ex) {
                showError(ex.getMessage());
            }
        }

        private void toggleClientStatus(boolean active) {
            String action = active ? "reactivar" : "inactivar";
            String clientId = JOptionPane.showInputDialog(this, "Ingresa el ID del cliente a " + action + ":");
            if (clientId == null) {
                return;
            }

            try {
                bankSystem.setClientActive(clientId, active);
                refreshView();
                showInfo("Estado del cliente actualizado.");
            } catch (IllegalArgumentException ex) {
                showError(ex.getMessage());
            }
        }

        private void updateExchangeRate() {
            String newRateText = JOptionPane.showInputDialog(
                    this,
                    "Nuevo valor de 1 USD en CRC:",
                    bankSystem.getUsdToCrcRate().toPlainString()
            );
            if (newRateText == null) {
                return;
            }

            try {
                BigDecimal newRate = parsePositiveAmount(newRateText.trim());
                bankSystem.updateUsdRate(newRate);
                refreshView();
                showInfo("Tipo de cambio actualizado correctamente.");
            } catch (IllegalArgumentException ex) {
                showError(ex.getMessage());
            }
        }

        private void logout() {
            currentUser = null;
            mainFrame.showLogin();
        }

        private void showInfo(String message) {
            JOptionPane.showMessageDialog(this, message, "Administracion", JOptionPane.INFORMATION_MESSAGE);
        }

        private void showError(String message) {
            JOptionPane.showMessageDialog(this, message, "Administracion", JOptionPane.ERROR_MESSAGE);
        }

        private void addField(JPanel panel, GridBagConstraints gbc, int row, String label, java.awt.Component field) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            panel.add(new JLabel(label), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(field, gbc);
        }
    }

    private static final class TransactionRequest {
        private final BigDecimal amount;
        private final Currency currency;
        private final String detail;

        private TransactionRequest(BigDecimal amount, Currency currency, String detail) {
            this.amount = amount;
            this.currency = currency;
            this.detail = detail;
        }
    }

    private static final class TransactionDialog {
        private static TransactionRequest show(JPanel parent, String title, boolean allowDetail) {
            JTextField amountField = new JTextField();
            JComboBox<Currency> currencyCombo = new JComboBox<>(Currency.values());
            JTextField detailField = new JTextField();

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(new JLabel("Monto:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(amountField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0;
            panel.add(new JLabel("Moneda:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add(currencyCombo, gbc);

            if (allowDetail) {
                gbc.gridx = 0;
                gbc.gridy = 2;
                gbc.weightx = 0;
                panel.add(new JLabel("Detalle:"), gbc);
                gbc.gridx = 1;
                gbc.weightx = 1;
                panel.add(detailField, gbc);
            }

            int option = JOptionPane.showConfirmDialog(
                    parent,
                    panel,
                    title,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (option != JOptionPane.OK_OPTION) {
                return null;
            }

            BigDecimal amount = parsePositiveAmount(amountField.getText().trim());
            Currency currency = (Currency) currencyCombo.getSelectedItem();
            String detail = allowDetail
                    ? (detailField.getText().trim().isEmpty() ? "Sin detalle adicional" : detailField.getText().trim())
                    : "Retiro de efectivo";

            return new TransactionRequest(amount, currency, detail);
        }
    }

    private static int parseInt(String value, String message) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(message);
        }
    }

    private static BigDecimal parsePositiveAmount(String value) {
        try {
            BigDecimal amount = new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El monto debe ser mayor a cero.");
            }
            return amount;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Debes ingresar un monto numerico valido.");
        }
    }

    private static BigDecimal parsePositiveAmountAllowZero(String value) {
        try {
            BigDecimal amount = new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El saldo inicial no puede ser negativo.");
            }
            return amount;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Debes ingresar un monto numerico valido.");
        }
    }

    private static String formatMoney(BigDecimal amount, Currency currency) {
        String symbol = currency == Currency.CRC ? "CRC " : "USD ";
        return symbol + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
