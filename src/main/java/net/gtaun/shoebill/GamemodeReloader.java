package net.gtaun.shoebill;

import net.gtaun.shoebill.event.resource.ResourceLoadEvent;
import net.gtaun.shoebill.resource.Gamemode;
import net.gtaun.shoebill.resource.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/**
 * Created by marvin on 03.06.15 in project shoebill-gamemode-reloader.
 * Copyright (c) 2015 Marvin Haschker. All rights reserved.
 */
public class GamemodeReloader extends Plugin {

    private Thread watcherThread;

    @Override
    protected void onEnable() throws Throwable {
        getEventManager().registerHandler(ResourceLoadEvent.class, event -> {
            if (event.getResource() instanceof Gamemode) {
                Gamemode gamemode = (Gamemode) event.getResource();
                if (watcherThread == null) {
                    try {
                        File file = gamemode.getDescription().getFile();
                        File dir = file.getParentFile();
                        WatchService watchService = FileSystems.getDefault().newWatchService();
                        dir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                        watcherThread = new Thread(() -> {
                            try {
                                while (true) {
                                    WatchKey key = watchService.take();
                                    for (WatchEvent ev : key.pollEvents()) {
                                        WatchEvent<Path> watchEvent = (WatchEvent<Path>) ev;
                                        Path path = watchEvent.context();
                                        if (path.toFile().getName().equals(file.getName())) {
                                            Thread.sleep(500); //Sleep for half a second to allow the filewriting to finish.
                                            gamemode.getLogger().info("Gamemode has been modified. Reloading...");
                                            Shoebill.get().runOnSampThread(() -> Shoebill.get().reload());
                                            return;
                                        }
                                    }
                                }
                            } catch (InterruptedException ignored) {
                            }
                        });
                        watcherThread.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onDisable() throws Throwable {
        watcherThread.interrupt();
        watcherThread = null;
    }
}
