package com.termux.app.features;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class CommandStoreTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private CommandStore mStore;
    private File mStorageDir;

    @Before
    public void setUp() throws IOException {
        mStorageDir = tempFolder.newFolder("termux_store_test");
        mStore = CommandStore.createForTesting();
    }

    @Test
    public void testHistoryCap100() {
        for (int i = 1; i <= 120; i++) {
            mStore.addHistoryCommand("cmd_" + i, mStorageDir);
        }

        List<CommandItem> history = mStore.getHistoryList();
        Assert.assertEquals(100, history.size());
        // Most recent should be cmd_120 at index 0
        Assert.assertEquals("cmd_120", history.get(0).getCommand());
        // Oldest preserved should be cmd_21 (since 1..20 evicted)
        Assert.assertEquals("cmd_21", history.get(99).getCommand());
    }

    @Test
    public void testEvictionSkipsPinned() {
        // Add 50 commands
        for (int i = 1; i <= 50; i++) {
            mStore.addHistoryCommand("cmd_" + i, mStorageDir);
        }

        // Pin cmd_1 (which is currently the oldest at the end)
        List<CommandItem> history = mStore.getHistoryList();
        CommandItem oldest = history.get(history.size() - 1);
        Assert.assertEquals("cmd_1", oldest.getCommand());
        boolean pinned = mStore.togglePin(oldest, mStorageDir);
        Assert.assertTrue(pinned);
        Assert.assertTrue(oldest.isPinned());

        // Now add another 70 commands (total added: 120, cap is 100)
        for (int i = 51; i <= 120; i++) {
            mStore.addHistoryCommand("cmd_" + i, mStorageDir);
        }

        List<CommandItem> currentHistory = mStore.getHistoryList();
        Assert.assertEquals(100, currentHistory.size());

        // Verify cmd_1 is STILL present in history because it was pinned!
        boolean foundCmd1 = false;
        for (CommandItem item : currentHistory) {
            if ("cmd_1".equals(item.getCommand())) {
                foundCmd1 = true;
                Assert.assertTrue(item.isPinned());
                break;
            }
        }
        Assert.assertTrue("Pinned command cmd_1 must not be evicted", foundCmd1);

        // Check that an unpinned command like cmd_2 WAS evicted
        boolean foundCmd2 = false;
        for (CommandItem item : currentHistory) {
            if ("cmd_2".equals(item.getCommand())) {
                foundCmd2 = true;
                break;
            }
        }
        Assert.assertFalse("Unpinned command cmd_2 should have been evicted", foundCmd2);
    }

    @Test
    public void testDeduplication() {
        mStore.addHistoryCommand("ls -la", mStorageDir);
        mStore.addHistoryCommand("cd ~", mStorageDir);
        mStore.addHistoryCommand("git status", mStorageDir);

        Assert.assertEquals(3, mStore.getHistoryList().size());
        Assert.assertEquals("git status", mStore.getHistoryList().get(0).getCommand());

        // Pin 'ls -la'
        CommandItem lsItem = mStore.getHistoryList().get(2);
        Assert.assertEquals("ls -la", lsItem.getCommand());
        mStore.togglePin(lsItem, mStorageDir);
        Assert.assertTrue(lsItem.isPinned());

        // Re-run 'ls -la'
        long beforeTime = System.currentTimeMillis();
        boolean changed = mStore.addHistoryCommand("ls -la", mStorageDir);
        Assert.assertTrue(changed);

        List<CommandItem> after = mStore.getHistoryList();
        Assert.assertEquals(3, after.size()); // Size must NOT increase
        Assert.assertEquals("ls -la", after.get(0).getCommand()); // Moved to front
        Assert.assertTrue("Pin state must be preserved", after.get(0).isPinned());
        Assert.assertTrue(after.get(0).getTimestamp() >= beforeTime);
    }

    @Test
    public void testMax5PinsLimit() {
        mStore.addHistoryCommand("cmd_1", mStorageDir);
        mStore.addHistoryCommand("cmd_2", mStorageDir);
        mStore.addHistoryCommand("cmd_3", mStorageDir);
        mStore.addTemplate("T1", "echo 1", mStorageDir);
        mStore.addTemplate("T2", "echo 2", mStorageDir);
        mStore.addTemplate("T3", "echo 3", mStorageDir);

        List<CommandItem> history = mStore.getHistoryList();
        List<CommandItem> templates = mStore.getTemplatesList();

        // Pin 3 history items
        Assert.assertTrue(mStore.togglePin(history.get(0), mStorageDir));
        Assert.assertTrue(mStore.togglePin(history.get(1), mStorageDir));
        Assert.assertTrue(mStore.togglePin(history.get(2), mStorageDir));

        // Pin 2 template items (Total = 5 pins)
        Assert.assertTrue(mStore.togglePin(templates.get(0), mStorageDir));
        Assert.assertTrue(mStore.togglePin(templates.get(1), mStorageDir));

        Assert.assertEquals(5, mStore.countTotalPins());

        // Attempt to pin a 6th item (templates.get(2)) -> MUST FAIL
        boolean pin6 = mStore.togglePin(templates.get(2), mStorageDir);
        Assert.assertFalse("6th pin must be rejected", pin6);
        Assert.assertFalse(templates.get(2).isPinned());
        Assert.assertEquals(5, mStore.countTotalPins());

        // Unpin one item
        Assert.assertTrue(mStore.togglePin(history.get(0), mStorageDir));
        Assert.assertFalse(history.get(0).isPinned());
        Assert.assertEquals(4, mStore.countTotalPins());

        // Now pinning the 6th item should succeed
        Assert.assertTrue(mStore.togglePin(templates.get(2), mStorageDir));
        Assert.assertTrue(templates.get(2).isPinned());
        Assert.assertEquals(5, mStore.countTotalPins());
    }

    @Test
    public void testClearHistoryKeepsPinned() {
        mStore.addHistoryCommand("cmd_unpinned_1", mStorageDir);
        mStore.addHistoryCommand("cmd_pinned_1", mStorageDir);
        mStore.addHistoryCommand("cmd_unpinned_2", mStorageDir);
        mStore.addHistoryCommand("cmd_pinned_2", mStorageDir);

        List<CommandItem> history = mStore.getHistoryList();
        mStore.togglePin(history.get(0), mStorageDir); // cmd_pinned_2
        mStore.togglePin(history.get(2), mStorageDir); // cmd_pinned_1

        Assert.assertEquals(4, mStore.getHistoryList().size());

        mStore.clearHistoryKeepPinned(mStorageDir);

        List<CommandItem> remaining = mStore.getHistoryList();
        Assert.assertEquals(2, remaining.size());
        for (CommandItem item : remaining) {
            Assert.assertTrue(item.isPinned());
        }
    }

    @Test
    public void testPersistence() {
        mStore.addHistoryCommand("git commit -m 'feat'", mStorageDir);
        mStore.addHistoryCommand("docker run hello-world", mStorageDir);
        mStore.addTemplate("Deploy script", "./deploy.sh --prod", mStorageDir);

        // Pin one history and one template
        mStore.togglePin(mStore.getHistoryList().get(0), mStorageDir);
        mStore.togglePin(mStore.getTemplatesList().get(0), mStorageDir);

        // Create a new store instance and load from the same directory
        CommandStore restoredStore = CommandStore.createForTesting();
        restoredStore.load(mStorageDir);

        List<CommandItem> restoredHistory = restoredStore.getHistoryList();
        List<CommandItem> restoredTemplates = restoredStore.getTemplatesList();

        Assert.assertEquals(2, restoredHistory.size());
        Assert.assertEquals("docker run hello-world", restoredHistory.get(0).getCommand());
        Assert.assertTrue(restoredHistory.get(0).isPinned());
        Assert.assertEquals("git commit -m 'feat'", restoredHistory.get(1).getCommand());
        Assert.assertFalse(restoredHistory.get(1).isPinned());

        Assert.assertFalse(restoredTemplates.isEmpty());
        CommandItem deployTemplate = null;
        for (CommandItem item : restoredTemplates) {
            if ("Deploy script".equals(item.getTitle())) {
                deployTemplate = item;
                break;
            }
        }
        Assert.assertNotNull(deployTemplate);
        Assert.assertEquals("./deploy.sh --prod", deployTemplate.getCommand());
        Assert.assertTrue(deployTemplate.isPinned());
    }
}
