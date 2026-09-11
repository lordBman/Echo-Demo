package io.bsoft.echo.desktop.tools;

import io.bsoft.echo.audio.SoundId;
import io.bsoft.echo.audio.SoundSynth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes every {@link SoundId} as a WAV file into the given directory. */
public final class GenerateSounds {

    private GenerateSounds() {
    }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of(args.length > 0 ? args[0] : "assets/sounds");
        Files.createDirectories(dir);
        for (SoundId id : SoundId.values()) {
            Path file = dir.resolve(id.name().toLowerCase() + ".wav");
            Files.write(file, SoundSynth.generate(id));
            System.out.println("wrote " + file);
        }
    }
}
