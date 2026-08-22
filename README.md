<div align="center">
  <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white" alt="Java" />
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
</div>

# Blinkit Picker Automation Backend

A robust, multi-threaded Spring Boot application designed to automate slot booking and shift scheduling for pickers. This backend service intelligently polls available slots, filters them based on user preferences, and securely books them while automatically handling token refresh and session persistence.

## 🚀 Features

*   **Multi-threaded Booking Engine**: Utilizes Java Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`) to handle concurrent users highly efficiently without blocking system threads.
*   **Smart Slot Filtering**: Filters incoming store slots by availability (`isBooked`), eligibility (`isAllowed`), and strict user-defined preferred time windows.
*   **Auto Token Rotation**: Intercepts `401/403` unauthorized responses during API calls and automatically refreshes AWS Cognito JWT/session tokens without dropping the booking task.
*   **State Recovery**: Active booking sessions are persisted to a PostgreSQL database. On application restart or crash, active workers are restored seamlessly.
*   **Role-based Polling Rate**: Polling frequency adjusts dynamically based on the user role (e.g., faster 2-second intervals for Admins, 5-second intervals for standard users) to prevent API rate-limiting.

---

## 🛠️ Database Setup

This project uses **PostgreSQL**. You need to configure your database connection before running the application.

1. Install PostgreSQL on your system or use a cloud provider (e.g., AWS RDS).
2. Create a new database (e.g., `blinkit_db`).
3. Open `src/main/resources/application.properties` and update the datasource properties with your credentials:

```properties
spring.datasource.url=jdbc:postgresql://<YOUR_DATABASE_HOST>:<PORT>/<DATABASE_NAME>
spring.datasource.username=<YOUR_DB_USERNAME>
spring.datasource.password=<YOUR_DB_PASSWORD>

# Hibernate will automatically create and update your database schema
spring.jpa.hibernate.ddl-auto=update
```

---

## 🔗 Configuring API Endpoints (Dynamic Database Cache)

To avoid leaking private APIs and to allow dynamic updates without restarting the application, the API target URLs and sensitive configurations are decoupled from the Java codebase and `application.properties`. 

Instead, this project uses an **in-memory `AppCahe`** powered by the PostgreSQL database. **You must insert your API endpoints** directly into the `server_api_configs` table.

1. Once the application runs for the first time, Hibernate will create the `server_api_configs` table.
2. Insert your configurations into the table matching the `ApiEnums`:

```sql
INSERT INTO server_api_configs (api_name, api_url) VALUES 
('API_BASE', 'https://your_base_api_url'),
('SEND_OTP', '/your_send_otp_endpoint'),
('VERIFY_OTP', '/your_verify_otp_endpoint'),
('LOGIN', '/your_login_endpoint'),
('BOOK_SLOT', '/your_book_slot_endpoint'),
('FETCH_SLOTS', '/your_fetch_slots_endpoint'),
('ROATATE_TOKEN', 'https://your_cognito_url'),
('JWT_SECRET', 'YOUR_SECURE_JWT_SECRET_KEY'),
('JWT_EXPIRATION', '86400000');
```

The `AppCahe.java` service will automatically load these into a thread-safe `ConcurrentHashMap` on startup.

---

## 🧠 Understanding the Booking Algorithm

The core booking engine logic is located in `src/main/java/com/picker/BlinkitPicker/Services/Worker/BookingWorker.java`. 

The algorithm runs continuously in a `while(!isStop)` loop inside an isolated virtual thread for every user session. Here is a technical breakdown of its lifecycle:

### 1. Polling and Fetching Slots
The system fetches available slots for the user's specific `siteId` (Store ID) based on their geolocation headers (`X-Lat`, `X-Long`).

```java
// BookingWorker.java - fecthSlots()
FetchSlotsRequest request = FetchSlotsRequest.builder()
        .endDate(endDateUtc)
        .startDate(startDateUtc)
        .locationInfo(FetchSlotsRequest.Location.builder()
                .xLat(Double.parseDouble(headers.getXLat().trim()))
                .xLong(Double.parseDouble(headers.getXLong().trim()))
                .build())
        .build();

// Automatically handles token refresh wrapping via blockWithTokenRefresh
ResponseEntity<FetchSlotsResponse> response = blockWithTokenRefresh(
        "fetch slots",
        () -> webClientServices.getSlotsDetails(headers, request),
        headers.getRefreshToken(), headers);
```

### 2. Intelligent Time Matching
Fetched slots are pushed through a filtering pipeline. The system ensures `!isBooked` and `isAllowed` are true, then applies the user's exact time preferences using `DateToUtc.isTimeMatch()`.

```java
// BookingWorker.java - filterSlotId()
for (String preferredKey : times) {
    for (FetchSlotsResponse.Slot slot : availableSlots) {
        // Matches user preferred time against slot start/end UTC times
        if (DateToUtc.isTimeMatch(preferredKey, slot.getStartTime(), slot.getEndTime())) {
            String slotKey = DateToUtc.slotTimeKey(slot.getStartTime(), slot.getEndTime());
            matchedSlots.put(String.valueOf(slot.getId()), slotKey);
        }
    }
}
```

### 3. Execution & Session Logging
If the filter yields valid slots, a `bookSlots` API call is executed. Success increments the user's database records while writing to a thread-safe, in-memory log list for frontend consumption.

```java
// BookingWorker.java
ResponseEntity<GlobalRespons> bookingResponse = blockWithTokenRefresh(
        "book slots",
        () -> webClientServices.bookSlots(
               headers,
               BookSlotsRequest.builder().slotIds(slotIds).build(),
               timesLog), 
        headers.getRefreshToken(), headers);

if (bookingResponse.getStatusCode().is2xxSuccessful() && bookingResponse.getBody().isSuccess()) {
    addLog("Successfully booked " + slotIds.size() + " slot(s).");
    incrementBookedSlots(slotIds.size()); // Flushes count to PostgreSQL
}
```

## 🚀 How to Run locally

1. Clone the repository.
2. Set up your PostgreSQL instance and update `application.properties`.
3. Start the application once to generate the schema, then insert your secure API URLs and JWT configuration into the `server_api_configs` database table.
4. Run the Spring Boot application using the Maven wrapper:
   ```bash
   ./mvnw clean install
   ./mvnw spring-boot:run
   ```
