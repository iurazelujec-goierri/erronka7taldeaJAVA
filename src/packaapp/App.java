package packaapp;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import packconexion.Conexion;
import packtaulak.VentanaTabla;

public class App {
    private static Connection conn;
    private static String usuarioActual;
    private static String rolActual;
    private static int langileId;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> mostrarLogin());
    }
    
    private static void mostrarLogin() {
        JFrame frame = new JFrame("Login - Erronka 2");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 250);
        frame.setLocationRelativeTo(null);
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel lblTitulo = new JLabel("Erronka 2 - Login", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(lblTitulo, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(new JLabel("Gmail:"), gbc);
        
        JTextField txtEmail = new JTextField(20);
        gbc.gridx = 1;
        panel.add(txtEmail, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Pasahitza:"), gbc);
        
        JPasswordField txtPassword = new JPasswordField(20);
        gbc.gridx = 1;
        panel.add(txtPassword, gbc);
        
        JButton btnLogin = new JButton("Sartu");
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(btnLogin, gbc);
        
        btnLogin.addActionListener(e -> {
            String email = txtEmail.getText().trim();
            String password = new String(txtPassword.getPassword());
            
            if (validarLogin(email, password)) {
                frame.dispose();
                mostrarMenuPrincipal();
            } else {
                JOptionPane.showMessageDialog(frame, "Email edo pasahitza okerra", "Errorea", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        frame.add(panel);
        frame.setVisible(true);
    }
    
    private static boolean validarLogin(String email, String password) {
        try {
            conn = new Conexion().conectar();
            String sql = "SELECT e.langile_id, r.izena as rol FROM erabiltzaile e " +
                        "JOIN rolak r ON e.rol_id = r.id " +
                        "WHERE e.gmail = ? AND e.pass = ?";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                langileId = rs.getInt("langile_id");
                rolActual = rs.getString("rol");
                
                if (langileId > 0) {
                    String sqlNombre = "SELECT izena FROM langile WHERE id = ?";
                    PreparedStatement psNombre = conn.prepareStatement(sqlNombre);
                    psNombre.setInt(1, langileId);
                    ResultSet rsNombre = psNombre.executeQuery();
                    
                    if (rsNombre.next()) {
                        usuarioActual = rsNombre.getString("izena");
                    }
                } else {
                    usuarioActual = "Erabiltzailea";
                }
                
                JOptionPane.showMessageDialog(null, "Ongi etorri, " + usuarioActual + "!");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private static void mostrarMenuPrincipal() {
        JFrame frame = new JFrame("Erronka 2 - Menu Nagusia");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        
        JPanel panelTop = new JPanel();
        panelTop.add(new JLabel("Kaixo, " + usuarioActual + " (" + rolActual + ")"));
        
        String[] tablas = obtenerTablasDisponibles();
        JList<String> listTablas = new JList<>(tablas);
        listTablas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listTablas.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JButton btnAbrir = new JButton("Taula ireki");
        btnAbrir.addActionListener(e -> {
            String tablaSeleccionada = listTablas.getSelectedValue();
            if (tablaSeleccionada != null) {
                if (tieneAcceso(tablaSeleccionada)) {
                    new VentanaTabla(conn, tablaSeleccionada, rolActual);
                } else {
                    JOptionPane.showMessageDialog(frame, "Ez duzu taula honetarako sarbiderik", "Errorea", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        JPanel panelBotones = new JPanel();
        panelBotones.add(btnAbrir);
        
        frame.setLayout(new BorderLayout());
        frame.add(panelTop, BorderLayout.NORTH);
        frame.add(new JScrollPane(listTablas), BorderLayout.CENTER);
        frame.add(panelBotones, BorderLayout.SOUTH);
        
        frame.setVisible(true);
    }
    
    private static String[] obtenerTablasDisponibles() {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables("erronka2", null, "%", new String[]{"TABLE"});
            
            java.util.List<String> tablas = new java.util.ArrayList<>();
            while (rs.next()) {
                String tabla = rs.getString("TABLE_NAME");
                tablas.add(tabla);
            }
            return tablas.toArray(new String[0]);
        } catch (SQLException e) {
            e.printStackTrace();
            return new String[0];
        }
    }
    
    private static boolean tieneAcceso(String tabla) {
    	
        if (rolActual.equalsIgnoreCase("zuzendaria")) {
            return true;
        }
        
        if (rolActual.equalsIgnoreCase("biltegizaina")) {
            return tabla.matches("(?i)(produktuak|hornitzaile|konponketak)");
        }
        
        if (rolActual.equalsIgnoreCase("saltzailea")) {
            return tabla.matches("(?i)(bezero|faktura|faktura_lerroak|karrito|karrito_produktuak)");
        }
        
        if (rolActual.equalsIgnoreCase("teknikaria")) {
            return tabla.matches("(?i)(produktuak|konponketak|bezero)");
        }
        
        return false;
    }
}
