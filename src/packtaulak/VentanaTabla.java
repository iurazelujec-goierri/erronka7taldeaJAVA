package packtaulak;

import packpdf.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.*;

public class VentanaTabla extends JFrame {
    private Connection conn;
    private String nombreTabla;
    private String rolActual;
    private JTable table;
    private DefaultTableModel model;
    private java.util.List<String> columnas;
    private JPanel panelEdicion;
    private Map<String, JTextField> camposEdicion;
    private Object[] filaActual;
    
    public VentanaTabla(Connection conn, String nombreTabla, String rolActual) {
        this.conn = conn;
        this.nombreTabla = nombreTabla;
        this.rolActual = rolActual;
        this.camposEdicion = new HashMap<>();
        
        setTitle(nombreTabla + " - Taula");
        setSize(1100, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        cargarDatos();
        
        JScrollPane scrollPane = new JScrollPane(table);
        
        panelEdicion = new JPanel();
        panelEdicion.setLayout(new BoxLayout(panelEdicion, BoxLayout.Y_AXIS));
        panelEdicion.setPreferredSize(new Dimension(300, 0));
        panelEdicion.setVisible(false);
        JScrollPane scrollEdicion = new JScrollPane(panelEdicion);
        scrollEdicion.setPreferredSize(new Dimension(300, 0));
        
        JPanel panelBotones = new JPanel();
        JButton btnRefrescar = new JButton("Freskatu");
        JButton btnNuevo = new JButton("Berria");
        
        JButton btnPDF = null;
        if (nombreTabla.equalsIgnoreCase("faktura")) {
            btnPDF = new JButton("Faktura inprimatu (PDF)");
            btnPDF.addActionListener(e -> imprimirFacturaPDF());
        }
        
        btnRefrescar.addActionListener(e -> refrescarDatos());
        btnNuevo.addActionListener(e -> mostrarFormularioNuevo());
        
        panelBotones.add(btnRefrescar);
        panelBotones.add(btnNuevo);
        if (btnPDF != null) {
            panelBotones.add(btnPDF);
        }
        
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    mostrarPanelEdicion(row);
                }
            }
        });
        
        add(scrollPane, BorderLayout.CENTER);
        add(scrollEdicion, BorderLayout.EAST);
        add(panelBotones, BorderLayout.SOUTH);
        
        setVisible(true);
    }
    
    private void cargarDatos() {
        try {
            String sql = "SELECT * FROM " + nombreTabla;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            columnas = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnas.add(metaData.getColumnName(i));
            }
            
            model = new DefaultTableModel(columnas.toArray(), 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; 
                }
            };
            
            while (rs.next()) {
                Object[] fila = new Object[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    fila[i] = rs.getObject(i + 1);
                }
                model.addRow(fila);
            }
            
            table = new JTable(model);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Errorea datuak kargatzean: " + e.getMessage(), "Errorea", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void refrescarDatos() {
        model.setRowCount(0);
        try {
            String sql = "SELECT * FROM " + nombreTabla;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Object[] fila = new Object[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    fila[i] = rs.getObject(i + 1);
                }
                model.addRow(fila);
            }
            panelEdicion.setVisible(false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void mostrarPanelEdicion(int row) {
        panelEdicion.removeAll();
        panelEdicion.setVisible(true);
        panelEdicion.getParent().setPreferredSize(new Dimension(300, 600));
        camposEdicion.clear();
        
        filaActual = new Object[columnas.size()];
        for (int i = 0; i < columnas.size(); i++) {
            filaActual[i] = model.getValueAt(row, i);
        }
        
        JLabel lblTitulo = new JLabel("Editatu");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 16));
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelEdicion.add(lblTitulo);
        panelEdicion.add(Box.createRigidArea(new Dimension(0, 10)));
        
        for (int i = 0; i < columnas.size(); i++) {
            String columna = columnas.get(i);
            Object valor = model.getValueAt(row, i);
            
            JPanel panelCampo = new JPanel();
            panelCampo.setLayout(new BorderLayout(5, 5));
            panelCampo.setMaximumSize(new Dimension(280, 60));
            
            JLabel lbl = new JLabel(columna + ":");
            JTextField txt = new JTextField(valor != null ? valor.toString() : "");
            
            if (i == 0) {
                txt.setEditable(false);
                txt.setBackground(Color.LIGHT_GRAY);
            }
            
            panelCampo.add(lbl, BorderLayout.NORTH);
            panelCampo.add(txt, BorderLayout.CENTER);
            
            camposEdicion.put(columna, txt);
            panelEdicion.add(panelCampo);
        }
        
        panelEdicion.add(Box.createRigidArea(new Dimension(0, 10)));
        
        JButton btnGuardar = new JButton("Gorde");
        JButton btnEliminar = new JButton("Ezabatu");
        JButton btnCancelar = new JButton("Utzi");
        
        btnGuardar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnEliminar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCancelar.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        btnGuardar.addActionListener(e -> actualizarRegistro());
        btnEliminar.addActionListener(e -> eliminarRegistro());
        btnCancelar.addActionListener(e -> {
            panelEdicion.setVisible(false);
            panelEdicion.getParent().setPreferredSize(new Dimension(0, 600));
        });
        
        panelEdicion.add(btnGuardar);
        panelEdicion.add(Box.createRigidArea(new Dimension(0, 5)));
        panelEdicion.add(btnEliminar);
        panelEdicion.add(Box.createRigidArea(new Dimension(0, 5)));
        panelEdicion.add(btnCancelar);
        
        panelEdicion.revalidate();
        panelEdicion.repaint();
        revalidate();
    }
    
    private void actualizarRegistro() {
        try {
            StringBuilder sql = new StringBuilder("UPDATE " + nombreTabla + " SET ");
            
            for (int i = 1; i < columnas.size(); i++) {
                sql.append(columnas.get(i)).append(" = ?");
                if (i < columnas.size() - 1) {
                    sql.append(", ");
                }
            }
            
            sql.append(" WHERE ").append(columnas.get(0)).append(" = ?");
            
            PreparedStatement ps = conn.prepareStatement(sql.toString());
            
            int paramIndex = 1;
            for (int i = 1; i < columnas.size(); i++) {
                String valor = camposEdicion.get(columnas.get(i)).getText();
                ps.setString(paramIndex++, valor.isEmpty() ? null : valor);
            }
            
            ps.setObject(paramIndex, filaActual[0]);
            
            int filasAfectadas = ps.executeUpdate();
            
            if (filasAfectadas > 0) {
                JOptionPane.showMessageDialog(this, "Datuak eguneratuta!", "Arrakasta", JOptionPane.INFORMATION_MESSAGE);
                refrescarDatos();
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Errorea eguneratzean: " + e.getMessage(), "Errorea", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void eliminarRegistro() {
        int confirmacion = JOptionPane.showConfirmDialog(this, 
            "Ziur zaude ezabatu nahi duzula?", 
            "Berretsi", 
            JOptionPane.YES_NO_OPTION);
        
        if (confirmacion == JOptionPane.YES_OPTION) {
            try {
                String sql = "DELETE FROM " + nombreTabla + " WHERE " + columnas.get(0) + " = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setObject(1, filaActual[0]);
                
                int filasAfectadas = ps.executeUpdate();
                
                if (filasAfectadas > 0) {
                    JOptionPane.showMessageDialog(this, "Erregistroa ezabatuta!", "Arrakasta", JOptionPane.INFORMATION_MESSAGE);
                    refrescarDatos();
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Errorea ezabatzean: " + e.getMessage(), "Errorea", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void mostrarFormularioNuevo() {
        JDialog dialog = new JDialog(this, "Erregistro berria", true);
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        Map<String, JTextField> camposNuevos = new HashMap<>();
        
        for (int i = 1; i < columnas.size(); i++) {
            String columna = columnas.get(i);
            
            JPanel panelCampo = new JPanel(new BorderLayout(5, 5));
            panelCampo.setMaximumSize(new Dimension(350, 60));
            
            JLabel lbl = new JLabel(columna + ":");
            JTextField txt = new JTextField();
            
            panelCampo.add(lbl, BorderLayout.NORTH);
            panelCampo.add(txt, BorderLayout.CENTER);
            
            camposNuevos.put(columna, txt);
            panel.add(panelCampo);
        }
        
        JButton btnGuardar = new JButton("Gorde");
        btnGuardar.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        btnGuardar.addActionListener(e -> {
            if (insertarNuevoRegistro(camposNuevos)) {
                dialog.dispose();
                refrescarDatos();
            }
        });
        
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnGuardar);
        
        dialog.add(new JScrollPane(panel));
        dialog.setVisible(true);
    }
    
    private boolean insertarNuevoRegistro(Map<String, JTextField> campos) {
        try {
            StringBuilder sql = new StringBuilder("INSERT INTO " + nombreTabla + " (");
            StringBuilder values = new StringBuilder("VALUES (");
            
            int i = 0;
            for (String columna : columnas) {
                if (i == 0) { 
                    i++;
                    continue;
                }
                sql.append(columna);
                values.append("?");
                
                if (i < columnas.size() - 1) {
                    sql.append(", ");
                    values.append(", ");
                }
                i++;
            }
            
            sql.append(") ").append(values).append(")");
            
            PreparedStatement ps = conn.prepareStatement(sql.toString());
            
            int paramIndex = 1;
            for (int j = 1; j < columnas.size(); j++) {
                String valor = campos.get(columnas.get(j)).getText();
                ps.setString(paramIndex++, valor.isEmpty() ? null : valor);
            }
            
            int filasAfectadas = ps.executeUpdate();
            
            if (filasAfectadas > 0) {
                JOptionPane.showMessageDialog(this, "Erregistroa sortuta!", "Arrakasta", JOptionPane.INFORMATION_MESSAGE);
                return true;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Errorea sortzean: " + e.getMessage(), "Errorea", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }
    
    private void imprimirFacturaPDF() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Mesedez, hautatu faktura bat", "Oharra", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            int facturaId = (int) model.getValueAt(row, 0);
            GeneradorPDF.generarFacturaPDF(conn, facturaId);
            JOptionPane.showMessageDialog(this, "PDF sortuta: faktura_" + facturaId + ".pdf", "Arrakasta", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Errorea PDFa sortzean: " + e.getMessage(), "Errorea", JOptionPane.ERROR_MESSAGE);
        }
    }
}
