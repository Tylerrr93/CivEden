package net.civmc.kitpvp.arena;

import com.infernalsuite.asp.api.exceptions.UnknownWorldException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class FileSlimeLoader implements SlimeLoader {

    private static final String EXTENSION = ".slime";

    private final File worldDir;

    public FileSlimeLoader(File worldDir) {
        this.worldDir = worldDir;
        worldDir.mkdirs();
    }

    @Override
    public byte[] readWorld(String worldName) throws UnknownWorldException, IOException {
        File file = worldFile(worldName);
        if (!file.exists()) {
            throw new UnknownWorldException(worldName);
        }
        return Files.readAllBytes(file.toPath());
    }

    @Override
    public boolean worldExists(String worldName) throws IOException {
        return worldFile(worldName).exists();
    }

    @Override
    public List<String> listWorlds() throws IOException {
        List<String> names = new ArrayList<>();
        File[] files = worldDir.listFiles((dir, name) -> name.endsWith(EXTENSION));
        if (files != null) {
            for (File f : files) {
                names.add(f.getName().substring(0, f.getName().length() - EXTENSION.length()));
            }
        }
        return names;
    }

    @Override
    public void saveWorld(String worldName, byte[] serializedWorld) throws IOException {
        Files.write(worldFile(worldName).toPath(), serializedWorld);
    }

    @Override
    public void deleteWorld(String worldName) throws UnknownWorldException, IOException {
        File file = worldFile(worldName);
        if (!file.exists()) {
            throw new UnknownWorldException(worldName);
        }
        Files.delete(file.toPath());
    }

    private File worldFile(String worldName) {
        return new File(worldDir, worldName + EXTENSION);
    }
}