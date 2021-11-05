import com.sun.nio.sctp.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;
import java.text.*;
import java.util.*;

public class DaytimeServer {
    static int SERVER_PORT = 3456;
    static int US_STREAM = 0;
    static int FR_STREAM = 1;

    static SimpleDateFormat USformatter = new SimpleDateFormat(
                                "h:mm:ss a EEE d MMM yy, zzzz", Locale.US);
    static SimpleDateFormat FRformatter = new SimpleDateFormat(
                                "h:mm:ss a EEE d MMM yy, zzzz", Locale.FRENCH);

    public static void main(String[] args) throws IOException {
        if(args.length < 2)
        {
            System.out.println("Usage : ./prog port ip1 ip2 ...");
            System.exit(0);
        }
        for(int i = 0; i < args.length; ++i)
        {
            System.out.println(i + ":" + args[i]);
        }
        SctpChannel fix = SctpChannel.open();
        SctpServerChannel ssc = SctpServerChannel.open();
        InetSocketAddress serverAddr = 
            new InetSocketAddress(args[1], Integer.parseInt(args[0]));
        ssc.bind(serverAddr);
        for(int i = 2; i < args.length; ++i)
        {
            InetAddress serverAddr1 = 
                InetAddress.getByName(args[i]);
            ssc.bindAddress(serverAddr1);
        }

        ByteBuffer buf = ByteBuffer.allocateDirect(60);
        CharBuffer cbuf = CharBuffer.allocate(60);
        Charset charset = Charset.forName("ISO-8859-1");
        CharsetEncoder encoder = charset.newEncoder();
        ByteBuffer recvbuf = ByteBuffer.allocateDirect(255);
            
        System.out.println("Waiting a connection on :" + ssc.getAllLocalAddresses() + "...");
        SctpChannel sc = ssc.accept();
        AssociationHandler assocHandler = new AssociationHandler();
        while (!assocHandler.close) 
        {
            /* get the current date */
            Date today = new Date();
            cbuf.put(USformatter.format(today)).flip();
            encoder.encode(cbuf, buf, true);
            buf.flip();

            /* send the message on the US stream */
            MessageInfo outMessageInfo = 
                MessageInfo.createOutgoing(null, US_STREAM);
            sc.send(buf, outMessageInfo);

            /* update the buffer with French format */
            cbuf.clear();
            cbuf.put(FRformatter.format(today)).flip();
            buf.clear();
            encoder.encode(cbuf, buf, true);
            buf.flip();

            /* send the message on the French stream */
            outMessageInfo.streamNumber(FR_STREAM);
            sc.send(buf, outMessageInfo);

            cbuf.clear();
            buf.clear();

            /* receive all pending messages/notifications */
            //MessageInfo inMessageInfo = null;
            //while (true) {
            //  inMessageInfo = sc.receive(recvbuf, System.out, assocHandler);
            //  if (inMessageInfo == null || inMessageInfo.bytes() == -1) {
            //    break;
            //  }
            //}
            try
            {
                Thread.sleep(1500);
            }
            catch(InterruptedException e)
            {
                System.out.println("main thread interrupted");
                System.out.println(e);
            }
        } // while()
        sc.shutdown();
        sc.close();
    }// main

    static class AssociationHandler
        extends AbstractNotificationHandler<PrintStream>
    {
        public boolean close = false; 
        public HandlerResult handleNotification(AssociationChangeNotification not,
                                                PrintStream stream) {
            stream.println("AssociationChangeNotification received: " + not);
            return HandlerResult.CONTINUE;
        }

        public HandlerResult handleNotification(ShutdownNotification not,
                                                PrintStream stream) {
            stream.println("ShutdownNotification received: " + not);
            close = true;
            return HandlerResult.RETURN;
        }
    }
}
