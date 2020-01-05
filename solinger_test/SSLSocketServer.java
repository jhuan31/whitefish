import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

public class SSLSocketServer {

    static final int port = 2001;

    public static void main(String[] args) {

        try {
            SSLContext ctx;
            KeyManagerFactory kmf;
            KeyStore ks;
            char[] passphrase = "passphrase".toCharArray();

            ctx = SSLContext.getInstance("TLS");
            kmf = KeyManagerFactory.getInstance("SunX509");
            ks = KeyStore.getInstance("JKS");

            ks.load(new FileInputStream("testkeys"), passphrase);
            kmf.init(ks, passphrase);
            ctx.init(kmf.getKeyManagers(), null, null);

            SSLServerSocketFactory sslServerSocketFactory = ctx.getServerSocketFactory();

            ServerSocket serverSocket = sslServerSocketFactory.createServerSocket(port);
            System.out.println("ServerSocket started");
            System.out.println(serverSocket);

            serverSocket.setReceiveBufferSize(100);
            Socket socket = serverSocket.accept();
            socket.setReceiveBufferSize(100);
            System.out.println("Connection accepted. receive buf size is: " + socket.getReceiveBufferSize());

            try (BufferedReader bufferedReader =
                         new BufferedReader(
                                 new InputStreamReader(socket.getInputStream()))) {
                String line;
                while((line = bufferedReader.readLine()) != null){
                    if (line.equals("stop")) {
                        System.out.println("Stop receiving for 20 seconds");
                        // stop receiving to put back pressure on the sender side
                        Thread.sleep(20000);
                    }
                }
            }

            socket.close();
            serverSocket.close();
            System.out.println("Exiting...");
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

}