package master;
 
import lejos.hardware.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
 
import lejos.hardware.ev3.LocalEV3;
import lejos.remote.ev3.RemoteEV3;
 
public class BrickFinder {
    private static final int DISCOVERY_PORT = 3016;
    private static final String FIND_CMD = "find";
    private static Brick defaultBrick, localBrick;
    private static final int MAX_DISCOVERY_TIME = 2000;
    private static final int MAX_PACKET_SIZE = 32;
    private static final int MAX_HOPS = 2;
 
    /**
     * Internal class to implement a name server that can respond to queries
     * generated by discover and find methods. User code will not normally need to
     * run an instance of this as the leJOS menu will usually do so.
     * @author andy
     *
     */
    private static class DiscoveryServer implements Runnable
    {
        DatagramSocket socket;
        boolean forward;
        static DiscoveryServer server;
        static Thread serverThread;
 
        private DiscoveryServer(boolean forward)
        {
            this.forward = forward;
        }
       
        public void run()
        {
            try {
                // Keep a socket open to listen to all the UDP trafic that is
                // destined for this port
                socket = new DatagramSocket(DISCOVERY_PORT,
                        InetAddress.getByName("0.0.0.0"));
                socket.setBroadcast(true);
                // make sure localBrick is valid
                if (getLocal() == null) return;
                // Receive a packet
                byte[] recvBuf = new byte[MAX_PACKET_SIZE];
                DatagramPacket packet = new DatagramPacket(recvBuf,
                        recvBuf.length);
                while (true)
                {
                    // Receive a packet
                    socket.receive(packet);
                    // Packet received
                    //System.out.println(getClass().getName()
                            //+ ">>>Discovery packet received from: "
                            //+ packet.getAddress().getHostAddress());
                    // See if the packet holds the right command (message)
                    String message = new String(packet.getData(), 0, packet.getLength()).trim();
                    //System.out.println("Message " + message);
                    String[] args = message.split("\\s+");
                    if (args.length == 5 && args[0].equalsIgnoreCase(FIND_CMD))
                    {
                        InetAddress replyAddr = InetAddress.getByName(args[2]);
                        int replyPort = Integer.parseInt(args[3]);
                        int hops = Integer.parseInt(args[4]);
                        String hostname = localBrick.getName();
                        if ((!forward || hops == MAX_HOPS) && (args[1].equalsIgnoreCase("*") || args[1].equalsIgnoreCase(hostname)))
                        {
                            byte[] sendData = hostname.getBytes();
                            //System.out.println("Send response to " + replyAddr);
                            // Send a response
                            DatagramPacket sendPacket = new DatagramPacket(
                                    sendData, sendData.length, replyAddr,
                                    replyPort);
                            try {
                                socket.send(sendPacket);
                            } catch (IOException e)
                            {
                                System.out.println("Error in send" + e);
                                // ignore errors on send , we need to keep running
                            }
 
                            //System.out.println(getClass().getName()
                                    //+ ">>>Sent packet to: "
                                    //+ sendPacket.getAddress().getHostAddress());
                        }
                        if (forward && --hops > 0)
                            broadcastFindRequest(socket, args[1], replyAddr, replyPort, hops);
                    }
                }
            } catch (SocketException e) {
                // do nothing we use this to force an exit of the thread
            } catch (IOException e) {
                System.out.println("Brickfinder Got error " + e);
            } finally {
                socket.close();
            }
        }
 
        /**
         * Start the discovery server for this device
         * @param forward true if requests should be forwarded to other devices
         */
        public static synchronized void start(boolean forward)
        {
            if (server == null)
            {
                server = new DiscoveryServer(forward);
                serverThread = new Thread(server);
                serverThread.setDaemon(true);
                serverThread.start();
            }
        }
 
        /**
         * Stop the discovery service on this device. Wait for the server to exit.
         */
        public static synchronized void stop()
        {
            if (server != null && server.socket != null)
            {
                // abort the running server
                if (server.socket != null)
                    server.socket.close();
                try
                {
                    serverThread.join();
                } catch (InterruptedException e)
                {
                    // not a lot to do
                }
                server.socket = null;
                server = null;             
            }
        }
    }
   
   
    public static Brick getLocal() {
        if (localBrick != null) return localBrick;
        // Check we are running on an EV3
        localBrick = LocalEV3.get();
        return localBrick;
    }
   
    public static Brick getDefault() {
        if (defaultBrick != null) return defaultBrick;
        try {
            // See if we are running on an EV3
            defaultBrick =  LocalEV3.get();
            return defaultBrick;
        } catch (Throwable e) {
            try {
                BrickInfo[] bricks = discover();
                if (bricks.length > 0) {
                    defaultBrick = new RemoteEV3(bricks[0].getIPAddress());
                    return defaultBrick;
                } else throw new DeviceException("No EV3 brick found");
            } catch (Exception e1) {
                throw new DeviceException("No brick found");
            }
        }
    }
   
