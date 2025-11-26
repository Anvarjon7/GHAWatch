package org.example.monitor;

import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

public class EventEmitter {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public synchronized void emit(WorkflowEvent event){

        String ts = ISO.format(event.getTimeStamp());

        StringJoiner sj = new StringJoiner(" | ");
        sj.add(ts);
        sj.add(event.getEventType().name());
        sj.add("repo=" + event.getRepo());
        sj.add("run=" + event.getRunId());

        event.getJobId().ifPresent(j -> sj.add("job=" + j));
        if (!event.getJobId().isPresent()) sj.add("job=-");

        event.getStepNumber().ifPresent(s -> sj.add("step="+s));
        if (!event.getStepNumber().isPresent()) sj.add("step=-");

        event.getBranch().ifPresent(b -> sj.add("branch=" + b));
        if (!event.getBranch().isPresent()) sj.add("branch=-");

        event.getShaShort().ifPresent(s -> sj.add("sha=" + s));
        if (!event.getShaShort().isPresent()) sj.add("sha=-");

        event.getStatus().ifPresent(st -> sj.add("status=" + st));
        if (!event.getStatus().isPresent()) sj.add("status=-");

        event.getMessage().ifPresent(m -> sj.add("msg=\"" + escape(m) + "\""));
        if (!event.getMessage().isPresent()) sj.add("msg=-");

        System.out.println(sj.toString());
    }

    private String escape(String s) {
        return s.replace("\"", "\\\"");
    }
}
