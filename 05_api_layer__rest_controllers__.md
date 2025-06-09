# Chapter 5: API Layer (REST Controllers)

Welcome back to our exploration of the `Digital-Banking` project! So far, we've built the foundational layers of our backend: we know how to structure our data ([Entities](01_entities__data_models__.md)), package data for transfer ([DTOs](02_data_transfer_objects__dtos__.md)), access the database ([Repositories](03_data_access_layer__repositories__.md)), and implement the core banking rules ([Services](04_business_logic_layer__services__.md)).

Now, how does the outside world – like a web browser running our frontend application, or a mobile app – actually *use* all this backend power? How do they *request* a list of customers, *ask* for account history, or *send* instructions to make a deposit?

This is where the **API Layer** comes in. In modern web applications, this layer is often built using **REST Controllers**.

## What is the API Layer (REST Controllers)?

Imagine our backend application is a building full of specialized workers (our Services, Repositories, etc.). The **API Layer** acts as the **reception desk and communication hub** for this building.

Its main jobs are:

1.  **Listen for incoming requests:** It's constantly waiting for requests from clients (like your web browser). These requests usually arrive over the internet using the HTTP protocol (the same one your browser uses to visit websites).
2.  **Understand the request:** It figures out *what* the client wants to do (e.g., "get customer list," "deposit money") based on the request's URL and method (like GET, POST).
3.  **Get data from the request:** If the client is sending data (like deposit details), the API layer extracts this information.
4.  **Delegate the work:** It doesn't perform the banking operations itself! It knows *which* [Service](04_business_logic_layer__services__.md) method is needed for this request and calls that method, passing any necessary data.
5.  **Receive the result:** The [Service](04_business_logic_layer__services__.md) does the heavy lifting and returns the result (often a [DTO](02_data_transfer_objects__dtos__.md)) back to the API layer.
6.  **Format and send the response:** The API layer takes the result, formats it (typically as JSON, a common data format for APIs), and sends it back to the client who made the request.

In Spring Boot, classes that perform this role for RESTful APIs are typically annotated with `@RestController`. They define the **endpoints** (the specific URLs clients can call) and the **HTTP methods** (GET, POST, PUT, DELETE) associated with each endpoint.

## Key Concepts for REST Controllers

Let's look at the building blocks you'll see in our REST Controller code:

*   **`@RestController`**: This annotation tells Spring that this Java class is a controller that handles incoming web requests and should automatically convert method return values into HTTP responses (like JSON).
*   **`@RequestMapping("/some-path")`**: Used at the class level or method level to define the base URL path(s) this controller or method handles. More commonly, specific method-level annotations are used:
*   **`@GetMapping("/path")`**: Maps HTTP GET requests to a specific handler method. Used for fetching data.
*   **`@PostMapping("/path")`**: Maps HTTP POST requests. Used for creating new resources or performing actions that submit data.
*   **`@PutMapping("/path/{id}")`**: Maps HTTP PUT requests. Used for updating existing resources.
*   **`@DeleteMapping("/path/{id}")`**: Maps HTTP DELETE requests. Used for deleting resources.
*   **`@PathVariable`**: Used to extract values from the URL path itself (e.g., the `{id}` in `/customers/{id}`).
*   **`@RequestParam`**: Used to extract values from the query parameters in the URL (e.g., `page=0` in `/accounts?page=0&size=10`).
*   **`@RequestBody`**: Used to tell Spring to convert the content of the HTTP request body (usually JSON sent by the client) into a Java object (like a [DTO](02_data_transfer_objects__dtos__.md)).
*   **Dependency Injection**: Controllers need access to the [Services](04_business_logic_layer__services__.md) they delegate to. Spring provides this automatically, usually via constructor injection (using `@AllArgsConstructor` with Lombok, like in our project).

## Our REST Controllers

In the `Digital-Banking` project, we have two main REST Controller classes:

*   `CustomerRestController`: Handles requests related to customers.
*   `BankAccountRestController`: Handles requests related to bank accounts and operations.

