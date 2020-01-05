import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/*
 * This class shows the effect of SOLINGER option when sending is blocked.
 * Without SOLINGER, it has to wait for close_notify to get through before closing the connection
 * With SOLINGER enabled and timeout set to 0, the connection is closed immediately except
 * with JDK 11, SOLINGER option is ignored and it still takes a long time to close the connection
 */
public class SSLSocketClient {
    static final int PORT = 2001;
    static final int PACKETSIZE = 800;
    static final int NUM = 1000;

    public static void main(String[] args) {

        SSLSocketFactory sslSocketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
        try {
            SSLSocket socket = (SSLSocket)sslSocketFactory.createSocket("localhost", PORT);
            // set a small send buffer size to make it easier to block sending
            socket.setSendBufferSize(100);
            // enable the SOLINGER option and set the timeout to 0
            socket.setSoLinger(true, 0);

            socket.startHandshake();

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // create a string to be sent
            char[] chars = new char[PACKETSIZE];
            Arrays.fill(chars, 'a');
            String line = new String(chars);

            try {
                // this will make the server to stop receiving and eventually cause sending blocked
                out.println("stop");

                // send a lot data and sending will be blocked
                for (int i = 0; i < NUM; i++) {
                    new Thread(() -> out.println(line)).start();
                }

                System.out.println("Sent " + NUM + " packets asynchronously. Closing the socket");
                long before = System.currentTimeMillis();
                // at this time sending is blocked. close_notify will be blocked too.
                // without SOLINGER it has to wait until the other side resumes receiving before the connection can be closed
                // with SOLINGER, the connection can be closed within the timeout.
                socket.close();
                long timeToClose = System.currentTimeMillis() - before;
                System.out.println("Takes " + timeToClose + "ms to close the socket");
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
