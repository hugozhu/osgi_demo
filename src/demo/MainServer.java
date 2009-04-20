package demo;

import org.mortbay.jetty.Server;

/**
 * Author: Hugo Zhu
 * Date:   2009-4-16 16:14:41
 */
public class MainServer {
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        server.setHandler(new RequestHandler());
        server.start();
        System.out.println("===============================================================");
        System.out.println("Use http://localhost:8080/ to test service call...");
        System.out.println("Use http://localhost:8080/admin to start/stop installed bundles");
    }
}