Both are located in the `com.example.backend.web` package. Let's examine parts of them.

## Use Case: Listing All Customers

Let's trace how a request from the frontend to "get all customers" is handled by the API layer.

1.  The frontend (e.g., `http://localhost:4200`) makes an HTTP GET request to `http://localhost:8085/customers`.
2.  Our backend application, specifically the API layer, receives this request.
3.  Spring looks at the URL (`/customers`) and the method (GET) and finds the method in `CustomerRestController` that matches.

Here's the relevant snippet from `CustomerRestController.java`:

```java
// Inside CustomerRestController.java

@RestController // Tells Spring this class handles web requests
@AllArgsConstructor // Lombok creates a constructor for injecting dependencies
@Slf4j // For logging
@CrossOrigin("*") // Allows requests from any origin (useful during development)

public class CustomerRestController {
    // Spring will inject an instance of BankAccountService here
    private BankAccountService bankAccountService;

    @GetMapping("/customers") // Maps HTTP GET requests to the /customers URL
    // @PreAuthorize("hasAuthority('SCOPE_USER')") // Security check (covered later)
    public List<CustomerDTO> customers(){ // This method handles the request
        // Step 1: Delegate the work to the Service Layer
        log.info("Received request to list all customers");
        List<CustomerDTO> customerList = bankAccountService.listCostumers(); // Call the Service method

        // Step 2: Return the result (a List of CustomerDTOs)
        log.info("Returning {} customers", customerList.size());
        return customerList; // Spring automatically converts this List<CustomerDTO> into JSON
    }

    // ... other methods ...
}
```

Let's break it down:

*   The `@RestController` annotation marks this class.
*   `private BankAccountService bankAccountService;` and `@AllArgsConstructor`: This is how the controller gets access to the [Service layer](04_business_logic_layer__services__.md). Spring automatically creates a `BankAccountService` instance and gives it to the controller when the controller is created.
*   `@GetMapping("/customers")`: This annotation is placed on the `customers()` method. It tells Spring: "When you get an HTTP GET request for the `/customers` path, call this `customers()` method."
*   Inside the `customers()` method:
    *   `bankAccountService.listCostumers()`: The controller calls the appropriate method in the injected [Service layer](04_business_logic_layer__services__.md). The controller doesn't know *how* the service gets the customers (it relies on the service to use the [Repositories](03_data_access_layer__repositories__.md)).
    *   `return bankAccountService.listCostumers();`: The method returns the result it got from the [Service](04_business_logic_layer__services__.md), which is a `List<CustomerDTO>`.
*   **Automatic JSON Conversion:** Because this class is a `@RestController`, Spring automatically takes the `List<CustomerDTO>` returned by the method, converts it into a JSON array, and sends it back as the body of the HTTP response to the client.

The frontend receives this JSON response and can then easily display the list of customers.

## Use Case: Getting Account History with Pagination

This example shows how the API layer handles more complex requests involving data from the URL path and query parameters.

Imagine the frontend needs history for account "ACC123" and wants the second page (page 1, since counting starts at 0) with 5 operations per page. It makes an HTTP GET request like `http://localhost:8085/accounts/ACC123/pageOperations?page=1&size=5`.

Here's the relevant snippet from `BankAccountRestController.java`:

