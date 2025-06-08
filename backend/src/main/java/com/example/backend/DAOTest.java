package com.example.backend;

import com.example.backend.entities.*;
import com.example.backend.enums.AccountStatus;
import com.example.backend.repositories.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Date;
import java.util.UUID;

@SpringBootApplication
public class DAOTest {
   /* public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    CommandLineRunner start(CustomerRepository customerRepository,
                            BankAccountRepository bankAccountRepository,
                            OperationRepository operationRepository) {
        return args -> {
            // 🔹 Créer un client
            Customer customer = new Customer();
            customer.setName("Laila");
            customer.setEmail("laila@bank.com");
            customerRepository.save(customer);

            // 🔹 Créer un compte courant
            CurrentAccount currentAccount = new CurrentAccount();
            currentAccount.setId(UUID.randomUUID().toString());
            currentAccount.setBalance(5000);
            currentAccount.setCreatedAt(new Date());
            currentAccount.setStatus(AccountStatus.CREATED);
            currentAccount.setOverDraft(1000);
            currentAccount.setCustomer(customer);
            bankAccountRepository.save(currentAccount);

            // 🔹 Créer un compte épargne
            SavingsAccount savingsAccount = new SavingsAccount();
            savingsAccount.setId(UUID.randomUUID().toString());
            savingsAccount.setBalance(8000);
            savingsAccount.setCreatedAt(new Date());
            savingsAccount.setStatus(AccountStatus.CREATED);
            savingsAccount.setInterestRate(3.5);
            savingsAccount.setCustomer(customer);
            bankAccountRepository.save(savingsAccount);

            // 🔹 Afficher les comptes
            bankAccountRepository.findAll().forEach(acc -> {
                System.out.println("==============");
                System.out.println("ID: " + acc.getId());
                System.out.println("Balance: " + acc.getBalance());
                System.out.println("Customer: " + acc.getCustomer().getName());
                System.out.println("Type: " + acc.getClass().getSimpleName());
            });
        };
    }*/
}
