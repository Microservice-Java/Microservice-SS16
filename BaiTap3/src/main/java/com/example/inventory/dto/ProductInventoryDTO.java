package com.example.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductInventoryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String productId;
    private String productName;
    private Integer quantity;
}
