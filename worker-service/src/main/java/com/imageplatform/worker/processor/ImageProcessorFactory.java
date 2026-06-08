package com.imageplatform.worker.processor;

import com.imageplatform.common.event.JobCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageProcessorFactory {

    private final ResizeProcessor resizeProcessor;

    public String process(JobCreatedEvent event) {
        return switch (event.getOperationType()) {
            case RESIZE     -> resizeProcessor.process(event);
            case THUMBNAIL  -> resizeProcessor.thumbnail(event);
            default         -> throw new UnsupportedOperationException(
                    "Operation not yet implemented: " + event.getOperationType());
        };
    }
}
