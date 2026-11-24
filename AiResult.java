package com.astrax.ai;

/** Kết quả một lần gọi API AI: thành công (có text) hoặc thất bại (có loại lỗi). */
public record AiResult(boolean success, String text, Type error, String detail) {

    public enum Type {
        NO_KEY,
        INVALID_KEY,
        QUOTA,
        RATE_LIMIT,
        TIMEOUT,
        SERVER,
        NETWORK,
        BAD_REQUEST,
        BAD_RESPONSE,
        EMPTY,
        INSECURE_URL
    }

    public static AiResult ok(String text) {
        return new AiResult(true, text, null, "");
    }

    public static AiResult fail(Type type, String detail) {
        return new AiResult(false, "", type, detail == null ? "" : detail);
    }

    /** Thông báo thân thiện gửi cho người chơi (không chứa chi tiết kỹ thuật). */
    public String userMessage() {
        if (success || error == null) {
            return "";
        }
        return switch (error) {
            case NO_KEY -> "Trợ lý AI chưa được cấu hình API Key. Hãy báo cho admin.";
            case INVALID_KEY -> "Dịch vụ AI đang gặp lỗi cấu hình (API Key không hợp lệ). Hãy báo cho admin.";
            case QUOTA -> "Dịch vụ AI đã hết hạn mức sử dụng. Hãy báo cho admin.";
            case RATE_LIMIT -> "Dịch vụ AI đang quá tải yêu cầu, hãy thử lại sau ít phút.";
            case TIMEOUT -> "AI phản hồi quá lâu, hãy thử lại sau nhé.";
            case SERVER -> "Dịch vụ AI đang bận hoặc gặp sự cố, hãy thử lại sau.";
            case NETWORK -> "Không kết nối được tới dịch vụ AI, hãy thử lại sau.";
            case INSECURE_URL -> "API URL phải dùng HTTPS. Hãy báo cho admin.";
            case BAD_REQUEST -> "Yêu cầu gửi tới AI không hợp lệ. Hãy báo cho admin nếu lỗi lặp lại.";
            case BAD_RESPONSE, EMPTY -> "AI trả về phản hồi không hợp lệ, hãy thử lại.";
        };
    }
}
