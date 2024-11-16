package com.vattima.lego.data.service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ItemDto {
    private int itemId;
    private String itemNumber;
    private String itemName;
    private String notes;
    private Integer categoryId;
}
