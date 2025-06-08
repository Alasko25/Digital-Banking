# Chapter 7: Frontend Services

Welcome back to our `Digital-Banking` tutorial! In the [previous chapter](06_security_configuration__backend_.md), we made sure our backend API was secure, controlling who can access our banking operations using Spring Security and JWTs. Now our backend is ready to serve data and perform actions, but how does the frontend application, which runs in the user's web browser, actually *talk* to this secure backend?

Think of the frontend as the user interface – the buttons, forms, and displays that the user sees and interacts with. When a user clicks "View Customers," the frontend needs to ask the backend for the list of customers. When they fill out a deposit form and click "Deposit," the frontend needs to send that information to the backend to perform the transaction.

Making these requests involves dealing with technical details: building the correct URL, choosing the right HTTP method (GET, POST, etc.), sending data in the correct format (like JSON), handling responses, and dealing with potential errors. If every part of our user interface (every button, every page) had to manage these details directly, our frontend code would become messy, repetitive, and hard to maintain.

This is where **Frontend Services** come in.

## What are Frontend Services?

In Angular (the framework used for our frontend), **Frontend Services** are classes specifically designed to handle the communication with the backend API. They act as a **clean interface** between the parts of the frontend that manage the user interface (called **Components**, which we'll discuss in the next chapter) and the backend API.

You can think of a Frontend Service as a **specialized messenger** for a specific type of data or operation (like 'Customer data' or 'Account operations').

Key roles of Frontend Services:

1.  **Encapsulate HTTP Logic:** They contain the code for making HTTP requests using Angular's built-in `HttpClient`.
2.  **Talk to the Backend:** They know the backend API's URLs and which methods (GET, POST) to use for different tasks.
3.  **Provide Data to Components:** Components call methods on these services (e.g., `customerService.getCustomers()`) to get the data they need, without needing to know *how* the service gets the data.
4.  **Handle Data Transformation (Optional but Common):** Sometimes they might do minor transformations on the data received from the backend before giving it to the components.
5.  **Centralize API Calls:** All backend communication related to a specific area (like customers) goes through one service, making it easy to manage and update.

Frontend Services in Angular are typically marked with the `@Injectable()` decorator, which allows them to be "injected" into components or other services that need to use them.

## Use Case: Fetching the List of Customers

Let's use the example of displaying a list of customers on the frontend. Our backend already has an API endpoint for this: `GET /customers`, which is handled by our `CustomerRestController` and returns a list of `CustomerDTO`s (converted to JSON).

Here's how a Frontend Service handles getting this data:

1.  A **Component** (e.g., a page showing the customer list) needs the customer data.
2.  Instead of making the HTTP call itself, the Component calls a method on the **`CustomerService`**.
3.  The `CustomerService` uses `HttpClient` to make the actual `GET` request to `http://localhost:8085/customers`.
4.  `HttpClient` sends the request. Importantly, our frontend includes an **Interceptor** ([`app-http.interceptor.ts`](#file-digibank-frontendsrcappinterceptorsapp-httpinterceptor.ts)), which automatically adds the JWT (obtained during login) to the request headers before it's sent to the backend. This ensures the request is authenticated, as required by our [backend security configuration](06_security_configuration__backend_.md).
5.  The backend receives the request, validates the JWT, processes the request (using the [API layer](05_api_layer__rest_controllers__.md) and [Service layer](04_business_logic_layer__services_.md) to fetch [Entities](01_entities__data_models__.md) via [Repositories](03_data_access_layer__repositories_.md)), and sends back a JSON response containing the list of customer data (based on `CustomerDTO`).
6.  `HttpClient` receives the JSON response and automatically converts it into the expected Angular/TypeScript object type (`Array<Customer>`).
7.  The `CustomerService` returns this data (usually wrapped in an `Observable`) back to the Component.
8.  The Component receives the data and uses it to update the user interface, displaying the list of customers.

## Our Frontend Services

In our `digibank-frontend` project, you'll find services in the `src/app/services/` directory. The main ones related to backend communication are:

1.  `CustomerService`: Handles customer-related API calls.
2.  `AccountsService`: Handles bank account and operation-related API calls.
3.  `AuthService`: Handles authentication (login) and manages the JWT.

Let's look at `CustomerService` and its `getCustomers` method.

### `CustomerService`

This service is responsible for operations like fetching, searching, saving, and deleting customers by talking to the backend's `/customers` endpoints.

```typescript
// src/app/services/customer.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http"; // Needed to make HTTP calls
import { Observable } from "rxjs"; // For asynchronous operations
import { Customer } from "../model/customer.model"; // The expected data structure

@Injectable({ // <-- Marks this as a service that can be injected
  providedIn: 'root' // <-- Makes the service available throughout the app
})
export class CustomerService {
  // Base URL for our backend API (important to keep this centralized)
  private baseUrl = "http://localhost:8085";

  // Inject HttpClient into the service
  constructor(private http:HttpClient) { }

  // Method to get all customers from the backend
  public getCustomers():Observable<Array<Customer>>{
    // Use HttpClient's get method
    return this.http.get<Array<Customer>>(this.baseUrl+"/customers") // <-- Makes the actual HTTP GET request
  }

  // Method to search customers
  public searchCustomers(keyword : string):Observable<Array<Customer>>{
    // Uses query parameters in the URL
    return this.http.get<Array<Customer>>(this.baseUrl+"/customers/search?keyword="+keyword)
  }

  // Method to save a new customer
  public saveCustomer(customer: Customer):Observable<Customer>{
    // Uses HttpClient's post method and sends data in the body
    return this.http.post<Customer>(this.baseUrl+"/customers",customer);
  }

  // Method to delete a customer
  public deleteCustomer(id: number){
    // Uses HttpClient's delete method with ID in the path
    return this.http.delete(this.baseUrl+"/customers/"+id);
  }
}
```

*   `@Injectable({ providedIn: 'root' })`: This decorator is essential. It tells Angular that this is a service and registers it so that other parts of the application can request an instance of it. `providedIn: 'root'` makes it a **singleton** available everywhere.
*   `constructor(private http:HttpClient)`: This is how the service gets access to Angular's `HttpClient`. Angular automatically creates an instance of `HttpClient` and "injects" it into the service's constructor. The service then uses this `http` object to make requests.
*   `private baseUrl = "http://localhost:8085";`: Storing the base URL here makes it easy to change if the backend address ever changes.
*   `public getCustomers():Observable<Array<Customer>>`: This method makes the call.
    *   `this.http.get<Array<Customer>>(...)`: This calls the `get` method of the injected `HttpClient`.
        *   `<Array<Customer>>`: This tells `HttpClient` what type of data to expect in the response body (an array of objects that match the `Customer` model). `HttpClient` will automatically try to convert the incoming JSON into this type. The `Customer` model (`src/app/model/customer.model.ts`) is a simple TypeScript interface or class defining the structure of the data, similar to our backend [DTOs](02_data_transfer_objects__dtos__.md).
        *   `this.baseUrl+"/customers"`: This is the full URL to the backend endpoint.
    *   `Observable<Array<Customer>>`: `HttpClient` methods *do not* return the data directly. They return an `Observable`. This is because HTTP requests are asynchronous – they take time to complete, and the rest of your application shouldn't freeze while waiting. An `Observable` represents a stream of data that will arrive *at some point in the future*. Components that need the data must **subscribe** to this `Observable` to get notified when the data arrives.

The other methods (`searchCustomers`, `saveCustomer`, `deleteCustomer`) show variations: using query parameters, using `post` to send data (the `customer` object in `saveCustomer`'s case, which Angular will convert to JSON), and using `delete`. All return `Observable`s.

### `AccountsService`

Similar to `CustomerService`, `AccountsService` provides methods for interacting with the backend endpoints related to bank accounts and operations (`/accounts/...`).

```typescript
// src/app/services/accounts.service.ts (simplified)

import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {AccountDetails} from "../model/account.model"; // Expected model for account history

@Injectable({
  providedIn: 'root'
})
export class AccountsService {
  private baseUrl = "http://localhost:8085";

  constructor(private http : HttpClient) { }

  // Method to get account history with pagination
  public getAccount(accountId : string, page : number, size : number):Observable<AccountDetails>{
    // Uses path variable and query parameters
    return this.http.get<AccountDetails>(this.baseUrl+"/accounts/"+accountId+"/pageOperations?page="+page+"&size="+size);
  }

  // Method to perform a debit operation (Withdrawal)
  public debit(accountId : string, amount : number, description:string){
    // Prepares the data structure expected by the backend's DebitDTO
    let data={accountId : accountId, amount : amount, description : description}
    return this.http.post(this.baseUrl+"/accounts/debit", data); // Sends data via POST
  }

  // Method to perform a credit operation (Deposit)
  public credit(accountId : string, amount : number, description:string){
    // Prepares data for backend's CreditDTO
    let data={accountId : accountId, amount : amount, description : description}
    return this.http.post(this.baseUrl+"/accounts/credit",data); // Sends data via POST
  }

  // Method to perform a transfer
  public transfer(accountSource: string,accountDestination: string, amount : number, description:string){
    // Prepares data for backend's TransferRequestDTO
    let data={accountSource, accountDestination, amount, description }
    return this.http.post(this.baseUrl+"/accounts/transfer",data); // Sends data via POST
  }
}
```

This service follows the same pattern: inject `HttpClient`, define a `baseUrl`, and create methods that use `http.get` or `http.post` (or `put`, `delete`) to communicate with the backend, returning `Observable`s. Notice how the data sent in `debit`, `credit`, and `transfer` matches the structure of the backend's input [DTOs](02_data_transfer_objects__dtos__.md) (`DebitDTO`, `CreditDTO`, `TransferRequestDTO`).

### `AuthService` (Handling Authentication)

`AuthService` is slightly different as its primary job is handling the login process and managing the JWT token.

```typescript
// src/app/services/auth.service.ts (simplified)

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from "@angular/common/http";
import { jwtDecode } from "jwt-decode"; // Library to decode the JWT

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  accessToken!: string ; // Where we'll store the token
  isAuthenticated: boolean = false; // Flag to track login state
  roles: any; // User roles from the token
  username: any; // Username from the token
  // password: any; // Removed sensitive info

  constructor(private http: HttpClient) { }

  // Method to send login credentials to the backend
  public login(username: string, password: string) {
    // Backend expects form data for login, not JSON body
    let options = {
      headers: new HttpHeaders().set("Content-Type", "application/x-www-form-urlencoded")
    };
    // Build form data parameters
    let paras = new HttpParams().set("username", username).set("password", password);

    // Send POST request to the login endpoint
    return this.http.post("http://localhost:8085/auth/login", paras, options); // Returns an Observable
  }

  // Method to process the data received after successful login
  public loadProfile(data: any) {
    this.isAuthenticated = true;
    this.accessToken = data["access-token"]; // Extract the token from the response
    let decodeJwt:any=jwtDecode(this.accessToken); // Decode the token to read its contents
    this.roles = decodeJwt.scope; // Extract roles (scope) from the decoded token
    this.username = decodeJwt.sub; // Extract subject (username) from the decoded token
  }
  // Method to get roles as an array
  public getRoles(){
    return this.roles ? this.roles.split(" ") : [];
  }
}
```

*   `login(username, password)`: This method sends the username and password to the backend's `/auth/login` endpoint. Notice it uses `HttpParams` and specific headers because the backend's login endpoint (configured by Spring Security) expects data in `application/x-www-form-urlencoded` format, not the default JSON.
*   `loadProfile(data)`: This method is called *after* the `login` observable returns successfully. It receives the response data (which contains the `access-token` field, as returned by our [backend `SecurityController`](06_security_configuration__backend_.md)). It stores the token and uses the `jwtDecode` library to read the token's payload, extracting the user's roles (`scope`) and username (`sub`). This information is then stored in the service's properties (`isAuthenticated`, `accessToken`, `roles`, `username`) so other parts of the application can access it.

The `AuthService` is crucial because it provides the mechanism to obtain the JWT, which is then needed for *all* protected requests made by the other services.

## The HTTP Interceptor (`app-http.interceptor.ts`)

How does that JWT token get added to every protected request automatically? This is handled by an **HTTP Interceptor**.

An Interceptor is a feature in Angular's `HttpClient` that allows you to intercept incoming or outgoing HTTP requests and responses and modify them. Our `appHttpInterceptor` intercepts outgoing requests and adds the `Authorization: Bearer <token>` header if a token exists and the request is not the login request itself.

```typescript
// src/app/interceptors/app-http.interceptor.ts

import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service'; // To get the token

// Define the interceptor function
export const appHttpInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> => {
  // Inject the AuthService to access the stored token
  const authService = inject(AuthService);
  // Get the token from AuthService or localStorage (localStorage persistence logic might be added)
  const token = authService.accessToken; // Use the token stored in AuthService

  // Check if a token exists AND the request is NOT the login request
  if (token && !req.url.includes('/auth/login')) {
    // Clone the request and add the Authorization header
    const cloned = req.clone({
      headers: req.headers.set('Authorization', 'Bearer ' + token) // <-- Adds the header!
    });
    // Pass the cloned request to the next handler (which could be another interceptor or HttpClient itself)
    return next(cloned);
  }

  // If no token, or it's the login request, just pass the original request along
  return next(req);
};
```

*   `HttpInterceptorFn`: Defines the type of the interceptor function.
*   `inject(AuthService)`: This is a modern way (since Angular 14) to get an instance of `AuthService` within the function.
*   `const token = authService.accessToken;`: Retrieves the JWT from the `AuthService`.
*   `if (token && !req.url.includes('/auth/login'))`: Checks if we have a token *and* if the request is *not* the one going to the login endpoint (because the login request shouldn't have a JWT yet).
*   `const cloned = req.clone({...})`: Requests are immutable, so we clone the request to modify it.
*   `headers: req.headers.set('Authorization', 'Bearer ' + token)`: On the cloned request, we set the `Authorization` header with the "Bearer " prefix followed by the token. This is the standard way to send JWTs.
*   `return next(cloned);`: Pass the modified request down the chain.
*   `return next(req);`: If no token or it's the login request, pass the original request.

This interceptor is registered in Angular's `app.config.ts` (or `app.module.ts` in older versions) so that `HttpClient` uses it for *every* request.

## Under the Hood: Frontend Request Flow

Let's trace the journey of a request initiated by a Component asking for protected data, keeping the services and interceptor in mind.

```mermaid
sequenceDiagram
    participant Component
    participant Frontend Service<br>(e.g., CustomerService)
    participant HTTP Interceptor<br>(appHttpInterceptor)
    participant Angular HttpClient
    participant Internet
    participant Backend API Layer

    Component->>Frontend Service<br>(e.g., CustomerService): Call getCustomers()
    Frontend Service<br>(e.g., CustomerService)->>Angular HttpClient: Call http.get('/customers')
    Angular HttpClient->>HTTP Interceptor<br>(appHttpInterceptor): Request is sent to interceptor
    Note over HTTP Interceptor<br>(appHttpInterceptor): Gets JWT from AuthService<br/>Adds Authorization header
    HTTP Interceptor<br>(appHttpInterceptor)->>Angular HttpClient: Pass modified request
    Angular HttpClient->>Internet: Send HTTP Request<br/>(GET /customers with JWT header)
    Internet->>Backend API Layer: Request Arrives
    Note over Backend API Layer: Backend Security validates JWT<br/>Request proceeds to Controller/Service<br/>Backend processes and sends JSON response
    Backend API Layer-->>Internet: HTTP Response (JSON data)
    Internet-->>Angular HttpClient: Response Arrives
    Angular HttpClient->>Frontend Service<br>(e.g., CustomerService): Convert JSON to Array<Customer><br/>Emit data through Observable
    Frontend Service<br>(e.g., CustomerService)-->>Component: Observable emits Array<Customer>
    Note over Component: Component subscribes to Observable<br/>Receives data and updates UI
```

1.  A **Component** needs data (e.g., customer list). It calls the appropriate method on an injected **Frontend Service** (e.g., `customerService.getCustomers()`).
2.  The **Frontend Service** uses its injected `HttpClient` instance to prepare the HTTP request (e.g., a GET request to `/customers`).
3.  Angular's **`HttpClient`** processing pipeline sends the outgoing request to the configured **HTTP Interceptor** (`appHttpInterceptor`).
4.  The **Interceptor** gets the current JWT from the `AuthService`. If a token exists and the request is not the login request, it clones the request and adds the `Authorization: Bearer <token>` header.
5.  The **Interceptor** passes the (potentially modified) request back to `HttpClient`.
6.  **`HttpClient`** sends the request over the **Internet** to the **Backend API Layer**.
7.  The **Backend API Layer** (specifically, Spring Security configured in [Chapter 6](06_security_configuration__backend_.md)) intercepts the request, validates the JWT, and if authorized, allows the request to proceed to the appropriate [REST Controller](05_api_layer__rest_controllers__.md) and [Service layer](04_business_logic_layer__services_.md). The backend processes the request and sends a JSON response back.
8.  **`HttpClient`** receives the response. It automatically parses the JSON body and converts it into the expected Angular/TypeScript type (`Array<Customer>` in this case).
9.  **`HttpClient`** returns the processed data to the **Frontend Service** by emitting it through the `Observable` that the `http.get` call returned.
10. The **Frontend Service** returns this `Observable` to the **Component**.
11. The **Component** must be **subscribed** to this `Observable` to receive the data when it arrives. Once the data is received, the Component can update its template (the HTML) to display the information to the user.

This flow shows how Frontend Services and the Interceptor work together to cleanly handle backend communication and security headers, keeping the Components focused on UI presentation.

## Why Use Frontend Services?

*   **Clean Code:** Components are simpler because they don't contain HTTP logic. They just call service methods.
*   **Reusability:** Multiple components can use the same service method (e.g., `getCustomers()`) to fetch data from the same endpoint, avoiding code duplication.
*   **Maintainability:** If the backend API changes (e.g., a URL changes), you only need to update the code in one place – the relevant service method – instead of finding and changing the HTTP call in many different components.
*   **Testability:** Services can be unit tested independently of components. You can "mock" (simulate) the `HttpClient` to test the service's logic.
*   **Separation of Concerns:** Services handle data fetching (the 'how'), while Components handle data display (the 'what the user sees').

## Summary Table (Frontend Added)

Let's expand our summary table to include the frontend layers:

| Layer/Concept                    | Main Role                                                    | Works With                                     | Used By                                       | Key Technology/Element(s)                                    |
| :------------------------------- | :----------------------------------------------------------- | :--------------------------------------------- | :-------------------------------------------- | :----------------------------------------------------------- |
| [Entities](01_entities__data_models__.md) | Blueprints for database tables & data structure              | Database                                     | Repositories, Services (internally)           | Java, JPA (`@Entity`, etc.)                                  |
| [DTOs](02_data_transfer_objects__dtos__.md) | Simple data packages for transfer                            | Other application layers (API, Frontend)     | Backend Services (for input/output), Backend API Layer | Java, Plain Classes, Lombok (`@Data`)                      |
| [Repositories](03_data_access_layer__repositories_.md) | Access & manage data in the database                         | Database, Entities                           | Backend Services                              | Java, Spring Data JPA (`JpaRepository`, `@Query`)          |
| [Services (Business Logic)](04_business_logic_layer__services_.md) | Implement core business rules & orchestrate                  | Repositories, DTOs, Entities                 | Backend API Layer, other Services             | Java, Spring (`@Service`, `@Transactional`)                |
| [API Layer (Controllers)](05_api_layer__rest_controllers__.md) | Receive requests, delegate to Service, send response         | DTOs, Services                               | Frontend / Other Clients                      | Java, Spring MVC (`@RestController`, `@GetMapping`, etc.)    |
| [Security Configuration](06_security_configuration__backend_.md) | Define access rules, authenticate users, authorize actions | Backend API Layer, Auth Components, JWT tools | Spring Security Filter Chain                  | Java, Spring Security (`@Configuration`, `SecurityFilterChain`, `@PreAuthorize`, JWT support) |
| **Frontend Services**            | **Communicate with Backend API, fetch/send data**            | **HttpClient, Observables, DTOs (Frontend Models)** | **Frontend Components**                       | **TypeScript, Angular (`@Injectable`, `HttpClient`), RxJS (`Observable`)** |

Frontend Services are the first layer of our frontend application that connects to the backend. They translate the needs of the user interface into API calls and provide the results back in a usable format.

## Conclusion

In this chapter, we introduced **Frontend Services** in our Angular application. We learned that these services are the dedicated messengers responsible for handling communication with our secure backend API. They use Angular's `HttpClient` to make requests, deal with asynchronous operations using `Observable`s, and rely on an HTTP Interceptor to automatically add necessary security information like the JWT. By centralizing API calls in services, we keep our frontend components clean, reusable, and easier to maintain.

Now that our frontend knows *how* to get data from the backend using these services, the next step is to see *how* the different screens and parts of the user interface are built and how they use these services to display data and interact with the user. In the next chapter, we will explore **Frontend Components**.

[Next Chapter: Frontend Components](08_frontend_components_.md)

---
