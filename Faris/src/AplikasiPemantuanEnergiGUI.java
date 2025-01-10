import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AplikasiPemantuanEnergiGUI {
    private static final String DB_URL = "jdbc:sqlite:energi_konsumsi.db";
    private JFrame frame;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel totalLabel, rataRataLabel, kategoriTerbesarLabel;

    public static void main(String[] args) {
        new AplikasiPemantuanEnergiGUI().initialize();
    }

    private void initialize() {
        cekKoneksiDatabase(); 
        buatDatabaseDanTabel(); 

        frame = new JFrame("Aplikasi Pemantauan Konsumsi Energi");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel dashboardPanel = new JPanel(new GridLayout(3, 1));
        totalLabel = new JLabel("Total Konsumsi: -", SwingConstants.CENTER);
        rataRataLabel = new JLabel("Rata-rata Konsumsi: -", SwingConstants.CENTER);
        kategoriTerbesarLabel = new JLabel("Kategori Konsumsi Terbesar: -", SwingConstants.CENTER);

        dashboardPanel.add(totalLabel);
        dashboardPanel.add(rataRataLabel);
        dashboardPanel.add(kategoriTerbesarLabel);
        dashboardPanel.setBorder(BorderFactory.createTitledBorder("Dashboard"));

        // Tombol
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Tambah Konsumsi Harian");
        JButton viewButton = new JButton("Lihat Konsumsi Bulanan");
        JButton tipsButton = new JButton("Tips Hemat Energi");

        buttonPanel.add(addButton);
        buttonPanel.add(viewButton);
        buttonPanel.add(tipsButton);

        tableModel = new DefaultTableModel(new String[]{"Tanggal", "Listrik (kWh)", "Air (m³)", "Gas (m³)"}, 0);
        table = new JTable(tableModel);

        mainPanel.add(dashboardPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);

        addButton.addActionListener(e -> tambahKonsumsiHarian());
        viewButton.addActionListener(e -> lihatKonsumsiBulanan());
        tipsButton.addActionListener(e -> tampilkanTipsHematEnergi());

        perbaruiDashboard();
    }

    private void cekKoneksiDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            System.out.println("Koneksi ke database berhasil.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal terhubung ke database: " + e.getMessage(), "Kesalahan", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private void buatDatabaseDanTabel() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            String buatTabelSQL = "CREATE TABLE IF NOT EXISTS konsumsi (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "tanggal TEXT NOT NULL, " +
                    "listrik REAL NOT NULL, " +
                    "air REAL NOT NULL, " +
                    "gas REAL NOT NULL);";

            stmt.execute(buatTabelSQL);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error membuat tabel: " + e.getMessage(), "Kesalahan", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void tambahKonsumsiHarian() {
        JPanel inputPanel = new JPanel(new GridLayout(4, 2));
        JTextField tanggalField = new JTextField();
        JTextField listrikField = new JTextField();
        JTextField airField = new JTextField();
        JTextField gasField = new JTextField();

        inputPanel.add(new JLabel("Tanggal (YYYY-MM-DD):"));
        inputPanel.add(tanggalField);
        inputPanel.add(new JLabel("Konsumsi Listrik (kWh):"));
        inputPanel.add(listrikField);
        inputPanel.add(new JLabel("Konsumsi Air (m³):"));
        inputPanel.add(airField);
        inputPanel.add(new JLabel("Konsumsi Gas (m³):"));
        inputPanel.add(gasField);

        int result = JOptionPane.showConfirmDialog(frame, inputPanel, "Tambah Konsumsi Harian", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            if (!tanggalField.getText().matches("\\d{4}-\\d{2}-\\d{2}")) {
                JOptionPane.showMessageDialog(frame, "Tanggal harus dalam format YYYY-MM-DD.", "Kesalahan Input", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO konsumsi (tanggal, listrik, air, gas) VALUES (?, ?, ?, ?);")) {

                pstmt.setString(1, tanggalField.getText());
                pstmt.setDouble(2, Double.parseDouble(listrikField.getText()));
                pstmt.setDouble(3, Double.parseDouble(airField.getText()));
                pstmt.setDouble(4, Double.parseDouble(gasField.getText()));
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(frame, "Konsumsi harian berhasil ditambahkan.");
                perbaruiDashboard();
            } catch (SQLException | NumberFormatException e) {
                JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage(), "Kesalahan", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void lihatKonsumsiBulanan() {
        String bulan = JOptionPane.showInputDialog(frame, "Masukkan bulan (YYYY-MM):", "Lihat Konsumsi Bulanan", JOptionPane.QUESTION_MESSAGE);
        if (bulan != null && bulan.matches("\\d{4}-\\d{2}")) {
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM konsumsi WHERE tanggal LIKE ?;")) {

                pstmt.setString(1, bulan + "%");
                ResultSet rs = pstmt.executeQuery();

                tableModel.setRowCount(0);
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                            rs.getString("tanggal"),
                            rs.getDouble("listrik"),
                            rs.getDouble("air"),
                            rs.getDouble("gas")
                    });
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage(), "Kesalahan", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Bulan harus dalam format YYYY-MM.", "Kesalahan Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void tampilkanTipsHematEnergi() {
        String tips = "Tips Hemat Energi:\n"
                + "1. Matikan lampu saat tidak digunakan.\n"
                + "2. Gunakan peralatan hemat energi.\n"
                + "3. Segera perbaiki kebocoran air.\n"
                + "4. Atur suhu AC ke tingkat hemat energi.\n"
                + "5. Cabut perangkat listrik saat tidak digunakan.";

        JOptionPane.showMessageDialog(frame, tips, "Tips Hemat Energi", JOptionPane.INFORMATION_MESSAGE);
    }

    private void perbaruiDashboard() {
        String querySQL = "SELECT SUM(listrik) AS totalListrik, SUM(air) AS totalAir, SUM(gas) AS totalGas, " +
                "AVG(listrik + air + gas) AS rataRata, " +
                "(CASE WHEN SUM(listrik) >= SUM(air) AND SUM(listrik) >= SUM(gas) THEN 'Listrik' " +
                "WHEN SUM(air) >= SUM(listrik) AND SUM(air) >= SUM(gas) THEN 'Air' " +
                "ELSE 'Gas' END) AS kategoriTerbesar " +
                "FROM konsumsi;";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(querySQL)) {

            if (rs.next()) {
                totalLabel.setText(String.format("Total Konsumsi: %.2f kWh/m³", rs.getDouble("totalListrik") + rs.getDouble("totalAir") + rs.getDouble("totalGas")));
                rataRataLabel.setText(String.format("Rata-rata Konsumsi: %.2f kWh/m³", rs.getDouble("rataRata")));
                kategoriTerbesarLabel.setText(String.format("Kategori Konsumsi Terbesar: %s", rs.getString("kategoriTerbesar")));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage(), "Kesalahan", JOptionPane.ERROR_MESSAGE);
        }
    }
}
