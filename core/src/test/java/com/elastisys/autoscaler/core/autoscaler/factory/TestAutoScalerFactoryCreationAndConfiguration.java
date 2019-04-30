package com.elastisys.autoscaler.core.autoscaler.factory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.elastisys.autoscaler.core.addon.FakeAddon;
import com.elastisys.autoscaler.core.api.types.ServiceStatus.State;
import com.elastisys.scale.commons.util.collection.Maps;
import com.elastisys.scale.commons.util.file.FileUtils;

/**
 * Exercises the logic involved in creating and configuring an
 * {@link AutoScalerFactory}.
 */
public class TestAutoScalerFactoryCreationAndConfiguration {
    /** Storage directory path to use throughout the tests. */
    private static final String storageDirPath = "target/instances";
    /** Storage directory to use throughout the tests. */
    private static final File storageDir = new File(storageDirPath);

    @Before
    public void onSetup() {
        assertThat(storageDir.exists(), is(false));
    }

    @After
    public void onTearDown() throws IOException {
        // make sure storage dir is deleted between every test method
        FileUtils.deleteRecursively(storageDir);
    }

    @Test
    public void createFactoryWithValidStorageDir() {
        storageDir.mkdirs();
        assertThat(storageDir.exists(), is(true));

        AutoScalerFactoryConfig config = new AutoScalerFactoryConfig(storageDir.getAbsolutePath(), null);
        AutoScalerFactory factory = AutoScalerFactory.launch(config);
        assertThat(factory, is(not(nullValue())));
        assertThat(factory.getConfiguration(), is(config));
        assertThat(factory.getStatus().getState(), is(State.STARTED));
        assertTrue(factory.getConfiguration().getAddonSubsytems().isEmpty());
        assertThat(storageDir.exists(), is(true));
    }

    @Test
    public void createFactoryWithValidAddonSubsystems() {
        storageDir.mkdirs();
        assertThat(storageDir.exists(), is(true));

        Map<String, String> addons = Maps.of("extSubsystem", FakeAddon.class.getName());
        AutoScalerFactoryConfig config = new AutoScalerFactoryConfig(storageDir.getAbsolutePath(), addons);
        AutoScalerFactory factory = AutoScalerFactory.launch(config);
        assertThat(factory, is(not(nullValue())));
        assertThat(factory.getConfiguration(), is(config));
        assertThat(factory.getStatus().getState(), is(State.STARTED));
        assertTrue(!factory.getConfiguration().getAddonSubsytems().isEmpty());
        assertTrue(factory.getConfiguration().getAddonSubsytems().containsKey("extSubsystem"));
        assertThat(factory.getConfiguration().getAddonSubsytems().get("extSubsystem"), is(FakeAddon.class.getName()));
        assertThat(storageDir.exists(), is(true));
    }

    /**
     * When an add-on subsystem implementation class is specified, an error
     * should be raised.
     */
    @Test(expected = IllegalArgumentException.class)
    public void createFactoryWithInvalidAddonSubsystems() {
        storageDir.mkdirs();
        assertThat(storageDir.exists(), is(true));
        // class does not exist
        Map<String, String> addons = Maps.of("extSubsystem", "bad.ImplClass");

        AutoScalerFactoryConfig config = new AutoScalerFactoryConfig(storageDir.getAbsolutePath(), addons);
        // should fail validation
        AutoScalerFactory.launch(config);
    }

    /**
     * Verifies that storage directory is properly created if it doesn't already
     * exist.
     *
     * @throws ConfigurationException
     */
    @Test
    public void createFactoryWithNonExistingStorageDir() {
        assertThat(storageDir.exists(), is(false));

        AutoScalerFactoryConfig config = new AutoScalerFactoryConfig(storageDir.getAbsolutePath(), null);
        AutoScalerFactory factory = AutoScalerFactory.launch(config);
        assertThat(factory, is(not(nullValue())));
        assertThat(factory.getConfiguration(), is(config));
        assertThat(factory.getStatus().getState(), is(State.STARTED));

        assertThat(storageDir.exists(), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createFactoryWithStorageDirPathThatRefersToAFile() throws Exception {
        storageDir.createNewFile();
        AutoScalerFactory.launch(new AutoScalerFactoryConfig(storageDirPath, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createFactoryWithNonExistingStorageDirThatCannotBeCreated() throws Exception {
        File dir = new File("/root/dir");
        assertThat(dir.exists(), is(false));
        AutoScalerFactory.launch(new AutoScalerFactoryConfig(dir.getAbsolutePath(), null));
    }

    @Test(expected = NullPointerException.class)
    public void createConfigWithNullPath() {
        new AutoScalerFactoryConfig(null, null);
    }
}
