package net.adoptopenjdk.icedteaweb.impl;

import dev.rico.core.functional.Subscription;
import dev.rico.internal.core.Assert;
import net.adoptopenjdk.icedteaweb.Application;
import net.adoptopenjdk.icedteaweb.ApplicationManager;
import net.sourceforge.jnlp.JNLPFile;
import net.sourceforge.jnlp.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ApplicationManagerImpl implements ApplicationManager {

    private final static Logger LOG = LoggerFactory.getLogger(ApplicationManagerImpl.class);

    private final List<Consumer<List<Application>>> changeListeners = new CopyOnWriteArrayList<>();

    private final List<Application> applications = new CopyOnWriteArrayList<>();

    private final Executor executor;

    @Deprecated
    private final Launcher launcher = new Launcher(false);

    public ApplicationManagerImpl(final Executor executor) {
        this.executor = Assert.requireNonNull(executor, "executor");
    }

    @Override
    public boolean contains(final Application application) {
        return applications.contains(application);
    }

    @Override
    public List<Application> getInstalledApplications() {
        return Collections.unmodifiableList(applications);
    }

    @Override
    public Subscription addChangeListener(final Consumer<List<Application>> changeListener) {
        Assert.requireNonNull(changeListener, "changeListener");
        changeListeners.add(changeListener);
        return () -> changeListeners.remove(changeListener);
    }

    private Application installSync(final URL url) {
        try {
            final JNLPFile file = new JNLPFile(url);
            final Application application = new ApplicationImpl(file, null);
            applications.add(application);
        } catch (Exception e) {
            LOG.error("Can not install application based on url {}", url);
            throw new RuntimeException("Error in installing application", e);
        }

        throw new RuntimeException("Not yet implemented");
    }

    private void removeSync(final Application application) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public CompletableFuture<Void> remove(final Application application) {
        return CompletableFuture.runAsync(() -> removeSync(application), executor);
    }

    @Override
    public CompletableFuture<Application> install(final URL url) {
        return CompletableFuture.supplyAsync(() -> installSync(url), executor);
    }

    @Override
    public CompletableFuture<Void> clear() {
        final CompletableFuture<Void>[] deleteTasks = getInstalledApplications().stream().
                map(a -> remove(a)).
                collect(Collectors.toList()).
                toArray(new CompletableFuture[0]);
        return CompletableFuture.allOf(deleteTasks);
    }
}
