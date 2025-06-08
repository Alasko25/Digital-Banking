package com.example.backend.dtos;

import com.example.backend.enums.OperationType;
import lombok.Data;

import java.util.Date;

@Data
public class OperationDTO {
    private Long id;
    private Date date;
    private double amount;
    private OperationType type;
    private String description;
}