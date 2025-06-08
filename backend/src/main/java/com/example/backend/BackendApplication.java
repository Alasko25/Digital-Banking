package com.example.backend;

import com.example.backend.entities.CurrentAccount;
import com.example.backend.entities.Customer;
import com.example.backend.entities.Operation;
import com.example.backend.entities.SavingsAccount;
import com.example.backend.enums.AccountStatus;
import com.example.backend.enums.OperationType;
import com.example.backend.repositories.BankAccountRepository;
import com.example.backend.repositories.CustomerRepository;
import com.example.backend.repositories.OperationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	CommandLineRunner start(CustomerRepository customerRepository,
							BankAccountRepository bankAccountRepository,
							OperationRepository operationRepository){
		return args -> {
			Stream.of("Hassan", "Yassine", "Aicha").forEach(name->{
				Customer customer = new Customer();
				customer.setName(name);
				customer.setEmail(name+"gmail.com");
				customerRepository.save(customer);
			});

			customerRepository.findAll().forEach(customer -> {
				// Current account
				CurrentAccount currentAccount = new CurrentAccount();
				currentAccount.setId(UUID.randomUUID().toString());
				currentAccount.setBalance(Math.random()*10000);
				currentAccount.setCreatedAt(new Date());
				currentAccount.setStatus(AccountStatus.CREATED);
				currentAccount.setCustomer(customer);
				currentAccount.setOverDraft(9000);
				bankAccountRepository.save(currentAccount);
				// Savings account
				SavingsAccount savingsAccount = new SavingsAccount();
				savingsAccount.setId(UUID.randomUUID().toString());
				savingsAccount.setBalance(Math.random()*10000);
				savingsAccount.setCreatedAt(new Date());
				savingsAccount.setStatus(AccountStatus.CREATED);
				savingsAccount.setCustomer(customer);
				savingsAccount.setInterestRate(1.2);
				bankAccountRepository.save(savingsAccount);
			});

			bankAccountRepository.findAll().forEach(account->{
				for(int i = 0; i<10; i++){
					Operation operation = new Operation();
					operation.setDate(new Date());
					operation.setAmount(Math.random()*10000);
					operation.setBankAccount(account);
					operation.setType(Math.random()>0.5? OperationType.DEBIT : OperationType.CREDIT);
					operationRepository.save(operation);
				}
			});
		};
	}
}