```java
// Inside BankAccountRestController.java

@RestController // Tells Spring this class handles web requests
@AllArgsConstructor // Lombok creates a constructor for injecting the Service
@CrossOrigin("*") // Allows requests from any origin

public class BankAccountRestController {
    // Spring will inject an instance of BankAccountService here
    private BankAccountService bankAccountService;

    @GetMapping("/accounts/{accountId}/pageOperations") // Maps GET requests with a path variable
    public AccountHistoryDTO getAccountHistory( // This method handles the request
            @PathVariable String accountId, // Extracts "ACC123" from the path
            @RequestParam(name="page",defaultValue = "0") int page, // Extracts "1" from ?page=1
            @RequestParam(name="size",defaultValue = "5")int size) // Extracts "5" from &size=5
            throws BankAccountNotFoundException { // Declares possible error

        // Step 1: Delegate the work to the Service Layer, passing extracted data
        log.info("Received request for history of account {} (page {}, size {})", accountId, page, size);
        AccountHistoryDTO accountHistory = bankAccountService.getAccountHistory(accountId, page, size); // Call Service method

        // Step 2: Return the result (an AccountHistoryDTO)
        log.info("Returning account history for account {}", accountId);
        return accountHistory; // Spring automatically converts this DTO to JSON
    }

    // ... other methods ...
}
```

Let's break this down:

