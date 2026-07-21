package com.imageplatform.worker.processor;

import com.imageplatform.common.event.JobCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class ResizeProcessor {

    @Value("${file.output-dir}")
    private String outputDir;

    public String process(JobCreatedEvent event) {
        java.util.Map<String, String> params = event.getParameters() != null
                ? event.getParameters() : java.util.Collections.emptyMap();
        int width  = Integer.parseInt(params.getOrDefault("width",  "800"));
        int height = Integer.parseInt(params.getOrDefault("height", "600"));

        Path input  = Paths.get(event.getFilePath());
        Path output = Paths.get(outputDir, event.getJobId() + "_resized.jpg");

        try {
            Thumbnails.of(input.toFile()).size(width, height).toFile(output.toFile());
            log.info("Resized {} to {}x{}", event.getJobId(), width, height);
            return output.toString();
        } catch (IOException e) {
            throw new RuntimeException("Resize failed for job: " + event.getJobId(), e);
        }
    }

    public String thumbnail(JobCreatedEvent event) {
        Path input  = Paths.get(event.getFilePath());
        Path output = Paths.get(outputDir, event.getJobId() + "_thumb.jpg");

        try {
            Thumbnails.of(input.toFile()).size(150, 150).toFile(output.toFile());
            return output.toString();
        } catch (IOException e) {
            throw new RuntimeException("Thumbnail failed for job: " + event.getJobId(), e);
        }
    }
}
