package demo.example.helloworld;

import demo.example.helloworld.service.HelloWorldService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.Constants;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * Author: Hugo Zhu
 * Date:   2009-4-18 14:12:16
 */
public class HelloWorldServiceImpl2 implements HelloWorldService, BundleActivator {
    ServiceRegistration helloServiceRegistration;
    public String sayHello() {
        return "Hello, World Again!";
    }

    public void start(BundleContext bundleContext) throws Exception {
        Dictionary props = new Hashtable();  
        props.put(Constants.SERVICE_RANKING, 0);
        props.put(Constants.BUNDLE_VERSION, bundleContext.getBundle().getHeaders().get(Constants.BUNDLE_VERSION));
        helloServiceRegistration = bundleContext.registerService(HelloWorldService.class.getName(), this, props);
    }

    public void stop(BundleContext bundleContext) throws Exception {
        helloServiceRegistration.unregister();
    }
}
