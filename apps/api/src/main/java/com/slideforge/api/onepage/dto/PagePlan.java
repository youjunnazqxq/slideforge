package com.slideforge.api.onepage.dto;

import java.util.List;

public record PagePlan(
        String slideTitle,
        String coreMessage,
        String audienceTakeaway,
        List<ContentBlock> contentBlocks,
        String speakerIntent,
        String layoutIntent,
        String visualStyle
) {

    public record ContentBlock(
            String id,
            String role,
            String type,
            String title,
            String content,
            List<String> sourceIds
    ) {
    }
}
