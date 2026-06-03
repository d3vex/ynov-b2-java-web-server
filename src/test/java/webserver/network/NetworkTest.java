package webserver.network;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NetworkTest {

    @Nested
    @DisplayName("TimeoutManager")
    class TimeoutManagerTests {

        @Test
        @DisplayName("Does not timeout connections within threshold")
        void noTimeoutWithinThreshold() throws Exception {
            TimeoutManager tm = new TimeoutManager(60000);
            Map<SocketChannel, SelectionKey> channels = new HashMap<>();
            tm.checkTimeouts(channels);
            // Should not throw or fail
        }

        @Test
        @DisplayName("Check timeouts called twice within 1 second skips second check")
        void rateLimited() throws Exception {
            TimeoutManager tm = new TimeoutManager(60000);
            Map<SocketChannel, SelectionKey> channels = new HashMap<>();
            tm.checkTimeouts(channels);
            tm.checkTimeouts(channels);
            // Second call should be skipped (rate limited to 1s)
        }

        @Test
        @DisplayName("Constructor accepts timeout value")
        void constructor() {
            TimeoutManager tm = new TimeoutManager(30000);
            assertNotNull(tm);
        }

        @Test
        @DisplayName("Handles null attachments gracefully")
        void nullAttachment() throws Exception {
            TimeoutManager tm = new TimeoutManager(1);
            Map<SocketChannel, SelectionKey> channels = new HashMap<>();

            try (var serverSocket = java.nio.channels.ServerSocketChannel.open()) {
                serverSocket.bind(new java.net.InetSocketAddress("127.0.0.1", 0));
                int port = serverSocket.socket().getLocalPort();

                try (SocketChannel client = SocketChannel.open()) {
                    client.connect(new java.net.InetSocketAddress("127.0.0.1", port));
                    client.configureBlocking(false);

                    try (var selector = java.nio.channels.Selector.open()) {
                        SelectionKey key = client.register(selector, 0);
                        channels.put(client, key);

                        // Sleep to let time pass
                        Thread.sleep(5);

                        // Should not throw with null attachment
                        tm.checkTimeouts(channels);
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("ClientConnection")
    class ClientConnectionTests {

        @Test
        @DisplayName("Create connection with socket channel")
        void createConnection() throws Exception {
            try (var serverSocket = java.nio.channels.ServerSocketChannel.open()) {
                serverSocket.bind(new java.net.InetSocketAddress("127.0.0.1", 0));
                int port = serverSocket.socket().getLocalPort();

                try (SocketChannel client = SocketChannel.open()) {
                    client.connect(new java.net.InetSocketAddress("127.0.0.1", port));
                    ClientConnection conn = new ClientConnection(client);

                    assertNotNull(conn.getSocketChannel());
                    assertTrue(conn.isOpen());
                    assertTrue(conn.isConnected());
                    assertNotNull(conn.getReadBuffer());

                    conn.close();
                    assertFalse(conn.isOpen());
                }
            }
        }

        @Test
        @DisplayName("Get remote address")
        void remoteAddress() throws Exception {
            try (var serverSocket = java.nio.channels.ServerSocketChannel.open()) {
                serverSocket.bind(new java.net.InetSocketAddress("127.0.0.1", 0));
                int port = serverSocket.socket().getLocalPort();

                try (SocketChannel client = SocketChannel.open()) {
                    client.connect(new java.net.InetSocketAddress("127.0.0.1", port));
                    ClientConnection conn = new ClientConnection(client);
                    assertNotNull(conn.getRemoteAddress());
                    conn.close();
                }
            }
        }

        @Test
        @DisplayName("Write buffer management")
        void writeBuffer() throws Exception {
            try (var serverSocket = java.nio.channels.ServerSocketChannel.open()) {
                serverSocket.bind(new java.net.InetSocketAddress("127.0.0.1", 0));
                int port = serverSocket.socket().getLocalPort();

                try (SocketChannel client = SocketChannel.open()) {
                    client.connect(new java.net.InetSocketAddress("127.0.0.1", port));
                    ClientConnection conn = new ClientConnection(client);

                    // Initially no write buffer
                    conn.CreateWriteBuffer();
                    assertNotNull(conn.getWriteBuffer());
                    assertEquals(0, conn.RemainingWriteBuffer());

                    conn.CreateWriteBuffer(1024);
                    assertNotNull(conn.getWriteBuffer());

                    conn.close();
                }
            }
        }

        @Test
        @DisplayName("Timeout and last activity tracking")
        void timeoutTracking() throws Exception {
            try (var serverSocket = java.nio.channels.ServerSocketChannel.open()) {
                serverSocket.bind(new java.net.InetSocketAddress("127.0.0.1", 0));
                int port = serverSocket.socket().getLocalPort();

                try (SocketChannel client = SocketChannel.open()) {
                    client.connect(new java.net.InetSocketAddress("127.0.0.1", port));
                    ClientConnection conn = new ClientConnection(client);

                    conn.setTimeoutMs(30000);
                    assertEquals(30000, conn.getTimeoutMs());

                    long before = conn.getLastActivityTime();
                    conn.UpdateLastActivityTime();
                    assertTrue(conn.getLastActivityTime() >= before);

                    conn.close();
                }
            }
        }

        @Test
        @DisplayName("CGI pending flag")
        void cgiPending() throws Exception {
            try (var serverSocket = java.nio.channels.ServerSocketChannel.open()) {
                serverSocket.bind(new java.net.InetSocketAddress("127.0.0.1", 0));
                int port = serverSocket.socket().getLocalPort();

                try (SocketChannel client = SocketChannel.open()) {
                    client.connect(new java.net.InetSocketAddress("127.0.0.1", port));
                    ClientConnection conn = new ClientConnection(client);

                    assertFalse(conn.isCgiResponsePending());
                    conn.setCgiResponsePending(true);
                    assertTrue(conn.isCgiResponsePending());
                    conn.setCgiResponsePending(false);
                    assertFalse(conn.isCgiResponsePending());

                    conn.close();
                }
            }
        }
    }

    @Nested
    @DisplayName("Memory leak scenarios")
    class MemoryLeakScenarios {

        @Test
        @DisplayName("TimeoutManager 1000 checks without channels")
        void manyChecks() {
            TimeoutManager tm = new TimeoutManager(60000);
            Map<SocketChannel, SelectionKey> channels = new HashMap<>();
            for (int i = 0; i < 1000; i++) {
                tm.checkTimeouts(channels);
            }
        }
    }
}
