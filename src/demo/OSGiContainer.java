package demo;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.framework.cache.BundleCache;
import org.osgi.framework.*;

import java.util.Map;
import java.io.File;

/**
 * An OSGi Container, main class to provide OSGi functionalities
 *
 * Author: Hugo Zhu
 * Date:   2009-4-16 15:42:23
 */
public class OSGiContainer {
    private Felix felix = null;
    private BundleContext context = null;

    private Map<Object,Object> parameters = null;

    public OSGiContainer(Map<Object,Object> parameters) {
        this.parameters = parameters;
    }
    
    public void init() {
        Map configMap = new StringMap(false);
        configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES,
            "org.osgi.framework; version=1.3.0," +
            "org.osgi.service.packageadmin; version=1.2.0," +
            "org.osgi.service.startlevel; version=1.0.0," +
            "org.osgi.service.url; version=1.0.0");

        if (parameters!=null) {
            for(Map.Entry<Object,Object> entry:parameters.entrySet()) {
                configMap.put(entry.getKey(),entry.getValue());
            }
        }

        configMap.put(BundleCache.CACHE_ROOTDIR_PROP, "cache");
        
        try
        {
            felix = new Felix(configMap);
            felix.start();
            context = felix.getBundleContext();
        }
        catch (Exception ex)
        {
            System.err.println("Could not create framework: " + ex);
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * 
     * @param location
     * @throws BundleException
     */
    public Bundle install(File location) throws BundleException {
        return context.installBundle(location.toURI().toString());
    }

    /**
     * Uninstalls all bundles from this OSGi instance.
     */
    public void uninstallAll() {
        for (Bundle bundle : context.getBundles()) {
            if (bundle.getSymbolicName().equals("org.apache.felix.framework")) continue;
            uninstall(bundle);
        }
    }


    /**
     * Uninstall the bundle
     * @param bundle
     */
    public void uninstall(Bundle bundle) {
        try {
            //if bundle is already in stopping state, we wait for it stops
            while (bundle.getState() == Bundle.STOPPING) {
                try {
                    Thread.sleep(100l);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }            
            bundle.stop();
            bundle.uninstall();
        }
        catch (BundleException e) {
            throw new RuntimeException("Could not uninstall the '" + bundle.getSymbolicName() + "' bundle:"+e.getMessage(),e);
        }
    }

    /**
     * Get bundle by location in file system or network, or symbolic name
     * If there are multiple version exists, the higest version will be returned
     * 
     * @param idOrLocation
     * @return
     */
    public Bundle getBundle(String idOrLocation) {
        if (idOrLocation.endsWith(".jar")) { // Is location
            String location=normalizeLocation(idOrLocation);
            for (Bundle bundle : getBundles())
                if (bundle.getLocation().equals(location))
                    return bundle;
        }
        else {
            Bundle highestMatch=null;
            for (Bundle bundle : getBundles()) {
                if ( ! bundle.getSymbolicName().equals(idOrLocation)) continue;                
                if (highestMatch==null || getVersion(highestMatch).compareTo(getVersion(bundle))<0)
                    highestMatch=bundle;
            }
            return highestMatch;        
        }
        return null;
    }

    public Bundle getBundle(long id) {
        return context.getBundle(id);
    }

    public Bundle getBundle(String id, String version) {
        for (Bundle bundle : getBundles()) {
            if ( ! bundle.getSymbolicName().equals(id)) continue;
            if ( ! getVersion(bundle).equals(version)) continue;
            return bundle;
        }
        return null;
    }

    public String getVersion(Bundle bundle) {
        Object bundleVersion=bundle.getHeaders().get("Bundle-Version");
        if (bundleVersion==null) {
            return "";
        }
        else {
            return bundleVersion.toString();
        }
    }

    protected String normalizeLocation(String location) {
        if (location.indexOf(':')<0)
            location="file:" + location;
        return location;
    }


    /**
     * Returns all installed bundles
     * @return all installed bundles including system bundles
     */
    public Bundle[] getBundles() {
        return context.getBundles();
    }


    /**
     * Shutdown the container
     * @throws Exception
     */
    public void shutdown() throws Exception{
        // Shut down the felix framework when stopping the
        // host application.
        felix.stop();
    }

    public Object getService(String serviceName) {
        ServiceReference serviceReference = context.getServiceReference(serviceName);
        if (serviceReference!=null) {
            return context.getService(serviceReference);
        }
        return null;
    }

    public Object getService(String serviceName,String filter) {
        try {
            ServiceReference[] serviceReferences = context.getServiceReferences(serviceName,filter);
            if (serviceReferences!=null) {
                return context.getService(serviceReferences[0]);
            }
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException("Could not get service by service name:"+serviceName+" and filter:"+filter+" "+e.getMessage(),e);
        }
        return null;
    }


    public Object getBundleService(String symbolicBundleName,String serviceName) {
        Bundle bundle=getBundle(symbolicBundleName);
        if (bundle==null) return null;
        return getBundleService(bundle,serviceName);
    }

    public Object getBundleService(Bundle bundle,String serviceName) {
        if (bundle.getBundleContext()==null) {
            return null;
        }
        ServiceReference serviceReference=bundle.getBundleContext().getServiceReference(serviceName);
        if (serviceReference==null)
            return null;
        try {
            return bundle.getBundleContext().getService(serviceReference);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }    
}
