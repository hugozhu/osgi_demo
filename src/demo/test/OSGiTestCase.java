package demo.test;

import junit.framework.TestCase;
import demo.OSGiContainer;
import demo.example.helloworld.service.HelloWorldService;

import java.io.File;
import java.util.Map;
import java.util.HashMap;

import org.osgi.framework.*;

/**
 * Created by IntelliJ IDEA.
 * User: hugozhu
 * Date: Apr 19, 2009
 * Time: 8:30:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class OSGiTestCase extends TestCase {
    OSGiContainer osgi;
    @Override
    protected void setUp() throws Exception {
        Map params = new HashMap();
        params.put(Constants.FRAMEWORK_BOOTDELEGATION,HelloWorldService.class.getPackage().getName());
        osgi = new OSGiContainer(params);
        osgi.init();
    }

    public void test_install_uninstall() throws Exception {
        osgi.install(new File("bundles/helloworld_bundle1.jar"));
        Bundle bundle = osgi.getBundle(HelloWorldService.class.getName());
        assertNotNull(bundle);
        osgi.uninstall(bundle);
        assertNull(osgi.getBundle(HelloWorldService.class.getName()));
    }

    public void test_service() throws Exception {
        Bundle bundle = osgi.install(new File("bundles/helloworld_bundle1.jar"));
        bundle.start();
        HelloWorldService helloWorld = (HelloWorldService) osgi.getService(HelloWorldService.class.getName());
        assertEquals("Hello, World!",helloWorld.sayHello());
        Bundle bundle2 = osgi.install(new File("bundles/helloworld_bundle2.jar"));
        bundle2.start();

        helloWorld = (HelloWorldService) osgi.getService(HelloWorldService.class.getName());
        //although we started bundle2, we still expect bundle provides the service since both of them have same service rank
        assertEquals("Hello, World!",helloWorld.sayHello());
        bundle.stop();

        helloWorld = (HelloWorldService) osgi.getService(HelloWorldService.class.getName());
        assertEquals("Hello, World Again!",helloWorld.sayHello());
    }

    public void test_service_filter() throws Exception {
        Bundle bundle = osgi.install(new File("bundles/helloworld_bundle1.jar"));
        bundle.start();
        Bundle bundle2 = osgi.install(new File("bundles/helloworld_bundle2.jar"));
        bundle2.start();
        HelloWorldService helloWorld = (HelloWorldService) osgi.getService(HelloWorldService.class.getName(),"(&(objectClass=*))");
        assertNotNull(helloWorld);
        
        helloWorld = (HelloWorldService) osgi.getService(HelloWorldService.class.getName(),"(&(objectClass=*)(Bundle-Version=1.0.0))");
        assertNotNull(helloWorld);
        assertEquals("Hello, World!",helloWorld.sayHello());

        helloWorld = (HelloWorldService) osgi.getService(HelloWorldService.class.getName(),"(&(objectClass=*)(Bundle-Version=2.0.0))");
        assertNotNull(helloWorld);
        assertEquals("Hello, World Again!",helloWorld.sayHello());

        helloWorld = (HelloWorldService) osgi.getService(HelloWorldService.class.getName(),"(&(objectClass=*)(Bundle-Version<=3.0.0))");
        assertNotNull(helloWorld);

        helloWorld = (HelloWorldService) osgi.getService(HelloWorldService.class.getName(),"(&(objectClass=*)(Bundle-Version>=1.0.0))");
        assertNotNull(helloWorld);

        helloWorld = (HelloWorldService) osgi.getService(HelloWorldService.class.getName(),"(&(objectClass=*)(Bundle-Version>=4.0.0))");
        assertNull(helloWorld);
    }

    public void test_get_bundle_service() throws Exception {
        osgi.install(new File("bundles/helloworld_bundle1.jar"));
        Bundle bundle = osgi.getBundle(HelloWorldService.class.getName());
        bundle.start();
        HelloWorldService helloWorld = (HelloWorldService) osgi.getBundleService(bundle,HelloWorldService.class.getName());
        assertEquals("Hello, World!",helloWorld.sayHello());

        osgi.install(new File("bundles/helloworld_bundle2.jar"));
        Bundle bundle2 = osgi.getBundle(HelloWorldService.class.getName());
        bundle2.start();
        bundle.stop();

        helloWorld =  (HelloWorldService) osgi.getBundleService(bundle2,HelloWorldService.class.getName());
        assertEquals("Hello, World Again!",helloWorld.sayHello());

        bundle.start();
        bundle2.stop();
        helloWorld =  (HelloWorldService) osgi.getBundleService(bundle,HelloWorldService.class.getName());
        assertEquals("Hello, World!",helloWorld.sayHello());
    }

    public void test_deploy_with_live_traffic() throws Exception {
        final Bundle bundle = osgi.install(new File("bundles/helloworld_bundle1.jar"));
        bundle.start();

        final Bundle bundle2 = osgi.install(new File("bundles/helloworld_bundle2.jar"));
        bundle2.start();

        Thread[] clientThreads = new Thread[10];
        for(int i=0;i<clientThreads.length;i++) {
            clientThreads[i] = new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        HelloWorldService helloWorld = (HelloWorldService) osgi.getService(HelloWorldService.class.getName());
                        if (helloWorld==null) {
                            break;
                        }
                        if (bundle.getState()!=Bundle.ACTIVE && bundle2.getState()!=Bundle.ACTIVE) {
                            break;
                        }
                        String hello = helloWorld.sayHello();
                    }
                }
            });
            clientThreads[i].start();
        }
        Thread deployThread = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.currentThread().sleep(1000);
                    bundle.stop(); //stop bundle, so we should only be able to use bundle2's service

                    Thread.currentThread().sleep(1000);
                    bundle.start();
                    bundle2.stop();
                    //start bundle and stop bundle2, so we should only be able to use bundle's service

                    Thread.currentThread().sleep(1000);
                    bundle.stop(); //stop bundle, no service available, client thread should exit now                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        deployThread.start();
        for(int i=0;i<clientThreads.length;i++) {
            clientThreads[i].join();
        }
    }    

    @Override
    protected void tearDown() throws Exception {
        osgi.uninstallAll();
        osgi.shutdown();
    }
}