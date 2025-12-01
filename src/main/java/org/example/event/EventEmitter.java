package org.example.event;

import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

public class EventEmitter {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public synchronized void emit(WorkflowEvent event) {

        StringJoiner sj = new StringJoiner(" | ");

        sj.add(ISO.format(event.getTimeStamp()));

        sj.add(event.getEventType().name());

        sj.add("run=" + event.getRunId());

        event.getJobId().ifPresentOrElse(
                id -> sj.add("job=" + id),
                () -> sj.add("job=-")
        );

        event.getStepNumber().ifPresentOrElse(
                step -> sj.add("step=" + step),
                () -> sj.add("step=-")
        );

        event.getBranch().ifPresentOrElse(
                b -> sj.add("branch=" + b),
                () -> sj.add("branch=-")
        );

        event.getShaShort().ifPresentOrElse(
                sha -> sj.add("sha=" + sha),
                () -> sj.add("sha=-")
        );

        event.getStatus().ifPresentOrElse(
                st -> sj.add("status=" + st),
                () -> sj.add("status=-")
        );

        event.getMessage().ifPresentOrElse(
                msg -> sj.add("msg=\"" + escape(msg) + "\""),
                () -> sj.add("msg=-")
        );

        System.out.println(sj.toString());

    }

    private String escape(String s) {
        return s.replace("\"", "\\\"");
    }
}
