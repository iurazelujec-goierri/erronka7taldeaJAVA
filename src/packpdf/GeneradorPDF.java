package packpdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Desktop;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;

public class GeneradorPDF {

    public static void generarFacturaPDF(Connection conn, int facturaId) throws Exception {

        String nombreArchivo = "faktura_" + facturaId + ".pdf";
        File archivoPDF = new File(nombreArchivo);

        try (PDDocument document = new PDDocument()) {

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream =
                         new PDPageContentStream(document, page)) {

                String sqlFactura =
                        "SELECT f.*, b.izena, b.abizena, b.Helbide, b.Telefono " +
                        "FROM faktura f " +
                        "JOIN bezero b ON f.bezero_id = b.id " +
                        "WHERE f.id = ?";

                PreparedStatement psFactura = conn.prepareStatement(sqlFactura);
                psFactura.setInt(1, facturaId);

                ResultSet rsFactura = psFactura.executeQuery();

                if (rsFactura.next()) {
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20);
                    contentStream.newLineAtOffset(50, 750);
                    contentStream.showText("FAKTURA #" + facturaId);
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 12);
                    contentStream.newLineAtOffset(50, 710);

                    contentStream.showText(
                            "Bezeroa: " +
                            rsFactura.getString("izena") + " " +
                            rsFactura.getString("abizena")
                    );

                    contentStream.newLineAtOffset(0, -20);
                    contentStream.showText("Helbidea: " + rsFactura.getString("Helbide"));

                    contentStream.newLineAtOffset(0, -20);
                    contentStream.showText("Telefonoa: " + rsFactura.getString("Telefono"));

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    contentStream.newLineAtOffset(0, -20);
                    contentStream.showText(
                            "Data: " + sdf.format(rsFactura.getTimestamp("data"))
                    );

                    contentStream.endText();

                    int yActual = 620;

                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    contentStream.newLineAtOffset(50, yActual);

                    contentStream.showText("Produktua");
                    contentStream.newLineAtOffset(250, 0);
                    contentStream.showText("Kop.");
                    contentStream.newLineAtOffset(80, 0);
                    contentStream.showText("Prezioa U.");
                    contentStream.newLineAtOffset(80, 0);
                    contentStream.showText("Guztira");

                    contentStream.endText();
                    String sqlLineas =
                            "SELECT fl.*, p.izena AS p_izena " +
                            "FROM faktura_lerroak fl " +
                            "JOIN produktuak p ON fl.produktu_id = p.id " +
                            "WHERE fl.faktura_id = ?";

                    PreparedStatement psL = conn.prepareStatement(sqlLineas);
                    psL.setInt(1, facturaId);

                    ResultSet rsL = psL.executeQuery();
                    contentStream.setFont(PDType1Font.HELVETICA, 11);

                    while (rsL.next()) {

                        contentStream.beginText();
                        contentStream.newLineAtOffset(50, yActual);

                        contentStream.showText(rsL.getString("p_izena"));
                        contentStream.newLineAtOffset(250, 0);
                        contentStream.showText(String.valueOf(rsL.getInt("kopurua")));
                        contentStream.newLineAtOffset(80, 0);
                        contentStream.showText(
                                String.format("%.2f", rsL.getDouble("prezioa_unitarioa"))
                        );

                        contentStream.newLineAtOffset(80, 0);
                        double subtotal =
                                rsL.getDouble("prezioa_unitarioa") *
                                rsL.getInt("kopurua");

                        contentStream.showText(
                                String.format("%.2f €", subtotal)
                        );

                        contentStream.endText();
                        yActual -= 20;
                    }
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                    contentStream.newLineAtOffset(400, yActual - 40);
                    contentStream.showText(
                            "GUZTIRA: " +
                            String.format("%.2f €", rsFactura.getDouble("guztira"))
                    );
                    contentStream.endText();
                }
            }

            document.save(archivoPDF);
        }

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(archivoPDF);
        }
    }
}
