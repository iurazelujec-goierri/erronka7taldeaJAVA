package packtaulak;
import java.util.*;
import javax.swing.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class VentanaTabla extends JFrame{
    private Connection conn;
    private String nombreTabla;
    private String rolActual;
    private JTable table;
    private DefaultTableModel model;
    public VentanaTabla(Connection conn,String nombreTabla,String rolActual){
        this.conn=conn;
        this.nombreTabla=nombreTabla;
        this.rolActual=rolActual;
        setTitle(nombreTabla+" - Taula");
        setSize(1100,600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        cargarDatos();
        table.getSelectionModel().addListSelectionListener(e->{
        	if(!e.getValueIsAdjusting()){
        		int fila=table.getSelectedRow();
        		if(fila!=-1){
        			System.out.println("Fila seleccionada: "+fila);
        		}
        	}
        });

        add(new JScrollPane(table),BorderLayout.CENTER);
        setVisible(true);
    }
    private void cargarDatos(){
        try{
            Statement stmt=conn.createStatement();
            ResultSet rs=stmt.executeQuery("SELECT * FROM "+nombreTabla);
            ResultSetMetaData metaData=rs.getMetaData();
            int columnCount=metaData.getColumnCount();
            Vector<String> columnNames=new Vector<>();
            for(int i=1;i<=columnCount;i++){
                columnNames.add(metaData.getColumnName(i));
            }
            Vector<Vector<Object>> data=new Vector<>();
            while(rs.next()){
                Vector<Object> vector=new Vector<>();
                for(int i=1;i<=columnCount;i++){
                    vector.add(rs.getObject(i));
                }
                data.add(vector);
            }
            model=new DefaultTableModel(data,columnNames);
            table=new JTable(model);
        }catch(SQLException e){
            e.printStackTrace();
        }
    }
}
