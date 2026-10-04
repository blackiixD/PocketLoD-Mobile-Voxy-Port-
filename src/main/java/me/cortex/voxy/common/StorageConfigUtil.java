package me.cortex.voxy.common;

import me.cortex.voxy.common.config.Serialization;
import me.cortex.voxy.common.config.compressors.LZ4Compressor;
import me.cortex.voxy.common.config.compressors.ZSTDCompressor;
import me.cortex.voxy.common.config.section.SectionSerializationStorage;
import me.cortex.voxy.common.config.storage.inmemory.MemoryStorageBackend;
import me.cortex.voxy.common.config.storage.other.CompressionStorageAdaptor;
import me.cortex.voxy.common.config.storage.rocksdb.RocksDBStorageBackend;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class StorageConfigUtil {

    public static <T> T getCreateStorageConfig(Class<T> clz, Predicate<T> verifier, Supplier<T> defaultConfig, Path path) {
        try {
            Files.createDirectories(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var json = path.resolve("config.json");
        T config = null;
        if (Files.exists(json)) {
            try {
                config = Serialization.GSON.fromJson(Files.readString(json), clz);
                if (config == null) {
                    Logger.error("Config deserialization null, reverting to default");
                } else {
                    if (!verifier.test(config)) {
                        Logger.error("Config section storage null, reverting to default");
                        config = null;
                    }
                }
            } catch (Exception e) {
                Logger.error("Failed to load the storage configuration file, resetting it to default, this will probably break your save if you used a custom storage config", e);
            }
        }

        if (config == null) {
            config = defaultConfig.get();
        }

        try {
            Files.writeString(json, Serialization.GSON.toJson(config));
        } catch (Exception e) {
            throw new RuntimeException("Failed write the config, aborting!", e);
        }

        if (config == null) {
            throw new IllegalStateException("Config is still null\n");
        }
        return config;
    }

    // Detecta Android (Zalith/Pojav) sem depender de nenhuma lib nativa
    public static boolean isAndroid() {
        return System.getProperty("os.version", "").startsWith("Android")
                || System.getProperty("pojav.path.minecraft") != null
                || System.getenv("POJAV_LAUNCHER") != null;
    }

    public static SectionSerializationStorage.Config createDefaultSerializer() {
        var compression = new CompressionStorageAdaptor.Config();

        if (isAndroid()) {
            // Sem nativos do mod: dados so em memoria, compressor LZ4 (cai para Java puro)
            Logger.info("Android detectado, usando Memory + LZ4 (sem nativos)");
            compression.delegate = new MemoryStorageBackend.Config();
            compression.compressor = new LZ4Compressor.Config();
        } else {
            var baseDB = new RocksDBStorageBackend.Config();
            var compressor = new ZSTDCompressor.Config();
            compressor.compressionLevel = 1;
            compression.delegate = baseDB;
            compression.compressor = compressor;
        }

        var serializer = new SectionSerializationStorage.Config();
        serializer.storage = compression;
        return serializer;
    }
}
