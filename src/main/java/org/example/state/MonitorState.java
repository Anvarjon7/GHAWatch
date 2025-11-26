package org.example.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.monitor.state.RunSnapshot;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MonitorState {

    @JsonProperty("last_processed_run_id")
    private long lastProcessedRunId;

    private Map<Long, RunSnapshot> runSnapshots = new HashMap<>();
    public MonitorState(long lastProcessedRunId) {
        this.lastProcessedRunId = lastProcessedRunId;
    }

   public void updateSnapshot(long runId, RunSnapshot snapshot){
        runSnapshots.put(runId,snapshot);
   }
}
