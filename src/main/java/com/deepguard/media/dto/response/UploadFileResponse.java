package com.deepguard.media.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadFileResponse {

    private String publicUrl;
    private String storagePath;
    private String fileName;

}
