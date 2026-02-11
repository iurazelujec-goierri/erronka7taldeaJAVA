package packpdf;

import java.awt.Desktop;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import packconexion.ConexionFTP;

public class GeneradorPDF {
    
    public static void generarFacturaPDF(Connection conn, int facturaId) throws Exception {
        String nombreArchivo = "faktura_" + facturaId + ".pdf";
        File archivoPDF = new File(nombreArchivo);
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                
                String sqlFactura = "SELECT f.*, b.izena, b.abizena, b.Helbide, b.Telefono FROM faktura f JOIN bezero b ON f.bezero_id = b.id WHERE f.id = ?";
                PreparedStatement psFactura = conn.prepareStatement(sqlFactura);
                psFactura.setInt(1, facturaId);
                ResultSet rsFactura = psFactura.executeQuery();
                
                if (rsFactura.next()) {
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 22);
                    contentStream.newLineAtOffset(50, 750);
                    contentStream.showText("FAKTURA #" + facturaId);
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 11);
                    contentStream.newLineAtOffset(50, 715);
                    contentStream.showText("Bezeroa: " + rsFactura.getString("izena") + " " + rsFactura.getString("abizena"));
                    contentStream.newLineAtOffset(0, -15);
                    contentStream.showText("Helbidea: " + (rsFactura.getString("Helbide") != null ? rsFactura.getString("Helbide") : "---"));
                    contentStream.newLineAtOffset(0, -15);
                    contentStream.showText("Telefonoa: " + (rsFactura.getString("Telefono") != null ? rsFactura.getString("Telefono") : "---"));
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    contentStream.newLineAtOffset(0, -15);
                    contentStream.showText("Data: " + sdf.format(rsFactura.getTimestamp("data")));
                    contentStream.endText();
                    
                    int yTabla = 630;
                    int colProd = 55, colKop = 320, colPrez = 410, colGuz = 510;

                    contentStream.setLineWidth(1f);
                    contentStream.addRect(50, yTabla - 5, 510, 20); 
                    contentStream.stroke();

                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11);
                    escribirTexto(contentStream, colProd, yTabla, "Produktua");
                    escribirTexto(contentStream, colKop, yTabla, "Kop.");
                    escribirTexto(contentStream, colPrez, yTabla, "Prezioa U.");
                    escribirTexto(contentStream, colGuz, yTabla, "Guztira");

                    int yFila = yTabla - 25;

                    String sqlLineas = "SELECT fl.*, p.izena as p_izena FROM faktura_lerroak fl JOIN produktuak p ON fl.produktu_id = p.id WHERE fl.faktura_id = ?";
                    PreparedStatement psL = conn.prepareStatement(sqlLineas);
                    psL.setInt(1, facturaId);
                    ResultSet rsL = psL.executeQuery();

                    contentStream.setFont(PDType1Font.HELVETICA, 10);
                    while (rsL.next()) {
                        escribirTexto(contentStream, colProd, yFila, rsL.getString("p_izena"));
                        escribirTexto(contentStream, colKop, yFila, String.valueOf(rsL.getInt("kopurua")));
                        escribirTexto(contentStream, colPrez, yFila, String.format("%.2f €", rsL.getDouble("prezioa_unitarioa")));
                        double subtotal = rsL.getDouble("prezioa_unitarioa") * rsL.getInt("kopurua");
                        escribirTexto(contentStream, colGuz, yFila, String.format("%.2f €", subtotal));
                        
                        contentStream.setLineWidth(0.5f);
                        contentStream.moveTo(50, yFila - 5);
                        contentStream.lineTo(560, yFila - 5);
                        contentStream.stroke();
                        
                        yFila -= 20;
                    }

                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 13);
                    contentStream.newLineAtOffset(420, yFila - 20);
                    contentStream.showText("GUZTIRA: " + String.format("%.2f €", rsFactura.getDouble("guztira")));
                    contentStream.endText();
                }
            }
            document.save(archivoPDF);
        }

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(archivoPDF);
        }

        Thread.sleep(1000);

        boolean ok = ConexionFTP.subirArchivo(archivoPDF);
        if(ok) System.out.println("Proceso finalizado: Factura #" + facturaId + " subida y borrada.");
    }

    private static void escribirTexto(PDPageContentStream cs, float x, float y, String texto) throws Exception {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(texto != null ? texto : "");
        cs.endText();
    }
}