    private static void broadcastFindRequest(DatagramSocket socket, String name, InetAddress replyAddr, int replyPort, int hop)
    {
        try
        {
            // Broadcast the message over all the network interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (interfaces.hasMoreElements())
            {
                NetworkInterface networkInterface = (NetworkInterface) interfaces
                        .nextElement();
 
                if (networkInterface.isLoopback() || !networkInterface.isUp())
                {
                    continue; // Don't want to broadcast to the loopback
                              // interface
                }
 
                for (InterfaceAddress interfaceAddress : networkInterface
                        .getInterfaceAddresses())
                {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null)
                        continue;
                    String message = FIND_CMD + " " + name;
                    if (replyAddr == null)
                        message += " " + interfaceAddress.getAddress().getHostAddress() + " " + socket.getLocalPort();
                    else
                        message += " " + replyAddr.getHostAddress() + " " + replyPort;
                    message += " " + hop;
                       
                    byte[] sendData = message.getBytes();
 
                    // Send the broadcast packet.
                    try
                    {
                        //System.out.println("Send to " + broadcast.getHostAddress() + " port " + DISCOVERY_PORT );
                        DatagramPacket sendPacket = new DatagramPacket(
                                sendData, sendData.length, broadcast, DISCOVERY_PORT);
                        socket.send(sendPacket);
                    } catch (Exception e)
                    {
                        System.err
                                .println("Exception sending to : "
                                        + networkInterface.getDisplayName()
                                        + " : " + e);
                    }
                }
            }
        } catch (IOException ex)
        {
            System.err.println("Exception opening socket : " + ex);
        }      
    }
 
    /**
     * Search for a named EV3. Return a table of the addresses that can be used to contact
     * the device. An empty table is returned if no EV3s are found.
     * @param name
     * @return A table of matching devices
     */
    public static BrickInfo[] find(String name)
    {
        DatagramSocket socket=null;
        Map<String,BrickInfo> ev3s = new HashMap<String,BrickInfo>();
        try
        {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            boolean findAll = name.equalsIgnoreCase("*");
            broadcastFindRequest(socket, name, null, -1, MAX_HOPS);
            socket.setSoTimeout(MAX_DISCOVERY_TIME/4);
            DatagramPacket packet = new DatagramPacket (new byte[MAX_PACKET_SIZE], MAX_PACKET_SIZE);
   
            long start = System.currentTimeMillis();
           
            while ((System.currentTimeMillis() - start) < MAX_DISCOVERY_TIME) {
                try {
                    socket.receive (packet);
                    String message = new String(packet.getData(), "UTF-8").trim();
                    if (findAll || message.equalsIgnoreCase(name))
                    {
                        String ip = packet.getAddress().getHostAddress();
                        ev3s.put(ip, new BrickInfo(message.trim(),ip,"EV3"));
                    }
                } catch (SocketTimeoutException e)
                {
                    // No response ask again
                    broadcastFindRequest(socket, name, null, -1, MAX_HOPS);                    
                }
            }
        } catch (IOException ex)
        {
            System.err.println("Exception opening socket : " + ex);
        } finally
        {
            if (socket != null)
                socket.close();
        }
        BrickInfo[] devices = new BrickInfo[ev3s.size()];
        int i = 0;
       
        for(String ev3: ev3s.keySet()) {
            devices[i++] = ev3s.get(ev3);
        }        
        return devices;
    }
   
   
    /**
     * Search for available EV3s and populate table with results.
     */
    public static BrickInfo[] discover() { 
        return find("*");
    }
   
    public static BrickInfo[] discoverNXT() {
        try {
            Collection<RemoteBTDevice> nxts = Bluetooth.getLocalDevice().search();
            BrickInfo[] bricks = new BrickInfo[nxts.size()];
            int i = 0;
            for(RemoteBTDevice d: nxts) {
                BrickInfo b = new BrickInfo(d.getName(), d.getAddress(), "NXT");
                bricks[i++] = b;
            }
            return bricks;
        } catch (Exception e) {
            throw new DeviceException("Error finding remote NXTs", e);
        }
    }
   
    public static void setDefault(Brick brick) {
        defaultBrick = brick;
    }
 
   
    /**
     * Start the discovery server running. There should be a single discovery server
     * running on each EV3. This provides responses to remote discover and find
     * requests. Normally this server will be run by the leJOS menu and so user code
     * does not need to start a copy.
     * @param forward true if requests should be forwarded to other devices
     */
    public static void startDiscoveryServer(boolean forward)
    {
        DiscoveryServer.start(forward);
    }
 
    /**
     * Stop the discovery server
     */
    public static void stopDiscoveryServer()
    {
        DiscoveryServer.stop();
    }
}