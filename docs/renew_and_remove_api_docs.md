# Renew Plan & Remove Date/Time API Documentation

This document outlines the requests, responses, permissions, and behavior for the **Renew Plan** and **Remove Date and Time** endpoints.

---

## 1. Renew Plan API

This endpoint allows an administrator to extend or renew a user's subscription plan.

### Metadata
- **Endpoint:** `/admin/renew-plan`
- **Method:** `POST`
- **Authentication:** Bearer Token Required (JWT)
- **Required Role:** `ADMIN`

### Headers
| Header | Value | Description |
| :--- | :--- | :--- |
| `Authorization` | `Bearer <jwt_token>` | Admin JWT token |
| `Content-Type` | `application/json` | Request payload format |

### Request Body (JSON)
| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `userId` | `Number` / `String` | Yes | The database ID of the user whose plan is being renewed |
| `plan` | `String` | Yes | The plan extension type: `"weekly"`, `"monthly"`, or `"3 months"` (case-insensitive) |

#### Example Request Body
```json
{
  "userId": 7,
  "plan": "monthly"
}
```

### Response (200 OK)
Returns a boolean indicating success.
```json
true
```

### Error Responses
- **401 Unauthorized**: Missing or invalid token.
- **403 Forbidden**: Logged-in user is not an `ADMIN`.
- **404 Not Found**: Target user with the specified `userId` does not exist.
  - Body: `"user not found"`

### Extension Behavior
When renewed, the user's current expiration time (`expiresAt`) is updated relative to its existing value:
- `"weekly"`: Adds **1 week**
- `"monthly"`: Adds **1 month**
- `"3 months"` or `"3months"`: Adds **3 months**

---

## 2. Remove Date and Time API

This endpoint removes a specific date and time slot from an active ongoing booking session.

### Metadata
- **Endpoint:** `/task/remove/{date}/{sessionId}`
- **Method:** `DELETE`
- **Authentication:** Bearer Token Required (JWT)
- **Required Role:** `USER`, `MAINTAINER`, or `ADMIN`
- **Scope:** Session owners can only modify their own sessions.

### Headers
| Header | Value | Description |
| :--- | :--- | :--- |
| `Authorization` | `Bearer <jwt_token>` | User/Admin JWT token |

### Path Parameters
| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `date` | `String` | Yes | The date to remove (e.g. `"2026-07-08"`) |
| `sessionId` | `String` | Yes | The session ID of the active booking session |

### Query Parameters
| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `time` | `String` | Yes | The time slot/window to remove (e.g. `"6:00 AM - 8:00 AM"`) |

### Example Request URL
```http
DELETE http://localhost:8080/task/remove/2026-07-08/a8c9b3d1-394f-4e09-9f7e-1234567890ab?time=6:00%20AM%20-%208%3A00%20AM
```

### Response (200 OK)
Returns a boolean indicating success.
```json
true
```

### Error Responses
- **401 Unauthorized**: Missing or invalid token.
- **400 Bad Request**: Invalid session or parameters.

---

## 3. Get Session Time and Date API

This endpoint retrieves the scheduled dates and time slots associated with a specific active booking session.

### Metadata
- **Endpoint:** `/task/get-session-data/{sessionID}`
- **Method:** `GET`
- **Authentication:** Bearer Token Required (JWT)
- **Required Role:** `USER`, `MAINTAINER`, or `ADMIN`
- **Scope:** Users can only fetch data for their own sessions.

### Headers
| Header | Value | Description |
| :--- | :--- | :--- |
| `Authorization` | `Bearer <jwt_token>` | User JWT token |

### Path Parameters
| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `sessionID` | `String` | Yes | The unique session ID of the active booking session |

### Example Request URL
```http
GET http://localhost:8080/task/get-session-data/a8c9b3d1-394f-4e09-9f7e-1234567890ab
```

### Response (200 OK)
Returns a `SessionDateTimeRespons` object containing the dates and time slots for the session.
```json
{
  "dates": ["2026-07-10", "2026-07-11", "2026-07-12"],
  "times": ["6:00 AM - 8:00 AM", "12:00 PM - 2:00 PM"]
}
```

### Error Responses
- **401 Unauthorized**: Missing or invalid token.
- **400 Bad Request**: Session not found or an unexpected error occurred.
  - Body: `"<error message>"`
