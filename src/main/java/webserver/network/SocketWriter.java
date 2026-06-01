package webserver.network;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class SocketWriter {

    private final SelectorManager selectorManager = SelectorManager.getInstance();

    public SocketWriter() {
    }

    public void write(SelectionKey key) {
        ClientConnection connection = (ClientConnection) key.attachment();
        SocketChannel channel = connection.getSocketChannel();
        ByteBuffer buffer = connection.getWriteBuffer();

        if (buffer == null) {
            try {
                selectorManager.disableWrite(channel);
            } catch (Exception e) {
                System.err.println("Error disabling write: " + e.getMessage());
            }
            return;
        }

        try {
            buffer.flip();
            int bytesWritten = channel.write(buffer);
            System.out.println("Wrote " + bytesWritten + " bytes to " + connection.getRemoteAddress());

            if (buffer.hasRemaining()) {
                buffer.compact();
            } else {
                connection.close();
                key.cancel();
                selectorManager.removeClientChannel(channel);
            }
        } catch (Exception e) {
            System.err.println("Error writing to client: " + e.getMessage());
            connection.close();
            key.cancel();
        }
    }
}
