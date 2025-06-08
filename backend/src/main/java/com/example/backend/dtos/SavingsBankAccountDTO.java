package com.example.backend.dtos;

import com.example.backend.enums.AccountStatus;
import lombok.Data;

import java.util.Date;

@Data
public class SavingsBankAccountDTO extends BankAccountDTO{
    private String id;
    private Date createdAt;
    private double balance;
    private AccountStatus status;
    private String currency;
    private double interestRate;
    private CustomerDTO customerDTO;

}