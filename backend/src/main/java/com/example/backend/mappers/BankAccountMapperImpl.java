package com.example.backend.mappers;

import com.example.backend.dtos.CurrentBankAccountDTO;
import com.example.backend.dtos.CustomerDTO;
import com.example.backend.dtos.OperationDTO;
import com.example.backend.dtos.SavingsBankAccountDTO;
import com.example.backend.entities.CurrentAccount;
import com.example.backend.entities.Customer;
import com.example.backend.entities.Operation;
import com.example.backend.entities.SavingsAccount;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class BankAccountMapperImpl {
    public CustomerDTO fromCustomer(Customer customer){
        CustomerDTO customerDTO = new CustomerDTO();
        BeanUtils.copyProperties(customer,customerDTO);
        return customerDTO;
    }

    public Customer fromCustomerDTO(CustomerDTO customerDTO){
        Customer customer = new Customer();
        BeanUtils.copyProperties(customerDTO, customer);
        return customer;

    }

    public SavingsBankAccountDTO fromSavingsBankAccount(SavingsAccount savingsAccount){
        SavingsBankAccountDTO savingsBankAccountDTO = new SavingsBankAccountDTO();
        BeanUtils.copyProperties(savingsAccount, savingsBankAccountDTO);
        savingsBankAccountDTO.setCustomerDTO(fromCustomer(savingsAccount.getCustomer()));
        savingsBankAccountDTO.setType(savingsAccount.getClass().getSimpleName());
        return savingsBankAccountDTO;
    }

    public SavingsAccount fromSavingsBankAccountDTO(SavingsBankAccountDTO savingsBankAccountDTO){
        SavingsAccount savingsAccount = new SavingsAccount();
        BeanUtils.copyProperties(savingsBankAccountDTO, savingsAccount);
        savingsAccount.setCustomer(fromCustomerDTO(savingsBankAccountDTO.getCustomerDTO()));
        return savingsAccount;
    }

    public CurrentBankAccountDTO fromCurrentBankAccount(CurrentAccount currentAccount){
        CurrentBankAccountDTO currentBankAccountDTO=new CurrentBankAccountDTO();
        BeanUtils.copyProperties(currentAccount,currentBankAccountDTO);
        currentBankAccountDTO.setCustomerDTO(fromCustomer(currentAccount.getCustomer()));
        currentBankAccountDTO.setType(currentAccount.getClass().getSimpleName());
        return currentBankAccountDTO;
    }
    public CurrentAccount fromCurrentBankAccountDTO(CurrentBankAccountDTO currentBankAccountDTO){
        CurrentAccount currentAccount=new CurrentAccount();
        BeanUtils.copyProperties(currentBankAccountDTO,currentAccount);
        currentAccount.setCustomer(fromCustomerDTO(currentBankAccountDTO.getCustomerDTO()));
        return currentAccount;
    }

    public OperationDTO fromAccountOperation(Operation accountOperation){
        OperationDTO accountOperationDTO=new OperationDTO();
        BeanUtils.copyProperties(accountOperation,accountOperationDTO);
        return accountOperationDTO;
    }



}