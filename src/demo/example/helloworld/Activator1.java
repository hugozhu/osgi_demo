package demo.example.helloworld;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Constants;
import demo.example.helloworld.service.HelloWorldService;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: hugozhu
 * Date: Apr 19, 2009
 * Time: 7:15:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class Activator1 implements BundleActivator {
    ServiceRegistration helloServiceRegistration;

    public void start(BundleContext bundleContext) throws Exception {
        HelloWorldService helloService = new HelloWorldServiceImpl1();
        Dictionary props = new Hashtable();
        props.put(Constants.SERVICE_RANKING, 0);
        props.put(Constants.BUNDLE_VERSION, bundleContext.getBundle().getHeaders().get(Constants.BUNDLE_VERSION));
        helloServiceRegistration = bundleContext.registerService(HelloWorldService.class.getName(), helloService, props);
    }

    public void stop(BundleContext bundleContext) throws Exception {
        helloServiceRegistration.unregister();
    }
}
