# Chapter 4: Business Logic Layer (Services)

Welcome back to our `Digital-Banking` tutorial! In the [previous chapter](03_data_access_layer__repositories__.md), we learned about the **Data Access Layer (Repositories)** and how they provide a simple way for our application to talk to the database, fetching and saving our [Entities (Data Models)](01_entities__data_models__.md). Before that, we covered [Data Transfer Objects (DTOs)](02_data_transfer_objects__dtos__.md), the clean packages of data we use to move information around, especially to and from the frontend.

Now we have the database structure defined ([Entities](01_entities__data_models__.md)), the data messages ready ([DTOs](02_data_transfer_objects__dtos__.md)), and the tools to access the database ([Repositories](03_data_access_layer__repositories_.md)). But where does the actual *banking* happen? Where do we decide if a customer has enough money to withdraw? Where do we perform a transfer by debiting one account and crediting another?

This is the responsibility of the **Business Logic Layer**, often implemented using **Services**.

## What is the Business Logic Layer (Services)?

Think of the Business Logic Layer as the **brain** or the **command center** of our backend application. It's where the *rules* and *processes* of the banking system are implemented.

If the [Repositories](03_data_access_layer__repositories_.md) are like the workers who can *fetch* or *store* specific items in the database vault, the Services are the **managers** who tell those workers what to do, when, and how, according to the bank's rules.

The Service layer:

1.  **Contains the Core Logic:** This is where the code for operations like "deposit money," "withdraw money," "transfer funds," "create a new account," or "list a customer's accounts" lives.
2.  **Orchestrates Tasks:** A single banking operation might require multiple steps. For example, a transfer involves checking the source account, debiting it, crediting the destination account, and recording two operations. The Service layer coordinates all these steps.
3.  **Uses Repositories:** Services *don't* talk directly to the database themselves. They rely on the [Repositories](03_data_access_layer__repositories_.md) to do the low-level data access work.
4.  **Works with DTOs:** Services often receive input data as [DTOs](02_data_transfer_objects__dtos__.md) (like a `CreditDTO` for a deposit) and return results as [DTOs](02_data_transfer_objects__dtos__.md) (like an `AccountHistoryDTO`). They are responsible for converting data between [Entities](01_entities__data_models__.md) (used internally with Repositories) and [DTOs](02_data_transfer_objects__dtos__.md) (used for communication with other layers, like the API).
5.  **Handles Exceptions:** If something goes wrong (like trying to withdraw too much money), the Service layer is where this is detected and where specific errors ([Exceptions](https://docs.oracle.com/javase/tutorial/essential/exceptions/)) are thrown.

In Spring, these services are typically implemented as classes annotated with `@Service`.

## Our Banking Service: `BankAccountServiceImpl`

In our project, the main class handling banking operations is `BankAccountServiceImpl`. It *implements* the `BankAccountService` interface, which simply *declares* the available operations.

```java
// backend/src/main/java/com/example/backend/services/BankAccountService.java
// This is the interface - it defines WHAT operations are available

package com.example.backend.services;

import com.example.backend.dtos.*;
import com.example.backend.exeptions.BalanceNotSufficientException;
import com.example.backend.exeptions.BankAccountNotFoundException;
import com.example.backend.exeptions.CustomerNotFoundException;

import java.util.List;

public interface BankAccountService {
    // Methods like saveCustomer, saveCurrentBankAccount, debit, credit, transfer, etc.
    // It just lists the methods, it doesn't contain the code for HOW they work.

    CustomerDTO saveCostumer(CustomerDTO customerDTO);
    CurrentBankAccountDTO saveCurrentBankAccount(double initialBalance, Long customerId, double overDraft) throws CustomerNotFoundException;
    // ... other method declarations ...
    void debit(String accountId, double amount, String description) throws BankAccountNotFoundException, BalanceNotSufficientException;
    void credit(String accountId, double amount, String description) throws BankAccountNotFoundException;
    // ... many more methods ...
}
```

```java
// backend/src/main/java/com/example/backend/services/BankAccountServiceImpl.java
// This is the implementation - it contains the code for HOW the operations work

package com.example.backend.services;

import com.example.backend.dtos.*;
import com.example.backend.entities.*; // Uses Entities
import com.example.backend.enums.OperationType;
import com.example.backend.exeptions.BalanceNotSufficientException;
import com.example.backend.exeptions.BankAccountNotFoundException;
import com.example.backend.exeptions.CustomerNotFoundException;
import com.example.backend.mappers.BankAccountMapperImpl; // Uses a Mapper for DTOs
import com.example.backend.repositories.BankAccountRepository; // Uses Repositories
import com.example.backend.repositories.CustomerRepository;   // Uses Repositories
import com.example.backend.repositories.OperationRepository;   // Uses Repositories
import jakarta.transaction.Transactional; // Important for transactions
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service; // Marks this as a Service

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service // <-- This tells Spring this is a Service component
@Transactional // <-- Ensures database operations are handled properly
@AllArgsConstructor // <-- Lombok helps with dependency injection
@Slf4j // <-- For logging (useful for debugging)
public class BankAccountServiceImpl implements BankAccountService {

    // Services NEED Repositories and Mappers to do their job
    private CustomerRepository customerRepository;
    private BankAccountRepository bankAccountRepository;
    private OperationRepository operationRepository;
    private BankAccountMapperImpl dtoMapper; // Tool to convert Entity <-> DTO

    // The rest of the class contains the implementation of the methods
    // declared in the BankAccountService interface...
}
```

*   `@Service`: This Spring annotation tells the framework that this class is a Service component. Spring manages these components and can inject them into other components (like our API controllers later).
*   `@Transactional`: This is very important! It ensures that all database operations within a method are treated as a single unit. If any part of the method fails (e.g., a database error), all changes made during that method call are automatically undone (rolled back). This prevents inconsistent data (like debiting an account but failing to credit the other in a transfer).
*   `@AllArgsConstructor`: Thanks to Lombok, this annotation automatically creates a constructor that takes all the `private` fields (`customerRepository`, `bankAccountRepository`, etc.) as arguments. Spring uses this constructor to **inject** the required Repositories and the DTO Mapper when it creates an instance of `BankAccountServiceImpl`. This is Spring's way of providing the tools a Service needs.
*   `private CustomerRepository customerRepository;` etc.: These are the references to the [Repositories](03_data_access_layer__repositories_.md) and the DTO Mapper (`dtoMapper`). The Service layer *uses* these injected dependencies to perform its tasks. It calls methods on these objects.

## Use Case: Making a Deposit (`credit`)

Let's trace how the `credit` (deposit) operation works in the Service layer. This is a core banking rule: increase the account balance and record the transaction.

Imagine a user wants to deposit $500 into account "ACC123".

1.  The frontend sends a request to the backend.
2.  The backend's [API Layer (REST Controllers)](05_api_layer__rest_controllers_.md) (which we'll see in [Chapter 5](05_api_layer__rest_controllers_.md)) receives this request. It might receive the data as a `CreditDTO`.
3.  The [API Layer (REST Controllers)](05_api_layer__rest_controllers_.md) calls the `credit` method in our `BankAccountService`.

Here's the (simplified) code for the `credit` method:

```java
// Inside BankAccountServiceImpl.java

@Override
public void credit(String accountId, double amount, String description) throws BankAccountNotFoundException {
    log.info("Crediting account {}", accountId); // Log the action

    // Step 1: Find the account Entity using the Repository
    BankAccount bankAccount = bankAccountRepository.findById(accountId) // Call Repository method
        .orElseThrow(() -> new BankAccountNotFoundException("Bank account not found")); // Handle error if not found

    // Step 2: Create a new Operation Entity to record the deposit
    Operation operation = new Operation();
    operation.setBankAccount(bankAccount);
    operation.setAmount(amount);
    operation.setDate(new Date()); // Set current date/time
    operation.setType(OperationType.CREDIT); // Set type to CREDIT
    operation.setDescription(description);

    // Step 3: Save the new Operation Entity using the Repository
    operationRepository.save(operation); // Call Repository method

    // Step 4: Update the balance on the BankAccount Entity
    bankAccount.setBalance(bankAccount.getBalance() + amount); // Apply the business rule: balance = old balance + amount

    // Step 5: Save the updated BankAccount Entity using the Repository
    bankAccountRepository.save(bankAccount); // Call Repository method

    log.info("Account {} credited successfully", accountId); // Log success
}
```

Let's break this down:

*   The method receives the `accountId`, `amount`, and `description`. These could come directly from the API call or be extracted from an input DTO ([CreditDTO](02_data_transfer_objects__dtos__.md)).
*   **Step 1: Get the Data.** It uses `bankAccountRepository.findById(accountId)` to fetch the specific `BankAccount` **Entity** from the database. If the account doesn't exist, it throws a `BankAccountNotFoundException`. This is a key part of business logic: you can't deposit into a non-existent account.
*   **Step 2: Create the Transaction Record.** It creates a new `Operation` **Entity**. It populates its fields, including setting the `OperationType` to `CREDIT` and linking it to the `bankAccount` **Entity** it just retrieved.
*   **Step 3: Save the Transaction.** It uses `operationRepository.save(operation)` to save the new `Operation` **Entity** to the database.
*   **Step 4: Apply the Rule.** It updates the `balance` field directly on the `bankAccount` **Entity** object in memory. This is where the core banking rule "balance increases by the deposited amount" is applied.
*   **Step 5: Persist the Change.** It uses `bankAccountRepository.save(bankAccount)` to save the updated `BankAccount` **Entity** back to the database. Spring Data JPA knows this is an *existing* account (because it has an ID) and will update the corresponding row in the database table.

This simple method demonstrates how the Service layer coordinates multiple steps (fetch, create, save, update, save again) using the [Repositories](03_data_access_layer__repositories_.md) to implement a single business operation.

## Use Case: Getting Account History (`getAccountHistory`)

This use case shows how the Service layer fetches data using [Repositories](03_data_access_layer__repositories_.md) and then converts it into [DTOs](02_data_transfer_objects__dtos__.md) before returning it.

Imagine a user wants to view the history for account "ACC123" on a specific page.

1.  The frontend sends a request (e.g., GET /accounts/ACC123/history?page=0&size=10).
2.  The [API Layer](05_api_layer__rest_controllers_.md) receives this request and calls the `getAccountHistory` method in our `BankAccountService`, passing the `accountId`, `page` number, and `size` (number of items per page).

Here's the (simplified) code for `getAccountHistory`:

```java
// Inside BankAccountServiceImpl.java

@Override
public AccountHistoryDTO getAccountHistory(String accountId, int page, int size) throws BankAccountNotFoundException {
    // Step 1: Find the BankAccount Entity using the Repository
    BankAccount bankAccount = bankAccountRepository.findById(accountId) // Call Repository method
        .orElseThrow(() -> new BankAccountNotFoundException("Account not Found")); // Handle error

    // Step 2: Find the Operation Entities for this account using the Repository
    // Uses a custom Repository method supporting pagination
    Page<Operation> accountOperations = operationRepository.findByBankAccountIdOrderByDateDesc(accountId, PageRequest.of(page, size)); // Call Repository method with pagination

    // Step 3: Create the output DTO container
    AccountHistoryDTO accountHistoryDTO = new AccountHistoryDTO();

    // Step 4: Convert the list of Operation Entities into a list of Operation DTOs
    List<OperationDTO> accountOperationDTOS = accountOperations.getContent().stream()
        .map(op -> dtoMapper.fromAccountOperation(op)) // Use the Mapper to convert each Operation Entity to OperationDTO
        .collect(Collectors.toList());

    // Step 5: Populate the rest of the AccountHistoryDTO
    accountHistoryDTO.setAccountOperationDTOS(accountOperationDTOS);
    accountHistoryDTO.setAccountId(bankAccount.getId()); // Get data from Entity
    accountHistoryDTO.setBalance(bankAccount.getBalance()); // Get data from Entity
    accountHistoryDTO.setCurrentPage(page);
    accountHistoryDTO.setPageSize(size);
    accountHistoryDTO.setTotalPages(accountOperations.getTotalPages()); // Get pagination info from the results

    // Step 6: Return the populated DTO
    return accountHistoryDTO;
}
```

Breakdown:

*   **Step 1: Get Account Data.** It fetches the `BankAccount` **Entity** using the `bankAccountRepository`.
*   **Step 2: Get Operation Data.** It uses `operationRepository.findByBankAccountIdOrderByDateDesc` to get a paginated list of `Operation` **Entities** for that account, ordered by date. `PageRequest.of(page, size)` is a standard Spring Data object for pagination.
*   **Step 3: Prepare Output DTO.** An `AccountHistoryDTO` is created. This DTO is designed specifically for displaying account history on the frontend ([Chapter 2: DTOs](02_data_transfer_objects__dtos__.md)).
*   **Step 4: Map Entities to DTOs.** This is where the conversion happens. It takes the `List<Operation>` (Entities) from the query result and uses the `dtoMapper` to transform each `Operation` Entity into an `OperationDTO`. The result is a `List<OperationDTO>`.
*   **Step 5: Populate DTO.** It fills the `AccountHistoryDTO` with the list of `OperationDTO`s, the account ID and balance from the `BankAccount` Entity, and the pagination details obtained from the paginated query result (`accountOperations.getTotalPages()`).
*   **Step 6: Return DTO.** The completed `AccountHistoryDTO` is returned. This DTO contains all the necessary data for the frontend, formatted cleanly and without including unnecessary Entity details or deep relationship graphs.

This flow clearly shows the Service layer's role: fetch the necessary [Entities](01_entities__data_models__.md) using [Repositories](03_data_access_layer__repositories_.md), apply any required logic or data shaping, convert the results into appropriate [DTOs](02_data_transfer_objects__dtos__.md), and return the [DTOs](02_data_transfer_objects__dtos__.md).

## Under the Hood: How Services Connect

```mermaid
sequenceDiagram
    participant API Layer
    participant BankAccountService (Interface)
    participant BankAccountServiceImpl (@Service)
    participant Repositories
    participant DTO Mapper
    participant Database

    API Layer->>BankAccountService (Interface): Call credit(accountId, amount, desc)
    BankAccountService (Interface)-->>BankAccountServiceImpl (@Service): Routed to implementation
    Note over BankAccountServiceImpl (@Service): Handles Business Logic<br/>(@Transactional applies here)
    BankAccountServiceImpl (@Service)->>Repositories: Find BankAccount (findById)
    Repositories->>Database: SELECT * FROM bank_account WHERE ...
    Database-->>Repositories: BankAccount Entity Data
    Repositories-->>BankAccountServiceImpl (@Service): Return BankAccount Entity
    BankAccountServiceImpl (@Service)->>Repositories: Save Operation (save)
    Repositories->>Database: INSERT INTO operation (...) VALUES (...)
    Database-->>Repositories: Result
    Repositories-->>BankAccountServiceImpl (@Service): Confirmation
    BankAccountServiceImpl (@Service)->>Repositories: Save BankAccount (save)
    Repositories->>Database: UPDATE bank_account SET balance = ... WHERE ...
    Database-->>Repositories: Result
    Repositories-->>BankAccountServiceImpl (@Service): Confirmation
    BankAccountServiceImpl (@Service)-->>API Layer: Operation Complete (void return)
    Note over BankAccountServiceImpl (@Service): @Transactional commits changes if successful<br/>or rolls back if error occurs
```

1.  An external request (like from the frontend, arriving via the API Layer) triggers a method call on the `BankAccountService` interface.
2.  Because `BankAccountServiceImpl` is marked with `@Service` and implements `BankAccountService`, Spring intercepts the call and directs it to the corresponding method in `BankAccountServiceImpl`.
3.  Inside the `BankAccountServiceImpl` method:
    *   It uses its injected `Repositories` to interact with the database, fetching or saving [Entities](01_entities__data_models__.md).
    *   It uses its injected `dtoMapper` to convert [Entities](01_entities__data_models__.md) received from [Repositories](03_data_access_layer__repositories_.md) into [DTOs](02_data_transfer_objects__dtos__.md) to be returned, or converts input [DTOs](02_data_transfer_objects__dtos__.md) into [Entities](01_entities__data_models__.md) to be saved.
    *   It applies the specific business rules (e.g., balance check in `debit`, balance update in `credit`).
    *   It handles potential errors by throwing specific [Exceptions](https://docs.oracle.com/javase/tutorial/essential/exceptions/).
4.  The `@Transactional` annotation ensures that all database interactions within that method are treated as a single atomic unit.
5.  The Service method returns the result, typically a [DTO](02_data_transfer_objects__dtos__.md), back to the calling layer (the API Layer).

## Why Services are Important

*   **Separation of Concerns:** The Service layer is solely focused on implementing business rules, keeping them separate from data access details ([Repositories](03_data_access_layer__repositories_.md)) and communication details ([API Layer](05_api_layer__rest_controllers_.md), [DTOs](02_data_transfer_objects__dtos__.md)).
*   **Reusability:** Business logic is defined once in the Service layer and can be called by different parts of the application (e.g., different API endpoints, or even scheduled jobs).
*   **Testability:** Services can be unit tested independently of the database or the API. You can "mock" (simulate) the [Repositories](03_data_access_layer__repositories_.md) and [DTO Mappers](https://mapstruct.org/documentation/stable/reference/html/) to test the business logic in isolation.
*   **Transaction Management:** `@Transactional` simplifies handling complex database operations, ensuring data consistency.

## Summary Table

| Layer/Concept                    | Main Role                                      | Works With                      | Used By                                      | Key Spring Element(s)                       |
| :------------------------------- | :--------------------------------------------- | :------------------------------ | :------------------------------------------- | :------------------------------------------ |
| [Entities](01_entities__data_models__.md) | Blueprints for database tables & data structure | Database                        | Repositories, Services (internally)          | `@Entity`, `@Id`, `@OneToMany`, `@ManyToOne` |
| [DTOs](02_data_transfer_objects__dtos__.md) | Simple data packages for transfer          | Other application layers (API, Frontend) | Services (for input/output), API Layer       | Plain Java Classes, Lombok (`@Data`)        |
| [Repositories](03_data_access_layer__repositories_.md) | Access & manage data in the database     | Database, Entities              | Services                                     | `JpaRepository`, `@Query`                   |
| **Services (Business Logic)**    | **Implement core business rules & orchestrate** | **Repositories, DTOs, Entities** | **API Layer, other Services**                | **`@Service`, `@Transactional`**            |

The Service layer is where everything comes together. It takes requests (often carrying [DTOs](02_data_transfer_objects__dtos__.md)), uses [Repositories](03_data_access_layer__repositories_.md) to manipulate [Entities](01_entities__data_models__.md) in the database according to the business rules, and prepares the response (often as [DTOs](02_data_transfer_objects__dtos__.md)).

## Conclusion

In this chapter, we explored the **Business Logic Layer**, implemented through **Services**. We learned that Services are the central place for implementing the rules and operations of our banking application. We saw how they orchestrate tasks by using [Repositories](03_data_access_layer__repositories_.md) to interact with [Entities](01_entities__data_models__.md) in the database and use [DTOs](02_data_transfer_objects__dtos__.md) for data exchange. We walked through examples like making a deposit and getting account history to see these concepts in action, and discussed the importance of `@Service` and `@Transactional`.

Now that our backend has its data models ([Entities](01_entities__data_models__.md)), data transfer objects ([DTOs](02_data_transfer_objects__dtos__.md)), database access tools ([Repositories](03_data_access_layer__repositories_.md)), and core logic implementation ([Services]), how does the outside world (like a web browser or mobile app) actually *trigger* these operations? That's the role of the API Layer. In the next chapter, we'll learn about the **API Layer (REST Controllers)**.

[Next Chapter: API Layer (REST Controllers)](05_api_layer__rest_controllers_.md)

---
