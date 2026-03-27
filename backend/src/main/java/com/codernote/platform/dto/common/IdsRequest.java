package com.codernote.platform.dto.common;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class IdsRequest {

    @NotEmpty(message = "id list cannot be empty")
    private List<Long> ids;
}
