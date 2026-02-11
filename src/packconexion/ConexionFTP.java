package packconexion;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ConexionFTP {
    
    private static final String SERVER = "192.168.115.156"; 
    private static final int PORT = 21; 
    private static final String USER = "root";
    private static final String PASS = "1234";
    private static final String REMOTE_PATH = "/";
    
    public static boolean subirArchivo(File archivoLocal) {
        FTPClient ftpClient = new FTPClient();
        boolean subidoConExito = false;

        try {
            ftpClient.connect(SERVER, PORT);
            boolean login = ftpClient.login(USER, PASS);

            if (!login) {
                System.out.println("Login FTP errorea.");
                return false;
            }

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            try (FileInputStream inputStream = new FileInputStream(archivoLocal)) {
                String remoteFile = archivoLocal.getName();
                subidoConExito = ftpClient.storeFile(remoteFile, inputStream);
            }
            if (subidoConExito) {
                System.out.println("Fitxategia zerbitzarira igo da.");
                
                try { Thread.sleep(500); } catch (InterruptedException e) {}
                if (archivoLocal.delete()) {
                    System.out.println("Fitxategi lokala ezabatu da.");
                } else {
                    System.out.println("Oharra: Ezin izan da fitxategi lokala ezabatu oraindik (irekita dago).");
                }
                return true;
            } else {
                System.out.println("Ezin izan da fitxategia igo.");
                return false;
            }

        } catch (IOException e) {
            System.out.println("Errorea: " + e.getMessage());
            return false;
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}