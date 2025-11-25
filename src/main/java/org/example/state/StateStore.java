package org.example.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class StateStore {

    private final Logger log = LoggerFactory.getLogger(StateStore.class);

    private final ObjectMapper mapper;

    private final File stateFile;

    public StateStore(String stateFile) {
        this.stateFile = new File(stateFile);
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);

        ensureDirectoryExist();
    }

    private void ensureDirectoryExist() {

        File parent = stateFile.getParentFile();
        if (parent != null && !parent.exists()) {
            if (parent.mkdirs()) {
                log.info("Created state directory {}", parent.getAbsolutePath());
            }
        }
    }

    public MonitorState load() {
        if (!stateFile.exists()) {
            log.info("No state file found. Assuming first run.");

            return new MonitorState();
        }

        try {
            MonitorState state = mapper.readValue(stateFile, MonitorState.class);
            log.info("Loaded state: last_processed_run_id={}", state.getLastProcessedRunId());

            return state;
        } catch (Exception e) {
            log.info("Failed to read state file. Using default state.", e);
            return new MonitorState(0L);
        }
    }

    public void save(MonitorState state) {
        File tempFile = new File(stateFile.getAbsolutePath() + ".tmp");

        try {
            mapper.writeValue(tempFile, state);

            if (tempFile.renameTo(stateFile)) {
                log.info("State saved: {}", state.getLastProcessedRunId());
            } else {
                throw new IOException("Failed to rename temp file");
            }
        } catch (Exception e) {
            log.error("Failed to write state to file", e);
        }
    }
}
