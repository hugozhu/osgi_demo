package demo;

import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.Request;
import org.osgi.framework.Constants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.HashMap;

import demo.example.helloworld.service.HelloWorldService;

/**
 * Author: Hugo Zhu
 * Date:   2009-4-16 16:23:45
 */
public class RequestHandler extends AbstractHandler {
    OSGiContainer container = null;

    @Override
    protected void doStart() throws Exception {
        Map params = new HashMap();
        params.put(Constants.FRAMEWORK_BOOTDELEGATION, HelloWorldService.class.getPackage().getName());
        container = new OSGiContainer(params);
        container.init();
        
        container.install(new File("bundles/helloworld_bundle1.jar"));
        container.install(new File("bundles/helloworld_bundle2.jar"));
    }

    @Override
    protected void doStop() throws Exception {
        container.uninstallAll();
        container.shutdown();
    }

    public void handle(String target, HttpServletRequest req, HttpServletResponse res, int dispatch) throws IOException, ServletException {
        res.setContentType("text/html;charset=UTF-8");
        if (req.getRequestURI().endsWith("/admin")) {
            handleAdmin(req,res);
        }
        else {
            handleServiceCall(req,res);
        }
        ((Request) req).setHandled(true);
    }

    protected void handleServiceCall(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        HelloWorldService helloworld = (HelloWorldService) container.getService(HelloWorldService.class.getName());
        if (helloworld==null) {
            res.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            res.getWriter().println("Service not available");
        }
        else {
            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().println(helloworld.sayHello());
        }
    }

    protected void handleAdmin(HttpServletRequest req, HttpServletResponse res)  throws IOException, ServletException {
        res.setStatus(HttpServletResponse.SC_OK);
        String action = req.getParameter("action");
        if (action!=null) {
            long id = Long.parseLong(req.getParameter("id"));
            Bundle bundle = container.getBundle(id);
            if (bundle!=null) {
                try {
                    if ("start".equalsIgnoreCase(action)) {
                        bundle.start();
                    }
                    else if ("stop".equalsIgnoreCase(action)) {
                        bundle.stop();
                    }
                } catch (BundleException e) {
                    throw new ServletException(e);
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>Installed bundles:</h1>");
        sb.append("<table border=1>");
        sb.append("<tr><td>ID</td><td>State</td><td>Name</td><td>Version</td><td>&nbsp;</td></tr>");
        Bundle[] bundles = container.getBundles();
        for(Bundle b: bundles) {
            sb.append("<tr>");
            String state = "&nbsp;";
            switch (b.getState()) {
                case Bundle.ACTIVE: state = "Active"; action = String.format("<a href='?action=stop&id=%s'>%s</a>",b.getBundleId(),"Stop"); break;
                case Bundle.INSTALLED: state = "Installed";action = String.format("<a href='?action=start&id=%s'>%s</a>",b.getBundleId(),"Start");break;
                case Bundle.STOPPING: state = "Stopping";break;
                case Bundle.UNINSTALLED: state = "Uninstalled";break;
                case Bundle.RESOLVED: state = "Resolved";action = String.format("<a href='?action=start&id=%s'>%s</a>",b.getBundleId(),"Start");break;
                case Bundle.STARTING: state = "Starting";break;
                default: state = "Unknown";                    
            }
            if (b.getBundleId()==0) {
                //disable action for system bundle
                action ="&nbsp;";
            }
            sb.append(String.format("<td>%d</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td>",b.getBundleId(),state,b.getSymbolicName(),container.getVersion(b),action));
            sb.append("</tr>");
        }
        sb.append("</table>");
        res.getWriter().println(sb.toString());
    }
}