*   `@GetMapping("/accounts/{accountId}/pageOperations")`: This maps GET requests. The `{accountId}` part is a placeholder.
*   `@PathVariable String accountId`: This annotation on the `accountId` parameter tells Spring: "Take the value from the `{accountId}` part of the URL path (e.g., 'ACC123') and pass it into this `accountId` method parameter."
*   `@RequestParam(name="page",defaultValue = "0") int page`: This tells Spring: "Look for a query parameter named `page` (e.g., `?page=1`). If found, convert its value to an `int` and pass it to the `page` method parameter. If not found, use `0` as a default."
*   `@RequestParam(name="size",defaultValue = "5") int size`: Similar to `page`, but for the `size` query parameter.
*   `throws BankAccountNotFoundException`: If the [Service layer](04_business_logic_layer__services__.md) throws this specific exception (meaning the account ID wasn't found), the controller allows it to propagate. Spring will automatically convert this exception into an appropriate HTTP error response (like a 404 Not Found) for the client.
*   `bankAccountService.getAccountHistory(accountId, page, size)`: The controller calls the [Service method](04_business_logic_layer__services__.md), passing the account ID, page number, and size it extracted from the request.
*   `return accountHistory`: The controller returns the `AccountHistoryDTO` received from the service. Spring converts it to JSON.

The frontend receives the `AccountHistoryDTO` as JSON, containing the account balance, pagination info, and the list of operations (as `OperationDTO`s), and displays it.

## Use Case: Making a Deposit (Sending Data)

This shows how the API layer handles requests that send data from the client to the backend, using the POST method and `@RequestBody`.

Imagine the frontend wants to deposit $100 into account "ACC456" with the description "Online Deposit". It might send an HTTP POST request to `http://localhost:8085/accounts/credit` with a JSON body like:

```json
{
  "accountId": "ACC456",
  "amount": 100,
  "description": "Online Deposit"
}
```

Here's the relevant snippet from `BankAccountRestController.java`:

```java
// Inside BankAccountRestController.java

@RestController // Tells Spring this class handles web requests
@AllArgsConstructor // Inject the Service
@CrossOrigin("*")

public class BankAccountRestController {
    private BankAccountService bankAccountService;

    @PostMapping("/accounts/credit") // Maps HTTP POST requests to /accounts/credit
    public CreditDTO credit(@RequestBody CreditDTO creditDTO) // Extracts JSON body into a DTO
            throws BankAccountNotFoundException { // Declares possible error

        // Step 1: Delegate the work to the Service Layer, passing data from the DTO
        log.info("Received request to credit account {}", creditDTO.getAccountId());
        this.bankAccountService.credit(
                creditDTO.getAccountId(),
                creditDTO.getAmount(),
                creditDTO.getDescription()); // Call Service method

        // Step 2: Return the input DTO as confirmation (optional, but common)
        log.info("Account {} credited. Returning input DTO.", creditDTO.getAccountId());
        return creditDTO; // Spring converts this DTO back to JSON for the response
    }

    // ... other methods (debit, transfer use similar @PostMapping and @RequestBody) ...
}
```

Breakdown:

*   `@PostMapping("/accounts/credit")`: Maps POST requests to `/accounts/credit`.
*   `@RequestBody CreditDTO creditDTO`: This is crucial for handling incoming data. It tells Spring: "Take the JSON data in the body of the request, and convert it into a `CreditDTO` object. Then, pass that object into the `creditDTO` method parameter." This is where our [DTOs](02_data_transfer_objects__dtos__.md) for input are used.
*   `this.bankAccountService.credit(...)`: The controller calls the [Service method](04_business_logic_layer__services__.md), using the data it extracted from the `creditDTO`.
*   `return creditDTO;`: In this case, the method returns the same `CreditDTO` that was received. This is a common pattern to confirm to the client what data was processed. Spring converts it back to JSON.

Similar patterns using `@PostMapping` and `@RequestBody` are used for the `debit` and `transfer` operations in `BankAccountRestController`, taking `DebitDTO` and `TransferRequestDTO` respectively.

`CustomerRestController` also uses `@PostMapping` and `@PutMapping` with `@RequestBody` to create and update customers, taking `CustomerDTO` as input.

```java
// Example from CustomerRestController (simplified)
@PostMapping("/customers") // Handles POST requests to /customers
// @PreAuthorize("hasAuthority('SCOPE_ADMIN')") // Security check
public CustomerDTO saveCustomer(@RequestBody CustomerDTO customerDTO){ // Gets CustomerDTO from request body
    log.info("Received request to save customer: {}", customerDTO.getName());
    // Calls Service to save the customer Entity (service converts DTO->Entity)
    return bankAccountService.saveCostumer(customerDTO); // Returns saved CustomerDTO (service converts Entity->DTO)
}
```

## Under the Hood: Request Flow

Let's visualize the journey of a request from the frontend to the backend, focusing on the API Layer's role.

```mermaid
sequenceDiagram
    participant Frontend (Browser/App)
    participant Internet
    participant API Layer (@RestController)
    participant Business Logic Layer (@Service)
    participant Data Access Layer (@Repository)
    participant Database

    Frontend (Browser/App)->>Internet: HTTP Request (URL, Method, Body?)
    Internet->>API Layer (@RestController): Request Arrives
    API Layer (@RestController)->>API Layer (@RestController): Spring routes request to correct method<br/>(based on URL, Method, Path/Query Params, Request Body)
    Note over API Layer (@RestController): Extracts data from request<br/>(@PathVariable, @RequestParam, @RequestBody)
    API Layer (@RestController)->>Business Logic Layer (@Service): Calls Service method<br/>(passes extracted data/DTOs)
    Note over Business Logic Layer (@Service): Performs business logic<br/>(uses @Repository, DTO Mapper, etc.)
    Business Logic Layer (@Service)-->>API Layer (@RestController): Returns result (often a DTO)
    Note over API Layer (@RestController): Formats result into HTTP Response<br/>(often converts DTO to JSON)
    API Layer (@RestController)-->>Internet: Sends HTTP Response
    Internet-->>Frontend (Browser/App): Response Arrives
    Note over Frontend (Browser/App): Uses data to update UI
```

1.  The Frontend creates an HTTP request (like GET `/customers` or POST `/accounts/credit` with a JSON body).
2.  The request travels over the internet to our backend server.
3.  The backend receives the request. Spring, seeing the `@RestController` classes, examines the request's URL and HTTP method.
4.  Spring finds the specific method in one of the `@RestController` classes that is mapped to that URL and method combination (e.g., the `customers()` method for GET `/customers`).
5.  Spring extracts any necessary data from the request based on annotations like `@PathVariable`, `@RequestParam`, and `@RequestBody`, and passes this data as arguments to the chosen method.
6.  The controller method calls the appropriate method in the injected [Service layer](04_business_logic_layer__services__.md), handing over the request data.
7.  The [Service layer](04_business_logic_layer__services__.md) performs the business logic, potentially interacting with the [Data Access Layer](03_data_access_layer__repositories__.md) to fetch or save [Entities](01_entities__data_models__.md).
8.  The [Service layer](04_business_logic_layer__services__.md) returns a result (usually a [DTO](02_data_transfer_objects__dtos__.md) or a list of [DTOs](02_data_transfer_objects__dtos__.md)) back to the controller method.
9.  The controller method returns this result. Because the class is a `@RestController`, Spring automatically takes the returned Java object and converts it into an appropriate format for the HTTP response body, typically JSON.
10. Spring sends the HTTP response back to the client over the internet.
11. The Frontend receives the response (the JSON data) and uses it to update the user interface.

The API Layer acts as the crucial bridge, translating incoming HTTP requests into method calls on our [Service layer](04_business_logic_layer__services__.md) and translating the results back into HTTP responses for the client.

## Why the API Layer is Important

*   **Entry Point:** It provides the defined ways for the outside world to interact with your backend. Clients don't need to know about your Services, Repositories, or Entities; they only need to know the API endpoints.
*   **Structure:** It clearly defines the available operations and how clients should call them (URL, method, required data).
*   **Decoupling:** It keeps the presentation/communication logic separate from the core business logic (in the [Service layer](04_business_logic_layer__services__.md)). This means you could change the API format (e.g., from REST to GraphQL) without changing your core banking logic.
*   **Data Formatting:** It handles the messy details of converting data between the Java objects your backend uses (often [DTOs](02_data_transfer_objects__dtos__.md)) and the format used for communication over the internet (JSON).

## Summary Table

Let's update our summary table with the API Layer:

| Layer/Concept                    | Main Role                                      | Works With                      | Used By                                       | Key Spring Element(s)                       |
| :------------------------------- | :--------------------------------------------- | :------------------------------ | :-------------------------------------------- | :------------------------------------------ |
| [Entities](01_entities__data_models__.md) | Blueprints for database tables & data structure | Database                        | Repositories, Services (internally)           | `@Entity`, `@Id`, `@OneToMany`, `@ManyToOne` |
| [DTOs](02_data_transfer_objects__dtos__.md) | Simple data packages for transfer          | Other application layers (API, Frontend) | Services (for input/output), API Layer        | Plain Java Classes, Lombok (`@Data`)        |
| [Repositories](03_data_access_layer__repositories__.md) | Access & manage data in the database     | Database, Entities              | Services                                      | `JpaRepository`, `@Query`                   |
| [Services (Business Logic)](04_business_logic_layer__services__.md) | Implement core business rules & orchestrate | Repositories, DTOs, Entities    | API Layer, other Services                     | `@Service`, `@Transactional`                |
| **API Layer (Controllers)**      | **Receive requests, delegate to Service, send response** | **DTOs, Services**              | **Frontend / Other Clients**                  | **`@RestController`, `@GetMapping`, `@PostMapping`, `@RequestBody`, etc.** |

The API Layer is the outermost layer of our backend, the one that clients directly interact with. It's lean, focused on communication, and relies entirely on the inner layers ([Services](04_business_logic_layer__services__.md), [Repositories](03_data_access_layer__repositories__.md)) to perform the actual work.

## Conclusion

In this chapter, we learned about the **API Layer** and the role of **REST Controllers** in our `Digital-Banking` project. We saw how `@RestController` classes define the entry points for clients, mapping specific URLs and HTTP methods to Java methods. We explored annotations like `@GetMapping`, `@PostMapping`, `@PathVariable`, `@RequestParam`, and `@RequestBody` for handling different types of requests and extracting data. Most importantly, we understood that controllers act as delegates, calling the [Service layer](04_business_logic_layer__services__.md) to execute business logic and then formatting the results (often [DTOs](02_data_transfer_objects__dtos__.md)) back into HTTP responses (like JSON).

With the API layer in place, our backend application now has defined ways for clients to access its functionality. However, making our banking operations available to the world means we need to control *who* can do *what*. In the next chapter, we will dive into **Security Configuration** for the backend to protect our application and data.

[Next Chapter: Security Configuration (Backend)](06_security_configuration__backend_.md)

---
