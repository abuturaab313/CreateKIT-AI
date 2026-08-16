package com.example.data.model

enum class ToolCategory(val displayName: String) {
    AI_MAGIC("AI Magic"),
    OPTIMIZE("Optimize"),
    CREATOR_STUDIO("Creator Studio"),
    VIDEO_AUDIO("Video & Audio"),
    DOCUMENTS("Documents")
}

enum class ToolType(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val category: ToolCategory,
    val badge: String? = null,
    val isAi: Boolean = false,
    val requiresVideo: Boolean = false
) {
    ENHANCE(
        title = "AI Enhance",
        subtitle = "Auto, face, low-light & 2x upscale",
        emoji = "✨",
        category = ToolCategory.AI_MAGIC,
        badge = "AI",
        isAi = true
    ),
    BACKGROUND_REMOVER(
        title = "Remove Background",
        subtitle = "Transparent PNG or custom backdrop",
        emoji = "🪄",
        category = ToolCategory.AI_MAGIC,
        badge = "AI",
        isAi = true
    ),
    OBJECT_REMOVER(
        title = "Magic Object Eraser",
        subtitle = "Smart brush inpainting eraser",
        emoji = "🧹",
        category = ToolCategory.AI_MAGIC,
        badge = "AI",
        isAi = true
    ),
    COMPRESS(
        title = "Image Compressor",
        subtitle = "Reduce MBs with high fidelity",
        emoji = "🗜️",
        category = ToolCategory.OPTIMIZE,
        badge = "FAST"
    ),
    RESIZE(
        title = "Resize & Canvas Crop",
        subtitle = "YouTube, Insta, TikTok & custom",
        emoji = "📐",
        category = ToolCategory.OPTIMIZE
    ),
    THUMBNAIL_MAKER(
        title = "Thumbnail Maker",
        subtitle = "Layers, text, gradients & stickers",
        emoji = "🎨",
        category = ToolCategory.CREATOR_STUDIO,
        badge = "POPULAR"
    ),
    AUTO_CAPTION(
        title = "Auto Captions & Subtitles",
        subtitle = "Speech-to-text burning & animated styles",
        emoji = "💬",
        category = ToolCategory.VIDEO_AUDIO,
        badge = "AI",
        isAi = true,
        requiresVideo = true
    ),
    IMAGE_TO_PDF(
        title = "Image to PDF",
        subtitle = "Multi-page high quality PDF export",
        emoji = "📄",
        category = ToolCategory.DOCUMENTS
    ),
    VIDEO_COMPRESSOR(
        title = "Video Compressor",
        subtitle = "Inspect, bitrate & optimize MP4",
        emoji = "🎬",
        category = ToolCategory.VIDEO_AUDIO,
        requiresVideo = true
    ),
    FORMAT_CONVERTER(
        title = "Format Converter",
        subtitle = "Convert JPG, PNG, WEBP & PDF",
        emoji = "🔄",
        category = ToolCategory.DOCUMENTS
    );

    companion object {
        val AI_ENHANCE get() = ENHANCE
        val REMOVE_BACKGROUND get() = BACKGROUND_REMOVER
        val REMOVE_OBJECT get() = OBJECT_REMOVER
        val IMAGE_COMPRESSOR get() = COMPRESS
        val RESIZE_TOOL get() = RESIZE
    }
}

enum class IntentPreset(
    val title: String,
    val emoji: String,
    val recommendedTools: List<ToolType>,
    val defaultDimensions: String
) {
    YOUTUBE_VIDEO(
        title = "YouTube Video",
        emoji = "🎬",
        recommendedTools = listOf(ToolType.THUMBNAIL_MAKER, ToolType.VIDEO_COMPRESSOR, ToolType.AUTO_CAPTION),
        defaultDimensions = "1920 × 1080 (16:9)"
    ),
    SHORT_REEL(
        title = "Short / Reel",
        emoji = "📱",
        recommendedTools = listOf(ToolType.AUTO_CAPTION, ToolType.RESIZE, ToolType.ENHANCE),
        defaultDimensions = "1080 × 1920 (9:16)"
    ),
    INSTAGRAM_POST(
        title = "Instagram Post",
        emoji = "🖼️",
        recommendedTools = listOf(ToolType.BACKGROUND_REMOVER, ToolType.RESIZE, ToolType.COMPRESS),
        defaultDimensions = "1080 × 1080 (1:1)"
    ),
    THUMBNAIL(
        title = "Thumbnail",
        emoji = "🎨",
        recommendedTools = listOf(ToolType.THUMBNAIL_MAKER, ToolType.ENHANCE, ToolType.BACKGROUND_REMOVER),
        defaultDimensions = "1280 × 720 (16:9)"
    ),
    PROFILE_PICTURE(
        title = "Profile Picture",
        emoji = "👤",
        recommendedTools = listOf(ToolType.ENHANCE, ToolType.BACKGROUND_REMOVER, ToolType.RESIZE),
        defaultDimensions = "500 × 500 (1:1)"
    ),
    WHATSAPP(
        title = "WhatsApp Status",
        emoji = "📲",
        recommendedTools = listOf(ToolType.COMPRESS, ToolType.RESIZE, ToolType.VIDEO_COMPRESSOR),
        defaultDimensions = "1080 × 1920"
    ),
    CUSTOM(
        title = "Custom Project",
        emoji = "⚙️",
        recommendedTools = listOf(ToolType.ENHANCE, ToolType.COMPRESS, ToolType.FORMAT_CONVERTER),
        defaultDimensions = "Custom sizing"
    );

    val icon: String get() = emoji
}
