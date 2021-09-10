
package beast.app.util;

import jam.framework.Application;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.Utils6;

/**
 * Since Oracle Java 9, Mac OS specific <code>com.apple.eawt</code> was replaced
 * by <code>java.awt.desktop</code>.
 * This class based on Java 8 will load <code>java.awt.desktop</code>,
 * if Java 9 is used in runtime.
 * Then, <i>about</i> and <i>quit</i> menu item will work properly.
 * The code is inspired from both source code of
 * <a href="https://github.com/rambaut/jam-lib">jam</a> package
 * and <a href="http://www.keystore-explorer.org">KeyStore Explorer</a>.
 */
public class NewOSXAdapter implements InvocationHandler {
    private static NewOSXAdapter theAdapter;
    private Application application;

    public NewOSXAdapter(Application var1) {
        this.application = var1;
    }

    /**
     * Use <code>reflect</code> to load <code>AboutHandler</code> and <code>QuitHandler</code>,
     * to avoid Mac specific classes being required in Java 9 and later.
     * @param var0
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    public void registerMacOSXApplication(Application var0) throws ClassNotFoundException,
            NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        if (theAdapter == null) {
            theAdapter = new NewOSXAdapter(var0);
        }

        // using reflection to avoid Mac specific classes being required for compiling KSE on other platforms
        Class<?> applicationClass = BEASTClassLoader.forName("com.apple.eawt.Application");
        Class<?> quitHandlerClass;
        Class<?> aboutHandlerClass;
        Class<?> openFilesHandlerClass;
        Class<?> preferencesHandlerClass;

        if (Utils6.isMajorAtLeast(Utils6.JAVA_9)) {
            quitHandlerClass = BEASTClassLoader.forName("java.awt.desktop.QuitHandler");
            aboutHandlerClass = BEASTClassLoader.forName("java.awt.desktop.AboutHandler");
//            openFilesHandlerClass = BEASTClassLoader.forName("java.awt.desktop.OpenFilesHandler");
//            preferencesHandlerClass = BEASTClassLoader.forName("java.awt.desktop.PreferencesHandler");
        } else {
            quitHandlerClass = BEASTClassLoader.forName("com.apple.eawt.QuitHandler");
            aboutHandlerClass = BEASTClassLoader.forName("com.apple.eawt.AboutHandler");
//            openFilesHandlerClass = BEASTClassLoader.forName("com.apple.eawt.OpenFilesHandler");
//            preferencesHandlerClass = BEASTClassLoader.forName("com.apple.eawt.PreferencesHandler");
        }

        Object application = applicationClass.getConstructor((Class[]) null).newInstance((Object[]) null);
//        Object proxy = Proxy.newProxyInstance(NewOSXAdapter.class.getClassLoader(), new Class<?>[]{
//                quitHandlerClass, aboutHandlerClass, openFilesHandlerClass, preferencesHandlerClass}, this);
        Object proxy = Proxy.newProxyInstance(NewOSXAdapter.class.getClassLoader(), new Class<?>[]{
                quitHandlerClass, aboutHandlerClass}, this);

        applicationClass.getDeclaredMethod("setQuitHandler", quitHandlerClass).invoke(application, proxy);
        applicationClass.getDeclaredMethod("setAboutHandler", aboutHandlerClass).invoke(application, proxy);
//        applicationClass.getDeclaredMethod("setOpenFileHandler", openFilesHandlerClass).invoke(application, proxy);
//        applicationClass.getDeclaredMethod("setPreferencesHandler", preferencesHandlerClass).invoke(application, proxy);

    }


    /**
     * Only <i>about</i> and <i>quit</i> are implemented.
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("handleAbout".equals(method.getName())) {
            if (this.application != null) {
                this.application.doAbout();
            } else {
                throw new IllegalStateException("handleAbout: Application instance detached from listener");
            }
        } else if ("handleQuitRequestWith".equals(method.getName())) {
            if (this.application != null) {
                this.application.doQuit();
            } else {
                throw new IllegalStateException("handleQuit: Application instance detached from listener");
            }
        }
        return null;
    }


}
